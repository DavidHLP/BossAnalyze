package com.david.hlp.Spring.crawler.proxy.service.https;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;

import com.david.hlp.Spring.crawler.proxy.model.HttpsProxy;
import com.david.hlp.Spring.crawler.proxy.model.dto.ProxyConstants;
import com.david.hlp.Spring.crawler.proxy.model.dto.ProxyConvert;
import com.david.hlp.Spring.crawler.proxy.model.entity.ProxyInfo;
import com.david.hlp.Spring.common.util.RedisCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * HTTPS代理池服务
 * 负责爬取、解析、验证和管理HTTPS代理
 *
 * @author david
 */
@Slf4j
@Service("HttpsProxyPoolService")
@RequiredArgsConstructor
public class ProxyPoolService {

    @Qualifier("proxyPoolChromeOptions")
    private final ChromeOptions proxyPoolChromeOptions;

    private final RedisCache redisCache;

    /**
     * 从HTML内容中解析代理表格
     *
     * @param htmlContent HTML内容
     * @return HTTPS代理列表
     */
    public List<HttpsProxy> parseProxyTable(String htmlContent) {
        List<HttpsProxy> httpsProxies = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(htmlContent);
            Element table = doc.selectFirst("table#proxyTable");

            if (table != null) {
                Elements rows = table.select("tbody tr");
                httpsProxies = extractProxyInfoFromRows(rows);
            }
        } catch (Exception e) {
            log.error("解析HTML内容时出错: {}", e.getMessage());
        }
        return httpsProxies;
    }

    /**
     * 从表格行中提取代理信息
     *
     * @param rows 表格行元素
     * @return HTTPS代理列表
     */
    private List<HttpsProxy> extractProxyInfoFromRows(Elements rows) {
        List<HttpsProxy> httpsProxies = new ArrayList<>();
        for (Element row : rows) {
            Elements cells = row.select("th");

            if (cells.size() >= 7) {
                String ip = cells.get(0).text();
                String port = cells.get(1).text();
                String anonymity = cells.get(2).text();
                String proxyType = cells.get(3).text();
                String location = cells.get(4).text();
                String responseTime = cells.get(5).text();
                String recordTime = cells.get(6).text();

                if (ProxyConstants.PROXY_TYPE_HTTPS.equalsIgnoreCase(proxyType)) {
                    HttpsProxy proxy = createHttpsProxy(ip, port, anonymity, proxyType, location, responseTime, recordTime);
                    httpsProxies.add(proxy);
                }
            }
        }
        return httpsProxies;
    }

    /**
     * 创建HTTPS代理对象
     */
    private HttpsProxy createHttpsProxy(String ip, String port, String anonymity, String proxyType,
                                       String location, String responseTime, String recordTime) {
        HttpsProxy proxy = new HttpsProxy();
        proxy.setIp(ip);
        proxy.setPort(port);
        proxy.setAnonymity(anonymity);
        proxy.setProxyType(proxyType);
        proxy.setLocation(location);
        proxy.setResponseTime(responseTime);
        proxy.setRecordTime(recordTime);
        return proxy;
    }

    /**
     * 从HTML文件中解析代理信息
     *
     * @param htmlFile HTML文件路径
     * @return HTTPS代理列表
     */
    public List<HttpsProxy> parseHtmlFile(String htmlFile) {
        try {
            File input = new File(htmlFile);
            Document doc = Jsoup.parse(input, "UTF-8");
            return parseProxyTable(doc.html());
        } catch (IOException e) {
            log.error("读取HTML文件时出错: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 爬取代理网站的代理数据
     *
     * @param url 代理网站URL
     * @return HTTPS代理列表
     */
    public List<HttpsProxy> scrapeProxies(String url) {
        WebDriver driver = null;
        
        try {
            driver = createWebDriver();
            driver.get(url);
            
            // 等待表格加载
            waitForTableLoad(driver);
            
            // 解析HTML内容
            return parseProxyTable(driver.getPageSource());
            
        } catch (Exception e) {
            log.error("抓取过程中发生错误: {}", e.getMessage());
            return new ArrayList<>();
        } finally {
            closeWebDriver(driver);
        }
    }
    
    /**
     * 创建WebDriver实例
     */
    private WebDriver createWebDriver() {
        return new ChromeDriver(proxyPoolChromeOptions);
    }
    
    /**
     * 等待表格元素加载完成
     */
    private void waitForTableLoad(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(ProxyConstants.PAGE_LOAD_WAIT));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("table")));
    }
    
    /**
     * 关闭WebDriver
     */
    private void closeWebDriver(WebDriver driver) {
        if (driver != null) {
            driver.quit();
        }
    }
    
    /**
     * 获取代理数据
     *
     * @return HTTPS代理列表
     */
    public List<HttpsProxy> listProxies() {
        List<HttpsProxy> allProxies = new ArrayList<>();
        List<ProxyInfo> allValidProxies = new ArrayList<>();
        
        for (int i = 1; i <= 10; i++) {
            String url = buildProxyPageUrl(i);
            log.info("正在爬取第{}页代理数据", i);
            
            List<HttpsProxy> pageProxies = scrapeProxies(url);
            
            if (pageProxies.isEmpty()) {
                log.warn("第{}页未能获取任何HTTPS代理数据", i);
                // 如果连续两页没有数据，则停止爬取
                if (shouldStopScraping(i, allProxies)) {
                    break;
                }
            } else {
                processPageProxies(i, pageProxies, allProxies, allValidProxies);
            }
            
            sleepBetweenRequests(1000);
        }
        
        logProxyScrapingResults(allProxies, allValidProxies);
        return allProxies;
    }
    
    /**
     * 构建代理页面URL
     */
    private String buildProxyPageUrl(int pageNumber) {
        return "https://www.qiyunip.com/freeProxy/" + pageNumber + ".html";
    }
    
    /**
     * 判断是否应该停止爬取
     */
    private boolean shouldStopScraping(int currentPage, List<HttpsProxy> allProxies) {
        return currentPage > 1 && allProxies.isEmpty();
    }
    
    /**
     * 处理当前页面的代理数据
     */
    private void processPageProxies(int pageNumber, List<HttpsProxy> pageProxies, 
                                   List<HttpsProxy> allProxies, List<ProxyInfo> allValidProxies) {
        log.info("第{}页获取到{}个HTTPS代理", pageNumber, pageProxies.size());
        allProxies.addAll(pageProxies);

        // 将当前页面的HttpsProxy转换为ProxyInfo
        List<ProxyInfo> pageProxyInfoList = ProxyConvert.toProxyInfoList(pageProxies);

        // 测试当前页面的代理
        log.info("开始验证第{}页的{}个代理", pageNumber, pageProxyInfoList.size());
        List<ProxyInfo> validProxies = validateProxies(pageProxyInfoList);

        // 处理有效代理
        processValidProxies(pageNumber, validProxies, allValidProxies);
    }

    /**
     * 处理有效代理
     */
    private void processValidProxies(int pageNumber, List<ProxyInfo> validProxies, List<ProxyInfo> allValidProxies) {
        if (!validProxies.isEmpty()) {
            log.info("第{}页有{}个有效代理", pageNumber, validProxies.size());
            allValidProxies.addAll(validProxies);
            
            // 将有效代理保存到Redis
            saveProxiesToRedis(validProxies);
            log.info("成功将第{}页的{}个有效HTTPS代理保存到Redis", pageNumber, validProxies.size());
        } else {
            log.warn("第{}页未找到有效代理", pageNumber);
        }
    }
    
    /**
     * 请求之间的睡眠时间
     */
    private void sleepBetweenRequests(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("线程休眠被中断: {}", e.getMessage());
        }
    }
    
    /**
     * 记录代理爬取结果
     */
    private void logProxyScrapingResults(List<HttpsProxy> allProxies, List<ProxyInfo> allValidProxies) {
        if (allProxies.isEmpty()) {
            log.warn("未能获取任何HTTPS代理数据");
        } else {
            log.info("共获取到{}个HTTPS代理，其中{}个有效", allProxies.size(), allValidProxies.size());
        }
    }
    
    /**
     * 测试单个代理是否可用
     * 
     * @param proxyInfo 代理信息
     * @return 代理是否可用
     */
    public boolean validateRedisProxies(ProxyInfo proxyInfo) {
        if (!isValidProxyInfo(proxyInfo)) {
            log.warn("测试代理失败: 代理信息不完整");
            return false;
        }

        try {
            // 创建URL对象
            URL url = new URL(ProxyConstants.HTTPS_TEST_URL);
            
            // 创建代理对象
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyInfo.getIp(), Integer.parseInt(proxyInfo.getPort())));
            
            // 打开连接
            HttpURLConnection connection = (HttpURLConnection)url.openConnection(proxy);
            
            // 设置连接超时和读取超时
            connection.setConnectTimeout((int)TimeUnit.SECONDS.toMillis(ProxyConstants.CONNECTION_TIMEOUT));
            connection.setReadTimeout((int)TimeUnit.SECONDS.toMillis(ProxyConstants.CONNECTION_TIMEOUT));
            
            // 设置请求方法
            connection.setRequestMethod("GET");
            
            // 连接
            connection.connect();
            
            // 获取响应码
            int responseCode = connection.getResponseCode();
            
            // 判断代理是否可用
            boolean isValid = responseCode >= 200 && responseCode < 400;
            
            if (isValid) {
                proxyInfo.setStatus(ProxyConstants.PROXY_STATUS_VALID);
                log.info("代理可用: {}", proxyInfo.getIp() + ":" + proxyInfo.getPort());
            } else {
                proxyInfo.setStatus(ProxyConstants.PROXY_STATUS_INVALID);
                log.info("代理不可用: {}, 响应码: {}", proxyInfo.getIp() + ":" + proxyInfo.getPort(), responseCode);
            }
            
            return isValid;
        } catch (Exception e) {
            proxyInfo.setStatus(ProxyConstants.PROXY_STATUS_INVALID);
            log.info("测试代理时出错: {} - {}", proxyInfo.getIp() + ":" + proxyInfo.getPort(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查代理信息是否有效
     */
    private boolean isValidProxyInfo(ProxyInfo proxyInfo) {
        return proxyInfo != null && proxyInfo.getIp() != null && proxyInfo.getPort() != null;
    }
    
    /**
     * 批量测试代理是否可用
     * 
     * @param proxyList 代理列表
     * @return 可用代理列表
     */
    public List<ProxyInfo> validateProxies(List<ProxyInfo> proxyList) {
        if (isEmptyProxyList(proxyList)) {
            log.warn("测试代理失败: 代理列表为空");
            return new ArrayList<>();
        }
        
        List<ProxyInfo> validProxies = new ArrayList<>();
        
        for (ProxyInfo proxy : proxyList) {
            log.info("测试代理: {}", proxy.getIp() + ":" + proxy.getPort());
            
            if (validateRedisProxies(proxy)) {
                validProxies.add(proxy);
            }
            
            sleepBetweenRequests(500);
        }
        
        log.info("代理测试完成, 共测试{}个代理, 其中{}个可用", proxyList.size(), validProxies.size());
        return validProxies;
    }
    
    /**
     * 检查代理列表是否为空
     */
    private boolean isEmptyProxyList(List<ProxyInfo> proxyList) {
        return proxyList == null || proxyList.isEmpty();
    }
    
    /**
     * 保存代理到Redis
     *
     * @param proxyList 代理列表
     */
    private void saveProxiesToRedis(List<ProxyInfo> proxyList) {
        if (isEmptyProxyList(proxyList)) {
            log.warn("保存代理失败: 代理列表为空");
            return;
        }

        String redisKey = ProxyConstants.HTTPS_PROXY_POOL_KEY;

        // 获取并处理现有代理
        Map<String, Object> proxyMap = getExistingProxiesFromRedis(redisKey);

        // 添加新的代理
        addNewProxiesToMap(proxyList, proxyMap);

        // 保存到Redis并设置过期时间
        saveProxyMapToRedis(redisKey, proxyMap, proxyList.size());
    }
    
    /**
     * 从Redis获取现有代理
     */
    private Map<String, Object> getExistingProxiesFromRedis(String redisKey) {
        Map<String, ProxyInfo> existingProxies = redisCache.getCacheMap(redisKey);
        Map<String, Object> proxyMap = new HashMap<>();

        if (existingProxies != null && !existingProxies.isEmpty()) {
            proxyMap.putAll(existingProxies);
            log.info("Redis中已存在{}个代理，进行增量更新", existingProxies.size());
        } else {
            log.info("Redis中无现有代理，创建新缓存");
        }
        
        return proxyMap;
    }
    
    /**
     * 添加新代理到代理Map
     */
    private void addNewProxiesToMap(List<ProxyInfo> proxyList, Map<String, Object> proxyMap) {
        for (ProxyInfo proxy : proxyList) {
            String key = proxy.getIp() + ":" + proxy.getPort();
            // 确保代理类型为HTTPS
            proxy.setType(ProxyConstants.PROXY_TYPE_HTTPS);
            proxyMap.put(key, proxy);
        }
    }
    
    /**
     * 保存代理Map到Redis
     */
    private void saveProxyMapToRedis(String redisKey, Map<String, Object> proxyMap, int proxyCount) {
        // 保存到Redis的Hash结构中
        redisCache.setCacheMap(redisKey, proxyMap);

        // 设置过期时间
        redisCache.expire(redisKey, ProxyConstants.REDIS_EXPIRE_HOURS, TimeUnit.HOURS);

        log.info("成功将{}个HTTPS代理保存到Redis", proxyCount);
    }
    
    /**
     * 获取代理并测试后保存到Redis
     * 
     * @return 有效代理列表
     */
    public List<ProxyInfo> getValidProxies() {
        // 爬取并验证代理
        listProxies();
        
        // 从Redis中获取有效代理
        return getValidProxiesFromRedis();
    }
    
    /**
     * 从Redis获取有效代理
     */
    private List<ProxyInfo> getValidProxiesFromRedis() {
        String redisKey = ProxyConstants.HTTPS_PROXY_POOL_KEY;
        Map<String, ProxyInfo> validProxies = redisCache.getCacheMap(redisKey);
        
        if (validProxies == null || validProxies.isEmpty()) {
            log.warn("Redis中不存在有效的HTTPS代理");
            return new ArrayList<>();
        }
        
        List<ProxyInfo> validProxyList = new ArrayList<>(validProxies.values());
        log.info("从Redis中获取到{}个有效的HTTPS代理", validProxyList.size());
        
        return validProxyList;
    }
    
    /**
     * 测试Redis中现有的代理，并更新状态
     * 
     * @return 有效代理列表
     */
    public List<ProxyInfo> validateRedisProxies() {
        // 获取Redis中的代理
        String redisKey = ProxyConstants.HTTPS_PROXY_POOL_KEY;
        List<ProxyInfo> proxyList = getProxyListFromRedis(redisKey);
        
        if (isEmptyProxyList(proxyList)) {
            log.warn("Redis中不存在HTTPS代理");
            return new ArrayList<>();
        }
        
        // 验证代理
        List<ProxyInfo> validProxies = validateProxies(proxyList);
        
        // 更新Redis中的代理状态
        updateProxiesInRedis(redisKey, proxyList, validProxies.size());
        
        return validProxies;
    }
    
    /**
     * 从Redis获取代理列表
     */
    private List<ProxyInfo> getProxyListFromRedis(String redisKey) {
        Map<String, ProxyInfo> existingProxies = redisCache.getCacheMap(redisKey);
        
        if (existingProxies == null || existingProxies.isEmpty()) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(existingProxies.values());
    }
    
    /**
     * 更新Redis中的代理
     */
    private void updateProxiesInRedis(String redisKey, List<ProxyInfo> proxyList, int validCount) {
        Map<String, Object> updatedProxyMap = new HashMap<>();
        for (ProxyInfo proxy : proxyList) {
            String key = proxy.getIp() + ":" + proxy.getPort();
            updatedProxyMap.put(key, proxy);
        }
        
        // 保存更新后的代理到Redis
        redisCache.setCacheMap(redisKey, updatedProxyMap);
        
        // 设置过期时间
        redisCache.expire(redisKey, ProxyConstants.REDIS_EXPIRE_HOURS, TimeUnit.HOURS);
        
        log.info("成功更新{}个HTTPS代理的状态，其中{}个有效", proxyList.size(), validCount);
    }
    
    /**
     * 定时爬取HTTPS代理
     * 每天执行一次
     */
    // @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledScrapeProxies() {
        log.info("开始定时爬取HTTPS代理");
        getValidProxies();
        log.info("定时爬取HTTPS代理完成");
    }
    
    /**
     * 定时测试现有代理
     * 每小时执行一次
     */
    // @Scheduled(cron = "0 0 * * * ?")
    public void scheduledTestProxies() {
        log.info("开始定时测试HTTPS代理");
        validateRedisProxies();
        log.info("定时测试HTTPS代理完成");
    }
}
