package com.david.hlp.web.system.auth;

import com.david.hlp.web.system.auth.controller.RecruitmentGatewayController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RecruitmentGatewayTest {
    @Test void preservesUpstreamValidationStatus() {
        RestTemplate client = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
        server.expect(requestTo("http://analytics.invalid/api/v1/boss/basic/city-name-list"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("invalid query"));
        var gateway = new RecruitmentGatewayController(client);
        ReflectionTestUtils.setField(gateway, "sparkServiceUrl", "http://analytics.invalid");
        var response = gateway.read(new MockHttpServletRequest("GET", "/api/v1/boss/basic/city-name-list"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        server.verify();
    }
    @Test void forwardsEncodedQueryToFixedUpstreamWithoutClientCredentials() {
        RestTemplate client = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
        server.expect(requestTo("http://analytics.invalid/api/v1/boss/basic/position-name-list?cityName=A%26B"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess("[\"sample\"]", MediaType.APPLICATION_JSON));
        var gateway = new RecruitmentGatewayController(client);
        ReflectionTestUtils.setField(gateway, "sparkServiceUrl", "http://analytics.invalid");
        var request = new MockHttpServletRequest("GET", "/api/v1/boss/basic/position-name-list");
        request.setQueryString("cityName=A%26B");
        request.addHeader("Authorization", "Bearer synthetic");
        assertEquals("[\"sample\"]", gateway.read(request).getBody());
        server.verify();
    }
    @Test void stripsUpstreamFramingAndConnectionNominatedHeaders() {
        var client = new RestTemplate();
        var server = MockRestServiceServer.bindTo(client).build();
        var headers = new org.springframework.http.HttpHeaders();
        headers.add("Connection", "keep-alive, X-Internal");
        headers.add("Transfer-Encoding", "chunked");
        headers.add("Keep-Alive", "timeout=60");
        headers.add("X-Internal", "private");
        server.expect(requestTo("http://analytics.invalid/api/v1/boss/basic/city-name-list"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(headers));
        var gateway = new RecruitmentGatewayController(client);
        ReflectionTestUtils.setField(gateway, "sparkServiceUrl", "http://analytics.invalid");
        var response = gateway.read(new MockHttpServletRequest("GET", "/api/v1/boss/basic/city-name-list"));
        assertEquals(200, response.getStatusCode().value());
        assertEquals("[]", response.getBody());
        assertEquals(java.util.Set.of("Content-Type"), response.getHeaders().keySet());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        server.verify();
    }
}
