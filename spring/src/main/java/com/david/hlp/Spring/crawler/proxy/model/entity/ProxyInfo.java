package com.david.hlp.Spring.crawler.proxy.model.entity;

import lombok.Data;
import java.io.Serializable;

/**
 * 代理信息实体类
 */
@Data
public class ProxyInfo implements Serializable {
    /**
     * 序列化版本UID
     */
    private static final long serialVersionUID = 1L;
    /**
     * IP地址
     */
    private String ip;
    /**
     * 端口
     */
    private String port;
    /**
     * 匿名度
     */
    private String anonymity;
    /**
     * 代理类型
     */
    private String type;
    /**
     * 位置
     */
    private String location;
    /**
     * 响应时间
     */
    private String responseTime;
    /**
     * 最后验证时间
     */
    private String lastVerifyTime;
    /**
     * 付费类型
     */
    private String paymentType;
    /**
     * 代理状态（有效/无效）
     */
    private String status;
}