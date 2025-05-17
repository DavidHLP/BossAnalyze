package com.david.hlp.crawler.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import com.david.hlp.crawler.ai.service.RequirementAnalyzer;
import com.david.hlp.crawler.ai.entity.ResumeRequest;
import com.david.hlp.crawler.ai.entity.UserSimilarity;
import org.springframework.web.bind.annotation.RequestBody;
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIRequirementsController {
    private final RequirementAnalyzer requirementAnalyzer;

    @GetMapping("/get-core-requirements")
    public List<String> getCoreRequirements(@RequestParam("city") String city, @RequestParam("position") String position) {
        List<String> coreRequirements = requirementAnalyzer.getCoreRequirements(city, position);
        return coreRequirements;
    }

    @PostMapping("/get-user-similarity")
    public List<UserSimilarity> getUserSimilarity(@RequestBody ResumeRequest resumeRequest) {
        List<UserSimilarity> userSimilarityList = requirementAnalyzer.getUserSimilarity(resumeRequest.getResume(), resumeRequest.getCity(), resumeRequest.getPosition());
        return userSimilarityList;
    }
}

