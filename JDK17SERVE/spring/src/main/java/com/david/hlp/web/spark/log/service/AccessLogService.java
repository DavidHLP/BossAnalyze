package com.david.hlp.web.spark.log.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.david.hlp.commons.utils.RedisCacheHelper;
import com.david.hlp.commons.entity.logs.*;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogService {
    private final RedisCacheHelper redisCacheHelper;

    // Redis key constants - 使用 LogCacheKeys 中的常量
    private static final String HEATMAP_KEY = LogCacheKeys.HEATMAP_KEY;
    private static final String IP_STATS_KEY = LogCacheKeys.IP_STATS_KEY;
    private static final String HTTP_METHODS_KEY = LogCacheKeys.HTTP_METHODS_KEY;
    private static final String WEEKDAY_STATS_KEY = LogCacheKeys.WEEKDAY_STATS_KEY;
    private static final String BROWSER_STATS_KEY = LogCacheKeys.BROWSER_STATS_KEY;
    private static final String ANALYSIS_RESULT_KEY = LogCacheKeys.ANALYSIS_RESULT_KEY;

    /**
     * 获取访问热力图数据
     * 
     * @return 包含分钟级和小时级热力图数据的Map
     */
    public Map<String, Object> getTimeBasedStats() {
        try {
            HeatMapData heatMapData = redisCacheHelper.getObject(HEATMAP_KEY, HeatMapData.class);
            Map<String, Object> result = new HashMap<>();

            if (heatMapData != null) {
                // 转换分钟级热力图数据
                Map<String, Object> minuteHeatmap = new HashMap<>();
                if (heatMapData.getMinuteHeatmap() != null) {
                    heatMapData.getMinuteHeatmap().forEach((key, value) -> minuteHeatmap.put(key, value.getCount()));
                }

                // 转换小时级热力图数据
                Map<String, Object> hourHeatmap = new HashMap<>();
                if (heatMapData.getHourHeatmap() != null) {
                    heatMapData.getHourHeatmap().forEach((key, value) -> hourHeatmap.put(key, value.getCount()));
                }

                result.put("minute_heatmap", minuteHeatmap);
                result.put("hour_heatmap", hourHeatmap);
            } else {
                result.put("minute_heatmap", new HashMap<>());
                result.put("hour_heatmap", new HashMap<>());
                log.warn("热力图数据为空，请检查Spark分析任务是否正常运行");
            }

            return result;
        } catch (Exception e) {
            log.error("获取热力图数据失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("minute_heatmap", new HashMap<>());
            result.put("hour_heatmap", new HashMap<>());
            return result;
        }
    }

    /**
     * 获取IP统计信息
     * 
     * @return IP统计信息，包括访问次数和地理位置
     */
    public Map<String, Object> getIpStats() {
        try {
            IpStatistics ipStatistics = redisCacheHelper.getObject(IP_STATS_KEY, IpStatistics.class);
            Map<String, Object> result = new HashMap<>();

            if (ipStatistics != null && ipStatistics.getIpStats() != null) {
                // 转换IP统计数据，直接返回符合前端格式的数据
                ipStatistics.getIpStats().forEach((ip, ipInfo) -> {
                    Map<String, Object> ipData = new HashMap<>();
                    ipData.put("count", ipInfo.getCount());
                    ipData.put("country_name", ipInfo.getCountryName()); // 修改为前端期望的字段名
                    ipData.put("city", ipInfo.getCity());
                    ipData.put("location", ipInfo.getRegion());
                    ipData.put("lat", ipInfo.getLatitude());
                    ipData.put("lng", ipInfo.getLongitude());
                    result.put(ip, ipData);
                });
            }

            return result;
        } catch (Exception e) {
            log.error("获取IP统计数据失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 获取HTTP方法分布数据
     * 
     * @return HTTP方法及其对应的请求次数
     */
    public Map<String, Object> getHttpMethodStats() {
        try {
            HttpMethodStatistics httpMethodStatistics = redisCacheHelper.getObject(HTTP_METHODS_KEY,
                    HttpMethodStatistics.class);
            Map<String, Object> result = new HashMap<>();

            if (httpMethodStatistics != null && httpMethodStatistics.getMethodStats() != null) {
                // 转换为前端期望的简单格式 {method: count}
                httpMethodStatistics.getMethodStats().forEach((method, methodInfo) -> {
                    result.put(method, methodInfo.getCount());
                });
            }

            return result;
        } catch (Exception e) {
            log.error("获取HTTP方法统计数据失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 获取工作日访问模式数据
     * 
     * @return 每周各天的访问量统计
     */
    public Map<String, Object> getWeekdayStats() {
        try {
            WeekdayStatistics weekdayStatistics = redisCacheHelper.getObject(WEEKDAY_STATS_KEY,
                    WeekdayStatistics.class);
            Map<String, Object> result = new HashMap<>();

            if (weekdayStatistics != null && weekdayStatistics.getWeekdayStats() != null) {
                // 转换为前端期望的简单格式 {weekday: count}
                weekdayStatistics.getWeekdayStats().forEach((weekday, dayInfo) -> {
                    result.put(weekday, dayInfo.getCount());
                });
            }

            return result;
        } catch (Exception e) {
            log.error("获取工作日统计数据失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 获取浏览器使用情况数据
     * 
     * @return 各浏览器的使用情况统计
     */
    public Map<String, Object> getBrowserStats() {
        try {
            BrowserStatistics browserStatistics = redisCacheHelper.getObject(BROWSER_STATS_KEY,
                    BrowserStatistics.class);
            Map<String, Object> result = new HashMap<>();

            if (browserStatistics != null && browserStatistics.getBrowserStats() != null) {
                // 转换为前端期望的简单格式 {browser: count}
                browserStatistics.getBrowserStats().forEach((browser, browserInfo) -> {
                    result.put(browser, browserInfo.getCount());
                });
            }

            return result;
        } catch (Exception e) {
            log.error("获取浏览器统计数据失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 获取完整的分析结果
     * 
     * @return 完整的访问日志分析结果
     */
    public AccessLogAnalysisResult getCompleteAnalysisResult() {
        try {
            AccessLogAnalysisResult result = redisCacheHelper.getObject(ANALYSIS_RESULT_KEY,
                    AccessLogAnalysisResult.class);
            if (result == null) {
                log.warn("完整分析结果为空，请检查Spark分析任务是否正常运行");
            }
            return result;
        } catch (Exception e) {
            log.error("获取完整分析结果失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取所有统计数据
     * 
     * @return 包含所有统计数据的Map，格式符合前端AccessLogStats接口
     */
    public Map<String, Object> getAllStats() {
        try {
            Map<String, Object> allStats = new HashMap<>();

            // 获取各项统计数据
            allStats.put("heatmap", getTimeBasedStats());
            allStats.put("ipStats", getIpStats()); // 现在直接返回符合前端格式的数据
            allStats.put("httpMethods", getHttpMethodStats());
            allStats.put("weekdayStats", getWeekdayStats());
            allStats.put("browserStats", getBrowserStats());

            // 计算总访问量和唯一IP数
            Map<String, Object> httpMethods = getHttpMethodStats();
            int totalVisits = httpMethods.values().stream()
                    .mapToInt(count -> (Integer) count)
                    .sum();
            
            int uniqueIps = getIpStats().size();

            // 添加前端需要的计算指标
            allStats.put("totalVisits", totalVisits);
            allStats.put("uniqueIps", uniqueIps);
            allStats.put("avgResponseTime", 0); // 暂未实现
            allStats.put("errorRate", 0); // 暂未实现

            return allStats;
        } catch (Exception e) {
            log.error("获取所有统计数据失败: {}", e.getMessage(), e);
            // 返回空数据结构，避免前端报错
            Map<String, Object> fallbackStats = new HashMap<>();
            fallbackStats.put("heatmap", Map.of("minute_heatmap", new HashMap<>(), "hour_heatmap", new HashMap<>()));
            fallbackStats.put("ipStats", new HashMap<>());
            fallbackStats.put("httpMethods", new HashMap<>());
            fallbackStats.put("weekdayStats", new HashMap<>());
            fallbackStats.put("browserStats", new HashMap<>());
            fallbackStats.put("totalVisits", 0);
            fallbackStats.put("uniqueIps", 0);
            fallbackStats.put("avgResponseTime", 0);
            fallbackStats.put("errorRate", 0);
            return fallbackStats;
        }
    }

    /**
     * 检查数据是否可用
     * 
     * @return 数据可用性状态
     */
    public Map<String, Object> getDataAvailability() {
        Map<String, Object> availability = new HashMap<>();

        availability.put("heatmapAvailable", redisCacheHelper.exists(HEATMAP_KEY));
        availability.put("ipStatsAvailable", redisCacheHelper.exists(IP_STATS_KEY));
        availability.put("httpMethodsAvailable", redisCacheHelper.exists(HTTP_METHODS_KEY));
        availability.put("weekdayStatsAvailable", redisCacheHelper.exists(WEEKDAY_STATS_KEY));
        availability.put("browserStatsAvailable", redisCacheHelper.exists(BROWSER_STATS_KEY));
        availability.put("completeResultAvailable", redisCacheHelper.exists(ANALYSIS_RESULT_KEY));

        boolean allAvailable = (Boolean) availability.get("heatmapAvailable") &&
                (Boolean) availability.get("ipStatsAvailable") &&
                (Boolean) availability.get("httpMethodsAvailable") &&
                (Boolean) availability.get("weekdayStatsAvailable") &&
                (Boolean) availability.get("browserStatsAvailable");

        availability.put("allDataAvailable", allAvailable);

        return availability;
    }
}