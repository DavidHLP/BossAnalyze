package com.david.hlp.commons.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import com.david.hlp.commons.utils.RedisCacheHelper;
import com.david.hlp.commons.utils.RedisCacheUtil;
import com.david.hlp.commons.utils.RedisLockUtil;
import com.david.hlp.commons.utils.RedisMutexUtil;
import com.david.hlp.commons.filter.BloomFilterUtil;

/**
 * Redis工具类自动配置
 *
 * @author david
 */
@Configuration
@ConditionalOnClass({ RedisTemplate.class, RedissonClient.class })
@ComponentScan(basePackages = "com.david.hlp.commons")
public class RedisUtilsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedisTemplate.class)
    public RedisCacheUtil redisCacheUtil(RedisTemplate<String, Object> redisTemplate) {
        return new RedisCacheUtil(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedissonClient.class)
    public RedisLockUtil redisLockUtil(RedissonClient redissonClient) {
        return new RedisLockUtil(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(RedisTemplate.class)
    public BloomFilterUtil bloomFilterUtil(RedisTemplate<String, Object> redisTemplate) {
        return new BloomFilterUtil(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ RedisLockUtil.class, RedisCacheUtil.class, BloomFilterUtil.class })
    public RedisMutexUtil redisMutexUtil(RedisLockUtil redisLockUtil, RedisCacheUtil redisCacheUtil,
            BloomFilterUtil bloomFilterUtil) {
        return new RedisMutexUtil(redisLockUtil, redisCacheUtil, bloomFilterUtil);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ RedisCacheUtil.class, RedisMutexUtil.class, RedisLockUtil.class })
    public RedisCacheHelper redisCacheHelper(RedisCacheUtil redisCacheUtil, RedisMutexUtil redisMutexUtil,
            RedisLockUtil redisLockUtil) {
        return new RedisCacheHelper(redisCacheUtil, redisMutexUtil, redisLockUtil);
    }
}