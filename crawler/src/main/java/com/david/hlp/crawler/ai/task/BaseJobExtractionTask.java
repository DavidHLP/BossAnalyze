package com.david.hlp.crawler.ai.task;

import java.util.List;

import org.springframework.stereotype.Component;

import com.david.hlp.crawler.ai.entity.MiniJobDetail;
import com.david.hlp.crawler.ai.service.AIExtractionOfJobRequirements;

import lombok.extern.slf4j.Slf4j;

/**
 * 职位信息提取任务基类，封装了通用的提取逻辑
 */
@Slf4j
@Component
public abstract class BaseJobExtractionTask {

    protected final AIExtractionOfJobRequirements aiExtractor;

    protected BaseJobExtractionTask(AIExtractionOfJobRequirements aiExtractor) {
        this.aiExtractor = aiExtractor;
    }

    /**
     * 获取待处理的职位数据
     */
    protected abstract List<MiniJobDetail> getJobDetails();

    /**
     * 执行AI提取操作
     */
    protected abstract List<String> extractData(String detailData);

    /**
     * 更新提取结果到数据库
     */
    protected abstract void updateJobDetail(MiniJobDetail jobDetail, List<String> extractedData);

    /**
     * 获取任务名称
     */
    protected abstract String getTaskName();

    /**
     * 执行提取任务
     * 
     * @return 更新成功的记录数
     */
    public int execute() {
        String taskName = getTaskName();
        try {
            List<MiniJobDetail> jobDetails = getJobDetails();
            log.info("需要更新{}的职位数量: {}", taskName, jobDetails.size());

            int updateCount = 0;
            for (MiniJobDetail jobDetail : jobDetails) {
                try {
                    List<String> extractedData = extractData(jobDetail.getDetailData());
                    if (extractedData == null || extractedData.isEmpty()) {
                        continue;
                    }
                    log.info("{}: {}", taskName, extractedData);
                    updateJobDetail(jobDetail, extractedData);
                    updateCount++;
                    log.info("更新{}成功: {}", taskName, jobDetail.getId());
                } catch (Exception e) {
                    log.error("更新{}时发生异常, 职位ID: {}, 异常信息: {}", taskName, jobDetail.getId(), e.getMessage(), e);
                }
            }
            log.info("{}更新完成，共更新{}条数据", taskName, updateCount);
            return updateCount;
        } catch (Exception e) {
            log.error("{}更新过程中发生异常: {}", taskName, e.getMessage(), e);
            return 0;
        }
    }
}