package com.david.hlp.crawler.ai.service;

import com.david.hlp.crawler.ai.controller.AIRequirementsController;
import com.david.hlp.crawler.ai.entity.SimilarityRequest;
import com.david.hlp.crawler.ai.mapper.TJobDetailMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumePrivacyTest {
    private static void disabled(org.junit.jupiter.api.function.Executable action) {
        var error = assertThrows(ResponseStatusException.class, action);
        assertEquals(503, error.getStatusCode().value());
        assertEquals("AI_RESUME_MATCHING_DISABLED", error.getReason());
        assertFalse(error.getMessage().contains("SYNTHETIC-ID"));
    }

    @Test void directClientAndLegacyHelperNeverSendResume() {
        var json = new ObjectMapper();
        var http = new RestTemplate();
        var server = MockRestServiceServer.bindTo(http).build();
        var ai = new AIExtractionOfJobRequirements(json, "http://synthetic.test", "synthetic", "model", http);
        for (String resume : List.of("", "Inline Synthetic Person SYNTHETIC-ID", "**姓名** 合成😀\n+1 202 555 0100")) {
            disabled(() -> ai.match(resume, List.of(), 30000));
            disabled(() -> ai.getUserSimilarity("{}", resume));
        }
        server.verify();
    }

    @Test void analyzerStopsBeforeDatabaseOrTransportAndDoesNotReturnPartialSuccess() {
        var jobs = mock(TJobDetailMapper.class);
        var ai = mock(AIExtractionOfJobRequirements.class);
        var format = mock(FormatDataService.class);
        var analyzer = new RequirementAnalyzer(jobs, ai, format, new ObjectMapper());
        disabled(() -> analyzer.matches("SYNTHETIC-ID", List.of("杭州"), "Java"));
        disabled(() -> analyzer.getUserSimilarity("SYNTHETIC-ID", List.of("杭州"), "Java"));
        verifyNoInteractions(jobs, ai, format);
    }

    @Test void bothPublicRoutesBlockInlineAndStoredResumesBeforeFetch() {
        var resolver = new AuthorizedResume("http://synthetic.test");
        var http = (RestTemplate)org.springframework.test.util.ReflectionTestUtils.getField(resolver, "http");
        var server = MockRestServiceServer.bindTo(http).build();
        var analyzer = mock(RequirementAnalyzer.class);
        var controller = new AIRequirementsController(analyzer, resolver);
        var request = new MockHttpServletRequest();
        var input = new SimilarityRequest();
        input.setResume("SYNTHETIC-ID");
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> controller.matches(input, request)).getStatusCode().value());
        request.setAttribute("boss.verified", true);
        disabled(() -> controller.matches(input, request));
        disabled(() -> controller.legacy(input, request));
        input.setResume(null); input.setResumeId("synthetic-stored-resume");
        disabled(() -> controller.matches(input, request));
        disabled(() -> controller.legacy(input, request));
        verifyNoInteractions(analyzer);
        server.verify();
    }
}
