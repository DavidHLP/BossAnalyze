package com.david.hlp.crawler.boss.service;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.david.hlp.crawler.boss.mapper.CityDataMapper;
import com.david.hlp.crawler.boss.mapper.PositionDataMapper;
import java.util.Random;
import com.david.hlp.crawler.boss.entity.CityData;
import com.david.hlp.crawler.boss.entity.PositionData;
import java.util.List;
import java.util.Map;
import com.david.hlp.crawler.boss.mapper.JobListMapper;
import com.david.hlp.crawler.boss.model.JobList;
import com.david.hlp.crawler.boss.exception.IpBlockedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import jakarta.annotation.PostConstruct;
import com.david.hlp.crawler.boss.entity.HtmlData;
import com.david.hlp.crawler.boss.mapper.HtmlDataMapper;
import com.david.hlp.crawler.common.threadpool.ScrapeBossDataThreadPool;
import com.david.hlp.crawler.boss.entity.JobDetailEntity;
import com.david.hlp.crawler.boss.mapper.JobDetailEntityMapper;
import com.david.hlp.crawler.boss.model.JobDetailData;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;

/**
 * Boss招聘网站数据爬取服务
 */
@Slf4j
@Component("boss_2025")
@RequiredArgsConstructor
public class DataScrapingService {
    // 常量定义
    private static final long NORMAL_SLEEP_MIN = 10000;
    private static final long NORMAL_SLEEP_MAX = 60000;
    private static final long IP_BLOCK_SLEEP_TIME = 600000; // 10分钟
    private static final int MAX_PAGE_COUNT = 10;
    private static final int SUCCESS_STATUS = 2;
    private static final int ERROR_STATUS = 4;
    private static final int RETRY_STATUS = 0;
    private static final int PENDING_PARSE_STATUS = 1;
    
    // 目标职位集合
    private static final Set<String> TARGET_POSITIONS = new HashSet<>(Arrays.asList(
        "Java开发", "Python开发", "C++开发", "前端开发", "后端开发", "全栈开发",
        "算法工程师", "数据分析师", "数据工程师", "DevOps工程师", "运维工程师", 
        "测试工程师", "软件工程师", "系统架构师", "数据库工程师", "人工智能",
        "机器学习", "深度学习", "区块链开发", "云计算工程师", "网络工程师",
        "安全工程师", "大数据开发", "iOS开发", "Android开发", "移动端开发"
    ));

    // 依赖注入
    private final CityDataMapper cityDataMapper;
    private final PositionDataMapper positionDataMapper;
    private final JobListMapper jobListMapper;
    private final HtmlDataMapper htmlDataMapper;
    private final JobDetailEntityMapper jobDetailEntityMapper;
    
    // 服务依赖
    private final JsonScrapingService jsonScrapingService;
    private final HtmlParserService htmlParserService;
    private final WebCrawlerService webCrawlerService;
    
    // 工具类
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
            // 获取城市和职位数据
            List<CityData> cityDataList = cityDataMapper.listAll();
            List<PositionData> positionDataList = positionDataMapper.listAll();
            if (cityDataList.isEmpty() || positionDataList.isEmpty()) {
                log.warn("城市或职位数据为空，无法执行爬取");
                return;
            }

            // 过滤出目标职位
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
            
            try {
                // 爬取每个页面的数据
                for (int page = 1; page <= MAX_PAGE_COUNT; page++) {
                    scrapeJobPage(page, randomCity, randomPosition, cityCode, positionCode);
                    randomSleep();
                }
            } catch (Exception e) {
                handleException(e, "爬取Boss网站URL");
            }
        } catch (Exception e) {
            log.error("执行Boss网站URL爬取逻辑时发生错误: {}", e.getMessage());
        }
    }

    /**
     * 爬取单个职位页面数据
     */
    private void scrapeJobPage(int page, CityData city, PositionData position, String cityCode, String positionCode) {
        try {
            Map<String, List<String>> result = jsonScrapingService.scrapeJobListJson(
                page, cityCode, positionCode);
            
            List<String> jobUrls = result.get("urls");
            List<String> jobJsons = result.get("json");
            
            for (int i = 0; i < jobUrls.size(); i++) {
                try {
                    String url = jobUrls.get(i);
                    String json = jobJsons.get(i);
                    
                    // 保存职位列表数据
                    saveJobData(url, json, city, position, cityCode, positionCode);
                } catch (DuplicateKeyException e) {
                    log.info("URL重复，跳过插入: {}", jobUrls.get(i));
                } catch (Exception e) {
                    log.error("插入职位列表数据时发生错误，URL: {}，错误: {}", jobUrls.get(i), e.getMessage());
                }
            }
        } catch (JsonProcessingException e) {
            log.error("解析职位列表JSON时发生错误: {}，城市：{}，职位：{}", e.getMessage(), cityCode, positionCode);
        }
    }
    
    /**
     * 保存职位数据和对应的HTML URL
     */
    private void saveJobData(String url, String json, CityData city, PositionData position, 
                            String cityCode, String positionCode) {
        // 保存职位列表数据
        if (jobListMapper.selectByUrl(url) == null) {
            JobList jobList = JobList.builder()
                .htmlUrl(url)
                .jsonData(json)
                .build();
            jobListMapper.insert(jobList);
        } else {
            log.info("职位列表URL已存在，跳过插入: {}", url);
        }
        
        // 保存HTML URL
        if (htmlDataMapper.getByUrl(url) == null) {
            HtmlData htmlData = HtmlData.builder()
                .url(url)
                .baseCityCode(cityCode)
                .basePositionCode(positionCode)
                .baseCity(city.getName())
                .basePosition(position.getName())
                .build();
            htmlDataMapper.insert(htmlData);
        } else {
            log.info("HTML URL已存在，跳过插入: {}", url);
        }
    }

    /**
     * 随机休眠一段时间，避免被反爬
     */
    private void randomSleep() {
        sleep(NORMAL_SLEEP_MIN + (long)(random.nextDouble() * (NORMAL_SLEEP_MAX - NORMAL_SLEEP_MIN)));
    }

    /**
     * 休眠指定时间
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
        processHtmlByStatus(RETRY_STATUS, 10); // 待处理
        processHtmlByStatus(3, 10); // 错误重试
        processHtmlByStatus(ERROR_STATUS, 10); // 成功但需更新
    }
    
    /**
     * 处理指定状态的HTML数据
     */
    private void processHtmlByStatus(int status, int limit) {
        List<HtmlData> htmlDataList = htmlDataMapper.listByStatus(status, limit);
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
            handleHtmlDataException(e, htmlData, "爬取HTML内容");
        }
    }

    /**
     * 解析Boss HTML数据
     */
    public void parseBossHtmlData() {
        List<HtmlData> htmlDataList = htmlDataMapper.listByStatus(PENDING_PARSE_STATUS, 10);
        for (HtmlData htmlData : htmlDataList) {
            processAndSaveJobDetail(htmlData);
        }
    }

    /**
     * 处理并保存职位详情
     */
    private void processAndSaveJobDetail(HtmlData htmlData) {
        try {
            // 解析职位详情
            log.info("开始解析HTML内容: {}", htmlData.getUrl());
            JobDetailData jobDetailData = parseHtmlContent(htmlData);
            if (jobDetailData == null) {
                return;
            }
            
            // 准备并保存职位详情
            String detailDataJson;
            try {
                detailDataJson = objectMapper.writeValueAsString(jobDetailData);
            } catch (JsonProcessingException e) {
                log.error("无法将职位详情数据转换为JSON: {}", e.getMessage());
                detailDataJson = "{}"; // 返回空JSON对象，避免插入失败
            }
            
            saveJobDetailEntity(htmlData, detailDataJson);
            
        } catch (DataIntegrityViolationException e) {
            log.error("数据完整性错误，无法保存职位详情: {}", e.getMessage());
            updateHtmlDataStatus(htmlData, ERROR_STATUS);
        } catch (Exception e) {
            handleHtmlDataException(e, htmlData, "处理HTML数据");
        }
    }
    
    /**
     * 解析HTML内容为职位详情数据
     */
    private JobDetailData parseHtmlContent(HtmlData htmlData) {
        try {
            return htmlParserService.parseJobDetail(htmlData.getHtmlContent());
        } catch (IOException e) {
            handleHtmlDataException(e, htmlData, "解析HTML内容");
            return null;
        }
    }
    
    /**
     * 保存职位详情实体
     */
    private void saveJobDetailEntity(HtmlData htmlData, String detailDataJson) {
        JobDetailEntity entity = JobDetailEntity.builder()
            .htmlUrl(htmlData.getUrl())
            .positionId(htmlData.getBasePositionCode())
            .cityId(htmlData.getBaseCityCode())
            .positionName(htmlData.getBasePosition())
            .cityName(htmlData.getBaseCity())
            .detailData(detailDataJson)
            .build();
            
        jobDetailEntityMapper.insert(entity);
        updateHtmlDataStatus(htmlData, SUCCESS_STATUS);
        log.info("成功处理HTML数据，URL: {}", htmlData.getUrl());
    }

    /**
     * 处理异常情况
     */
    private void handleException(Exception e, String operation) {
        if (isIpBlockException(e)) {
            log.error("代理IP被限制访问，需要处理异常代理并重新爬取: {}", e.getMessage());
            sleep(IP_BLOCK_SLEEP_TIME);
        } else {
            log.error("{}时发生错误: {}", operation, e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("异常详情:", e);
            }
        }
    }

    /**
     * 处理HTML数据相关的异常情况
     */
    private void handleHtmlDataException(Exception e, HtmlData htmlData, String operation) {
        if (isIpBlockException(e)) {
            log.error("代理IP被限制访问，需要处理异常代理并重新爬取: {}, URL: {}", 
                    e.getMessage(), htmlData.getUrl());
            updateHtmlDataStatus(htmlData, RETRY_STATUS);
            sleep(IP_BLOCK_SLEEP_TIME);
        } else {
            log.error("{}时发生错误: {}, URL: {}", operation, e.getMessage(), htmlData.getUrl());
            if (log.isDebugEnabled()) {
                log.debug("{}时发生错误详情:", operation, e);
            }
            updateHtmlDataStatus(htmlData, ERROR_STATUS);
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
}
