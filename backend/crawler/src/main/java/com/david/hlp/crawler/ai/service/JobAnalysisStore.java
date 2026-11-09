package com.david.hlp.crawler.ai.service;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import com.david.hlp.commons.recruitment.JobAnalysisFacts;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class JobAnalysisStore {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final AIExtractionOfJobRequirements ai;
    private final boolean writeback;
    public JobAnalysisStore(JdbcTemplate db,org.springframework.transaction.PlatformTransactionManager manager,ObjectMapper json,
            AIExtractionOfJobRequirements ai,@Value("${crawler.ai.writeback-enabled:false}")boolean writeback){
        this.db=db;this.tx=new TransactionTemplate(manager);this.json=json;this.ai=ai;this.writeback=writeback;
    }
    public record Lease(long id,long jobId,long fence,String source,JsonNode cached){
        public Lease(long id,long jobId,long fence,String source){this(id,jobId,fence,source,null);}
    }
    public Lease claim(long jobId,String source){
        return tx.execute(status -> {
            var current=db.query("SELECT detail_data FROM t_job_detail WHERE id=? AND is_deleted=0 FOR UPDATE",(r,i)->r.getString(1),jobId);
            if(current.isEmpty() || !JobAnalysisFacts.same(ai.detail(source),ai.detail(current.get(0))))return null;
            String snapshot=current.get(0);
            String digest=JobAnalysisFacts.digest(ai.detail(snapshot));
            db.update("INSERT INTO job_ai_analysis(job_id,input_digest,pipeline_version,model_id,policy_version,state,source_json) VALUES (?,?,'jev-1',?,'jev-policy-1','pending',?) ON DUPLICATE KEY UPDATE id=id",jobId,digest,ai.model(),snapshot);
            var cachedRows=db.query("SELECT result_json FROM job_ai_analysis WHERE job_id=? AND input_digest=? AND pipeline_version='jev-1' AND model_id=? AND policy_version='jev-policy-1' AND (state='superseded' OR (state='running' AND lease_until<=NOW(6))) FOR UPDATE",(r,i)->r.getString(1),jobId,digest,ai.model());
            JsonNode cached=null;
            if(!cachedRows.isEmpty() && cachedRows.get(0)!=null){
                try{
                    JsonNode result=json.readTree(cachedRows.get(0));
                    ai.validateAnalysis(Long.toString(jobId),ai.detail(snapshot),result);
                    if(Set.of("complete","no_evidence").contains(result.path("status").asText()))cached=result;
                }catch(java.io.IOException | org.springframework.web.server.ResponseStatusException ignored){ }
            }
            // Cache replay spends no inference attempt, but always obtains a fresh fence and lease.
            int claimed=db.update("UPDATE job_ai_analysis SET state='running',attempt_count=attempt_count+?,fence=fence+1,source_json=?,lease_until=DATE_ADD(NOW(6),INTERVAL 60 SECOND),next_retry_at=NULL,error_code=NULL WHERE job_id=? AND input_digest=? AND pipeline_version='jev-1' AND model_id=? AND policy_version='jev-policy-1' AND (attempt_count<3 OR ?) AND (state IN ('pending','superseded') OR (state='retry_wait' AND next_retry_at<=NOW(6)) OR (state='running' AND lease_until<=NOW(6)))",cached==null?1:0,snapshot,jobId,digest,ai.model(),cached!=null);
            if(claimed!=1){
                db.update("UPDATE job_ai_analysis SET state='failed',error_code='attempts_exhausted',lease_until=NULL WHERE job_id=? AND input_digest=? AND pipeline_version='jev-1' AND model_id=? AND policy_version='jev-policy-1' AND state='running' AND lease_until<=NOW(6) AND attempt_count>=3",jobId,digest,ai.model());
                return null;
            }
            JsonNode reuse=cached;
            return db.queryForObject("SELECT id,fence FROM job_ai_analysis WHERE job_id=? AND input_digest=? AND pipeline_version='jev-1' AND model_id=? AND policy_version='jev-policy-1'",(r,i)->new Lease(r.getLong(1),jobId,r.getLong(2),snapshot,reuse),jobId,digest,ai.model());
        });
    }
    public boolean finish(Lease lease,JsonNode result){
        return Boolean.TRUE.equals(tx.execute(status->{
            List<String> current=db.query("SELECT detail_data FROM t_job_detail WHERE id=? AND is_deleted=0 FOR UPDATE",(r,i)->r.getString(1),lease.jobId());
            if(current.isEmpty()||!JobAnalysisFacts.same(ai.detail(current.get(0)),ai.detail(lease.source()))){
                db.update("UPDATE job_ai_analysis SET state='superseded',lease_until=NULL WHERE id=? AND fence=? AND state='running' AND lease_until>NOW(6)",lease.id(),lease.fence());return false;
            }
            boolean complete=Set.of("complete","no_evidence").contains(result.path("status").asText())&&writeback;
            int updated=db.update("UPDATE job_ai_analysis SET state=?,result_json=?,input_tokens=?,output_tokens=?,lease_until=NULL WHERE id=? AND fence=? AND state='running' AND lease_until>NOW(6)",complete?"completed":"needs_review",result.toString(),tokens(result,"inputTokens"),tokens(result,"outputTokens"),lease.id(),lease.fence());
            if(updated!=1)return false;
            if(complete){
                db.update("UPDATE t_job_detail SET job_requirements=?,employee_benefits=? WHERE id=? AND is_deleted=0",json.valueToTree(ai.texts(result,"requirements")).toString(),json.valueToTree(ai.texts(result,"benefits")).toString(),lease.jobId());
                // Preserve previous timestamps so latest-attempt readers select this completion.
                db.update("UPDATE job_ai_analysis SET state='superseded',updated_at=updated_at WHERE job_id=? AND id<>? AND state IN ('pending','running','retry_wait','completed','needs_review')",lease.jobId(),lease.id());
            }
            return complete;
        }));
    }
    private Long tokens(JsonNode result,String field){JsonNode n=result.path("usage").path(field);return n.isNull()?null:n.longValue();}
    public void fail(Lease lease,String code,boolean retryable){
        db.update("UPDATE job_ai_analysis SET state=IF(attempt_count>=3 OR NOT ?,'failed','retry_wait'),next_retry_at=IF(attempt_count>=3 OR NOT ?,NULL,DATE_ADD(NOW(6),INTERVAL 5 MINUTE)),error_code=?,lease_until=NULL WHERE id=? AND fence=? AND state='running' AND lease_until>NOW(6)",retryable,retryable,code,lease.id(),lease.fence());
    }
    public int process(long start,long end){
        if(start<0||end<=start)throw new IllegalArgumentException("Explicit bounded AI job range required");
        int completed=0,claimed=0;
        long cursor=start;
        // Keyset paging applies eligibility in claim using the exact shared digest, before the ten-lease cap.
        while(cursor<end && claimed<10){
            var rows=db.query("SELECT j.id,j.detail_data FROM t_job_detail j WHERE j.is_deleted=0 AND j.id>? AND j.id<=? ORDER BY j.id LIMIT 10",(r,i)->Map.entry(r.getLong(1),r.getString(2)),cursor,end);
            if(rows.isEmpty())break;
            for(var row:rows){
                cursor=row.getKey();
                Lease lease=claim(row.getKey(),row.getValue());if(lease==null)continue;
                claimed++;
                try{if(finish(lease,lease.cached()!=null?lease.cached():ai.analyze(Long.toString(lease.jobId()),lease.source())))completed++;}
                catch(org.springframework.web.server.ResponseStatusException e){
                    int code=e.getStatusCode().value();
                    boolean retryable=Set.of(429,502,503,504).contains(code);
                    fail(lease,code==400?"invalid_input_400":code==422?"invalid_input_422":code==504?"deadline":code==502?"bad_output":retryable?"ai_unavailable":"ai_rejected",retryable);
                }
                if(claimed==10)break;
            }
        }
        return completed;
    }
}
