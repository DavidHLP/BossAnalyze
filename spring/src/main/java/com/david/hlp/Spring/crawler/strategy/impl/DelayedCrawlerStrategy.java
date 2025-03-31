package com.david.hlp.Spring.crawler.strategy.impl;

import org.springframework.stereotype.Component;

import com.david.hlp.Spring.crawler.strategy.CrawlerStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 延迟爬取策略实现
 * 使用更长的延迟时间和Cookie管理来应对严格的反爬机制
 */
@Component
public class DelayedCrawlerStrategy implements CrawlerStrategy {
    
    /**
     * 随机数生成器
     */
    private final Random random = new Random();
    
    /**
     * 常用用户代理列表
     */
    private final List<String> userAgents = new ArrayList<>();
    
    /**
     * 最小等待时间（毫秒）
     */
    private static final long MIN_WAIT_TIME = 10000;
    
    /**
     * 最大等待时间（毫秒）
     */
    private static final long MAX_WAIT_TIME = 30000;
    
    /**
     * 最小请求间隔（毫秒）
     */
    private static final long MIN_REQUEST_INTERVAL = 15000;
    
    /**
     * 最大请求间隔（毫秒）
     */
    private static final long MAX_REQUEST_INTERVAL = 45000;
    
    /**
     * 代理切换阈值（连续请求次数）
     */
    private static final int PROXY_SWITCH_THRESHOLD = 3;
    
    /**
     * 构造函数，初始化用户代理列表
     */
    public DelayedCrawlerStrategy() {
        initUserAgents();
    }
    
    /**
     * 初始化用户代理列表
     */
    private void initUserAgents() {
        // 更多的桌面浏览器User-Agent
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:92.0) Gecko/20100101 Firefox/92.0");
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:93.0) Gecko/20100101 Firefox/93.0");
        userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Safari/605.1.15");
        userAgents.add("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
        
        // 添加更多移动设备User-Agent
        userAgents.add("Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1");
        userAgents.add("Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1");
        userAgents.add("Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Mobile Safari/537.36");
        userAgents.add("Mozilla/5.0 (Linux; Android 12; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Mobile Safari/537.36");
    }
    
    @Override
    public long getRandomWaitTime() {
        // 生成随机等待时间，使用更长的延迟
        return MIN_WAIT_TIME + (long)(random.nextDouble() * (MAX_WAIT_TIME - MIN_WAIT_TIME));
    }
    
    @Override
    public String getRandomUserAgent() {
        // 从用户代理列表中随机选择一个
        return userAgents.get(random.nextInt(userAgents.size()));
    }
    
    @Override
    public boolean shouldSwitchProxy(int consecutiveRequests) {
        // 更频繁地切换代理
        if (consecutiveRequests >= PROXY_SWITCH_THRESHOLD) {
            return true;
        }
        // 有30%的概率随机切换代理，增加不可预测性
        return random.nextDouble() < 0.3;
    }
    
    @Override
    public long getRequestInterval() {
        // 生成更长的请求间隔时间
        return MIN_REQUEST_INTERVAL + (long)(random.nextDouble() * (MAX_REQUEST_INTERVAL - MIN_REQUEST_INTERVAL));
    }
    
    @Override
    public Map<String, String> getRandomHeaders() {
        Map<String, String> headers = new HashMap<>();
        
        // 添加随机User-Agent
        headers.put("User-Agent", getRandomUserAgent());
        
        // 添加基本请求头
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        
        // 添加更多随机请求头
        if (random.nextBoolean()) {
            headers.put("Accept-Encoding", "gzip, deflate, br");
        }
        
        if (random.nextBoolean()) {
            headers.put("DNT", "1");
        }
        
        // 随机添加Cache-Control
        String[] cacheControls = {
            "no-cache", 
            "max-age=0", 
            "no-store, no-cache, must-revalidate, post-check=0, pre-check=0"
        };
        headers.put("Cache-Control", cacheControls[random.nextInt(cacheControls.length)]);
        
        // 随机添加引用来源
        if (random.nextBoolean()) {
            String[] referers = {
                "https://www.google.com/search?q=jobs",
                "https://cn.bing.com/search?q=招聘信息",
                "https://www.baidu.com/s?wd=招聘",
                "https://www.zhipin.com/",
                "https://www.liepin.com/",
                "https://www.lagou.com/"
            };
            headers.put("Referer", referers[random.nextInt(referers.length)]);
        }
        
        // 添加一些随机Cookie
        if (random.nextBoolean()) {
            StringBuilder cookieBuilder = new StringBuilder();
            cookieBuilder.append("_ga=GA1.2.").append(random.nextInt(1000000000)).append(".")
                    .append(System.currentTimeMillis() / 1000 - random.nextInt(10000000));
            cookieBuilder.append("; _gid=GA1.2.").append(random.nextInt(1000000000)).append(".")
                    .append(System.currentTimeMillis() / 1000 - random.nextInt(100000));
            cookieBuilder.append("; JSESSIONID=").append(UUID.randomUUID().toString().replace("-", ""));
            
            headers.put("Cookie", cookieBuilder.toString());
        }
        
        return headers;
    }
} 