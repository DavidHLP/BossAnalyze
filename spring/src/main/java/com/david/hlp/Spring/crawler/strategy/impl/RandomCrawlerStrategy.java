package com.david.hlp.Spring.crawler.strategy.impl;

import org.springframework.stereotype.Component;

import com.david.hlp.Spring.crawler.strategy.CrawlerStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 随机爬取策略实现
 * 通过随机等待时间、随机用户代理、代理IP轮换等方式避免网站的反爬机制
 */
@Component
public class RandomCrawlerStrategy implements CrawlerStrategy {

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
    private static final long MIN_WAIT_TIME = 2000;
    
    /**
     * 最大等待时间（毫秒）
     */
    private static final long MAX_WAIT_TIME = 10000;
    
    /**
     * 最小请求间隔（毫秒）
     */
    private static final long MIN_REQUEST_INTERVAL = 5000;
    
    /**
     * 最大请求间隔（毫秒）
     */
    private static final long MAX_REQUEST_INTERVAL = 15000;
    
    /**
     * 代理切换阈值（连续请求次数）
     */
    private static final int PROXY_SWITCH_THRESHOLD = 5;
    
    /**
     * 构造函数，初始化用户代理列表
     */
    public RandomCrawlerStrategy() {
        initUserAgents();
    }
    
    /**
     * 初始化用户代理列表
     */
    private void initUserAgents() {
        // Windows Chrome
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36");
        // Windows Firefox
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:90.0) Gecko/20100101 Firefox/90.0");
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0");
        // Windows Edge
        userAgents.add("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.59");
        // macOS Safari
        userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.1 Safari/605.1.15");
        // macOS Chrome
        userAgents.add("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36");
        // iOS Safari
        userAgents.add("Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1");
        // Android Chrome
        userAgents.add("Mozilla/5.0 (Linux; Android 11; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");
    }
    
    @Override
    public long getRandomWaitTime() {
        // 生成MIN_WAIT_TIME到MAX_WAIT_TIME之间的随机时间
        return MIN_WAIT_TIME + (long)(random.nextDouble() * (MAX_WAIT_TIME - MIN_WAIT_TIME));
    }
    
    @Override
    public String getRandomUserAgent() {
        // 从用户代理列表中随机选择一个
        return userAgents.get(random.nextInt(userAgents.size()));
    }
    
    @Override
    public boolean shouldSwitchProxy(int consecutiveRequests) {
        // 根据连续请求次数判断是否需要切换代理
        if (consecutiveRequests >= PROXY_SWITCH_THRESHOLD) {
            return true;
        }
        // 有10%的概率随机切换代理，增加不可预测性
        return random.nextDouble() < 0.1;
    }
    
    @Override
    public long getRequestInterval() {
        // 生成MIN_REQUEST_INTERVAL到MAX_REQUEST_INTERVAL之间的随机间隔
        return MIN_REQUEST_INTERVAL + (long)(random.nextDouble() * (MAX_REQUEST_INTERVAL - MIN_REQUEST_INTERVAL));
    }
    
    @Override
    public Map<String, String> getRandomHeaders() {
        Map<String, String> headers = new HashMap<>();
        // 添加随机User-Agent
        headers.put("User-Agent", getRandomUserAgent());
        // 添加其他常见请求头
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.put("Cache-Control", "max-age=0");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        
        // 随机添加一些可选的请求头
        if (random.nextBoolean()) {
            headers.put("Accept-Encoding", "gzip, deflate, br");
        }
        
        if (random.nextBoolean()) {
            headers.put("DNT", "1");
        }
        
        if (random.nextBoolean()) {
            // 随机引用来源
            String[] referers = {
                "https://www.google.com/",
                "https://www.bing.com/",
                "https://www.baidu.com/",
                "https://www.zhipin.com/",
                "https://cn.bing.com/"
            };
            headers.put("Referer", referers[random.nextInt(referers.length)]);
        }
        
        return headers;
    }
}
