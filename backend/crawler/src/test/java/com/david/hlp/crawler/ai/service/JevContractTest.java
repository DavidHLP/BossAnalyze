package com.david.hlp.crawler.ai.service;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class JevContractTest {
    final ObjectMapper json=new ObjectMapper();
    AIExtractionOfJobRequirements client(){return new AIExtractionOfJobRequirements(json,"http://synthetic.test","synthetic-only","typesafe/jev-1.14.0",new RestTemplate());}
    String analysis(String status,String items){return "{\"jobId\":\"42\",\"status\":\""+status+"\",\"requirements\":"+items+",\"benefits\":[],\"coreRequirements\":[],\"model\":\"typesafe/jev-1.14.0\",\"pipelineVersion\":\"jev-1\",\"policyVersion\":\"jev-policy-1\",\"requestId\":\"test\",\"usage\":{\"inputTokens\":null,\"outputTokens\":null}}";}
    @Test void authenticatedBusinessRequestAndUnicodeEvidenceRoundTrip(){
        var ai=client();var http=(RestTemplate)ReflectionTestUtils.getField(ai,"http");var server=MockRestServiceServer.bindTo(http).build();
        String item="[{\"candidateId\":\"r1\",\"text\":\"Java, (Spring Boot)\",\"probability\":0.99,\"importance\":null,\"evidence\":[{\"sourcePath\":\"/text\",\"start\":1,\"end\":20,\"text\":\"Java, (Spring Boot)\"}]}]";
        // The end is in code points: emoji occupies one, unlike a Java char index.
        server.expect(requestTo("http://synthetic.test/internal/v1/job-analysis")).andExpect(header("Authorization","Bearer synthetic-only"))
            .andRespond(withSuccess(analysis("complete",item),MediaType.APPLICATION_JSON));
        // 19 code points of text, following one emoji.
        assertEquals(List.of("Java, (Spring Boot)"),ai.texts(ai.analyze("42","{\"text\":\"😀Java, (Spring Boot)\"}"),"requirements"));server.verify();
    }
    @Test void badEvidenceAndNonfiniteScoresFailClosed()throws Exception{
        assertThrows(ResponseStatusException.class,()->AIExtractionOfJobRequirements.evidence(json.readTree("[{\"sourcePath\":\"/x\",\"start\":0,\"end\":1,\"text\":\"invented\"}]"),json.readTree("{\"x\":\"a\"}"),"invented"));
        assertThrows(ResponseStatusException.class,()->AIExtractionOfJobRequirements.optionalNumber(com.fasterxml.jackson.databind.node.DoubleNode.valueOf(Double.NaN),10));
        assertThrows(ResponseStatusException.class,()->AIExtractionOfJobRequirements.optionalNumber(json.readTree("10.01"),10));
    }
    @Test void upstreamAuthDoesNotInvalidateUserAndNoRetry(){
        var ai=client();var server=MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(ai,"http")).build();
        server.expect(requestTo("http://synthetic.test/internal/v1/job-analysis")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertEquals(503,assertThrows(ResponseStatusException.class,()->ai.analyze("42","{}")).getStatusCode().value());server.verify();
    }
    @Test void missingTokenDoesNotPreventConstruction(){
        var ai=new AIExtractionOfJobRequirements(json,"http://synthetic.test","","model");
        assertEquals(503,assertThrows(ResponseStatusException.class,()->ai.analyze("42","{}")).getStatusCode().value());
    }
    @Test void deletedChangedAndExpiredLeasesNeverProject()throws Exception{
        for(int mode=0;mode<3;mode++){
            JdbcTemplate db=mock(JdbcTemplate.class);PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
            when(manager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
            when(db.query(anyString(),org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(),any(Object[].class)))
                .thenReturn(mode==0?List.of():List.of(mode==1?"{\"x\":2}":"{\"x\":1}"));
            var store=new JobAnalysisStore(db,manager,json,client(),true);
            assertFalse(store.finish(new JobAnalysisStore.Lease(1,42,7,"{\"x\":1}"),json.readTree(analysis("complete","[]"))));
            verify(db,never()).update(startsWith("UPDATE t_job_detail"),any(Object[].class));
        }
    }
    @Test void canonicalSourceKeepsSnapshotAndHasStableKeyOrder()throws Exception{
        var store=new JobAnalysisStore(mock(JdbcTemplate.class),mock(PlatformTransactionManager.class),json,client(),false);
        assertEquals(store.canonical(json.readTree("{\"b\":[2,1],\"a\":\"😀\"}")),store.canonical(json.readTree("{\"a\":\"😀\",\"b\":[2,1]}")));
    }
    @Test void matchIdSetAndUnknownScoredCombinationAreRejected() throws Exception {
        for (String id : List.of("extra", "42")) {
            var ai=client();var server=MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(ai,"http")).build();
            var out=json.createObjectNode().put("complete",true).put("model","typesafe/jev-1.14.0").put("policyVersion","jev-policy-1").put("requestId","test");
            out.putObject("usage").putNull("inputTokens").putNull("outputTokens");
            var item=out.putArray("items").addObject().put("jobId",id).put("status","scored").put("score",9.5);
            var dims=item.putArray("dimensions");for(String name:List.of("skills","relevant_experience","experience","education"))dims.addObject().put("name",name).put("status","unknown").putNull("rawScore").putNull("confidence").putNull("probabilities").putArray("evidence");
            server.expect(requestTo("http://synthetic.test/internal/v1/match-batch")).andRespond(withSuccess(out.toString(),MediaType.APPLICATION_JSON));
            var job=json.createObjectNode().put("jobId","42");job.putObject("detailData");
            assertEquals(502,assertThrows(ResponseStatusException.class,()->ai.match("Java",List.of(job),30000)).getStatusCode().value());server.verify();
        }
    }

}
