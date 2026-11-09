package com.david.hlp.contracts;

import com.david.hlp.crawler.ai.service.AIExtractionOfJobRequirements;
import com.david.hlp.spark.utils.IpGeoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalCredentialsContractTest {
    private static final String SYNTHETIC = "synthetic-contract-only-not-a-real-credential";

    private ApplicationContextRunner runner(Map<String, Object> properties, Class<?> configuration) {
        return new ApplicationContextRunner().withInitializer(context -> {
            // Never pick up an operator's credentials from the test machine.
            var sources = context.getEnvironment().getPropertySources();
            sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            sources.addFirst(new MapPropertySource("synthetic-contract", properties));
        }).withUserConfiguration(configuration);
    }

    private Map<String, Object> optional(String name, String value) {
        Map<String, Object> result = new HashMap<>();
        if (value != null) result.put(name, value);
        return result;
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AIExtractionOfJobRequirements.class)
    static class JevConfig {
        @Bean RestTemplate restTemplate() { return new RestTemplate(); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean MockRestServiceServer mockServer(RestTemplate template) {
            // Installing this request factory makes unexpected HTTP fail locally.
            return MockRestServiceServer.bindTo(template).build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(IpGeoService.class)
    static class IpGeoConfig { }

    @Test
    void jevStartsWithoutTokenAndFailsClosedBeforeNetwork() {
        runner(Map.of(), JevConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThatThrownBy(() -> context.getBean(AIExtractionOfJobRequirements.class).analyze("42", "{}"))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .hasMessageContaining("503").hasMessageContaining("AI_UNCONFIGURED");
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void ipGeoStartsWithoutOnlineCredentialOrDatabase(String value) {
        try (MockedStatic<HttpClients> clients = mockStatic(HttpClients.class)) {
            runner(optional("IPGEOLOCATION_API_KEY", value), IpGeoConfig.class).run(context -> {
                assertThat(context).hasNotFailed();
                var service = context.getBean(IpGeoService.class);
                assertThat(service.getStatus()).isEqualTo("unconfigured");
                assertThat(service.getIpGeoInfo("8.8.8.8"))
                        .containsExactlyInAnyOrderEntriesOf(Map.of("country_name", "", "city", ""));
                clients.verifyNoInteractions();
            });
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void obsoleteIpGeoKeysDoNotEnableOnlineLookup(String value) {
        try (MockedStatic<HttpClients> clients = mockStatic(HttpClients.class)) {
            runner(Map.of("ipgeolocation.api.key", value, "IPGEOLOCATION_API_KEY", SYNTHETIC),
                    IpGeoConfig.class).run(context -> {
                assertThat(context).hasNotFailed();
                var service = context.getBean(IpGeoService.class);
                assertThat(service.getStatus()).isEqualTo("unconfigured");
                assertThat(service.getIpGeoInfo("8.8.8.8")).containsEntry("country_name", "")
                        .doesNotContainKeys("latitude", "longitude");
                clients.verifyNoInteractions();
            });
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"IPGEOLOCATION_API_KEY", "ipgeolocation.api.key"})
    void obsoleteIpGeoKeysKeepLocalAndInvalidAddressesUnknown(String property) {
        try (MockedStatic<HttpClients> clients = mockStatic(HttpClients.class)) {
            runner(Map.of(property, SYNTHETIC), IpGeoConfig.class).run(context -> {
                assertThat(context).hasNotFailed();
                var service = context.getBean(IpGeoService.class);
                for (String ip : new String[]{"127.0.0.1", "::1", null, "example.com"}) {
                    assertThat(service.getIpGeoInfo(ip))
                            .containsExactlyInAnyOrderEntriesOf(Map.of("country_name", "", "city", ""));
                }
                clients.verifyNoInteractions();
            });
        }
    }

    @Test
    void absentOrInvalidOfflineDatabaseNeverFallsBackToHttp(@TempDir Path directory) throws Exception {
        var invalid = Files.writeString(directory.resolve("invalid.mmdb"), "synthetic invalid database");
        for (Path path : new Path[]{directory.resolve("missing.mmdb"), invalid}) {
            try (MockedStatic<HttpClients> clients = mockStatic(HttpClients.class)) {
                runner(Map.of("geolocation.mmdb-path", path.toString()), IpGeoConfig.class).run(context -> {
                    assertThat(context).hasNotFailed();
                    var service = context.getBean(IpGeoService.class);
                    assertThat(service.getStatus()).isEqualTo("unavailable");
                    assertThat(service.getIpGeoInfo("8.8.8.8"))
                            .containsExactlyInAnyOrderEntriesOf(Map.of("country_name", "", "city", ""));
                    clients.verifyNoInteractions();
                });
            }
        }
    }
}
