package com.david.hlp.spark.service.Log;

import com.david.hlp.spark.utils.RedisCache;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
import javax.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessLogService {

        private final RedisCache redisCache;
        private final SparkSession sparkSession;

        private static final String REDIS_ACCESS_STATS_KEY = "access_log:stats";
        private static final String REDIS_IP_STATS_KEY = "access_log:ip_stats";
        private static final String REDIS_URL_STATS_KEY = "access_log:url_stats";
        private static final String REDIS_ANOMALY_STATS_KEY = "access_log:anomaly_stats";
        private static final String REDIS_LOAD_STATS_KEY = "access_log:load_stats";
        private static final String REDIS_TIME_STATS_KEY = "access_log:time_stats";
        private static final int CACHE_EXPIRE_HOURS = 24;

        @PostConstruct
        public void init() {
                StartSpark();
        }

        @Scheduled(fixedRate = 60 * 60 * 1000)
        private void StartSpark() {
                log.info("开始执行日志分析任务...");
                String logsDir = "hdfs://hadoop-single:9000/logs";

                try {
                        // 读取并解析日志数据
                        Dataset<Row> parsedLogs = loadAndParseLogs(logsDir);

                        // 过滤掉 OPTIONS 请求
                        Dataset<Row> nonOptionsLogs = parsedLogs.filter(functions.col("method").notEqual("OPTIONS"));
                        nonOptionsLogs.cache();

                        // 执行各项分析
                        analyzeBasicStats(nonOptionsLogs);
                        analyzeApiPatterns(nonOptionsLogs);
                        analyzeUserBehavior(nonOptionsLogs);
                        analyzeAnomalies(nonOptionsLogs);
                        analyzeSystemLoad(nonOptionsLogs);
                        analyzeTimeBasedStats(nonOptionsLogs);

                        nonOptionsLogs.unpersist();
                        log.info("日志分析任务完成！");

                } catch (Exception e) {
                        log.error("日志分析任务执行失败: {}", e.getMessage(), e);
                        throw new RuntimeException("日志分析任务失败", e);
                }
        }

        private Dataset<Row> loadAndParseLogs(String logsDir) {
                return sparkSession.read()
                                .option("mode", "PERMISSIVE")
                                .option("columnNameOfCorruptRecord", "_corrupt_record")
                                .text(logsDir + "/*.log")
                                // 过滤出包含 "ACCESS|" 标记的自定义日志行，忽略基础SpringBoot日志
                                .filter(functions.col("value").contains("ACCESS|"))
                                .select(
                                                functions.regexp_extract(functions.col("value"),
                                                                "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})",
                                                                1).as("timestamp"),
                                                functions.regexp_extract(functions.col("value"), "ip=([^|]+)", 1)
                                                                .as("ip"),
                                                functions.regexp_extract(functions.col("value"), "path=([^|]+)", 1)
                                                                .as("path"),
                                                functions.regexp_extract(functions.col("value"), "method=([^|]+)", 1)
                                                                .as("method"),
                                                functions.regexp_extract(functions.col("value"), "ua=([^|]+)", 1)
                                                                .as("user_agent"))
                                .withColumn("timestamp",
                                                functions.to_timestamp(functions.col("timestamp"),
                                                                "yyyy-MM-dd HH:mm:ss"))
                                .withColumn("date", functions.date_format(functions.col("timestamp"), "yyyy-MM-dd"))
                                .withColumn("hour", functions.hour(functions.col("timestamp")))
                                .withColumn("minute", functions.minute(functions.col("timestamp")))
                                .withColumn("api_category", functions.regexp_extract(functions.col("path"),
                                                "/api/boss/([^/]+)", 1));
        }

        private void analyzeBasicStats(Dataset<Row> logs) {
                log.info("开始基础统计分析...");

                // 基本请求统计
                long totalRequests = logs.count();
                long uniqueIPs = logs.select("ip").distinct().count();
                long uniquePaths = logs.select("path").distinct().count();

                // API类别分布
                Dataset<Row> apiCategoryStats = logs.groupBy("api_category")
                                .count()
                                .sort(functions.col("count").desc());

                // 热门API路径
                Dataset<Row> popularPaths = logs.groupBy("path")
                                .count()
                                .sort(functions.col("count").desc())
                                .limit(10);

                // 缓存结果
                Map<String, Object> basicStats = new HashMap<>();
                basicStats.put("totalRequests", totalRequests);
                basicStats.put("uniqueIPs", uniqueIPs);
                basicStats.put("uniquePaths", uniquePaths);
                basicStats.put("apiCategoryStats",
                                convertRowsToMap(apiCategoryStats.collectAsList(), "api_category", "count"));
                basicStats.put("popularPaths", convertRowsToMap(popularPaths.collectAsList(), "path", "count"));

                redisCache.setCacheMap(REDIS_ACCESS_STATS_KEY, basicStats);
                redisCache.expire(REDIS_ACCESS_STATS_KEY, CACHE_EXPIRE_HOURS * 3600);
        }

        private void analyzeApiPatterns(Dataset<Row> logs) {
                log.info("开始API使用模式分析...");

                // 按日期时间的API访问路径统计
                Dataset<Row> pathDateStats = logs.groupBy("date", "path")
                                .count()
                                .sort(functions.col("date"), functions.col("count").desc());

                // 按日期时间的HTTP方法分布
                Dataset<Row> methodDateStats = logs.groupBy("date", "method")
                                .count()
                                .sort(functions.col("date"), functions.col("count").desc());

                // API关联分析
                Dataset<Row> apiSequence = logs.select("ip", "timestamp", "path")
                                .sort(functions.col("ip"), functions.col("timestamp"));

                Map<String, Object> apiPatterns = new HashMap<>();
                apiPatterns.put("pathDateStats", convertRowsToMap(pathDateStats.collectAsList(), "path", "count"));
                apiPatterns.put("methodDateStats",
                                convertRowsToMap(methodDateStats.collectAsList(), "method", "count"));
                apiPatterns.put("apiSequence", convertRowsToMap(apiSequence.collectAsList(), "ip", "path"));

                redisCache.setCacheMap(REDIS_URL_STATS_KEY, apiPatterns);
                redisCache.expire(REDIS_URL_STATS_KEY, CACHE_EXPIRE_HOURS * 3600);
        }

        private void analyzeUserBehavior(Dataset<Row> logs) {
                log.info("开始用户行为分析...");

                // 访问频率最高的IP
                Dataset<Row> ipStats = logs.groupBy("ip")
                                .count()
                                .sort(functions.col("count").desc())
                                .limit(10);

                // IP和路径组合分析
                Dataset<Row> ipPathStats = logs.groupBy("ip", "path")
                                .count()
                                .sort(functions.col("count").desc())
                                .limit(20);

                // 用户代理分析
                Dataset<Row> uaStats = logs.groupBy("user_agent")
                                .count()
                                .sort(functions.col("count").desc())
                                .limit(10);

                Map<String, Object> userBehavior = new HashMap<>();
                userBehavior.put("ipStats", convertRowsToMap(ipStats.collectAsList(), "ip", "count"));
                userBehavior.put("ipPathStats", convertRowsToMap(ipPathStats.collectAsList(), "ip", "count"));
                userBehavior.put("uaStats", convertRowsToMap(uaStats.collectAsList(), "user_agent", "count"));

                redisCache.setCacheMap(REDIS_IP_STATS_KEY, userBehavior);
                redisCache.expire(REDIS_IP_STATS_KEY, CACHE_EXPIRE_HOURS * 3600);
        }

        private void analyzeAnomalies(Dataset<Row> logs) {
                log.info("开始异常检测分析...");

                // 按分钟统计请求数
                Dataset<Row> minuteStats = logs.groupBy("date", "hour", "minute")
                                .count()
                                .sort(functions.col("date"), functions.col("hour"), functions.col("minute"));

                // 计算统计指标
                Row stats = minuteStats.agg(
                                functions.avg("count").cast("double").as("avg_requests"),
                                functions.stddev("count").cast("double").as("stddev_requests"),
                                functions.expr("percentile_approx(count, 0.95)").cast("double").as("p95_requests"))
                                .first();

                // 安全地获取数值，避免类型转换异常
                double avgVal = stats.get(0) instanceof Double ? stats.getDouble(0)
                                : ((Number) stats.get(0)).doubleValue();
                double stddevVal = stats.get(1) instanceof Double ? stats.getDouble(1)
                                : ((Number) stats.get(1)).doubleValue();
                double p95Val = stats.get(2) instanceof Double ? stats.getDouble(2)
                                : ((Number) stats.get(2)).doubleValue();
                double stdThreshold = avgVal + 2 * stddevVal;

                // 检测异常值
                Dataset<Row> stdAnomalies = minuteStats.filter(functions.col("count").gt(stdThreshold));
                Dataset<Row> p95Anomalies = minuteStats.filter(functions.col("count").gt(p95Val));

                Map<String, Object> anomalies = new HashMap<>();
                anomalies.put("avgRequests", avgVal);
                anomalies.put("stddevRequests", stddevVal);
                anomalies.put("p95Requests", p95Val);
                anomalies.put("stdThreshold", stdThreshold);
                anomalies.put("stdAnomalies", convertRowsToMap(stdAnomalies.collectAsList(), "date", "count"));
                anomalies.put("p95Anomalies", convertRowsToMap(p95Anomalies.collectAsList(), "date", "count"));

                redisCache.setCacheMap(REDIS_ANOMALY_STATS_KEY, anomalies);
                redisCache.expire(REDIS_ANOMALY_STATS_KEY, CACHE_EXPIRE_HOURS * 3600);
        }

        private void analyzeSystemLoad(Dataset<Row> logs) {
                log.info("开始系统负载分析...");

                // 每小时请求数
                Dataset<Row> hourlyStats = logs.groupBy("hour")
                                .count()
                                .withColumnRenamed("count", "request_count");

                // 每小时平均请求数
                Dataset<Row> hourlyAvg = hourlyStats
                                .agg(functions.avg("request_count").as("avg_requests"))
                                .withColumn("hour", functions.lit("hourly_avg"));

                // 每分钟峰值请求数
                Dataset<Row> peakMinute = logs.groupBy("date", "hour", "minute")
                                .count()
                                .sort(functions.col("count").desc())
                                .limit(5);

                Map<String, Object> loadStats = new HashMap<>();
                loadStats.put("hourlyAvg", convertRowsToMap(hourlyAvg.collectAsList(), "hour", "avg_requests"));
                loadStats.put("peakMinute", convertRowsToMap(peakMinute.collectAsList(), "date", "count"));

                redisCache.setCacheMap(REDIS_LOAD_STATS_KEY, loadStats);
                redisCache.expire(REDIS_LOAD_STATS_KEY, CACHE_EXPIRE_HOURS * 3600);
        }

        private Map<String, Object> convertRowsToMap(List<Row> rows, String keyField, String valueField) {
                Map<String, Object> result = new HashMap<>();
                for (Row row : rows) {
                        result.put(row.getAs(keyField).toString(), row.getAs(valueField));
                }
                return result;
        }

        // Getter methods for cached results
        public Map<String, Object> getBasicStats() {
                return redisCache.getCacheMap(REDIS_ACCESS_STATS_KEY);
        }

        public Map<String, Object> getApiPatterns() {
                return redisCache.getCacheMap(REDIS_URL_STATS_KEY);
        }

        public Map<String, Object> getUserBehavior() {
                return redisCache.getCacheMap(REDIS_IP_STATS_KEY);
        }

        public Map<String, Object> getAnomalies() {
                return redisCache.getCacheMap(REDIS_ANOMALY_STATS_KEY);
        }

        public Map<String, Object> getSystemLoad() {
                return redisCache.getCacheMap(REDIS_LOAD_STATS_KEY);
        }

        public Map<String, Object> getTimeBasedStats() {
                return redisCache.getCacheMap(REDIS_TIME_STATS_KEY);
        }

        /**
         * 基于时间维度的细粒度分析
         * 包括分钟级、小时级和日期级的请求统计
         */
        private void analyzeTimeBasedStats(Dataset<Row> logs) {
                log.info("开始时间维度细粒度分析...");

                // 添加日期时间相关列
                Dataset<Row> logsWithTime = logs
                                .withColumn("date_hour", functions.concat(functions.col("date"),
                                                functions.lit(" "),
                                                functions.lpad(functions.col("hour").cast("string"), 2, "0")))
                                .withColumn("date_hour_minute", functions.concat(functions.col("date_hour"),
                                                functions.lit(":"),
                                                functions.lpad(functions.col("minute").cast("string"), 2, "0")))
                                .withColumn("weekday", functions.date_format(functions.col("timestamp"), "E"))
                                .withColumn("week_of_year", functions.weekofyear(functions.col("timestamp")))
                                .withColumn("month", functions.month(functions.col("timestamp")))
                                .withColumn("day", functions.dayofmonth(functions.col("timestamp")));

                // 1. 分钟级统计 - 最近24小时内的每分钟请求量
                Dataset<Row> minuteStats = logsWithTime
                                .filter(functions.col("timestamp")
                                                .gt(functions.date_sub(functions.current_timestamp(), 1)))
                                .groupBy("date_hour_minute")
                                .count()
                                .sort("date_hour_minute");

                // 2. 小时级统计 - 按天分组的每小时请求量
                Dataset<Row> hourlyStats = logsWithTime
                                .groupBy("date", "hour")
                                .count()
                                .withColumn("hour_key", functions.concat(functions.col("date"),
                                                functions.lit(" "),
                                                functions.lpad(functions.col("hour").cast("string"), 2, "0")))
                                .sort("date", "hour");

                // 3. 每日统计 - 过去30天的每日请求量
                Dataset<Row> dailyStats = logsWithTime
                                .groupBy("date")
                                .count()
                                .sort("date");

                // 4. 按周分析 - 每周的请求分布
                Dataset<Row> weekdayStats = logsWithTime
                                .groupBy("weekday")
                                .count()
                                .sort(functions.col("count").desc());

                // 5. 按月分析 - 每月的请求分布
                Dataset<Row> monthlyStats = logsWithTime
                                .groupBy("month")
                                .count()
                                .sort("month");

                // 6. 按小时统计平均值和峰值 - 业务时间分析
                // 先按小时和分钟分组计算每个时间点的请求数
                Dataset<Row> requestCountsByMinute = logsWithTime
                                .groupBy("hour", "minute")
                                .agg(functions.count("*").as("request_count"));

                // 再按小时聚合，计算平均值和最大值
                Dataset<Row> hourPeaks = requestCountsByMinute
                                .groupBy("hour")
                                .agg(
                                                functions.count("*").as("total_minutes"),
                                                functions.sum("request_count").as("total_requests"),
                                                functions.avg("request_count").as("avg_requests"),
                                                functions.max("request_count").as("max_requests"))
                                .sort("hour");

                // 7. 热力图数据 - 日期和小时的二维分布
                Dataset<Row> heatmapData = logsWithTime
                                .groupBy("date", "hour")
                                .count()
                                .sort("date", "hour");

                // 收集并缓存所有结果
                Map<String, Object> timeStats = new HashMap<>();
                timeStats.put("minuteStats",
                                convertRowsToMap(minuteStats.collectAsList(), "date_hour_minute", "count"));
                timeStats.put("hourlyStats", convertRowsToMap(hourlyStats.collectAsList(), "hour_key", "count"));
                timeStats.put("dailyStats", convertRowsToMap(dailyStats.collectAsList(), "date", "count"));
                timeStats.put("weekdayStats", convertRowsToMap(weekdayStats.collectAsList(), "weekday", "count"));
                timeStats.put("monthlyStats", convertRowsToMap(monthlyStats.collectAsList(), "month", "count"));
                timeStats.put("hourPeaks", convertRowsToMap(hourPeaks.collectAsList(), "hour", "total_requests"));
                timeStats.put("heatmapData", convertMultiKeyRowsToMap(heatmapData.collectAsList(),
                                new String[] { "date", "hour" }, "count"));

                redisCache.setCacheMap(REDIS_TIME_STATS_KEY, timeStats);
                redisCache.expire(REDIS_TIME_STATS_KEY, CACHE_EXPIRE_HOURS * 3600);
        }

        /**
         * 将多键行数据转换为嵌套Map
         */
        private Map<String, Object> convertMultiKeyRowsToMap(List<Row> rows, String[] keyFields, String valueField) {
                Map<String, Object> result = new HashMap<>();
                for (Row row : rows) {
                        StringBuilder keyBuilder = new StringBuilder();
                        for (int i = 0; i < keyFields.length; i++) {
                                if (i > 0)
                                        keyBuilder.append("_");
                                keyBuilder.append(row.getAs(keyFields[i]).toString());
                        }
                        result.put(keyBuilder.toString(), row.getAs(valueField));
                }
                return result;
        }
}