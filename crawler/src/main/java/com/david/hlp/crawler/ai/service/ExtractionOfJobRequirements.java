package com.david.hlp.crawler.ai.service;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;

import com.david.hlp.crawler.ai.entity.MiniJobDetail;
import com.david.hlp.crawler.ai.mapper.TJobDetailMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionOfJobRequirements {
    private final TJobDetailMapper tJobDetailMapper;
    private final AIExtractionOfJobRequirements aiExtractionOfJobRequirements;

    private final Lock employeeBenefitsLock = new ReentrantLock();
    private final Lock jobRequirementsLock = new ReentrantLock();

    @Scheduled(cron = "0 0/5 * * * ?")
    public void extractionOfEmployeeBenefits() {
        if (employeeBenefitsLock.tryLock()) {
            try {
                List<MiniJobDetail> miniJobDetails = tJobDetailMapper.selectEmployeeBenefitsJobDetailIsNull();
                for (MiniJobDetail miniJobDetail : miniJobDetails) {
                    List<String> jobBenefits = aiExtractionOfJobRequirements.extractJobBenefits(miniJobDetail.getDetailData());
                    if (jobBenefits != null && !jobBenefits.isEmpty()) {
                        miniJobDetail.setEmployeeBenefits(jobBenefits.toString());
                        tJobDetailMapper.updateEmployeeBenefits(miniJobDetail);
                    }
                }
            } finally {
                employeeBenefitsLock.unlock();
            }
        }
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public void extractionOfJobRequirements() {
        if (jobRequirementsLock.tryLock()) {
            try {
                List<MiniJobDetail> miniJobDetails = tJobDetailMapper.selectJobRequirementsJobDetailIsNull();
                for (MiniJobDetail miniJobDetail : miniJobDetails) {
                    List<String> jobRequirements = aiExtractionOfJobRequirements.extractJobRequirements(miniJobDetail.getDetailData());
                    if (jobRequirements != null && !jobRequirements.isEmpty()) {
                        miniJobDetail.setJobRequirements(jobRequirements.toString());
                        tJobDetailMapper.updateJobRequirements(miniJobDetail);
                    }
                }
            } finally {
                jobRequirementsLock.unlock();
            }
        }
    }
}
