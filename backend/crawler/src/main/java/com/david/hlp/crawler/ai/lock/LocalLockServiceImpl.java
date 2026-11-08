package com.david.hlp.crawler.ai.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于本地锁的分布式锁服务实现
 */
@Slf4j
@Service
public class LocalLockServiceImpl implements DistributedLockService {

    private static final int DEFAULT_LOCK_TIMEOUT_SECONDS = 10;
    private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        Lock lock = getLock(lockKey);
        boolean locked = false;
        try {
            log.info("尝试获取[{}]锁", lockKey);
            locked = lock.tryLock(DEFAULT_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("无法获取[{}]锁，任务跳过", lockKey);
                return null;
            }

            log.info("成功获取[{}]锁，开始执行任务", lockKey);
            return task.get();
        } catch (InterruptedException e) {
            log.error("获取[{}]锁时被中断: {}", lockKey, e.getMessage(), e);
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (locked) {
                lock.unlock();
                log.info("释放[{}]锁", lockKey);
            }
        }
    }

    @Override
    public void executeWithLock(String lockKey, Runnable task) {
        executeWithLock(lockKey, () -> {
            task.run();
            return null;
        });
    }

    private Lock getLock(String lockKey) {
        return lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
    }
}