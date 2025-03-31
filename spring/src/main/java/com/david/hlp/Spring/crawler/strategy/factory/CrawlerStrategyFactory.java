package com.david.hlp.Spring.crawler.strategy.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.david.hlp.Spring.crawler.strategy.CrawlerStrategy;
import com.david.hlp.Spring.crawler.strategy.impl.AdvancedCrawlerStrategy;
import com.david.hlp.Spring.crawler.strategy.impl.DelayedCrawlerStrategy;
import com.david.hlp.Spring.crawler.strategy.impl.RandomCrawlerStrategy;

/**
 * 爬虫策略工厂
 * 根据不同的场景选择合适的爬取策略
 */
@Component
public class CrawlerStrategyFactory {

    /**
     * 基础随机策略
     */
    private final RandomCrawlerStrategy randomCrawlerStrategy;
    
    /**
     * 延迟策略
     */
    private final DelayedCrawlerStrategy delayedCrawlerStrategy;
    
    /**
     * 高级策略
     */
    private final AdvancedCrawlerStrategy advancedCrawlerStrategy;
    
    /**
     * 策略类型枚举
     */
    public enum StrategyType {
        /**
         * 基础随机策略
         */
        RANDOM,
        
        /**
         * 延迟策略
         */
        DELAYED,
        
        /**
         * 高级策略
         */
        ADVANCED,
        
        /**
         * 自动选择策略
         */
        AUTO
    }
    
    /**
     * 构造函数，注入所有策略实现
     * 
     * @param randomCrawlerStrategy 随机爬取策略
     * @param delayedCrawlerStrategy 延迟爬取策略
     * @param advancedCrawlerStrategy 高级爬取策略
     */
    @Autowired
    public CrawlerStrategyFactory(
            RandomCrawlerStrategy randomCrawlerStrategy,
            DelayedCrawlerStrategy delayedCrawlerStrategy,
            AdvancedCrawlerStrategy advancedCrawlerStrategy) {
        this.randomCrawlerStrategy = randomCrawlerStrategy;
        this.delayedCrawlerStrategy = delayedCrawlerStrategy;
        this.advancedCrawlerStrategy = advancedCrawlerStrategy;
    }
    
    /**
     * 获取爬取策略
     * 
     * @param type 策略类型
     * @return 爬取策略实现
     */
    public CrawlerStrategy getStrategy(StrategyType type) {
        // 根据类型返回相应的策略实现
        switch (type) {
            case RANDOM:
                return randomCrawlerStrategy;
            case DELAYED:
                return delayedCrawlerStrategy;
            case ADVANCED:
                return advancedCrawlerStrategy;
            case AUTO:
                // 自动选择策略的逻辑，这里可以根据时间、请求频率等因素动态选择
                return getAutoStrategy();
            default:
                // 默认返回随机策略
                return randomCrawlerStrategy;
        }
    }
    
    /**
     * 根据当前时间、系统负载等因素自动选择合适的策略
     * 
     * @return 自动选择的爬取策略
     */
    private CrawlerStrategy getAutoStrategy() {
        // 获取当前小时
        int hour = java.time.LocalDateTime.now().getHour();
        
        // 夜间使用基础随机策略（网站流量低，检测不严格）
        if (hour >= 0 && hour < 6) {
            return randomCrawlerStrategy;
        }
        
        // 早晚高峰时段使用高级策略（网站流量高，检测更严格）
        if ((hour >= 8 && hour < 10) || (hour >= 17 && hour < 20)) {
            return advancedCrawlerStrategy;
        }
        
        // 其他时间使用延迟策略
        return delayedCrawlerStrategy;
    }
    
    /**
     * 根据连续请求次数自动选择策略
     * 随着请求次数增加，增加策略的复杂度和安全性
     * 
     * @param consecutiveRequests 已连续请求次数
     * @return 自动选择的爬取策略
     */
    public CrawlerStrategy getStrategyByRequestCount(int consecutiveRequests) {
        if (consecutiveRequests < 10) {
            // 初始阶段使用基础策略
            return randomCrawlerStrategy;
        } else if (consecutiveRequests < 30) {
            // 中间阶段使用延迟策略
            return delayedCrawlerStrategy;
        } else {
            // 长时间爬取使用高级策略
            return advancedCrawlerStrategy;
        }
    }
} 