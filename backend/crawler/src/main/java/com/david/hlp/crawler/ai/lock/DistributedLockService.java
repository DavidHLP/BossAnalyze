package com.david.hlp.crawler.ai.lock;

import java.util.function.Supplier;

/**
 * 分布式锁服务接口
 */
public interface DistributedLockService {

    /**
     * 使用锁执行任务
     *
     * @param lockKey 锁的唯一标识
     * @param task    需要执行的任务
     * @param <T>     任务返回结果类型
     * @return 任务执行结果，如果获取锁失败则返回null
     */
    <T> T executeWithLock(String lockKey, Supplier<T> task);

    /**
     * 使用锁执行无返回值的任务
     *
     * @param lockKey 锁的唯一标识
     * @param task    需要执行的任务
     */
    void executeWithLock(String lockKey, Runnable task);
}