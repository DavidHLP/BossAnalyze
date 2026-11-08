package com.david.hlp.crawler.ai.executor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 任务执行服务接口
 */
public interface TaskExecutorService {

    /**
     * 异步执行任务
     *
     * @param taskName 任务名称
     * @param task     任务
     * @param <T>      任务返回结果类型
     * @return CompletableFuture对象，可用于获取任务执行结果
     */
    <T> CompletableFuture<T> executeAsync(String taskName, Supplier<T> task);

    /**
     * 异步执行无返回值的任务
     *
     * @param taskName 任务名称
     * @param task     任务
     * @return CompletableFuture对象，任务完成时返回null
     */
    CompletableFuture<Void> executeAsync(String taskName, Runnable task);
}