package com.david.hlp.web.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Hadoop配置类
 * 提供Hadoop连接相关的Bean配置。
 * 针对Hadoop集群环境，需要进行相应的配置调整。
 */
@Slf4j
@org.springframework.context.annotation.Configuration
@PropertySource("classpath:application.yml")
public class HadoopConfig {

    /**
     * Hadoop HDFS的URI。
     * 对于单机模式，通常是 hdfs://hostname:9000。
     * 对于高可用（HA）集群，这里应该配置为nameservice的名称，例如：hdfs://mycluster。
     * 该值在 application.yml 文件中配置。
     */
    @Value("${hadoop.hdfs.uri:hdfs://mycluster}")
    private String hdfsUri;

    @Value("${hadoop.hdfs.user:root}")
    private String hdfsUser;

    /**
     * 创建Hadoop配置。
     * 提示: 要连接到一个真正的Hadoop集群，请确认以下几点：
     * 1. 在 `application.yml` 中将 `hadoop.hdfs.uri` 指向您集群的Active NameNode地址或HA
     * NameService ID。
     * 2. 为了使客户端能够解析HA NameService，需要将集群的 `core-site.xml` 和 `hdfs-site.xml`
     * 配置文件放置在项目的 `resources` 目录下。
     * 3. 如果集群启用了Kerberos安全认证，则需要添加额外的安全相关配置。
     */
    @Bean
    public Configuration hadoopConfiguration() {
        Configuration configuration = new Configuration();
        configuration.set("fs.defaultFS", hdfsUri);
        if (hdfsUri.startsWith("file:")) {
            // The checksum local filesystem does not support appending existing logs.
            configuration.setClass("fs.file.impl", RawLocalFileSystem.class, FileSystem.class);
        }
        // 设置HDFS用户
        System.setProperty("HADOOP_USER_NAME", hdfsUser);
        // 设置HDFS副本数。对于生产环境的集群，通常设置为3。单机测试时可以设为1。
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
