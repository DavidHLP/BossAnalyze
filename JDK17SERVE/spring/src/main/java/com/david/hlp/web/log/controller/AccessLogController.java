package com.david.hlp.web.log.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.david.hlp.web.common.result.Result;
import com.david.hlp.web.log.service.AccessLogService;
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
        Map<String, Object> allStats = accessLogService.getAllStats();
        return Result.success(allStats);
    }
}
