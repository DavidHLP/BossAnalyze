package com.david.hlp.crawler.boss.service.boss_2024;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.david.hlp.crawler.boss.entity.CityData;
import com.david.hlp.crawler.boss.entity.HtmlData;
import com.david.hlp.crawler.boss.entity.JobDetailEntity;
import com.david.hlp.crawler.boss.entity.PositionData;
import com.david.hlp.crawler.boss.mapper.CityDataMapper;
import com.david.hlp.crawler.boss.mapper.HtmlDataMapper;
import com.david.hlp.crawler.boss.mapper.JobDetailEntityMapper;
import com.david.hlp.crawler.boss.mapper.PositionDataMapper;
import com.david.hlp.crawler.boss.model.JobDetailData;
import com.david.hlp.crawler.common.threadpool.ScrapeBossDataThreadPool;
import com.david.hlp.crawler.boss.exception.IpBlockedException;
import java.util.List;
import java.util.Random;
import java.io.IOException;
import org.springframework.dao.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.annotation.PostConstruct;

@Slf4j
@Component("boss_2024")
@RequiredArgsConstructor
public class DataScrapingService {
    private final CityDataMapper cityDataMapper;
    private final PositionDataMapper positionDataMapper;
    private final HtmlDataMapper htmlDataMapper;
    private final HtmlParserService htmlParserService;
    private final WebCrawlerService webCrawlerService;
    private final JobDetailEntityMapper jobDetailEntityMapper;
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
     */
    public void scrapeBossUrl() {
        doScrapeBossUrl();
    }

    /**
     * 执行Boss网站URL爬取逻辑
     */
    private void doScrapeBossUrl() {
        List<CityData> cityDataList = cityDataMapper.listAll();
        List<PositionData> positionDataList = positionDataMapper.listAll();
        if (cityDataList.isEmpty() || positionDataList.isEmpty()) {
            return;
        }

        CityData randomCity = getRandomCity(cityDataList);
        PositionData randomPosition = getRandomPosition(positionDataList);

        processPositionPages(randomCity, randomPosition);
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
     * 处理多个职位页面
     *
     * @param city 城市数据
     * @param position 职位数据
     */
    private void processPositionPages(CityData city, PositionData position) {
        for (int i = 0; i < 10; i++) {
            log.info("开始爬取Boss网站URL: {}", city.getName() + " " + position.getName());
            List<HtmlData> htmlDataList = fetchPositionPageUrls(city, position, i);
            processHtmlDataList(htmlDataList);
            randomSleep();
        }
    }

    /**
     * 获取职位页面URL列表
     *
     * @param city 城市数据
     * @param position 职位数据
     * @param pageIndex 页码索引
     * @return HTML数据列表
     */
    private List<HtmlData> fetchPositionPageUrls(CityData city, PositionData position, int pageIndex) {
        return webCrawlerService.getUrlWithSelenium(
            "position",
            city.getCode().toString(),
            position.getCode().toString(),
            city.getName(),
            position.getName(),
            pageIndex
        );
    }

    /**
     * 处理HTML数据列表
     *
     * @param htmlDataList HTML数据列表
     */
    private void processHtmlDataList(List<HtmlData> htmlDataList) {
        for (HtmlData htmlData : htmlDataList) {
            saveHtmlData(htmlData);
        }
    }

    /**
     * 保存HTML数据
     *
     * @param htmlData HTML数据
     */
    private void saveHtmlData(HtmlData htmlData) {
        try {
            // 先检查URL是否已存在
            HtmlData existingData = htmlDataMapper.getByUrl(htmlData.getUrl());
            if (existingData != null) {
                log.info("URL已存在，跳过插入: {}", htmlData.getUrl());
                return;
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

    /**
     * 随机休眠一段时间，避免被反爬
     */
    private void randomSleep() {
        sleep((long)(random.nextDouble() * 50000 + 10000));
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
            handleException(e, htmlData, "爬取Boss网站数据");
        }
    }
    /**
     * 处理异常情况
     *
     * @param e 捕获的异常
     * @param htmlData HTML数据对象
     * @param operation 当前执行的操作描述
     */
    private void handleException(Exception e, HtmlData htmlData, String operation) {
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
    private void handleIpBlockException(Exception e, HtmlData htmlData) {
        log.error("代理IP被限制访问，需要处理异常代理并重新爬取: {}, URL: {}", e.getMessage(), htmlData.getUrl());

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
    private void handleGenericException(Exception e, HtmlData htmlData, String operation) {
        log.error("{}时发生错误: {}", operation, e.getMessage());
        // 始终记录完整堆栈，确保即使e.getMessage()为null也能显示异常信息
        log.error("{}时发生错误详情:", operation, e);

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
            handleGenericException(e, htmlData, "处理HTML数据");
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
            handleException(e, htmlData, "解析HTML内容");
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
}
