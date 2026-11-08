package com.david.hlp.web.system.auth;

import com.david.hlp.commons.security.DownstreamAuthFilter;
import com.david.hlp.commons.security.DownstreamAuthProperties;
import com.david.hlp.web.config.RestTemplateConfig;
import com.david.hlp.web.system.auth.controller.AuthController;
import com.david.hlp.web.system.auth.controller.LogController;
import com.david.hlp.web.system.auth.entity.auth.AuthUser;
import com.david.hlp.web.system.auth.filter.JwtAuthenticationFilter;
import com.david.hlp.web.system.auth.mapper.RoleMapper;
import com.david.hlp.web.system.auth.service.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class DownstreamIntegrationTest {
    @Mock RoleMapper roles;
    @Mock PermissionService permissions;
    @Mock UserDetailsService identities;
    @InjectMocks AuthController auth;

    @Test void actualIdentityCallbacksAuthenticateDownstreamAndPreserveFailureStatus() throws Exception {
        try (var mocks = MockitoAnnotations.openMocks(this)) {
            var mvc = MockMvcBuilders.standaloneSetup(auth).addFilters(
                    new JwtAuthenticationFilter(identities, roles, new ObjectMapper())).build();
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/auth/", exchange -> {
                try {
                    var result = mvc.perform(get(exchange.getRequestURI())
                            .header("Authorization", exchange.getRequestHeaders().getFirst("Authorization")))
                            .andReturn().getResponse();
                    byte[] body = result.getContentAsByteArray();
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(result.getStatus(), body.length);
                    exchange.getResponseBody().write(body);
                } catch (Exception e) {
                    throw new java.io.IOException(e);
                } finally {
                    SecurityContextHolder.clearContext();
                    exchange.close();
                }
            });
            server.start();
            try {
                String base = "http://127.0.0.1:" + server.getAddress().getPort();
                var properties = new DownstreamAuthProperties();
                properties.setVerificationUrl(base + "/api/auth/getUserPrivateInformation");
                properties.setPermissionCheckUrl(base + "/api/auth/hasPagePermission");
                properties.setPermissionProtectedPaths(List.of("/api/log/access/"));
                properties.setTriggerPath("/api/log/access/analysis/trigger");
                var filter = new DownstreamAuthFilter(properties);
                when(identities.loadUserByUsername("synthetic-token")).thenReturn(AuthUser.builder()
                        .userId(1L).authorities(List.of("custom:dashboard")).build());
                when(roles.permissionsForPath("/console/dashboard")).thenReturn(List.of("custom:dashboard"));
                when(permissions.getUserPermissions(1L)).thenReturn(List.of("custom:dashboard"));
                assertEquals(200, downstream(filter, "/api/log/access/dashboard/stats"));
                assertEquals(403, downstream(filter, properties.getTriggerPath()));
                var endpoint = new TriggerEndpoint();
                var downstreamMvc = MockMvcBuilders.standaloneSetup(endpoint).addFilters(filter).build();
                for (String path : List.of("/api/log/access/analysis/%74rigger",
                        "/api/log/access/analysis/trigger;x=1", "/api/%6cog/access/analysis/trigger",
                        "/api/log/access/analysis/../analysis/trigger")) {
                    downstreamMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post(URI.create(path)).header("Authorization", "Bearer synthetic-token"))
                            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
                }
                assertEquals(0, endpoint.calls);
                when(permissions.getUserPermissions(1L)).thenReturn(List.of("custom:dashboard", "logs.analysis.trigger"));
                assertEquals(200, downstream(filter, properties.getTriggerPath()));
                when(permissions.getUserPermissions(1L)).thenReturn(List.of());
                assertEquals(403, downstream(filter, "/api/log/access/dashboard/stats"));
                when(identities.loadUserByUsername("synthetic-token")).thenThrow(new UsernameNotFoundException("revoked"));
                assertEquals(401, downstream(filter, "/api/recruitment/read"));
                doThrow(new DataAccessResourceFailureException("offline")).when(identities).loadUserByUsername("synthetic-token");
                assertEquals(503, downstream(filter, "/api/recruitment/read"));
            } finally {
                server.stop(0);
            }
        }
    }

    private int downstream(DownstreamAuthFilter filter, String path) throws Exception {
        var request = new MockHttpServletRequest("GET", path);
        request.addHeader("Authorization", "Bearer synthetic-token");
        var response = new MockHttpServletResponse();
        boolean[] reached = {false};
        filter.doFilter(request, response, (a, b) -> reached[0] = true);
        assertEquals(response.getStatus() == 200, reached[0]);
        return response.getStatus();
    }

    @org.springframework.web.bind.annotation.RestController
    static class TriggerEndpoint {
        int calls;
        @org.springframework.web.bind.annotation.PostMapping("/api/log/access/analysis/trigger")
        String trigger() { calls++; return "reached"; }
    }

    @Test void logGatewayUsesTrustedTemplateWithoutForwardingToForeignDestinations() {
        var config = new RestTemplateConfig();
        ReflectionTestUtils.setField(config, "sparkServiceUrl", "http://analytics.test:8082");
        var rest = config.restTemplate();
        var server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo("http://analytics.test:8082/api/log/access/dashboard/stats"))
                .andExpect(header("Authorization", "Bearer synthetic-token"))
                .andRespond(withSuccess("{\"computed\":1}", org.springframework.http.MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://external.test/read")).andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess());
        server.expect(requestTo("http://analytics.test:8083/read")).andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess());
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer synthetic-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            var controller = new LogController();
            ReflectionTestUtils.setField(controller, "restTemplate", rest);
            ReflectionTestUtils.setField(controller, "sparkServiceUrl", "http://analytics.test:8082");
            assertEquals(200, controller.getDashboardStats().getStatusCode().value());
            rest.getForEntity(URI.create("http://external.test/read"), String.class);
            rest.getForEntity(URI.create("http://analytics.test:8083/read"), String.class);
            server.verify();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
