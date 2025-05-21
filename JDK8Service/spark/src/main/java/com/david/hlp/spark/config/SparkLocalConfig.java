package com.david.hlp.spark.config;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

/**
 * Spark 本地模式配置类
 */
@Configuration
@Getter
public class SparkLocalConfig {

    private final String appName = "LocalSparkApp";
    private final String masterUri = "local[*]";

    /**
     * 创建 Spark 配置 (本地模式)
     */
    @Bean
    public SparkConf sparkConf() {
        SparkConf conf = new SparkConf()
                .setAppName(appName)
                .setMaster(masterUri);
        return conf;
    }

    /**
     * 创建 JavaSparkContext (本地模式)
     * 用于 RDD 操作的上下文
     * 从 SparkSession Bean 获取 SparkContext
     */
    @Bean
    @ConditionalOnMissingBean(JavaSparkContext.class)
    public JavaSparkContext javaSparkContext(SparkSession sparkSession) {
        // 从 SparkSession 获取底层的 SparkContext 并包装
        return JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
    }

    /**
     * 创建 SparkSession (本地模式)
     * Spark 2.0+ 中用于处理结构化数据(DataFrame, Dataset)的入口
     */
    @Bean
    public SparkSession sparkSession() {
        return SparkSession
                .builder()
                .config(sparkConf())
                .getOrCreate();
    }
}