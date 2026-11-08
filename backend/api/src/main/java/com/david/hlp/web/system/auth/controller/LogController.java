package com.david.hlp.web.system.auth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.HashMap;

/**
 * 日志分析控制器 - SpringCloud网关
 * 通过调用Spark模块获取处理后的数据
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/system/logs")
@CrossOrigin(origins = "*")
public class LogController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spark.service.url:http://localhost:8082}")
    private String sparkServiceUrl;

    /**
     * 获取仪表板统计数据
     * 调用Spark模块的处理结果
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            String url = sparkServiceUrl + "/api/log/access/dashboard/stats";
            log.info("调用Spark服务获取仪表板数据: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() == null || response.getBody().isEmpty()) {
                log.warn("Spark服务返回空数据");
                return ResponseEntity.ok(getDefaultDashboardData());
            }

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("获取仪表板数据失败: {}", e.getMessage(), e);
            // 返回默认数据而不是错误，确保前端能正常显示
            return ResponseEntity.ok(getDefaultDashboardData());
        }
    }

    /**
     * 获取访问日志统计（兼容现有接口）
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAccessLogStats() {
        try {
            String url = sparkServiceUrl + "/api/log/access/analysis/complete";
            log.info("调用Spark服务获取完整分析结果: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getBody() == null) {
                return ResponseEntity.ok(getDefaultStats());
            }

            // 转换数据格式以兼容前端现有接口
            Map<String, Object> convertedData = convertAnalysisResultToStats(response.getBody());
            return ResponseEntity.ok(convertedData);

        } catch (Exception e) {
            log.error("获取访问日志统计失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(getDefaultStats());
        }
    }

    /**
     * 获取热力图数据
     */
    @GetMapping("/heatmap")
    public ResponseEntity<Map<String, Object>> getHeatMapData() {
        try {
            String url = sparkServiceUrl + "/api/log/access/analysis/heatmap";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return ResponseEntity.ok(response.getBody() != null ? response.getBody() : new HashMap<>());
        } catch (Exception e) {
            log.error("获取热力图数据失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(new HashMap<>());
        }
    }

    /**
     * 获取IP统计数据
     */
    @GetMapping("/ip-statistics")
    public ResponseEntity<Map<String, Object>> getIpStatistics() {
        try {
            String url = sparkServiceUrl + "/api/log/access/analysis/ip-statistics";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return ResponseEntity.ok(response.getBody() != null ? response.getBody() : new HashMap<>());
        } catch (Exception e) {
            log.error("获取IP统计数据失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(new HashMap<>());
        }
    }

    /**
     * 获取HTTP方法统计
     */
    @GetMapping("/http-methods")
    public ResponseEntity<Map<String, Object>> getHttpMethodStatistics() {
        try {
            String url = sparkServiceUrl + "/api/log/access/analysis/http-methods";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return ResponseEntity.ok(response.getBody() != null ? response.getBody() : new HashMap<>());
        } catch (Exception e) {
            log.error("获取HTTP方法统计失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(new HashMap<>());
        }
    }

    /**
     * 获取浏览器统计
     */
    @GetMapping("/browser-usage")
    public ResponseEntity<Map<String, Object>> getBrowserStatistics() {
        try {
            String url = sparkServiceUrl + "/api/log/access/analysis/browser-usage";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return ResponseEntity.ok(response.getBody() != null ? response.getBody() : new HashMap<>());
        } catch (Exception e) {
            log.error("获取浏览器统计失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(new HashMap<>());
        }
    }

    /**
     * 手动触发日志分析
     */
    @PostMapping("/analysis/trigger")
    public ResponseEntity<String> triggerAnalysis() {
        try {
            String url = sparkServiceUrl + "/api/log/access/analysis/trigger";
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            return ResponseEntity.ok(response.getBody() != null ? response.getBody() : "分析任务已触发");
        } catch (Exception e) {
            log.error("触发日志分析失败: {}", e.getMessage(), e);
            return ResponseEntity.ok("触发分析任务失败: " + e.getMessage());
        }
    }

    /**
     * 服务健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // 检查Spark服务连接
            String url = sparkServiceUrl + "/api/log/access/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            health.put("status", "UP");
            health.put("sparkService", "UP");
            health.put("sparkResponse", response.getBody());
            health.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            health.put("status", "DEGRADED");
            health.put("sparkService", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
        }

        return ResponseEntity.ok(health);
    }

    /**
     * 转换分析结果为统计数据格式
     */
    private Map<String, Object> convertAnalysisResultToStats(Map<String, Object> analysisResult) {
        Map<String, Object> stats = new HashMap<>();

        // 基础统计
        stats.put("totalVisits", analysisResult.get("totalVisits"));
        stats.put("uniqueIps", analysisResult.get("uniqueVisitors"));

        // 热力图数据
        Object heatMapData = analysisResult.get("heatMapData");
        if (heatMapData != null) {
            stats.put("heatmap", heatMapData);
        }

        // IP统计
        Object ipStatistics = analysisResult.get("ipStatistics");
        if (ipStatistics != null) {
            stats.put("ipStats", ipStatistics);
        }

        // HTTP方法统计
        Object httpMethodStats = analysisResult.get("httpMethodStatistics");
        if (httpMethodStats != null) {
            stats.put("httpMethods", httpMethodStats);
        }

        // 星期统计
        Object weekdayStats = analysisResult.get("weekdayStatistics");
        if (weekdayStats != null) {
            stats.put("weekdayStats", weekdayStats);
        }

        // 浏览器统计
        Object browserStats = analysisResult.get("browserStatistics");
        if (browserStats != null) {
            stats.put("browserStats", browserStats);
        }

        return stats;
    }

    /**
     * 获取默认仪表板数据
     */
    private Map<String, Object> getDefaultDashboardData() {
        Map<String, Object> defaultData = new HashMap<>();

        // 概览数据
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalVisits", 0L);
        overview.put("uniqueVisitors", 0L);
        overview.put("avgSessionDuration", 0.0);
        overview.put("bounceRate", 0.0);
        overview.put("errorRate", 0.0);

        // 实时统计
        Map<String, Object> realTimeStats = new HashMap<>();
        realTimeStats.put("onlineUsers", 0);
        realTimeStats.put("recentVisits", 0);
        realTimeStats.put("activePages", 0);

        defaultData.put("overview", overview);
        defaultData.put("realTimeStats", realTimeStats);
        defaultData.put("generatedAt", System.currentTimeMillis());

        return defaultData;
    }

    /**
     * 获取默认统计数据
     */
    private Map<String, Object> getDefaultStats() {
        Map<String, Object> defaultStats = new HashMap<>();

        defaultStats.put("totalVisits", 0);
        defaultStats.put("uniqueIps", 0);
        defaultStats.put("avgResponseTime", 0.0);
        defaultStats.put("errorRate", 0.0);

        // 空的统计对象
        defaultStats.put("heatmap", new HashMap<>());
        defaultStats.put("ipStats", new HashMap<>());
        defaultStats.put("httpMethods", new HashMap<>());
        defaultStats.put("weekdayStats", new HashMap<>());
        defaultStats.put("browserStats", new HashMap<>());

        return defaultStats;
    }
}
