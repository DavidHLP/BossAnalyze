package com.david.hlp.web.hdfs.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * HDFS日志工具类
 * 提供将日志内容写入HDFS的方法
 */
@Slf4j
@Component
public class LogUtil {

    private final FileSystem fileSystem;
    private final String processId;

    public LogUtil(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        // 获取进程ID
        String name = ManagementFactory.getRuntimeMXBean().getName();
        this.processId = name.split("@")[0];
    }

    /**
     * 将日志内容写入HDFS
     * 
     * @param logContent  日志内容
     * @param logDir      HDFS上的日志目录
     * @param logFileName 日志文件名
     * @return 是否写入成功
     */
    public boolean writeLog(String logContent, String logDir, String logFileName) {
        try {
            // 确保目录存在
            Path dirPath = new Path(logDir);
            if (!fileSystem.exists(dirPath)) {
                fileSystem.mkdirs(dirPath);
            }

            // 构建完整日志路径
            Path logFilePath = new Path(logDir + "/" + logFileName);

            // 追加内容到日志文件
            FSDataOutputStream outputStream;
            if (fileSystem.exists(logFilePath)) {
                outputStream = fileSystem.append(logFilePath);
            } else {
                outputStream = fileSystem.create(logFilePath);
            }

            outputStream.write(logContent.getBytes(StandardCharsets.UTF_8));
            outputStream.close();

            log.info("日志已成功写入HDFS: {}", logFilePath);
            return true;
        } catch (IOException e) {
            log.error("写入日志到HDFS失败", e);
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
}
