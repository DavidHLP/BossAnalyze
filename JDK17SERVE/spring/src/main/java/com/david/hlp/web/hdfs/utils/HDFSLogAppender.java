package com.david.hlp.web.hdfs.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * HDFS日志追加器
 * 将Logback日志事件写入HDFS
 */
public class HDFSLogAppender extends AppenderBase<ILoggingEvent> {

    private FileSystem fileSystem;
    private String logDir;
    private String datePattern = "yyyy-MM-dd";
    private FSDataOutputStream currentOutputStream;
    private String currentDate;
    private String processId;
    
    // 日志缓存集合
    private final List<String> logCache = new ArrayList<>();
    // 缓存锁，避免并发问题
    private final Lock cacheLock = new ReentrantLock();
    // 默认缓存阈值，达到此数量时触发写入
    private int cacheThreshold = 100;
    // 上次刷新时间
    private long lastFlushTime = System.currentTimeMillis();
    // 最大缓存时间（毫秒），超过此时间触发写入
    private long flushInterval = 30000;

    public HDFSLogAppender(FileSystem fileSystem, String logDir) {
        this.fileSystem = fileSystem;
        this.logDir = logDir;
        this.processId = System.getProperty("PID", "?");
    }
    
    /**
     * 设置缓存阈值
     * @param threshold 新的缓存阈值
     */
    public void setCacheThreshold(int threshold) {
        if (threshold > 0) {
            this.cacheThreshold = threshold;
        }
    }
    
    /**
     * 设置刷新间隔（毫秒）
     * @param interval 刷新间隔
     */
    public void setFlushInterval(long interval) {
        if (interval > 0) {
            this.flushInterval = interval;
        }
    }

    @Override
    public void start() {
        if (fileSystem == null) {
            addError("FileSystem不能为空");
            return;
        }
        if (logDir == null || logDir.isEmpty()) {
            addError("日志目录不能为空");
            return;
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }

        try {
            String formattedDate = new SimpleDateFormat(datePattern).format(new Date());

            // 如果日期变化了或者输出流未初始化，创建新的输出流并刷新缓存
            if (currentOutputStream == null || !formattedDate.equals(currentDate)) {
                // 先刷新当前缓存中的日志
                flushCache();
                closeCurrentStream();
                
                currentDate = formattedDate;
                String fileName = currentDate + ".log";

                // 确保目录存在
                Path dirPath = new Path(logDir);
                if (!fileSystem.exists(dirPath)) {
                    fileSystem.mkdirs(dirPath);
                }

                // 构建完整日志路径
                Path logFilePath = new Path(logDir + "/" + fileName);

                // 打开或创建日志文件
                if (fileSystem.exists(logFilePath)) {
                    currentOutputStream = fileSystem.append(logFilePath);
                } else {
                    currentOutputStream = fileSystem.create(logFilePath);
                }
            }

            // 格式化日志条目，与Logback标准格式一致
            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String timestamp = timestampFormat.format(new Date(event.getTimeStamp()));

            String logLevel = event.getLevel().toString();
            String threadName = event.getThreadName();
            String loggerName = event.getLoggerName();
            String message = event.getFormattedMessage();

            // 标准Logback格式：时间 级别 PID --- [线程] 类名 : 消息
            String fullLogEntry = String.format("%s %s %s --- [%s] %s : %s%n",
                    timestamp,
                    logLevel,
                    processId,
                    threadName,
                    loggerName,
                    message);

            // 添加到缓存
            cacheLock.lock();
            try {
                logCache.add(fullLogEntry);
                
                // 检查是否需要刷新缓存
                long currentTime = System.currentTimeMillis();
                boolean timeThresholdExceeded = (currentTime - lastFlushTime) > flushInterval;
                
                if (logCache.size() >= cacheThreshold || timeThresholdExceeded) {
                    flushCache();
                    lastFlushTime = currentTime;
                }
            } finally {
                cacheLock.unlock();
            }
        } catch (Exception e) {
            addError("处理日志事件失败", e);
        }
    }

    /**
     * 刷新缓存中的日志到HDFS
     */
    private void flushCache() {
        if (currentOutputStream == null || logCache.isEmpty()) {
            return;
        }
        
        List<String> logsToWrite;
        cacheLock.lock();
        try {
            if (logCache.isEmpty()) {
                return;
            }
            logsToWrite = new ArrayList<>(logCache);
            logCache.clear();
        } finally {
            cacheLock.unlock();
        }
        
        try {
            // 批量写入所有缓存日志
            StringBuilder batchContent = new StringBuilder();
            for (String log : logsToWrite) {
                batchContent.append(log);
            }
            
            byte[] contentBytes = batchContent.toString().getBytes(StandardCharsets.UTF_8);
            currentOutputStream.write(contentBytes);
            currentOutputStream.hsync(); // 确保数据落盘
            
            addInfo(String.format("批量写入%d条日志到HDFS", logsToWrite.size()));
        } catch (IOException e) {
            addError("批量写入日志到HDFS失败", e);
            // 写入失败时，将日志放回缓存以便下次尝试
            cacheLock.lock();
            try {
                // 添加到缓存开头，优先下次写入
                logsToWrite.addAll(logCache);
                logCache.clear();
                logCache.addAll(logsToWrite);
            } finally {
                cacheLock.unlock();
            }
        }
    }
    
    private void closeCurrentStream() {
        if (currentOutputStream != null) {
            try {
                currentOutputStream.close();
            } catch (IOException e) {
                addError("关闭HDFS输出流失败", e);
            } finally {
                currentOutputStream = null;
            }
        }
    }

    @Override
    public void stop() {
        flushCache(); // 关闭前刷新所有缓存
        closeCurrentStream();
        super.stop();
    }
}