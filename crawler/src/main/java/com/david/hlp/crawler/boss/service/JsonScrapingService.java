package com.david.hlp.crawler.boss.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// Playwright导入
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Paths;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Arrays;
import java.util.Random;

/**
 * Boss直聘JSON数据爬取服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonScrapingService {
    // 常量定义
    private static final String TARGET_URL = "https://www.zhipin.com/web/geek/jobs?";
    private static final String API_URL = "https://www.zhipin.com/wapi/zpgeek/search/joblist.json?";
    private static final Integer PAGE_SIZE = 30;
    private static final String BASE_URL_JOB = "https://www.zhipin.com/job_detail/{}.html";
    private static final long VERIFICATION_WAIT_TIME = 30000; // 验证码等待时间
    private static final long IP_BLOCK_WAIT_TIME = 300000; // IP被封等待时间(5分钟)
    
    // Token管理
    private static final Map<String, Map<String, String>> TOKEN_MAP = new ConcurrentHashMap<>();

    // 默认请求头
    private static final Map<String, String> DEFAULT_HEADERS = new HashMap<String, String>() {
        {
            put("Accept", "application/json, text/plain, */*");
            put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            put("Sec-Fetch-Dest", "empty");
            put("Sec-Fetch-Mode", "cors");
            put("Sec-Fetch-Site", "same-origin");
            put("Cache-Control", "no-cache");
            put("Pragma", "no-cache");
            put("X-Requested-With", "XMLHttpRequest");
            put("Connection", "keep-alive");
        }
    };

    // 随机User-Agent列表
    private static final String[] USER_AGENT_LIST = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:138.0) Gecko/20100101 Firefox/138.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 OPR/106.0.0.0"
    };

    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取访问API所需的token
     */
    public Map<String, Object> getToken(boolean isHeadless, String cityCode, String positionCode) {
        log.info("获取token(无头模式:{})", isHeadless);
        String targetUrl = TARGET_URL + "city=" + cityCode + "&position=" + positionCode;

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(isHeadless));
            // 创建浏览器上下文
            BrowserContext context = createBrowserContext(browser, null, null, cityCode, positionCode, 1);
            Page page = context.newPage();

            try {
                // 设置页面基本配置并访问目标URL
                page.setDefaultTimeout(60000);
                page.setDefaultNavigationTimeout(60000);
                page.navigate(targetUrl);
                page.waitForLoadState();
                randomWait(page, 2000, 5000);

                // 处理验证码页面
                if (page.url().contains("verify")) {
                    log.warn("需要手动处理验证码，等待30秒");
                    page.waitForTimeout(VERIFICATION_WAIT_TIME);
                    page.navigate(TARGET_URL + "city=" + cityCode + "&position=" + positionCode);
                    page.waitForLoadState();
                }
                randomWait(page, 3000, 7000);

                simulateUserBehavior(page);

                // 从页面获取token
                String token = null;
                try {
                    token = page.evaluate("() => { " +
                            "const cookie = document.cookie; " +
                            "const match = cookie.match(/__zp_stoken__=([^;]+)/); " +
                            "return match ? decodeURIComponent(match[1]) : ''; " +
                            "}").toString();
                } catch (Exception e) {
                    log.error("JS获取token失败: {}", e.getMessage());
                }

                // 如果页面获取失败，尝试从context获取
                if (token == null || token.isEmpty()) {
                    for (com.microsoft.playwright.options.Cookie cookie : context.cookies()) {
                        if ("__zp_stoken__".equals(cookie.name)) {
                            token = cookie.value;
                            break;
                        }
                    }
                }

                if (token == null || token.isEmpty()) {
                    try {
                        page.screenshot(new Page.ScreenshotOptions()
                                .setPath(Paths.get("no_token_" + System.currentTimeMillis() / 1000 + ".png")));
                    } catch (Exception e) {
                        log.error("截图保存失败", e);
                    }
                    throw new RuntimeException("无法获取__zp_stoken__");
                }

                // 获取所有cookies
                Map<String, String> cookieMap = new HashMap<>();
                for (com.microsoft.playwright.options.Cookie cookie : context.cookies()) {
                    cookieMap.put(cookie.name, cookie.value);
                }

                // 保存token到TOKEN_MAP
                TOKEN_MAP.put(token, cookieMap);
                log.info("成功获取并保存token，当前token数量: {}", TOKEN_MAP.size());

                Map<String, Object> result = new HashMap<>();
                result.put("token", token);
                result.put("cookies", cookieMap);
                return result;
            } catch (Exception e) {
                log.error("获取token错误：{}", e.getMessage());
                try {
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(Paths.get("error_" + System.currentTimeMillis() / 1000 + ".png")));
                } catch (Exception ex) {
                    log.error("截图保存失败", ex);
                }
                return null;
            } finally {
                browser.close();
            }
        }
    }

    /**
     * 模拟用户行为
     */
    private void simulateUserBehavior(Page page) {
        // 随机用户操作和滚动
        if (random.nextDouble() < 0.7)
            performRandomUserAction(page);
        simulateUserScrolling(page);
        if (random.nextDouble() < 0.8) {
            performRandomUserAction(page);
            randomWait(page, 1000, 4000);
        }

        // 检查token是否已设置
        try {
            page.waitForFunction("document.cookie.includes('__zp_stoken__')", null,
                    new Page.WaitForFunctionOptions().setTimeout(10000));
        } catch (Exception e) {
            log.warn("等待token设置超时");
        }
    }

    /**
     * 模拟用户滚动行为
     */
    private void simulateUserScrolling(Page page) {
        randomWait(page, 1000, 3000);
        int scrollTimes = 3 + random.nextInt(6);

        // 生成滚动位置并执行滚动
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < scrollTimes; i++) {
            int pos = 300 * (i + 1) + random.nextInt(200) - 100;
            positions.add(Math.max(100, pos));

            scrollToPosition(page, positions.get(i), 1500 + random.nextInt(1500));
            if (random.nextDouble() < 0.7)
                performRandomUserAction(page);
        }

        // 有时会在列表中上下移动
        if (random.nextDouble() < 0.6) {
            // 从下往上滚动几次
            for (int i = 0; i < 1 + random.nextInt(3); i++) {
                int randomIndex = random.nextInt(positions.size());
                if (randomIndex > 0) {
                    scrollToPosition(page, positions.get(randomIndex - 1), 1000 + random.nextInt(2000));
                }
            }
        }

        // 最后随机回到某个位置
        scrollToPosition(page, positions.get(random.nextInt(positions.size())) / 2,
                2000 + random.nextInt(2000));
    }

    /**
     * 执行随机用户操作
     */
    private void performRandomUserAction(Page page) {
        try {
            double action = random.nextDouble();
            if (action < 0.3) {
                randomMouseMovement(page);
            } else if (action < 0.5) {
                randomWait(page, 1000, 5000);
            } else if (action < 0.7) {
                attemptRandomElementClick(page);
            } else if (action < 0.8) {
                randomWindowResize(page);
            } else {
                simulateJobDetailView(page);
            }
        } catch (Exception e) {
            log.warn("随机操作出错: {}", e.getMessage());
        }
    }

    /**
     * 随机鼠标移动
     */
    private void randomMouseMovement(Page page) {
        try {
            int pageWidth = (int) page.evaluate("window.innerWidth");
            int pageHeight = (int) page.evaluate("window.innerHeight");
            int movements = 3 + random.nextInt(4);

            for (int i = 0; i < movements; i++) {
                page.mouse().move(random.nextInt(pageWidth), random.nextInt(pageHeight));
                page.waitForTimeout(200 + random.nextInt(800));
            }
        } catch (Exception e) {
            log.warn("鼠标移动失败: {}", e.getMessage());
        }
    }

    /**
     * 随机等待
     */
    private void randomWait(Page page, int minMs, int maxMs) {
        page.waitForTimeout(minMs + random.nextInt(maxMs - minMs));
    }

    /**
     * 尝试随机点击元素
     */
    private void attemptRandomElementClick(Page page) {
        try {
            List<String> selectors = Arrays.asList(
                    ".job-card", ".filter-select", ".dorpdown-menu",
                    ".search-box", ".city-sel", ".search-condition-wrapper a");

            String selector = selectors.get(random.nextInt(selectors.size()));
            if ((boolean) page.evaluate("!!document.querySelector('" + selector + "')")) {
                int count = (int) page.evaluate("document.querySelectorAll('" + selector + "').length");
                if (count > 0) {
                    int index = random.nextInt(Math.min(count, 5));
                    String currentUrl = page.url();

                    // 滚动到元素并点击
                    page.evaluate("document.querySelectorAll('" + selector + "')[" + index +
                            "].scrollIntoView({behavior: 'smooth', block: 'center'})");
                    randomWait(page, 500, 1500);
                    page.evaluate("document.querySelectorAll('" + selector + "')[" + index + "].click()");
                    randomWait(page, 2000, 5000);

                    // 如果URL改变，返回原页面
                    if (!page.url().equals(currentUrl)) {
                        page.goBack();
                        page.waitForLoadState();
                        randomWait(page, 1000, 3000);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("点击元素失败: {}", e.getMessage());
        }
    }

    /**
     * 随机调整窗口大小
     */
    private void randomWindowResize(Page page) {
        try {
            int currentWidth = (int) page.evaluate("window.innerWidth");
            int currentHeight = (int) page.evaluate("window.innerHeight");

            int widthDelta = (int) (currentWidth * 0.1);
            int heightDelta = (int) (currentHeight * 0.1);

            int newWidth = Math.max(800, Math.min(1600,
                    currentWidth + random.nextInt(widthDelta * 2) - widthDelta));
            int newHeight = Math.max(600, Math.min(1200,
                    currentHeight + random.nextInt(heightDelta * 2) - heightDelta));

            page.setViewportSize(newWidth, newHeight);
            randomWait(page, 1000, 3000);

            if (random.nextDouble() < 0.5) {
                page.setViewportSize(currentWidth, currentHeight);
            }
        } catch (Exception e) {
            log.warn("窗口调整失败: {}", e.getMessage());
        }
    }

    /**
     * 模拟查看职位详情
     */
    private void simulateJobDetailView(Page page) {
        try {
            if ((boolean) page.evaluate("!!document.querySelector('.job-card')")) {
                int count = (int) page.evaluate("document.querySelectorAll('.job-card').length");
                if (count > 0) {
                    int index = random.nextInt(Math.min(count, 10));

                    // 滚动到卡片并模拟悬停
                    page.evaluate("document.querySelectorAll('.job-card')[" + index +
                            "].scrollIntoView({behavior: 'smooth', block: 'center'})");
                    randomWait(page, 800, 2000);

                    // 鼠标悬停
                    page.evaluate("document.querySelectorAll('.job-card')[" + index +
                            "].dispatchEvent(new MouseEvent('mouseover', {bubbles: true}))");
                    randomWait(page, 500, 1500);

                    // 模拟阅读行为
                    page.evaluate(
                            "const card = document.querySelectorAll('.job-card')[" + index + "];" +
                                    "let top = card.offsetTop;" +
                                    "const height = card.offsetHeight;" +
                                    "const smallScroll = height / 4;" +
                                    "window.scrollTo({top: top, behavior: 'smooth'});" +
                                    "setTimeout(() => {" +
                                    "  window.scrollTo({top: top + smallScroll, behavior: 'smooth'});" +
                                    "}, 800);" +
                                    "setTimeout(() => {" +
                                    "  window.scrollTo({top: top + smallScroll * 2, behavior: 'smooth'});" +
                                    "}, 1600);");

                    randomWait(page, 2000, 4000);
                }
            }
        } catch (Exception e) {
            log.warn("模拟查看职位详情失败: {}", e.getMessage());
        }
    }

    /**
     * 滚动到指定位置
     */
    private void scrollToPosition(Page page, int position, int waitTime) {
        if (random.nextDouble() < 0.7) {
            // 平滑滚动
            page.evaluate("window.scrollTo({top: " + position + ", behavior: 'smooth'})");
        } else {
            // 分段滚动，模拟人工操作
            int currentPosition = (int) page.evaluate("window.scrollY");
            int distance = position - currentPosition;
            int steps = 5 + random.nextInt(10);

            for (int i = 1; i <= steps; i++) {
                page.evaluate("window.scrollTo(0, " + (currentPosition + (distance * i / steps)) + ")");
                page.waitForTimeout(50 + random.nextInt(150));
            }
        }
        page.waitForTimeout(waitTime);
    }

    /**
     * 执行爬取任务
     */
    public String executeScrapingTask(boolean isHeadless, Integer page, String cityCode, String positionCode) {
        // 尝试从TOKEN_MAP中获取随机token
        Map<String, Object> tokenData = getRandomTokenFromMap();
        
        // 如果没有可用token，则获取新token，重试最多3次
        if (tokenData == null) {
            log.info("TOKEN_MAP中无可用token，使用token模式获取新token");
            tokenData = getTokenWithRetry(isHeadless, cityCode, positionCode, 3);
            if (tokenData == null) {
                throw new RuntimeException("获取token失败，无法爬取数据");
            }
        }
        
        // 获取当前使用的token值(用于在失败时移除)
        String currentToken = (String) tokenData.get("token");

        // 请求API
        String jsonData = requestApi(tokenData, page, cityCode, positionCode);
        if (jsonData != null) {
            // 检查访问异常并处理
            if (jsonData.contains("您的访问行为异常")) {
                // 移除失效的token
                if (currentToken != null) {
                    TOKEN_MAP.remove(currentToken);
                    log.warn("移除失效token，剩余token数量: {}", TOKEN_MAP.size());
                }
                
                if (!isHeadless) {
                    log.error("非无头模式仍检测到访问异常，等待5分钟");
                    try {
                        Thread.sleep(IP_BLOCK_WAIT_TIME);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    // 无头模式失败后，必须重新获取token尝试
                    Map<String, Object> newTokenData = getTokenWithRetry(false, cityCode, positionCode, 3);
                    if (newTokenData == null) {
                        throw new RuntimeException("获取新token失败，无法爬取数据");
                    }
                    return executeScrapingTask(false, page, cityCode, positionCode);
                }

                log.warn("检测到访问异常，切换到非无头模式重试");
                try {
                    Thread.sleep(IP_BLOCK_WAIT_TIME);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待被中断");
                }
                return executeScrapingTask(false, page, cityCode, positionCode);
            }

            try {
                objectMapper.readTree(jsonData); // 只验证JSON格式
                log.info("API响应成功，获取数据");
            } catch (Exception e) {
                log.error("JSON解析失败", e);
                // 移除失效的token
                if (currentToken != null) {
                    TOKEN_MAP.remove(currentToken);
                    log.warn("移除失效token(JSON解析失败)，剩余token数量: {}", TOKEN_MAP.size());
                }
                // JSON解析失败，重新获取token尝试
                Map<String, Object> newTokenData = getTokenWithRetry(isHeadless, cityCode, positionCode, 2);
                if (newTokenData != null) {
                    return requestApi(newTokenData, page, cityCode, positionCode);
                }
            }

            return jsonData;
        } else {
            // 请求失败，移除失效token
            if (currentToken != null) {
                TOKEN_MAP.remove(currentToken);
                log.warn("移除失效token(API请求失败)，剩余token数量: {}", TOKEN_MAP.size());
            }
            log.error("API请求失败，尝试重新获取token");
            
            // API请求失败，重新获取token尝试
            Map<String, Object> newTokenData = getTokenWithRetry(isHeadless, cityCode, positionCode, 2);
            if (newTokenData != null) {
                return requestApi(newTokenData, page, cityCode, positionCode);
            }
            
            return null;
        }
    }
    
    /**
     * 获取token并重试
     * @param isHeadless 是否无头模式
     * @param cityCode 城市代码
     * @param positionCode 职位代码
     * @param maxRetries 最大重试次数
     * @return token数据
     */
    private Map<String, Object> getTokenWithRetry(boolean isHeadless, String cityCode, String positionCode, int maxRetries) {
        Map<String, Object> tokenData = null;
        int retries = 0;
        
        while (tokenData == null && retries < maxRetries) {
            retries++;
            log.info("尝试获取token (第{}次尝试)", retries);
            tokenData = getToken(isHeadless, cityCode, positionCode);
            
            if (tokenData == null && retries < maxRetries) {
                log.warn("获取token失败，等待10秒后重试");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        return tokenData;
    }
    
    /**
     * 从TOKEN_MAP中随机获取一个token
     */
    private Map<String, Object> getRandomTokenFromMap() {
        if (TOKEN_MAP.isEmpty()) {
            return null;
        }
        
        // 随机选择一个token
        List<String> tokens = new ArrayList<>(TOKEN_MAP.keySet());
        if (tokens.isEmpty()) {
            return null;
        }
        
        String randomToken = tokens.get(random.nextInt(tokens.size()));
        Map<String, String> cookies = TOKEN_MAP.get(randomToken);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", randomToken);
        result.put("cookies", cookies);
        
        log.info("从TOKEN_MAP中随机选择token，当前token数量: {}", TOKEN_MAP.size());
        return result;
    }

    /**
     * 获取随机User-Agent
     */
    private String getRandomUserAgent() {
        return USER_AGENT_LIST[random.nextInt(USER_AGENT_LIST.length)];
    }

    /**
     * 创建带有必要cookie的BrowserContext
     */
    private BrowserContext createBrowserContext(Browser browser, Map<String, String> cookies,
            String token, String cityCode, String positionCode, Integer page) {
        // 创建浏览器上下文
        int width = 1100 + random.nextInt(400);
        int height = 800 + random.nextInt(400);
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(getRandomUserAgent())
                .setViewportSize(width, height)
                .setLocale("zh-CN")
                .setTimezoneId("Asia/Shanghai"));

        // 准备所有必要的cookie
        List<com.microsoft.playwright.options.Cookie> playwrightCookies = new ArrayList<>();

        // 如果有token，添加token cookie
        if (token != null && !token.isEmpty()) {
            com.microsoft.playwright.options.Cookie tokenCookie = new com.microsoft.playwright.options.Cookie(
                    "__zp_stoken__", token);
            tokenCookie.setDomain("www.zhipin.com");
            tokenCookie.setPath("/");
            tokenCookie.setSecure(true);
            playwrightCookies.add(tokenCookie);
        }

        // 添加必要的默认cookie
        Map<String, String> defaultCookies = new HashMap<>();
        defaultCookies.put("lastCity", cityCode);
        defaultCookies.put("__g", "-");
        defaultCookies.put("__c", String.valueOf(System.currentTimeMillis() / 1000));
        defaultCookies.put("__l", "l=/www.zhipin.com/web/geek/jobs?city=" +
                cityCode + "&position=" + positionCode + "&page=" + page + "&s=3&friend_source=0");

        // 合并提供的cookies和默认cookies
        Map<String, String> allCookies = new HashMap<>(defaultCookies);
        if (cookies != null) {
            allCookies.putAll(cookies);
        }

        // 添加所有cookies
        for (Map.Entry<String, String> entry : allCookies.entrySet()) {
            // 跳过已经添加的token cookie
            if ("__zp_stoken__".equals(entry.getKey()) && token != null) {
                continue;
            }
            com.microsoft.playwright.options.Cookie cookie = new com.microsoft.playwright.options.Cookie(
                    entry.getKey(), entry.getValue());
            cookie.setDomain("www.zhipin.com");
            cookie.setPath("/");
            cookie.setSecure(true);
            playwrightCookies.add(cookie);
        }

        context.addCookies(playwrightCookies);
        return context;
    }

    /**
     * 请求API获取数据
     */
    public String requestApi(Map<String, Object> tokenData, Integer page, String cityCode, String positionCode) {
        if (tokenData == null) {
            log.error("无有效token，无法请求API");
            return null;
        }

        String token = (String) tokenData.get("token");
        if (token == null || token.isEmpty()) {
            log.error("token为空，无法请求API");
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, String> cookies = (Map<String, String>) tokenData.get("cookies");
        String url = API_URL + "page=" + page + "&pageSize=" + PAGE_SIZE +
                "&city=" + cityCode + "&position=" + positionCode +
                "&expectInfo=" + "&query=" + "&multiSubway=" + "&multiBusinessDistrict=" +
                "&jobType=" + "&salary=" + "&experience=" + "&degree=" +
                "&industry=" + "&scale=" + "&stage=" +
                "&scene=" + (random.nextInt(9) + 1) +
                "&_=" + System.currentTimeMillis();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = createBrowserContext(browser, cookies, token, cityCode, positionCode, page);

            Page apiPage = context.newPage();
            try {
                apiPage.setDefaultTimeout(30000);
                apiPage.setDefaultNavigationTimeout(30000);

                // 设置API请求头
                Map<String, String> headers = new HashMap<>(DEFAULT_HEADERS);
                headers.put("Referer", "https://www.zhipin.com/web/geek/jobs?city=" + cityCode + "&position="
                        + positionCode + "&page=" + page);
                headers.put("traceId", "F-" + Long.toHexString(System.currentTimeMillis()).substring(0, 10));
                apiPage.setExtraHTTPHeaders(headers);

                apiPage.navigate(url);
                apiPage.waitForLoadState();
                randomWait(apiPage, 5000, 15000);

                // 从页面提取JSON数据
                String jsonResult = null;
                try {
                    jsonResult = apiPage.evaluate("() => {" +
                            "try {" +
                            "  const pre = document.querySelector('pre');" +
                            "  if (pre) return pre.textContent;" +
                            "  return document.body.textContent;" +
                            "} catch (e) {" +
                            "  return document.body.textContent;" +
                            "}" +
                            "}").toString();

                    // 验证JSON有效性
                    objectMapper.readTree(jsonResult);
                } catch (Exception e) {
                    log.warn("提取JSON失败: {}", e.getMessage());
                    try {
                        jsonResult = apiPage.content();
                    } catch (Exception ex) {
                        log.error("获取页面内容失败: {}", ex.getMessage());
                        return null;
                    }
                }

                // 检查异常访问
                if (jsonResult != null && jsonResult.contains("您的访问行为异常")) {
                    log.warn("API返回访问异常");
                }

                return jsonResult;
            } finally {
                apiPage.close();
                context.close();
                browser.close();
            }
        } catch (Exception e) {
            log.error("请求API失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 爬取职位列表JSON
     */
    public Map<String, List<String>> scrapeJobListJson(Integer page, String cityCode, String positionCode)
            throws JsonProcessingException {
        String jsonData = executeScrapingTask(true, page, cityCode, positionCode);
        if (jsonData == null) {
            return null;
        }
        return parseJobListJson(jsonData);
    }

    /**
     * 解析职位列表JSON
     */
    private Map<String, List<String>> parseJobListJson(String jsonData) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(jsonData);
        JsonNode jobsNode = jsonNode.get("zpData").get("jobList");

        if (jobsNode == null || jobsNode.isEmpty()) {
            throw new RuntimeException("未获取到职位列表数据");
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("urls", new ArrayList<>());
        result.put("json", new ArrayList<>());

        for (JsonNode jobNode : jobsNode) {
            String url = BASE_URL_JOB.replace("{}", jobNode.get("encryptJobId").asText());
            result.get("urls").add(url);
            result.get("json").add(jobNode.toString());
        }

        return result;
    }
}
