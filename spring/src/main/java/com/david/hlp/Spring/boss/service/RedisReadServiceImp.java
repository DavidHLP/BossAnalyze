package com.david.hlp.Spring.boss.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

import com.david.hlp.Spring.common.util.RedisCache;
import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisReadServiceImp {
    private final RedisCache redisCache;
    private final Gson gson;
    @Value("${spark.redis.key-prefix:job:}")
    private String redisKeyPrefix;

    @Value("${spark.redis.data.expire-days:7}")
    private int redisDataExpireDays;
    /**
     * 从Redis获取所有职位分析数据
     * @return 职位分析数据列表
     */
    public List<Map<String, Object>> listAllJobData() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            // 获取所有职位
            List<String> positions = redisCache.getCacheObject(redisKeyPrefix + "positions");
            log.info("positions: {}", positions);
            if (positions != null && !positions.isEmpty()) {
                for (String position : positions) {
                    String redisHashKey = redisKeyPrefix + "data:" + position;
                    Map<String, String> hashEntries = redisCache.getCacheMap(redisHashKey);
                    if (hashEntries != null && !hashEntries.isEmpty()) {
                        for (Map.Entry<String, String> entry : hashEntries.entrySet()) {
                            if (entry.getValue() != null) {
                                // 将JSON转换为Map
                                @SuppressWarnings("unchecked")
                                Map<String, Object> dataMap = gson.fromJson(entry.getValue(), Map.class);
                                result.add(dataMap);
                            }
                        }
                    }
                }
                log.info("成功从Redis获取{}条职位数据记录", result.size());
            }
        } catch (Exception e) {
            log.error("从Redis获取数据时出错: {}", e.getMessage(), e);
        }
        return result;
    }

    /**
     * 按职位名称获取数据
     * @param position 职位名称
     * @return 职位分析数据列表
     */
    public List<Map<String, Object>> listJobDataByPosition(String position) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String redisHashKey = redisKeyPrefix + "data:" + position;
            Map<String, String> hashEntries = redisCache.getCacheMap(redisHashKey);

            if (hashEntries != null && !hashEntries.isEmpty()) {
                for (Map.Entry<String, String> entry : hashEntries.entrySet()) {
                    if (entry.getValue() != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = gson.fromJson(entry.getValue(), Map.class);
                        result.add(dataMap);
                    }
                }
                log.info("成功获取职位[{}]的{}条数据记录", position, result.size());
            }
        } catch (Exception e) {
            log.error("获取职位[{}]数据时出错: {}", position, e.getMessage(), e);
        }
        return result;
    }

    /**
     * 按职位名称和时间获取数据
     * @param position 职位名称
     * @param timePeriod 时间段(年月)
     * @return 职位分析数据列表
     */
    public List<Map<String, Object>> listJobDataByPositionAndTime(String position, String timePeriod) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            // 获取该职位和时间段下的所有城市
            String posTimeKey = redisKeyPrefix + "pos_time:" + position + ":" + timePeriod;
            Set<String> cities = redisCache.getCacheSet(posTimeKey);

            if (cities != null && !cities.isEmpty()) {
                String redisHashKey = redisKeyPrefix + "data:" + position;

                for (String city : cities) {
                    String hashField = timePeriod + ":" + city;
                    String jsonData = redisCache.getCacheMapValue(redisHashKey, hashField);

                    if (jsonData != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = gson.fromJson(jsonData, Map.class);
                        result.add(dataMap);
                    }
                }
                log.info("成功获取职位[{}]在时间段[{}]的{}条数据记录", position, timePeriod, result.size());
            }
        } catch (Exception e) {
            log.error("获取职位[{}]在时间段[{}]的数据时出错: {}", position, timePeriod, e.getMessage(), e);
        }
        return result;
    }

    /**
     * 获取所有职位名称
     * @return 职位名称列表
     */
    public List<String> listAllPositions() {
        List<String> positions = redisCache.getCacheObject(redisKeyPrefix + "positions");
        return positions != null ? positions : new ArrayList<>();
    }

    /**
     * 获取所有时间段
     * @return 时间段列表
     */
    public List<String> listAllTimePeriods() {
        List<String> timePeriods = redisCache.getCacheObject(redisKeyPrefix + "time_periods");
        return timePeriods != null ? timePeriods : new ArrayList<>();
    }

    /**
     * 获取所有城市
     * @return 城市列表
     */
    public List<String> listAllCities() {
        List<String> cities = redisCache.getCacheObject(redisKeyPrefix + "cities");
        return cities != null ? cities : new ArrayList<>();
    }
}
