package com.david.hlp.spark.config;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkClusterConfig {

    @Value("${spark.app.name}")
    private String appName;

    @Value("${spark.master.uri}")
    private String masterUri;

    @Value("${spark.deploy.zookeeper.url}")
    private String zookeeperUrl;

    @Value("${spark.deploy.zookeeper.dir}")
    private String zookeeperDir;

    @Value("${spark.eventLog.dir}")
    private String eventLogDir;

    @Bean
    public SparkConf sparkConf() throws IOException {
        boolean local = masterUri.equals("local") || masterUri.startsWith("local[");
        SparkConf config = new SparkConf()
                .setAppName(appName)
                .setMaster(masterUri)
                .set("spark.kryoserializer.buffer.max", "64m")
                .set("spark.network.timeout", "300s")
                .set("spark.sql.adaptive.enabled", "true")
                .set("spark.sql.adaptive.coalescePartitions.enabled", "true")
                .set("spark.dynamicAllocation.enabled", Boolean.toString(!local))
                .set("spark.dynamicAllocation.minExecutors", "1")
                .set("spark.dynamicAllocation.maxExecutors", "10")
                .set("spark.dynamicAllocation.initialExecutors", "3")
                .set("spark.shuffle.service.enabled", Boolean.toString(!local))
                .set("spark.shuffle.compress", "true")
                .set("spark.shuffle.spill.compress", "true")
                .set("spark.eventLog.enabled", "true")
                .set("spark.eventLog.dir", eventLogDir);
        if (local) {
            URI eventLogUri = URI.create(eventLogDir);
            if ("file".equalsIgnoreCase(eventLogUri.getScheme())) {
                Files.createDirectories(Path.of(eventLogUri));
            }
            config.set("spark.driver.host", "127.0.0.1")
                    .set("spark.driver.bindAddress", "127.0.0.1");
        } else {
            config.set("spark.deploy.recoveryMode", "ZOOKEEPER")
                    .set("spark.deploy.zookeeper.url", zookeeperUrl)
                    .set("spark.deploy.zookeeper.dir", zookeeperDir);
        }
        return config;
    }

    @Bean
    public SparkSession sparkSession(SparkConf sparkConf) {
        return SparkSession.builder()
                .config(sparkConf)
                .getOrCreate();
    }
}
