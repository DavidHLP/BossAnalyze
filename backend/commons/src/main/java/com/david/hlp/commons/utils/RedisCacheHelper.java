package com.david.hlp.commons.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.david.hlp.commons.interfaces.CacheLoader;

import lombok.RequiredArgsConstructor;

/**
 * Redis 缓存辅助工具类
 * 提供便捷的缓存操作方法，简化使用复杂度
 */
@Component
@RequiredArgsConstructor
public class RedisCacheHelper {

    private final RedisCacheUtil redisCacheUtil;
    private final RedisMutexUtil redisMutexUtil;
    private final RedisLockUtil redisLockUtil;

    /**
     * 获取或加载字符串缓存
     */
    public String getOrLoadString(String key, long timeoutMinutes, Supplier<String> loader) {
        return redisMutexUtil.getOrLoad(key, timeoutMinutes, new CacheLoader<String>() {
            @Override
            public String load() {
                return loader.get();
            }

            @Override
            public Class<String> getType() {
                return String.class;
            }
        });
    }

    /**
     * 获取或加载对象缓存
     */
    public <T> T getOrLoadObject(String key, Class<T> clazz, long timeoutMinutes, Supplier<T> loader) {
        return redisMutexUtil.getOrLoad(key, timeoutMinutes, new CacheLoader<T>() {
            @Override
            public T load() {
                return loader.get();
            }

            @Override
            public Class<T> getType() {
                return clazz;
            }
        });
    }

    /**
     * 获取或加载列表缓存
     */
    public <T> List<T> getOrLoadList(String key, Class<T> elementClass, long timeoutMinutes, Supplier<List<T>> loader) {
        return redisMutexUtil.getOrLoad(key, timeoutMinutes, new CacheLoader<List<T>>() {
            @Override
            public List<T> load() {
                return loader.get();
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<List<T>> getType() {
                return (Class<List<T>>) (Class<?>) List.class;
            }
        });
    }

    // ==================== 便捷的缓存操作方法 ====================

    /**
     * 设置字符串缓存
     */
    public void setString(String key, String value, long timeout, TimeUnit timeUnit) {
        redisCacheUtil.setCacheObject(key, value, String.class, timeout, timeUnit);
    }

    /**
     * 获取字符串缓存
     */
    public String getString(String key) {
        return redisCacheUtil.getCacheObject(key, String.class);
    }

    /**
     * 设置对象缓存
     */
    public <T> void setObject(String key, T value, Class<T> clazz, long timeout, TimeUnit timeUnit) {
        redisCacheUtil.setCacheObject(key, value, clazz, timeout, timeUnit);
    }

    /**
     * 获取对象缓存
     */
    public <T> T getObject(String key, Class<T> clazz) {
        return redisCacheUtil.getCacheObject(key, clazz);
    }

    /**
     * 获取Map缓存 - String类型的值
     */
    public Map<String, String> getStringMap(String key) {
        return redisCacheUtil.getCacheMap(key, String.class);
    }

    /**
     * 获取Map缓存 - 指定类型的值
     */
    public <T> Map<String, T> getMap(String key, Class<T> valueClass) {
        return redisCacheUtil.getCacheMap(key, valueClass);
    }

    /**
     * 获取List缓存
     */
    public <T> List<T> getList(String key, Class<T> elementClass) {
        return redisCacheUtil.getCacheList(key, elementClass);
    }

    // ==================== 锁相关操作 ====================

    /**
     * 尝试获取锁并执行操作
     */
    public <T> T executeWithLock(String lockKey, long waitSeconds, long leaseSeconds, Supplier<T> action) {
        boolean locked = redisLockUtil.tryLock(lockKey, waitSeconds, leaseSeconds, TimeUnit.SECONDS);
        if (!locked) {
            throw new RuntimeException("获取锁失败: " + lockKey);
        }

        try {
            return action.get();
        } finally {
            redisLockUtil.unlock(lockKey);
        }
    }

    /**
     * 尝试获取锁并执行操作（使用默认参数）
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        return executeWithLock(lockKey, 10, 30, action);
    }

    /**
     * 尝试获取锁并执行操作（无返回值）
     */
    public void executeWithLock(String lockKey, long waitSeconds, long leaseSeconds, Runnable action) {
        executeWithLock(lockKey, waitSeconds, leaseSeconds, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 尝试获取锁并执行操作（无返回值，使用默认参数）
     */
    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, 10, 30, action);
    }

    // ==================== 其他实用方法 ====================

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redisCacheUtil.deleteObject(key);
    }

    /**
     * 批量删除缓存
     */
    public Long deleteByPattern(String pattern) {
        return redisCacheUtil.deleteByPattern(pattern);
    }

    /**
     * 检查键是否存在
     */
    public Boolean exists(String key) {
        return redisCacheUtil.hasKey(key);
    }

    /**
     * 获取键的过期时间
     */
    public long getExpire(String key) {
        return redisCacheUtil.getExpire(key);
    }
}