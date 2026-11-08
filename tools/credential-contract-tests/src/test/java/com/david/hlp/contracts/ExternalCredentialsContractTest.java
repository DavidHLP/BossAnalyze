package com.david.hlp.contracts;

import com.david.hlp.crawler.ai.service.AIExtractionOfJobRequirementsAPI;
import com.david.hlp.spark.utils.IpGeoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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
    @Import(AIExtractionOfJobRequirementsAPI.class)
    static class OpenRouterConfig {
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
    void contextAllowsUnusedOpenRouterWithoutCredential() {
        runner(Map.of(), OpenRouterConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeanFactory().containsSingleton(
                    AIExtractionOfJobRequirementsAPI.class.getName())).isFalse();
            context.getBean(MockRestServiceServer.class).verify();
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void openRouterRejectsMissingOrBlankEnvironmentValueBeforeUse(String value) {
        runner(optional("OPENROUTER_API_KEY", value), OpenRouterConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThatThrownBy(() -> context.getBean(AIExtractionOfJobRequirementsAPI.class))
                    .isInstanceOf(BeanCreationException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("OpenRouter API key is required; configure OPENROUTER_API_KEY or openrouter.api.key");
            context.getBean(MockRestServiceServer.class).verify();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void explicitBlankOpenRouterPropertyDoesNotFallBackToEnvironment(String value) {
        runner(Map.of("openrouter.api.key", value, "OPENROUTER_API_KEY", SYNTHETIC),
                OpenRouterConfig.class).run(context -> {
            assertThatThrownBy(() -> context.getBean(AIExtractionOfJobRequirementsAPI.class))
                    .hasRootCauseInstanceOf(IllegalStateException.class);
            context.getBean(MockRestServiceServer.class).verify();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPENROUTER_API_KEY", "openrouter.api.key"})
    void configuredOpenRouterUsesSyntheticHeaderAndParsesStubResponse(String property) {
        runner(Map.of(property, SYNTHETIC, "openrouter.api.url", "https://contract.invalid/chat"),
                OpenRouterConfig.class).run(context -> {
            var server = context.getBean(MockRestServiceServer.class);
            server.expect(requestTo("https://contract.invalid/chat"))
                    .andExpect(header("Authorization", "Bearer " + SYNTHETIC))
                    .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"[\\\"Java\\\"]\"}}]}",
                            MediaType.APPLICATION_JSON));
            assertThat(context.getBean(AIExtractionOfJobRequirementsAPI.class)
                    .extractJobRequirements("{}")).containsExactly("Java");
            server.verify();
        });
    }

    @Test
    void explicitOpenRouterPropertyWinsOverEnvironment() {
        runner(Map.of("openrouter.api.key", SYNTHETIC, "OPENROUTER_API_KEY", "unused-synthetic",
                        "openrouter.api.url", "https://contract.invalid/chat"), OpenRouterConfig.class)
                .run(context -> {
                    var server = context.getBean(MockRestServiceServer.class);
                    server.expect(requestTo("https://contract.invalid/chat"))
                            .andExpect(header("Authorization", "Bearer " + SYNTHETIC))
                            .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"[]\"}}]}",
                                    MediaType.APPLICATION_JSON));
                    context.getBean(AIExtractionOfJobRequirementsAPI.class).extractJobRequirements("{}");
                    server.verify();
                });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void ipGeoRejectsMissingOrBlankEnvironmentValueDuringInitialization(String value) {
        try (MockedStatic<HttpClients> clients = mockStatic(HttpClients.class)) {
            runner(optional("IPGEOLOCATION_API_KEY", value), IpGeoConfig.class).run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                        .hasRootCauseMessage("IP geolocation API key is required; configure IPGEOLOCATION_API_KEY or ipgeolocation.api.key");
                clients.verifyNoInteractions();
            });
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void explicitBlankIpGeoPropertyDoesNotFallBackToEnvironment(String value) {
        runner(Map.of("ipgeolocation.api.key", value, "IPGEOLOCATION_API_KEY", SYNTHETIC),
                IpGeoConfig.class).run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"IPGEOLOCATION_API_KEY", "ipgeolocation.api.key"})
    void configuredIpGeoHandlesLocalAddressesWithoutAnyHttpClient(String property) {
        try (MockedStatic<HttpClients> clients = mockStatic(HttpClients.class)) {
            runner(Map.of(property, SYNTHETIC), IpGeoConfig.class).run(context -> {
                assertThat(context).hasNotFailed();
                var service = context.getBean(IpGeoService.class);
                assertThat(service.getIpGeoInfo("127.0.0.1")).containsEntry("country_name", "本地网络");
                assertThat(service.getIpGeoInfo("::1")).containsEntry("country_name", "本地网络");
                assertThat(service.getIpGeoInfo(null)).containsEntry("country_name", "");
                clients.verifyNoInteractions();
            });
        }
    }

    @Test
    void ipGeoUsesConfiguredPropertyAndParsesOnlyMockedHttp() throws Exception {
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        var response = new BasicClassicHttpResponse(200);
        response.setEntity(new StringEntity("{\"country_name\":\"Test Country\",\"city\":\"Test City\"}",
                ContentType.APPLICATION_JSON));
        when(client.executeOpen(isNull(), any(ClassicHttpRequest.class), isNull())).thenReturn(response);
        try (MockedStatic<HttpClients> clients = mockStatic(HttpClients.class)) {
            clients.when(HttpClients::createDefault).thenReturn(client);
            runner(Map.of("ipgeolocation.api.key", SYNTHETIC, "IPGEOLOCATION_API_KEY", "unused-synthetic"),
                    IpGeoConfig.class).run(context -> {
                assertThat(context.getBean(IpGeoService.class).getIpGeoInfo("203.0.113.1"))
                        .containsEntry("country_name", "Test Country").containsEntry("city", "Test City");
            });
            var request = ArgumentCaptor.forClass(ClassicHttpRequest.class);
            verify(client).executeOpen(isNull(), request.capture(), isNull());
            assertThat(request.getValue().getUri().getRawQuery()).contains("apiKey=" + SYNTHETIC);
            assertThat(request.getValue().getFirstHeader("Accept").getValue()).isEqualTo("application/json");
            verify(client).close();
        }
    }
}
