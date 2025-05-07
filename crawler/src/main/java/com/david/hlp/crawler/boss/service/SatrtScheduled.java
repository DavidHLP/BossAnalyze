package com.david.hlp.crawler.boss.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import com.david.hlp.crawler.common.threadpool.ScrapeBossUrlThreadPool;
import com.david.hlp.crawler.common.threadpool.ParseBossHtmlDataThreadPool;
import lombok.extern.slf4j.Slf4j;
import com.david.hlp.crawler.common.threadpool.ScrapeBossDataThreadPool;
@Service
@RequiredArgsConstructor
@Slf4j
public class SatrtScheduled {
    private final DataScrapingService dataScrapingService;

    @Scheduled(fixedDelay = 1000 * 60 * 1)
    public void startScrapeBossUrl() {
        ScrapeBossUrlThreadPool.executeWithSpringContext(() -> {
            dataScrapingService.scrapeBossUrl();
        });
    }

    @Scheduled(fixedDelay = 1000 * 30 * 1)
    public void startParseBossHtmlData() {
        ParseBossHtmlDataThreadPool.executeWithSpringContext(() -> {
            dataScrapingService.parseBossHtmlData();
        });
    }
    @Scheduled(fixedDelay = 1000 * 60 * 1)
    public void startScrapeBossData() {
        ScrapeBossDataThreadPool.executeWithSpringContext(() -> {
            dataScrapingService.scrapeBossData();
        });
    }
}
