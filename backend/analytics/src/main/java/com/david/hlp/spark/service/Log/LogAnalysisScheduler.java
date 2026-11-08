package com.david.hlp.spark.service.Log;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spark.logs.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class LogAnalysisScheduler {

    private final LogServiceImp logService;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void analyzeLogs() {
        logService.analyzeLogs();
    }
}
