package com.david.hlp.commons.config;

import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RedisUtilsAutoConfigurationTest {
    @Test
    void lockClientUsesScalarCodecWithoutConnectingToRedis() {
        var client = mock(RedissonClient.class);
        try (var redisson = mockStatic(Redisson.class)) {
            redisson.when(() -> Redisson.create(any(Config.class))).thenAnswer(invocation -> {
                Config config = invocation.getArgument(0);
                assertSame(StringCodec.INSTANCE, config.getCodec());
                return client;
            });
            assertSame(client, new RedisUtilsAutoConfiguration().redissonClient(new RedisProperties()));
            redisson.verify(() -> Redisson.create(any(Config.class)));
        }
    }
}
