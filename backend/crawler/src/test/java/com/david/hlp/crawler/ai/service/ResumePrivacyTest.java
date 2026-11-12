package com.david.hlp.crawler.ai.service;

import com.david.hlp.crawler.ai.controller.AIRequirementsController;
import com.david.hlp.crawler.ai.entity.SimilarityRequest;
import com.david.hlp.crawler.ai.mapper.TJobDetailMapper;
import com.david.hlp.commons.security.DownstreamAuthFilter;
import com.david.hlp.commons.security.DownstreamAuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumePrivacyTest {
    private static final List<String> MATCH_ROUTES = List.of(
            "/api/v1/ai/get-user-similarity", "/api/v2/ai/matches");

    @Test void authenticatedMalformedBodyIsRejectedBeforeReading() throws Exception {
        for (String path : MATCH_ROUTES) {
            var boundary = new HttpBoundary();
            boundary.identity.expect(requestTo("http://identity.test/verify"))
                    .andExpect(header("Authorization", "Bearer synthetic-only"))
                    .andRespond(withSuccess("{\"code\":200,\"data\":[]}", MediaType.APPLICATION_JSON));
            var request = new TrackedBodyRequest(path, "{", false);
            request.addHeader("Authorization", "Bearer synthetic-only");
            boundary.mvc.perform(context -> request)
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(result -> assertEquals("AI_RESUME_MATCHING_DISABLED",
                            ((ResponseStatusException)result.getResolvedException()).getReason()));
            assertFalse(request.bodyAccessed);
            boundary.verify();
        }
    }

    @Test void authenticatedMatchingNeverOpensInlineStoredEmptyOrUnreadableBodies() throws Exception {
        for (String path : MATCH_ROUTES) {
            for (String body : List.of("", "{\"resume\":\"SYNTHETIC-ID\"}",
                    "{\"resumeId\":\"synthetic-stored-resume\"}")) {
                for (boolean unreadable : List.of(false, true)) {
                    var boundary = new HttpBoundary();
                    boundary.identity.expect(requestTo("http://identity.test/verify"))
                            .andRespond(withSuccess("{\"code\":200,\"data\":[]}", MediaType.APPLICATION_JSON));
                    var request = new TrackedBodyRequest(path, body, unreadable);
                    request.addHeader("Authorization", "Bearer synthetic-only");
                    boundary.mvc.perform(context -> request)
                            .andExpect(status().isServiceUnavailable())
                            .andExpect(result -> assertEquals("AI_RESUME_MATCHING_DISABLED",
                                    ((ResponseStatusException)result.getResolvedException()).getReason()));
                    assertFalse(request.bodyAccessed);
                    boundary.verify();
                }
            }
        }
    }

    @Test void anonymousAndRevokedIdentityTakePrecedenceWithoutOpeningTheBody() throws Exception {
        for (String path : MATCH_ROUTES) {
            for (boolean revoked : List.of(false, true)) {
                for (boolean unreadable : List.of(false, true)) {
                    var boundary = new HttpBoundary();
                    var request = new TrackedBodyRequest(path, "{", unreadable);
                    if (revoked) {
                        request.addHeader("Authorization", "Bearer synthetic-revoked");
                        boundary.identity.expect(requestTo("http://identity.test/verify"))
                                .andRespond(withSuccess("{\"code\":401,\"data\":null}", MediaType.APPLICATION_JSON));
                    }
                    boundary.mvc.perform(context -> request)
                            .andExpect(status().isUnauthorized())
                            .andExpect(jsonPath("$.code").value(401));
                    assertFalse(request.bodyAccessed);
                    boundary.verify();
                }
            }
        }
    }

    private static final class HttpBoundary {
        final TJobDetailMapper jobs = mock(TJobDetailMapper.class);
        final MockRestServiceServer identity;
        final MockRestServiceServer resumes;
        final MockRestServiceServer model;
        final org.springframework.test.web.servlet.MockMvc mvc;

        HttpBoundary() {
            var json = new ObjectMapper();
            var properties = new DownstreamAuthProperties();
            properties.setVerificationUrl("http://identity.test/verify");
            var filter = new DownstreamAuthFilter(properties);
            identity = MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(filter, "restTemplate")).build();
            var resolver = new AuthorizedResume("http://synthetic.test");
            resumes = MockRestServiceServer.bindTo((RestTemplate)ReflectionTestUtils.getField(resolver, "http")).build();
            var http = new RestTemplate();
            model = MockRestServiceServer.bindTo(http).build();
            var ai = new AIExtractionOfJobRequirements(json, "http://synthetic.test", "synthetic-only", "model", http);
            var analyzer = new RequirementAnalyzer(jobs, ai, new FormatDataService(json), json);
            mvc = MockMvcBuilders.standaloneSetup(new AIRequirementsController(analyzer, resolver))
                    .addFilters(filter).build();
        }

        void verify() {
            identity.verify();
            resumes.verify();
            model.verify();
            verifyNoInteractions(jobs);
        }
    }

    private static final class TrackedBodyRequest extends MockHttpServletRequest {
        boolean bodyAccessed;
        final boolean unreadable;

        TrackedBodyRequest(String path, String body, boolean unreadable) {
            super("POST", path);
            setContentType(MediaType.APPLICATION_JSON_VALUE);
            setContent(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            this.unreadable = unreadable;
        }

        @Override public ServletInputStream getInputStream() {
            bodyAccessed = true;
            if (unreadable) throw new java.io.UncheckedIOException(new IOException("synthetic unreadable body"));
            return super.getInputStream();
        }

        @Override public BufferedReader getReader() throws java.io.UnsupportedEncodingException {
            bodyAccessed = true;
            if (unreadable) throw new java.io.UncheckedIOException(new IOException("synthetic unreadable body"));
            return super.getReader();
        }
    }

    private static void disabled(org.junit.jupiter.api.function.Executable action) {
        var error = assertThrows(ResponseStatusException.class, action);
        assertEquals(503, error.getStatusCode().value());
        assertEquals("AI_RESUME_MATCHING_DISABLED", error.getReason());
        assertFalse(error.getMessage().contains("SYNTHETIC-ID"));
    }

    @Test void directClientKeepsJobAnalysisAvailableWhileMatchingNeverSendsResume() {
        var json = new ObjectMapper();
        var http = new RestTemplate();
        var server = MockRestServiceServer.bindTo(http).build();
        var ai = new AIExtractionOfJobRequirements(json, "http://synthetic.test", "synthetic", "model", http);
        for (String resume : List.of("", "Inline Synthetic Person SYNTHETIC-ID", "**姓名** 合成😀\n+1 202 555 0100")) {
            disabled(() -> ai.match(resume, List.of(), 30000));
            disabled(() -> ai.getUserSimilarity("{}", resume));
        }
        server.expect(requestTo("http://synthetic.test/internal/v1/job-analysis"))
                .andExpect(header("Authorization", "Bearer synthetic"))
                .andRespond(withSuccess("""
                        {"jobId":"42","status":"no_evidence","requirements":[],"benefits":[],
                         "coreRequirements":[],"model":"model","pipelineVersion":"jev-1",
                         "policyVersion":"jev-policy-1","requestId":"synthetic-only",
                         "usage":{"inputTokens":null,"outputTokens":null}}
                        """, MediaType.APPLICATION_JSON));
        assertEquals("no_evidence", ai.analyze("42", "{}").path("status").asText());
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
