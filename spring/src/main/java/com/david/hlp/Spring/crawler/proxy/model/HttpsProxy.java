package com.david.hlp.Spring.crawler.proxy.model;

import lombok.Data;

/**
 * HTTPS代理信息模型
 */
@Data
public class HttpsProxy {
    private String ip;
    private String port;
    private String anonymity;
    private String proxyType;
    private String location;
    private String responseTime;
    private String recordTime;
} 