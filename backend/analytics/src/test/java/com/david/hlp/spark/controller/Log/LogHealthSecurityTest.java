package com.david.hlp.spark.controller.Log;

import com.david.hlp.commons.security.DownstreamAuthFilter;
import com.david.hlp.commons.security.DownstreamAuthProperties;
import com.david.hlp.spark.service.Log.LogServiceImp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LogHealthSecurityTest {
    @Test
    void shippedConfigurationRequiresIdentityForRedactedHealthWithoutDashboardGrant() throws Exception {
        var environment = new StandardEnvironment();
        for (var source : new YamlPropertySourceLoader().load("analytics", new ClassPathResource("application.yml"))) {
            environment.getPropertySources().addLast(source);
        }
        var properties = Binder.get(environment).bind("security.downstream-auth", DownstreamAuthProperties.class).get();
        assertTrue(properties.isEnabled());
        var filter = new DownstreamAuthFilter(properties);
        var rest = (RestTemplate) ReflectionTestUtils.getField(filter, "restTemplate");
        var identity = MockRestServiceServer.bindTo(rest).build();
        var logs = mock(LogServiceImp.class);
        var mvc = MockMvcBuilders.standaloneSetup(new LogController(logs)).addFilters(filter).build();

        mvc.perform(get("/api/log/access/health")).andExpect(status().isUnauthorized());
        identity.expect(requestTo(properties.getVerificationUrl()))
                .andRespond(withSuccess("{\"code\":200,\"data\":[]}", MediaType.APPLICATION_JSON));
        mvc.perform(get("/api/log/access/health").header("Authorization", "Bearer synthetic-token"))
                .andExpect(status().isOk()).andExpect(content().string("Access Log Service is running"));
        identity.verify();
        verifyNoInteractions(logs);
    }
}
