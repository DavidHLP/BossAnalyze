package com.david.hlp.Spring.crawler.boss.service.boss_2025;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.david.hlp.Spring.crawler.boss.mapper.CityDataMapper;
import com.david.hlp.Spring.crawler.boss.mapper.PositionDataMapper;
import java.util.Random;
import com.david.hlp.Spring.crawler.boss.entity.CityData;
import com.david.hlp.Spring.crawler.boss.entity.PositionData;
import java.util.List;
import java.util.Map;
import com.david.hlp.Spring.crawler.boss.mapper.JobListMapper;
import org.springframework.transaction.annotation.Transactional;
import com.david.hlp.Spring.crawler.boss.model.JobList;
import com.david.hlp.Spring.crawler.boss.exception.IpBlockedException;
import com.david.hlp.Spring.crawler.proxy.model.entity.ProxyInfo;
import com.david.hlp.Spring.crawler.proxy.service.http.ProxyToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import jakarta.annotation.PostConstruct;
import com.david.hlp.Spring.crawler.boss.entity.HTMLData;
import com.david.hlp.Spring.crawler.boss.mapper.HtmlDataMapper;

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
    @Qualifier("HttpProxyToolService")
    private final ProxyToolService proxyToolService;

    @PostConstruct
    public void init() {
        log.info("开始初始化数据爬取服务");
    }

    @Transactional
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
                        processJobList(bossIdList, jobListJson , randomCity.getCode().toString(), randomPosition.getCode().toString());
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
    private void processJobList(List<String> bossIdList, List<String> jobListJson , String cityCode, String positionCode) {
        for (int j = 0; j < bossIdList.size(); j++) {
            try {
                saveJobList(bossIdList.get(j), jobListJson.get(j));
                saveHtmlUrl(bossIdList.get(j), cityCode, positionCode);
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
        jobListMapper.insert(jobList);
    }

    private void saveHtmlUrl(String bossId, String cityCode, String positionCode) {
        HTMLData htmlUrl = HTMLData.builder()
            .url(bossId)
            .baseCity(cityCode)
            .basePosition(positionCode)
            .build();
        htmlDataMapper.insert(htmlUrl);
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

        // 处理异常代理
        handleBlockedProxy();

        // 延长休眠时间
        sleep(1000 * 60 * 10); // 休眠10分钟
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
