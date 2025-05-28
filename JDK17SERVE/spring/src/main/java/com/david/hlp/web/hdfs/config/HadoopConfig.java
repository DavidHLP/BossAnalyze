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

        // 解决数据节点替换错误 - 设置替换策略为NEVER
        // 这将阻止在写入失败时尝试替换数据节点，而是直接报告错误
        configuration.set("dfs.client.block.write.replace-datanode-on-failure.policy", "NEVER");
        configuration.setBoolean("dfs.client.block.write.replace-datanode-on-failure.enable", false);

        // IO性能和重试相关配置
        configuration.setInt("dfs.socket.timeout", 120000); // 套接字超时时间，单位毫秒
        configuration.setInt("dfs.datanode.socket.write.timeout", 120000); // 数据节点写入超时
        configuration.setInt("dfs.client.socket-timeout", 120000); // 客户端超时
        configuration.setInt("ipc.client.connect.timeout", 60000); // IPC连接超时
        configuration.setInt("ipc.client.connect.max.retries", 10); // IPC连接最大重试次数
        configuration.setInt("ipc.client.connect.retry.interval", 5000); // IPC重试间隔

        // 流缓冲区大小设置
        configuration.setInt("io.file.buffer.size", 65536); // 缓冲区大小64KB

        log.info("初始化Hadoop配置: {}, 数据节点替换策略: NEVER", hdfsUri);
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