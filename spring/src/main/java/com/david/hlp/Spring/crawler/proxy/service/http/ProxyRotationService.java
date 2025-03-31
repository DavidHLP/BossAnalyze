package com.david.hlp.Spring.crawler.proxy.service.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.david.hlp.Spring.crawler.proxy.model.entity.ProxyInfo;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;

/**
 * HTTP代理轮换服务
 * 负责管理HTTP代理池、测试代理可用性和轮换代理IP
 */
@Slf4j
@Service("HttpProxyRotationService")
@RequiredArgsConstructor
public class ProxyRotationService {

    /**
     * 代理池服务
     */
    private final ProxyPoolService proxyPoolService;

    /**
     * 可用HTTP代理列表
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
     * 测试URL
     */
    private static final String TEST_URL = "http://httpbin.org/ip";

    /**
     * 连接超时时间（秒）
     */
    private static final int TIMEOUT = 5;

    /**
     * 默认快代理URL基础地址
     */
    private static final String DEFAULT_PROXY_BASE_URL = "https://www.kuaidaili.com/free/inha/";

    /**
     * 定时刷新代理列表
     * 每2小时执行一次
     */
    // @Scheduled(fixedRate = 7200000)
    public void refreshProxyList() {
        log.info("开始刷新代理列表");

        // 先从Redis获取代理列表
        List<ProxyInfo> redisProxies = proxyPoolService.getProxiesFromRedis();

        if (redisProxies.isEmpty()) {
            log.info("Redis中没有可用代理，开始爬取新代理");
            // 如果Redis中没有可用代理，则爬取所有页面的代理
            List<ProxyInfo> newProxies = proxyPoolService.crawlAndSaveAllProxies(DEFAULT_PROXY_BASE_URL);

            if (!newProxies.isEmpty()) {
                // 清空并重新添加代理
                availableProxies.clear();
                availableProxies.addAll(newProxies);
                // 重置索引
                currentProxyIndex.set(0);
                log.info("代理列表已刷新，共获取到{}个可用代理", availableProxies.size());
            } else {
                log.warn("未获取到任何可用代理");
            }
        } else {
            // 使用Redis中的代理更新本地缓存
            availableProxies.clear();
            availableProxies.addAll(redisProxies);
            currentProxyIndex.set(0);
            log.info("从Redis加载了{}个可用代理", redisProxies.size());

            // 验证Redis中的代理是否仍然可用
            validateProxiesFromRedis();
        }
    }

    /**
     * 验证Redis中的代理是否可用
     * 移除不可用的代理
     */
    private void validateProxiesFromRedis() {
        log.info("开始验证Redis中的代理...");
        List<ProxyInfo> invalidProxies = new ArrayList<>();

        for (ProxyInfo proxy : availableProxies) {
            if (!testProxy(proxy)) {
                // 如果代理不可用，添加到无效代理列表
                invalidProxies.add(proxy);
                // 从Redis中移除
                proxyPoolService.removeProxyFromRedis(proxy);
            }
        }

        if (!invalidProxies.isEmpty()) {
            // 从本地缓存中移除无效代理
            availableProxies.removeAll(invalidProxies);
            log.info("移除了{}个不可用代理，剩余{}个可用代理", invalidProxies.size(), availableProxies.size());

            // 如果可用代理太少，尝试爬取新代理
            if (availableProxies.size() < 3) {
                log.info("可用代理数量低于阈值，开始爬取新代理");
                List<ProxyInfo> newProxies = proxyPoolService.crawlAndSaveAllProxies(DEFAULT_PROXY_BASE_URL);
                if (!newProxies.isEmpty()) {
                    availableProxies.addAll(newProxies);
                    log.info("新增{}个可用代理，当前总共{}个可用代理", newProxies.size(), availableProxies.size());
                }
            }
        } else {
            log.info("Redis中的所有代理均可用");
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
            log.warn("代理池为空，尝试刷新");
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
        log.info("获取代理: {}:{} (类型: {})", proxy.getIp(), proxy.getPort(), proxy.getType());
        return proxy;
    }

    /**
     * 获取随机代理
     *
     * @return 随机代理信息或null（无可用代理时）
     */
    public ProxyInfo getRandomProxy() {
        if (availableProxies.isEmpty()) {
            log.warn("代理池为空，尝试刷新");
            refreshProxyList();
            if (availableProxies.isEmpty()) {
                return null;
            }
        }

        // 随机选择一个代理
        int index = random.nextInt(availableProxies.size());
        ProxyInfo proxy = availableProxies.get(index);
        log.info("获取随机代理: {}:{} (类型: {})", proxy.getIp(), proxy.getPort(), proxy.getType());
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
            log.info("标记代理为不可用: {}:{}", proxy.getIp(), proxy.getPort());
            availableProxies.remove(proxy);

            // 从Redis中删除不可用代理
            proxyPoolService.removeProxyFromRedis(proxy);

            // 如果可用代理太少，尝试刷新
            if (availableProxies.size() < 3) {
                log.info("可用代理数量低于阈值，尝试刷新代理列表");
                refreshProxyList();
            }
        }
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
        String proxyType = proxy.getType().toLowerCase();
        
        // 仅处理HTTP代理
        if (!"http".equals(proxyType)) {
            log.info("跳过非HTTP代理 {}:{} (类型: {})", ip, port, proxyType);
            return false;
        }

        try {
            // 使用HTTP代理
            Proxy javaProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, Integer.parseInt(port)));

            // 创建连接
            URL url = new URL(TEST_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(javaProxy);
            connection.setConnectTimeout(TIMEOUT * 1000);
            connection.setReadTimeout(TIMEOUT * 1000);

            // 获取响应代码
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                log.info("HTTP代理 {}:{} 可用，响应状态码: {}", ip, port, responseCode);
                return true;
            } else {
                log.info("HTTP代理 {}:{} 返回状态码: {}", ip, port, responseCode);
                return false;
            }
        } catch (Exception e) {
            log.info("HTTP代理 {}:{} 不可用: {}", ip, port, e.getMessage());
            return false;
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
     * @return 可用代理列表
     */
    public List<ProxyInfo> getAllProxies() {
        return Collections.unmodifiableList(new ArrayList<>(availableProxies));
    }
    
    /**
     * 强制刷新代理池
     * 重新爬取所有页面的代理
     */
    public void forceRefreshProxyPool() {
        log.info("强制刷新代理池...");
        List<ProxyInfo> newProxies = proxyPoolService.crawlAndSaveAllProxies(DEFAULT_PROXY_BASE_URL);
        
        if (!newProxies.isEmpty()) {
            // 清空并重新添加代理
            availableProxies.clear();
            availableProxies.addAll(newProxies);
            // 重置索引
            currentProxyIndex.set(0);
            log.info("代理池已强制刷新，共获取到{}个可用代理", availableProxies.size());
        } else {
            log.warn("强制刷新未获取到任何可用代理");
        }
    }
} 