package com.david.hlp.contracts;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.yaml.snakeyaml.Yaml;
import static org.junit.jupiter.api.Assertions.*;

class RedisPlaceholderContractTest {
    @SuppressWarnings("unchecked")
    private String redisPassword(String module) throws Exception {
        return redisExpression(module, false);
    }

    @SuppressWarnings("unchecked")
    private String redisExpression(String module, boolean localDev) throws Exception {
        Path yaml = Path.of(System.getProperty("boss.source.root"), "JDK17SERVE", module,
                "src/main/resources", localDev ? "application-local-dev.yml" : "application.yml");
        try (var reader = Files.newBufferedReader(yaml)) {
            Map<String, Object> document = new Yaml().load(reader);
            var spring = (Map<String, Object>) document.get("spring");
            var data = (Map<String, Object>) spring.get("data");
            var redis = (Map<String, Object>) data.get("redis");
            Object password = redis.get("password");
            // Do not print actual configuration values in an assertion failure.
            String expected = localDev ? "${BOSSANALYZE_REDIS_PASSWORD:}" : "${BOSSANALYZE_REDIS_PASSWORD}";
            assertTrue(expected.equals(password), "Redis profile expression changed; do not print its value");
            return (String) password;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring", "spark"})
    void missingRedisVariableHasNoTrackedFallback(String module) throws Exception {
        String expression = redisPassword(module);
        var resolver = new PropertySourcesPropertyResolver(new MutablePropertySources());
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveRequiredPlaceholders(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring", "spark"})
    void configuredSyntheticRedisValueResolvesWithoutConnecting(String module) throws Exception {
        var sources = new MutablePropertySources();
        sources.addFirst(new MapPropertySource("synthetic", Map.of("BOSSANALYZE_REDIS_PASSWORD", "synthetic-only")));
        var resolver = new PropertySourcesPropertyResolver(sources);
        assertEquals("synthetic-only", resolver.resolveRequiredPlaceholders(redisPassword(module)));
    }

    private String bootBind(String expression, String suppliedValue) {
        var sources = new MutablePropertySources();
        if (suppliedValue != null) {
            sources.addFirst(new MapPropertySource("synthetic", Map.of("BOSSANALYZE_REDIS_PASSWORD", suppliedValue)));
        }
        var config = new MapConfigurationPropertySource(Map.of("spring.data.redis.password", expression));
        return new Binder(java.util.List.of(config), new PropertySourcesPlaceholdersResolver(sources))
                .bind("spring.data.redis.password", String.class).get();
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring", "spark"})
    void bootBinderKeepsMissingDefaultVariableAsLiteralNotFailFast(String module) throws Exception {
        assertEquals("${BOSSANALYZE_REDIS_PASSWORD}", bootBind(redisExpression(module, false), null));
    }

    @ParameterizedTest
    @CsvSource({"spring,false", "spark,false", "spring,true", "spark,true"})
    void bootBinderDoesNotRejectExplicitEmptyOrWhitespaceInEitherProfile(String module, boolean localDev) throws Exception {
        String expression = redisExpression(module, localDev);
        assertEquals("", bootBind(expression, ""));
        assertEquals("   ", bootBind(expression, "   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring", "spark"})
    void existingLocalDevProfileKeepsPasswordlessDefault(String module) throws Exception {
        assertEquals("", bootBind(redisExpression(module, true), null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring", "spark"})
    void localDevCanOverrideItsEmptyDefaultWithSyntheticValue(String module) throws Exception {
        assertEquals("synthetic-only", bootBind(redisExpression(module, true), "synthetic-only"));
    }
}
