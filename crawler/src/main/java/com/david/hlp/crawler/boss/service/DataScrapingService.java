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
import org.springframework.transaction.annotation.Transactional;
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
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@Component("boss_2025")
@RequiredArgsConstructor
public class DataScrapingService {
    private final CityDataMapper cityDataMapper;
    private final PositionDataMapper positionDataMapper;
    private final Random random = new Random();
    private final JsonScrapingService jsonScrapingService;
    private final JobListMapper jobListMapper;
    private final HtmlDataMapper htmlDataMapper;
    private final HtmlParserService htmlParserService;
    private final WebCrawlerService webCrawlerService;
    private final JobDetailEntityMapper jobDetailEntityMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        log.info("开始初始化数据爬取服务");
    }

    public void scrapeBossUrl() {
        try {
            List<CityData> cityDataList = cityDataMapper.listAll();
            List<PositionData> positionDataList = positionDataMapper.listAll();
            if (cityDataList.isEmpty() || positionDataList.isEmpty()) {
                return;
            }

            CityData randomCity = getRandomCity(cityDataList);
            PositionData randomPosition = getRandomPosition(positionDataList);
            try {
                for (int i = 1; i <= 10; i++) {
                        Map<String, List<String>> result = jsonScrapingService.scrapeJobListJson(i, randomCity.getCode().toString(), randomPosition.getCode().toString());
                        List<String> bossIdList = result.get("urls");
                        List<String> jobListJson = result.get("json");
                        processJobList(bossIdList, jobListJson , randomCity.getCode().toString(), randomPosition.getCode().toString() , randomCity.getName() , randomPosition.getName());
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
     * 处理职位列表数据
     *
     * @param bossIdList Boss ID列表
     * @param jobListJson 职位数据JSON列表
     */
    @Transactional
    private void processJobList(List<String> bossIdList, List<String> jobListJson , String cityCode, String positionCode , String cityName , String positionName) {
        for (int j = 0; j < bossIdList.size(); j++) {
            try {
                saveJobList(bossIdList.get(j), jobListJson.get(j));
                saveHtmlUrl(bossIdList.get(j), cityCode, positionCode , cityName , positionName);
            } catch (DuplicateKeyException e) {
                log.info("URL重复，跳过插入: {}", bossIdList.get(j));
            } catch (Exception e) {
                log.error("插入职位列表数据时发生错误，URL: {}，错误: {}", bossIdList.get(j), e.getMessage());
            }
        }
    }

    /**
     * 保存职位列表数据
     *
     * @param htmlUrl HTML URL
     * @param jsonData JSON数据
     */
    private void saveJobList(String htmlUrl, String jsonData) {
        JobList jobList = JobList.builder()
            .htmlUrl(htmlUrl)
            .jsonData(jsonData)
            .build();
        if (jobListMapper.selectByUrl(htmlUrl) == null) {
            jobListMapper.insert(jobList);
        }else{
            log.info("URL已存在，跳过插入: {}", htmlUrl);
        }
    }

    private void saveHtmlUrl(String bossId, String cityCode, String positionCode , String cityName , String positionName) {
        HtmlData htmlUrl = HtmlData.builder()
            .url(bossId)
            .baseCityCode(cityCode)
            .basePositionCode(positionCode)
            .baseCity(cityName)
            .basePosition(positionName)
            .build();
        if (htmlDataMapper.getByUrl(bossId) == null) {
            htmlDataMapper.insert(htmlUrl);
        }else{
            log.info("URL已存在，跳过插入: {}", bossId);
        }
    }

    /**
     * 获取随机城市
     *
     * @param cityDataList 城市数据列表
     * @return 随机选择的城市
     */
    private CityData getRandomCity(List<CityData> cityDataList) {
        return cityDataList.get(random.nextInt(cityDataList.size()));
    }

    /**
     * 获取随机职位
     *
     * @param positionDataList 职位数据列表
     * @return 随机选择的职位
     */
    private PositionData getRandomPosition(List<PositionData> positionDataList) {
        return positionDataList.get(random.nextInt(positionDataList.size()));
    }

    /**
     * 随机休眠一段时间，避免被反爬
     */
    private void randomSleep() {
        sleep((long)(random.nextDouble() * 50000 + 10000));
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
     * 使用线程池爬取Boss网站的数据
     */
    public void scrapeBossData() {
        ScrapeBossDataThreadPool.executeWithSpringContext(() -> {
            doScrapeBossData();
        });
    }

    /**
     * 执行Boss网站数据爬取逻辑
     */
    private void doScrapeBossData() {
        List<HtmlData> htmlDataList = htmlDataMapper.listByStatus(0, 10);
        for (HtmlData htmlData : htmlDataList) {
            processHtmlData(htmlData);
        }
        List<HtmlData> errorHtmlDataList = htmlDataMapper.listByStatus(3, 10);
        for (HtmlData htmlData : errorHtmlDataList) {
            processHtmlData(htmlData);
        }
        List<HtmlData> successHtmlDataList = htmlDataMapper.listByStatus(4, 10);
        for (HtmlData htmlData : successHtmlDataList) {
            processHtmlData(htmlData);
        }
    }

    /**
     * 处理单个HTML数据
     *
     * @param htmlData HTML数据
     */
    private void processHtmlData(HtmlData htmlData) {
        try {
            log.info("开始爬取Boss网站数据: {}", htmlData.getUrl());
            htmlData = webCrawlerService.getHtmlContent(htmlData);
            htmlDataMapper.update(htmlData);
            randomSleep();
        } catch (Exception e) {
            handleHtmlDataException(e, htmlData, "爬取Boss网站数据");
        }
    }

    /**
     * 使用线程池解析Boss HTML数据
     */
    public void parseBossHtmlData() {
        doParseBossHtmlData();
    }

    /**
     * 执行Boss HTML数据解析逻辑
     */
    private void doParseBossHtmlData() {
        List<HtmlData> htmlDataList = htmlDataMapper.listByStatus(1, 10);
        for (HtmlData htmlData : htmlDataList) {
            processAndSaveJobDetail(htmlData);
        }
    }

    /**
     * 处理并保存职位详情
     *
     * @param htmlData HTML数据
     */
    private void processAndSaveJobDetail(HtmlData htmlData) {
        try {
            JobDetailData jobDetailData = parseJobDetail(htmlData);
            if (jobDetailData == null) {
                return;
            }
            String detailDataJson = convertToJson(jobDetailData);
            JobDetailEntity jobDetailEntity = createJobDetailEntity(htmlData, detailDataJson);
            saveJobDetailAndUpdateStatus(jobDetailEntity, htmlData);
        } catch (DataIntegrityViolationException e) {
            log.error("数据完整性错误，无法保存职位详情: {}", e.getMessage());
            updateHtmlDataStatus(htmlData, 4);
        } catch (Exception e) {
            handleHtmlDataException(e, htmlData, "处理HTML数据");
        }
    }

    /**
     * 解析职位详情
     *
     * @param htmlData HTML数据
     * @return 职位详情数据，解析失败则返回null
     */
    private JobDetailData parseJobDetail(HtmlData htmlData) {
        try {
            log.info("开始解析HTML内容: {}", htmlData.getUrl());
            return htmlParserService.parseJobDetail(htmlData.getHtmlContent());
        } catch (IOException e) {
            handleHtmlDataException(e, htmlData, "解析HTML内容");
            return null;
        }
    }

    /**
     * 将职位详情数据转换为JSON字符串
     *
     * @param jobDetailData 职位详情数据
     * @return JSON字符串
     */
    private String convertToJson(JobDetailData jobDetailData) {
        try {
            return objectMapper.writeValueAsString(jobDetailData);
        } catch (Exception e) {
            log.error("无法将JobDetailData转换为JSON: {}", e.getMessage());
            // 设置为有效的JSON对象，避免插入失败
            return "{}";
        }
    }

    /**
     * 创建职位详情实体
     *
     * @param htmlData HTML数据
     * @param detailDataJson 详情数据JSON
     * @return 职位详情实体
     */
    private JobDetailEntity createJobDetailEntity(HtmlData htmlData, String detailDataJson) {
        return JobDetailEntity.builder()
            .positionId(htmlData.getBasePositionCode())
            .cityId(htmlData.getBaseCityCode())
            .positionName(htmlData.getBasePosition())
            .cityName(htmlData.getBaseCity())
            .detailData(detailDataJson)
            .build();
    }

    /**
     * 保存职位详情并更新HTML数据状态
     *
     * @param jobDetailEntity 职位详情实体
     * @param htmlData HTML数据
     */
    private void saveJobDetailAndUpdateStatus(JobDetailEntity jobDetailEntity, HtmlData htmlData) {
        jobDetailEntity.setHtmlUrl(htmlData.getUrl());
        jobDetailEntityMapper.insert(jobDetailEntity);
        updateHtmlDataStatus(htmlData, 2);
        log.info("成功处理HTML数据，URL: {}", htmlData.getUrl());
    }

    /**
     * 处理异常情况
     *
     * @param e 捕获的异常
     * @param operation 当前执行的操作描述
     */
    private void handleException(Exception e, String operation) {
        if (isIpBlockException(e)) {
            handleIpBlockException(e);
        } else {
            handleGenericException(e, operation);
        }
    }

    /**
     * 处理HTML数据相关的异常情况
     *
     * @param e 捕获的异常
     * @param htmlData HTML数据对象
     * @param operation 当前执行的操作描述
     */
    private void handleHtmlDataException(Exception e, HtmlData htmlData, String operation) {
        if (isIpBlockException(e)) {
            handleIpBlockExceptionWithHtmlData(e, htmlData);
        } else {
            handleGenericExceptionWithHtmlData(e, htmlData, operation);
        }
    }

    /**
     * 处理带有HTML数据的IP访问受限异常
     *
     * @param e 捕获的异常
     * @param htmlData HTML数据对象
     */
    private void handleIpBlockExceptionWithHtmlData(Exception e, HtmlData htmlData) {
        log.error("代理IP被限制访问，需要处理异常代理并重新爬取: {}, URL: {}", e.getMessage(), htmlData.getUrl());

        // 将任务标记为需要重新爬取
        updateHtmlDataStatus(htmlData, 0);

        // 延长休眠时间
        sleep(1000 * 60 * 10); // 休眠10分钟
    }

    /**
     * 处理带有HTML数据的一般性异常
     *
     * @param e 捕获的异常
     * @param htmlData HTML数据对象
     * @param operation 当前执行的操作描述
     */
    private void handleGenericExceptionWithHtmlData(Exception e, HtmlData htmlData, String operation) {
        log.error("{}时发生错误: {}", operation, e.getMessage());
        // 记录异常详情
        if (log.isDebugEnabled()) {
            log.debug("{}时发生错误详情:", operation, e);
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
     */
    private void handleIpBlockException(Exception e) {
        log.error("代理IP被限制访问，需要处理异常代理并重新爬取: {}", e.getMessage());

        // 延长休眠时间
        sleep(1000 * 60 * 10); // 休眠10分钟
    }

    /**
     * 处理一般性异常
     *
     * @param e 捕获的异常
     * @param operation 当前执行的操作描述
     */
    private void handleGenericException(Exception e, String operation) {
        log.error("{}时发生错误: {}", operation, e.getMessage());

        if (log.isDebugEnabled()) {
            log.debug("异常详情:", e);
        }
    }
}
