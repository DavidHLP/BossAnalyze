package com.david.hlp.spark.service;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.*;
import org.apache.spark.storage.StorageLevel;
import static org.apache.spark.sql.functions.*;
import org.apache.spark.api.java.function.VoidFunction2;
import com.david.hlp.spark.utils.RedisCache;
import com.david.hlp.spark.config.SparkConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Boss职位数据Spark流处理服务
 * 从Kafka读取职位数据，进行分析，并将结果存储到Redis
 * 支持单机和集群模式
 */
@Slf4j
@Component
@RequiredArgsConstructor
@PropertySource("classpath:application.yml")
public class BossSpark {

    private final RedisCache redisCache;
    private final SparkSession sparkSession;
    private final SparkConfig sparkConfig;

    @Value("${spark.redis.data.expire-days:7}")
    private int redisDataExpireDays;

    private Dataset<Row> accumulatedData = null;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private StreamingQuery query;

    // 用于存储持久化的中间结果Dataset
    private Set<Dataset<Row>> persistedDatasets = new HashSet<>();

    /**
     * Spring Bean初始化后启动Spark任务
     */
    @PostConstruct
    public void startSparkJob() {
        try {
            log.info("初始化Spark任务...");

            // 启动任务前尝试恢复上一次的累积数据
            recoverFromCheckpoint();

            processStreamingData();
            log.info("Spark任务启动成功");
        } catch (Exception e) {
            log.error("启动Spark任务时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 根据配置获取StorageLevel
     */
    private StorageLevel getStorageLevel() {
        // 在集群模式下，考虑使用MEMORY_AND_DISK_SER来减少内存占用
        if (sparkConfig.isClusterMode()) {
            switch (sparkConfig.getPersistenceStorageLevel().toUpperCase()) {
                case "MEMORY_ONLY":
                    return StorageLevel.MEMORY_ONLY_SER();
                case "MEMORY_AND_DISK":
                    return StorageLevel.MEMORY_AND_DISK_SER();
                case "MEMORY_ONLY_SER":
                    return StorageLevel.MEMORY_ONLY_SER();
                case "MEMORY_AND_DISK_SER":
                    return StorageLevel.MEMORY_AND_DISK_SER();
                case "DISK_ONLY":
                    return StorageLevel.DISK_ONLY();
                default:
                    return StorageLevel.MEMORY_AND_DISK_SER();
            }
        } else {
            switch (sparkConfig.getPersistenceStorageLevel().toUpperCase()) {
                case "MEMORY_ONLY":
                    return StorageLevel.MEMORY_ONLY();
                case "MEMORY_AND_DISK":
                    return StorageLevel.MEMORY_AND_DISK();
                case "MEMORY_ONLY_SER":
                    return StorageLevel.MEMORY_ONLY_SER();
                case "MEMORY_AND_DISK_SER":
                    return StorageLevel.MEMORY_AND_DISK_SER();
                case "DISK_ONLY":
                    return StorageLevel.DISK_ONLY();
                default:
                    return StorageLevel.MEMORY_AND_DISK();
            }
        }
    }

    /**
     * 持久化DataFrame，如果启用了持久化功能
     */
    private Dataset<Row> persistDataset(Dataset<Row> dataset, String name) {
        if (sparkConfig.isPersistenceEnabled() && dataset != null) {
            try {
                // 设置名称方便在Spark UI中识别
                dataset.createOrReplaceTempView(name);

                // 如果是集群模式，在持久化前先进行适当的分区
                if (sparkConfig.isClusterMode() && dataset.rdd().getNumPartitions() < sparkConfig.getDefaultParallelism()) {
                    int partitionCount = Math.min(sparkConfig.getDefaultParallelism(), 
                            (int)dataset.count() / 10000 + 1); // 每10000条数据一个分区
                    dataset = dataset.repartition(partitionCount);
                    log.info("集群模式下重新分区，数据集: {}, 分区数: {}", name, partitionCount);
                }

                // 持久化数据
                dataset.persist(getStorageLevel());

                // 记录已持久化的Dataset，便于后续清理
                persistedDatasets.add(dataset);
                log.info("已持久化数据集: {}, 存储级别: {}", name, 
                        sparkConfig.isClusterMode() ? getStorageLevel().description() + " (集群模式)" : 
                                      sparkConfig.getPersistenceStorageLevel());
            } catch (Exception e) {
                log.error("持久化数据集时出错: {}", e.getMessage(), e);
            }
        }
        return dataset;
    }

    /**
     * 释放持久化的资源
     */
    private void unpersistDataset(Dataset<Row> dataset) {
        if (dataset != null) {
            try {
                dataset.unpersist();
                persistedDatasets.remove(dataset);
                log.debug("已释放持久化数据集");
            } catch (Exception e) {
                log.error("释放持久化数据集时出错: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 清理所有持久化的资源
     */
    private void clearAllPersistence() {
        persistedDatasets.forEach(dataset -> {
            try {
                if (dataset != null) {
                    dataset.unpersist();
                }
            } catch (Exception e) {
                log.warn("清理持久化数据时出错: {}", e.getMessage());
            }
        });
        persistedDatasets.clear();
        log.info("已清理所有持久化数据");
    }

    /**
     * 处理流数据
     */
    private void processStreamingData() {
        try {
            // 从Kafka读取数据流
            Dataset<Row> kafkaStream = sparkSession
                    .readStream()
                    .format("kafka")
                    .option("kafka.bootstrap.servers", "kafka:9092")
                    .option("subscribe", "boss_data_jobs")
                    .option("startingOffsets", "earliest")
                    .option("failOnDataLoss", "false")
                    .load();

            // 在集群模式下，设置watermark以处理迟到的数据
            if (sparkConfig.isClusterMode()) {
                kafkaStream = kafkaStream.withWatermark("timestamp", "1 hours");
            }

            // 将二进制value转换为字符串
            Dataset<Row> valueDf = kafkaStream.selectExpr("CAST(value AS STRING) AS json_value");

            // 定义JSON数据的Schema
            StructType jobSchema = createJobSchema();

            // 解析JSON数据
            Dataset<Row> parsedDf = valueDf.select(
                    from_json(col("json_value"), jobSchema).alias("job")
            );

            // 提取需要分析的数据
            Dataset<Row> jobDf = parsedDf.select(
                    col("job.data.position_name").alias("职位名称"),
                    col("job.data.company_name").alias("公司地点"),
                    col("job.data.detail_data.basicInfo.city").alias("城市"),
                    col("job.data.detail_data.basicInfo.salary").alias("薪资"),
                    col("job.data.detail_data.basicInfo.degree").alias("学历"),
                    col("job.data.detail_data.updateTime").alias("信息有效时间"),
                    col("job.data.detail_data.companyInfo.industry").alias("行业"),
                    col("job.data.detail_data.companyInfo.companySize").alias("公司规模"),
                    col("job.data.detail_data.companyInfo.establishDate").alias("公司创建时间"),
                    col("job.data.detail_data.jobTags").alias("职位标签")
            );

            // 处理每批次数据
            query = jobDf.writeStream()
                    .outputMode("append")
                    .option("checkpointLocation", sparkConfig.getDataStoragePath("kafka-checkpoint"))
                    .foreachBatch(new JobBatchProcessor())
                    .start();

            // 在单独的线程中等待查询终止
            new Thread(() -> {
                try {
                    query.awaitTermination();
                } catch (StreamingQueryException e) {
                    log.error("Spark流处理异常: {}", e.getMessage(), e);
                }
            }, "spark-streaming-job").start();

        } catch (Exception e) {
            log.error("创建Spark流处理时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 停止Spark任务
     */
    public void stopSparkJob() {
        if (query != null && query.isActive()) {
            try {
                query.stop();
                // 清理持久化资源
                clearAllPersistence();
                log.info("Spark任务已停止并清理资源");
            } catch (TimeoutException e) {
                log.error("停止Spark任务时超时: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 应用停止时自动清理资源
     */
    @PreDestroy
    public void cleanup() {
        stopSparkJob();
    }

    /**
     * 创建Job Schema
     */
    private StructType createJobSchema() {
        // 创建basicInfo、companyInfo、jobDescription的MapType
        DataType mapStringType = DataTypes.createMapType(DataTypes.StringType, DataTypes.StringType);

        // 创建详细数据的结构
        StructType detailDataType = new StructType()
                .add("jobTags", DataTypes.createArrayType(DataTypes.StringType))
                .add("basicInfo", mapStringType)
                .add("updateTime", DataTypes.StringType)
                .add("companyInfo", mapStringType)
                .add("jobDescription", mapStringType);

        // 创建data字段的结构
        StructType dataType = new StructType()
                .add("id", DataTypes.IntegerType)
                .add("position_id", DataTypes.StringType)
                .add("position_name", DataTypes.StringType)
                .add("company_id", DataTypes.StringType)
                .add("company_name", DataTypes.StringType)
                .add("detail_data", detailDataType)
                .add("gmt_create", DataTypes.StringType)
                .add("gmt_modified", DataTypes.StringType)
                .add("is_deleted", DataTypes.IntegerType);

        // 创建根结构
        return new StructType()
                .add("database", DataTypes.StringType)
                .add("table", DataTypes.StringType)
                .add("type", DataTypes.StringType)
                .add("ts", DataTypes.IntegerType)
                .add("xid", DataTypes.IntegerType)
                .add("commit", DataTypes.StringType)
                .add("data", dataType);
    }

    /**
     * 保存数据到Redis
     */
    private void saveToRedis(Dataset<Row> df) {
        // 在集群模式下，考虑数据分区进行批量保存以提高性能
        if (sparkConfig.isClusterMode()) {
            try {
                // 收集前先强制执行一次分区内计算以减少内存压力
                df.cache().count();
                
                // 确保合理的分区数以避免OOM
                int partitionCount = (int)Math.min(df.count() / 1000 + 1, 20);
                Dataset<Row> repartitionedDf = df.repartition(partitionCount);
                
                // 按分区批量处理
                repartitionedDf.foreachPartition(rows -> {
                    List<Row> partitionRows = new ArrayList<>();
                    while (rows.hasNext()) {
                        partitionRows.add(rows.next());
                    }
                    batchSaveToRedis(partitionRows);
                });
                
                log.info("集群模式：已将{}条记录分{}个分区保存到Redis", df.count(), partitionCount);
            } catch (Exception e) {
                log.error("集群模式保存数据到Redis出错: {}", e.getMessage(), e);
            }
        } else {
            // 单机模式保持原有逻辑
            try {
                List<Row> rows = df.collectAsList();
                batchSaveToRedis(rows);
                log.info("成功将{}条记录保存到Redis", rows.size());
            } catch (Exception e) {
                log.error("保存到Redis时出错: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 批量保存数据到Redis
     */
    private void batchSaveToRedis(List<Row> rows) {
        try {
            // 删除旧数据
            redisCache.deleteObject(sparkConfig.getRedisKeyPrefix() + "*");

            // 按维度存储数据
            // 1. 存储职位维度汇总数据
            Map<String, Map<String, Map<String, Row>>> positionData = new HashMap<>();

            // 按职位名称、年月、城市进行分组
            for (Row row : rows) {
                String position = row.getAs("职位名称");
                String validTime = row.getAs("有效时间年月");
                String city = row.getAs("城市");

                // 创建嵌套Map结构
                positionData.computeIfAbsent(position, k -> new HashMap<>())
                          .computeIfAbsent(validTime, k -> new HashMap<>())
                          .put(city, row);
            }

            // 存储职位索引集合
            String positionsKey = sparkConfig.getRedisKeyPrefix() + "positions";
            redisCache.setCacheObject(positionsKey, new ArrayList<>(positionData.keySet()));
            redisCache.expire(positionsKey, redisDataExpireDays, TimeUnit.DAYS);

            // 存储时间索引集合
            String timePeriodsKey = sparkConfig.getRedisKeyPrefix() + "time_periods";
            Set<String> allTimePeriods = new HashSet<>();
            positionData.values().forEach(timeMap -> allTimePeriods.addAll(timeMap.keySet()));
            redisCache.setCacheObject(timePeriodsKey, new ArrayList<>(allTimePeriods));
            redisCache.expire(timePeriodsKey, redisDataExpireDays, TimeUnit.DAYS);

            // 存储城市索引集合
            String citiesKey = sparkConfig.getRedisKeyPrefix() + "cities";
            Set<String> allCities = new HashSet<>();
            positionData.values().forEach(timeMap ->
                timeMap.values().forEach(cityMap ->
                    allCities.addAll(cityMap.keySet())));
            redisCache.setCacheObject(citiesKey, new ArrayList<>(allCities));
            redisCache.expire(citiesKey, redisDataExpireDays, TimeUnit.DAYS);

            // 2. 存储详细数据 - 使用Hash结构
            for (Row row : rows) {
                try {
                    // 获取关键字段
                    String position = row.getAs("职位名称");
                    String validTime = row.getAs("有效时间年月");
                    String city = row.getAs("城市");

                    // 构建唯一的Redis键
                    String redisHashKey = sparkConfig.getRedisKeyPrefix() + "data:" + position;
                    String hashField = validTime + ":" + city;

                    // 将Row对象转换为Map
                    Map<String, Object> rowMap = new HashMap<>();
                    for (String fieldName : row.schema().fieldNames()) {
                        if (row.getAs(fieldName) != null) {
                            rowMap.put(fieldName, row.getAs(fieldName));
                        }
                    }

                    // 转换为JSON字符串
                    String rowJson = gson.toJson(rowMap);

                    // 使用Hash存储
                    redisCache.setCacheMapValue(redisHashKey, hashField, rowJson);
                    redisCache.expire(redisHashKey, redisDataExpireDays, TimeUnit.DAYS);

                    // 存储职位-时间索引
                    String posTimeKey = sparkConfig.getRedisKeyPrefix() + "pos_time:" + position + ":" + validTime;
                    redisCache.setCacheSet(posTimeKey, Collections.singleton(city));
                    redisCache.expire(posTimeKey, redisDataExpireDays, TimeUnit.DAYS);

                    // 存储职位-城市索引
                    String posCityKey = sparkConfig.getRedisKeyPrefix() + "pos_city:" + position + ":" + city;
                    redisCache.setCacheSet(posCityKey, Collections.singleton(validTime));
                    redisCache.expire(posCityKey, redisDataExpireDays, TimeUnit.DAYS);

                    // 存储时间-城市索引
                    String timeCityKey = sparkConfig.getRedisKeyPrefix() + "time_city:" + validTime + ":" + city;
                    redisCache.setCacheSet(timeCityKey, Collections.singleton(position));
                    redisCache.expire(timeCityKey, redisDataExpireDays, TimeUnit.DAYS);

                } catch (Exception e) {
                    log.error("处理数据时出错: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("批量保存到Redis时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 批处理类
     * 需要实现Serializable以支持集群间序列化
     */
    private class JobBatchProcessor implements VoidFunction2<Dataset<Row>, Long>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public void call(Dataset<Row> df, Long epochId) {
            if (df.count() > 0) {
                // 在集群模式下，对数据进行合理分区，提高处理效率
                if (sparkConfig.isClusterMode()) {
                    // 估计一个合理的分区数
                    long rowCount = df.count();
                    int partitions = (int)Math.min(Math.max(rowCount / 10000, 1), sparkConfig.getDefaultParallelism());
                    df = df.repartition(partitions);
                    log.info("集群模式，批次数据重分区，记录数: {}, 分区数: {}", rowCount, partitions);
                }
                
                // 处理有效时间，只保留年月
                df = df.withColumn("有效时间年月",
                       date_format(to_date(col("信息有效时间"), "yyyy-MM-dd"), "yyyy-MM"));

                // 处理薪资，提取最低和最高薪资
                df = df.withColumn("最低薪资", regexp_extract(col("薪资"), "([0-9]+)-([0-9]+)K", 1).cast("integer"));
                df = df.withColumn("最高薪资", regexp_extract(col("薪资"), "([0-9]+)-([0-9]+)K", 2).cast("integer"));

                // 确保提取到的薪资值有效
                df = df.withColumn("最低薪资", when(col("最低薪资").isNull().or(col("最低薪资").equalTo(0)), 0).otherwise(col("最低薪资")));
                df = df.withColumn("最高薪资", when(col("最高薪资").isNull().or(col("最高薪资").equalTo(0)), 0).otherwise(col("最高薪资")));

                // 计算每个职位的平均薪资 (最低+最高)/2
                df = df.withColumn("平均薪资", when(col("最低薪资").equalTo(0).and(col("最高薪资").equalTo(0)), 0)
                        .otherwise((col("最低薪资").plus(col("最高薪资"))).divide(2)));

                // 处理学历，将null值转为"不限"
                df = df.withColumn("学历", when(col("学历").isNull(), "不限").otherwise(col("学历")));

                // 预处理后的数据进行持久化
                df = persistDataset(df, "preprocessed_data_" + epochId);
                
                // 合并当前数据和累积数据
                if (accumulatedData == null) {
                    accumulatedData = df;
                    log.info("初始化累积数据");
                } else {
                    // 先将当前累积数据解除持久化，避免内存泄漏
                    if (persistedDatasets.contains(accumulatedData)) {
                        unpersistDataset(accumulatedData);
                    }
                    
                    // 在集群模式下，需要重新分区以保证性能
                    if (sparkConfig.isClusterMode()) {
                        long totalCount = accumulatedData.count() + df.count();
                        int optimalPartitions = (int)Math.min(Math.max(totalCount / 10000, 4), sparkConfig.getDefaultParallelism());
                        
                        // 对两个数据集使用相同的分区键进行重分区，以提高JOIN效率
                        accumulatedData = accumulatedData.repartition(
                                col("职位名称"), col("有效时间年月"), col("城市"));
                        df = df.repartition(col("职位名称"), col("有效时间年月"), col("城市"));
                        
                        log.info("集群模式重分区，累积数据:{} 条，新数据:{} 条，分区数:{}",
                                accumulatedData.count(), df.count(), optimalPartitions);
                    }
                    
                    accumulatedData = accumulatedData.union(df);
                    log.info("合并新数据，当前累积记录数: {}", accumulatedData.count());
                }
                
                // 持久化累积数据
                accumulatedData = persistDataset(accumulatedData, "accumulated_data");
                
                // 将累积数据保存到checkpoint
                try {
                    // 在集群模式下，使用适当的分区数保存
                    if (sparkConfig.isClusterMode()) {
                        int savePartitions = Math.min((int)(accumulatedData.count() / 50000) + 1, 10);
                        accumulatedData.repartition(savePartitions)
                                .write()
                                .mode("overwrite")
                                .parquet(sparkConfig.getDataStoragePath("accumulated_data"));
                        log.info("集群模式：已将累积数据({}条)保存到checkpoint目录，使用{}个分区", 
                                accumulatedData.count(), savePartitions);
                    } else {
                        accumulatedData.write()
                                .mode("overwrite")
                                .parquet(sparkConfig.getDataStoragePath("accumulated_data"));
                        log.info("已将累积数据保存到checkpoint目录");
                    }
                } catch (Exception e) {
                    log.error("保存累积数据到checkpoint时出错: {}", e.getMessage(), e);
                }

                // 薪资统计分析 - 使用简单聚合而非加权平均
                Dataset<Row> salaryAnalysis = accumulatedData.groupBy("职位名称", "有效时间年月", "城市").agg(
                        min("最低薪资").alias("最低薪资K"),
                        max("最高薪资").alias("最高薪资K"),
                        round(avg("平均薪资"), 1).alias("_平均薪资")
                );
                
                // 持久化薪资分析结果
                salaryAnalysis = persistDataset(salaryAnalysis, "salary_analysis");

                // 确保平均薪资在最低和最高薪资范围内
                salaryAnalysis = salaryAnalysis.withColumn("平均薪资K",
                        when(col("_平均薪资").gt(col("最高薪资K")), col("最高薪资K"))
                        .when(col("_平均薪资").lt(col("最低薪资K")), col("最低薪资K"))
                        .otherwise(col("_平均薪资")))
                        .drop("_平均薪资");

                // 2. 基本信息汇总
                Dataset<Row> positionAnalysis = accumulatedData.groupBy("职位名称", "有效时间年月", "城市").agg(
                        count("*").alias("职位数量"),
                        array_join(collect_set("行业"), "、").alias("所属行业")
                );
                
                // 持久化职位分析结果
                positionAnalysis = persistDataset(positionAnalysis, "position_analysis");

                // 3. 学历分析
                // 先对每种学历进行分组计数
                Dataset<Row> educationAnalysis = accumulatedData.groupBy("职位名称", "有效时间年月", "城市", "学历")
                        .count()
                        .withColumnRenamed("count", "数量");
                
                // 持久化学历分析中间结果
                educationAnalysis = persistDataset(educationAnalysis, "education_analysis");

                // 对每种学历数据进行透视，生成列名为"学历_X"的列
                Dataset<Row> pivotedEducation = educationAnalysis
                        .groupBy("职位名称", "有效时间年月", "城市")
                        .pivot("学历")
                        .sum("数量");
                
                // 持久化透视后的学历分析结果
                pivotedEducation = persistDataset(pivotedEducation, "pivoted_education");

                // 将所有列重命名为"学历_X"格式
                for (String colName : pivotedEducation.columns()) {
                    if (!colName.equals("职位名称") && !colName.equals("有效时间年月") && !colName.equals("城市")) {
                        pivotedEducation = pivotedEducation.withColumnRenamed(colName, "学历_" + colName);
                    }
                }

                // 合并所需的字段到最终结果
                Dataset<Row> finalResult = positionAnalysis
                        .join(salaryAnalysis, new String[]{"职位名称", "有效时间年月", "城市"}, "left")
                        .join(pivotedEducation, new String[]{"职位名称", "有效时间年月", "城市"}, "left")
                        .orderBy("职位名称", "有效时间年月", "城市");
                
                // 持久化最终合并结果
                Dataset<Row> processedResult = persistDataset(finalResult, "final_result");

                // 处理null值
                Map<String, Object> valuesToFill = new HashMap<>();
                valuesToFill.put("所属行业", "未指定");
                // 所有学历计数字段的null值填充为0
                for (String colName : processedResult.columns()) {
                    if (colName.startsWith("学历_")) {
                        valuesToFill.put(colName, 0);
                    }
                }

                final Dataset<Row> finalResultData = processedResult.na().fill(valuesToFill);
                
                // 保存最终结果到checkpoint目录
                try {
                    if (sparkConfig.isClusterMode()) {
                        // 集群模式下使用合适的分区数
                        int savePartitions = Math.min((int)(finalResultData.count() / 10000) + 1, 5);
                        finalResultData.repartition(savePartitions)
                                .write()
                                .mode("overwrite")
                                .parquet(sparkConfig.getDataStoragePath("final_result"));
                        log.info("集群模式：已将最终分析结果({}条)保存到checkpoint目录，使用{}个分区", 
                                finalResultData.count(), savePartitions);
                    } else {
                        finalResultData.write()
                                .mode("overwrite")
                                .parquet(sparkConfig.getDataStoragePath("final_result"));
                        log.info("已将最终分析结果保存到checkpoint目录");
                    }
                } catch (Exception e) {
                    log.error("保存最终结果到checkpoint时出错: {}", e.getMessage(), e);
                }

                // 将结果保存到Redis
                saveToRedis(finalResultData);
                
                // 清理这个批次的中间结果，保留accumulatedData和finalResult
                // 创建要清理的数据集合的副本，避免ConcurrentModificationException
                Set<Dataset<Row>> datasetsToUnpersist = new HashSet<>(persistedDatasets);
                datasetsToUnpersist.stream()
                    .filter(ds -> ds != accumulatedData && ds != finalResultData)
                    .forEach(this::unpersistSafely);
            }
        }
        
        /**
         * 安全地解除Dataset持久化
         */
        private void unpersistSafely(Dataset<Row> dataset) {
            try {
                unpersistDataset(dataset);
            } catch (Exception e) {
                log.warn("解除Dataset持久化时出错: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 从checkpoint恢复累积数据
     */
    public void recoverFromCheckpoint() {
        try {
            // 检查checkpoint目录中是否存在累积数据
            String dataPath = sparkConfig.getDataStoragePath("accumulated_data");
            log.info("尝试从路径恢复数据: {}", dataPath);
            
            // 在读取前检查路径是否存在
            try {
                // 尝试读取，如果路径不存在会抛出异常
                sparkSession.read().parquet(dataPath).count();
            } catch (Exception e) {
                log.info("没有找到之前的累积数据，将创建新的数据集");
                return;
            }
            
            Dataset<Row> checkpointData = sparkSession.read().parquet(dataPath);
            
            if (checkpointData != null && checkpointData.count() > 0) {
                // 集群模式下需要重新分区
                if (sparkConfig.isClusterMode()) {
                    long rowCount = checkpointData.count();
                    int partitions = (int)Math.min(Math.max(rowCount / 10000, 2), sparkConfig.getDefaultParallelism());
                    checkpointData = checkpointData.repartition(partitions);
                    log.info("集群模式：从checkpoint恢复累积数据并重分区，记录数: {}, 分区数: {}", 
                            rowCount, partitions);
                }
                
                accumulatedData = checkpointData;
                persistDataset(accumulatedData, "recovered_accumulated_data");
                log.info("已从checkpoint恢复累积数据，记录数: {}", accumulatedData.count());
            }
        } catch (Exception e) {
            log.warn("从checkpoint恢复数据时出错: {}", e.getMessage());
            log.info("将使用空的累积数据继续处理");
            // 异常时不抛出，只记录日志，确保程序正常启动
        }
    }
}
