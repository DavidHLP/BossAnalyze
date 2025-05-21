package com.david.hlp.crawler.boss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import com.david.hlp.crawler.boss.mapper.CityDataMapper;
import com.david.hlp.crawler.boss.mapper.PositionDataMapper;
import com.david.hlp.crawler.boss.mapper.JobListMapper;
import com.david.hlp.crawler.boss.mapper.HtmlDataMapper;
import com.david.hlp.crawler.boss.mapper.JobDetailEntityMapper;
import com.david.hlp.crawler.boss.entity.CityData;
import com.david.hlp.crawler.boss.entity.PositionData;
import com.david.hlp.crawler.boss.entity.HtmlData;
import com.david.hlp.crawler.boss.entity.JobDetailEntity;
import com.david.hlp.crawler.boss.model.JobList;
import com.david.hlp.crawler.boss.model.JobDetailData;
import com.david.hlp.crawler.boss.exception.IpBlockedException;
import com.david.hlp.crawler.common.threadpool.ScrapeBossDataThreadPool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.Random;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;

/**
 * Boss招聘网站数据爬取服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataScrapingService {
    // 常量定义
    private static final long NORMAL_SLEEP_MIN = 10000;
    private static final long NORMAL_SLEEP_MAX = 60000;
    private static final long IP_BLOCK_SLEEP_TIME = 600000; // 10分钟
    private static final int MAX_PAGE_COUNT = 10;

    // 状态常量
    private static final int SUCCESS_STATUS = 2;
    private static final int ERROR_STATUS = 4;
    private static final int RETRY_STATUS = 0;
    private static final int PENDING_PARSE_STATUS = 1;

    // 目标职位集合
    private static final Set<String> TARGET_POSITIONS = new HashSet<>(Arrays.asList(
            "Golang", "Java", "Python", "C++", "C#", "PHP", "JavaScript", "Node.js", "Ruby", "Swift", "Kotlin", "Go",
            "Rust", "Scala", "TypeScript", "C", "C#", "Objective-C", "Objective-C++", "Objective-C#", "Objective-C#++",
            "Objective-C#+++"));

    // 依赖注入
    private final CityDataMapper cityDataMapper;
    private final PositionDataMapper positionDataMapper;
    private final JobListMapper jobListMapper;
    private final HtmlDataMapper htmlDataMapper;
    private final JobDetailEntityMapper jobDetailEntityMapper;
    private final JsonScrapingService jsonScrapingService;
    private final HtmlParserService htmlParserService;
    private final WebCrawlerService webCrawlerService;
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        log.info("开始初始化数据爬取服务");
    }

    /**
     * 爬取Boss招聘职位URL
     */
    public void scrapeBossUrl() {
        try {
            // 获取并过滤数据
            List<CityData> cityDataList = cityDataMapper.listAll();
            List<PositionData> positionDataList = positionDataMapper.listAll();

            if (cityDataList.isEmpty() || positionDataList.isEmpty()) {
                log.warn("城市或职位数据为空，无法执行爬取");
                return;
            }

            // 过滤目标职位
            positionDataList.removeIf(position -> !TARGET_POSITIONS.contains(position.getName()));
            if (positionDataList.isEmpty()) {
                log.warn("过滤后没有符合条件的职位数据");
                return;
            }

            // 随机选择城市和职位
            CityData randomCity = cityDataList.get(random.nextInt(cityDataList.size()));
            PositionData randomPosition = positionDataList.get(random.nextInt(positionDataList.size()));
            String cityCode = randomCity.getCode().toString();
            String positionCode = randomPosition.getCode().toString();

            // 爬取每个页面的数据
            for (int page = 1; page <= MAX_PAGE_COUNT; page++) {
                try {
                    Map<String, List<String>> result = jsonScrapingService.scrapeJobListJson(
                            page, cityCode, positionCode);

                    List<String> jobUrls = result.get("urls");
                    List<String> jobJsons = result.get("json");

                    for (int i = 0; i < jobUrls.size(); i++) {
                        try {
                            // 保存职位列表数据
                            if (jobListMapper.selectByUrl(jobUrls.get(i)) == null) {
                                jobListMapper.insert(JobList.builder()
                                        .htmlUrl(jobUrls.get(i))
                                        .jsonData(jobJsons.get(i))
                                        .build());
                            }

                            // 保存HTML URL
                            if (htmlDataMapper.getByUrl(jobUrls.get(i)) == null) {
                                htmlDataMapper.insert(HtmlData.builder()
                                        .url(jobUrls.get(i))
                                        .baseCityCode(cityCode)
                                        .basePositionCode(positionCode)
                                        .baseCity(randomCity.getName())
                                        .basePosition(randomPosition.getName())
                                        .build());
                            }
                        } catch (DuplicateKeyException e) {
                            log.info("URL重复，跳过插入: {}", jobUrls.get(i));
                        } catch (Exception e) {
                            log.error("插入职位列表数据时发生错误，URL: {}，错误: {}", jobUrls.get(i), e.getMessage());
                        }
                    }
                } catch (JsonProcessingException e) {
                    log.error("解析职位列表JSON时发生错误: {}，城市：{}，职位：{}", e.getMessage(), cityCode, positionCode);
                }

                // 随机休眠一段时间，避免被反爬
                try {
                    Thread.sleep(
                            NORMAL_SLEEP_MIN + (long) (random.nextDouble() * (NORMAL_SLEEP_MAX - NORMAL_SLEEP_MIN)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            if (isIpBlockException(e)) {
                log.error("代理IP被限制访问，需要处理异常代理并重新爬取: {}", e.getMessage());
                sleep(IP_BLOCK_SLEEP_TIME);
            } else {
                log.error("爬取Boss网站URL时发生错误: {}", e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("异常详情:", e);
                }
            }
        }
    }

    /**
     * 使用线程池爬取Boss网站的HTML内容
     */
    public void scrapeBossData() {
        ScrapeBossDataThreadPool.executeWithSpringContext(this::fetchHtmlContent);
    }

    /**
     * 获取待处理的HTML内容
     */
    private void fetchHtmlContent() {
        // 处理不同状态的HTML数据
        List<HtmlData> htmlDataList = htmlDataMapper.listByStatus(RETRY_STATUS, 10); // 待处理
        for (HtmlData htmlData : htmlDataList) {
            processHtmlData(htmlData);
        }

        htmlDataList = htmlDataMapper.listByStatus(3, 10); // 错误重试
        for (HtmlData htmlData : htmlDataList) {
            processHtmlData(htmlData);
        }

        htmlDataList = htmlDataMapper.listByStatus(ERROR_STATUS, 10); // 成功但需更新
        for (HtmlData htmlData : htmlDataList) {
            processHtmlData(htmlData);
        }
    }

    /**
     * 处理单个HTML数据
     */
    private void processHtmlData(HtmlData htmlData) {
        try {
            log.info("开始爬取HTML内容: {}", htmlData.getUrl());
            htmlData = webCrawlerService.getHtmlContent(htmlData);
            htmlDataMapper.update(htmlData);
            randomSleep();
        } catch (Exception e) {
            if (isIpBlockException(e)) {
                log.error("代理IP被限制访问，需要处理异常代理并重新爬取: {}, URL: {}", e.getMessage(), htmlData.getUrl());
                updateHtmlDataStatus(htmlData, RETRY_STATUS);
                sleep(IP_BLOCK_SLEEP_TIME);
            } else {
                log.error("爬取HTML内容时发生错误: {}, URL: {}", e.getMessage(), htmlData.getUrl());
                updateHtmlDataStatus(htmlData, ERROR_STATUS);
            }
        }
    }

    /**
     * 解析Boss HTML数据
     */
    public void parseBossHtmlData() {
        List<HtmlData> htmlDataList = htmlDataMapper.listByStatus(PENDING_PARSE_STATUS, 10);
        for (HtmlData htmlData : htmlDataList) {
            try {
                // 解析职位详情
                log.info("开始解析HTML内容: {}", htmlData.getUrl());
                JobDetailData jobDetailData;
                try {
                    jobDetailData = htmlParserService.parseJobDetail(htmlData.getHtmlContent());
                } catch (IOException e) {
                    if (isIpBlockException(e)) {
                        log.error("代理IP被限制访问，需要处理异常代理并重新爬取: {}, URL: {}", e.getMessage(), htmlData.getUrl());
                        updateHtmlDataStatus(htmlData, RETRY_STATUS);
                        sleep(IP_BLOCK_SLEEP_TIME);
                    } else {
                        log.error("解析HTML内容时发生错误: {}, URL: {}", e.getMessage(), htmlData.getUrl());
                        updateHtmlDataStatus(htmlData, ERROR_STATUS);
                    }
                    continue;
                }

                if (jobDetailData == null) {
                    continue;
                }

                // 将对象转换为JSON字符串
                String detailDataJson;
                try {
                    detailDataJson = objectMapper.writeValueAsString(jobDetailData);
                } catch (JsonProcessingException e) {
                    log.error("无法将对象转换为JSON: {}", e.getMessage());
                    detailDataJson = "{}";
                }

                // 保存职位详情
                JobDetailEntity entity = JobDetailEntity.builder()
                        .htmlUrl(htmlData.getUrl())
                        .positionId(htmlData.getBasePositionCode())
                        .cityId(htmlData.getBaseCityCode())
                        .positionName(htmlData.getBasePosition())
                        .cityName(htmlData.getBaseCity())
                        .detailData(detailDataJson)
                        .build();

                jobDetailEntityMapper.insert(entity);

                // 更新HTML数据状态
                htmlData.setStatus(SUCCESS_STATUS);
                htmlDataMapper.update(htmlData);
                log.info("成功处理HTML数据，URL: {}", htmlData.getUrl());

            } catch (DataIntegrityViolationException e) {
                log.error("数据完整性错误，无法保存职位详情: {}", e.getMessage());
                updateHtmlDataStatus(htmlData, ERROR_STATUS);
            } catch (Exception e) {
                if (isIpBlockException(e)) {
                    log.error("代理IP被限制访问，需要处理异常代理并重新爬取: {}, URL: {}", e.getMessage(), htmlData.getUrl());
                    updateHtmlDataStatus(htmlData, RETRY_STATUS);
                    sleep(IP_BLOCK_SLEEP_TIME);
                } else {
                    log.error("处理HTML数据时发生错误: {}, URL: {}", e.getMessage(), htmlData.getUrl());
                    updateHtmlDataStatus(htmlData, ERROR_STATUS);
                }
            }
        }
    }

    /**
     * 更新HTML数据状态
     */
    private void updateHtmlDataStatus(HtmlData htmlData, int status) {
        try {
            htmlData.setStatus(status);
            htmlDataMapper.update(htmlData);
        } catch (Exception e) {
            log.error("更新HTML数据状态失败: {}", e.getMessage());
        }
    }

    /**
     * 判断是否为IP访问受限异常
     */
    private boolean isIpBlockException(Exception e) {
        return e instanceof IpBlockedException ||
                (e.getMessage() != null && (e.getMessage().contains("IP访问受限") ||
                        e.getMessage().contains("IP被封锁")));
    }

    /**
     * 随机休眠一段时间，避免被反爬
     */
    private void randomSleep() {
        sleep(NORMAL_SLEEP_MIN + (long) (random.nextDouble() * (NORMAL_SLEEP_MAX - NORMAL_SLEEP_MIN)));
    }

    /**
     * 休眠指定时间
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
