package com.david.hlp.crawler.ai.task;

import java.util.List;

import org.springframework.stereotype.Component;

import com.david.hlp.crawler.ai.entity.MiniJobDetail;
import com.david.hlp.crawler.ai.mapper.TJobDetailMapper;
import com.david.hlp.crawler.ai.service.AIExtractionOfJobRequirements;
import com.david.hlp.crawler.ai.task.Abstract.BaseJobExtractionTask;

import lombok.extern.slf4j.Slf4j;

/**
 * 职位要求提取任务
 */
@Slf4j
@Component
public class JobRequirementsExtractionTask extends BaseJobExtractionTask {

    private static final String TASK_NAME = "职位要求";
    private final TJobDetailMapper tJobDetailMapper;

    public JobRequirementsExtractionTask(AIExtractionOfJobRequirements aiExtractor, TJobDetailMapper tJobDetailMapper) {
        super(aiExtractor);
        this.tJobDetailMapper = tJobDetailMapper;
    }

    @Override
    protected List<MiniJobDetail> getJobDetails() {
        return tJobDetailMapper.selectJobRequirementsJobDetailIsNull();
    }

    @Override
    protected List<String> extractData(String detailData) {
        return aiExtractor.extractJobRequirements(detailData);
    }

    @Override
    protected void updateJobDetail(MiniJobDetail jobDetail, List<String> extractedData) {
        jobDetail.setJobRequirements(extractedData.toString());
        tJobDetailMapper.updateJobRequirements(jobDetail);
    }

    @Override
    protected String getTaskName() {
        return TASK_NAME;
    }
}