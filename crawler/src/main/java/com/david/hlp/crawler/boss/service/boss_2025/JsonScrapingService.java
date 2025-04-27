package com.david.hlp.crawler.boss.service.boss_2025;

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
import java.nio.file.Paths;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Arrays;
import java.util.Random;
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonScrapingService {

    private static final String TARGET_URL = "https://www.zhipin.com/web/geek/jobs?";
    private static final String API_URL = "https://www.zhipin.com/wapi/zpgeek/search/joblist.json?";
    private static final Integer PAGE_SIZE = 30;
    private static final String BASE_URL_JOB = "https://www.zhipin.com/job_detail/{}.html";

    private static final String[] USER_AGENT_LIST = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 OPR/106.0.0.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    };

    private final Random random = new Random();

    /**
     * 获取访问API所需的token
     * @param isHeadless 是否使用无头模式
     * @param cityCode 城市代码
     * @param positionCode 职位代码
     * @return 包含token和cookies的Map，如果获取失败则返回null
     */
    public Map<String, Object> getToken(boolean isHeadless, String cityCode, String positionCode) {
        log.info("正在访问Boss直聘获取token...(无头模式: {})", isHeadless);
        String targetUrl = TARGET_URL + "city=" + cityCode + "&position=" + positionCode;
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(isHeadless));

            // 随机设置浏览器窗口大小
            int width = 1100 + (int)(Math.random() * 400); // 1100-1500
            int height = 800 + (int)(Math.random() * 400); // 800-1200

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(getRandomUserAgent())
                    .setViewportSize(width, height)
                    .setLocale("zh-CN")
                    .setTimezoneId("Asia/Shanghai"));

            Page page = context.newPage();
            String token = "";

            try {
                // 设置更长的超时时间
                page.setDefaultTimeout(60000);
                page.setDefaultNavigationTimeout(60000);

                // 导航到目标URL
                log.info("正在导航到目标页面...");
                page.navigate(targetUrl);

                // 等待页面加载完成
                log.info("等待页面加载...");
                page.waitForLoadState();

                // 随机等待一段时间，模拟用户阅读页面
                randomWait(page, 2000, 5000);

                // 检查验证码页面
                if (page.url().contains("verify")) {
                    handleVerificationPage(page, cityCode, positionCode);
                }

                // 等待页面稳定
                log.info("等待页面稳定...");
                randomWait(page, 3000, 7000);

                // 检查页面URL，确保在正确的页面上
                log.info("当前页面URL: {}", page.url());

                // 模拟用户滚动前的随机操作
                if (Math.random() < 0.7) {
                    performRandomUserAction(page);
                }

                // 模拟用户滚动
                simulateUserScrolling(page);

                // 滚动后再执行一些随机操作
                if (Math.random() < 0.8) {
                    performRandomUserAction(page);
                    randomWait(page, 1000, 4000);
                }

                // 使用waitForFunction确保cookie已设置
                log.info("检查cookie是否已设置...");
                try {
                    page.waitForFunction("document.cookie.includes('__zp_stoken__')", null, new Page.WaitForFunctionOptions().setTimeout(10000));
                    log.info("cookie已设置，包含token");
                } catch (Exception e) {
                    log.warn("等待token设置超时，尝试继续获取");
                }

                // 获取Token
                token = getTokenFromPage(page, context);

                if (token == null || token.isEmpty()) {
                    log.error("未获取到token");
                    saveErrorScreenshot(page, "no_token");
                    throw new Exception("无法获取 __zp_stoken__，可能 Cookie 未设置");
                }

                log.info("成功获取token");

                // 获取所有cookies
                Map<String, String> cookieMap = getAllCookies(context);

                Map<String, Object> result = new HashMap<>();
                result.put("token", token);
                result.put("cookies", cookieMap);
                return result;

            } catch (Exception e) {
                log.error("获取token时发生错误：{}", e.getMessage(), e);
                saveErrorScreenshot(page, "error");
                return null;
            } finally {
                browser.close();
            }
        }
    }

    /**
     * 处理验证码页面
     * @param page Playwright页面对象
     */
    private void handleVerificationPage(Page page, String cityCode, String positionCode) {
        log.warn("需要手动处理验证码！");
        log.info("等待30秒供人工处理验证码...");
        page.waitForTimeout(30000);

        // 重新导航并等待
        page.navigate(TARGET_URL + "city=" + cityCode + "&position=" + positionCode);
        page.waitForLoadState();
    }

    /**
     * 模拟用户滚动行为
     * @param page Playwright页面对象
     */
    private void simulateUserScrolling(Page page) {
        log.info("模拟用户滚动行为...");

        // 随机等待一段时间后再开始滚动
        randomWait(page, 1000, 3000);

        // 随机决定滚动次数 (3-8次)
        int scrollTimes = 3 + (int)(Math.random() * 6);
        log.info("将进行{}次随机滚动", scrollTimes);

        // 随机滚动位置列表
        List<Integer> scrollPositions = new ArrayList<>();
        for (int i = 0; i < scrollTimes; i++) {
            // 生成递增但有随机性的滚动位置
            int basePosition = 300 * (i + 1);
            int randomOffset = (int)(Math.random() * 200) - 100; // -100到+100的随机偏移
            scrollPositions.add(Math.max(100, basePosition + randomOffset));
        }

        // 执行滚动并在每次滚动间执行随机用户操作
        for (int i = 0; i < scrollPositions.size(); i++) {
            int position = scrollPositions.get(i);

            // 滚动到随机位置
            scrollToPosition(page, position, 1500 + (int)(Math.random() * 1500));

            // 随机执行额外操作
            if (Math.random() < 0.7) {
                performRandomUserAction(page);
            }
        }

        // 最后随机回到某个位置
        int finalPosition = scrollPositions.get((int)(Math.random() * scrollPositions.size()));
        scrollToPosition(page, finalPosition / 2, 2000 + (int)(Math.random() * 2000));
    }

    /**
     * 执行随机用户操作
     * @param page Playwright页面对象
     */
    private void performRandomUserAction(Page page) {
        double actionRandom = Math.random();
        try {
            if (actionRandom < 0.3) {
                // 随机鼠标移动
                log.debug("执行随机鼠标移动");
                randomMouseMovement(page);
            } else if (actionRandom < 0.5) {
                // 随机暂停
                log.debug("执行随机暂停");
                randomWait(page, 1000, 5000);
            } else if (actionRandom < 0.7) {
                // 随机点击某个元素后返回
                log.debug("尝试随机点击链接");
                attemptRandomElementClick(page);
            } else if (actionRandom < 0.8) {
                // 随机调整窗口大小
                log.debug("随机调整窗口大小");
                randomWindowResize(page);
            } else {
                // 模拟查看详情
                log.debug("模拟查看职位详情");
                simulateJobDetailView(page);
            }
        } catch (Exception e) {
            log.warn("执行随机用户操作时出错: {}", e.getMessage());
        }
    }

    /**
     * 随机鼠标移动
     * @param page Playwright页面对象
     */
    private void randomMouseMovement(Page page) {
        try {
            // 获取页面尺寸
            int pageWidth = (int)page.evaluate("window.innerWidth");
            int pageHeight = (int)page.evaluate("window.innerHeight");
            
            // 执行3-6次随机鼠标移动
            int movements = 3 + (int)(Math.random() * 4);
            for (int i = 0; i < movements; i++) {
                int x = (int)(Math.random() * pageWidth);
                int y = (int)(Math.random() * pageHeight);
                
                // 使用鼠标移动到随机位置
                page.mouse().move(x, y);
                
                // 随机小暂停
                page.waitForTimeout(200 + (int)(Math.random() * 800));
            }
        } catch (Exception e) {
            log.warn("随机鼠标移动失败: {}", e.getMessage());
        }
    }

    /**
     * 随机等待
     * @param page Playwright页面对象
     * @param minMs 最小等待时间(毫秒)
     * @param maxMs 最大等待时间(毫秒)
     */
    private void randomWait(Page page, int minMs, int maxMs) {
        int waitTime = minMs + (int)(Math.random() * (maxMs - minMs));
        page.waitForTimeout(waitTime);
    }

    /**
     * 尝试随机点击元素
     * @param page Playwright页面对象
     */
    private void attemptRandomElementClick(Page page) {
        try {
            // 尝试获取所有可能的链接元素
            List<String> selectors = Arrays.asList(
                ".job-card", 
                ".filter-select", 
                ".dorpdown-menu",
                ".search-box",
                ".city-sel",
                ".search-condition-wrapper a"
            );
            
            // 随机选择一个选择器
            String selector = selectors.get((int)(Math.random() * selectors.size()));
            
            // 检查元素是否存在
            boolean hasElement = (boolean)page.evaluate("!!document.querySelector('" + selector + "')");
            
            if (hasElement) {
                // 获取元素数量
                int count = (int)page.evaluate("document.querySelectorAll('" + selector + "').length");
                
                if (count > 0) {
                    // 随机选择一个元素索引
                    int index = (int)(Math.random() * Math.min(count, 5)); // 限制在前5个元素中选择
                    
                    // 点击前保存当前URL
                    String currentUrl = page.url();
                    
                    // 滚动到元素并点击
                    page.evaluate("document.querySelectorAll('" + selector + "')[" + index + "].scrollIntoView({behavior: 'smooth', block: 'center'})");
                    randomWait(page, 500, 1500);
                    page.evaluate("document.querySelectorAll('" + selector + "')[" + index + "].click()");
                    
                    // 等待新页面可能加载
                    randomWait(page, 2000, 5000);
                    
                    // 如果URL改变，返回原页面
                    if (!page.url().equals(currentUrl)) {
                        log.info("URL已改变，返回原页面");
                        page.goBack();
                        page.waitForLoadState();
                        randomWait(page, 1000, 3000);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("尝试随机点击元素失败: {}", e.getMessage());
        }
    }

    /**
     * 随机调整窗口大小
     * @param page Playwright页面对象
     */
    private void randomWindowResize(Page page) {
        try {
            // 获取当前窗口大小
            int currentWidth = (int)page.evaluate("window.innerWidth");
            int currentHeight = (int)page.evaluate("window.innerHeight");
            
            // 计算新的窗口大小 (变化范围±10%)
            int widthDelta = (int)(currentWidth * 0.1);
            int heightDelta = (int)(currentHeight * 0.1);
            
            int newWidth = currentWidth + (int)(Math.random() * widthDelta * 2) - widthDelta;
            int newHeight = currentHeight + (int)(Math.random() * heightDelta * 2) - heightDelta;
            
            // 确保窗口尺寸在合理范围内
            newWidth = Math.max(800, Math.min(1600, newWidth));
            newHeight = Math.max(600, Math.min(1200, newHeight));
            
            // 设置新窗口大小
            page.setViewportSize(newWidth, newHeight);
            
            // 等待一段时间
            randomWait(page, 1000, 3000);
            
            // 有50%的概率恢复原窗口大小
            if (Math.random() < 0.5) {
                page.setViewportSize(currentWidth, currentHeight);
            }
        } catch (Exception e) {
            log.warn("随机调整窗口大小失败: {}", e.getMessage());
        }
    }

    /**
     * 模拟查看职位详情
     * @param page Playwright页面对象
     */
    private void simulateJobDetailView(Page page) {
        try {
            // 尝试找到职位卡片
            boolean hasJobCards = (boolean)page.evaluate("!!document.querySelector('.job-card')");
            
            if (hasJobCards) {
                // 获取职位卡片数量
                int count = (int)page.evaluate("document.querySelectorAll('.job-card').length");
                
                if (count > 0) {
                    // 随机选择一个职位卡片
                    int index = (int)(Math.random() * Math.min(count, 10)); // 限制在前10个卡片中选择
                    
                    // 滚动到职位卡片
                    page.evaluate("document.querySelectorAll('.job-card')[" + index + "].scrollIntoView({behavior: 'smooth', block: 'center'})");
                    randomWait(page, 800, 2000);
                    
                    // 鼠标悬停在卡片上
                    page.evaluate("document.querySelectorAll('.job-card')[" + index + "].dispatchEvent(new MouseEvent('mouseover', {bubbles: true}))");
                    randomWait(page, 500, 1500);
                    
                    // 模拟查看详情（不实际点击，避免页面跳转）
                    // 可以通过滚动模拟阅读行为
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
                        "}, 1600);"
                    );
                    
                    randomWait(page, 2000, 4000);
                }
            }
        } catch (Exception e) {
            log.warn("模拟查看职位详情失败: {}", e.getMessage());
        }
    }

    /**
     * 滚动到指定位置并等待
     * @param page Playwright页面对象
     * @param position 滚动位置
     * @param waitTime 等待时间(毫秒)
     */
    private void scrollToPosition(Page page, int position, int waitTime) {
        log.debug("滑动到{}px位置", position);
        
        // 使用更自然的滚动方式，有时使用平滑滚动，有时使用分段滚动
        if (Math.random() < 0.7) {
            // 平滑滚动
            page.evaluate("window.scrollTo({top: " + position + ", behavior: 'smooth'})");
        } else {
            // 分段滚动，更像人类
            int currentPosition = (int)page.evaluate("window.scrollY");
            int distance = position - currentPosition;
            int steps = 5 + (int)(Math.random() * 10); // 5-15步
            
            for (int i = 1; i <= steps; i++) {
                int stepPosition = currentPosition + (distance * i / steps);
                page.evaluate("window.scrollTo(0, " + stepPosition + ")");
                page.waitForTimeout(50 + (int)(Math.random() * 150)); // 短暂停顿
            }
        }
        
        page.waitForTimeout(waitTime);
    }

    /**
     * 从页面或浏览器上下文中获取token
     * @param page Playwright页面对象
     * @param context Playwright浏览器上下文
     * @return 获取到的token
     */
    private String getTokenFromPage(Page page, BrowserContext context) {
        String token = "";
        // 获取Token - 使用更安全的方式
        log.info("获取token...");
        try {
            token = page.evaluate("() => { " +
                    "const cookie = document.cookie; " +
                    "const match = cookie.match(/__zp_stoken__=([^;]+)/); " +
                    "return match ? decodeURIComponent(match[1]) : ''; " +
                    "}").toString();
        } catch (Exception e) {
            log.error("获取token时JS执行错误: {}", e.getMessage());
        }

        // 确保token非空
        if (token == null || token.isEmpty()) {
            log.info("通过cookies列表获取token...");
            // 获取所有cookies
            List<com.microsoft.playwright.options.Cookie> cookies = context.cookies();

            // 从cookies列表中查找token
            for (com.microsoft.playwright.options.Cookie cookie : cookies) {
                if ("__zp_stoken__".equals(cookie.name)) {
                    token = cookie.value;
                    log.info("从cookies列表中获取到token");
                    break;
                }
            }
        }

        return token;
    }

    /**
     * 获取所有cookies并转换为Map
     * @param context Playwright浏览器上下文
     * @return Cookie Map
     */
    private Map<String, String> getAllCookies(BrowserContext context) {
        List<com.microsoft.playwright.options.Cookie> cookies = context.cookies();
        Map<String, String> cookieMap = new HashMap<>();
        for (com.microsoft.playwright.options.Cookie cookie : cookies) {
            cookieMap.put(cookie.name, cookie.value);
            log.debug("Cookie: {}={}", cookie.name, cookie.value);
        }
        return cookieMap;
    }

    /**
     * 保存错误截图
     * @param page Playwright页面对象
     * @param prefix 截图文件名前缀
     */
    private void saveErrorScreenshot(Page page, String prefix) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        try {
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(prefix + "_" + timestamp + ".png")));
        } catch (Exception se) {
            log.error("截图保存失败", se);
        }
    }

    /**
     * 使用Playwright请求API获取数据
     * @param tokenData 包含token和cookies的数据
     * @param page 页码
     * @param cityCode 城市代码
     * @param positionCode 职位代码
     * @return API返回的JSON数据
     */
    public String requestApi(Map<String, Object> tokenData, Integer page, String cityCode, String positionCode) {
        if (tokenData == null) {
            log.error("未获取到有效token，无法请求API");
            return null;
        }

        String token = (String) tokenData.get("token");
        @SuppressWarnings("unchecked")
        Map<String, String> cookies = (Map<String, String>) tokenData.get("cookies");
        String url = buildApiUrl(page, cityCode, positionCode);
        log.info("正在使用Playwright请求API: {}", url);

        try (Playwright playwright = Playwright.create()) {
            // 使用无头模式启动浏览器，API请求不需要UI
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true));
            
            // 创建新的浏览器上下文
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(getRandomUserAgent())
                    // 设置较小的视口，因为这只是API请求
                    .setViewportSize(1024, 768)
                    .setLocale("zh-CN")
                    .setTimezoneId("Asia/Shanghai"));
            
            // 添加所有cookies
            addCookiesToContext(context, cookies, token);
            
            // 创建新页面
            Page apiPage = context.newPage();
            
            try {
                // 设置请求超时时间
                apiPage.setDefaultTimeout(30000);
                apiPage.setDefaultNavigationTimeout(30000);
                
                // 添加请求头
                apiPage.setExtraHTTPHeaders(createApiHeaders());
                
                // 发起请求
                log.info("正在请求API: {}", url);
                apiPage.navigate(url);
                
                // 等待页面加载
                apiPage.waitForLoadState();
                
                // 随机等待一段时间
                randomWait(apiPage, 5000, 15000);
                
                // 从页面中提取JSON数据
                String jsonResult = extractJsonFromPage(apiPage);
                
                // 检查是否有访问异常
                if (jsonResult != null && jsonResult.contains("您的访问行为异常")) {
                    log.warn("API返回访问行为异常信息");
                }
                
                return jsonResult;
            } finally {
                apiPage.close();
                context.close();
                browser.close();
            }
        } catch (Exception e) {
            log.error("使用Playwright请求API失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 向浏览器上下文添加cookies
     * @param context 浏览器上下文
     * @param cookies cookies映射
     * @param token API token
     */
    private void addCookiesToContext(BrowserContext context, Map<String, String> cookies, String token) {
        List<com.microsoft.playwright.options.Cookie> playwrightCookies = new ArrayList<>();
        
        // 添加token cookie
        playwrightCookies.add(createCookie("__zp_stoken__", token));
        
        // 添加其他cookies
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            playwrightCookies.add(createCookie(entry.getKey(), entry.getValue()));
        }
        
        // 将cookies添加到上下文
        context.addCookies(playwrightCookies);
    }
    
    /**
     * 创建Playwright cookie对象
     * @param name cookie名称
     * @param value cookie值
     * @return Playwright cookie对象
     */
    private com.microsoft.playwright.options.Cookie createCookie(String name, String value) {
        com.microsoft.playwright.options.Cookie cookie = new com.microsoft.playwright.options.Cookie(name, value);
        cookie.setDomain("www.zhipin.com");
        cookie.setPath("/");
        cookie.setSecure(true);
        return cookie;
    }
    
    /**
     * 创建API请求头
     * @return 请求头映射
     */
    private Map<String, String> createApiHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Referer", "https://www.zhipin.com/web/geek/jobs");
        headers.put("Sec-Fetch-Dest", "empty");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Site", "same-origin");
        headers.put("Cache-Control", "no-cache");
        headers.put("Pragma", "no-cache");
        return headers;
    }
    
    /**
     * 从页面中提取JSON数据
     * @param page Playwright页面对象
     * @return 提取的JSON字符串
     */
    private String extractJsonFromPage(Page page) {
        try {
            // 首先尝试获取预渲染的JSON
            String json = page.evaluate("() => {" +
                "try {" +
                "  const pre = document.querySelector('pre');" +
                "  if (pre) return pre.textContent;" +
                "  return document.body.textContent;" +
                "} catch (e) {" +
                "  return document.body.textContent;" +
                "}" +
            "}").toString();
            
            // 验证是否是有效的JSON
            new ObjectMapper().readTree(json);
            return json;
        } catch (Exception e) {
            log.warn("无法从页面提取有效JSON: {}", e.getMessage());
            
            // 回退方案：获取整个页面内容
            try {
                return page.content();
            } catch (Exception ex) {
                log.error("无法获取页面内容: {}", ex.getMessage());
                return null;
            }
        }
    }

    /**
     * 构建API URL
     * @param page 页码
     * @param cityCode 城市代码
     * @param positionCode 职位代码
     * @return 完整的API URL
     */
    private String buildApiUrl(Integer page, String cityCode, String positionCode) {
        // 添加随机 scene 和 时间戳 
        int randomScene = random.nextInt(10) + 1; // 生成 1 到 10 之间的随机数
        long timestamp = System.currentTimeMillis();
        return API_URL + "page=" + page + "&pageSize=" + PAGE_SIZE +
               "&city=" + cityCode + "&position=" + positionCode + "&scene=" + randomScene + "&_=" + timestamp;
    }

    /**
     * 执行爬取任务的主方法
     */
    public Map<String, List<String>> scrapeJobListJson(Integer page, String cityCode, String positionCode) throws JsonProcessingException {
        String jsonData = executeScrapingTask(true, page, cityCode, positionCode);
        if (jsonData == null) {
            return null;
        }

        return parseJobListJson(jsonData);
    }

    /**
     * 解析职位列表JSON数据
     * @param jsonData JSON响应数据
     * @return 解析结果Map
     */
    private Map<String, List<String>> parseJobListJson(String jsonData) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonData);
        JsonNode jobsNode = jsonNode.get("zpData").get("jobList");

        if (jobsNode == null || jobsNode.isEmpty()) {
            log.error("未能获取到职位列表数据");
            throw new RuntimeException("未能获取到职位列表数据");
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

    /**
     * 执行爬取任务的主方法
     * @param isHeadless 是否使用无头模式
     */
    public String executeScrapingTask(boolean isHeadless, Integer page, String cityCode, String positionCode) {
        // 获取token
        Map<String, Object> tokenData = getToken(isHeadless,cityCode, positionCode);
        if (tokenData == null) {
            log.error("获取token失败，任务中止");
            throw new RuntimeException("获取token失败，任务中止");
        }

        // 使用token请求API
        String jsonData = requestApi(tokenData, page, cityCode, positionCode);
        if (jsonData != null) {
            log.info("成功获取API数据");

            // 检查是否返回访问异常
            if (jsonData.contains("您的访问行为异常")) {
                log.warn("检测到访问行为异常，切换到非无头模式重试");
                // 如果当前已经是非无头模式，则不再重试，避免无限循环
                if (!isHeadless) {
                    log.error("即使在非无头模式下仍然检测到访问行为异常，任务终止，等待5分钟");
                    return null;
                }

                log.info("等待5分钟结束，继续执行");
                try {
                    Thread.sleep(1000 * 60 * 5);
                } catch (InterruptedException e) {
                    log.error("等待期间发生中断", e);
                    throw new RuntimeException("等待期间发生中断");
                }

                // 使用非无头模式重新执行
                String jsonData2 = executeScrapingTask(false, page, cityCode, positionCode);
                return jsonData2;
            }

            // 记录API响应结果
            logJsonResponse(jsonData);

            return jsonData;
        } else {
            log.error("未能获取API数据");
            return null;
        }
    }

    /**
     * 记录JSON响应
     * @param jsonData JSON响应数据
     */
    private void logJsonResponse(String jsonData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonData);
            log.info("API响应结果:\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode).substring(0, 75));
        } catch (Exception e) {
            log.error("JSON解析失败", e);
        }
    }

    /**
     * 获取随机的User-Agent
     * @return 随机的User-Agent字符串
     */
    private String getRandomUserAgent() {
        int index = (int) (Math.random() * USER_AGENT_LIST.length);
        return USER_AGENT_LIST[index];
    }
}
