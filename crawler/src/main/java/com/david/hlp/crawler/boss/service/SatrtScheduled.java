package com.david.hlp.crawler.boss.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.david.hlp.crawler.boss.service.boss_2024.DataScrapingService;
import org.springframework.scheduling.annotation.Scheduled;
import com.david.hlp.crawler.common.threadpool.ScrapeBossUrlThreadPool;
import org.springframework.beans.factory.annotation.Qualifier;
import com.david.hlp.crawler.common.threadpool.ParseBossHtmlDataThreadPool;
import lombok.extern.slf4j.Slf4j;
import com.david.hlp.crawler.common.threadpool.ScrapeBossDataThreadPool;
@Service
@RequiredArgsConstructor
@Slf4j
public class SatrtScheduled {
    @Qualifier("boss_2024")
    private final DataScrapingService dataScrapingService;
    @Qualifier("boss_2025")
    private final com.david.hlp.crawler.boss.service.boss_2025.DataScrapingService boss2025DataScrapingService;

    // @Scheduled(fixedDelay = 1000 * 60 * 1)
    // public void startScrapeBossUrl() {
    //     ScrapeBossUrlThreadPool.executeWithSpringContext(() -> {
    //         dataScrapingService.scrapeBossUrl();
    //     });
    // }

    @Scheduled(fixedDelay = 1000 * 60 * 1)
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

    @Scheduled(fixedDelay = 1000 * 60 * 1)
    public void startScrapeBossData2025() {
        ScrapeBossUrlThreadPool.executeWithSpringContext(() -> {
            boss2025DataScrapingService.scrapeBossUrl();
        });
    }
}
