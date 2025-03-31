package com.david.hlp.Spring.crawler.strategy;

/**
 * 随机爬取策略接口
 * 实现该接口的类可以提供不同的随机化策略以避免被网站反爬机制识别
 */
public interface CrawlerStrategy {
    
    /**
     * 获取随机等待时间（毫秒）
     * 
     * @return 随机等待的毫秒数
     */
    long getRandomWaitTime();
    
    /**
     * 获取随机用户代理
     * 
     * @return 随机的User-Agent字符串
     */
    String getRandomUserAgent();
    
    /**
     * 是否需要切换代理IP
     * 
     * @param consecutiveRequests 已连续请求次数
     * @return 是否需要切换代理
     */
    boolean shouldSwitchProxy(int consecutiveRequests);
    
    /**
     * 获取随机请求间隔（毫秒）
     * 
     * @return 两次请求之间的随机间隔时间
     */
    long getRequestInterval();
    
    /**
     * 生成随机的请求头参数
     * 
     * @return 随机的请求头参数映射
     */
    java.util.Map<String, String> getRandomHeaders();
}
