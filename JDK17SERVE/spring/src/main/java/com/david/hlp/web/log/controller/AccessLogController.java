package com.david.hlp.web.log.controller;

import com.david.hlp.web.log.model.AccessLogStats;
import com.david.hlp.web.log.model.AllStats;
import com.david.hlp.web.log.model.IpStats;
import com.david.hlp.web.log.model.TimeStats;
import com.david.hlp.web.log.model.UrlStats;
import com.david.hlp.web.log.service.AccessLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/logs")
public class AccessLogController {

    @Autowired
    private AccessLogService accessLogService;

    /**
     * 获取访问日志分析概要数据
     */
    @GetMapping("/summary")
    public AccessLogStats getAccessLogSummary() {
        return accessLogService.getCachedAnalysisResults();
    }

    /**
     * 获取IP统计数据
     */
    @GetMapping("/ip-stats")
    public IpStats getIpStats() {
        return accessLogService.getIpStats();
    }

    /**
     * 获取URL统计数据
     */
    @GetMapping("/url-stats")
    public UrlStats getUrlStats() {
        return accessLogService.getUrlStats();
    }

    /**
     * 获取时间统计数据
     */
    @GetMapping("/time-stats")
    public TimeStats getTimeStats() {
        return accessLogService.getTimeStats();
    }

    /**
     * 获取所有统计数据
     */
    @GetMapping("/all-stats")
    public AllStats getAllStats() {
        AccessLogStats summary = accessLogService.getCachedAnalysisResults();
        IpStats ipStats = accessLogService.getIpStats();
        UrlStats urlStats = accessLogService.getUrlStats();
        TimeStats timeStats = accessLogService.getTimeStats();

        AllStats allStats = AllStats.builder()
                .summary(summary)
                .ipStats(ipStats)
                .urlStats(urlStats)
                .timeStats(timeStats)
                .build();

        return allStats;
    }

    /**
     * 获取异常检测数据
     */
    @GetMapping("/anomaly-stats")
    public Map<String, Object> getAnomalyStats() {
        return accessLogService.getAnomalyStats();
    }

    /**
     * 获取用户行为分析数据
     */
    @GetMapping("/user-behavior")
    public Map<String, Object> getUserBehaviorStats() {
        return accessLogService.getUserBehavior();
    }

    /**
     * 获取热力图数据（按日期和小时的访问分布）
     */
    @GetMapping("/heatmap")
    public Map<String, Object> getHeatmapData() {
        Map<String, Object> timeStats = accessLogService.getTimeBasedStats();
        if (timeStats != null && timeStats.containsKey("heatmapData")) {
            return (Map<String, Object>) timeStats.get("heatmapData");
        }
        return new HashMap<>();
    }

    /**
     * 验证Redis数据一致性
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateRedisData() {
        boolean isValid = accessLogService.validateRedisData();
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("timestamp", new Date());
        return ResponseEntity.ok(response);
    }
}
