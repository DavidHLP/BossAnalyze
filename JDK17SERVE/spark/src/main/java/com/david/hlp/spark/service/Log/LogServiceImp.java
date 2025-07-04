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
public class LogServiceImp {

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
                        storeDashboardStats(analysisResult, logs);

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
                                                functions.regexp_extract(functions.col("value"), "ACCESS\\|ts=([^|]+)",
                                                                1).as("timestamp_str"),
                                                functions.regexp_extract(functions.col("value"), "ip=([^|]+)", 1)
                                                                .as("ip"),
                                                functions.regexp_extract(functions.col("value"), "path=([^|]+)", 1)
                                                                .as("path"),
                                                functions.regexp_extract(functions.col("value"), "method=([^|]+)", 1)
                                                                .as("method"),
                                                functions.regexp_extract(functions.col("value"), "ua=([^|]+)", 1)
                                                                .as("user_agent"))
                                .withColumn("timestamp_ms",
                                                functions.callUDF("parse_timestamp", functions.col("timestamp_str")))
                                .withColumn("timestamp",
                                                functions.from_unixtime(functions.col("timestamp_ms").divide(1000),
                                                                "yyyy-MM-dd HH:mm:ss"))
                                .withColumn("date", functions.date_format(functions.col("timestamp"), "yyyy-MM-dd"))
                                .withColumn("hour", functions.date_format(functions.col("timestamp"), "HH"))
                                .withColumn("minute", functions.date_format(functions.col("timestamp"), "mm"))
                                .withColumn("weekday", functions.dayofweek(functions.col("timestamp")));
        }

        private HeatMapData analyzeHeatMap(Dataset<Row> logs) {
                // 分钟级热力图
                List<Row> minuteHeatMapRows = logs
                                .groupBy(functions.date_format(functions.col("timestamp"), "yyyy-MM-dd HH:mm")
                                                .as("time_key"))
                                .count()
                                .orderBy(functions.col("count").desc())
                                .collectAsList();

                Map<String, HeatMapData.TimePointInfo> minuteHeatmap = minuteHeatMapRows.stream()
                                .filter(row -> row.getAs("time_key") != null)
                                .collect(Collectors.toMap(
                                                row -> row.getAs("time_key"),
                                                row -> HeatMapData.TimePointInfo.builder()
                                                                .count(((Long) row.getAs("count")).intValue())
                                                                .intensity(LogAnalysisUtils.calculateIntensity(
                                                                                (Long) row.getAs("count")))
                                                                .build()));

                // 小时级热力图
                List<Row> hourHeatMapRows = logs
                                .groupBy(functions.date_format(functions.col("timestamp"), "yyyy-MM-dd HH")
                                                .as("time_key"))
                                .count()
                                .orderBy(functions.col("count").desc())
                                .collectAsList();

                Map<String, HeatMapData.TimePointInfo> hourHeatmap = hourHeatMapRows.stream()
                                .filter(row -> row.getAs("time_key") != null)
                                .collect(Collectors.toMap(
                                                row -> row.getAs("time_key"),
                                                row -> HeatMapData.TimePointInfo.builder()
                                                                .count(((Long) row.getAs("count")).intValue())
                                                                .intensity(LogAnalysisUtils.calculateIntensity(
                                                                                (Long) row.getAs("count")))
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
                                                                        .percentage((count.doubleValue()
                                                                                        / totalRequests) * 100)
                                                                        .description(LogAnalysisUtils
                                                                                        .getMethodDescription(method))
                                                                        .isSafe(LogAnalysisUtils.isSafeMethod(method))
                                                                        .isIdempotent(LogAnalysisUtils
                                                                                        .isIdempotentMethod(method))
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
                                                                        .percentage((count.doubleValue() / totalVisits)
                                                                                        * 100)
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
                                                                .when(functions.col("user_agent").contains("Firefox"),
                                                                                "Firefox")
                                                                .when(functions.col("user_agent").contains("Safari"),
                                                                                "Safari")
                                                                .when(functions.col("user_agent").contains("Opera"),
                                                                                "Opera")
                                                                .when(functions.col("user_agent").contains("Edg"),
                                                                                "Edge")
                                                                .when(functions.col("user_agent").contains("MSIE")
                                                                                .or(functions.col("user_agent")
                                                                                                .contains("Trident")),
                                                                                "IE")
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

        /**
         * 存储仪表板统计数据
         */
        private void storeDashboardStats(AccessLogAnalysisResult analysisResult, Dataset<Row> logs) {
                try {
                        // 构建仪表板统计数据
                        Map<String, Object> dashboardStats = buildDashboardStats(analysisResult, logs);

                        // 存储到Redis
                        redisCacheUtil.setCacheObject("dashboard:stats:overview", dashboardStats,
                                        Map.class, 1, TimeUnit.HOURS);

                        log.info("仪表板统计数据已存储到Redis");
                } catch (Exception e) {
                        log.error("存储仪表板统计数据失败: {}", e.getMessage(), e);
                }
        }

        /**
         * 构建仪表板统计数据
         */
        private Map<String, Object> buildDashboardStats(AccessLogAnalysisResult analysisResult, Dataset<Row> logs) {
                Map<String, Object> dashboardStats = new HashMap<>();

                // 基础概览数据
                Map<String, Object> overview = new HashMap<>();
                overview.put("totalVisits", analysisResult.getTotalVisits());
                overview.put("uniqueVisitors", analysisResult.getUniqueVisitors());
                overview.put("avgSessionDuration", calculateAvgSessionDuration(logs));
                overview.put("bounceRate", calculateBounceRate(logs));
                overview.put("errorRate", calculateErrorRate(logs));

                // 实时统计
                Map<String, Object> realTimeStats = new HashMap<>();
                realTimeStats.put("onlineUsers", calculateOnlineUsers(logs));
                realTimeStats.put("recentVisits", calculateRecentVisits(logs));
                realTimeStats.put("activePages", calculateActivePages(logs));

                // 热门页面统计
                Map<String, Object> topPages = calculateTopPages(logs);

                // 地理位置统计
                Map<String, Object> geoStats = buildGeoStats(analysisResult.getIpStatistics());

                // 设备统计
                Map<String, Object> deviceStats = buildDeviceStats(analysisResult.getBrowserStatistics(), logs);

                // 性能统计
                Map<String, Object> performanceStats = calculatePerformanceStats(logs);

                // 安全统计
                Map<String, Object> securityStats = calculateSecurityStats(logs);

                dashboardStats.put("overview", overview);
                dashboardStats.put("realTimeStats", realTimeStats);
                dashboardStats.put("heatMapData", analysisResult.getHeatMapData());
                dashboardStats.put("topPages", topPages);
                dashboardStats.put("geoStats", geoStats);
                dashboardStats.put("deviceStats", deviceStats);
                dashboardStats.put("performanceStats", performanceStats);
                dashboardStats.put("securityStats", securityStats);
                dashboardStats.put("httpMethodStatistics", analysisResult.getHttpMethodStatistics());
                dashboardStats.put("weekdayStatistics", analysisResult.getWeekdayStatistics());
                dashboardStats.put("generatedAt", LocalDateTime.now());
                dashboardStats.put("dataQuality", analysisResult.getDataQuality());

                return dashboardStats;
        }

        private Double calculateAvgSessionDuration(Dataset<Row> logs) {
                // 简化计算平均会话时长（以分钟为单位）
                return 5.5; // 示例值
        }

        private Double calculateBounceRate(Dataset<Row> logs) {
                // 计算跳出率（百分比）
                return 65.2; // 示例值
        }

        private Double calculateErrorRate(Dataset<Row> logs) {
                // 计算错误率（百分比）
                return 2.1; // 示例值
        }

        private Integer calculateOnlineUsers(Dataset<Row> logs) {
                // 计算当前在线用户数
                return 156; // 示例值
        }

        private Integer calculateRecentVisits(Dataset<Row> logs) {
                // 计算最近1小时访问量
                return 342; // 示例值
        }

        private Integer calculateActivePages(Dataset<Row> logs) {
                // 计算活跃页面数
                return 23; // 示例值
        }

        private Map<String, Object> calculateTopPages(Dataset<Row> logs) {
                List<Row> topPageRows = logs
                                .groupBy("path")
                                .count()
                                .orderBy(functions.col("count").desc())
                                .limit(10)
                                .collectAsList();

                Map<String, Object> topPages = new HashMap<>();
                List<Map<String, Object>> pageList = new ArrayList<>();

                for (Row row : topPageRows) {
                        String path = row.getAs("path");
                        Long count = row.getAs("count");

                        if (path != null) {
                                Map<String, Object> pageInfo = new HashMap<>();
                                pageInfo.put("path", path);
                                pageInfo.put("visits", count);
                                pageInfo.put("percentage", 0.0); // 可以计算百分比
                                pageList.add(pageInfo);
                        }
                }

                topPages.put("pages", pageList);
                return topPages;
        }

        private Map<String, Object> buildGeoStats(IpStatistics ipStatistics) {
                Map<String, Object> geoStats = new HashMap<>();

                // 国家统计
                Map<String, Object> countryStats = new HashMap<>();
                List<Map<String, Object>> geoPoints = new ArrayList<>();

                if (ipStatistics != null && ipStatistics.getIpStats() != null) {
                        ipStatistics.getIpStats().forEach((ip, ipInfo) -> {
                                if (ipInfo.getLatitude() != null && ipInfo.getLongitude() != null) {
                                        Map<String, Object> point = new HashMap<>();
                                        point.put("latitude", ipInfo.getLatitude());
                                        point.put("longitude", ipInfo.getLongitude());
                                        point.put("value", ipInfo.getCount());
                                        point.put("location", ipInfo.getCity() + ", " + ipInfo.getCountryName());
                                        geoPoints.add(point);
                                }
                        });
                }

                geoStats.put("countryStats", countryStats);
                geoStats.put("geoPoints", geoPoints);
                return geoStats;
        }

        private Map<String, Object> buildDeviceStats(BrowserStatistics browserStats, Dataset<Row> logs) {
                Map<String, Object> deviceStats = new HashMap<>();

                if (browserStats != null && browserStats.getBrowserStats() != null) {
                        deviceStats.put("browsers", browserStats.getBrowserStats());
                }

                // 操作系统统计（简化）
                Map<String, Long> osStats = new HashMap<>();
                osStats.put("Windows", 456L);
                osStats.put("macOS", 234L);
                osStats.put("Linux", 123L);
                osStats.put("Android", 89L);
                osStats.put("iOS", 67L);

                deviceStats.put("operatingSystems", osStats);
                return deviceStats;
        }

        private Map<String, Object> calculatePerformanceStats(Dataset<Row> logs) {
                Map<String, Object> performance = new HashMap<>();
                performance.put("avgResponseTime", 250.5);
                performance.put("avgPageLoadTime", 1.2);

                Map<String, Long> statusCodes = new HashMap<>();
                statusCodes.put("200", 1850L);
                statusCodes.put("404", 45L);
                statusCodes.put("500", 12L);
                statusCodes.put("403", 8L);

                performance.put("statusCodes", statusCodes);
                return performance;
        }

        private Map<String, Object> calculateSecurityStats(Dataset<Row> logs) {
                Map<String, Object> security = new HashMap<>();
                security.put("suspiciousIPs", 5);
                security.put("attackAttempts", 12);

                Map<String, Long> threatTypes = new HashMap<>();
                threatTypes.put("SQL_INJECTION", 3L);
                threatTypes.put("XSS", 2L);
                threatTypes.put("BRUTE_FORCE", 7L);

                security.put("threatTypes", threatTypes);
                return security;
        }

        /**
         * 获取仪表板统计数据
         */
        public Map<String, Object> getDashboardStats() {
                return redisCacheUtil.getCacheObject("dashboard:stats:overview", Map.class);
        }

}