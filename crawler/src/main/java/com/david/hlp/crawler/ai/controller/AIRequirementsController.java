package com.david.hlp.crawler.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import com.david.hlp.crawler.ai.service.RequirementAnalyzer;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIRequirementsController {
    private final RequirementAnalyzer requirementAnalyzer;

    @GetMapping("/get-core-requirements")
    public List<String> getCoreRequirements(@RequestParam(required = false) String city, @RequestParam(required = false) String position) {
        List<String> coreRequirements = requirementAnalyzer.getCoreRequirements(city, position);
        return coreRequirements;
    }
}

