package com.david.hlp.Spring.crawler.proxy.model.dto;

/**
 * 代理相关常量类
 * 
 * @author david
 */
public class ProxyConstants {
    
    /**
     * Redis中HTTPS代理池键名
     */
    public static final String HTTPS_PROXY_POOL_KEY = "proxy:https:pool";
    
    /**
     * Redis中HTTP代理池键名
     */
    public static final String HTTP_PROXY_POOL_KEY = "proxy:http:pool";
    
    /**
     * 代理测试URL-HTTPS
     */
    public static final String HTTPS_TEST_URL = "https://api.ipify.org/";
    
    /**
     * 代理测试URL-HTTP
     */
    public static final String HTTP_TEST_URL = "http://httpbin.org/ip";
    
    /**
     * 连接超时时间（秒）
     */
    public static final int CONNECTION_TIMEOUT = 5;
    
    /**
     * 页面加载等待时间（秒）
     */
    public static final int PAGE_LOAD_WAIT = 10;
    
    /**
     * Redis缓存过期时间（小时）
     */
    public static final int REDIS_EXPIRE_HOURS = 24;
    
    /**
     * 代理池刷新间隔（毫秒）-2小时
     */
    public static final long PROXY_REFRESH_INTERVAL = 7200000;
    
    /**
     * 代理类型-HTTPS
     */
    public static final String PROXY_TYPE_HTTPS = "https";
    
    /**
     * 代理类型-HTTP
     */
    public static final String PROXY_TYPE_HTTP = "http";
    
    /**
     * 代理状态-有效
     */
    public static final String PROXY_STATUS_VALID = "有效";
    
    /**
     * 代理状态-无效
     */
    public static final String PROXY_STATUS_INVALID = "无效";
} 