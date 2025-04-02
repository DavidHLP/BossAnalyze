package com.david.hlp.Spring.crawler.boss.service.boss_2024;

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
import com.david.hlp.Spring.crawler.boss.entity.HTMLData;
import com.david.hlp.Spring.crawler.boss.exception.IpBlockedException;

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
/*
 * 爬虫服务类
 *
 * 负责获取目标URL的HTML数据，并保存到本地文件
 *
 * buildTargetUrl - 构建目标URL
 * initWebDriver - 初始化WebDriver
 * closeWebDriver - 关闭WebDriver
 * handleVerification - 处理验证页面
 * applyCookies - 应用Cookies
 * getPageContent - 获取页面内容
 * parseJobLinks - 解析职位链接
 * buildHtmlData - 构建HTMLData对象
 * handleIpBlockedException - 处理IP被封锁异常
 * handleGeneralException - 处理通用异常
 * randomSleep - 随机休眠
 * ensureDirectoryExists - 确保目录存在
 * generateFileName - 生成文件名
 * buildFilePath - 构建文件路径
 * writeHtmlToFile - 写入HTML到文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebCrawlerService {

    /**
     * Boss直聘基础URL
     */
    private static final String RES_URL = "https://www.zhipin.com/web/geek/job?";
    /**
     * Boss直聘域名URL
     */
    private static final String END_URL = "https://www.zhipin.com";

    /**
     * IP被封锁的提示信息
     */
    private static final String IP_BLOCK_MESSAGE = "当前IP地址可能存在异常访问行为，完成验证后即可正常使用.";

    /**
     * 默认等待时间最小值(毫秒)
     */
    private static final long WAIT_TIME_MIN = 5000;

    /**
     * 默认等待时间最大值(毫秒)
     */
    private static final long WAIT_TIME_MAX = 30000;

    /**
     * 验证等待超时时间(秒)
     */
    private static final long VERIFY_WAIT_TIMEOUT = 10;

    private final ChromeOptions chromeOptions;
    private final Random random;

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
        String baseUrl = buildTargetUrl(baseType, baseCityCode, basePositionCode, baseIndex);
        log.info("爬虫启动，URL: {}，城市: {}，职位: {}，页码: {}", baseUrl, baseCity, basePosition, baseIndex);

        List<HTMLData> resList = new ArrayList<>();
        WebDriver driver = null;

        try {
            driver = initWebDriver();
            driver.get(END_URL);
            checkIpBlocked(driver.getPageSource());

            // 处理验证页面并获取Cookies
            handleVerification(driver);
            Set<Cookie> cookies = driver.manage().getCookies();
            closeWebDriver(driver);

            // 重新初始化浏览器并应用Cookies
            driver = initWebDriver();
            driver.get(END_URL);
            checkIpBlocked(driver.getPageSource());
            applyCookies(driver, cookies);

            // 访问目标页面并获取职位数据
            String html = getPageContent(driver, baseUrl);
            checkIpBlocked(html);

            // 解析职位数据
            resList = parseJobLinks(html, baseCity, basePosition, baseCityCode, basePositionCode);
            log.info("共返回{}条数据", resList.size());

            return resList;
        } catch (IpBlockedException e) {
            handleIpBlockedException(e);
            return resList;
        } catch (Exception e) {
            handleGeneralException(e, "爬虫执行");
            return resList;
        } finally {
            closeWebDriver(driver);
        }
    }

    /**
     * 构建目标URL
     *
     * @param baseType 类型，"position"表示按职位，其他值表示按行业
     * @param baseCityCode 城市代码
     * @param basePositionCode 职位或行业代码
     * @param baseIndex 页码
     * @return 构建好的URL
     */
    private String buildTargetUrl(String baseType, String baseCityCode, String basePositionCode, int baseIndex) {
        String url;
        if ("position".equals(baseType)) {
            url = RES_URL + "city=" + baseCityCode + "&position=" + basePositionCode + "&page=" + baseIndex;
        } else {
            url = RES_URL + "city=" + baseCityCode + "&industry=" + basePositionCode + "&page=" + baseIndex;
        }
        // 确保URL使用HTTPS协议
        return convertHttpToHttps(url);
    }

    /**
     * 初始化WebDriver
     *
     * @return 初始化好的WebDriver实例
     */
    private WebDriver initWebDriver() {
        chromeOptions.addArguments("--headless=new");

        // 反检测设置
        chromeOptions.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        chromeOptions.setExperimentalOption("useAutomationExtension", false);
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");

        // 模拟真实用户
        chromeOptions.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        chromeOptions.addArguments("--lang=zh-CN");

        // 性能优化
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--disable-extensions");
        chromeOptions.addArguments("--disable-infobars");
        chromeOptions.addArguments("--remote-allow-origins=*");
        return new ChromeDriver(chromeOptions);
    }

    /**
     * 关闭WebDriver
     *
     * @param driver WebDriver实例
     */
    private void closeWebDriver(WebDriver driver) {
        if (driver != null) {
            driver.quit();
            log.debug("WebDriver已关闭");
            randomSleep();
        }
    }

    /**
     * 处理验证页面
     *
     * @param driver WebDriver实例
     */
    private void handleVerification(WebDriver driver) {
        try {
            log.info("检查验证按钮");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(VERIFY_WAIT_TIMEOUT));
            WebElement verifyButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("button[ka='validate_button_click']"))
            );
            verifyButton.click();
            log.info("验证按钮已点击");
            randomSleep();  // 等待手动完成验证
        } catch (Exception e) {
            log.info("未发现验证页面");
        }
    }

    /**
     * 应用Cookies到WebDriver
     *
     * @param driver WebDriver实例
     * @param cookies Cookies集合
     */
    private void applyCookies(WebDriver driver, Set<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            driver.manage().addCookie(cookie);
        }
        log.debug("已应用{}个Cookie", cookies.size());
    }

    /**
     * 获取页面内容
     *
     * @param driver WebDriver实例
     * @param url 目标URL
     * @return 页面HTML内容
     */
    private String getPageContent(WebDriver driver, String url) {
        log.info("访问目标页面: {}", url);

        // 设置页面加载超时
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));

        // 添加重试机制
        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                driver.get(url);
                randomSleep();  // 等待页面加载完成
                return driver.getPageSource();
            } catch (Exception e) {
                lastException = e;
                log.warn("第{}次访问页面{}失败: {}", retryCount + 1, url, e.getMessage());
                retryCount++;

                if (retryCount < maxRetries) {
                    // 增加等待时间
                    long waitTime = WAIT_TIME_MIN * (retryCount + 1);
                    log.info("等待{}毫秒后重试...", waitTime);
                    sleep(waitTime);
                }
            }
        }

        // 所有重试都失败，抛出最后一个异常
        if (lastException != null) {
            log.error("访问页面{}失败，已重试{}次", url, maxRetries);
            throw new RuntimeException("访问页面失败: " + lastException.getMessage(), lastException);
        }

        return driver.getPageSource();
    }

    /**
     * 解析职位链接
     *
     * @param html HTML内容
     * @param baseCity 城市名称
     * @param basePosition 职位名称
     * @param baseCityCode 城市代码
     * @param basePositionCode 职位代码
     * @return HTML数据对象列表
     */
    private List<HTMLData> parseJobLinks(String html, String baseCity, String basePosition,
                                         String baseCityCode, String basePositionCode) {
        List<HTMLData> resultList = new ArrayList<>();
        Document document = Jsoup.parse(html);
        Elements linkElements = document.select("a.job-card-left");

        if (linkElements.size() == 0) {
            log.error("未找到职位链接");
            throw new RuntimeException("未找到职位链接");
        }

        log.info("找到{}个职位链接", linkElements.size());

        for (int i = 0; i < linkElements.size(); i++) {
            String url = linkElements.get(i).attr("href");
            // 处理相对URL
            if (url.startsWith("/")) {
                url = END_URL + url.split("\\?")[0];  // 生成完整的职位URL
            } else {
                // 确保URL使用HTTPS
                url = convertHttpToHttps(url);
            }
            HTMLData htmlData = buildHtmlData(url, baseCity, basePosition, baseCityCode, basePositionCode);
            resultList.add(htmlData);
        }

        return resultList;
    }

    /**
     * 将HTTP URL转换为HTTPS URL
     *
     * @param url 原始URL
     * @return 转换后的URL（如果原始URL是HTTP则转为HTTPS，否则保持不变）
     */
    private String convertHttpToHttps(String url) {
        if (url != null && url.startsWith("http://")) {
            log.debug("将HTTP URL转换为HTTPS: {}", url);
            return url.replace("http://", "https://");
        }
        return url;
    }

    /**
     * 构建HTMLData对象
     *
     * @param url 职位URL
     * @param baseCity 城市名称
     * @param basePosition 职位名称
     * @param baseCityCode 城市代码
     * @param basePositionCode 职位代码
     * @return HTMLData对象
     */
    private HTMLData buildHtmlData(String url, String baseCity, String basePosition, 
                                   String baseCityCode, String basePositionCode) {
        return HTMLData.builder()
                .url(url)
                .baseCity(baseCity)
                .basePosition(basePosition)
                .baseCityCode(baseCityCode)
                .basePositionCode(basePositionCode)
                .htmlContent("")
                .build();
    }

    /**
     * 处理IP被封锁异常
     *
     * @param e IP被封锁异常
     */
    private void handleIpBlockedException(IpBlockedException e) {
        log.error("IP被封锁: {}", e.getMessage());
    }

    /**
     * 处理通用异常
     *
     * @param e 异常
     * @param operation 操作描述
     */
    private void handleGeneralException(Exception e, String operation) {
        log.error("{}发生错误: {}", operation, e.getMessage());
        if (log.isDebugEnabled()) {
            log.debug("异常详情:", e);
        }
    }

    /**
     * 随机休眠一段时间
     */
    private void randomSleep() {
        sleep((long)(random.nextDouble() * (WAIT_TIME_MAX - WAIT_TIME_MIN) + WAIT_TIME_MIN));
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
            // 确保URL使用HTTPS协议
            String targetUrl = convertHttpToHttps(htmlData.getUrl());
            htmlData.setUrl(targetUrl);

            // 初始化浏览器并获取Cookies
            driver = initWebDriver();
            driver.get(END_URL);
            checkIpBlocked(driver.getPageSource());

            // 等待手动验证完成并获取Cookies
            randomSleep();
            Set<Cookie> cookies = driver.manage().getCookies();
            closeWebDriver(driver);

            // 重新初始化浏览器并应用Cookies
            driver = initWebDriver();
            driver.get(END_URL);
            checkIpBlocked(driver.getPageSource());
            applyCookies(driver, cookies);

            // 访问目标职位页面并获取HTML
            String html = getPageContent(driver, targetUrl);
            checkIpBlocked(html);

            htmlData.setHtmlContent(html);
            log.info("页面获取成功: {}", targetUrl);
        } catch (IpBlockedException e) {
            handleIpBlockedException(e);
        } catch (Exception e) {
            handleGeneralException(e, "获取页面" + htmlData.getUrl());
        } finally {
            closeWebDriver(driver);
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
            String dir = ensureDirectoryExists(directory);
            if (dir == null) {
                return null;
            }

            String finalFileName = generateFileName(fileName);
            String filePath = buildFilePath(dir, finalFileName);

            return writeHtmlToFile(htmlContent, filePath);
        } catch (IOException e) {
            handleGeneralException(e, "保存HTML内容到文件");
            return null;
        }
    }

    /**
     * 确保目录存在
     *
     * @param directory 目录路径
     * @return 处理后的目录路径，创建失败则返回null
     */
    private String ensureDirectoryExists(String directory) {
        String dir = directory == null || directory.isEmpty() ? "./" : directory;
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            boolean mkdirResult = dirFile.mkdirs();
            if (!mkdirResult) {
                log.error("创建目录失败: {}", dir);
                return null;
            }
        }
        return dir;
    }

    /**
     * 生成文件名
     *
     * @param fileName 原始文件名
     * @return 处理后的文件名
     */
    private String generateFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            // 使用时间戳作为文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            return "html_" + timestamp + ".html";
        } else {
            // 确保文件名有.html后缀
            return fileName.endsWith(".html") ? fileName : fileName + ".html";
        }
    }

    /**
     * 构建文件完整路径
     *
     * @param directory 目录
     * @param fileName 文件名
     * @return 完整文件路径
     */
    private String buildFilePath(String directory, String fileName) {
        return directory + (directory.endsWith("/") ? "" : "/") + fileName;
    }

    /**
     * 将HTML内容写入文件
     *
     * @param htmlContent HTML内容
     * @param filePath 文件路径
     * @return 成功则返回文件路径，失败返回null
     * @throws IOException 写入文件失败
     */
    private String writeHtmlToFile(String htmlContent, String filePath) throws IOException {
        File file = new File(filePath);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(htmlContent);
            log.info("HTML内容已保存到文件: {}", filePath);
            return filePath;
        }
    }

    /**
     * 检查HTML内容是否包含IP被封锁的信息
     *
     * @param htmlContent HTML内容
     * @throws IpBlockedException 如果检测到IP被封锁
     */
    private void checkIpBlocked(String htmlContent) throws IpBlockedException {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return;
        }

        if (htmlContent.contains(IP_BLOCK_MESSAGE)) {
            log.warn("检测到IP被限制访问");
            throw new IpBlockedException("IP访问受限，需要完成验证");
        }
    }
}
