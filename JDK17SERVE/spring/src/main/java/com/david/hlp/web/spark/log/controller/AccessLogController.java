package com.david.hlp.web.spark.log.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.david.hlp.web.common.entity.Result;
import com.david.hlp.web.spark.log.service.AccessLogService;
import com.david.hlp.commons.entity.logs.AccessLogAnalysisResult;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/system/logs")
@RequiredArgsConstructor
public class AccessLogController {
    private final AccessLogService accessLogService;

    /**
     * 获取所有访问日志统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getAllStats() {
        try {
            Map<String, Object> allStats = accessLogService.getAllStats();
            return Result.success(allStats);
        } catch (Exception e) {
            log.error("获取访问日志统计失败: {}", e.getMessage(), e);
            return Result.error(500, "获取访问日志统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取热力图数据
     */
    @GetMapping("/stats/heatmap")
    public Result<Map<String, Object>> getHeatMapStats() {
        try {
            Map<String, Object> heatmapStats = accessLogService.getTimeBasedStats();
            return Result.success(heatmapStats);
        } catch (Exception e) {
            log.error("获取热力图数据失败: {}", e.getMessage(), e);
            return Result.error(500, "获取热力图数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取IP统计数据
     */
    @GetMapping("/stats/ip")
    public Result<Map<String, Object>> getIpStats() {
        try {
            Map<String, Object> ipStats = accessLogService.getIpStats();
            return Result.success(ipStats);
        } catch (Exception e) {
            log.error("获取IP统计数据失败: {}", e.getMessage(), e);
            return Result.error(500, "获取IP统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取HTTP方法统计数据
     */
    @GetMapping("/stats/methods")
    public Result<Map<String, Object>> getHttpMethodStats() {
        try {
            Map<String, Object> methodStats = accessLogService.getHttpMethodStats();
            return Result.success(methodStats);
        } catch (Exception e) {
            log.error("获取HTTP方法统计数据失败: {}", e.getMessage(), e);
            return Result.error(500, "获取HTTP方法统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作日访问模式数据
     */
    @GetMapping("/stats/weekday")
    public Result<Map<String, Object>> getWeekdayStats() {
        try {
            Map<String, Object> weekdayStats = accessLogService.getWeekdayStats();
            return Result.success(weekdayStats);
        } catch (Exception e) {
            log.error("获取工作日统计数据失败: {}", e.getMessage(), e);
            return Result.error(500, "获取工作日统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取浏览器使用情况数据
     */
    @GetMapping("/stats/browser")
    public Result<Map<String, Object>> getBrowserStats() {
        try {
            Map<String, Object> browserStats = accessLogService.getBrowserStats();
            return Result.success(browserStats);
        } catch (Exception e) {
            log.error("获取浏览器统计数据失败: {}", e.getMessage(), e);
            return Result.error(500, "获取浏览器统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取完整的分析结果
     */
    @GetMapping("/analysis/complete")
    public Result<AccessLogAnalysisResult> getCompleteAnalysis() {
        try {
            AccessLogAnalysisResult result = accessLogService.getCompleteAnalysisResult();
            if (result != null) {
                return Result.success(result);
            } else {
                return Result.error(404, "分析结果不可用，请检查Spark分析任务是否正常运行");
            }
        } catch (Exception e) {
            log.error("获取完整分析结果失败: {}", e.getMessage(), e);
            return Result.error(500, "获取完整分析结果失败: " + e.getMessage());
        }
    }

    /**
     * 检查数据可用性
     */
    @GetMapping("/availability")
    public Result<Map<String, Object>> getDataAvailability() {
        try {
            Map<String, Object> availability = accessLogService.getDataAvailability();
            return Result.success(availability);
        } catch (Exception e) {
            log.error("检查数据可用性失败: {}", e.getMessage(), e);
            return Result.error(500, "检查数据可用性失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> availability = accessLogService.getDataAvailability();
            boolean isHealthy = (Boolean) availability.getOrDefault("allDataAvailable", false);

            Map<String, Object> health = Map.of(
                    "status", isHealthy ? "healthy" : "unhealthy",
                    "dataAvailability", availability,
                    "timestamp", System.currentTimeMillis());

            return Result.success(health);
        } catch (Exception e) {
            log.error("健康检查失败: {}", e.getMessage(), e);
            return Result.error(500, "健康检查失败: " + e.getMessage());
        }
    }
}
