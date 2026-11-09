package com.david.hlp.crawler.ai.controller;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.david.hlp.crawler.ai.service.*;
import com.david.hlp.crawler.ai.entity.*;

@RestController
@RequiredArgsConstructor
public class AIRequirementsController {
    private final RequirementAnalyzer analyzer;
    private final AuthorizedResume resumes;
    @PostMapping("/api/v1/ai/get-user-similarity")
    public List<UserSimilarity> legacy(@RequestBody SimilarityRequest input,HttpServletRequest request){
        return analyzer.getUserSimilarity(resumes.resolve(input,request),input.getCity(),input.getPosition());
    }
    @PostMapping("/api/v2/ai/matches")
    public ObjectNode matches(@RequestBody SimilarityRequest input,HttpServletRequest request){
        return analyzer.matches(resumes.resolve(input,request),input.getCity(),input.getPosition());
    }
}
