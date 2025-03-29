package com.david.hlp.Spring.boss.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.david.hlp.Spring.boss.entity.CityData;
import com.david.hlp.Spring.boss.entity.PositionData;
import com.david.hlp.Spring.boss.mapper.CityDataMapper;
import com.david.hlp.Spring.boss.mapper.HtmlDataMapper;
import com.david.hlp.Spring.boss.mapper.PositionDataMapper;
import com.david.hlp.Spring.boss.entity.HTMLData;
import lombok.RequiredArgsConstructor;
import com.david.hlp.Spring.boss.model.JobDetailData;
import com.david.hlp.Spring.boss.entity.JobDetailEntity;
import com.david.hlp.Spring.boss.mapper.JobDetailEntityMapper;
import com.david.hlp.Spring.common.threadpool.ScrapeBossUrlThreadPool;
import com.david.hlp.Spring.common.threadpool.ScrapeBossDataThreadPool;
import com.david.hlp.Spring.common.threadpool.ParseBossHtmlDataThreadPool;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.List;
import java.util.Random;
import java.io.IOException;
import org.springframework.dao.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    @Scheduled(fixedDelay = 1000 * 60 * 1)
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
                sleep((long)(random.nextDouble() * 240000 + 60000));
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
                    sleep((long)(random.nextDouble() * 240000 + 60000));
                } catch (Exception e) {
                    log.error("爬取Boss网站数据时发生错误: {}", e.getMessage());
                }
            }
        });
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
                        log.error("解析HTML内容失败: {}, URL: {}", e.getMessage(), htmlData.getUrl());
                        htmlData.setStatus(4); // 标记为处理失败
                        htmlDataMapper.update(htmlData);
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
                    htmlData.setStatus(2);
                    htmlDataMapper.update(htmlData);
                    log.info("成功处理HTML数据，URL: {}", htmlData.getUrl());
                } catch (DataIntegrityViolationException e) {
                    log.error("数据完整性错误，无法保存职位详情: {}", e.getMessage());
                    // 将此记录标记为处理失败
                    htmlData.setStatus(4);
                    htmlDataMapper.update(htmlData);
                } catch (Exception e) {
                    log.error("处理HTML数据时发生错误: {}", e.getMessage());
                    if (log.isDebugEnabled()) {
                        log.debug("异常详情:", e);
                    }
                    // 将此记录标记为处理失败
                    htmlData.setStatus(4);
                    htmlDataMapper.update(htmlData);
                }
            }
        });
    }
}
