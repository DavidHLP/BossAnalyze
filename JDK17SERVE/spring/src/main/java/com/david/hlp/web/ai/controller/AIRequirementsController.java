package com.david.hlp.web.ai.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.extern.slf4j.Slf4j;

import com.david.hlp.web.ai.model.UserSimilarity;
import com.david.hlp.web.common.enums.ResultCode;
import com.david.hlp.web.common.result.Result;
import com.david.hlp.web.ai.model.SimilarityRequest;
import com.david.hlp.web.ai.model.AsyncResponse;
import com.david.hlp.web.ai.service.AsyncAIService;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIRequirementsController {

        private final AsyncAIService asyncAIService;

        @PostMapping("/get-user-similarity")
        public Result<AsyncResponse<List<UserSimilarity>>> getUserSimilarity(
                        @RequestBody SimilarityRequest similarityRequest) {
                try {
                    if (similarityRequest == null || similarityRequest.getResumeId() == null) {
                        log.warn("简历相似度请求参数无效");
                        return Result.error(ResultCode.BAD_REQUEST, "请求参数无效");
                    }
                    // 调用Service处理业务逻辑
                    AsyncResponse<List<UserSimilarity>> response = asyncAIService
                                    .handleUserSimilarityRequest(similarityRequest);

                    return Result.success(response);
                } catch (Exception e) {
                    log.error("处理简历相似度请求失败,resumeId:{}", 
                        similarityRequest != null ? similarityRequest.getResumeId() : "未知", e);
                    return Result.error(ResultCode.INTERNAL_ERROR, "处理请求失败");
                }
        }
}
