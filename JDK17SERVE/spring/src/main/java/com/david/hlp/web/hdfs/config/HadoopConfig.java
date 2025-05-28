package com.david.hlp.web.hdfs.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Hadoop配置类
 * 提供Hadoop连接相关的Bean配置
 */
@Slf4j
@org.springframework.context.annotation.Configuration
@PropertySource("classpath:application.yml")
public class HadoopConfig {

    @Value("${hadoop.hdfs.uri:hdfs://hadoop-single:9000}")
    private String hdfsUri;

    @Value("${hadoop.hdfs.user:root}")
    private String hdfsUser;

    /**
     * 创建Hadoop配置
     */
    @Bean
    public Configuration hadoopConfiguration() {
        Configuration configuration = new Configuration();
        configuration.set("fs.defaultFS", hdfsUri);
        // 设置HDFS用户
        System.setProperty("HADOOP_USER_NAME", hdfsUser);
        // 设置副本数，通常单机测试时设为1
        configuration.set("dfs.replication", "3");
        log.info("初始化Hadoop配置: {}", hdfsUri);
        return configuration;
    }

    /**
     * 创建HDFS文件系统客户端
     */
    @Bean(destroyMethod = "close")
    public FileSystem fileSystem(Configuration configuration) {
        FileSystem fileSystem = null;
        try {
            URI uri = new URI(hdfsUri);
            fileSystem = FileSystem.get(uri, configuration, hdfsUser);
            log.info("HDFS文件系统连接成功: {}", hdfsUri);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            log.error("创建HDFS文件系统实例失败", e);
            // 返回null，让Spring容器处理错误
        }
        return fileSystem;
    }
}