package com.david.hlp.web.ai.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.david.hlp.web.ai.model.UserSimilarity;
import com.david.hlp.web.ai.model.SimilarityRequest;
import com.david.hlp.web.ai.model.AsyncResponse;
import com.david.hlp.web.common.util.RedisCache;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncAIService {

    private final RestTemplate restTemplate;
    private final RedisCache redisCache;

    // AI服务配置
    @Value("${ai.service.base-url:http://localhost:8081/api/v1/ai}")
    private String aiServiceBaseUrl;

    @Value("${ai.service.similarity-endpoint:/get-user-similarity}")
    private String similarityEndpoint;

    @Value("${ai.service.timeout:30000}")
    private int requestTimeout;

    // 缓存相关常量
    private static final int CACHE_EXPIRE_TIME = 3600; // 1小时
    private static final int ERROR_CACHE_EXPIRE_TIME = 300; // 5分钟
    private static final int PROCESSING_CACHE_EXPIRE_TIME = 600; // 10分钟

    // 缓存键前缀
    private static final String CACHE_KEY_PREFIX = "ai:similarity";
    private static final String ERROR_SUFFIX = ":error";
    private static final String PROCESSING_SUFFIX = ":processing";

    // 响应消息常量
    private static final String LOADING_MESSAGE = "正在为您智能匹配职位,请稍候...";
    private static final String PARAM_ERROR_MESSAGE = "简历ID不能为空";
    private static final String EMPTY_RESULT_MESSAGE = "AI服务返回空结果";
    private static final String SERVICE_ERROR_PREFIX = "AI服务调用失败: ";

    /**
     * 处理用户相似度请求
     * 
     * @param similarityRequest 相似度请求参数
     * @return 异步响应结果
     */
    public AsyncResponse<List<UserSimilarity>> handleUserSimilarityRequest(SimilarityRequest similarityRequest) {
                // 参数验证
        if (!isValidRequest(similarityRequest)) {
            log.warn("请求参数验证失败,resumeId: {}",
                    similarityRequest != null ? similarityRequest.getResumeId() : null);
            return AsyncResponse.error(PARAM_ERROR_MESSAGE, null);
        }

        // 生成缓存键
        String cacheKey = generateCacheKey(similarityRequest);

        // 检查Redis缓存
        List<UserSimilarity> cachedData = getCachedResult(cacheKey);
        if (cachedData != null) {
            return AsyncResponse.completed(cachedData, null);
        }

        // 检查是否有错误状态
        String errorMessage = getErrorStatus(cacheKey);
        if (errorMessage != null) {
            log.error("发现错误状态,cacheKey: {}, error: {}", cacheKey, errorMessage);
            return AsyncResponse.error(errorMessage, null);
        }

        // 检查是否正在处理中
        if (!isProcessing(cacheKey)) {
            // 设置处理中标记
            setProcessingFlag(cacheKey);

            // 异步调用AI服务
            processAIRequestAsync(similarityRequest, cacheKey);
        }

        // 返回"请稍等"响应
        return AsyncResponse.loading(LOADING_MESSAGE, null);
    }

        /**
     * 验证请求参数
     */
    private boolean isValidRequest(SimilarityRequest request) {
        return request != null && 
               request.getResumeId() != null && 
               !request.getResumeId().trim().isEmpty();
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(SimilarityRequest request) {
        return String.format("%s:%s", CACHE_KEY_PREFIX, request.getResumeId());
    }

    /**
     * 获取缓存结果
     */
    private List<UserSimilarity> getCachedResult(String cacheKey) {
        try {
            return redisCache.getCacheObject(cacheKey);
        } catch (Exception e) {
            log.error("获取缓存数据失败,cacheKey: {}, error: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    /**
     * 获取错误状态
     */
    private String getErrorStatus(String cacheKey) {
        try {
            return redisCache.getCacheObject(cacheKey + ERROR_SUFFIX);
        } catch (Exception e) {
            log.error("获取错误状态失败,cacheKey: {}, error: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    /**
     * 检查是否正在处理中
     */
    private boolean isProcessing(String cacheKey) {
        try {
            String processingFlag = redisCache.getCacheObject(cacheKey + PROCESSING_SUFFIX);
            return processingFlag != null;
        } catch (Exception e) {
            log.error("检查处理状态失败,cacheKey: {}, error: {}", cacheKey, e.getMessage());
            return false;
        }
    }

    /**
     * 设置处理中标记
     */
    private void setProcessingFlag(String cacheKey) {
        try {
            redisCache.setCacheObject(cacheKey + PROCESSING_SUFFIX, "true",
                    PROCESSING_CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("设置处理中标记失败,cacheKey: {}, error: {}", cacheKey, e.getMessage());
        }
    }

    @Async
    public void processAIRequestAsync(SimilarityRequest similarityRequest, String cacheKey) {
        try {
            URI url = UriComponentsBuilder.fromUriString(aiServiceBaseUrl)
                    .path(similarityEndpoint)
                    .build().encode().toUri();

            ResponseEntity<List<UserSimilarity>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(similarityRequest),
                    new ParameterizedTypeReference<List<UserSimilarity>>() {
                    });

            List<UserSimilarity> response = responseEntity.getBody();

            if (response != null) {
                // 将结果存储到Redis
                redisCache.setCacheObject(cacheKey, response, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);

                // 清除处理中标记
                redisCache.deleteObject(cacheKey + PROCESSING_SUFFIX);
            } else {
                log.error("AI服务返回空结果,cacheKey: {}", cacheKey);
                storeErrorStatus(cacheKey, EMPTY_RESULT_MESSAGE);
            }

        } catch (Exception e) {
            log.error("异步处理AI请求失败,cacheKey: {}, error: {}", cacheKey, e.getMessage());
            storeErrorStatus(cacheKey, SERVICE_ERROR_PREFIX + e.getMessage());
        }
    }

    /**
     * 存储错误状态
     */
    private void storeErrorStatus(String cacheKey, String errorMessage) {
        try {
            redisCache.setCacheObject(cacheKey + ERROR_SUFFIX, errorMessage,
                    ERROR_CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
            // 清除处理中标记
            redisCache.deleteObject(cacheKey + PROCESSING_SUFFIX);
        } catch (Exception e) {
            log.error("存储错误状态失败,cacheKey: {}, error: {}", cacheKey, e.getMessage());
        }
    }

    @Async
    public void processAIRequest(SimilarityRequest similarityRequest, String cacheKey) {
        processAIRequestAsync(similarityRequest, cacheKey);
    }
}