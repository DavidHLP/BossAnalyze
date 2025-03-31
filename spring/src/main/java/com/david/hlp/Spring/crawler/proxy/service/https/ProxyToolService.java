package com.david.hlp.Spring.crawler.proxy.service.https;

import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;

import com.david.hlp.Spring.common.util.RedisCache;
import com.david.hlp.Spring.crawler.proxy.model.dto.ProxyConstants;
import com.david.hlp.Spring.crawler.proxy.model.entity.ProxyInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * HTTPS代理工具服务
 * 提供操作HTTPS代理的工具方法
 *
 * @author david
 */
@Slf4j
@Service("HttpsProxyToolService")
@RequiredArgsConstructor
public class ProxyToolService {

    @Qualifier("HttpsProxyRotationService")
    private final ProxyRotationService proxyRotationService;

    private final RedisCache redisCache;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 移除指定IP和端口的代理
     *
     * @param ip 代理IP地址
     * @param port 代理端口
     * @return 是否成功移除
     */
    public boolean removeProxy(String ip, String port) {
        if (ip == null || ip.isEmpty() || port == null || port.isEmpty()) {
            log.warn("移除代理失败: IP或端口为空");
            return false;
        }

        log.info("开始移除代理: {}:{}", ip, port);

        // 获取所有可用代理
        List<ProxyInfo> allProxies = proxyRotationService.getAllProxies();

        // 查找匹配的代理
        for (ProxyInfo proxy : allProxies) {
            if (ip.equals(proxy.getIp()) && port.equals(proxy.getPort())) {
                // 标记为不可用并从Redis中移除
                proxyRotationService.markProxyAsUnavailable(proxy);
                log.info("成功移除代理: {}:{}", ip, port);
                return true;
            }
        }

        log.warn("未找到指定的代理: {}:{}", ip, port);
        return false;
    }

    /**
     * 移除指定的代理
     *
     * @param proxy 要移除的代理信息
     * @return 是否成功移除
     */
    public boolean removeProxy(ProxyInfo proxy) {
        if (proxy == null) {
            log.warn("移除代理失败: 代理对象为空");
            return false;
        }

        log.info("开始移除代理: {}:{}", proxy.getIp(), proxy.getPort());
        proxyRotationService.markProxyAsUnavailable(proxy);
        log.info("成功移除代理: {}:{}", proxy.getIp(), proxy.getPort());
        return true;
    }

    /**
     * 获取指定数量的HTTPS代理信息
     *
     * @param count 需要的代理数量
     * @return HTTPS代理列表
     */
    public List<ProxyInfo> listProxies(int count) {
        log.info("开始获取{}个HTTPS代理", count);

        if (count <= 0) {
            log.warn("请求的代理数量无效: {}", count);
            return new ArrayList<>();
        }

        // 检查代理池是否存在
        String redisKey = ProxyConstants.HTTPS_PROXY_POOL_KEY;
        Map<String, ProxyInfo> proxyMap = redisCache.getCacheMap(redisKey);

        if (proxyMap == null || proxyMap.isEmpty()) {
            log.warn("Redis中没有HTTPS代理池或代理池为空");
            // 强制刷新代理池
            proxyRotationService.forceRefreshProxyPool();
            // 重新获取
            proxyMap = redisCache.getCacheMap(redisKey);
            if (proxyMap == null || proxyMap.isEmpty()) {
                return new ArrayList<>();
            }
        }

        // 从Redis直接获取所有代理
        List<ProxyInfo> proxies = new ArrayList<>(proxyMap.values());

        // 取出需要数量的代理
        if (proxies.size() > count) {
            proxies = proxies.subList(0, count);
        }

        log.info("成功从Redis获取{}个HTTPS代理", proxies.size());
        return proxies;
    }

    /**
     * 获取Redis中的代理池Key
     *
     * @return 代理池Key列表
     */
    public List<String> getProxyPoolKeys() {
        Set<String> keys = redisTemplate.keys(ProxyConstants.HTTPS_PROXY_POOL_KEY);
        return keys != null ? new ArrayList<>(keys) : new ArrayList<>();
    }

    /**
     * 获取Redis中代理数量
     *
     * @return 代理数量
     */
    public int getProxyCount() {
        String key = ProxyConstants.HTTPS_PROXY_POOL_KEY;
        
        Map<String, ProxyInfo> proxyMap = redisCache.getCacheMap(key);
        return proxyMap != null ? proxyMap.size() : 0;
    }
    
    /**
     * 获取一个可用代理
     * 
     * @return 可用代理
     */
    public ProxyInfo getAvailableProxy() {
        // 根据负载均衡策略获取代理
        return proxyRotationService.getNextProxy();
    }
    
    /**
     * 获取随机可用代理
     * 
     * @return 随机可用代理
     */
    public ProxyInfo getRandomProxy() {
        // 随机获取代理
        return proxyRotationService.getRandomProxy();
    }
    
    /**
     * 检查代理是否可用
     * 
     * @param proxy 要检查的代理
     * @return 代理是否可用
     */
    public boolean isProxyAvailable(ProxyInfo proxy) {
        return proxyRotationService.testProxy(proxy);
    }
    
    /**
     * 强制刷新代理池
     */
    public void refreshProxyPool() {
        proxyRotationService.forceRefreshProxyPool();
    }
}
