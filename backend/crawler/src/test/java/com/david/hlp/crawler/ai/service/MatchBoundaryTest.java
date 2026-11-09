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
        when(ai.detail(anyString())).thenReturn(json.createObjectNode());when(ai.model()).thenReturn("test");when(format.convertToJobAnalysisData(any())).thenAnswer(c->new JobAnalysisData());
        when(ai.match(anyString(),anyList(),anyInt())).thenAnswer(c->{var r=json.createObjectNode();r.putArray("items").addObject().put("jobId","1").put("status","scored").put("score",4.5);((ArrayNode)r.path("items")).addObject().put("jobId","2").put("status","scored").put("score",4.51);return r;});
        var result=new RequirementAnalyzer(mapper,ai,format,json).getUserSimilarity("Java",List.of("杭州"),"Java");
        assertEquals(2L,result.get(0).getId());assertEquals(5,result.get(1).getSimilarity());
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
    }

}
