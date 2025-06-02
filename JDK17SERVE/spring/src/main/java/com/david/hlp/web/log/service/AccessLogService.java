package com.david.hlp.web.log.service;

import lombok.RequiredArgsConstructor;
import com.david.hlp.web.common.util.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogService {
    private final RedisCache redisCache;

    // Redis key constants
    private static final String REDIS_KEY_PREFIX = "access_log:analysis:";
    private static final String HEATMAP_KEY = REDIS_KEY_PREFIX + "heatmap";
    private static final String IP_STATS_KEY = REDIS_KEY_PREFIX + "ip_stats";
    private static final String HTTP_METHODS_KEY = REDIS_KEY_PREFIX + "http_methods";
    private static final String WEEKDAY_STATS_KEY = REDIS_KEY_PREFIX + "weekday_stats";
    private static final String BROWSER_STATS_KEY = REDIS_KEY_PREFIX + "browser_stats";

    // Field names in Redis hash maps
    private static final String MINUTE_HEATMAP_FIELD = "minute_heatmap";
    private static final String HOUR_HEATMAP_FIELD = "hour_heatmap";

    /**
     * 获取访问热力图数据
     * 
     * @return 包含分钟级和小时级热力图数据的Map
     */
    public Map<String, Object> getTimeBasedStats() {
        Map<String, Object> heatmapData = redisCache.getCacheMap(HEATMAP_KEY);
        Map<String, Object> result = new HashMap<>();

        // 从Redis中获取热力图数据
        if (heatmapData != null && !heatmapData.isEmpty()) {
            result.put("minute_heatmap", (Map<?, ?>) heatmapData.getOrDefault(MINUTE_HEATMAP_FIELD, new HashMap<>()));
            result.put("hour_heatmap", (Map<?, ?>) heatmapData.getOrDefault(HOUR_HEATMAP_FIELD, new HashMap<>()));
        } else {
            result.put("minute_heatmap", new HashMap<>());
            result.put("hour_heatmap", new HashMap<>());
        }

        return result;
    }

    /**
     * 获取IP统计信息
     * 
     * @return IP统计信息，包括访问次数和地理位置
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getIpStats() {
        Map<String, Object> ipStats = redisCache.getCacheMap(IP_STATS_KEY);
        return ipStats != null ? ipStats : new HashMap<>();
    }

    /**
     * 获取HTTP方法分布数据
     * 
     * @return HTTP方法及其对应的请求次数
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getHttpMethodStats() {
        Map<String, Object> httpMethods = redisCache.getCacheMap(HTTP_METHODS_KEY);
        return httpMethods != null ? httpMethods : new HashMap<>();
    }

    /**
     * 获取工作日访问模式数据
     * 
     * @return 每周各天的访问量统计
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getWeekdayStats() {
        Map<String, Object> weekdayStats = redisCache.getCacheMap(WEEKDAY_STATS_KEY);
        return weekdayStats != null ? weekdayStats : new HashMap<>();
    }

    /**
     * 获取浏览器使用情况数据
     * 
     * @return 各浏览器的使用情况统计
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getBrowserStats() {
        Map<String, Object> browserStats = redisCache.getCacheMap(BROWSER_STATS_KEY);
        return browserStats != null ? browserStats : new HashMap<>();
    }

    /**
     * 获取所有统计数据
     * 
     * @return 包含所有统计数据的Map
     */
    public Map<String, Object> getAllStats() {
        Map<String, Object> allStats = new HashMap<>();
        allStats.put("heatmap", getTimeBasedStats());
        allStats.put("ipStats", getIpStats());
        allStats.put("httpMethods", getHttpMethodStats());
        allStats.put("weekdayStats", getWeekdayStats());
        allStats.put("browserStats", getBrowserStats());
        return allStats;
    }
}