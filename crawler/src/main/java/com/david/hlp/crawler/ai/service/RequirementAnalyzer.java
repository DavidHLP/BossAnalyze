package com.david.hlp.crawler.ai.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.david.hlp.crawler.ai.entity.MiniJobDetail;
import com.david.hlp.crawler.ai.mapper.TJobDetailMapper;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class RequirementAnalyzer {
    private final TJobDetailMapper tJobDetailMapper;
    private final AIExtractionOfJobRequirements aiExtractionOfJobRequirements;
    private Long currentIndex = 0L;
    @Value("${limit:20}")
    private Integer limit = 20;
    private StringBuffer requirementBuilder = new StringBuffer();

    public  List<String> getCoreRequirements(String cityName, String positionName) {
        List<MiniJobDetail> jobDetailList = null;
        List<String> coreRequirementList = new ArrayList<>();
        do {
            jobDetailList = tJobDetailMapper.selectJobRequirements(cityName, positionName, currentIndex, limit);
            jobDetailList.sort(Comparator.comparing(MiniJobDetail::getId));
            for (MiniJobDetail jobDetail : jobDetailList) {
                String jobRequirement = jobDetail.getJobRequirements();
                requirementBuilder.append(jobRequirement);
                currentIndex = jobDetail.getId();
            }
            for (String requirement : coreRequirementList) {
                requirementBuilder.append(requirement);
            }
            coreRequirementList = aiExtractionOfJobRequirements.coreRequirementAnalyzer(requirementBuilder.toString());
        } while (jobDetailList.isEmpty());
        return coreRequirementList;
    }
}
