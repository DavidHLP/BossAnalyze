package com.david.hlp.commons.security;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DownstreamAuthFilterTest {

    private static final String VERIFY_URL = "http://identity.test/api/auth/getUserPrivateInformation";
    private static final String PERMISSION_URL = "http://identity.test/api/auth/hasPagePermission";
    private static final String PERMISSION_CHECK = PERMISSION_URL + "?path=/console/dashboard";
    private static final String GRANTS = "{\"code\":200,\"data\":[\"dashboard:view\",\"logs\"]}";
    private static final String ALLOWED = "{\"code\":200,\"data\":true}";
    private static final String DENIED = "{\"code\":200,\"data\":false}";

    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicBoolean passed = new AtomicBoolean();
    private final FilterChain chain = (request, response) -> passed.set(true);

    private DownstreamAuthProperties properties;
    private MockRestServiceServer server;
    private DownstreamAuthFilter filter;

    @BeforeEach
    void setUp() {
        passed.set(false);
        properties = new DownstreamAuthProperties();
        properties.setVerificationUrl(VERIFY_URL);
        properties.setPermissionCheckUrl(PERMISSION_URL);
        properties.setPublicPaths(new ArrayList<>(List.of("/api/log/access/health")));
        properties.setPermissionProtectedPaths(new ArrayList<>(List.of("/api/log/access")));
        properties.setTriggerPath("/api/log/access/analysis/trigger");
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        filter = new DownstreamAuthFilter(properties, restTemplate, mapper);
    }

    @Test
    void anonymousRequestIsRejectedWithoutCallingTheIdentityService() throws Exception {
        MockHttpServletResponse response = run("GET", "/api/v1/boss/user/city", null);
        assertEquals(401, response.getStatus());
        assertEquals(401, body(response).path("code").asInt());
        assertFalse(passed.get());
    }

    @Test
    void authenticatedRequestReachesTheChainWithItsBearerToken() throws Exception {
        server.expect(requestTo(VERIFY_URL))
                .andExpect(header("Authorization", "Bearer live-token"))
                .andRespond(withSuccess(GRANTS, MediaType.APPLICATION_JSON));
        MockHttpServletResponse response = run("GET", "/api/v1/boss/user/city", "Bearer live-token");
        assertEquals(200, response.getStatus());
        assertTrue(passed.get());
        server.verify();
    }

    @Test
    void revokedBearerAndIdentityOutageFailClosedWithDistinctStatuses() throws Exception {
        server.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("{\"code\":401,\"data\":null}", MediaType.APPLICATION_JSON));
        assertEquals(401, run("GET", "/api/resume", "Bearer revoked").getStatus());

        server.reset();
        passed.set(false);
        server.expect(requestTo(VERIFY_URL)).andRespond(withServerError());
        MockHttpServletResponse outage = run("GET", "/api/resume", "Bearer live-token");
        assertEquals(503, outage.getStatus());
        assertFalse(passed.get());
    }

    @Test
    void dashboardAccessUsesTheApisLivePathPermissionInsteadOfThePathItself() throws Exception {
        server.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("{\"code\":200,\"data\":[\"logs\"]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(PERMISSION_CHECK)).andRespond(withSuccess(DENIED, MediaType.APPLICATION_JSON));
        assertEquals(403, run("GET", "/api/log/access/statistics", "Bearer live-token").getStatus());
        assertFalse(passed.get());

        server.reset();
        passed.set(false);
        server.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess(GRANTS, MediaType.APPLICATION_JSON));
        server.expect(requestTo(PERMISSION_CHECK)).andRespond(withSuccess(ALLOWED, MediaType.APPLICATION_JSON));
        assertEquals(200, run("GET", "/api/log/access/statistics", "Bearer live-token").getStatus());
        assertTrue(passed.get());
    }

    @Test
    void analysisTriggerAlsoRequiresTheDedicatedTriggerGrant() throws Exception {
        server.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess(GRANTS, MediaType.APPLICATION_JSON));
        server.expect(requestTo(PERMISSION_CHECK)).andRespond(withSuccess(ALLOWED, MediaType.APPLICATION_JSON));
        assertEquals(403,
                run("POST", "/api/log/access/analysis/trigger", "Bearer live-token").getStatus());
        assertFalse(passed.get());

        server.reset();
        passed.set(false);
        server.expect(requestTo(VERIFY_URL)).andRespond(withSuccess(
                "{\"code\":200,\"data\":[\"dashboard:view\",\"logs.analysis.trigger\"]}",
                MediaType.APPLICATION_JSON));
        server.expect(requestTo(PERMISSION_CHECK)).andRespond(withSuccess(ALLOWED, MediaType.APPLICATION_JSON));
        assertEquals(200,
                run("POST", "/api/log/access/analysis/trigger", "Bearer live-token").getStatus());
        assertTrue(passed.get());
    }

    @Test
    void publicPathsAreServedWithoutContactingTheIdentityService() throws Exception {
        assertEquals(200, run("GET", "/api/log/access/health", null).getStatus());
        assertEquals(200, run("OPTIONS", "/api/log/access/statistics", null).getStatus());
        assertTrue(passed.get());
    }

    @Test
    void noncanonicalRoutesAreRejectedBeforeIdentityAndPublicPrefixChecks() throws Exception {
        for (String path : List.of("/api/%6cog/access/analysis/trigger", "/api/log/access/analysis/%74rigger",
                "/api/log/access/analysis/trigger;x=1", "/api/log/access/health/../analysis/trigger",
                "/api/log/access//analysis/trigger", "/api/log/access/analysis\\trigger")) {
            assertEquals(400, run("POST", path, "Bearer live-token").getStatus());
            assertFalse(passed.get());
        }
        assertEquals(401, run("GET", "/api/log/access/health-extra", null).getStatus());
        server.verify();
    }

    @Test
    void redactedHealthRequiresIdentityButNotDashboardPermission() throws Exception {
        properties.setPublicPaths(List.of("/actuator", "/error"));
        assertEquals(401, run("GET", "/api/log/access/health", null).getStatus());
        assertFalse(passed.get());
        server.expect(requestTo(VERIFY_URL))
                .andExpect(header("Authorization", "Bearer live-token"))
                .andRespond(withSuccess("{\"code\":200,\"data\":[]}", MediaType.APPLICATION_JSON));
        assertEquals(200, run("GET", "/api/log/access/health", "Bearer live-token").getStatus());
        assertTrue(passed.get());
        server.verify();
    }

    private MockHttpServletResponse run(String method, String path, String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return mapper.readTree(response.getContentAsString());
    }
}
