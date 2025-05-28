package com.david.hlp.web.log.service;

import com.david.hlp.web.common.util.RedisCache;
import com.david.hlp.web.log.model.AccessLogStats;
import com.david.hlp.web.log.model.IpStats;
import com.david.hlp.web.log.model.TimeStats;
import com.david.hlp.web.log.model.UrlStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AccessLogService {

    private static final Logger log = LoggerFactory.getLogger(AccessLogService.class);

    @Autowired
    private RedisCache redisCache;

    private static final String REDIS_ACCESS_STATS_KEY = "access_log:stats";
    private static final String REDIS_IP_STATS_KEY = "access_log:ip_stats";
    private static final String REDIS_URL_STATS_KEY = "access_log:url_stats";
    private static final String REDIS_ANOMALY_STATS_KEY = "access_log:anomaly_stats";
    private static final String REDIS_LOAD_STATS_KEY = "access_log:load_stats";
    private static final String REDIS_TIME_STATS_KEY = "access_log:time_stats";

    /**
     * 获取访问日志分析结果
     *
     * @param logPath 日志文件路径（此参数保留但不使用，实际只从Redis获取数据）
     * @return 分析结果概要
     */
    public AccessLogStats analyzeAccessLog(String logPath) {
        // 直接从Redis获取分析结果
        return getCachedAnalysisResults();
    }

    /**
     * 获取缓存中的分析结果
     */
    public AccessLogStats getCachedAnalysisResults() {
        Map<String, Object> cachedStats = redisCache.getCacheMap(REDIS_ACCESS_STATS_KEY);
        if (cachedStats == null || cachedStats.isEmpty()) {
            log.warn("未找到缓存的访问日志统计数据");
            return AccessLogStats.builder().build();
        }

        log.info("成功从Redis获取访问日志统计数据: {}", cachedStats);
        return mapToAccessLogStats(cachedStats);
    }

    /**
     * 获取IP统计分析结果
     */
    public IpStats getIpStats() {
        Map<String, Object> ipStatsMap = redisCache.getCacheMap(REDIS_IP_STATS_KEY);
        if (ipStatsMap == null || ipStatsMap.isEmpty()) {
            return IpStats.builder().build();
        }

        return mapToIpStats(ipStatsMap);
    }

    /**
     * 获取URL统计分析结果
     */
    public UrlStats getUrlStats() {
        Map<String, Object> urlStatsMap = redisCache.getCacheMap(REDIS_URL_STATS_KEY);
        if (urlStatsMap == null || urlStatsMap.isEmpty()) {
            return UrlStats.builder().build();
        }

        return mapToUrlStats(urlStatsMap);
    }

    /**
     * 获取异常统计分析结果
     */
    public Map<String, Object> getAnomalyStats() {
        Map<String, Object> anomalyStatsMap = redisCache.getCacheMap(REDIS_ANOMALY_STATS_KEY);
        if (anomalyStatsMap == null || anomalyStatsMap.isEmpty()) {
            return new HashMap<>();
        }
        return anomalyStatsMap;
    }

    /**
     * 获取系统负载统计分析结果
     */
    public Map<String, Object> getLoadStats() {
        Map<String, Object> loadStatsMap = redisCache.getCacheMap(REDIS_LOAD_STATS_KEY);
        if (loadStatsMap == null || loadStatsMap.isEmpty()) {
            return new HashMap<>();
        }
        return loadStatsMap;
    }

    /**
     * 获取时间统计分析结果
     */
    public TimeStats getTimeStats() {
        Map<String, Object> timeStatsMap = redisCache.getCacheMap(REDIS_LOAD_STATS_KEY);
        if (timeStatsMap == null || timeStatsMap.isEmpty()) {
            return TimeStats.builder().build();
        }
        return mapToTimeStats(timeStatsMap);
    }
    
    /**
     * 获取用户行为分析数据
     */
    public Map<String, Object> getUserBehavior() {
        Map<String, Object> userBehaviorMap = redisCache.getCacheMap(REDIS_IP_STATS_KEY);
        if (userBehaviorMap == null || userBehaviorMap.isEmpty()) {
            return new HashMap<>();
        }
        
        // 从IP统计中提取用户行为相关数据
        Map<String, Object> userBehavior = new HashMap<>();
        if (userBehaviorMap.containsKey("uaStats")) {
            userBehavior.put("uaStats", userBehaviorMap.get("uaStats"));
        }
        if (userBehaviorMap.containsKey("ipPathStats")) {
            userBehavior.put("ipPathStats", userBehaviorMap.get("ipPathStats"));
        }
        
        return userBehavior;
    }
    
    /**
     * 获取基于时间维度的细粒度分析数据
     */
    public Map<String, Object> getTimeBasedStats() {
        Map<String, Object> timeStatsMap = redisCache.getCacheMap(REDIS_TIME_STATS_KEY);
        if (timeStatsMap == null || timeStatsMap.isEmpty()) {
            // 尝试从负载统计中获取部分时间维度数据
            Map<String, Object> loadStats = getLoadStats();
            if (loadStats != null && !loadStats.isEmpty()) {
                return loadStats;
            }
            return new HashMap<>();
        }
        return timeStatsMap;
    }

    /**
     * 将Redis缓存的Map转换为AccessLogStats对象
     */
    @SuppressWarnings("unchecked")
    private AccessLogStats mapToAccessLogStats(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return AccessLogStats.builder().build();
        }

        Map<String, Object> apiCategoryStats = (Map<String, Object>) map.get("apiCategoryStats");
        Map<String, Object> popularPaths = (Map<String, Object>) map.get("popularPaths");

        // 处理空键的情况
        Map<String, Integer> cleanedApiCategoryStats = cleanMapWithEmptyKeys(apiCategoryStats);
        Map<String, Integer> cleanedPopularPaths = cleanMapWithEmptyKeys(popularPaths);

        return AccessLogStats.builder()
                .totalRequests(getLongValue(map, "totalRequests"))
                .uniqueIPs(getLongValue(map, "uniqueIPs"))
                .uniquePaths(getLongValue(map, "uniquePaths"))
                .apiCategoryStats(cleanedApiCategoryStats)
                .popularPaths(cleanedPopularPaths)
                .build();
    }

    /**
     * 清理Map中的空键
     */
    @SuppressWarnings("unchecked")
    private Map<String, Integer> cleanMapWithEmptyKeys(Map<String, Object> sourceMap) {
        if (sourceMap == null || sourceMap.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Integer> cleanedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String key = entry.getKey();
            // 如果键为空或只包含空格，则使用"未知"代替
            if (key == null || key.trim().isEmpty()) {
                key = "未知路径";
            }
            
            Object value = entry.getValue();
            if (value instanceof Integer) {
                cleanedMap.put(key, (Integer) value);
            } else if (value instanceof Long) {
                cleanedMap.put(key, ((Long) value).intValue());
            } else if (value instanceof String) {
                try {
                    cleanedMap.put(key, Integer.parseInt((String) value));
                } catch (NumberFormatException e) {
                    cleanedMap.put(key, 0);
                }
            }
        }
        
        return cleanedMap;
    }

    /**
     * 将Redis缓存的Map转换为IpStats对象
     */
    @SuppressWarnings("unchecked")
    private IpStats mapToIpStats(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return IpStats.builder().build();
        }

        // 从Redis获取ipStats
        Map<String, Object> ipStatsMap = (Map<String, Object>) map.get("ipStats");
        
        // 若没有ipStats字段，可能是整个map就是ipStats
        if (ipStatsMap == null) {
            // 获取uniqueCount，如果没有则使用map大小
            Long uniqueCount = getLongValue(map, "uniqueCount");
            if (uniqueCount == 0L && map.containsKey("topIPs")) {
                Map<String, Object> topIPs = (Map<String, Object>) map.get("topIPs");
                uniqueCount = (long) (topIPs != null ? topIPs.size() : 0);
            }
            
            return IpStats.builder()
                    .uniqueCount(uniqueCount)
                    .topIPs(convertToStringIntegerMap(map.get("topIPs")))
                    .build();
        }
        
        // 获取uniqueCount
        Long uniqueCount = (long) ipStatsMap.size();
        
        log.debug("转换IpStats: uniqueCount={}, topIPsSize={}", uniqueCount, 
                ipStatsMap != null ? ipStatsMap.size() : 0);
        
        return IpStats.builder()
                .uniqueCount(uniqueCount)
                .topIPs(convertToStringIntegerMap(ipStatsMap))
                .build();
    }

    /**
     * 将Redis缓存的Map转换为UrlStats对象
     */
    @SuppressWarnings("unchecked")
    private UrlStats mapToUrlStats(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return UrlStats.builder().build();
        }

        // 从Redis获取pathDateStats
        Map<String, Object> pathDateStatsMap = (Map<String, Object>) map.get("pathDateStats");
        
        // 若没有pathDateStats字段，可能是整个map就是urlStats
        if (pathDateStatsMap == null) {
            // 获取uniqueCount，如果没有则使用map大小
            Long uniqueCount = getLongValue(map, "uniqueCount");
            if (uniqueCount == 0L && map.containsKey("topUrls")) {
                Map<String, Object> topUrls = (Map<String, Object>) map.get("topUrls");
                uniqueCount = (long) (topUrls != null ? topUrls.size() : 0);
            }
            
            return UrlStats.builder()
                    .uniqueCount(uniqueCount)
                    .topUrls(convertToStringIntegerMap(map.get("topUrls")))
                    .build();
        }
        
        // 获取uniqueCount
        Long uniqueCount = (long) pathDateStatsMap.size();
        
        log.debug("转换UrlStats: uniqueCount={}, pathDateStatsSize={}", uniqueCount, 
                pathDateStatsMap != null ? pathDateStatsMap.size() : 0);
        
        return UrlStats.builder()
                .uniqueCount(uniqueCount)
                .topUrls(convertToStringIntegerMap(pathDateStatsMap))
                .build();
    }

    /**
     * 将Redis缓存的Map转换为TimeStats对象
     */
    @SuppressWarnings("unchecked")
    private TimeStats mapToTimeStats(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return TimeStats.builder().build();
        }

        // 从Redis获取hourlyAvg和peakMinute
        Map<String, Object> hourlyAvgMap = (Map<String, Object>) map.get("hourlyAvg");
        Map<String, Object> peakMinuteMap = (Map<String, Object>) map.get("peakMinute");

        // 构建hourlyDistribution
        Map<String, Integer> hourlyDistribution = new HashMap<>();
        if (hourlyAvgMap != null) {
            for (Map.Entry<String, Object> entry : hourlyAvgMap.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    hourlyDistribution.put(entry.getKey(), ((Number) value).intValue());
                }
            }
        }

        // 找出峰值小时
        int peakHour = 0;
        int maxCount = -1;
        if (hourlyDistribution != null && !hourlyDistribution.isEmpty()) {
            for (Map.Entry<String, Integer> entry : hourlyDistribution.entrySet()) {
                try {
                    int hour = Integer.parseInt(entry.getKey());
                    int count = entry.getValue();
                    if (count > maxCount) {
                        maxCount = count;
                        peakHour = hour;
                    }
                } catch (NumberFormatException e) {
                    // 忽略非数字的小时值
                }
            }
        }

        log.debug("转换TimeStats: peakHour={}, hourlyDistribution={}", peakHour, hourlyDistribution);
        
        return TimeStats.builder()
                .peakHour(peakHour)
                .hourlyDistribution(hourlyDistribution)
                .build();
    }

    /**
     * 将Object转换为Map<String, Integer>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Integer> convertToStringIntegerMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }

        if (obj instanceof Map) {
            Map<String, Object> sourceMap = (Map<String, Object>) obj;
            Map<String, Integer> targetMap = new HashMap<>();

            for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
                if (entry.getValue() instanceof Integer) {
                    targetMap.put(entry.getKey(), (Integer) entry.getValue());
                } else if (entry.getValue() instanceof Long) {
                    targetMap.put(entry.getKey(), ((Long) entry.getValue()).intValue());
                } else if (entry.getValue() instanceof String) {
                    try {
                        targetMap.put(entry.getKey(), Integer.parseInt((String) entry.getValue()));
                    } catch (NumberFormatException e) {
                        targetMap.put(entry.getKey(), 0);
                    }
                } else {
                    targetMap.put(entry.getKey(), 0);
                }
            }

            return targetMap;
        }

        return new HashMap<>();
    }

    /**
     * 安全获取Long类型值
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    /**
     * 验证Redis数据一致性
     */
    public boolean validateRedisData() {
        Map<String, Object> cachedStats = redisCache.getCacheMap(REDIS_ACCESS_STATS_KEY);
        if (cachedStats == null || cachedStats.isEmpty()) {
            log.error("Redis中不存在访问日志统计数据");
            return false;
        }

        // 验证必要字段是否存在
        String[] requiredFields = {"totalRequests", "uniqueIPs", "uniquePaths", "apiCategoryStats", "popularPaths"};
        for (String field : requiredFields) {
            if (!cachedStats.containsKey(field)) {
                log.error("Redis数据缺少必要字段: {}", field);
                return false;
            }
        }

        // 验证数据类型
        try {
            Long totalRequests = getLongValue(cachedStats, "totalRequests");
            Long uniqueIPs = getLongValue(cachedStats, "uniqueIPs");
            Long uniquePaths = getLongValue(cachedStats, "uniquePaths");
            Map<String, Object> apiCategoryStats = (Map<String, Object>) cachedStats.get("apiCategoryStats");
            Map<String, Object> popularPaths = (Map<String, Object>) cachedStats.get("popularPaths");

            log.info("Redis数据验证通过: totalRequests={}, uniqueIPs={}, uniquePaths={}, apiCategoryCount={}, popularPathsCount={}",
                    totalRequests, uniqueIPs, uniquePaths,
                    apiCategoryStats != null ? apiCategoryStats.size() : 0,
                    popularPaths != null ? popularPaths.size() : 0);
            return true;
        } catch (Exception e) {
            log.error("Redis数据验证失败: {}", e.getMessage());
            return false;
        }
    }
}