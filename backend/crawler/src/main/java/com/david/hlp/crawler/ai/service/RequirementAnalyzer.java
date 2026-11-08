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
    @Value("${limit:20}")
    private Integer limit = 20;
    public List<UserSimilarity> getUserSimilarity(String resume, List<String> cityList, String position) {
        Long currentIndex = 0L;
        List<MiniJobDetail> jobDetailList = new ArrayList<>();
        CompareQueue<UserSimilarity> compareQueue = new CompareQueue<>(10,
                Comparator.comparing(UserSimilarity::getSimilarity).reversed());
        do {
            for (String city : cityList) {
                jobDetailList.addAll(tJobDetailMapper.selectMiniJobDetail(city, position, currentIndex, limit));
            }
            if (jobDetailList.isEmpty()) {
                break;
            }
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
            jobDetailList.clear();
        } while (jobDetailList.isEmpty());
        List<UserSimilarity> resultList = new ArrayList<>();
        while (!compareQueue.isEmpty()) {
            UserSimilarity userSimilarity = compareQueue.poll();
            userSimilarity.setJobAnalysisData(formatDataService.convertToJobAnalysisData(userSimilarity.getMiniJobDetail()));
            userSimilarity.getJobAnalysisData().setJobUrl(userSimilarity.getMiniJobDetail().getUrl());
            userSimilarity.setMiniJobDetail(null);
            resultList.add(userSimilarity);
        }
        return resultList;
    }
}
