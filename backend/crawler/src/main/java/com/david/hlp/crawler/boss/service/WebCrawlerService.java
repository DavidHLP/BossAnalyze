package com.david.hlp.crawler.boss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.util.Collections;
import com.david.hlp.crawler.boss.entity.HtmlData;
import com.david.hlp.crawler.boss.exception.IpBlockedException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Boss网站爬虫服务
 * 负责获取目标URL的HTML数据并处理验证、IP限制等问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebCrawlerService {
    // 常量定义
    private static final String RES_URL = "https://www.zhipin.com/web/geek/job?";
    private static final String END_URL = "https://www.zhipin.com";
    private static final String IP_BLOCK_MESSAGE = "当前IP地址可能存在异常访问行为，完成验证后即可正常使用.";
    private static final long WAIT_TIME_MIN = 10000;
    private static final long WAIT_TIME_MAX = 20000;
    private static final long VERIFY_WAIT_TIMEOUT = 10;
    private static final String[] EXCEPTION_HTML = {
            "正在加载中...", "正在加载中", "网站访客身份验证 - BOSS直聘",
            "当前 IP 地址可能存在异常访问行为，完成验证后即可正常使用.", "点击进行验证"
    };

    // 依赖注入
    private final ChromeOptions chromeOptions;
    private final Random random;

    /**
     * 获取职位列表页面数据
     */
    public List<HtmlData> getUrlWithSelenium(String baseType, String baseCityCode,
            String basePositionCode, String baseCity,
            String basePosition, int baseIndex) {
        // 构建目标URL
        String url = buildTargetUrl(baseType, baseCityCode, basePositionCode, baseIndex);
        log.info("爬取URL: {}，城市: {}，职位: {}", url, baseCity, basePosition);

        List<HtmlData> resList = new ArrayList<>();
        WebDriver driver = null;

        try {
            // 获取cookies
            Set<Cookie> cookies = initDriverAndGetCookies();

            // 使用cookies访问目标页面
            driver = initDriverWithCookies(cookies);
            String html = getPageContent(driver, url);

            // 解析职位链接
            resList = parseJobLinks(html, baseCity, basePosition, baseCityCode, basePositionCode);
            log.info("解析到{}条职位数据", resList.size());
        } catch (IpBlockedException e) {
            log.error("IP被封锁: {}", e.getMessage());
        } catch (Exception e) {
            log.error("爬取过程异常: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("异常详情:", e);
            }
        } finally {
            closeWebDriver(driver);
        }

        return resList;
    }

    /**
     * 获取职位详情页面内容
     */
    public HtmlData getHtmlContent(HtmlData htmlData) {
        log.info("获取页面内容: {}", htmlData.getUrl());
        WebDriver driver = null;

        try {
            // 确保URL使用HTTPS协议
            String targetUrl = convertHttpToHttps(htmlData.getUrl());
            htmlData.setUrl(targetUrl);

            // 获取cookies
            Set<Cookie> cookies = initDriverAndGetCookies();

            // 使用cookies访问目标页面
            driver = initDriverWithCookies(cookies);
            String html = getPageContent(driver, targetUrl);

            // 设置HTML内容和状态
            htmlData.setStatus(1);
            htmlData.setHtmlContent(html);

            // 检查内容是否异常
            if (isHtmlContentException(html)) {
                htmlData.setHtmlContentException(true);
                htmlData.setStatus(4);
                log.warn("页面内容异常: {}", targetUrl);
            }

            log.info("页面获取完成: {}", targetUrl);
        } catch (IpBlockedException e) {
            log.error("IP被封锁: {}", e.getMessage());
        } catch (Exception e) {
            log.error("获取页面异常: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("异常详情:", e);
            }
        } finally {
            closeWebDriver(driver);
        }

        return htmlData;
    }

    /**
     * 构建目标URL
     */
    private String buildTargetUrl(String baseType, String baseCityCode, String basePositionCode, int baseIndex) {
        String url = RES_URL + "city=" + baseCityCode;
        if ("position".equals(baseType)) {
            url += "&position=" + basePositionCode;
        } else {
            url += "&industry=" + basePositionCode;
        }
        url += "&page=" + baseIndex;
        return convertHttpToHttps(url);
    }

    /**
     * 初始化WebDriver并获取Cookies
     */
    private Set<Cookie> initDriverAndGetCookies() throws IpBlockedException {
        WebDriver driver = null;
        try {
            driver = initWebDriver();
            driver.get(END_URL);
            checkIpBlocked(driver.getPageSource());
            handleVerification(driver);
            Set<Cookie> cookies = driver.manage().getCookies();
            return cookies;
        } finally {
            closeWebDriver(driver);
        }
    }

    /**
     * 使用Cookies初始化WebDriver
     */
    private WebDriver initDriverWithCookies(Set<Cookie> cookies) throws IpBlockedException {
        WebDriver driver = initWebDriver();
        driver.get(END_URL);
        checkIpBlocked(driver.getPageSource());
        applyCookies(driver, cookies);
        return driver;
    }

    /**
     * 处理验证页面
     */
    private void handleVerification(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(VERIFY_WAIT_TIMEOUT));
            WebElement verifyButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector("button[ka='validate_button_click']")));
            verifyButton.click();
            log.info("检测到验证页面，已点击验证按钮");
            randomSleep();
        } catch (Exception e) {
            // 未找到验证按钮，忽略
        }
    }

    /**
     * 解析职位链接
     */
    private List<HtmlData> parseJobLinks(String html, String baseCity, String basePosition,
            String baseCityCode, String basePositionCode) {
        List<HtmlData> resList = new ArrayList<>();
        Document document = Jsoup.parse(html);
        Elements linkElements = document.select("a.job-card-left");

        if (linkElements.isEmpty()) {
            throw new RuntimeException("未找到职位链接");
        }

        for (int i = 0; i < linkElements.size(); i++) {
            String jobUrl = linkElements.get(i).attr("href");

            // 处理相对URL
            if (jobUrl.startsWith("/")) {
                jobUrl = END_URL + jobUrl.split("\\?")[0];
            } else {
                jobUrl = convertHttpToHttps(jobUrl);
            }

            resList.add(HtmlData.builder()
                    .url(jobUrl)
                    .baseCity(baseCity)
                    .basePosition(basePosition)
                    .baseCityCode(baseCityCode)
                    .basePositionCode(basePositionCode)
                    .htmlContent("")
                    .build());
        }

        return resList;
    }

    /**
     * 检查HTML内容是否异常
     */
    private boolean isHtmlContentException(String html) {
        if (html != null && !html.isEmpty()) {
            for (String exception : EXCEPTION_HTML) {
                if (html.contains(exception)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 初始化WebDriver
     */
    private WebDriver initWebDriver() {
        // 设置浏览器选项
        chromeOptions.addArguments("--headless=new");

        // 反检测设置
        chromeOptions.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        chromeOptions.setExperimentalOption("useAutomationExtension", false);
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");

        // 模拟真实用户
        chromeOptions.addArguments(
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        chromeOptions.addArguments("--lang=zh-CN");

        // 性能优化
        chromeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu",
                "--disable-extensions", "--disable-infobars", "--remote-allow-origins=*");

        return new ChromeDriver(chromeOptions);
    }

    /**
     * 应用Cookies
     */
    private void applyCookies(WebDriver driver, Set<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            driver.manage().addCookie(cookie);
        }
    }

    /**
     * 获取页面内容
     */
    private String getPageContent(WebDriver driver, String url) {
        log.info("访问页面: {}", url);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        // 重试机制
        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                driver.get(url);
                randomSleep();
                String html = driver.getPageSource();
                checkIpBlocked(html);
                return html;
            } catch (Exception e) {
                lastException = e;
                log.warn("第{}次访问失败: {}", retryCount + 1, e.getMessage());
                retryCount++;

                if (retryCount < maxRetries) {
                    long waitTime = WAIT_TIME_MIN * (retryCount + 1);
                    sleep(waitTime);
                }
            }
        }

        if (lastException != null) {
            throw new RuntimeException("访问页面失败: " + lastException.getMessage(), lastException);
        }

        return driver.getPageSource();
    }

    /**
     * 将HTTP URL转换为HTTPS URL
     */
    private String convertHttpToHttps(String url) {
        if (url != null && url.startsWith("http://")) {
            return url.replace("http://", "https://");
        }
        return url;
    }

    /**
     * 关闭WebDriver
     */
    private void closeWebDriver(WebDriver driver) {
        if (driver != null) {
            driver.quit();
            randomSleep();
        }
    }

    /**
     * 检查HTML内容是否包含IP被封锁的信息
     */
    private void checkIpBlocked(String htmlContent) throws IpBlockedException {
        if (htmlContent != null && !htmlContent.isEmpty() && htmlContent.contains(IP_BLOCK_MESSAGE)) {
            throw new IpBlockedException("IP访问受限，需要完成验证");
        }
    }

    /**
     * 随机休眠
     */
    private void randomSleep() {
        sleep(WAIT_TIME_MIN + (long) (random.nextDouble() * (WAIT_TIME_MAX - WAIT_TIME_MIN)));
    }

    /**
     * 指定时间休眠
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 保存HTML内容到文件
     */
    public String saveHtmlToFile(String htmlContent, String fileName, String directory) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            log.error("HTML内容为空，无法保存");
            return null;
        }

        try {
            // 确保目录存在
            String dir = directory == null || directory.isEmpty() ? "./" : directory;
            File dirFile = new File(dir);
            if (!dirFile.exists() && !dirFile.mkdirs()) {
                log.error("创建目录失败: {}", dir);
                return null;
            }

            // 生成文件名和路径
            String finalFileName;
            if (fileName == null || fileName.isEmpty()) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                finalFileName = "html_" + timestamp + ".html";
            } else {
                finalFileName = fileName.endsWith(".html") ? fileName : fileName + ".html";
            }
            String filePath = dir + (dir.endsWith("/") ? "" : "/") + finalFileName;

            // 写入文件
            File file = new File(filePath);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(htmlContent);
                log.info("HTML已保存到: {}", filePath);
                return filePath;
            }
        } catch (IOException e) {
            log.error("保存HTML文件失败: {}", e.getMessage());
            return null;
        }
    }
}
