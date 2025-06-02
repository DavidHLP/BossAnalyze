package com.david.hlp.spark.service.Log;

import com.david.hlp.spark.utils.IpGeoService;
import com.david.hlp.spark.utils.RedisCache;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogService {

    private final RedisCache redisCache;
    private final SparkSession sparkSession;
    private final IpGeoService ipGeoService;

    // Redis缓存键名
    private static final String REDIS_KEY_PREFIX = "access_log:analysis:";
    private static final String REDIS_HEATMAP_KEY = REDIS_KEY_PREFIX + "heatmap";
    private static final String REDIS_IP_STATS_KEY = REDIS_KEY_PREFIX + "ip_stats";
    private static final String REDIS_HTTP_METHODS_KEY = REDIS_KEY_PREFIX + "http_methods";
    private static final String REDIS_WEEKDAY_STATS_KEY = REDIS_KEY_PREFIX + "weekday_stats";
    private static final String REDIS_BROWSER_STATS_KEY = REDIS_KEY_PREFIX + "browser_stats";

    // 热力图字段名
    private static final String MINUTE_HEATMAP_FIELD = "minute_heatmap";
    private static final String HOUR_HEATMAP_FIELD = "hour_heatmap";

    // 缓存过期时间（小时）
    private static final int CACHE_EXPIRE_HOURS = 24;

    @Scheduled(fixedRate = 5 * 60 * 1000) // 每5分钟执行一次
    public void analyzeLogs() {
        log.info("开始执行日志分析任务...");
        String logsDir = "hdfs://hadoop-single:9000/logs";

        try {
            // 1. 读取并解析日志数据
            Dataset<Row> logs = loadAndParseLogs(logsDir);

            // 2. 分析访问热力图
            analyzeHeatMap(logs);

            // 3. 分析IP统计
            analyzeIpStats(logs);

            // 4. 分析HTTP方法
            analyzeHttpMethods(logs);

            // 5. 分析工作日模式
            analyzeWeekdayPatterns(logs);

            // 6. 分析浏览器使用情况
            analyzeBrowserUsage(logs);

            log.info("日志分析任务完成！");

        } catch (Exception e) {
            log.error("日志分析任务执行失败: {}", e.getMessage(), e);
        }
    }

    private Dataset<Row> loadAndParseLogs(String logsDir) {
        // 注册UDF函数
        sparkSession.udf().register("parse_timestamp",
                (String timestampStr) -> {
                    try {
                        return Long.parseLong(timestampStr);
                    } catch (NumberFormatException e) {
                        return 0L;
                    }
                },
                DataTypes.LongType);

        // 读取日志文件并解析
        return sparkSession.read()
                .option("mode", "PERMISSIVE")
                .option("columnNameOfCorruptRecord", "_corrupt_record")
                .text(logsDir + "/*.log")
                .filter(functions.col("value").contains("ACCESS|ts="))
                .select(
                        functions.regexp_extract(functions.col("value"), "ACCESS\\|ts=([^|]+)", 1).as("timestamp_str"),
                        functions.regexp_extract(functions.col("value"), "ip=([^|]+)", 1).as("ip"),
                        functions.regexp_extract(functions.col("value"), "path=([^|]+)", 1).as("path"),
                        functions.regexp_extract(functions.col("value"), "method=([^|]+)", 1).as("method"),
                        functions.regexp_extract(functions.col("value"), "ua=([^|]+)", 1).as("user_agent"))
                .withColumn("timestamp_ms", functions.callUDF("parse_timestamp", functions.col("timestamp_str")))
                .withColumn("timestamp",
                        functions.from_unixtime(functions.col("timestamp_ms").divide(1000), "yyyy-MM-dd HH:mm:ss"))
                .withColumn("date", functions.date_format(functions.col("timestamp"), "yyyy-MM-dd"))
                .withColumn("hour", functions.date_format(functions.col("timestamp"), "HH"))
                .withColumn("minute", functions.date_format(functions.col("timestamp"), "mm"))
                .withColumn("weekday", functions.dayofweek(functions.col("timestamp")));
    }

    private void analyzeHeatMap(Dataset<Row> logs) {
        // 分钟级热力图
        Dataset<Row> minuteHeatMap = logs
                .groupBy(
                        functions.date_format(functions.col("timestamp"), "yyyy-MM-dd HH:mm").as("time_key"))
                .count()
                .orderBy(functions.col("count").desc());

        // 小时级热力图
        Dataset<Row> hourHeatMap = logs
                .groupBy(
                        functions.date_format(functions.col("timestamp"), "yyyy-MM-dd HH").as("time_key"))
                .count()
                .orderBy(functions.col("count").desc());

        // 转换为Map
        Map<String, Object> minuteHeatMapData = convertToMap(minuteHeatMap.collectAsList(), "time_key", "count");
        Map<String, Object> hourHeatMapData = convertToMap(hourHeatMap.collectAsList(), "time_key", "count");

        // 创建嵌套的Map结构
        Map<String, Object> heatMapData = new HashMap<>();
        heatMapData.put(MINUTE_HEATMAP_FIELD, minuteHeatMapData);
        heatMapData.put(HOUR_HEATMAP_FIELD, hourHeatMapData);

        // 存储到Redis
        redisCache.setCacheMap(REDIS_HEATMAP_KEY, heatMapData);
        redisCache.expire(REDIS_HEATMAP_KEY, CACHE_EXPIRE_HOURS * 3600);
    }

    private void analyzeIpStats(Dataset<Row> logs) {
        // 统计IP访问次数
        Dataset<Row> ipStats = logs
                .groupBy("ip")
                .count()
                .orderBy(functions.col("count").desc());

        // 转换为Map并存入Redis
        Map<String, Object> ipData = new HashMap<>();
        List<String> localIps = new ArrayList<>();

        // 收集IP统计信息
        for (Row row : ipStats.collectAsList()) {
            String ip = row.getAs("ip");
            long count = row.getAs("count");

            // 获取IP地理信息
            Map<String, String> geoInfo = ipGeoService.getIpGeoInfo(ip);

            // 构建IP信息
            Map<String, Object> ipInfo = new HashMap<>();
            ipInfo.put("count", count);
            ipInfo.put("country_name", geoInfo.get("country_name"));
            ipInfo.put("city", geoInfo.get("city"));

            // 如果是本地IP，添加到本地IP列表
            if (ipGeoService.isLocalIp(ip)) {
                localIps.add(ip);
            }

            ipData.put(ip, ipInfo);
        }

        // 将本地IP列表存入结果中
        if (!localIps.isEmpty()) {
            ipData.put("local_networks", localIps);
        }

        // 保存到Redis
        redisCache.setCacheMap(REDIS_IP_STATS_KEY, ipData);
        redisCache.expire(REDIS_IP_STATS_KEY, CACHE_EXPIRE_HOURS * 3600);
    }

    private void analyzeHttpMethods(Dataset<Row> logs) {
        // 统计HTTP方法分布
        Dataset<Row> httpMethods = logs
                .groupBy("method")
                .count()
                .orderBy(functions.col("count").desc());

        // 转换为Map并存入Redis
        Map<String, Object> httpData = convertToMap(httpMethods.collectAsList(), "method", "count");
        redisCache.setCacheMap(REDIS_HTTP_METHODS_KEY, httpData);
        redisCache.expire(REDIS_HTTP_METHODS_KEY, CACHE_EXPIRE_HOURS * 3600);
    }

    private void analyzeWeekdayPatterns(Dataset<Row> logs) {
        // 统计工作日访问模式
        Dataset<Row> weekdayStats = logs
                .groupBy("weekday")
                .count()
                .orderBy("weekday");

        // 转换为Map并存入Redis
        Map<String, Object> weekdayData = convertToMap(weekdayStats.collectAsList(), "weekday", "count");
        redisCache.setCacheMap(REDIS_WEEKDAY_STATS_KEY, weekdayData);
        redisCache.expire(REDIS_WEEKDAY_STATS_KEY, CACHE_EXPIRE_HOURS * 3600);
    }

    private void analyzeBrowserUsage(Dataset<Row> logs) {
        // 分析浏览器使用情况
        Dataset<Row> browserStats = logs
                .withColumn("browser",
                        functions.when(functions.col("user_agent").contains("Chrome"), "Chrome")
                                .when(functions.col("user_agent").contains("Firefox"), "Firefox")
                                .when(functions.col("user_agent").contains("Safari"), "Safari")
                                .when(functions.col("user_agent").contains("Opera"), "Opera")
                                .when(functions.col("user_agent").contains("Edg"), "Edge")
                                .when(functions.col("user_agent").contains("MSIE")
                                        .or(functions.col("user_agent").contains("Trident")), "IE")
                                .otherwise("其他"))
                .groupBy("browser")
                .count()
                .orderBy(functions.col("count").desc());

        // 转换为Map并存入Redis
        Map<String, Object> browserData = convertToMap(browserStats.collectAsList(), "browser", "count");
        redisCache.setCacheMap(REDIS_BROWSER_STATS_KEY, browserData);
        redisCache.expire(REDIS_BROWSER_STATS_KEY, CACHE_EXPIRE_HOURS * 3600);
    }

    // 已迁移到IpGeoService中

    private Map<String, Object> convertToMap(List<Row> rows, String keyField, String valueField) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Row row : rows) {
            Object key = row.getAs(keyField);
            Object value = row.getAs(valueField);

            if (key != null) {
                result.put(key.toString(), value);
            }
        }

        return result;
    }
}