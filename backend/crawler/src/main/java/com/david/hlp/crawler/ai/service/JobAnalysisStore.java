package com.david.hlp.crawler.ai.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    public record Lease(long id,long jobId,long fence,String source){}
    JsonNode canonical(JsonNode n){
        if(n.isObject()){var o=json.createObjectNode();var keys=new TreeSet<String>();n.fieldNames().forEachRemaining(keys::add);keys.forEach(k->o.set(k,canonical(n.get(k))));return o;}
        if(n.isArray()){var a=json.createArrayNode();n.forEach(v->a.add(canonical(v)));return a;}return n;
    }
    public Lease claim(long jobId,String source){
        return tx.execute(status -> {
        try{
            String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(ai.detail(source)).toString().getBytes(StandardCharsets.UTF_8)));
            db.update("INSERT INTO job_ai_analysis(job_id,input_digest,pipeline_version,model_id,policy_version,state,source_json) VALUES (?,?,'jev-1',?,'jev-policy-1','pending',?) ON DUPLICATE KEY UPDATE id=id",jobId,digest,ai.model(),source);
            int claimed=db.update("UPDATE job_ai_analysis SET state='running',attempt_count=attempt_count+1,fence=fence+1,lease_until=DATE_ADD(NOW(6),INTERVAL 60 SECOND),error_code=NULL WHERE job_id=? AND input_digest=? AND pipeline_version='jev-1' AND model_id=? AND policy_version='jev-policy-1' AND attempt_count<3 AND (state='pending' OR (state='retry_wait' AND next_retry_at<=NOW(6)) OR (state='running' AND lease_until<=NOW(6)))",jobId,digest,ai.model());
            if(claimed!=1)return null;
            return db.queryForObject("SELECT id,fence FROM job_ai_analysis WHERE job_id=? AND input_digest=? AND pipeline_version='jev-1' AND model_id=? AND policy_version='jev-policy-1'",(r,i)->new Lease(r.getLong(1),jobId,r.getLong(2),source),jobId,digest,ai.model());
        }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
        });
    }
    public boolean finish(Lease lease,JsonNode result){
        return Boolean.TRUE.equals(tx.execute(status->{
            List<String> current=db.query("SELECT detail_data FROM t_job_detail WHERE id=? AND is_deleted=0 FOR UPDATE",(r,i)->r.getString(1),lease.jobId());
            if(current.isEmpty()||!ai.detail(current.get(0)).equals(ai.detail(lease.source()))){
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
    public void fail(Lease lease,String code){
        db.update("UPDATE job_ai_analysis SET state=IF(attempt_count>=3,'failed','retry_wait'),next_retry_at=DATE_ADD(NOW(6),INTERVAL 5 MINUTE),error_code=?,lease_until=NULL WHERE id=? AND fence=? AND state='running' AND lease_until>NOW(6)",code,lease.id(),lease.fence());
    }
    public int process(long start,long end){
        if(start<0||end<=start)throw new IllegalArgumentException("Explicit bounded AI job range required");
        db.update("UPDATE job_ai_analysis SET state='failed',error_code='attempts_exhausted',lease_until=NULL WHERE state='running' AND lease_until<=NOW(6) AND attempt_count>=3 AND job_id>? AND job_id<=?",start,end);
        var rows=db.query("SELECT j.id,j.detail_data FROM t_job_detail j WHERE j.is_deleted=0 AND j.id>? AND j.id<=? AND NOT EXISTS (SELECT 1 FROM job_ai_analysis a WHERE a.job_id=j.id AND a.source_json=CAST(j.detail_data AS JSON) AND a.model_id=? AND a.pipeline_version='jev-1' AND a.policy_version='jev-policy-1' AND (a.state IN ('completed','needs_review','failed') OR (a.state='running' AND a.lease_until>NOW(6)) OR (a.state='retry_wait' AND a.next_retry_at>NOW(6)))) ORDER BY j.id LIMIT 10",(r,i)->Map.entry(r.getLong(1),r.getString(2)),start,end,ai.model());
        int completed=0;
        for(var row:rows){
            Lease lease=claim(row.getKey(),row.getValue());if(lease==null)continue;
            try{if(finish(lease,ai.analyze(Long.toString(lease.jobId()),lease.source())))completed++;}
            catch(org.springframework.web.server.ResponseStatusException e){fail(lease,e.getStatusCode().value()==504?"deadline":"ai_unavailable");}
        }
        return completed;
    }
}
