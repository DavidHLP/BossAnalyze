package com.david.hlp.spark.controller.Log;

import com.david.hlp.commons.entity.logs.*;
import com.david.hlp.spark.service.Log.AccessLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 访问日志分析控制器
 * 提供日志分析数据的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/log/access")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccessLogController {

    private final AccessLogService accessLogService;

    /**
     * 获取完整的日志分析结果
     * 
     * @return 完整的分析结果
     */
    @GetMapping("/analysis/complete")
    public ResponseEntity<AccessLogAnalysisResult> getCompleteAnalysisResult() {
        try {
            AccessLogAnalysisResult result = accessLogService.getCompleteAnalysisResult();
            if (result == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取完整分析结果失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取热力图数据
     * 
     * @return 热力图数据
     */
    @GetMapping("/analysis/heatmap")
    public ResponseEntity<HeatMapData> getHeatMapData() {
        try {
            HeatMapData heatMapData = accessLogService.getHeatMapData();
            if (heatMapData == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(heatMapData);
        } catch (Exception e) {
            log.error("获取热力图数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取IP统计数据
     * 
     * @return IP统计数据
     */
    @GetMapping("/analysis/ip-statistics")
    public ResponseEntity<IpStatistics> getIpStatistics() {
        try {
            IpStatistics ipStatistics = accessLogService.getIpStatistics();
            if (ipStatistics == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(ipStatistics);
        } catch (Exception e) {
            log.error("获取IP统计数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取HTTP方法统计数据
     * 
     * @return HTTP方法统计数据
     */
    @GetMapping("/analysis/http-methods")
    public ResponseEntity<HttpMethodStatistics> getHttpMethodStatistics() {
        try {
            HttpMethodStatistics httpMethodStatistics = accessLogService.getHttpMethodStatistics();
            if (httpMethodStatistics == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(httpMethodStatistics);
        } catch (Exception e) {
            log.error("获取HTTP方法统计数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取工作日统计数据
     * 
     * @return 工作日统计数据
     */
    @GetMapping("/analysis/weekday-patterns")
    public ResponseEntity<WeekdayStatistics> getWeekdayStatistics() {
        try {
            WeekdayStatistics weekdayStatistics = accessLogService.getWeekdayStatistics();
            if (weekdayStatistics == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(weekdayStatistics);
        } catch (Exception e) {
            log.error("获取工作日统计数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取浏览器统计数据
     * 
     * @return 浏览器统计数据
     */
    @GetMapping("/analysis/browser-usage")
    public ResponseEntity<BrowserStatistics> getBrowserStatistics() {
        try {
            BrowserStatistics browserStatistics = accessLogService.getBrowserStatistics();
            if (browserStatistics == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(browserStatistics);
        } catch (Exception e) {
            log.error("获取浏览器统计数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 手动触发日志分析任务
     * 
     * @return 操作结果
     */
    @PostMapping("/analysis/trigger")
    public ResponseEntity<String> triggerAnalysis() {
        try {
            // 在新线程中执行分析任务，避免阻塞
            new Thread(() -> {
                try {
                    accessLogService.analyzeLogs();
                    log.info("手动触发的日志分析任务完成");
                } catch (Exception e) {
                    log.error("手动触发的日志分析任务失败: {}", e.getMessage(), e);
                }
            }).start();

            return ResponseEntity.ok("日志分析任务已开始执行");
        } catch (Exception e) {
            log.error("触发日志分析任务失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("触发分析任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取分析类型列表
     * 
     * @return 支持的分析类型
     */
    @GetMapping("/analysis/types")
    public ResponseEntity<LogAnalysisType[]> getAnalysisTypes() {
        try {
            return ResponseEntity.ok(LogAnalysisType.values());
        } catch (Exception e) {
            log.error("获取分析类型失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据分析类型获取数据
     * 
     * @param analysisType 分析类型
     * @return 对应的分析数据
     */
    @GetMapping("/analysis/by-type/{analysisType}")
    public ResponseEntity<Object> getAnalysisByType(@PathVariable String analysisType) {
        try {
            LogAnalysisType type = LogAnalysisType.fromCode(analysisType);
            if (type == null) {
                return ResponseEntity.badRequest().build();
            }

            Object result = switch (type) {
                case HEATMAP -> accessLogService.getHeatMapData();
                case IP_STATISTICS -> accessLogService.getIpStatistics();
                case HTTP_METHODS -> accessLogService.getHttpMethodStatistics();
                case WEEKDAY_PATTERNS -> accessLogService.getWeekdayStatistics();
                case BROWSER_USAGE -> accessLogService.getBrowserStatistics();
                default -> accessLogService.getCompleteAnalysisResult();
            };

            if (result == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("根据类型获取分析数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 健康检查接口
     * 
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Access Log Service is running");
    }
}