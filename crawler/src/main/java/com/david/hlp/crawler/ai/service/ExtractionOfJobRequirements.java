package com.david.hlp.crawler.ai.service;

import org.springframework.stereotype.Service;

import com.david.hlp.crawler.ai.executor.TaskExecutorService;
import com.david.hlp.crawler.ai.lock.DistributedLockService;
import com.david.hlp.crawler.ai.task.EmployeeBenefitsExtractionTask;
import com.david.hlp.crawler.ai.task.JobRequirementsExtractionTask;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 职位信息提取服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionOfJobRequirements {
    private final DistributedLockService lockService;
    private final TaskExecutorService taskExecutorService;
    private final EmployeeBenefitsExtractionTask employeeBenefitsTask;
    private final JobRequirementsExtractionTask jobRequirementsTask;

    // /**
    //  * 定时提取职位福利信息
    //  */
    // @Scheduled(cron = "0 0/5 * * * ?")
    // private void extractionOfEmployeeBenefits() {
    //     lockService.executeWithLock("职位福利提取", () -> {
    //         taskExecutorService.executeAsync("职位福利提取任务", employeeBenefitsTask::execute);
    //         return null;
    //     });
    // }

    // /**
    //  * 定时提取职位要求信息
    //  */
    // @Scheduled(cron = "0 0/5 * * * ?")
    // private void extractionOfJobRequirements() {
    //     lockService.executeWithLock("职位要求提取", () -> {
    //         taskExecutorService.executeAsync("职位要求提取任务", jobRequirementsTask::execute);
    //         return null;
    //     });
    // }
}
