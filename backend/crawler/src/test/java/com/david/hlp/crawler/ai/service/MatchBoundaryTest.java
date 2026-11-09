package com.david.hlp.crawler.ai.service;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.david.hlp.crawler.ai.entity.*;
import com.david.hlp.crawler.ai.mapper.TJobDetailMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MatchBoundaryTest {
    final ObjectMapper json=new ObjectMapper();
    @Test void boundedRangeAndUnknownNeverBecomeGlobalTopTen() {
        var mapper=mock(TJobDetailMapper.class);var ai=mock(AIExtractionOfJobRequirements.class);var format=mock(FormatDataService.class);
        List<MiniJobDetail> jobs=new ArrayList<>();for(long i=1;i<=101;i++)jobs.add(MiniJobDetail.builder().id(i).detailData("{}").build());
        when(mapper.selectMiniJobDetail("杭州","Java",0L,101)).thenReturn(jobs);
        when(ai.fitsMatchBatch(anyString(),anyList())).thenReturn(true);
        when(ai.detail(anyString())).thenReturn(json.createObjectNode());when(ai.model()).thenReturn("test");
        when(ai.match(anyString(),anyList(),anyInt())).thenAnswer(call->{var r=json.createObjectNode();var items=r.putArray("items");
            for(ObjectNode job:call.<List<ObjectNode>>getArgument(1))items.addObject().put("jobId",job.path("jobId").asText()).put("status","insufficient_evidence").putNull("score");return r;});
        var analyzer=new RequirementAnalyzer(mapper,ai,format,json);
        JsonNode out=analyzer.matches("技能 Java",List.of("杭州"),"Java");assertFalse(out.path("complete").asBoolean());assertEquals(100,out.path("consideredCount").asInt());
        assertEquals(100,out.path("unrankable").size());assertTrue(out.path("items").isEmpty());assertTrue(out.path("scope").path("truncated").asBoolean());
        assertEquals(422,assertThrows(ResponseStatusException.class,()->analyzer.getUserSimilarity("技能 Java",List.of("杭州"),"Java")).getStatusCode().value());
    }
    @Test void decimalRankingPrecedesLegacyHalfUpRounding() {
        var mapper=mock(TJobDetailMapper.class);var ai=mock(AIExtractionOfJobRequirements.class);var format=mock(FormatDataService.class);
        when(mapper.selectMiniJobDetail(anyString(),anyString(),anyLong(),anyInt())).thenReturn(List.of(MiniJobDetail.builder().id(1L).detailData("{}").build(),MiniJobDetail.builder().id(2L).detailData("{}").build()));
        when(ai.fitsMatchBatch(anyString(),anyList())).thenReturn(true);
        when(ai.detail(anyString())).thenReturn(json.createObjectNode());when(ai.model()).thenReturn("test");when(format.convertToJobAnalysisData(any())).thenAnswer(c->new JobAnalysisData());
        when(ai.match(anyString(),anyList(),anyInt())).thenAnswer(c->{var r=json.createObjectNode();r.putArray("items").addObject().put("jobId","1").put("status","scored").put("score",4.5);((ArrayNode)r.path("items")).addObject().put("jobId","2").put("status","scored").put("score",4.51);return r;});
        var result=new RequirementAnalyzer(mapper,ai,format,json).getUserSimilarity("Java",List.of("杭州"),"Java");
        assertEquals(2L,result.get(0).getId());assertEquals(5,result.get(1).getSimilarity());
    }
    @Test void laterBatchFailurePreservesScoresAndMarksRemainingJobsFailed() {
        for(int status:List.of(502,503,504)) {
            var mapper=mock(TJobDetailMapper.class);var ai=mock(AIExtractionOfJobRequirements.class);
            var jobs=new ArrayList<MiniJobDetail>();
            for(long id=1;id<=21;id++)jobs.add(MiniJobDetail.builder().id(id).detailData("{}").build());
            when(mapper.selectMiniJobDetail(anyString(),anyString(),anyLong(),anyInt())).thenReturn(jobs);
            when(ai.fitsMatchBatch(anyString(),anyList())).thenReturn(true);
            when(ai.detail(anyString())).thenReturn(json.createObjectNode());when(ai.model()).thenReturn("test");
            when(ai.match(anyString(),anyList(),anyInt())).thenAnswer(c->{
                List<ObjectNode> batch=c.getArgument(1);
                if(!batch.get(0).path("jobId").asText().equals("1"))throw AIExtractionOfJobRequirements.error(status,"synthetic");
                var response=json.createObjectNode();var items=response.putArray("items");
                for(var job:batch)items.addObject().put("jobId",job.path("jobId").asText()).put("status","scored").put("score",8.75);
                return response;
            });
            var analyzer=new RequirementAnalyzer(mapper,ai,new FormatDataService(json),json);
            JsonNode result=analyzer.matches("Java",List.of("杭州"),"Java");
            assertEquals(10,result.path("items").size());assertEquals(11,result.path("unrankable").size());
            assertEquals(21,result.path("consideredCount").asInt());assertFalse(result.path("complete").asBoolean());
            assertEquals("11",result.at("/unrankable/0/jobId").asText());
            assertEquals("21",result.at("/unrankable/10/jobId").asText());
            for(JsonNode failed:result.path("unrankable")){assertEquals("failed",failed.path("status").asText());assertTrue(failed.path("score").isNull());}
            verify(ai,times(2)).match(anyString(),anyList(),anyInt());
            assertEquals(503,assertThrows(ResponseStatusException.class,()->analyzer.getUserSimilarity("Java",List.of("杭州"),"Java")).getStatusCode().value());
        }
    }
    @Test void byteBoundedBatchesPreserveJobsAndIsolateAnOversizedRecord() {
        for(String mode:List.of("none","chinese","emoji")) {
            boolean oversized=!mode.equals("none");
            var mapper=mock(TJobDetailMapper.class);
            var ai=spy(new AIExtractionOfJobRequirements(json,"http://synthetic.test","synthetic","typesafe/jev-1.14.0"));
            String resume="合成简历".repeat(300);
            var jobs=new ArrayList<MiniJobDetail>();
            for(long id=1;id<=10;id++) {
                var detail=json.createObjectNode();
                String text=mode.equals("emoji")&&id==5?"😀".repeat(4100):"汉".repeat(oversized&&id==5?17000:1750)+"\"\\😀";
                detail.putObject("jobDescription").put("requirements",text);
                jobs.add(MiniJobDetail.builder().id(id).detailData(detail.toString()).build());
            }
            when(mapper.selectMiniJobDetail(anyString(),anyString(),anyLong(),anyInt())).thenReturn(jobs);
            var seen=new ArrayList<String>();var sizes=new ArrayList<Integer>();
            doAnswer(call->{
                List<ObjectNode> batch=call.getArgument(1);
                var body=json.createObjectNode().put("resume",resume).put("timeoutMs",call.<Integer>getArgument(2));
                body.set("jobs",json.valueToTree(batch));
                assertTrue(json.writeValueAsBytes(body).length<=48000);
                assertFalse(batch.isEmpty());sizes.add(batch.size());
                var response=json.createObjectNode();var items=response.putArray("items");
                for(var job:batch){String id=job.path("jobId").asText();seen.add(id);items.addObject().put("jobId",id).put("status","scored").put("score",8.75);}
                return response;
            }).when(ai).match(anyString(),anyList(),anyInt());
            var analyzer=new RequirementAnalyzer(mapper,ai,new FormatDataService(json),json);
            JsonNode result=analyzer.matches(resume,List.of("杭州"),"Java");
            var expected=jobs.stream().filter(job->!oversized||job.getId()!=5).map(job->job.getId().toString()).toList();
            assertEquals(expected,seen);assertTrue(sizes.size()>1);
            assertEquals(expected.size(),result.path("items").size());assertEquals(!oversized,result.path("complete").asBoolean());
            assertEquals(oversized?1:0,result.path("unrankable").size());
            if(oversized)assertEquals("5",result.at("/unrankable/0/jobId").asText());
            assertEquals(422,assertThrows(ResponseStatusException.class,()->analyzer.matches("x".repeat(12001),List.of("杭州"),"Java")).getStatusCode().value());
        }
    }
    @Test void identityIsNotSelfAssertedAndTextIsMinimized(){
        var resolver=new AuthorizedResume("http://synthetic.test");var request=new MockHttpServletRequest();var input=new SimilarityRequest();input.setResume("姓名：张三\n邮箱：test@example.test\nJava开发");
        assertEquals(401,assertThrows(ResponseStatusException.class,()->resolver.resolve(input,request)).getStatusCode().value());
        request.setAttribute("boss.verified",true);assertEquals("Java开发",resolver.resolve(input,request));
        input.setUserId("victim");assertEquals(422,assertThrows(ResponseStatusException.class,()->resolver.resolve(input,request)).getStatusCode().value());
        input.setUserId(null);input.setResumeId("another-user-id");assertEquals(422,assertThrows(ResponseStatusException.class,()->resolver.resolve(input,request)).getStatusCode().value());
    }
    @Test void resumeOwnershipIsCheckedByPrimaryApiBeforeExtractingOnlyContent() {
        var resolver=new AuthorizedResume("http://synthetic.test");
        var http=(org.springframework.web.client.RestTemplate)org.springframework.test.util.ReflectionTestUtils.getField(resolver,"http");
        var server=org.springframework.test.web.client.MockRestServiceServer.bindTo(http).build();
        var request=new MockHttpServletRequest();request.setAttribute("boss.verified",true);request.addHeader("Authorization","Bearer synthetic-user");
        var input=new SimilarityRequest();input.setResumeId("owned");
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://synthetic.test/api/resume/owned"))
            .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.header("Authorization","Bearer synthetic-user"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("{\"code\":200,\"data\":{\"id\":\"owned\",\"content\":\"Java\",\"userId\":9,\"author_email\":\"private@example.test\"}}",org.springframework.http.MediaType.APPLICATION_JSON));
        assertEquals("Java",resolver.resolve(input,request));server.verify();server.reset();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://synthetic.test/api/resume/owned"))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("{\"code\":404,\"data\":null}",org.springframework.http.MediaType.APPLICATION_JSON));
        assertEquals(403,assertThrows(ResponseStatusException.class,()->resolver.resolve(input,request)).getStatusCode().value());server.verify();
        server.reset();
        for(int status:new int[]{401,403,404,500,503}){
            server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("http://synthetic.test/api/resume/owned"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.valueOf(status))
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body("{\"error\":\"PRIVATE_UPSTREAM_DETAIL\"}"));
            var error=assertThrows(ResponseStatusException.class,()->resolver.resolve(input,request));
            assertEquals(status==401?401:status==403||status==404?403:503,error.getStatusCode().value());
            assertFalse(error.getReason().contains("PRIVATE_UPSTREAM_DETAIL"));
            server.verify();server.reset();
        }
    }

}
