package com.david.hlp.commons.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 工具类
 * 专注于 Redis 数据类型的存储、获取、删除操作
 * 所有数据以 JSON 格式存储，自动序列化/反序列化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheUtil {
    private final RedisTemplate<String, Object> redisTemplate;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    /**
     * 使用lua脚本删除
     */
    public void useLuaDelete(String key, String value) {
        redisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(key),
                value);
    }

    /**
     * redis自增
     */
    public Long increment(String keyPrefix, String date) {
        return (Long) redisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
    }

    // ==============================String=============================

    /**
     * 缓存基本的对象（JSON格式）
     * 
     * @param key   缓存的键
     * @param value 缓存的值
     * @param clazz 值的类型
     */
    public <T> void setCacheObject(final String key, final T value, Class<T> clazz) {
        if (value != null) {
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value));
        }
    }

    /**
     * 获取缓存的基本对象
     *
     * @param key   缓存的键
     * @param clazz 返回值的类型
     * @return 缓存的对象
     */
    public <T> T getCacheObject(final String key, Class<T> clazz) {
        String jsonStr = (String) redisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        try {
            return JSONUtil.toBean(jsonStr, clazz);
        } catch (Exception e) {
            log.error("反序列化缓存对象失败, key: {}, clazz: {}", key, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 缓存基本的对象（带过期时间，JSON格式）
     * 
     * @param key      缓存的键
     * @param value    缓存的值
     * @param clazz    值的类型
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public <T> Boolean setCacheObject(final String key, final T value, Class<T> clazz,
            final long timeout, final TimeUnit timeUnit) {
        if (value != null) {
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), timeout, timeUnit);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * 删除缓存对象
     *
     * @param key 缓存的键
     * @return 是否删除成功
     */
    public Boolean deleteObject(final String key) {
        if (key == null) {
            return Boolean.FALSE;
        }
        return redisTemplate.delete(key);
    }

    /**
     * 根据模式删除多个缓存键
     *
     * @param pattern 键的模式，支持通配符 *
     * @return 删除的键数量
     */
    public Long deleteByPattern(final String pattern) {
        if (StrUtil.isBlank(pattern)) {
            return 0L;
        }

        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }

        return redisTemplate.delete(keys);
    }

    // ==============================Hash=============================

    /**
     * 缓存Hash数据（JSON格式）
     * 
     * @param key     缓存的键
     * @param dataMap 数据Map
     * @param clazz   值的类型
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap, Class<T> clazz) {
        if (dataMap != null && !dataMap.isEmpty()) {
            Map<String, String> jsonMap = dataMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> JSONUtil.toJsonStr(entry.getValue())));
            redisTemplate.opsForHash().putAll(key, jsonMap);
        }
    }

    /**
     * 获取整个Hash缓存
     *
     * @param key   缓存的键
     * @param clazz 值的类型
     * @return Hash对象
     */
    public <T> Map<String, T> getCacheMap(final String key, Class<T> clazz) {
        Map<String, String> jsonMap = redisTemplate.<String, String>opsForHash().entries(key);
        if (jsonMap == null || jsonMap.isEmpty()) {
            return Collections.emptyMap();
        }

        return jsonMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            try {
                                return JSONUtil.toBean(entry.getValue(), clazz);
                            } catch (Exception e) {
                                log.error("反序列化Hash值失败, key: {}, field: {}, clazz: {}",
                                        key, entry.getKey(), clazz.getName(), e);
                                return null;
                            }
                        }));
    }

    /**
     * 获取Hash中的单个字段
     *
     * @param key   缓存的键
     * @param field 字段名
     * @param clazz 值的类型
     * @return 字段值
     */
    public <T> T getCacheMapValue(final String key, final String field, Class<T> clazz) {
        String jsonStr = (String) redisTemplate.opsForHash().get(key, field);
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        try {
            return JSONUtil.toBean(jsonStr, clazz);
        } catch (Exception e) {
            log.error("反序列化Hash字段失败, key: {}, field: {}, clazz: {}", key, field, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 设置Hash中的单个字段
     *
     * @param key   缓存的键
     * @param field 字段名
     * @param value 字段值
     * @param clazz 值的类型
     */
    public <T> void setCacheMapValue(final String key, final String field, final T value, Class<T> clazz) {
        if (value != null) {
            redisTemplate.opsForHash().put(key, field, JSONUtil.toJsonStr(value));
        }
    }

    /**
     * 缓存Hash数据（带过期时间，JSON格式）
     * 
     * @param key      缓存的键
     * @param dataMap  数据Map
     * @param clazz    值的类型
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public <T> Boolean setCacheMap(final String key, final Map<String, T> dataMap, Class<T> clazz,
            final long timeout, final TimeUnit timeUnit) {
        if (dataMap != null && !dataMap.isEmpty()) {
            setCacheMap(key, dataMap, clazz);
            return expire(key, timeout, timeUnit);
        }
        return Boolean.FALSE;
    }

    /**
     * 删除Hash中的指定字段
     *
     * @param key   缓存的键
     * @param hKeys 要删除的字段数组
     * @return 删除的字段数量
     */
    public Long deleteHashKeys(final String key, final Object... hKeys) {
        if (key == null || hKeys == null || hKeys.length == 0) {
            return 0L;
        }
        return redisTemplate.opsForHash().delete(key, hKeys);
    }

    // ==============================List=============================

    /**
     * 缓存List数据（JSON格式）
     * 
     * @param key      缓存的键
     * @param dataList 数据列表
     * @param clazz    元素类型
     */
    public <T> Long setCacheList(final String key, final List<T> dataList, Class<T> clazz) {
        if (dataList != null && !dataList.isEmpty()) {
            List<String> jsonList = dataList.stream()
                    .map(JSONUtil::toJsonStr)
                    .collect(Collectors.toList());
            return redisTemplate.opsForList().rightPushAll(key, jsonList);
        }
        return 0L;
    }

    /**
     * 获取List缓存
     *
     * @param key   缓存的键
     * @param clazz 元素类型
     * @return List对象
     */
    public <T> List<T> getCacheList(final String key, Class<T> clazz) {
        List<Object> objectList = redisTemplate.opsForList().range(key, 0, -1);
        if (objectList == null || objectList.isEmpty()) {
            return Collections.emptyList();
        }

        return objectList.stream()
                .map(obj -> {
                    try {
                        String jsonStr = (String) obj;
                        return JSONUtil.toBean(jsonStr, clazz);
                    } catch (Exception e) {
                        log.error("反序列化List元素失败, key: {}, clazz: {}", key, clazz.getName(), e);
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 缓存List数据（带过期时间，JSON格式）
     * 
     * @param key      缓存的键
     * @param dataList 数据列表
     * @param clazz    元素类型
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public <T> Boolean setCacheList(final String key, final List<T> dataList, Class<T> clazz,
            final long timeout, final TimeUnit timeUnit) {
        if (dataList != null && !dataList.isEmpty()) {
            setCacheList(key, dataList, clazz);
            return expire(key, timeout, timeUnit);
        }
        return Boolean.FALSE;
    }

    /**
     * 向List左侧添加元素
     * 
     * @param key   缓存的键
     * @param value 要添加的值
     * @param clazz 值的类型
     * @return 添加后的列表长度
     */
    public <T> Long leftPushToList(final String key, final T value, Class<T> clazz) {
        if (value != null) {
            return redisTemplate.opsForList().leftPush(key, JSONUtil.toJsonStr(value));
        }
        return 0L;
    }

    /**
     * 向List右侧添加元素
     * 
     * @param key   缓存的键
     * @param value 要添加的值
     * @param clazz 值的类型
     * @return 添加后的列表长度
     */
    public <T> Long rightPushToList(final String key, final T value, Class<T> clazz) {
        if (value != null) {
            return redisTemplate.opsForList().rightPush(key, JSONUtil.toJsonStr(value));
        }
        return 0L;
    }

    /**
     * 从List左侧弹出元素
     * 
     * @param key   缓存的键
     * @param clazz 值的类型
     * @return 弹出的元素
     */
    public <T> T leftPopFromList(final String key, Class<T> clazz) {
        String jsonStr = (String) redisTemplate.opsForList().leftPop(key);
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        try {
            return JSONUtil.toBean(jsonStr, clazz);
        } catch (Exception e) {
            log.error("反序列化List元素失败, key: {}, clazz: {}", key, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 从List右侧弹出元素
     * 
     * @param key   缓存的键
     * @param clazz 值的类型
     * @return 弹出的元素
     */
    public <T> T rightPopFromList(final String key, Class<T> clazz) {
        String jsonStr = (String) redisTemplate.opsForList().rightPop(key);
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        try {
            return JSONUtil.toBean(jsonStr, clazz);
        } catch (Exception e) {
            log.error("反序列化List元素失败, key: {}, clazz: {}", key, clazz.getName(), e);
            return null;
        }
    }

    /**
     * 裁剪List，只保留指定区间内的元素
     *
     * @param key   缓存的键
     * @param start 开始位置
     * @param end   结束位置
     */
    public void trimList(final String key, long start, long end) {
        if (key != null) {
            redisTemplate.opsForList().trim(key, start, end);
        }
    }

    // ==============================Set=============================

    /**
     * 缓存Set数据（JSON格式）
     * 
     * @param key     缓存的键
     * @param dataSet 数据集合
     * @param clazz   元素类型
     */
    public <T> Long setCacheSet(final String key, final Set<T> dataSet, Class<T> clazz) {
        if (dataSet != null && !dataSet.isEmpty()) {
            String[] jsonArray = dataSet.stream()
                    .map(JSONUtil::toJsonStr)
                    .toArray(String[]::new);
            return redisTemplate.opsForSet().add(key, (Object[]) jsonArray);
        }
        return 0L;
    }

    /**
     * 获取Set缓存
     *
     * @param key   缓存的键
     * @param clazz 元素类型
     * @return Set对象
     */
    public <T> Set<T> getCacheSet(final String key, Class<T> clazz) {
        Set<Object> objectSet = redisTemplate.opsForSet().members(key);
        if (objectSet == null || objectSet.isEmpty()) {
            return Collections.emptySet();
        }

        return objectSet.stream()
                .map(obj -> {
                    try {
                        String jsonStr = (String) obj;
                        return JSONUtil.toBean(jsonStr, clazz);
                    } catch (Exception e) {
                        log.error("反序列化Set元素失败, key: {}, clazz: {}", key, clazz.getName(), e);
                        return null;
                    }
                })
                .collect(Collectors.toSet());
    }

    /**
     * 缓存Set数据（带过期时间，JSON格式）
     * 
     * @param key      缓存的键
     * @param dataSet  数据集合
     * @param clazz    元素类型
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public <T> Boolean setCacheSet(final String key, final Set<T> dataSet, Class<T> clazz,
            final long timeout, final TimeUnit timeUnit) {
        if (dataSet != null && !dataSet.isEmpty()) {
            setCacheSet(key, dataSet, clazz);
            return expire(key, timeout, timeUnit);
        }
        return Boolean.FALSE;
    }

    /**
     * 向Set中添加元素
     * 
     * @param key   缓存的键
     * @param value 要添加的值
     * @param clazz 值的类型
     * @return 添加的元素数量
     */
    public <T> Long addToSet(final String key, final T value, Class<T> clazz) {
        if (value != null) {
            return redisTemplate.opsForSet().add(key, JSONUtil.toJsonStr(value));
        }
        return 0L;
    }

    /**
     * 从Set中移除元素
     *
     * @param key   缓存的键
     * @param value 要移除的值
     * @param clazz 值的类型
     * @return 移除的元素数量
     */
    public <T> Long removeFromSet(final String key, final T value, Class<T> clazz) {
        if (value != null) {
            return redisTemplate.opsForSet().remove(key, JSONUtil.toJsonStr(value));
        }
        return 0L;
    }

    /**
     * 从Set中随机移除并返回一个元素
     *
     * @param key   缓存的键
     * @param clazz 元素类型
     * @return 被移除的元素
     */
    public <T> T popFromSet(final String key, Class<T> clazz) {
        String jsonStr = (String) redisTemplate.opsForSet().pop(key);
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        try {
            return JSONUtil.toBean(jsonStr, clazz);
        } catch (Exception e) {
            log.error("反序列化Set元素失败, key: {}, clazz: {}", key, clazz.getName(), e);
            return null;
        }
    }

    // ==============================ZSet=============================

    /**
     * 缓存ZSet数据（JSON格式）
     * 
     * @param key     缓存的键
     * @param dataSet 数据集合
     * @param score   分数
     * @param clazz   元素类型
     */
    public <T> Boolean setCacheZSet(final String key, final Set<T> dataSet, final double score, Class<T> clazz) {
        if (dataSet != null && !dataSet.isEmpty()) {
            Set<ZSetOperations.TypedTuple<Object>> tuples = dataSet.stream()
                    .map(value -> new DefaultTypedTuple<Object>(JSONUtil.toJsonStr(value), score))
                    .collect(Collectors.toSet());
            Long result = redisTemplate.opsForZSet().add(key, tuples);
            return result != null && result > 0L;
        }
        return Boolean.FALSE;
    }

    /**
     * 向ZSet中添加单个元素
     * 
     * @param key   缓存的键
     * @param value 要添加的值
     * @param score 分数
     * @param clazz 值的类型
     * @return 是否添加成功
     */
    public <T> Boolean addToZSet(final String key, final T value, final double score, Class<T> clazz) {
        if (value != null) {
            return redisTemplate.opsForZSet().add(key, JSONUtil.toJsonStr(value), score);
        }
        return Boolean.FALSE;
    }

    /**
     * 获取ZSet缓存（按分数升序）
     *
     * @param key   缓存的键
     * @param clazz 元素类型
     * @return Set对象
     */
    public <T> Set<T> getCacheZSet(final String key, Class<T> clazz) {
        Set<Object> objectSet = redisTemplate.opsForZSet().range(key, 0, -1);
        if (objectSet == null || objectSet.isEmpty()) {
            return Collections.emptySet();
        }

        return objectSet.stream()
                .map(obj -> {
                    try {
                        String jsonStr = (String) obj;
                        return JSONUtil.toBean(jsonStr, clazz);
                    } catch (Exception e) {
                        log.error("反序列化ZSet元素失败, key: {}, clazz: {}", key, clazz.getName(), e);
                        return null;
                    }
                })
                .collect(Collectors.toSet());
    }

    /**
     * 获取ZSet缓存（按分数范围）
     *
     * @param key   缓存的键
     * @param min   最小分数
     * @param max   最大分数
     * @param clazz 元素类型
     * @return Set对象
     */
    public <T> Set<T> getCacheZSetByScore(final String key, double min, double max, Class<T> clazz) {
        Set<Object> objectSet = redisTemplate.opsForZSet().rangeByScore(key, min, max);
        if (objectSet == null || objectSet.isEmpty()) {
            return Collections.emptySet();
        }

        return objectSet.stream()
                .map(obj -> {
                    try {
                        String jsonStr = (String) obj;
                        return JSONUtil.toBean(jsonStr, clazz);
                    } catch (Exception e) {
                        log.error("反序列化ZSet元素失败, key: {}, clazz: {}", key, clazz.getName(), e);
                        return null;
                    }
                })
                .collect(Collectors.toSet());
    }

    /**
     * 缓存ZSet数据（带过期时间，JSON格式）
     * 
     * @param key      缓存的键
     * @param dataSet  数据集合
     * @param score    分数
     * @param clazz    元素类型
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public <T> Boolean setCacheZSet(final String key, final Set<T> dataSet, final double score, Class<T> clazz,
            final long timeout, final TimeUnit timeUnit) {
        if (dataSet != null && !dataSet.isEmpty()) {
            setCacheZSet(key, dataSet, score, clazz);
            return expire(key, timeout, timeUnit);
        }
        return Boolean.FALSE;
    }

    /**
     * 从ZSet中移除元素
     *
     * @param key   缓存的键
     * @param value 要移除的值
     * @param clazz 值的类型
     * @return 移除的元素数量
     */
    public <T> Long removeFromZSet(final String key, final T value, Class<T> clazz) {
        if (value != null) {
            return redisTemplate.opsForZSet().remove(key, JSONUtil.toJsonStr(value));
        }
        return 0L;
    }

    /**
     * 移除ZSet中指定分数区间的元素
     *
     * @param key 缓存的键
     * @param min 最小分数
     * @param max 最大分数
     * @return 移除的元素数量
     */
    public Long removeFromZSetByScore(final String key, double min, double max) {
        if (key == null) {
            return 0L;
        }
        return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    }

    /**
     * 移除ZSet中指定排名区间的元素
     *
     * @param key   缓存的键
     * @param start 开始排名
     * @param end   结束排名
     * @return 移除的元素数量
     */
    public Long removeFromZSetByRank(final String key, long start, long end) {
        if (key == null) {
            return 0L;
        }
        return redisTemplate.opsForZSet().removeRange(key, start, end);
    }

    // ==============================Other=============================

    /**
     * 设置有效时间
     */
    private boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /**
     * 获取有效时间
     */
    public long getExpire(final String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断 key是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

}