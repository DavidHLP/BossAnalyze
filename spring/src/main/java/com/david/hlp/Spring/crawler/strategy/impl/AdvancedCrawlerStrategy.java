package com.david.hlp.Spring.crawler.strategy.impl;

import org.springframework.stereotype.Component;

import com.david.hlp.Spring.crawler.strategy.CrawlerStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.time.LocalDateTime;

/**
 * 高级爬取策略实现
 * 提供更复杂的反爬措施，包括动态Cookie管理、IP轮换策略和浏览器指纹模拟
 */
@Component
public class AdvancedCrawlerStrategy implements CrawlerStrategy {

    /**
     * 随机数生成器
     */
    private final Random random = new Random();

    /**
     * 用户代理列表
     */
    private final List<String> userAgents = new ArrayList<>();

    /**
     * 浏览器版本列表
     */
    private final List<String> browserVersions = new ArrayList<>();

    /**
     * 最小等待时间（毫秒）
     */
    private static final long MIN_WAIT_TIME = 5000;

    /**
     * 最大等待时间（毫秒）
     */
    private static final long MAX_WAIT_TIME = 20000;

    /**
     * 最小请求间隔（毫秒）
     */
    private static final long MIN_REQUEST_INTERVAL = 10000;

    /**
     * 最大请求间隔（毫秒）
     */
    private static final long MAX_REQUEST_INTERVAL = 30000;
    
    /**
     * 代理切换阈值（连续请求次数）
     */
    private static final int PROXY_SWITCH_THRESHOLD = 4;
    
    /**
     * 浏览器指纹参数
     */
    private static final String[] PLATFORM_NAMES = {"Win32", "MacIntel", "Linux x86_64"};
    private static final int[] SCREEN_WIDTHS = {1366, 1440, 1536, 1920, 2560};
    private static final int[] SCREEN_HEIGHTS = {768, 900, 864, 1080, 1440};
    private static final String[] TIMEZONES = {"Asia/Shanghai", "Asia/Hong_Kong", "Asia/Tokyo", "Europe/London", "America/New_York"};
    
    /**
     * 构造函数，初始化用户代理和浏览器版本列表
     */
    public AdvancedCrawlerStrategy() {
        initUserAgents();
        initBrowserVersions();
    }
    
    /**
     * 初始化用户代理列表
     */
    private void initUserAgents() {
        // 主流浏览器的User-Agent
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:90.0) Gecko/20100101 Firefox/90.0");
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) Gecko/20100101 Firefox/91.0");
        userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Safari/605.1.15");
        userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        userAgents.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        // 移动设备的User-Agent
        userAgents.add("Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1");
        userAgents.add("Mozilla/5.0 (iPad; CPU OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1");
        userAgents.add("Mozilla/5.0 (Linux; Android 11; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");
    }
    
    /**
     * 初始化浏览器版本列表
     */
    private void initBrowserVersions() {
        // Chrome版本
        browserVersions.add("90.0.4430.212");
        browserVersions.add("91.0.4472.124");
        browserVersions.add("92.0.4515.107");
        browserVersions.add("93.0.4577.63");
        browserVersions.add("94.0.4606.81");
        
        // Firefox版本
        browserVersions.add("89.0");
        browserVersions.add("90.0");
        browserVersions.add("91.0");
        browserVersions.add("92.0");
        browserVersions.add("93.0");
        
        // Safari版本
        browserVersions.add("14.1");
        browserVersions.add("14.1.1");
        browserVersions.add("14.1.2");
        browserVersions.add("15.0");
    }
    
    @Override
    public long getRandomWaitTime() {
        // 使用正态分布生成更自然的等待时间
        double gaussian = random.nextGaussian() * 0.5 + 0.5; // 大部分值在0-1之间
        gaussian = Math.max(0, Math.min(1, gaussian)); // 确保在0-1之间
        
        long waitTime = MIN_WAIT_TIME + (long)(gaussian * (MAX_WAIT_TIME - MIN_WAIT_TIME));
        
        // 有10%的几率增加额外等待时间，模拟用户暂时离开
        if (random.nextDouble() < 0.1) {
            waitTime += random.nextInt(60000); // 额外增加最多1分钟
        }
        
        return waitTime;
    }
    
    @Override
    public String getRandomUserAgent() {
        // 从用户代理列表中随机选择一个基础UA
        String baseUA = userAgents.get(random.nextInt(userAgents.size()));
        
        // 有50%的概率直接返回基础UA
        if (random.nextBoolean()) {
            return baseUA;
        }
        
        // 对于另外50%，动态生成一个更随机的UA
        if (baseUA.contains("Chrome")) {
            // 为Chrome生成一个随机版本
            String chromeVersion = browserVersions.get(random.nextInt(5)); // 前5个是Chrome版本
            return baseUA.replaceAll("Chrome/\\d+\\.\\d+\\.\\d+\\.\\d+", "Chrome/" + chromeVersion);
        } else if (baseUA.contains("Firefox")) {
            // 为Firefox生成一个随机版本
            String firefoxVersion = browserVersions.get(5 + random.nextInt(5)); // 中间5个是Firefox版本
            return baseUA.replaceAll("Firefox/\\d+\\.\\d+", "Firefox/" + firefoxVersion);
        } else if (baseUA.contains("Safari") && !baseUA.contains("Chrome")) {
            // 为Safari生成一个随机版本
            String safariVersion = browserVersions.get(10 + random.nextInt(4)); // 最后4个是Safari版本
            return baseUA.replaceAll("Version/\\d+\\.\\d+(\\.\\d+)?", "Version/" + safariVersion);
        }
        
        return baseUA;
    }
    
    @Override
    public boolean shouldSwitchProxy(int consecutiveRequests) {
        // 基于请求次数和随机因素的复杂切换策略
        
        // 超过阈值，必须切换
        if (consecutiveRequests >= PROXY_SWITCH_THRESHOLD) {
            return true;
        }
        
        // 请求次数越多，切换概率越高
        double switchProbability = 0.1 + (consecutiveRequests * 0.05);
        
        // 在一天中的不同时间段调整切换概率（模拟正常用户行为）
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        // 工作时间段（9-17点）减少切换频率
        if (hour >= 9 && hour <= 17) {
            switchProbability *= 0.8;
        } 
        // 夜间时段（22-6点）增加切换频率
        else if (hour >= 22 || hour <= 6) {
            switchProbability *= 1.2;
        }
        
        return random.nextDouble() < switchProbability;
    }
    
    @Override
    public long getRequestInterval() {
        // 使用更自然的正态分布生成请求间隔
        double gaussian = random.nextGaussian() * 0.5 + 0.5;
        gaussian = Math.max(0, Math.min(1, gaussian));
        
        return MIN_REQUEST_INTERVAL + (long)(gaussian * (MAX_REQUEST_INTERVAL - MIN_REQUEST_INTERVAL));
    }
    
    @Override
    public Map<String, String> getRandomHeaders() {
        Map<String, String> headers = new HashMap<>();
        
        // 添加随机User-Agent
        String userAgent = getRandomUserAgent();
        headers.put("User-Agent", userAgent);
        
        // 通用请求头
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        
        // 总是添加的随机变量请求头
        headers.put("Accept-Encoding", random.nextBoolean() ? "gzip, deflate, br" : "gzip, deflate");
        
        // 随机添加DNT（Do Not Track）
        if (random.nextBoolean()) {
            headers.put("DNT", "1");
        }
        
        // 随机添加Cache-Control
        String[] cacheControls = {
            "no-cache", 
            "max-age=0", 
            "no-store, must-revalidate",
            "no-store, no-cache, must-revalidate, post-check=0, pre-check=0"
        };
        headers.put("Cache-Control", cacheControls[random.nextInt(cacheControls.length)]);
        
        // 随机添加引用来源
        if (random.nextBoolean()) {
            String[] referers = {
                "https://www.google.com/search?q=招聘信息",
                "https://cn.bing.com/search?q=招聘+北京",
                "https://www.baidu.com/s?wd=招聘信息+上海",
                "https://www.zhipin.com/web/geek/job-recommend",
                "https://www.liepin.com/zhaopin/?industries=000",
                "https://www.lagou.com/wn/jobs?labelWords=&fromSearch=true&suginput="
            };
            headers.put("Referer", referers[random.nextInt(referers.length)]);
        }
        
        // 生成模拟真实浏览器的Cookie
        StringBuilder cookieBuilder = new StringBuilder();
        
        // 会话ID
        String sessionId = generateRandomId(16);
        cookieBuilder.append("JSESSIONID=").append(sessionId).append("; ");
        
        // Google Analytics Cookie
        long currentTime = System.currentTimeMillis() / 1000;
        long gaCreateTime = currentTime - random.nextInt(10000000);
        long gidCreateTime = currentTime - random.nextInt(100000);
        cookieBuilder.append("_ga=GA1.2.").append(random.nextInt(2000000000)).append(".").append(gaCreateTime).append("; ");
        cookieBuilder.append("_gid=GA1.2.").append(random.nextInt(2000000000)).append(".").append(gidCreateTime).append("; ");
        
        // 本地存储的访问信息
        cookieBuilder.append("Hm_lvt_").append(generateRandomId(10)).append("=").append(currentTime - 86400 * random.nextInt(7)).append("; ");
        cookieBuilder.append("Hm_lpvt_").append(generateRandomId(10)).append("=").append(currentTime).append("; ");
        
        // 设备ID
        cookieBuilder.append("DEVICEID=").append(generateRandomId(32)).append("; ");
        
        // 用户ID（有时存在，有时不存在）
        if (random.nextBoolean()) {
            cookieBuilder.append("uid=").append(random.nextInt(10000000)).append("; ");
        }
        
        // 添加语言和地区偏好
        String[] languages = {"zh-CN", "zh-TW", "en-US"};
        String[] regions = {"CN", "TW", "US"};
        cookieBuilder.append("lang=").append(languages[random.nextInt(languages.length)]).append("; ");
        cookieBuilder.append("region=").append(regions[random.nextInt(regions.length)]);
        
        headers.put("Cookie", cookieBuilder.toString());
        
        // 添加浏览器特有的X-header，提高真实性
        if (userAgent.contains("Chrome")) {
            headers.put("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"91\", \"Google Chrome\";v=\"91\"");
            headers.put("sec-ch-ua-mobile", random.nextBoolean() ? "?1" : "?0");
            if (random.nextBoolean()) {
                headers.put("sec-ch-ua-platform", "\"" + PLATFORM_NAMES[random.nextInt(PLATFORM_NAMES.length)] + "\"");
            }
        }
        
        return headers;
    }
    
    /**
     * 生成指定长度的随机ID
     * 
     * @param length ID长度
     * @return 随机ID字符串
     */
    private String generateRandomId(int length) {
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
} 