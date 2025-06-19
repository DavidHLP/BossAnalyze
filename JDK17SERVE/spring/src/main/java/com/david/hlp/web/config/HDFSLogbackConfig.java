package com.david.hlp.web.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.david.hlp.web.utils.HDFSLogAppender;

import java.lang.management.ManagementFactory;

/**
 * HDFS Logback配置
 * 配置Logback将日志同时写入HDFS
 */
@Configuration
public class HDFSLogbackConfig {

    private final FileSystem fileSystem;
    private HDFSLogAppender hdfsLogAppender;
    private final String processId;

    @Value("${hadoop.hdfs.log.dir:/logs}")
    private String logDir;

    @Value("${hadoop.hdfs.log.level:INFO}")
    private String logLevel;

    public HDFSLogbackConfig(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        // 获取进程ID
        String name = ManagementFactory.getRuntimeMXBean().getName();
        this.processId = name.split("@")[0];

        // 设置系统属性，供日志格式使用
        System.setProperty("PID", this.processId);
    }

    @PostConstruct
    public void init() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // 创建HDFS Appender
        hdfsLogAppender = new HDFSLogAppender(fileSystem, logDir);
        hdfsLogAppender.setContext(loggerContext);
        hdfsLogAppender.setName("HDFS");
        hdfsLogAppender.start();

        // 获取根Logger并添加Appender
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(hdfsLogAppender);

        // 设置HDFS日志级别
        Level level = Level.toLevel(logLevel, Level.INFO);
        rootLogger.setLevel(level);
    }

    @PreDestroy
    public void destroy() {
        if (hdfsLogAppender != null) {
            hdfsLogAppender.stop();
        }
    }
}