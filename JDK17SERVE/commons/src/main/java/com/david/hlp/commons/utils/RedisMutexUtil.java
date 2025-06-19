package com.david.hlp.commons.utils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.david.hlp.commons.interfaces.CacheLoader;
import com.david.hlp.commons.filter.BloomFilterUtil;


/**
 * Redis 互斥工具类
 * 处理缓存击穿、缓存穿透等并发问题
 * 
 * @author david
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMutexUtil {

    private final RedisLockUtil redisLockUtil;
    private final RedisCacheUtil redisCacheUtil;
    private final BloomFilterUtil bloomFilterUtil;

    /** 缓存空值的过期时间（分钟） */
    private static final long CACHE_NULL_TTL = 2L;

    /** 默认锁等待时间（秒） */
    private static final long DEFAULT_WAIT_TIME = 10L;

    /** 默认锁租约时间（秒） */
    private static final long DEFAULT_LEASE_TIME = 30L;

    /** 默认布隆过滤器名称后缀 */
    private static final String DEFAULT_BLOOM_FILTER_SUFFIX = "_bloom";

    /**
     * 使用布隆过滤器 + 分布式锁解决缓存击穿和穿透问题
     * 
     * @param key           缓存键
     * @param element       布隆过滤器元素（用于判断数据是否可能存在）
     * @param bloomFilter   布隆过滤器名称
     * @param cacheTimeout  缓存超时时间
     * @param cacheTimeUnit 缓存时间单位
     * @param lockKey       锁键
     * @param waitTime      等待锁时间
     * @param leaseTime     锁租约时间
     * @param lockTimeUnit  锁时间单位
     * @param loader        数据加载器
     * @return 缓存数据
     */
    public <T> T getWithBloomAndMutex(final String key, final String element, final String bloomFilter,
            final long cacheTimeout, final TimeUnit cacheTimeUnit, final String lockKey,
            final long waitTime, final long leaseTime, final TimeUnit lockTimeUnit,
            final CacheLoader<T> loader) {

        Class<T> clazz = loader.getType();

        // 1. 先查询缓存（最快的路径）
        T cachedValue = redisCacheUtil.getCacheObject(key, clazz);
        if (cachedValue != null) {
            return cachedValue;
        }

        // 2. 缓存未命中，检查布隆过滤器（第一道防线）
        if (!bloomFilterUtil.mightContain(bloomFilter, element)) {
            log.debug("布隆过滤器判断元素不存在: element={}, bloomFilter={}", element, bloomFilter);

            return null; // 返回null，避免无效查询
        }

        // 3. 布隆过滤器判断可能存在，使用分布式锁
        boolean lockAcquired = redisLockUtil.tryLock(lockKey, waitTime, leaseTime, lockTimeUnit);
        if (!lockAcquired) {
            log.warn("获取分布式锁失败, lockKey: {}", lockKey);
            return null;
        }

        try {
            // 4. 获取锁成功，再次检查缓存（双重检查）
            T value = redisCacheUtil.getCacheObject(key, clazz);
            if (value != null) {
                return value;
            }

            // 5. 从数据源加载数据
            value = loader.load();

            // 6. 设置缓存和布隆过滤器
            if (value != null) {
                // 数据存在：缓存数据并添加到布隆过滤器
                redisCacheUtil.setCacheObject(key, value, clazz, cacheTimeout, cacheTimeUnit);
                bloomFilterUtil.add(bloomFilter, element);
                log.debug("数据加载成功并添加到布隆过滤器: element={}, key={}", element, key);
            } else {
                // 数据不存在：缓存空对象作为保底方案（防止缓存穿透）
                T emptyValue = loader.emptyInstance();
                redisCacheUtil.setCacheObject(key, emptyValue, clazz, CACHE_NULL_TTL, TimeUnit.MINUTES);
                log.debug("数据不存在，缓存空对象: key={}", key);
            }

            return value;

        } catch (Exception e) {
            log.error("从数据源加载数据时发生异常, key: {}, element: {}", key, element, e);
            throw new RuntimeException("加载数据失败: " + e.getMessage(), e);
        } finally {
            // 释放锁
            redisLockUtil.unlock(lockKey);
        }
    }

    /**
     * 使用分布式锁解决缓存击穿问题（原有方法，保持兼容性）
     * 
     * @param key           缓存键
     * @param cacheTimeout  缓存超时时间
     * @param cacheTimeUnit 缓存时间单位
     * @param lockKey       锁键
     * @param waitTime      等待锁时间
     * @param leaseTime     锁租约时间
     * @param lockTimeUnit  锁时间单位
     * @param loader        数据加载器
     * @return 缓存数据
     */
    public <T> T getWithMutex(final String key, final long cacheTimeout, final TimeUnit cacheTimeUnit,
            final String lockKey, final long waitTime, final long leaseTime, final TimeUnit lockTimeUnit,
            final CacheLoader<T> loader) {

        Class<T> clazz = loader.getType();

        // 1. 先查缓存
        T value = redisCacheUtil.getCacheObject(key, clazz);
        if (value != null) {
            return value;
        }

        // 2. 缓存未命中，使用分布式锁
        boolean lockAcquired = redisLockUtil.tryLock(lockKey, waitTime, leaseTime, lockTimeUnit);
        if (!lockAcquired) {
            log.warn("获取分布式锁失败, lockKey: {}", lockKey);
            return null;
        }

        try {
            // 3. 获取锁成功，再次检查缓存（双重检查）
            value = redisCacheUtil.getCacheObject(key, clazz);
            if (value != null) {
                return value;
            }

            // 4. 从数据源加载数据
            value = loader.load();

            // 5. 设置缓存，空数据也缓存（防止缓存穿透）
            long actualTimeout = value != null ? cacheTimeout : CACHE_NULL_TTL;
            T cacheValue = value != null ? value : loader.emptyInstance();

            redisCacheUtil.setCacheObject(key, cacheValue, clazz, actualTimeout, cacheTimeUnit);

            return value;

        } catch (Exception e) {
            log.error("从数据源加载数据时发生异常, key: {}", key, e);
            throw new RuntimeException("加载数据失败: " + e.getMessage(), e);
        } finally {
            // 释放锁
            redisLockUtil.unlock(lockKey);
        }
    }

    /**
     * 使用布隆过滤器 + 分布式锁解决缓存问题（使用默认锁参数）
     * 
     * @param key           缓存键
     * @param element       布隆过滤器元素
     * @param bloomFilter   布隆过滤器名称
     * @param cacheTimeout  缓存超时时间
     * @param cacheTimeUnit 缓存时间单位
     * @param loader        数据加载器
     * @return 缓存数据
     */
    public <T> T getWithBloomAndMutex(final String key, final String element, final String bloomFilter,
            final long cacheTimeout, final TimeUnit cacheTimeUnit, final CacheLoader<T> loader) {
        String lockKey = "lock:" + key;
        return getWithBloomAndMutex(key, element, bloomFilter, cacheTimeout, cacheTimeUnit, lockKey,
                DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS, loader);
    }

    /**
     * 使用布隆过滤器 + 分布式锁解决缓存问题（使用默认布隆过滤器名称和锁参数）
     * 
     * @param key          缓存键
     * @param element      布隆过滤器元素
     * @param cacheTimeout 缓存超时时间（分钟）
     * @param loader       数据加载器
     * @return 缓存数据
     */
    public <T> T getWithBloomAndMutex(final String key, final String element, final long cacheTimeout,
            final CacheLoader<T> loader) {
        String bloomFilter = key + DEFAULT_BLOOM_FILTER_SUFFIX;
        return getWithBloomAndMutex(key, element, bloomFilter, cacheTimeout, TimeUnit.MINUTES, loader);
    }

    /**
     * 使用分布式锁解决缓存击穿问题（使用默认锁参数）
     * 
     * @param key           缓存键
     * @param cacheTimeout  缓存超时时间
     * @param cacheTimeUnit 缓存时间单位
     * @param loader        数据加载器
     * @return 缓存数据
     */
    public <T> T getWithMutex(final String key, final long cacheTimeout, final TimeUnit cacheTimeUnit,
            final CacheLoader<T> loader) {
        String lockKey = "lock:" + key;
        return getWithMutex(key, cacheTimeout, cacheTimeUnit, lockKey,
                DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS, loader);
    }

    /**
     * 简化版本：默认使用布隆过滤器自动处理缓存穿透和击穿问题
     * 
     * @param key          缓存键
     * @param element      布隆过滤器元素
     * @param cacheTimeout 缓存超时时间（分钟）
     * @param loader       数据加载器
     * @return 缓存数据
     */
    public <T> T getOrLoad(final String key, final String element, final long cacheTimeout,
            final CacheLoader<T> loader) {
        return getWithBloomAndMutex(key, element, cacheTimeout, loader);
    }

    /**
     * 简化版本：默认使用布隆过滤器（元素使用key作为element）
     * 
     * @param key          缓存键
     * @param cacheTimeout 缓存超时时间（分钟）
     * @param loader       数据加载器
     * @return 缓存数据
     */
    public <T> T getOrLoad(final String key, final long cacheTimeout, final CacheLoader<T> loader) {
        return getOrLoad(key, key, cacheTimeout, loader);
    }

    /**
     * 不使用布隆过滤器的版本：仅使用分布式锁处理缓存击穿问题
     * 
     * @param key          缓存键
     * @param cacheTimeout 缓存超时时间（分钟）
     * @param loader       数据加载器
     * @return 缓存数据
     */
    public <T> T getOrLoadWithoutBloom(final String key, final long cacheTimeout, final CacheLoader<T> loader) {
        return getWithMutex(key, cacheTimeout, TimeUnit.MINUTES, loader);
    }

    /**
     * 初始化布隆过滤器
     * 
     * @param bloomFilter 布隆过滤器名称
     * @param capacity    预期容量
     * @param errorRate   错误率
     */
    public void initBloomFilter(String bloomFilter, long capacity, double errorRate) {
        bloomFilterUtil.initBloomFilter(bloomFilter, capacity, errorRate);
        log.info("布隆过滤器初始化完成: bloomFilter={}, capacity={}, errorRate={}", bloomFilter, capacity, errorRate);
    }

    /**
     * 初始化布隆过滤器（使用默认参数）
     * 
     * @param bloomFilter 布隆过滤器名称
     */
    public void initBloomFilter(String bloomFilter) {
        bloomFilterUtil.initBloomFilter(bloomFilter);
        log.info("布隆过滤器初始化完成（默认参数）: bloomFilter={}", bloomFilter);
    }

    /**
     * 预加载数据到布隆过滤器
     * 
     * @param bloomFilter 布隆过滤器名称
     * @param elements    要预加载的元素列表
     * @param capacity    预期容量
     * @param errorRate   错误率
     */
    public void preloadBloomFilter(String bloomFilter, List<String> elements, long capacity, double errorRate) {
        // 初始化布隆过滤器
        initBloomFilter(bloomFilter, capacity, errorRate);

        // 批量添加元素
        int addedCount = bloomFilterUtil.addBatch(bloomFilter, elements);
        log.info("布隆过滤器预加载完成: bloomFilter={}, 预期容量={}, 实际添加={}", bloomFilter, elements.size(), addedCount);
    }

    /**
     * 获取布隆过滤器统计信息
     * 
     * @param bloomFilter 布隆过滤器名称
     * @return 统计信息
     */
    public String getBloomFilterStats(String bloomFilter) {
        return bloomFilterUtil.getStats(bloomFilter);
    }

    /**
     * 清空布隆过滤器
     * 
     * @param bloomFilter 布隆过滤器名称
     */
    public void clearBloomFilter(String bloomFilter) {
        bloomFilterUtil.clear(bloomFilter);
        log.info("布隆过滤器已清空: bloomFilter={}", bloomFilter);
    }
}
