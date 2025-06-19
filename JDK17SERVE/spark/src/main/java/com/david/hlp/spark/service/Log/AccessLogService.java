package com.david.hlp.spark.service.Log;

import com.david.hlp.commons.entity.logs.*;
import com.david.hlp.commons.utils.LogAnalysisUtils;
import com.david.hlp.commons.utils.RedisCacheUtil;
import com.david.hlp.spark.utils.IpGeoService;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.DataTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogService {

    private final RedisCacheUtil redisCacheUtil;
    private final SparkSession sparkSession;
    private final IpGeoService ipGeoService;

    @Scheduled(fixedRate = 5 * 60 * 1000) // 每5分钟执行一次
    public void analyzeLogs() {
        log.info("开始执行日志分析任务...");
        String logsDir = "hdfs://hadoop-single:9000/logs/";

        try {
            LocalDateTime analysisStartTime = LocalDateTime.now();

            // 1. 读取并解析日志数据
            Dataset<Row> logs = loadAndParseLogs(logsDir);

            // 统计总访问量和唯一访客数
            long totalVisits = logs.count();
            long uniqueVisitors = logs.select("ip").distinct().count();

            // 2. 并行分析所有维度数据
            HeatMapData heatMapData = analyzeHeatMap(logs);
            IpStatistics ipStatistics = analyzeIpStats(logs);
            HttpMethodStatistics httpMethodStatistics = analyzeHttpMethods(logs);
            WeekdayStatistics weekdayStatistics = analyzeWeekdayPatterns(logs);
            BrowserStatistics browserStatistics = analyzeBrowserUsage(logs);

            // 3. 构建完整的分析结果
            AccessLogAnalysisResult analysisResult = AccessLogAnalysisResult.builder()
                    .analysisTime(analysisStartTime)
                    .dataStartTime(getDataStartTime(logs))
                    .dataEndTime(getDataEndTime(logs))
                    .totalVisits(totalVisits)
                    .uniqueVisitors(uniqueVisitors)
                    .heatMapData(heatMapData)
                    .ipStatistics(ipStatistics)
                    .httpMethodStatistics(httpMethodStatistics)
                    .weekdayStatistics(weekdayStatistics)
                    .browserStatistics(browserStatistics)
                    .dataQuality(calculateDataQuality(logs, totalVisits))
                    .build();

            // 4. 存储分析结果到Redis
            storeAnalysisResults(analysisResult);

            log.info("日志分析任务完成！总访问量: {}, 唯一访客: {}", totalVisits, uniqueVisitors);

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
                .text(logsDir)
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

    private HeatMapData analyzeHeatMap(Dataset<Row> logs) {
        // 分钟级热力图
        List<Row> minuteHeatMapRows = logs
                .groupBy(functions.date_format(functions.col("timestamp"), "yyyy-MM-dd HH:mm").as("time_key"))
                .count()
                .orderBy(functions.col("count").desc())
                .collectAsList();

        Map<String, HeatMapData.TimePointInfo> minuteHeatmap = minuteHeatMapRows.stream()
                .filter(row -> row.getAs("time_key") != null)
                .collect(Collectors.toMap(
                        row -> row.getAs("time_key"),
                        row -> HeatMapData.TimePointInfo.builder()
                                .count(((Long) row.getAs("count")).intValue())
                                .intensity(LogAnalysisUtils.calculateIntensity((Long) row.getAs("count")))
                                .build()));

        // 小时级热力图
        List<Row> hourHeatMapRows = logs
                .groupBy(functions.date_format(functions.col("timestamp"), "yyyy-MM-dd HH").as("time_key"))
                .count()
                .orderBy(functions.col("count").desc())
                .collectAsList();

        Map<String, HeatMapData.TimePointInfo> hourHeatmap = hourHeatMapRows.stream()
                .filter(row -> row.getAs("time_key") != null)
                .collect(Collectors.toMap(
                        row -> row.getAs("time_key"),
                        row -> HeatMapData.TimePointInfo.builder()
                                .count(((Long) row.getAs("count")).intValue())
                                .intensity(LogAnalysisUtils.calculateIntensity((Long) row.getAs("count")))
                                .build()));

        return HeatMapData.builder()
                .minuteHeatmap(minuteHeatmap)
                .hourHeatmap(hourHeatmap)
                .build();
    }

    private IpStatistics analyzeIpStats(Dataset<Row> logs) {
        List<Row> ipStatsRows = logs
                .groupBy("ip")
                .count()
                .orderBy(functions.col("count").desc())
                .collectAsList();

        Map<String, IpStatistics.IpInfo> ipStats = new HashMap<>();
        List<String> localNetworks = new ArrayList<>();

        for (Row row : ipStatsRows) {
            String ip = row.getAs("ip");
            Long count = row.getAs("count");

            // 获取IP地理信息
            Map<String, String> geoInfo = ipGeoService.getIpGeoInfo(ip);
            boolean isLocal = ipGeoService.isLocalIp(ip);

            // 构建IP信息
            IpStatistics.IpInfo ipInfo = IpStatistics.IpInfo.builder()
                    .count(count.intValue())
                    .countryName(geoInfo.get("country_name"))
                    .city(geoInfo.get("city"))
                    .region(geoInfo.get("region"))
                    .longitude(LogAnalysisUtils.parseDouble(geoInfo.get("longitude")))
                    .latitude(LogAnalysisUtils.parseDouble(geoInfo.get("latitude")))
                    .isLocal(isLocal)
                    .isp(geoInfo.get("isp"))
                    .riskLevel(LogAnalysisUtils.calculateRiskLevel(count.intValue(), isLocal))
                    .build();

            ipStats.put(ip, ipInfo);

            if (isLocal) {
                localNetworks.add(ip);
            }
        }

        return IpStatistics.builder()
                .ipStats(ipStats)
                .localNetworks(localNetworks)
                .build();
    }

    private HttpMethodStatistics analyzeHttpMethods(Dataset<Row> logs) {
        List<Row> httpMethodRows = logs
                .groupBy("method")
                .count()
                .orderBy(functions.col("count").desc())
                .collectAsList();

        long totalRequests = logs.count();

        Map<String, HttpMethodStatistics.MethodInfo> methodStats = httpMethodRows.stream()
                .filter(row -> row.getAs("method") != null)
                .collect(Collectors.toMap(
                        row -> row.getAs("method"),
                        row -> {
                            String method = row.getAs("method");
                            Long count = row.getAs("count");
                            return HttpMethodStatistics.MethodInfo.builder()
                                    .count(count.intValue())
                                    .percentage((count.doubleValue() / totalRequests) * 100)
                                    .description(LogAnalysisUtils.getMethodDescription(method))
                                    .isSafe(LogAnalysisUtils.isSafeMethod(method))
                                    .isIdempotent(LogAnalysisUtils.isIdempotentMethod(method))
                                    .build();
                        }));

        return HttpMethodStatistics.builder()
                .methodStats(methodStats)
                .build();
    }

    private WeekdayStatistics analyzeWeekdayPatterns(Dataset<Row> logs) {
        List<Row> weekdayRows = logs
                .groupBy("weekday")
                .count()
                .orderBy("weekday")
                .collectAsList();

        long totalVisits = logs.count();

        Map<String, WeekdayStatistics.DayInfo> weekdayStats = weekdayRows.stream()
                .filter(row -> row.getAs("weekday") != null)
                .collect(Collectors.toMap(
                        row -> row.getAs("weekday").toString(),
                        row -> {
                            Integer weekday = row.getAs("weekday");
                            Long count = row.getAs("count");
                            return WeekdayStatistics.DayInfo.builder()
                                    .count(count.intValue())
                                    .percentage((count.doubleValue() / totalVisits) * 100)
                                    .dayName(LogAnalysisUtils.getDayName(weekday))
                                    .isWorkday(LogAnalysisUtils.isWorkday(weekday))
                                    .isWeekend(LogAnalysisUtils.isWeekend(weekday))
                                    .avgHourlyVisits(count.doubleValue() / 24)
                                    .build();
                        }));

        return WeekdayStatistics.builder()
                .weekdayStats(weekdayStats)
                .build();
    }

    private BrowserStatistics analyzeBrowserUsage(Dataset<Row> logs) {
        List<Row> browserRows = logs
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
                .orderBy(functions.col("count").desc())
                .collectAsList();

        long totalVisits = logs.count();

        Map<String, BrowserStatistics.BrowserInfo> browserStats = new HashMap<>();

        for (Row row : browserRows) {
            String browser = row.getAs("browser");
            Long count = row.getAs("count");

            if (browser != null) {
                BrowserStatistics.BrowserInfo browserInfo = BrowserStatistics.BrowserInfo.builder()
                        .count(count.intValue())
                        .percentage((count.doubleValue() / totalVisits) * 100)
                        .version("latest") // 简化处理，可以进一步分析User-Agent获取详细版本
                        .osDistribution(new HashMap<>()) // 可以进一步分析User-Agent获取OS信息
                        .deviceTypeDistribution(new HashMap<>()) // 可以进一步分析设备类型
                        .marketRank(LogAnalysisUtils.getBrowserMarketRank(browser))
                        .isModern(LogAnalysisUtils.isModernBrowser(browser))
                        .build();

                browserStats.put(browser, browserInfo);
            }
        }

        return BrowserStatistics.builder()
                .browserStats(browserStats)
                .build();
    }

    private void storeAnalysisResults(AccessLogAnalysisResult analysisResult) {
        // 存储完整的分析结果
        redisCacheUtil.setCacheObject(LogCacheKeys.ANALYSIS_RESULT_KEY, analysisResult,
                AccessLogAnalysisResult.class, LogCacheKeys.CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        // 分别存储各个维度的数据（兼容现有接口）
        redisCacheUtil.setCacheObject(LogCacheKeys.HEATMAP_KEY, analysisResult.getHeatMapData(),
                HeatMapData.class, LogCacheKeys.CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        redisCacheUtil.setCacheObject(LogCacheKeys.IP_STATS_KEY, analysisResult.getIpStatistics(),
                IpStatistics.class, LogCacheKeys.CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        redisCacheUtil.setCacheObject(LogCacheKeys.HTTP_METHODS_KEY, analysisResult.getHttpMethodStatistics(),
                HttpMethodStatistics.class, LogCacheKeys.CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        redisCacheUtil.setCacheObject(LogCacheKeys.WEEKDAY_STATS_KEY, analysisResult.getWeekdayStatistics(),
                WeekdayStatistics.class, LogCacheKeys.CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        redisCacheUtil.setCacheObject(LogCacheKeys.BROWSER_STATS_KEY, analysisResult.getBrowserStatistics(),
                BrowserStatistics.class, LogCacheKeys.CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    // 获取分析结果的公共方法
    public AccessLogAnalysisResult getCompleteAnalysisResult() {
        return redisCacheUtil.getCacheObject(LogCacheKeys.ANALYSIS_RESULT_KEY, AccessLogAnalysisResult.class);
    }

    public HeatMapData getHeatMapData() {
        return redisCacheUtil.getCacheObject(LogCacheKeys.HEATMAP_KEY, HeatMapData.class);
    }

    public IpStatistics getIpStatistics() {
        return redisCacheUtil.getCacheObject(LogCacheKeys.IP_STATS_KEY, IpStatistics.class);
    }

    public HttpMethodStatistics getHttpMethodStatistics() {
        return redisCacheUtil.getCacheObject(LogCacheKeys.HTTP_METHODS_KEY, HttpMethodStatistics.class);
    }

    public WeekdayStatistics getWeekdayStatistics() {
        return redisCacheUtil.getCacheObject(LogCacheKeys.WEEKDAY_STATS_KEY, WeekdayStatistics.class);
    }

    public BrowserStatistics getBrowserStatistics() {
        return redisCacheUtil.getCacheObject(LogCacheKeys.BROWSER_STATS_KEY, BrowserStatistics.class);
    }

    // 工具方法
    private LocalDateTime getDataStartTime(Dataset<Row> logs) {
        // 实现获取数据起始时间的逻辑
        return LocalDateTime.now().minusHours(24);
    }

    private LocalDateTime getDataEndTime(Dataset<Row> logs) {
        // 实现获取数据结束时间的逻辑
        return LocalDateTime.now();
    }

    private AccessLogAnalysisResult.DataQualityInfo calculateDataQuality(Dataset<Row> logs, long totalVisits) {
        // 计算数据质量信息
        long totalLines = totalVisits; // 简化处理
        long parsedLines = totalVisits;
        long failedLines = 0;

        return AccessLogAnalysisResult.DataQualityInfo.builder()
                .totalLogLines(totalLines)
                .parsedLines(parsedLines)
                .failedLines(failedLines)
                .parseSuccessRate(parsedLines == 0 ? 0.0 : (double) parsedLines / totalLines * 100)
                .qualityScore(LogAnalysisUtils.calculateQualityScore(parsedLines, totalLines))
                .build();
    }

}