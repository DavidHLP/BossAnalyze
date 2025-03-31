package com.david.hlp.Spring.crawler.proxy.service.http;

import org.springframework.stereotype.Service;

import com.david.hlp.Spring.common.util.RedisCache;
import com.david.hlp.Spring.crawler.proxy.model.entity.ProxyInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * HTTP代理工具服务
 */
@Slf4j
@Service("HttpProxyToolService")
@RequiredArgsConstructor
public class ProxyToolService {

    private final ProxyRotationService proxyRotationService;
    private final RedisCache redisCache;

    /**
     * Redis HTTP代理池键名
     */
    private static final String HTTP_PROXY_POOL_KEY = "proxy:pool:http";

    /**
     * 移除指定IP和端口的HTTP代理
     *
     * @param ip 代理IP地址
     * @param port 代理端口
     * @return 是否成功移除
     */
    public boolean removeProxy(String ip, String port) {
        if (ip == null || ip.isEmpty() || port == null || port.isEmpty()) {
            log.warn("移除HTTP代理失败: IP或端口为空");
            return false;
        }

        log.info("开始移除HTTP代理: {}:{}", ip, port);

        // 获取所有可用代理
        List<ProxyInfo> allProxies = proxyRotationService.getAllProxies();

        // 查找匹配的代理
        for (ProxyInfo proxy : allProxies) {
            if (ip.equals(proxy.getIp()) && port.equals(proxy.getPort()) && "http".equalsIgnoreCase(proxy.getType())) {
                // 标记为不可用并从Redis中移除
                proxyRotationService.markProxyAsUnavailable(proxy);
                // 从HTTP代理池中移除
                removeProxyFromHttpPool(proxy);
                log.info("成功移除HTTP代理: {}:{}", ip, port);
                return true;
            }
        }

        log.warn("未找到指定的HTTP代理: {}:{}", ip, port);
        return false;
    }

    /**
     * 移除指定的HTTP代理
     *
     * @param proxy 要移除的代理信息
     * @return 是否成功移除
     */
    public boolean removeProxy(ProxyInfo proxy) {
        if (proxy == null) {
            log.warn("移除HTTP代理失败: 代理对象为空");
            return false;
        }

        if (!"http".equalsIgnoreCase(proxy.getType())) {
            log.warn("非HTTP代理无法处理: {}:{} (类型: {})", proxy.getIp(), proxy.getPort(), proxy.getType());
            return false;
        }

        log.info("开始移除HTTP代理: {}:{}", proxy.getIp(), proxy.getPort());
        proxyRotationService.markProxyAsUnavailable(proxy);
        // 从HTTP代理池中移除
        removeProxyFromHttpPool(proxy);
        log.info("成功移除HTTP代理: {}:{}", proxy.getIp(), proxy.getPort());
        return true;
    }

    /**
     * 获取指定数量的HTTP代理信息
     *
     * @param count 需要的代理数量
     * @return HTTP代理列表
     */
    public List<ProxyInfo> listProxies(int count) {
        log.info("开始获取{}个HTTP代理", count);

        if (count <= 0) {
            log.warn("请求的HTTP代理数量无效: {}", count);
            return new ArrayList<>();
        }

        // 从Redis获取HTTP代理池
        Map<String, ProxyInfo> proxyMap = redisCache.getCacheMap(HTTP_PROXY_POOL_KEY);

        if (proxyMap == null || proxyMap.isEmpty()) {
            log.warn("Redis中没有HTTP代理池或代理池为空");
            return new ArrayList<>();
        }

        // 从Redis直接获取HTTP代理
        List<ProxyInfo> proxies = new ArrayList<>(proxyMap.values());

        // 取出需要数量的代理
        if (proxies.size() > count) {
            proxies = proxies.subList(0, count);
        }

        log.info("成功从Redis获取{}个HTTP代理", proxies.size());
        return proxies;
    }

    /**
     * 保存HTTP代理到Redis
     *
     * @param proxyList 代理列表
     */
    public void saveHttpProxies(List<ProxyInfo> proxyList) {
        if (proxyList == null || proxyList.isEmpty()) {
            log.warn("保存HTTP代理失败: 代理列表为空");
            return;
        }

        // 筛选HTTP代理
        List<ProxyInfo> httpProxies = new ArrayList<>();
        for (ProxyInfo proxy : proxyList) {
            if ("http".equalsIgnoreCase(proxy.getType())) {
                httpProxies.add(proxy);
            }
        }

        if (httpProxies.isEmpty()) {
            log.warn("没有HTTP代理需要保存");
            return;
        }

        // 获取Redis中已有的HTTP代理
        Map<String, ProxyInfo> existingProxies = redisCache.getCacheMap(HTTP_PROXY_POOL_KEY);
        Map<String, Object> proxyMap = new HashMap<>();

        if (existingProxies != null && !existingProxies.isEmpty()) {
            proxyMap.putAll(existingProxies);
            log.info("Redis中已存在{}个HTTP代理，进行增量更新", existingProxies.size());
        } else {
            log.info("Redis中无现有HTTP代理，创建新缓存");
        }

        // 添加新的HTTP代理
        for (ProxyInfo proxy : httpProxies) {
            String key = proxy.getIp() + ":" + proxy.getPort();
            proxyMap.put(key, proxy);
        }

        // 保存到Redis的Hash结构中
        redisCache.setCacheMap(HTTP_PROXY_POOL_KEY, proxyMap);

        // 设置过期时间（24小时）
        redisCache.expire(HTTP_PROXY_POOL_KEY, 24, TimeUnit.HOURS);

        log.info("成功将{}个HTTP代理保存到Redis", httpProxies.size());
    }

    /**
     * 从HTTP代理池中移除指定代理
     *
     * @param proxy 要移除的代理
     */
    private void removeProxyFromHttpPool(ProxyInfo proxy) {
        if (proxy == null) {
            return;
        }

        String key = proxy.getIp() + ":" + proxy.getPort();

        // 从HTTP代理池中移除
        boolean result = redisCache.deleteCacheMapValue(HTTP_PROXY_POOL_KEY, key);

        if (result) {
            log.info("从HTTP代理池中移除代理: {}:{}", proxy.getIp(), proxy.getPort());
        }
    }

    /**
     * 获取HTTP代理数量
     *
     * @return HTTP代理数量
     */
    public int getHttpProxyCount() {
        Map<String, ProxyInfo> proxyMap = redisCache.getCacheMap(HTTP_PROXY_POOL_KEY);
        return proxyMap != null ? proxyMap.size() : 0;
    }
}
