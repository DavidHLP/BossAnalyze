package com.david.hlp.Spring.common.threadpool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Boss HTML数据解析专用单线程线程池
 * 只允许一个线程执行任务
 */
public class ParseBossHtmlDataThreadPool {
    /**
     * 线程池单例
     */
    private static final ThreadPoolExecutor EXECUTOR;

    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE = 1;

    /**
     * 最大线程数
     */
    private static final int MAX_POOL_SIZE = 1;

    /**
     * 空闲线程存活时间(秒)
     */
    private static final long KEEP_ALIVE_TIME = 0L;

    /**
     * 任务队列容量
     */
    private static final int QUEUE_CAPACITY = 100;

    static {
        // 自定义线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "html-parser-thread-" + threadNumber.getAndIncrement());
                // 设置为非守护线程
                thread.setDaemon(false);
                return thread;
            }
        };
        EXECUTOR = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    /**
     * 私有构造函数，防止实例化
     */
    private ParseBossHtmlDataThreadPool() {
    }
    
    /**
     * 提交任务到线程池
     *
     * @param task 任务
     */
    public static void execute(Runnable task) {
        EXECUTOR.execute(task);
    }
    
    /**
     * 关闭线程池
     */
    public static void shutdown() {
        if (!EXECUTOR.isShutdown()) {
            EXECUTOR.shutdown();
        }
    }

    /**
     * 使用Spring上下文安全提交任务
     * 保证在异步线程中可以获取Spring上下文
     *
     * @param task 需要执行的任务
     */
    public static void executeWithSpringContext(final Runnable task) {
        // 获取当前线程的类加载器
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        
        EXECUTOR.execute(() -> {
            // 保存执行线程的原始类加载器
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                // 设置提交任务线程的类加载器到执行线程
                Thread.currentThread().setContextClassLoader(contextClassLoader);
                // 执行任务
                task.run();
            } catch (Exception e) {
                // 统一异常处理
                Thread.currentThread().getUncaughtExceptionHandler()
                      .uncaughtException(Thread.currentThread(), e);
            } finally {
                // 恢复原始类加载器
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        });
    }
    
    /**
     * 等待所有任务完成
     * 
     * @param timeout 超时时间(秒)
     * @return 是否在超时前完成所有任务
     */
    public static boolean awaitTermination(long timeout) {
        try {
            return EXECUTOR.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 获取当前活动线程数
     *
     * @return 活动线程数
     */
    public static int getActiveCount() {
        return EXECUTOR.getActiveCount();
    }
    
    /**
     * 获取队列中等待执行的任务数
     *
     * @return 等待执行的任务数
     */
    public static int getQueueSize() {
        return EXECUTOR.getQueue().size();
    }

    /**
     * 获取线程池
     *
     * @return 线程池实例
     */
    public static ThreadPoolExecutor getExecutor() {
        return EXECUTOR;
    }
}
