package com.david.hlp.Spring.boss.service;

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
import org.springframework.beans.factory.annotation.Value;

import com.david.hlp.Spring.boss.entity.HTMLData;

import jakarta.annotation.PostConstruct;
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
@Service
@Slf4j
public class WebCrawlerService {

    @Value("${webdriver.chrome.driver:/home/david/Driver/chromedriver-linux64/chromedriver}")
    private String chromeDriverPath;

    @Value("${webdriver.chrome.headless:true}")
    private boolean headless;

    @Value("${webdriver.chrome.disable-gpu:true}")
    private boolean disableGpu;

    @Value("${webdriver.chrome.no-sandbox:true}")
    private boolean noSandbox;

    @Value("${webdriver.chrome.disable-dev-shm-usage:true}")
    private boolean disableDevShmUsage;

    @PostConstruct
    public void initChromeDriver() {
        // 验证ChromeDriver是否存在
        File driverFile = new File(chromeDriverPath);
        if (!driverFile.exists() || !driverFile.canExecute()) {
            log.error("ChromeDriver不存在或无法执行: {}", chromeDriverPath);
            return;
        }
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        log.info("ChromeDriver初始化成功，路径: {}", chromeDriverPath);
    }

    /**
     * Boss直聘基础URL
     */
    private static final String RES_URL = "https://www.zhipin.com/web/geek/job?";
    /**
     * Boss直聘域名URL
     */
    private static final String END_URL = "https://www.zhipin.com";
    /**
     * 随机数生成器
     */
    private final Random random = new Random();
    /**
     * 服务初始化
     */
    @PostConstruct
    public void init() {
        log.info("WebCrawlerService初始化完成");
    }
    /**
     * 使用Selenium获取目标URL的HTML数据
     *
     * @param baseType 类型，"position"表示按职位，其他值表示按行业
     * @param baseCityCode 城市代码
     * @param basePositionCode 职位或行业代码
     * @param baseCity 城市名称
     * @param basePosition 职位或行业名称
     * @param baseIndex 页码
     * @return HTML数据对象列表
     */
    public List<HTMLData> getUrlWithSelenium(String baseType, String baseCityCode,
                                      String basePositionCode, String baseCity,
                                      String basePosition, int baseIndex) {
        String baseUrl;
        if ("position".equals(baseType)) {
            baseUrl = RES_URL + "city=" + baseCityCode + "&position=" + basePositionCode + "&page=" + baseIndex;
        } else {
            baseUrl = RES_URL + "city=" + baseCityCode + "&industry=" + basePositionCode + "&page=" + baseIndex;
        }
        log.info("爬虫启动，URL: {}，城市: {}，职位: {}，页码: {}", baseUrl, baseCity, basePosition, baseIndex);
        List<HTMLData> resList = new ArrayList<>();
        WebDriver driver = null;
        try {
            // System.setProperty("webdriver.chrome.driver", "/home/david/Driver/chromedriver-linux64/chromedriver");
            // 配置ChromeOptions
            ChromeOptions options = getConfiguredChromeOptions();
            // 启动浏览器
            driver = new ChromeDriver(options);
            driver.get(END_URL);
            // 检查并处理验证按钮
            try {
                log.info("检查验证按钮");
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                WebElement verifyButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.cssSelector("button[ka='validate_button_click']"))
                );
                verifyButton.click();
                log.info("验证按钮已点击");
                sleep((long)(random.nextDouble() * 25000 + 5000));  // 等待5-30秒手动完成验证
            } catch (Exception e) {
                log.info("未发现验证页面");
            }
            // 获取Cookies
            Set<Cookie> cookies = driver.manage().getCookies();
            // 关闭浏览器并重新启动以加载Cookies
            driver.quit();
            log.info("浏览器已关闭，准备重启并加载Cookies");
            sleep((long)(random.nextDouble() * 25000 + 5000));  // 等待5-30秒
            driver = new ChromeDriver(options);
            driver.get(END_URL);
            // 添加cookies
            for (Cookie cookie : cookies) {
                driver.manage().addCookie(cookie);
            }
            // 访问目标页面
            log.info("访问目标页面");
            driver.get(baseUrl);
            sleep((long)(random.nextDouble() * 25000 + 5000));  // 等待5-30秒页面加载完成
            String html = driver.getPageSource();
            // 使用Jsoup解析HTML内容
            Document document = Jsoup.parse(html);
            Elements linkElements = document.select("a.job-card-left");
            log.info("找到{}个职位链接", linkElements.size());
            for (int i = 0; i < linkElements.size(); i++) {
                String url = linkElements.get(i).attr("href");
                // 处理相对URL
                if (url.startsWith("/")) {
                    url = END_URL + url.split("\\?")[0];  // 生成完整的职位URL
                }
                HTMLData htmlData = HTMLData.builder()
                        .url(url)
                        .baseCity(baseCity)
                        .basePosition(basePosition)
                        .baseCityCode(baseCityCode)
                        .basePositionCode(basePositionCode)
                        .htmlContent("")
                        .build();
                resList.add(htmlData);
            }
            log.info("共返回{}条数据", resList.size());
            return resList;
        } catch (Exception e) {
            log.error("爬虫执行发生错误: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("异常详情:", e);
            }
            return resList;
        } finally {
            // 确保浏览器关闭
            if (driver != null) {
                driver.quit();
            }
        }
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
     * 获取职位详情页面HTML内容
     *
     * @param htmlData 包含目标URL的HTML数据对象
     * @return 添加了HTML内容的HTML数据对象
     */
    public HTMLData getHtmlContent(HTMLData htmlData) {
        log.info("获取页面内容: {}", htmlData.getUrl());
        WebDriver driver = null;
        try {
            // 设置ChromeDriver路径
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
            // 第一次启动浏览器（用于获取验证后的Cookies）
            ChromeOptions options = getConfiguredChromeOptions();
            driver = new ChromeDriver(options);
            log.info("访问Boss直聘主页进行验证");
            driver.get(END_URL);
            // 等待手动验证完成
            sleep((long)(random.nextDouble() * 25000 + 5000));  // 预留5-30秒手动验证时间
            // 获取Cookies
            Set<Cookie> cookies = driver.manage().getCookies();
            // 关闭并重新启动浏览器，加载Cookies
            driver.quit();
            sleep((long)(random.nextDouble() * 25000 + 5000));
            // 重新启动浏览器并加载Cookies
            driver = new ChromeDriver(options);
            driver.get(END_URL);
            for (Cookie cookie : cookies) {
                driver.manage().addCookie(cookie);
            }
            // 访问目标职位页面并获取HTML
            log.info("访问目标职位页面: {}", htmlData.getUrl());
            driver.get(htmlData.getUrl());
            sleep((long)(random.nextDouble() * 25000 + 5000));  // 页面加载等待5-30秒
            String html = driver.getPageSource();
            htmlData.setHtmlContent(html);
            log.info("页面获取成功: {}", htmlData.getUrl());
        } catch (Exception e) {
            log.error("获取页面{}时发生错误: {}", htmlData.getUrl(), e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("异常详情:", e);
            }
        } finally {
            // 确保浏览器关闭
            if (driver != null) {
                driver.quit();
                log.debug("WebDriver已关闭");
            }
        }
        return htmlData;
    }

    /**
     * 保存HTML内容到本地文件
     *
     * @param htmlContent HTML内容
     * @param fileName 文件名，若为空则使用时间戳生成
     * @param directory 保存目录，默认为当前目录
     * @return 保存的文件路径
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
            if (!dirFile.exists()) {
                boolean mkdirResult = dirFile.mkdirs();
                if (!mkdirResult) {
                    log.error("创建目录失败: {}", dir);
                    return null;
                }
            }
            // 生成文件名
            String finalFileName;
            if (fileName == null || fileName.isEmpty()) {
                // 使用时间戳作为文件名
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                finalFileName = "html_" + timestamp + ".html";
            } else {
                // 确保文件名有.html后缀
                finalFileName = fileName.endsWith(".html") ? fileName : fileName + ".html";
            }
            // 完整文件路径
            String filePath = dir + (dir.endsWith("/") ? "" : "/") + finalFileName;
            File file = new File(filePath);
            // 写入文件
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(htmlContent);
                log.info("HTML内容已保存到文件: {}", filePath);
                return filePath;
            }
        } catch (IOException e) {
            log.error("保存HTML内容到文件时发生错误: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("异常详情:", e);
            }
            return null;
        }
    }

    /**
     * 获取已配置的ChromeOptions
     * 
     * @return 配置好的ChromeOptions对象
     */
    private ChromeOptions getConfiguredChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless");
        }
        if (disableGpu) {
            options.addArguments("--disable-gpu");
        }
        if (noSandbox) {
            options.addArguments("--no-sandbox");
        }
        if (disableDevShmUsage) {
            options.addArguments("--disable-dev-shm-usage");
        }
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
        return options;
    }
}
