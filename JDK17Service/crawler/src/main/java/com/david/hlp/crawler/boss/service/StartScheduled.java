package com.david.hlp.crawler.boss.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import com.david.hlp.crawler.common.threadpool.ScrapeBossUrlThreadPool;
import com.david.hlp.crawler.common.threadpool.ParseBossHtmlDataThreadPool;
import lombok.extern.slf4j.Slf4j;
import com.david.hlp.crawler.common.threadpool.ScrapeBossDataThreadPool;

/**
 * Boss招聘网站爬虫定时任务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StartScheduled {
    private final DataScrapingService dataScrapingService;

    @Scheduled(fixedDelay = 1000 * 60)
    public void startScrapeBossUrl() {
        ScrapeBossUrlThreadPool.executeWithSpringContext(dataScrapingService::scrapeBossUrl);
    }

    @Scheduled(fixedDelay = 1000 * 30)
    public void startParseBossHtmlData() {
        ParseBossHtmlDataThreadPool.executeWithSpringContext(dataScrapingService::parseBossHtmlData);
    }

    @Scheduled(fixedDelay = 1000 * 60)
    public void startScrapeBossData() {
        ScrapeBossDataThreadPool.executeWithSpringContext(dataScrapingService::scrapeBossData);
    }
}
