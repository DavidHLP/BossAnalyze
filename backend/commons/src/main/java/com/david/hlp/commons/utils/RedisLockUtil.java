package com.david.hlp.commons.utils;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 分布式锁工具类
 * 使用 RedissonClient 处理分布式锁相关操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private final RedissonClient redissonClient;

    // 锁的默认超时时间，单位：秒
    private static final long DEFAULT_LEASE_TIME = 30;
    private static final long DEFAULT_WAIT_TIME = 10;

    /**
     * 获取分布式锁
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            return redissonClient.getLock(lockKey).tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁[{}]失败：{}", lockKey, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("获取分布式锁[{}]发生异常：{}", lockKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取分布式锁（使用默认参数）
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 释放分布式锁
     */
    public void unlock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("释放分布式锁[{}]发生异常：{}", lockKey, e.getMessage(), e);
        }
    }

    /**
     * 获取读锁
     */
    public boolean tryReadLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            return redissonClient.getReadWriteLock(lockKey).readLock().tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取读锁[{}]失败：{}", lockKey, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("获取读锁[{}]发生异常：{}", lockKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取读锁（使用默认参数）
     */
    public boolean tryReadLock(String lockKey) {
        return tryReadLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 获取写锁
     */
    public boolean tryWriteLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            return redissonClient.getReadWriteLock(lockKey).writeLock().tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取写锁[{}]失败：{}", lockKey, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("获取写锁[{}]发生异常：{}", lockKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取写锁（使用默认参数）
     */
    public boolean tryWriteLock(String lockKey) {
        return tryWriteLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 释放读写锁
     *
     * @param lockKey     锁的key
     * @param isWriteLock 是否是写锁
     */
    public void unlockReadWriteLock(String lockKey, boolean isWriteLock) {
        try {
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
            if (isWriteLock) {
                RLock writeLock = readWriteLock.writeLock();
                if (writeLock.isHeldByCurrentThread()) {
                    writeLock.unlock();
                }
            } else {
                RLock readLock = readWriteLock.readLock();
                if (readLock.isHeldByCurrentThread()) {
                    readLock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("释放{}锁[{}]发生异常：{}", isWriteLock ? "写" : "读", lockKey, e.getMessage(), e);
        }
    }
}