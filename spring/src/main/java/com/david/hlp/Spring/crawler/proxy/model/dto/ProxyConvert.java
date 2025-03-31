package com.david.hlp.Spring.crawler.proxy.model.dto;

import com.david.hlp.Spring.crawler.proxy.model.HttpsProxy;
import com.david.hlp.Spring.crawler.proxy.model.entity.ProxyInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 代理对象转换工具类
 * 
 * @author david
 */
public class ProxyConvert {
    
    /**
     * 将HttpsProxy转换为ProxyInfo
     *
     * @param httpsProxy HTTPS代理对象
     * @return 代理信息对象
     */
    public static ProxyInfo toProxyInfo(HttpsProxy httpsProxy) {
        if (httpsProxy == null) {
            return null;
        }
        
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setIp(httpsProxy.getIp());
        proxyInfo.setPort(httpsProxy.getPort());
        proxyInfo.setAnonymity(httpsProxy.getAnonymity());
        proxyInfo.setType(ProxyConstants.PROXY_TYPE_HTTPS);
        proxyInfo.setLocation(httpsProxy.getLocation());
        proxyInfo.setResponseTime(httpsProxy.getResponseTime());
        proxyInfo.setLastVerifyTime(httpsProxy.getRecordTime());
        
        return proxyInfo;
    }
    
    /**
     * 批量将HttpsProxy列表转换为ProxyInfo列表
     *
     * @param httpsProxies HTTPS代理列表
     * @return 代理信息列表
     */
    public static List<ProxyInfo> toProxyInfoList(List<HttpsProxy> httpsProxies) {
        if (httpsProxies == null || httpsProxies.isEmpty()) {
            return new ArrayList<>();
        }
        
        return httpsProxies.stream()
                .map(ProxyConvert::toProxyInfo)
                .collect(Collectors.toList());
    }
} 