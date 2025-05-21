package com.david.hlp.crawler.ai.executor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 默认任务执行服务实现，使用线程池管理任务执行
 */
@Slf4j
@Service
public class DefaultTaskExecutorService implements TaskExecutorService {

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final long KEEP_ALIVE_TIME = 60L;

    private final ExecutorService executorService;

    public DefaultTaskExecutorService() {
        this.executorService = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new NamedThreadFactory("task-executor"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        log.info("任务执行服务初始化完成，核心线程数:{}, 最大线程数:{}", CORE_POOL_SIZE, MAX_POOL_SIZE);
    }

    @Override
    public <T> CompletableFuture<T> executeAsync(String taskName, Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                log.info("开始执行任务: {}", taskName);
                T result = task.get();
                log.info("任务执行完成: {}, 耗时: {}ms", taskName, System.currentTimeMillis() - startTime);
                return result;
            } catch (Exception e) {
                log.error("任务执行异常: {}, 错误: {}", taskName, e.getMessage(), e);
                throw e;
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> executeAsync(String taskName, Runnable task) {
        return CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                log.info("开始执行任务: {}", taskName);
                task.run();
                log.info("任务执行完成: {}, 耗时: {}ms", taskName, System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("任务执行异常: {}, 错误: {}", taskName, e.getMessage(), e);
                throw e;
            }
        }, executorService);
    }

    @PreDestroy
    public void shutdown() {
        log.info("关闭任务执行服务线程池");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 自定义线程工厂，为线程指定名称前缀
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String namePrefix) {
            group = Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix + "-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}