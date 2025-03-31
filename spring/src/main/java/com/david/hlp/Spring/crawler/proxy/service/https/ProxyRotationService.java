package com.david.hlp.Spring.crawler.proxy.service.https;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import com.david.hlp.Spring.crawler.proxy.model.HttpsProxy;
import com.david.hlp.Spring.crawler.proxy.model.dto.ProxyConstants;
import com.david.hlp.Spring.crawler.proxy.model.dto.ProxyConvert;
import com.david.hlp.Spring.crawler.proxy.model.entity.ProxyInfo;
import com.david.hlp.Spring.common.util.RedisCache;
import lombok.RequiredArgsConstructor;

/**
 * HTTPS代理轮换服务
 * 负责管理代理池、测试代理可用性和轮换代理IP
 * 
 * @author david
 */
@Slf4j
@RequiredArgsConstructor
@Service("HttpsProxyRotationService")
public class ProxyRotationService {

    /**
     * 代理池服务
     */
    @Qualifier("HttpsProxyPoolService")
    private final ProxyPoolService proxyPoolService;

    /**
     * Redis缓存服务
     */
    private final RedisCache redisCache;

    /**
     * 可用代理列表（线程安全）
     */
    private final List<ProxyInfo> availableProxies = new CopyOnWriteArrayList<>();

    /**
     * 当前使用的代理索引
     */
    private final AtomicInteger currentProxyIndex = new AtomicInteger(0);

    /**
     * 随机数生成器
     */
    private final Random random = new Random();
    
    /**
     * 定时刷新代理列表
     * 每2小时执行一次
     */
    // @Scheduled(fixedRate = ProxyConstants.PROXY_REFRESH_INTERVAL)
    public void refreshProxyList() {
        log.info("开始刷新HTTPS代理列表");

        try {
            // 先从Redis获取代理列表
            List<ProxyInfo> redisProxies = getProxiesFromRedis();
            
            if (redisProxies.isEmpty()) {
                log.info("Redis中没有可用HTTPS代理，开始获取新代理");
                // 获取新的HTTPS代理
                List<HttpsProxy> httpsProxies = proxyPoolService.listProxies();
                
                if (httpsProxies.isEmpty()) {
                    log.warn("未获取到任何HTTPS代理");
                    return;
                }
                
                // 将HttpsProxy转换为ProxyInfo
                List<ProxyInfo> newProxies = ProxyConvert.toProxyInfoList(httpsProxies);
                
                // 使用ProxyPoolService验证代理
                List<ProxyInfo> validProxies = proxyPoolService.validateProxies(newProxies);
                
                if (!validProxies.isEmpty()) {
                    // 清空并重新添加代理
                    availableProxies.clear();
                    availableProxies.addAll(validProxies);
                    // 重置索引
                    currentProxyIndex.set(0);
                    
                    // 保存到Redis，由于ProxyPoolService的saveProxiesToRedis是私有方法，使用本地实现
                    saveToRedis(validProxies);
                    
                    log.info("HTTPS代理列表已刷新，共获取到{}个可用代理", validProxies.size());
                } else {
                    log.warn("未获取到任何可用的HTTPS代理");
                }
            } else {
                // 使用Redis中的代理更新本地缓存
                availableProxies.clear();
                availableProxies.addAll(redisProxies);
                currentProxyIndex.set(0);
                log.info("从Redis加载了{}个可用HTTPS代理", redisProxies.size());

                // 验证Redis中的代理是否仍然可用
                validateRedisProxies();
            }
        } catch (Exception e) {
            log.error("刷新HTTPS代理列表时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 验证Redis中的代理是否可用
     * 移除不可用的代理
     */
    private void validateRedisProxies() {
        log.info("开始验证Redis中的HTTPS代理...");
        
        // 调用ProxyPoolService的validateRedisProxies方法
        List<ProxyInfo> validProxies = proxyPoolService.validateRedisProxies();
        
        if (validProxies.size() < availableProxies.size()) {
            // 清空并重新添加有效代理
            availableProxies.clear();
            availableProxies.addAll(validProxies);
            currentProxyIndex.set(0);
            
            log.info("验证后保留了{}个可用HTTPS代理", validProxies.size());
            
            // 如果可用代理太少，尝试获取新代理
            if (availableProxies.size() < 3) {
                log.info("可用HTTPS代理数量低于阈值，开始获取新代理");
                List<HttpsProxy> httpsProxies = proxyPoolService.listProxies();
                if (!httpsProxies.isEmpty()) {
                    List<ProxyInfo> newProxies = ProxyConvert.toProxyInfoList(httpsProxies);
                    List<ProxyInfo> newValidProxies = proxyPoolService.validateProxies(newProxies);
                    availableProxies.addAll(newValidProxies);
                    saveToRedis(newValidProxies);
                    log.info("新增{}个可用HTTPS代理，当前总共{}个可用代理", newValidProxies.size(), availableProxies.size());
                }
            }
        } else {
            log.info("Redis中的所有HTTPS代理均可用");
        }
    }

    /**
     * 从Redis获取代理列表
     *
     * @return 代理列表
     */
    public List<ProxyInfo> getProxiesFromRedis() {
        // 使用ProxyPoolService的方法获取有效代理
        List<ProxyInfo> proxies = proxyPoolService.getValidProxies();
        
        if (proxies.isEmpty()) {
            log.info("Redis中没有HTTPS代理");
        } else {
            log.info("从Redis获取到{}个HTTPS代理", proxies.size());
        }
        
        return proxies;
    }

    /**
     * 从Redis中删除代理
     *
     * @param proxy 要删除的代理
     */
    public void removeProxyFromRedis(ProxyInfo proxy) {
        if (proxy == null) {
            return;
        }
        
        String redisKey = ProxyConstants.HTTPS_PROXY_POOL_KEY;
        String proxyKey = proxy.getIp() + ":" + proxy.getPort();
        
        redisCache.deleteCacheMapValue(redisKey, proxyKey);
        log.info("从Redis中删除HTTPS代理: {}", proxyKey);
    }
    
    /**
     * 保存代理到Redis
     * 
     * @param proxyList 要保存的代理列表
     */
    private void saveToRedis(List<ProxyInfo> proxyList) {
        if (proxyList == null || proxyList.isEmpty()) {
            log.warn("保存代理失败: 代理列表为空");
            return;
        }

        String redisKey = ProxyConstants.HTTPS_PROXY_POOL_KEY;

        // 获取Redis中已有的代理
        Map<String, ProxyInfo> existingProxies = redisCache.getCacheMap(redisKey);
        Map<String, Object> proxyMap = new HashMap<>();

        if (existingProxies != null && !existingProxies.isEmpty()) {
            proxyMap.putAll(existingProxies);
            log.info("Redis中已存在{}个代理，进行增量更新", existingProxies.size());
        } else {
            log.info("Redis中无现有代理，创建新缓存");
        }

        // 添加新的代理
        for (ProxyInfo proxy : proxyList) {
            String key = proxy.getIp() + ":" + proxy.getPort();
            // 确保代理类型为HTTPS
            proxy.setType(ProxyConstants.PROXY_TYPE_HTTPS);
            proxyMap.put(key, proxy);
        }

        // 保存到Redis的Hash结构中
        redisCache.setCacheMap(redisKey, proxyMap);

        // 设置过期时间
        redisCache.expire(redisKey, ProxyConstants.REDIS_EXPIRE_HOURS, TimeUnit.HOURS);

        log.info("成功将{}个HTTPS代理保存到Redis", proxyList.size());
    }
    
    /**
     * 测试代理是否可用
     *
     * @param proxy 要测试的代理
     * @return 是否可用
     */
    public boolean testProxy(ProxyInfo proxy) {
        if (proxy == null) {
            return false;
        }

        String ip = proxy.getIp();
        String port = proxy.getPort();

        try {
            // 构建代理
            Proxy javaProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, Integer.parseInt(port)));

            // 创建连接
            URL url = new URL(ProxyConstants.HTTPS_TEST_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(javaProxy);
            connection.setConnectTimeout(ProxyConstants.CONNECTION_TIMEOUT * 1000);
            connection.setReadTimeout(ProxyConstants.CONNECTION_TIMEOUT * 1000);

            // 获取响应代码
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                proxy.setStatus(ProxyConstants.PROXY_STATUS_VALID);
                log.info("HTTPS代理 {}:{} 可用，响应状态码: {}", ip, port, responseCode);
                return true;
            } else {
                proxy.setStatus(ProxyConstants.PROXY_STATUS_INVALID);
                log.info("HTTPS代理 {}:{} 返回状态码: {}", ip, port, responseCode);
                return false;
            }
        } catch (Exception e) {
            proxy.setStatus(ProxyConstants.PROXY_STATUS_INVALID);
            log.info("HTTPS代理 {}:{} 不可用: {}", ip, port, e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取下一个代理
     * 按顺序轮换代理
     *
     * @return 代理信息或null（无可用代理时）
     */
    public ProxyInfo getNextProxy() {
        if (availableProxies.isEmpty()) {
            log.warn("HTTPS代理池为空，尝试刷新");
            refreshProxyList();
            if (availableProxies.isEmpty()) {
                return null;
            }
        }

        // 轮询获取下一个代理
        int index = currentProxyIndex.getAndIncrement() % availableProxies.size();
        if (index < 0) {
            // 处理整数溢出情况
            currentProxyIndex.set(0);
            index = 0;
        }

        ProxyInfo proxy = availableProxies.get(index);
        log.info("获取HTTPS代理: {}:{}", proxy.getIp(), proxy.getPort());
        return proxy;
    }

    /**
     * 获取随机代理
     *
     * @return 随机代理信息或null（无可用代理时）
     */
    public ProxyInfo getRandomProxy() {
        if (availableProxies.isEmpty()) {
            log.warn("HTTPS代理池为空，尝试刷新");
            refreshProxyList();
            if (availableProxies.isEmpty()) {
                return null;
            }
        }

        // 随机选择一个代理
        int index = random.nextInt(availableProxies.size());
        ProxyInfo proxy = availableProxies.get(index);
        log.info("获取随机HTTPS代理: {}:{}", proxy.getIp(), proxy.getPort());
        return proxy;
    }

    /**
     * 标记代理为不可用
     * 从可用代理列表中移除，并从Redis中删除
     *
     * @param proxy 不可用的代理
     */
    public void markProxyAsUnavailable(ProxyInfo proxy) {
        if (proxy != null) {
            log.info("标记HTTPS代理为不可用: {}:{}", proxy.getIp(), proxy.getPort());
            availableProxies.remove(proxy);

            // 从Redis中删除不可用代理
            removeProxyFromRedis(proxy);

            // 如果可用代理太少，尝试刷新
            if (availableProxies.size() < 3) {
                log.info("可用HTTPS代理数量低于阈值，尝试刷新代理列表");
                refreshProxyList();
            }
        }
    }
    
    /**
     * 获取代理数量
     * 
     * @return 可用代理数量
     */
    public int getProxyCount() {
        return availableProxies.size();
    }
    
    /**
     * 获取所有可用代理
     * 
     * @return 可用代理列表（只读）
     */
    public List<ProxyInfo> getAllProxies() {
        return Collections.unmodifiableList(new ArrayList<>(availableProxies));
    }
    
    /**
     * 强制刷新代理池
     */
    public void forceRefreshProxyPool() {
        log.info("强制刷新HTTPS代理池...");
        
        // 获取新的HTTPS代理
        List<HttpsProxy> httpsProxies = proxyPoolService.listProxies();
        
        if (!httpsProxies.isEmpty()) {
            // 将HttpsProxy转换为ProxyInfo
            List<ProxyInfo> newProxies = ProxyConvert.toProxyInfoList(httpsProxies);
            
            // 使用ProxyPoolService验证代理
            List<ProxyInfo> validProxies = proxyPoolService.validateProxies(newProxies);
            
            if (!validProxies.isEmpty()) {
                // 清空并重新添加代理
                availableProxies.clear();
                availableProxies.addAll(validProxies);
                // 重置索引
                currentProxyIndex.set(0);
                
                // 保存到Redis
                saveToRedis(validProxies);
                
                log.info("HTTPS代理池已强制刷新，共获取到{}个可用代理", validProxies.size());
            } else {
                log.warn("强制刷新未获取到任何可用的HTTPS代理");
            }
        } else {
            log.warn("强制刷新未获取到任何HTTPS代理");
        }
    }
}
