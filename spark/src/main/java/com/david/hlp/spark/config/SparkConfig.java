package com.david.hlp.spark.config;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spark配置类
 */
@Configuration
@Getter
public class SparkConfig {
    private static final Logger log = LoggerFactory.getLogger(SparkConfig.class);

    @Value("${spark.app.name}")
    private String appName;

    @Value("${spark.master.uri}")
    private String masterUri;

    @Value("${spark.redis.key-prefix:spark:job:}")
    private String redisKeyPrefix;

    @Value("${spark.checkpoint.dir:/bigdata/checkpoint}")
    private String checkpointDir;

    @Value("${spark.persistence.enabled:true}")
    private boolean persistenceEnabled;

    @Value("${spark.persistence.storage-level:MEMORY_AND_DISK}")
    private String persistenceStorageLevel;

    @Value("${spark.cluster.mode:false}")
    private boolean clusterMode;

    @Value("${spark.executor.instances:2}")
    private int executorInstances;

    @Value("${spark.executor.cores:2}")
    private int executorCores;

    @Value("${spark.executor.memory:2g}")
    private String executorMemory;

    @Value("${spark.driver.memory:2g}")
    private String driverMemory;

    @Value("${spark.sql.shuffle.partitions:200}")
    private int shufflePartitions;

    @Value("${spark.default.parallelism:20}")
    private int defaultParallelism;

    @Value("${spark.distributed.fs.enabled:false}")
    private boolean distributedFsEnabled;

    @Value("${spark.distributed.fs.prefix:hdfs://namenode:8020}")
    private String distributedFsPrefix;

    /**
     * 创建Spark配置
     */
    @Bean
    public SparkConf sparkConf() {
        SparkConf conf = new SparkConf()
                .setAppName(appName)
                .setMaster(masterUri)
                .set("spark.driver.memory", driverMemory)
                .set("spark.executor.memory", executorMemory)
                .set("spark.sql.warehouse.dir", "/bigdata/jupyter/Json/warehouse");
        
        // 如果是集群模式，添加集群相关配置
        if (clusterMode) {
            configureClusterMode(conf);
        }
        
        return conf;
    }
    
    /**
     * 配置集群模式的Spark参数
     */
    private void configureClusterMode(SparkConf conf) {
        log.info("配置Spark集群模式，executor实例数: {}, 每个executor核心数: {}", 
                executorInstances, executorCores);
        
        // 设置Spark SQL的shuffle分区数，影响并行度
        conf.set("spark.sql.shuffle.partitions", String.valueOf(shufflePartitions));
        
        // 设置默认并行度，影响RDD分区数
        conf.set("spark.default.parallelism", String.valueOf(defaultParallelism));
        
        // 如果使用的是yarn集群，可以动态调整分配
        conf.set("spark.dynamicAllocation.enabled", "true");
        conf.set("spark.dynamicAllocation.initialExecutors", String.valueOf(executorInstances));
        
        // 设置序列化方式为Kryo，提高性能
        conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
        
        // 设置广播变量的压缩
        conf.set("spark.broadcast.compress", "true");
        
        // 设置RDD压缩
        conf.set("spark.rdd.compress", "true");
        
        // 设置executor核心数
        conf.set("spark.executor.cores", String.valueOf(executorCores));
    }

    /**
     * 创建JavaSparkContext
     * 用于RDD操作的上下文
     */
    @Bean
    @ConditionalOnMissingBean(JavaSparkContext.class)
    public JavaSparkContext javaSparkContext() {
        JavaSparkContext jsc = new JavaSparkContext(sparkConf());
        
        // 设置checkpoint目录
        String effectiveCheckpointDir = getEffectiveCheckpointDir();
        jsc.setCheckpointDir(effectiveCheckpointDir);
        log.info("设置checkpoint目录: {}", effectiveCheckpointDir);
        
        return jsc;
    }

    /**
     * 创建SparkSession
     * Spark 2.0+中用于处理结构化数据(DataFrame, Dataset)的入口
     */
    @Bean
    public SparkSession sparkSession() {
        return SparkSession
                .builder()
                .config(sparkConf())
                .sparkContext(javaSparkContext().sc())
                .getOrCreate();
    }
    
    /**
     * 获取有效的checkpoint目录
     * 在集群模式下，确保使用分布式文件系统
     */
    public String getEffectiveCheckpointDir() {
        if (clusterMode && distributedFsEnabled) {
            // 如果checkpoint目录是绝对路径且没有协议前缀，则添加分布式文件系统前缀
            if (checkpointDir.startsWith("/") && !checkpointDir.contains("://")) {
                String path = distributedFsPrefix + checkpointDir;
                log.info("集群模式：将本地路径 {} 转换为分布式存储路径 {}", checkpointDir, path);
                return path;
            }
        }
        return checkpointDir;
    }
    
    /**
     * 获取数据存储路径
     * 确保在集群环境下使用分布式文件系统
     */
    public String getDataStoragePath(String subPath) {
        String basePath = getEffectiveCheckpointDir();
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        return basePath + subPath;
    }
}