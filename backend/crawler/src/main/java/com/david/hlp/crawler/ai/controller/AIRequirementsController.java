package com.david.hlp.crawler.ai.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.david.hlp.crawler.ai.service.RequirementAnalyzer;
import com.david.hlp.crawler.ai.entity.SimilarityRequest;
import com.david.hlp.crawler.ai.entity.UserSimilarity;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIRequirementsController {
    private final RequirementAnalyzer requirementAnalyzer;

    @PostMapping("/get-user-similarity")
    public List<UserSimilarity> getUserSimilarity(@RequestBody SimilarityRequest resumeRequest) {
        List<UserSimilarity> userSimilarityList = requirementAnalyzer.getUserSimilarity(resumeRequest.getResume(), resumeRequest.getCity(), resumeRequest.getPosition());
        return userSimilarityList;
    }
}

