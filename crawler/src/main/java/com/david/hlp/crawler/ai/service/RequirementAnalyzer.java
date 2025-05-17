package com.david.hlp.crawler.ai.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.david.hlp.crawler.ai.entity.UserSimilarity;
import lombok.RequiredArgsConstructor;

import com.david.hlp.crawler.ai.entity.MiniJobDetail;
import com.david.hlp.crawler.ai.mapper.TJobDetailMapper;
import java.util.ArrayList;
import com.david.hlp.crawler.ai.entity.CompareQueue;

@Service
@RequiredArgsConstructor
public class RequirementAnalyzer {
    private final TJobDetailMapper tJobDetailMapper;
    private final AIExtractionOfJobRequirements aiExtractionOfJobRequirements;
    private final FormatDataService formatDataService;
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

    public List<UserSimilarity> getUserSimilarity(String resume, String city, String position) {
        List<MiniJobDetail> jobDetailList = null;
        CompareQueue<UserSimilarity> compareQueue = new CompareQueue<>(10,
                Comparator.comparing(UserSimilarity::getSimilarity).reversed());
        do {
            jobDetailList = tJobDetailMapper.selectMiniJobDetail(city, position, currentIndex, limit);
            jobDetailList.sort(Comparator.comparing(MiniJobDetail::getId));
            for (MiniJobDetail jobDetail : jobDetailList) {
                int similarity = aiExtractionOfJobRequirements.getUserSimilarity(jobDetail.getDetailData(), resume);
                UserSimilarity userSimilarity = UserSimilarity.builder()
                        .id(jobDetail.getId())
                        .similarity(similarity)
                        .miniJobDetail(jobDetail)
                        .build();
                compareQueue.add(userSimilarity);
                currentIndex = jobDetail.getId();
            }
        } while (jobDetailList.isEmpty());
        List<UserSimilarity> resultList = new ArrayList<>();
        while (!compareQueue.isEmpty()) {
            UserSimilarity userSimilarity = compareQueue.poll();
            userSimilarity.setJobAnalysisData(formatDataService.convertToJobAnalysisData(userSimilarity.getMiniJobDetail()));
            userSimilarity.setMiniJobDetail(null);
            resultList.add(userSimilarity);
        }
        return resultList;
    }
}
