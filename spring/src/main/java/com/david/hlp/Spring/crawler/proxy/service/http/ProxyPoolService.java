package com.david.hlp.Spring.crawler.proxy.service.http;

import com.david.hlp.Spring.common.util.RedisCache;
import com.david.hlp.Spring.crawler.proxy.model.entity.ProxyInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * 代理池服务
 */
@Slf4j
@Service("HttpProxyPoolService")
@RequiredArgsConstructor
public class ProxyPoolService {

    /**
     * 测试URL
     */
    private static final String TEST_URL = "http://httpbin.org/ip";
    /**
     * 连接超时时间（秒）
     */
    private static final int TIMEOUT = 5;
    /**
     * Redis代理池键
     */
    private static final String PROXY_POOL_KEY = "proxy:http:pool";
    /**
     * 页面加载等待时间（秒）
     */
    private static final int PAGE_LOAD_WAIT = 3;
    /**
     * 页面爬取间隔时间（秒）
     */
    private static final int PAGE_CRAWL_INTERVAL = 5;
    /**
     * 最大爬取页数
     */
    private static final int MAX_CRAWL_PAGES = 50;

    private final ChromeOptions proxyPoolChromeOptions;

    private final RedisCache redisCache;
    /**
     * 使用Selenium获取网页的HTML内容
     *
     * @param url 要访问的网页URL
     * @return 网页HTML内容
     */
    public String getWebpage(String url) {
        WebDriver driver = null;
        try {
            // 初始化WebDriver
            driver = new ChromeDriver(proxyPoolChromeOptions);
            // 访问网页
            driver.get(url);
            // 等待页面加载完成
            TimeUnit.SECONDS.sleep(PAGE_LOAD_WAIT);
            // 获取网页HTML内容
            String htmlContent = driver.getPageSource();
            log.info("成功获取网页内容");
            return htmlContent;
        } catch (Exception e) {
            log.error("获取网页内容出错: {}", e.getMessage(), e);
            return null;
        } finally {
            // 关闭浏览器
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * 解析HTML内容，提取代理IP数据
     *
     * @param htmlContent HTML内容
     * @return 代理信息列表
     */
    public List<ProxyInfo> parseProxyData(String htmlContent) {
        List<ProxyInfo> proxyList = new ArrayList<>();
        try {
            // 使用Jsoup解析HTML
            Document doc = Jsoup.parse(htmlContent);
            // 查找代理数据表格
            Element tbody = doc.selectFirst(".kdl-table-tbody");
            if (tbody != null) {
                // 获取所有行
                Elements rows = tbody.select("tr");
                for (Element row : rows) {
                    // 获取每一行中的所有单元格
                    Elements cells = row.select("td.kdl-table-cell");

                    if (cells.size() >= 8) {
                        ProxyInfo proxyInfo = new ProxyInfo();
                        proxyInfo.setIp(cells.get(0).text().trim());
                        proxyInfo.setPort(cells.get(1).text().trim());
                        proxyInfo.setAnonymity(cells.get(2).text().trim());
                        proxyInfo.setType(cells.get(3).text().trim());
                        proxyInfo.setLocation(cells.get(4).text().trim());
                        proxyInfo.setResponseTime(cells.get(5).text().trim());
                        proxyInfo.setLastVerifyTime(cells.get(6).text().trim());
                        proxyInfo.setPaymentType(cells.get(7).text().trim());

                        proxyList.add(proxyInfo);
                    }
                }
            }

            return proxyList;
        } catch (Exception e) {
            log.error("解析代理数据出错: {}", e.getMessage(), e);
            return proxyList;
        }
    }

    /**
     * 测试代理IP是否可用
     *
     * @param proxyInfo 代理信息
     * @return 是否可用
     */
    public boolean testProxy(ProxyInfo proxyInfo) {
        String ip = proxyInfo.getIp();
        String port = proxyInfo.getPort();
        String proxyType = proxyInfo.getType().toLowerCase();
        
        // 只处理HTTP类型代理
        if (!"http".equals(proxyType)) {
            log.info("跳过非HTTP代理 {}:{} (类型: {})", ip, port, proxyType);
            return false;
        }

        try {
            // 使用HTTP代理
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, Integer.parseInt(port)));

            // 创建连接
            URL url = new URL(TEST_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
            connection.setConnectTimeout(TIMEOUT * 1000);
            connection.setReadTimeout(TIMEOUT * 1000);

            // 获取响应代码
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                log.info("HTTP代理 {}:{} 可用！响应状态码: {}", ip, port, responseCode);
                return true;
            } else {
                log.info("HTTP代理 {}:{} 返回状态码: {}", ip, port, responseCode);
                return false;
            }
        } catch (Exception e) {
            log.info("HTTP代理 {}:{} 不可用: {}", ip, port, e.getMessage());
            return false;
        }
    }

    /**
     * 从HTML内容中提取总页数
     *
     * @param htmlContent HTML内容
     * @return 总页数
     */
    public int getTotalPages(String htmlContent) {
        try {
            // 使用Jsoup解析HTML
            Document doc = Jsoup.parse(htmlContent);

            // 查找最后一页的页码
            Elements paginationItems = doc.select(".pagination__item");
            if (!paginationItems.isEmpty()) {
                // 获取最后一个页码元素的data-index属性值
                int lastPage = 0;
                for (Element item : paginationItems) {
                    String dataIndex = item.attr("data-index");
                    if (dataIndex != null && !dataIndex.isEmpty()) {
                        int pageNum = Integer.parseInt(dataIndex);
                        if (pageNum > lastPage) {
                            lastPage = pageNum;
                        }
                    }
                }
                return lastPage;
            }

            return 0;
        } catch (Exception e) {
            log.error("获取总页数出错: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 获取可用的代理列表
     *
     * @param url 代理网站URL
     * @return 可用的代理列表
     */
    public List<ProxyInfo> getAvailableProxies(String url) {
        List<ProxyInfo> availableProxies = new ArrayList<>();

        // 获取网页内容
        String htmlContent = getWebpage(url);
        if (htmlContent == null || htmlContent.isEmpty()) {
            log.error("无法获取代理网页内容");
            return availableProxies;
        }

        // 获取总页数
        int totalPages = getTotalPages(htmlContent);
        log.info("总页数: {}", totalPages);

        // 解析代理数据
        List<ProxyInfo> proxyList = parseProxyData(htmlContent);

        // 测试代理
        log.info("共找到 {} 个代理IP", proxyList.size());
        int count = 1;
        for (ProxyInfo proxy : proxyList) {
            log.info("\n代理 {}:", count++);
            log.info("  IP: {}", proxy.getIp());
            log.info("  端口: {}", proxy.getPort());
            log.info("  匿名度: {}", proxy.getAnonymity());
            log.info("  类型: {}", proxy.getType());
            log.info("  位置: {}", proxy.getLocation());
            log.info("  响应时间: {}秒", proxy.getResponseTime());
            log.info("  最后验证时间: {}", proxy.getLastVerifyTime());
            log.info("  付费方式: {}", proxy.getPaymentType());

            // 测试代理是否可用
            log.info("  测试代理是否可用...");
            boolean isValid = testProxy(proxy);
            log.info("  测试结果: {}", isValid ? "可用" : "不可用");

            if (isValid) {
                availableProxies.add(proxy);
            }
        }

        return availableProxies;
    }

    /**
     * 爬取所有页面的代理信息并保存到Redis
     *
     * @param baseUrl 代理网站基础URL，不包含页码
     * @return 可用的代理列表
     */
    public List<ProxyInfo> crawlAndSaveAllProxies(String baseUrl) {
        List<ProxyInfo> allAvailableProxies = new ArrayList<>();

        // 获取第一页内容
        String firstPageUrl = baseUrl + "1";
        String htmlContent = getWebpage(firstPageUrl);
        if (htmlContent == null || htmlContent.isEmpty()) {
            log.error("无法获取第一页代理网页内容");
            return allAvailableProxies;
        }

        // 获取总页数
        int totalPages = getTotalPages(htmlContent);
        log.info("总页数: {}", totalPages);

        // 限制最大爬取页数
        int pagesToCrawl = Math.min(totalPages, MAX_CRAWL_PAGES);

        // 爬取第一页
        List<ProxyInfo> firstPageProxies = processPage(htmlContent);
        if (!firstPageProxies.isEmpty()) {
            // 立即保存第一页的可用代理到Redis
            saveProxiesToRedis(firstPageProxies);
            allAvailableProxies.addAll(firstPageProxies);
            log.info("第1页爬取完成，获取到{}个可用代理并已保存到Redis", firstPageProxies.size());
        }

        // 爬取剩余页面
        for (int page = 2; page <= pagesToCrawl; page++) {
            try {
                // 添加爬取间隔，避免请求过于频繁
                log.info("等待{}秒后爬取第{}页...", PAGE_CRAWL_INTERVAL, page);
                TimeUnit.SECONDS.sleep(PAGE_CRAWL_INTERVAL);

                // 构造页面URL
                String pageUrl = baseUrl + page;
                log.info("开始爬取第{}页: {}", page, pageUrl);

                // 获取页面内容
                String pageHtml = getWebpage(pageUrl);
                if (pageHtml == null || pageHtml.isEmpty()) {
                    log.error("无法获取第{}页代理网页内容", page);
                    continue;
                }

                // 处理页面内容
                List<ProxyInfo> pageProxies = processPage(pageHtml);
                if (!pageProxies.isEmpty()) {
                    // 立即保存当前页的可用代理到Redis
                    saveProxiesToRedis(pageProxies);
                    allAvailableProxies.addAll(pageProxies);
                    log.info("第{}页爬取完成，获取到{}个可用代理并已保存到Redis", page, pageProxies.size());
                } else {
                    log.info("第{}页爬取完成，未获取到可用代理", page);
                }
            } catch (Exception e) {
                log.error("爬取第{}页时出错: {}", page, e.getMessage(), e);
            }
        }

        log.info("爬取完成，共获取到{}个可用代理", allAvailableProxies.size());
        return allAvailableProxies;
    }

    /**
     * 处理单个页面的代理信息
     *
     * @param htmlContent 页面HTML内容
     * @return 可用的代理列表
     */
    private List<ProxyInfo> processPage(String htmlContent) {
        List<ProxyInfo> availableProxies = new ArrayList<>();

        // 解析代理数据
        List<ProxyInfo> proxyList = parseProxyData(htmlContent);
        log.info("页面中找到{}个代理IP", proxyList.size());

        // 筛选HTTP代理并测试
        for (ProxyInfo proxy : proxyList) {
            if ("http".equalsIgnoreCase(proxy.getType())) {
                log.info("测试HTTP代理: {}:{}", proxy.getIp(), proxy.getPort());
                boolean isValid = testProxy(proxy);
                if (isValid) {
                    availableProxies.add(proxy);
                    log.info("HTTP代理可用，添加到列表中");
                }
            } else {
                log.info("跳过非HTTP代理: {}:{} (类型: {})", proxy.getIp(), proxy.getPort(), proxy.getType());
            }
        }

        return availableProxies;
    }

    /**
     * 将代理信息保存到Redis
     *
     * @param proxyList 代理信息列表
     */
    public void saveProxiesToRedis(List<ProxyInfo> proxyList) {
        if (proxyList == null || proxyList.isEmpty()) {
            log.warn("没有可用代理需要保存");
            return;
        }

        // 获取Redis中已有的代理
        Map<String, ProxyInfo> existingProxies = redisCache.getCacheMap(PROXY_POOL_KEY);
        Map<String, Object> proxyMap;

        if (existingProxies != null && !existingProxies.isEmpty()) {
            // 如果已有代理，创建副本进行增量更新
            proxyMap = new HashMap<>(existingProxies);
            log.info("Redis中已存在{}个代理，进行增量更新", existingProxies.size());
        } else {
            // 如果没有已有代理，创建新的Map
            proxyMap = new HashMap<>(proxyList.size());
            log.info("Redis中无现有代理，创建新缓存");
        }

        // 添加新的代理
        int newProxiesCount = 0;
        for (ProxyInfo proxy : proxyList) {
            String key = proxy.getIp() + ":" + proxy.getPort();
            if (!proxyMap.containsKey(key)) {
                newProxiesCount++;
            }
            proxyMap.put(key, proxy);
        }

        // 保存到Redis的Hash结构中
        redisCache.setCacheMap(PROXY_POOL_KEY, proxyMap);

        // 设置过期时间（24小时）
        redisCache.expire(PROXY_POOL_KEY, 24, TimeUnit.HOURS);

        log.info("成功将{}个新代理添加到Redis，当前总共有{}个代理", newProxiesCount, proxyMap.size());
    }

    /**
     * 从Redis获取所有代理信息
     *
     * @return 代理信息列表
     */
    public List<ProxyInfo> getProxiesFromRedis() {
        Map<String, ProxyInfo> proxyMap = redisCache.getCacheMap(PROXY_POOL_KEY);
        if (proxyMap == null || proxyMap.isEmpty()) {
            log.warn("Redis中没有可用代理");
            return new ArrayList<>();
        }

        List<ProxyInfo> proxyList = new ArrayList<>(proxyMap.values());
        log.info("从Redis获取到{}个代理", proxyList.size());
        return proxyList;
    }

    /**
     * 从Redis删除特定代理
     *
     * @param proxy 要删除的代理信息
     * @return 是否删除成功
     */
    public boolean removeProxyFromRedis(ProxyInfo proxy) {
        if (proxy == null) {
            return false;
        }

        String key = proxy.getIp() + ":" + proxy.getPort();
        boolean result = redisCache.deleteCacheMapValue(PROXY_POOL_KEY, key);

        if (result) {
            log.info("从Redis中删除代理: {}:{}", proxy.getIp(), proxy.getPort());
        }

        return result;
    }
}
