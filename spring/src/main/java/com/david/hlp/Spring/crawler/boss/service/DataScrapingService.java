package com.david.hlp.Spring.crawler.boss.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.david.hlp.Spring.common.threadpool.ScrapeBossUrlThreadPool;
import com.david.hlp.Spring.crawler.boss.entity.CityData;
import com.david.hlp.Spring.crawler.boss.entity.HTMLData;
import com.david.hlp.Spring.crawler.boss.entity.JobDetailEntity;
import com.david.hlp.Spring.crawler.boss.entity.PositionData;
import com.david.hlp.Spring.crawler.boss.mapper.CityDataMapper;
import com.david.hlp.Spring.crawler.boss.mapper.HtmlDataMapper;
import com.david.hlp.Spring.crawler.boss.mapper.JobDetailEntityMapper;
import com.david.hlp.Spring.crawler.boss.mapper.PositionDataMapper;
import com.david.hlp.Spring.crawler.boss.model.JobDetailData;
import com.david.hlp.Spring.common.threadpool.ScrapeBossDataThreadPool;
import com.david.hlp.Spring.common.threadpool.ParseBossHtmlDataThreadPool;
import com.david.hlp.Spring.crawler.proxy.model.entity.ProxyInfo;
import com.david.hlp.Spring.crawler.proxy.service.http.ProxyToolService;
import com.david.hlp.Spring.crawler.boss.exception.IpBlockedException;

import org.springframework.scheduling.annotation.Scheduled;
import java.util.List;
import java.util.Random;
import java.io.IOException;
import org.springframework.dao.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
@Slf4j
@Component
@RequiredArgsConstructor
public class DataScrapingService {
    private final CityDataMapper cityDataMapper;
    private final PositionDataMapper positionDataMapper;
    private final HtmlDataMapper htmlDataMapper;
    private final HtmlParserService htmlParserService;
    private final WebCrawlerService webCrawlerService;
    private final JobDetailEntityMapper jobDetailEntityMapper;
    @Qualifier("HttpProxyToolService")
    private final ProxyToolService proxyToolService;
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        log.info("开始初始化数据爬取服务");
    }
        /**
     * 休眠指定时间
     *
     * @param millis 休眠时间(毫秒)
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("线程休眠被中断");
        }
    }

    /**
     * 使用线程池爬取Boss网站的URL
     * 每30分钟执行一次，在上午8点到晚上6点之间
     */
    // 运行间隔30分钟
    @Scheduled(fixedDelay = 1000 * 60 * 20)
    // @Scheduled(cron = "0 */30 8-18 * * *")
    public void scrapeBossUrl() {
        ScrapeBossUrlThreadPool.executeWithSpringContext(() -> {
            List<CityData> cityDataList = cityDataMapper.listAll();
            List<PositionData> positionDataList = positionDataMapper.listAll();
            if (cityDataList.isEmpty() || positionDataList.isEmpty()) {
                return;
            }
            CityData randomCity = cityDataList.get(random.nextInt(cityDataList.size()));
            PositionData randomPosition = positionDataList.get(random.nextInt(positionDataList.size()));
            for (int i = 0; i < 10; i++) {
                log.info("开始爬取Boss网站URL: {}", randomCity.getName() + " " + randomPosition.getName());
                List<HTMLData> htmlDataList = webCrawlerService.getUrlWithSelenium(
                    "position",
                    randomCity.getCode().toString(),
                    randomPosition.getCode().toString(),
                    randomCity.getName(),
                    randomPosition.getName(),
                    i
                );
                for (HTMLData htmlData : htmlDataList) {
                    try {
                        // 先检查URL是否已存在
                        HTMLData existingData = htmlDataMapper.getByUrl(htmlData.getUrl());
                        if (existingData != null) {
                            log.info("URL已存在，跳过插入: {}", htmlData.getUrl());
                            continue;
                        }
                        htmlDataMapper.insert(htmlData);
                        log.debug("成功插入HTML数据，URL: {}", htmlData.getUrl());
                    } catch (DuplicateKeyException e) {
                        // 捕获重复键异常，记录日志但不中断处理
                        log.info("URL重复，跳过插入: {}", htmlData.getUrl());
                    } catch (Exception e) {
                        // 捕获其他异常
                        log.error("插入HTML数据时发生错误，URL: {}，错误: {}", htmlData.getUrl(), e.getMessage());
                    }
                }
                sleep((long)(random.nextDouble() * 50000 + 10000));
            }
        });
    }

    /**
     * 使用线程池爬取Boss网站的数据
     * 每20分钟执行一次，在上午8点到晚上6点之间
     */
    // 运行间隔30分钟
    @Scheduled(fixedDelay = 1000 * 60 * 1)
    // @Scheduled(cron = "0 */30 8-18 * * *")
    public void scrapeBossData() {
        ScrapeBossDataThreadPool.executeWithSpringContext(() -> {
            List<HTMLData> htmlDataList = htmlDataMapper.listByStatus(0, 10);
            for (HTMLData htmlData : htmlDataList) {
                try {
                    log.info("开始爬取Boss网站数据: {}", htmlData.getUrl());
                    htmlData = webCrawlerService.getHtmlContent(htmlData);
                    htmlData.setStatus(1);
                    htmlDataMapper.update(htmlData);
                    sleep((long)(random.nextDouble() * 50000 + 10000));
                } catch (Exception e) {
                    handleException(e, htmlData, "爬取Boss网站数据");
                }
            }
        });
    }

    /**
     * 处理被封锁的代理
     */
    private void handleBlockedProxy() {
        try {
            // 获取当前使用的HTTPS代理
            List<ProxyInfo> currentProxies = proxyToolService.listProxies(1);
            if (currentProxies != null && !currentProxies.isEmpty()) {
                ProxyInfo blockedProxy = currentProxies.get(0);
                log.info("移除被封锁的HTTPS代理: {}:{}", blockedProxy.getIp(), blockedProxy.getPort());
                proxyToolService.removeProxy(blockedProxy);
            } else {
                log.warn("未找到当前使用的HTTPS代理，无法移除");
            }
        } catch (Exception ex) {
            log.error("移除HTTPS代理时发生错误: {}", ex.getMessage());
        }
    }

    /**
     * 处理异常情况
     *
     * @param e 捕获的异常
     * @param htmlData HTML数据对象
     * @param operation 当前执行的操作描述
     */
    private void handleException(Exception e, HTMLData htmlData, String operation) {
        if (isIpBlockException(e)) {
            handleIpBlockException(e, htmlData);
        } else {
            handleGenericException(e, htmlData, operation);
        }
    }

    /**
     * 判断是否为IP访问受限异常
     *
     * @param e 捕获的异常
     * @return 是否为IP访问受限异常
     */
    private boolean isIpBlockException(Exception e) {
        return e instanceof IpBlockedException ||
               (e.getMessage() != null && (e.getMessage().contains("IP访问受限") ||
                                          e.getMessage().contains("IP被封锁")));
    }

    /**
     * 处理IP访问受限异常
     *
     * @param e 捕获的异常
     * @param htmlData HTML数据对象
     */
    private void handleIpBlockException(Exception e, HTMLData htmlData) {
        log.error("代理IP被限制访问，需要处理异常代理并重新爬取: {}, URL: {}", e.getMessage(), htmlData.getUrl());

        // 处理异常代理
        handleBlockedProxy();

        // 将任务标记为需要重新爬取
        updateHtmlDataStatus(htmlData, 0);

        // 延长休眠时间
        sleep(1000 * 60 * 10); // 休眠10分钟
    }

    /**
     * 处理一般性异常
     *
     * @param e 捕获的异常
     * @param htmlData HTML数据对象
     * @param operation 当前执行的操作描述
     */
    private void handleGenericException(Exception e, HTMLData htmlData, String operation) {
        log.error("{}时发生错误: {}", operation, e.getMessage());

        if (log.isDebugEnabled()) {
            log.debug("异常详情:", e);
        }

        // 将此记录标记为处理失败
        updateHtmlDataStatus(htmlData, 4);
    }

    /**
     * 更新HTML数据状态
     *
     * @param htmlData HTML数据对象
     * @param status 新状态
     */
    private void updateHtmlDataStatus(HTMLData htmlData, int status) {
        try {
            htmlData.setStatus(status);
            htmlDataMapper.update(htmlData);
        } catch (Exception e) {
            log.error("更新HTML数据状态失败: {}", e.getMessage());
        }
    }

    /**
     * 使用线程池解析Boss HTML数据
     * 每30分钟执行一次，在上午8点到晚上6点之间
     */
    @Transactional
    // 运行间隔30分钟
    @Scheduled(fixedDelay = 1000 * 60 * 1)
    // @Scheduled(cron = "0 */30 8-18 * * *")
    public void parseBossHtmlData() {
        ParseBossHtmlDataThreadPool.executeWithSpringContext(() -> {
            List<HTMLData> htmlDataList = htmlDataMapper.listByStatus(1, 10);
            for (HTMLData htmlData : htmlDataList) {
                try {
                    // 解析HTML内容获取职位详情数据
                    JobDetailData jobDetailData;
                    try {
                        log.info("开始解析HTML内容: {}", htmlData.getUrl());
                        jobDetailData = htmlParserService.parseJobDetail(htmlData.getHtmlContent());
                    } catch (IOException e) {
                        handleException(e, htmlData, "解析HTML内容");
                        continue;
                    }

                    // 使用ObjectMapper将JobDetailData对象转换为JSON字符串
                    String detailDataJson;
                    try {
                        detailDataJson = objectMapper.writeValueAsString(jobDetailData);
                    } catch (Exception e) {
                        log.error("无法将JobDetailData转换为JSON: {}", e.getMessage());
                        // 设置为有效的JSON对象，避免插入失败
                        detailDataJson = "{}";
                    }

                    JobDetailEntity jobDetailEntity = JobDetailEntity.builder()
                        .positionId(htmlData.getBasePositionCode())
                        .companyId(htmlData.getBaseCityCode())
                        .positionName(htmlData.getBasePosition())
                        .companyName(htmlData.getBaseCity())
                        .detailData(detailDataJson)
                        .build();

                    jobDetailEntityMapper.insert(jobDetailEntity);

                    // 更新HTML数据状态为已处理
                    updateHtmlDataStatus(htmlData, 2);

                    log.info("成功处理HTML数据，URL: {}", htmlData.getUrl());
                } catch (DataIntegrityViolationException e) {
                    log.error("数据完整性错误，无法保存职位详情: {}", e.getMessage());
                    updateHtmlDataStatus(htmlData, 4);
                } catch (Exception e) {
                    handleGenericException(e, htmlData, "处理HTML数据");
                }
            }
        });
    }
}
