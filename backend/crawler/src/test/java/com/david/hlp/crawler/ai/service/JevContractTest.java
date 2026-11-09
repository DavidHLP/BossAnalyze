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
        String item="[{\"candidateId\":\"r1\",\"text\":\"Java, (Spring Boot)\",\"probability\":0.99,\"importance\":null,\"evidence\":[{\"sourcePath\":\"/jobTags/0\",\"start\":1,\"end\":20,\"text\":\"Java, (Spring Boot)\"}]}]";
        // The end is in code points: emoji occupies one, unlike a Java char index.
        server.expect(requestTo("http://synthetic.test/internal/v1/job-analysis")).andExpect(header("Authorization","Bearer synthetic-only"))
            .andRespond(withSuccess(analysis("complete",item),MediaType.APPLICATION_JSON));
        // 19 code points of text, following one emoji.
        assertEquals(List.of("Java, (Spring Boot)"),ai.texts(ai.analyze("42","{\"jobTags\":[\"😀Java, (Spring Boot)\"]}"),"requirements"));server.verify();
    }
    @Test void extractionTotalSpansBatchesButKeepsCandidateAndEvidenceLimits() throws Exception {
        for (String mode : List.of("49", "192", "193", "duplicate", "bad-evidence", "numeric-id", "overflow")) {
            int count = mode.matches("[0-9]+") ? Integer.parseInt(mode) : 49;
            var items = json.createArrayNode();
            var source = json.createObjectNode().putObject("jobDescription");
            for (int i=0;i<count;i++) {
                String text="条件"+i;
                source.put("r"+i,text);
                var item=items.addObject().put("candidateId","c"+i).put("text",text).put("probability",0.99).putNull("importance");
                item.putArray("evidence").addObject().put("sourcePath","/jobDescription/r"+i).put("start",0).put("end",text.length()).put("text",text);
            }
            if(mode.equals("duplicate"))((com.fasterxml.jackson.databind.node.ObjectNode)items.get(1)).put("candidateId","c0");
            if(mode.equals("bad-evidence"))((com.fasterxml.jackson.databind.node.ObjectNode)items.get(0).path("evidence").get(0)).put("text","invented");
            if(mode.equals("overflow"))((com.fasterxml.jackson.databind.node.ObjectNode)items.get(0).path("evidence").get(0)).put("start",4294967296L);
            var response=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(analysis("needs_review",items.toString()));
            response.set("benefits",items.deepCopy());
            var core=response.putArray("coreRequirements");for(int i=0;i<Math.min(20,count);i++)core.add(items.get(i));
            if(mode.equals("numeric-id"))response.put("jobId",42);
            var ai=client();var server=MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(ai,"http")).build();
            server.expect(requestTo("http://synthetic.test/internal/v1/job-analysis")).andRespond(withSuccess(response.toString(),MediaType.APPLICATION_JSON));
            var input=json.createObjectNode();input.set("jobDescription",source);
            if(mode.equals("49")||mode.equals("192"))assertEquals(count,ai.analyze("42",input.toString()).path("requirements").size());
            else assertEquals(502,assertThrows(ResponseStatusException.class,()->ai.analyze("42",input.toString())).getStatusCode().value());
            server.verify();
        }
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
    @Test void pythonInputLimitsRemainInputErrorsWithoutRetry(){
        for(HttpStatus status:List.of(HttpStatus.BAD_REQUEST,HttpStatus.UNPROCESSABLE_ENTITY)) {
            var ai=client();var server=MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(ai,"http")).build();
            server.expect(requestTo("http://synthetic.test/internal/v1/job-analysis")).andRespond(withStatus(status));
            assertEquals(status.value(),assertThrows(ResponseStatusException.class,()->ai.analyze("42","{}")).getStatusCode().value());server.verify();
        }
    }
    @Test void missingTokenDoesNotPreventConstruction(){
        var ai=new AIExtractionOfJobRequirements(json,"http://synthetic.test","","model");
        assertEquals(503,assertThrows(ResponseStatusException.class,()->ai.analyze("42","{}")).getStatusCode().value());
    }
    @Test void inferenceDeadlineLeavesReturnTimeInsideTheReadBudget() {
        for(int timeout:List.of(3,100,3000,30000)) {
            var ai=client();var server=MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(ai,"http")).build();
            int connect=Math.min(1000,Math.max(1,timeout/2)),read=timeout-connect;
            server.expect(requestTo("http://synthetic.test/internal/v1/match-batch"))
                .andExpect(request->{
                    var body=json.readTree(((org.springframework.mock.http.client.MockClientHttpRequest)request).getBodyAsString());
                    assertTrue(body.path("timeoutMs").asInt()>0);
                    assertTrue(body.path("timeoutMs").asInt()<read);
                    if(timeout==30000)assertEquals(28000,body.path("timeoutMs").asInt());
                }).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
            assertEquals(503,assertThrows(ResponseStatusException.class,()->ai.match("synthetic",List.of(),timeout)).getStatusCode().value());
            server.verify();
        }
        var ai=client();var server=MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(ai,"http")).build();
        for(int timeout:List.of(0,1,2))assertEquals(504,assertThrows(ResponseStatusException.class,()->ai.match("synthetic",List.of(),timeout)).getStatusCode().value());
        server.verify();
    }
    @Test void deletedChangedAndExpiredLeasesNeverProject()throws Exception{
        for(int mode=0;mode<3;mode++){
            JdbcTemplate db=mock(JdbcTemplate.class);PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
            when(manager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
            when(db.query(anyString(),org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<String>>any(),any(Object[].class)))
                .thenReturn(mode==0?List.of():List.of(mode==1?"{\"jobTags\":[\"B\"]}":"{\"jobTags\":[\"A\"]}"));
            var store=new JobAnalysisStore(db,manager,json,client(),true);
            assertFalse(store.finish(new JobAnalysisStore.Lease(1,42,7,"{\"jobTags\":[\"A\"]}"),json.readTree(analysis("complete","[]"))));
            verify(db,never()).update(startsWith("UPDATE t_job_detail"),any(Object[].class));
        }
    }
    @Test void pythonFiveLevelAndNullableDimensionShapeIsAcceptedWithoutDroppingValidation() throws Exception {
        for(String mode:List.of("valid","unknown","bad-distribution","missing-nullable","rounded","lower-bound","upper-bound","outside-lower","outside-upper","unnormalizable")) {
            var ai=client();var server=MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(ai,"http")).build();
            var out=json.createObjectNode().put("complete",!mode.equals("unknown")).put("model","typesafe/jev-1.14.0").put("policyVersion","jev-policy-1").put("requestId","test");
            out.putObject("usage").putNull("inputTokens").put("outputTokens",30);
            var item=out.putArray("items").addObject().put("jobId","42").put("status",mode.equals("unknown")?"insufficient_evidence":"scored");
            if(mode.equals("unknown"))item.putNull("score");else item.put("score",8.75);
            var dims=item.putArray("dimensions");
            for(String name:List.of("skills","relevant_experience","experience","education")) {
                var d=dims.addObject().put("name",name);
                if(name.equals("education")||mode.equals("unknown")) {
                    d.put("status",mode.equals("unknown")?"unknown":"not_applicable").putNull("rawScore").putNull("confidence").putNull("probabilities").putArray("evidence");
                } else {
                    d.put("status","scored").put("rawScore",3.5).put("confidence",0.95);
                    d.putObject("probabilities").put("0",0).put("1",0).put("2",0).put("3",0.5).put("4",0.5);
                    var ev=d.putArray("evidence");
                    for(String path:List.of("/job/jobTags/0","/resume"))ev.addObject().put("sourcePath",path).put("start",0).put("end",5).put("text","😀Java");
                }
            }
            if(Set.of("rounded","lower-bound","upper-bound","outside-lower","outside-upper","unnormalizable").contains(mode)) {
                // [.335,.335,.325,0,0] leaves .005 mass: feasible expectation is [.985,1.005].
                double raw=switch(mode){case "lower-bound" -> .98; case "upper-bound" -> 1.01;
                    case "outside-lower" -> .979999; case "outside-upper" -> 1.010001; default -> 1;};
                item.put("score",raw*2.5);
                for(var d:dims)if(d.path("status").asText().equals("scored")) {
                    var dimension=(com.fasterxml.jackson.databind.node.ObjectNode)d;
                    dimension.put("rawScore",raw);
                    dimension.putObject("probabilities").put("0",mode.equals("unnormalizable")?.36:.34).put("1",.34).put("2",.33).put("3",0).put("4",0);
                }
            }
            if(mode.equals("bad-distribution"))((com.fasterxml.jackson.databind.node.ObjectNode)dims.get(0).path("probabilities")).put("4",0.1);
            if(mode.equals("missing-nullable"))((com.fasterxml.jackson.databind.node.ObjectNode)dims.get(3)).remove("probabilities");
            server.expect(requestTo("http://synthetic.test/internal/v1/match-batch")).andRespond(withSuccess(out.toString(),MediaType.APPLICATION_JSON));
            var job=json.createObjectNode().put("jobId","42");job.putObject("detailData").putArray("jobTags").add("😀Java");
            if(Set.of("valid","unknown","rounded","lower-bound","upper-bound").contains(mode))assertEquals(out,ai.match("😀Java",List.of(job),30000));
            else assertEquals(502,assertThrows(ResponseStatusException.class,()->ai.match("😀Java",List.of(job),30000)).getStatusCode().value());
            server.verify();
        }
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
