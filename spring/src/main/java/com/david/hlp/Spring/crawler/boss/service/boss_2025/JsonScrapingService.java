package com.david.hlp.Spring.crawler.boss.service.boss_2025;

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
// 使用完全限定名称以避免冲突
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.nio.file.Paths;
import com.fasterxml.jackson.core.JsonProcessingException;
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

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(getRandomUserAgent())
                    .setViewportSize(1280, 1024)
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

                // 检查验证码页面
                if (page.url().contains("verify")) {
                    handleVerificationPage(page, cityCode, positionCode);
                }

                // 等待页面稳定
                log.info("等待页面稳定...");
                page.waitForTimeout(5000);

                // 检查页面URL，确保在正确的页面上
                log.info("当前页面URL: {}", page.url());

                // 模拟用户滚动
                simulateUserScrolling(page);

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

        // 滑动1：缓慢滚动到300
        scrollToPosition(page, 300, 2000);

        // 滑动2：继续滚动到600
        scrollToPosition(page, 600, 1500);

        // 滑动3：滚动到900
        scrollToPosition(page, 900, 2500);

        // 滑动4：滚动到1200
        scrollToPosition(page, 1200, 1800);

        // 滑动5：滚动到1500
        scrollToPosition(page, 1500, 2200);

        // 滑动6：回到中间位置
        scrollToPosition(page, 800, 3000);
    }

    /**
     * 滚动到指定位置并等待
     * @param page Playwright页面对象
     * @param position 滚动位置
     * @param waitTime 等待时间(毫秒)
     */
    private void scrollToPosition(Page page, int position, int waitTime) {
        log.info("滑动到{}px位置", position);
        page.evaluate("window.scrollTo({top: " + position + ", behavior: 'smooth'})");
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
     * 使用请求API
     * @param tokenData 包含token和cookies的数据
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
        log.info("正在请求API: {}", url);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = createHttpRequest(url, token, cookies);

            return executeHttpRequest(httpClient, httpGet);
        } catch (Exception e) {
            log.error("API请求失败: {}", e.getMessage(), e);
            return null;
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
        return API_URL + "page=" + page + "&pageSize=" + PAGE_SIZE + 
               "&city=" + cityCode + "&position=" + positionCode;
    }

    /**
     * 创建HTTP请求对象
     * @param url 请求URL
     * @param token API Token
     * @param cookies Cookies
     * @return 配置好的HttpGet对象
     */
    private HttpGet createHttpRequest(String url, String token, Map<String, String> cookies) {
        HttpGet httpGet = new HttpGet(url);

        // 设置请求头
        httpGet.addHeader("Accept", "application/json");
        httpGet.addHeader("User-Agent", getRandomUserAgent());
        httpGet.addHeader("Referer", "https://www.zhipin.com/web/geek/jobs");

        // 添加所有cookies到请求
        httpGet.addHeader("Cookie", buildCookieString(token, cookies));

        return httpGet;
    }

    /**
     * 构建Cookie字符串
     * @param token API Token
     * @param cookies Cookies Map
     * @return 格式化的Cookie字符串
     */
    private String buildCookieString(String token, Map<String, String> cookies) {
        StringBuilder cookiesStr = new StringBuilder();
        cookiesStr.append("__zp_stoken__=").append(token);

        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            cookiesStr.append("; ").append(entry.getKey()).append("=").append(entry.getValue());
        }

        return cookiesStr.toString();
    }

    /**
     * 执行HTTP请求
     * @param httpClient HTTP客户端
     * @param httpGet HTTP请求对象
     * @return 响应JSON字符串
     */
    private String executeHttpRequest(CloseableHttpClient httpClient, HttpGet httpGet) throws Exception {
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                log.error("API请求失败: HTTP状态码 {}", response.getStatusLine().getStatusCode());
                return null;
            }

            HttpEntity entity = response.getEntity();
            String jsonResult = EntityUtils.toString(entity, "UTF-8");

            // 记录响应数据，便于排查问题
            log.debug("API响应内容: {}", jsonResult);

            // 检查是否有访问异常
            if (jsonResult.contains("您的访问行为异常")) {
                log.warn("API返回访问行为异常信息");
            }

            return jsonResult;
        }
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

        if (jobsNode == null) {
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
            log.info("API响应结果:\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode).substring(0, 50));
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
