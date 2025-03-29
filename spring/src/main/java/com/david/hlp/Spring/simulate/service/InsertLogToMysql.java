package com.david.hlp.Spring.simulate.service;

import org.springframework.stereotype.Service;

import com.david.hlp.Spring.simulate.entity.NginxAccessLog;
import com.david.hlp.Spring.simulate.mapper.LogToMysqlMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsertLogToMysql {

    private final SimulateLogService simulateLogService;
    private final LogToMysqlMapper logToMysqlMapper;

    // 每批次插入的最大记录数，防止一次性插入过多导致MySQL异常
    private static final int BATCH_SIZE = 50;
    // 默认生成的日志数量
    private static final int DEFAULT_LOG_COUNT = 100;

    // @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void insertLogToMysql() {
        try {
            log.info("开始定时生成和插入模拟日志数据...");
            long startTime = System.currentTimeMillis();
            // 生成模拟日志数据
            List<NginxAccessLog> logs = simulateLogService.generateMockLogs(DEFAULT_LOG_COUNT);
            if (logs == null || logs.isEmpty()) {
                log.warn("未能基于真实数据生成有效的模拟日志，本次任务结束");
                return;
            }
            log.info("成功生成{}条模拟日志数据，准备分批插入到数据库", logs.size());
            // 分批处理
            int totalInserted = 0;
            for (int i = 0; i < logs.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, logs.size());
                List<NginxAccessLog> batchLogs = logs.subList(i, endIndex);
                try {
                    logToMysqlMapper.insertAllLogs(batchLogs);
                    totalInserted += batchLogs.size();
                    log.info("成功插入第{}批数据，共{}条，当前进度: {}/{}",
                            (i / BATCH_SIZE + 1), batchLogs.size(), totalInserted, logs.size());
                } catch (Exception e) {
                    log.error("插入第{}批数据时出错: {}，尝试单条插入", (i / BATCH_SIZE + 1), e.getMessage());
                    // 尝试单条插入
                    for (NginxAccessLog accessLog : batchLogs) {
                        try {
                            logToMysqlMapper.insertLog(accessLog);
                            totalInserted++;
                        } catch (Exception ex) {
                            log.error("单条插入日志失败: {}", ex.getMessage());
                        }
                    }
                }
            }
            long endTime = System.currentTimeMillis();
            log.info("本次定时任务完成，共插入{}条数据，耗时{}毫秒", 
                    totalInserted, (endTime - startTime));
        } catch (Exception e) {
            log.error("执行定时插入日志任务时发生异常: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
