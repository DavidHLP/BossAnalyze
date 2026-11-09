package com.david.hlp.commons.utils;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisCacheUtilTest {
    @Test
    void stringCacheRoundTripsVerificationCodesAndLiteralJson() {
        RedisCacheUtil cache = fixture();
        for (boolean ttl : List.of(false, true)) {
            for (String value : List.of("", " \t\n", "\u3000", "012345", "Unicode 简历", "{\"code\":\"012345\"}", "\"quoted\"")) {
                if (ttl) {
                    cache.setCacheObject("fixture", value, String.class, 1L, TimeUnit.MINUTES);
                } else {
                    cache.setCacheObject("fixture", value, String.class);
                }
                assertEquals(value, cache.getCacheObject("fixture", String.class));
            }
        }
    }

    @Test
    void objectCacheAndMissingEntriesKeepTheirContracts() {
        RedisCacheUtil cache = fixture();
        assertNull(cache.getCacheObject("missing", String.class));
        assertNull(cache.getCacheObject("missing", Map.class));
        cache.setCacheObject("snapshot", Map.of("status", "ready"), Map.class, 1L, TimeUnit.MINUTES);
        assertEquals(Map.of("status", "ready"), cache.getCacheObject("snapshot", Map.class));
    }

    private RedisCacheUtil fixture() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        Map<String, Object> stored = new HashMap<>();
        when(template.opsForValue()).thenReturn(values);
        Answer<Void> save = call -> {
            stored.put(call.getArgument(0), call.getArgument(1));
            return null;
        };
        doAnswer(save).when(values).set(anyString(), any());
        doAnswer(save).when(values).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        when(values.get(anyString())).thenAnswer(call -> stored.get(call.getArgument(0)));
        return new RedisCacheUtil(template);
    }
}
