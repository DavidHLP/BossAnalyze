package com.david.hlp.web.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * HDFS日志工具类
 * 提供将日志内容写入HDFS的方法
 */
@Slf4j
@Component
public class LogUtil {

    private final FileSystem fileSystem;
    private final String processId;

    // 日志缓存，按文件路径分组
    private final Map<String, List<String>> logCache = new ConcurrentHashMap<>();
    // 缓存锁，避免并发问题
    private final Lock cacheLock = new ReentrantLock();

    // 默认缓存阈值，达到此数量时触发写入
    @Value("${hdfs.log.cache.threshold:100}")
    private int cacheThreshold;

    // 默认最大缓存时间（毫秒），超过此时间触发写入
    @Value("${hdfs.log.flush.interval:30000}")
    private long flushInterval;

    public LogUtil(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        // 获取进程ID
        String name = ManagementFactory.getRuntimeMXBean().getName();
        this.processId = name.split("@")[0];
    }

    /**
     * 将日志内容添加到缓存
     * 
     * @param logContent  日志内容
     * @param logDir      HDFS上的日志目录
     * @param logFileName 日志文件名
     * @return 是否成功添加到缓存
     */
    public boolean writeLog(String logContent, String logDir, String logFileName) {
        try {
            String logPath = logDir + "/" + logFileName;

            cacheLock.lock();
            try {
                // 将日志添加到对应路径的缓存中
                logCache.computeIfAbsent(logPath, k -> new ArrayList<>()).add(logContent);

                // 如果缓存达到阈值，执行批量写入
                if (logCache.get(logPath).size() >= cacheThreshold) {
                    flushCache(logPath);
                }
            } finally {
                cacheLock.unlock();
            }

            return true;
        } catch (Exception e) {
            log.error("添加日志到缓存失败", e);
            return false;
        }
    }

    /**
     * 执行实际的HDFS写入操作
     * 
     * @param logPath 日志文件完整路径
     * @return 是否写入成功
     */
    private boolean flushCache(String logPath) {
        List<String> cachedLogs;

        // 获取并清空指定路径的缓存
        cacheLock.lock();
        try {
            if (!logCache.containsKey(logPath) || logCache.get(logPath).isEmpty()) {
                return true; // 没有需要写入的日志
            }

            cachedLogs = new ArrayList<>(logCache.get(logPath));
            logCache.get(logPath).clear();
        } finally {
            cacheLock.unlock();
        }

        try {
            // 确保目录存在
            Path filePath = new Path(logPath);
            Path dirPath = filePath.getParent();
            if (!fileSystem.exists(dirPath)) {
                fileSystem.mkdirs(dirPath);
            }

            // 批量写入日志
            FSDataOutputStream outputStream;
            if (fileSystem.exists(filePath)) {
                outputStream = fileSystem.append(filePath);
            } else {
                outputStream = fileSystem.create(filePath);
            }

            // 一次性写入所有缓存日志
            StringBuilder batchContent = new StringBuilder();
            for (String log : cachedLogs) {
                batchContent.append(log);
            }

            byte[] contentBytes = batchContent.toString().getBytes(StandardCharsets.UTF_8);
            outputStream.write(contentBytes);
            outputStream.hsync(); // 确保数据落盘
            outputStream.close();

            log.info("批量日志已成功写入HDFS: {}, 条数: {}", logPath, cachedLogs.size());
            return true;
        } catch (IOException e) {
            log.error("批量写入日志到HDFS失败: " + logPath, e);
            // 写入失败时，将日志放回缓存以便下次尝试
            cacheLock.lock();
            try {
                List<String> currentCache = logCache.computeIfAbsent(logPath, k -> new ArrayList<>());
                currentCache.addAll(0, cachedLogs); // 添加到缓存开头，优先下次写入
            } finally {
                cacheLock.unlock();
            }
            return false;
        }
    }

    /**
     * 使用当前日期作为文件名写入日志
     * 
     * @param logContent 日志内容
     * @param logDir     HDFS上的日志目录
     * @return 是否写入成功
     */
    public boolean writeLogWithDateFile(String logContent, String logDir) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fileName = sdf.format(new Date()) + ".log";
        return writeLog(logContent, logDir, fileName);
    }

    /**
     * 格式化日志内容为Spring Boot格式
     * 
     * @param level     日志级别
     * @param className 类名
     * @param message   日志消息
     * @return 格式化后的日志内容
     */
    public String formatSpringBootLog(String level, String className, String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String timestamp = sdf.format(new Date());
        String threadName = Thread.currentThread().getName();

        return String.format("%s %s %s --- [%s] %s : %s%n",
                timestamp, level, processId, threadName, className, message);
    }

    /**
     * 设置缓存阈值
     * 
     * @param threshold 新的缓存阈值
     */
    public void setCacheThreshold(int threshold) {
        if (threshold > 0) {
            this.cacheThreshold = threshold;
        }
    }

    /**
     * 定时任务，定期刷新缓存中的日志到HDFS
     * 防止日志量较少时长时间不写入
     */
    @Scheduled(fixedDelayString = "${hdfs.log.flush.interval:30000}")
    public void scheduledFlush() {
        flushAllCaches();
    }

    /**
     * 刷新所有缓存中的日志到HDFS
     */
    public void flushAllCaches() {
        log.debug("开始执行定时日志缓存刷新");
        List<String> paths;

        cacheLock.lock();
        try {
            paths = new ArrayList<>(logCache.keySet());
        } finally {
            cacheLock.unlock();
        }

        // 遍历所有缓存的路径，进行刷新
        for (String path : paths) {
            flushCache(path);
        }
    }

    /**
     * 在应用关闭前，确保所有缓存的日志都写入到HDFS
     */
    @PreDestroy
    public void destroy() {
        log.info("应用关闭，刷新所有日志缓存到HDFS");
        flushAllCaches();
    }
}
