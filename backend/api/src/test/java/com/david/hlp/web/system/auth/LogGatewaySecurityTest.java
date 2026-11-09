package com.david.hlp.web.system.auth;

import com.david.hlp.web.system.auth.controller.LogController;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LogGatewaySecurityTest {
    @Test void upstreamHealthAndExceptionsNeverLeakAndFailureIsNotSuccess() {
        var rest = mock(RestTemplate.class);
        var controller = new LogController();
        ReflectionTestUtils.setField(controller,"restTemplate",rest);
        ReflectionTestUtils.setField(controller,"sparkServiceUrl","http://synthetic.test");
        when(rest.getForEntity("http://synthetic.test/api/log/access/health",String.class))
                .thenReturn(ResponseEntity.ok("private upstream detail"));
        var health = controller.healthCheck();
        assertEquals(200,health.getStatusCode().value());
        assertFalse(health.getBody().toString().contains("private"));
        when(rest.getForEntity("http://synthetic.test/api/log/access/health",String.class))
                .thenThrow(new IllegalStateException("private database detail"));
        health = controller.healthCheck();
        assertEquals(503,health.getStatusCode().value());
        assertFalse(health.getBody().toString().contains("private"));
        when(rest.postForEntity("http://synthetic.test/api/log/access/analysis/trigger",null,String.class))
                .thenThrow(new IllegalStateException("private trigger detail"));
        var trigger = controller.triggerAnalysis();
        assertEquals(503,trigger.getStatusCode().value());
        assertFalse(trigger.getBody().contains("private"));
        doThrow(new org.springframework.web.client.HttpClientErrorException(
                org.springframework.http.HttpStatus.FORBIDDEN,"private upstream body"))
                .when(rest).postForEntity("http://synthetic.test/api/log/access/analysis/trigger",null,String.class);
        assertEquals(403,controller.triggerAnalysis().getStatusCode().value());
    }
    @Test void onlyExplicitNoContentMeansMissingSnapshot() {
        var rest = mock(RestTemplate.class);
        var controller = new LogController();
        ReflectionTestUtils.setField(controller, "restTemplate", rest);
        ReflectionTestUtils.setField(controller, "sparkServiceUrl", "http://synthetic.test");
        String url = "http://synthetic.test/api/log/access/dashboard/stats";
        when(rest.getForEntity(url, java.util.Map.class)).thenReturn(ResponseEntity.noContent().build());
        assertEquals(204, controller.getDashboardStats().getStatusCode().value());
        assertNull(controller.getDashboardStats().getBody());
        when(rest.getForEntity(url, java.util.Map.class)).thenReturn(ResponseEntity.ok(java.util.Map.of()));
        assertEquals(503, controller.getDashboardStats().getStatusCode().value());
        when(rest.getForEntity(url, java.util.Map.class)).thenReturn(ResponseEntity.ok().build());
        assertEquals(503, controller.getDashboardStats().getStatusCode().value());
        when(rest.getForEntity(url, java.util.Map.class)).thenThrow(new IllegalStateException("private"));
        assertEquals(503, controller.getDashboardStats().getStatusCode().value());
        doThrow(new org.springframework.web.client.HttpClientErrorException(
                org.springframework.http.HttpStatus.FORBIDDEN)).when(rest).getForEntity(url, java.util.Map.class);
        assertEquals(403, controller.getDashboardStats().getStatusCode().value());
    }
}
