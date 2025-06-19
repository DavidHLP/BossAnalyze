package com.david.hlp.commons.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的布隆过滤器工具类
 * 用于解决缓存穿透问题，快速判断某个元素是否可能存在
 * 
 * @author david
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 默认容量 */
    private static final long DEFAULT_CAPACITY = 1000000L;

    /** 默认错误率 */
    private static final double DEFAULT_ERROR_RATE = 0.01;

    /** 布隆过滤器前缀 */
    private static final String BLOOM_FILTER_PREFIX = "bloom_filter:";

    /** 配置信息前缀 */
    private static final String CONFIG_PREFIX = "bloom_config:";

    /**
     * 布隆过滤器配置类
     */
    public static class BloomFilterConfig {
        private final long capacity; // 预期元素数量
        private final double errorRate; // 误判率
        private final int hashFunctions; // 哈希函数数量
        private final long bitArraySize; // 位数组大小

        public BloomFilterConfig(long capacity, double errorRate) {
            this.capacity = capacity;
            this.errorRate = errorRate;
            this.bitArraySize = calculateBitArraySize(capacity, errorRate);
            this.hashFunctions = calculateHashFunctions(capacity, bitArraySize);
        }

        private static long calculateBitArraySize(long capacity, double errorRate) {
            return (long) (-capacity * Math.log(errorRate) / (Math.log(2) * Math.log(2)));
        }

        private static int calculateHashFunctions(long capacity, long bitArraySize) {
            return Math.max(1, (int) Math.round((double) bitArraySize / capacity * Math.log(2)));
        }

        // Getters
        public long getCapacity() {
            return capacity;
        }

        public double getErrorRate() {
            return errorRate;
        }

        public int getHashFunctions() {
            return hashFunctions;
        }

        public long getBitArraySize() {
            return bitArraySize;
        }
    }

    /**
     * 初始化布隆过滤器
     * 
     * @param filterName 过滤器名称
     * @param capacity   预期元素数量
     * @param errorRate  误判率
     */
    public void initBloomFilter(String filterName, long capacity, double errorRate) {
        BloomFilterConfig config = new BloomFilterConfig(capacity, errorRate);
        String configKey = CONFIG_PREFIX + filterName;

        redisTemplate.opsForHash().put(configKey, "capacity", config.getCapacity());
        redisTemplate.opsForHash().put(configKey, "errorRate", config.getErrorRate());
        redisTemplate.opsForHash().put(configKey, "hashFunctions", config.getHashFunctions());
        redisTemplate.opsForHash().put(configKey, "bitArraySize", config.getBitArraySize());

        // 设置配置过期时间（30天）
        redisTemplate.expire(configKey, 30, TimeUnit.DAYS);

        log.info("布隆过滤器初始化成功: filterName={}, capacity={}, errorRate={}, bitArraySize={}, hashFunctions={}",
                filterName, capacity, errorRate, config.getBitArraySize(), config.getHashFunctions());
    }

    /**
     * 使用默认参数初始化布隆过滤器
     * 
     * @param filterName 过滤器名称
     */
    public void initBloomFilter(String filterName) {
        initBloomFilter(filterName, DEFAULT_CAPACITY, DEFAULT_ERROR_RATE);
    }

    /**
     * 添加元素到布隆过滤器
     * 
     * @param filterName 过滤器名称
     * @param element    要添加的元素
     * @return 是否添加成功
     */
    public boolean add(String filterName, String element) {
        if (element == null || element.trim().isEmpty()) {
            return false;
        }

        BloomFilterConfig config = getConfig(filterName);
        if (config == null) {
            log.warn("布隆过滤器配置不存在，尝试使用默认配置: filterName={}", filterName);
            initBloomFilter(filterName);
            config = getConfig(filterName);
        }

        try {
            String bloomKey = BLOOM_FILTER_PREFIX + filterName;
            long[] hashes = generateHashes(element, config.getHashFunctions(), config.getBitArraySize());

            for (long hash : hashes) {
                redisTemplate.opsForValue().setBit(bloomKey, hash, true);
            }

            // 设置过期时间（7天）
            redisTemplate.expire(bloomKey, 7, TimeUnit.DAYS);

            return true;
        } catch (Exception e) {
            log.error("添加元素到布隆过滤器失败: filterName={}, element={}", filterName, element, e);
            return false;
        }
    }

    /**
     * 检查元素是否可能存在于布隆过滤器中
     * 
     * @param filterName 过滤器名称
     * @param element    要检查的元素
     * @return true表示可能存在，false表示肯定不存在
     */
    public boolean mightContain(String filterName, String element) {
        if (element == null || element.trim().isEmpty()) {
            return false;
        }

        BloomFilterConfig config = getConfig(filterName);
        if (config == null) {
            log.debug("布隆过滤器配置不存在: filterName={}", filterName);
            return true; // 配置不存在时，保守地返回true，避免误拦截
        }

        try {
            String bloomKey = BLOOM_FILTER_PREFIX + filterName;
            long[] hashes = generateHashes(element, config.getHashFunctions(), config.getBitArraySize());

            for (long hash : hashes) {
                Boolean bit = redisTemplate.opsForValue().getBit(bloomKey, hash);
                if (bit == null || !bit) {
                    return false; // 任何一个位为0，则肯定不存在
                }
            }

            return true; // 所有位都为1，可能存在
        } catch (Exception e) {
            log.error("检查布隆过滤器元素失败: filterName={}, element={}", filterName, element, e);
            return true; // 出错时保守地返回true
        }
    }

    /**
     * 批量添加元素
     * 
     * @param filterName 过滤器名称
     * @param elements   要添加的元素列表
     * @return 成功添加的元素数量
     */
    public int addBatch(String filterName, List<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (String element : elements) {
            if (add(filterName, element)) {
                successCount++;
            }
        }

        log.info("批量添加元素完成: filterName={}, 总数={}, 成功={}", filterName, elements.size(), successCount);
        return successCount;
    }

    /**
     * 批量检查元素
     * 
     * @param filterName 过滤器名称
     * @param elements   要检查的元素列表
     * @return 可能存在的元素列表
     */
    public List<String> filterExisting(String filterName, List<String> elements) {
        if (elements == null || elements.isEmpty()) {
            return List.of();
        }

        return elements.stream()
                .filter(Objects::nonNull)
                .filter(element -> mightContain(filterName, element))
                .toList();
    }

    /**
     * 清空布隆过滤器
     * 
     * @param filterName 过滤器名称
     */
    public void clear(String filterName) {
        String bloomKey = BLOOM_FILTER_PREFIX + filterName;
        String configKey = CONFIG_PREFIX + filterName;

        redisTemplate.delete(bloomKey);
        redisTemplate.delete(configKey);

        log.info("布隆过滤器已清空: filterName={}", filterName);
    }

    /**
     * 获取布隆过滤器统计信息
     * 
     * @param filterName 过滤器名称
     * @return 统计信息字符串
     */
    public String getStats(String filterName) {
        BloomFilterConfig config = getConfig(filterName);
        if (config == null) {
            return "布隆过滤器不存在: " + filterName;
        }

        return String.format("布隆过滤器[%s] - 容量:%d, 误判率:%.4f, 位数组大小:%d, 哈希函数数:%d",
                filterName, config.getCapacity(), config.getErrorRate(),
                config.getBitArraySize(), config.getHashFunctions());
    }

    /**
     * 获取布隆过滤器配置
     */
    private BloomFilterConfig getConfig(String filterName) {
        try {
            String configKey = CONFIG_PREFIX + filterName;

            Object capacityObj = redisTemplate.opsForHash().get(configKey, "capacity");
            Object errorRateObj = redisTemplate.opsForHash().get(configKey, "errorRate");

            if (capacityObj == null || errorRateObj == null) {
                return null;
            }

            long capacity = Long.parseLong(capacityObj.toString());
            double errorRate = Double.parseDouble(errorRateObj.toString());

            return new BloomFilterConfig(capacity, errorRate);
        } catch (Exception e) {
            log.error("获取布隆过滤器配置失败: filterName={}", filterName, e);
            return null;
        }
    }

    /**
     * 生成多个哈希值
     */
    private long[] generateHashes(String element, int hashFunctions, long bitArraySize) {
        long[] hashes = new long[hashFunctions];

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

            byte[] md5Hash = md5.digest(element.getBytes(StandardCharsets.UTF_8));
            byte[] sha1Hash = sha1.digest(element.getBytes(StandardCharsets.UTF_8));

            long hash1 = bytesToLong(md5Hash);
            long hash2 = bytesToLong(sha1Hash);

            for (int i = 0; i < hashFunctions; i++) {
                long combinedHash = hash1 + (i * hash2);
                hashes[i] = Math.abs(combinedHash % bitArraySize);
            }

        } catch (NoSuchAlgorithmException e) {
            log.error("哈希算法不可用", e);
            // 降级使用简单哈希
            for (int i = 0; i < hashFunctions; i++) {
                hashes[i] = Math.abs((element.hashCode() + i * 31) % bitArraySize);
            }
        }

        return hashes;
    }

    /**
     * 字节数组转长整型
     */
    private long bytesToLong(byte[] bytes) {
        long result = 0;
        for (int i = 0; i < Math.min(8, bytes.length); i++) {
            result = (result << 8) | (bytes[i] & 0xFF);
        }
        return result;
    }
}