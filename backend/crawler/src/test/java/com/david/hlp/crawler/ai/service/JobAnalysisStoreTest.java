package com.david.hlp.crawler.ai.service;

import com.david.hlp.commons.recruitment.JobAnalysisFacts;
import com.fasterxml.jackson.databind.*;
import java.sql.ResultSet;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.*;
import org.springframework.transaction.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** In-memory JDBC predicate fixture, not evidence of MySQL locking/transaction semantics. */
class JobAnalysisStoreTest {
    final ObjectMapper json=new ObjectMapper();
    static class Row {
        long id,job,fence; int attempts; boolean live;
        String state="pending",result,source,error;
    }
    class Fixture {
        final TreeMap<Long,String> jobs=new TreeMap<>();
        final Map<String,Row> rows=new HashMap<>();
        final Map<Long,String> projection=new HashMap<>();
        final JdbcTemplate db=mock(JdbcTemplate.class);
        final AIExtractionOfJobRequirements ai=spy(new JevContractTest().client());
        final JobAnalysisStore store;
        Fixture() throws Exception {
            var manager=mock(PlatformTransactionManager.class);
            when(manager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
            store=new JobAnalysisStore(db,manager,json,ai,true);
            doAnswer(call->{
                String sql=call.getArgument(0); Object[] a=call.getArguments();
                RowMapper<?> mapper=(RowMapper<?>)a[1]; var out=new ArrayList<Object>();
                if(sql.startsWith("SELECT j.id")) {
                    for(var entry:jobs.entrySet())if(entry.getKey()>(Long)a[2] && entry.getKey()<=(Long)a[3] && out.size()<10)
                        out.add(mapper.mapRow(rs(entry.getKey(),entry.getValue()),out.size()));
                } else if(sql.startsWith("SELECT detail_data")) {
                    if(jobs.containsKey((Long)a[2]))out.add(mapper.mapRow(rs(jobs.get((Long)a[2])),0));
                } else if(sql.startsWith("SELECT result_json")) {
                    Row r=rows.get(a[2]+":"+a[3]);
                    if(r!=null && (r.state.equals("superseded") || r.state.equals("running")&&!r.live))out.add(mapper.mapRow(rs(r.result),0));
                } else throw new AssertionError(sql);
                return out;
            }).when(db).query(anyString(),any(RowMapper.class),any(Object[].class));
            doAnswer(call->{Object[] a=call.getArguments();Row r=rows.get(a[2]+":"+a[3]);return ((RowMapper<?>)a[1]).mapRow(rs(r.id,r.fence),0);})
                .when(db).queryForObject(anyString(),any(RowMapper.class),any(Object[].class));
            doAnswer(call->{
                Object[] a=call.getArguments();String sql=(String)a[0];
                if(sql.startsWith("INSERT")) {
                    rows.computeIfAbsent(a[1]+":"+a[2],k->{Row r=new Row();r.id=rows.size()+1;r.job=(Long)a[1];r.source=(String)a[4];return r;});return 1;
                }
                if(sql.contains("attempt_count=attempt_count+?")) {
                    Row r=rows.get(a[3]+":"+a[4]);
                    if((r.attempts>=3 && !(Boolean)a[6]) || !(Set.of("pending","superseded","retry_wait").contains(r.state)||r.state.equals("running")&&!r.live))return 0;
                    r.attempts+=(Integer)a[1];r.source=(String)a[2];r.fence++;r.live=true;r.state="running";return 1;
                }
                if(sql.startsWith("UPDATE t_job_detail")){projection.put((Long)a[3],(String)a[1]);return 1;}
                if(sql.contains("updated_at=updated_at")) {
                    rows.values().stream().filter(r->r.job==(Long)a[1] && r.id!=(Long)a[2] && Set.of("pending","running","retry_wait","completed","needs_review").contains(r.state)).forEach(r->r.state="superseded");return 1;
                }
                if(sql.contains("attempts_exhausted"))return 0;
                boolean finish=sql.contains("result_json=?"), failure=sql.contains("error_code=?");
                long id=(Long)a[finish?5:failure?4:1],fence=(Long)a[finish?6:failure?5:2];
                Row r=rows.values().stream().filter(v->v.id==id).findFirst().orElseThrow();
                if(r.fence!=fence || !r.live || !r.state.equals("running"))return 0;
                r.live=false;
                if(finish){r.state=(String)a[1];r.result=(String)a[2];}
                else if(failure){r.state=r.attempts>=3||!(Boolean)a[1]?"failed":"retry_wait";r.error=(String)a[3];}
                else r.state="superseded";
                return 1;
            }).when(db).update(anyString(),any(Object[].class));
            doAnswer(call->output(call.getArgument(0),call.getArgument(1))).when(ai).analyze(anyString(),anyString());
        }
        Row row(String source) {return rows.get("42:"+JobAnalysisFacts.digest(ai.detail(source)));}
    }
    ResultSet rs(Object... values) throws Exception {
        var rs=mock(ResultSet.class);
        when(rs.getString(anyInt())).thenAnswer(c->(String)values[(Integer)c.getArgument(0)-1]);
        when(rs.getLong(anyInt())).thenAnswer(c->((Number)values[(Integer)c.getArgument(0)-1]).longValue());return rs;
    }
    String source(String text){return "{\"jobTags\":[\""+text+"\"]}";}
    JsonNode output(String id,String source) throws Exception {
        String text=json.readTree(source).path("jobTags").get(0).asText();
        var out=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(new JevContractTest().analysis("complete","[]"));out.put("jobId",id);
        var item=out.withArray("requirements").addObject().put("candidateId","c0").put("text",text).put("probability",0.99).putNull("importance");
        item.putArray("evidence").addObject().put("sourcePath","/jobTags/0").put("start",0).put("end",text.codePointCount(0,text.length())).put("text",text);
        return out;
    }
    @Test void restorationReprojectsValidatedCacheWithoutAnotherCallEvenAtBudget() throws Exception {
        var f=new Fixture();String a=source("😀A"),b=source("B");f.jobs.put(42L,a);
        assertEquals(1,f.store.process(0,50));f.jobs.put(42L,b);assertEquals(1,f.store.process(0,50));
        Row old=f.row(a);assertEquals("superseded",old.state);old.attempts=3;
        String restored=a.substring(0,a.length()-1)+",\"updateTime\":3}";
        f.jobs.put(42L,restored);long previousFence=old.fence;
        assertEquals(1,f.store.process(0,50));assertEquals("[\"😀A\"]",f.projection.get(42L));
        assertEquals(3,old.attempts);assertTrue(old.fence>previousFence);assertEquals(restored,old.source);
        assertFalse(f.store.finish(new JobAnalysisStore.Lease(old.id,42,previousFence,a),output("42",a)));
        verify(f.ai,times(2)).analyze(anyString(),anyString());
    }
    @Test void expiredCacheReplayAtExhaustedBudgetCanBeReclaimedWithoutPaying() throws Exception {
        var f=new Fixture();String a=source("A");f.jobs.put(42L,a);assertEquals(1,f.store.process(0,50));
        Row r=f.row(a);r.state="superseded";r.attempts=3;
        var lost=f.store.claim(42,a);assertNotNull(lost.cached());r.live=false;
        var restored=f.store.claim(42,a);assertNotNull(restored.cached());assertTrue(restored.fence()>lost.fence());
        assertFalse(f.store.finish(lost,lost.cached()));assertTrue(f.store.finish(restored,restored.cached()));
        assertEquals(3,r.attempts);verify(f.ai,times(1)).analyze(anyString(),anyString());
    }
    @Test void metadataDoesNotRebillAndChangedFactsDoWhileClaimRechecksSource() throws Exception {
        var f=new Fixture();String a=source("A");f.jobs.put(42L,a);assertEquals(1,f.store.process(0,50));
        String metadata=a.substring(0,a.length()-1)+",\"updateTime\":2,\"analysisReport\":\"ignore\",\"company\":\"ignore\"}";
        f.jobs.put(42L,metadata);assertEquals(0,f.store.process(0,50));assertEquals(1,f.rows.size());
        assertNull(f.store.claim(42,source("B")));
        f.jobs.put(42L,source("B"));assertEquals(1,f.store.process(0,50));assertEquals(2,f.rows.size());
        verify(f.ai,times(2)).analyze(anyString(),anyString());
        var lease=f.store.claim(43,a);assertNull(lease);
    }
    @Test void terminalFirstTenDoNotStarveAndDeterministicErrorsAreNeverReclaimed() throws Exception {
        for(int status:List.of(400,422)) {
            var f=new Fixture();for(long id=1;id<=10;id++)f.jobs.put(id,source("A"));
            doThrow(AIExtractionOfJobRequirements.error(status,"synthetic")).when(f.ai).analyze(anyString(),anyString());
            assertEquals(0,f.store.process(0,20));
            assertTrue(f.rows.values().stream().allMatch(r->r.state.equals("failed")&&r.error.equals("invalid_input_"+status)));
            f.jobs.put(11L,source("B"));doAnswer(c->output(c.getArgument(0),c.getArgument(1))).when(f.ai).analyze(anyString(),anyString());
            assertEquals(1,f.store.process(0,20));assertEquals(0,f.store.process(0,20));
            verify(f.ai,times(11)).analyze(anyString(),anyString());
        }
    }
    @Test void supersededWithoutUsableCacheHonorsBudgetAndCannotBlockNextPage() throws Exception {
        var f=new Fixture();
        for(long id=1;id<=10;id++) {
            f.jobs.put(id,source("A"));var lease=f.store.claim(id,source("A"));
            Row r=f.rows.values().stream().filter(v->v.id==lease.id()).findFirst().orElseThrow();r.state="superseded";r.attempts=3;
        }
        f.jobs.put(11L,source("B"));assertEquals(1,f.store.process(0,20));
        verify(f.ai,times(1)).analyze(anyString(),anyString());
    }
    @Test void completionAllowsMetadataChangesButRejectsChangedDeletedAndExpiredSources() throws Exception {
        for(String mode:List.of("metadata","facts","deleted","expired")) {
            var f=new Fixture();String a=source("A");f.jobs.put(42L,a);var lease=f.store.claim(42,a);
            if(mode.equals("metadata"))f.jobs.put(42L,a.substring(0,a.length()-1)+",\"updateTime\":2}");
            if(mode.equals("facts"))f.jobs.put(42L,source("B"));
            if(mode.equals("deleted"))f.jobs.remove(42L);
            if(mode.equals("expired"))f.row(a).live=false;
            assertEquals(mode.equals("metadata"),f.store.finish(lease,output("42",a)));
            assertEquals(mode.equals("metadata"),f.projection.containsKey(42L));
        }
    }
    @Test void disabledWritebackReusesValidatedCacheWithoutProjectingOrPaying() throws Exception {
        var f=new Fixture();String a=source("A");f.jobs.put(42L,a);assertEquals(1,f.store.process(0,50));
        Row r=f.row(a);r.state="superseded";
        var manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        var disabled=new JobAnalysisStore(f.db,manager,json,f.ai,false);
        clearInvocations(f.ai,f.db);
        assertEquals(0,disabled.process(0,50));assertEquals("needs_review",r.state);assertEquals(1,r.attempts);
        verify(f.ai,never()).analyze(anyString(),anyString());
        verify(f.db,never()).update(startsWith("UPDATE t_job_detail"),any(Object[].class));
    }
    @Test void incompleteSupersededResultsSpendAnAttemptButNoEvidenceCacheDoesNot() throws Exception {
        for(String status:List.of("needs_review","no_evidence")) {
            var f=new Fixture();String a=source("A");f.jobs.put(42L,a);var lease=f.store.claim(42,a);
            var out=json.readTree(new JevContractTest().analysis(status,"[]"));
            f.store.finish(lease,out);Row r=f.row(a);r.state="superseded";
            var restored=f.store.claim(42,a);
            assertEquals(status.equals("needs_review"),restored.cached()==null);
            assertEquals(status.equals("needs_review")?2:1,r.attempts);
        }
    }
    @Test void badCachedEvidenceOrIdentityIsNotReusedAndRetryBudgetStaysThree() throws Exception {
        for(String corrupt:List.of("evidence","model","policy","pipeline")) {
            var f=new Fixture();String a="{\"jobTags\":[\"A\"],\"company\":\"A\"}";f.jobs.put(42L,a);assertEquals(1,f.store.process(0,50));Row r=f.row(a);r.state="superseded";
            r.result=r.result.replace(corrupt.equals("evidence")?"/jobTags/0":corrupt.equals("model")?"typesafe/jev-1.14.0":corrupt.equals("policy")?"jev-policy-1":"jev-1",corrupt.equals("evidence")?"/company":"wrong");
            var lease=f.store.claim(42,a);assertNull(lease.cached());assertEquals(2,r.attempts);
            f.store.fail(lease,"deadline",true);assertEquals("retry_wait",r.state);
            lease=f.store.claim(42,a);assertEquals(3,r.attempts);f.store.fail(lease,"deadline",true);
            assertEquals("failed",r.state);assertNull(f.store.claim(42,a));
        }
    }
}
