package com.david.hlp.web.boss.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.david.hlp.web.boss.client.BossSalaryClient;
import com.david.hlp.web.boss.model.JobAnalysisData;
import com.david.hlp.web.boss.model.SalaryJob;
import com.david.hlp.web.common.enums.RedisKeyCommon;
import com.david.hlp.web.common.enums.RedisLockKeyCommon;
import com.david.hlp.web.common.util.RedisCache;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BossSalaryServiceImp {
    private final BossSalaryClient bossSalaryClient;
    private final RedisCache redisCache;

    public List<SalaryJob> getSalaryHotJob(Long limit) {
        return redisCache.getWithMutex(RedisKeyCommon.SALARY_ANALYSIS_KEY.getKey() + "getSalaryHotJob"+":"+limit,
                RedisKeyCommon.SALARY_ANALYSIS_KEY.getTimeout(), RedisKeyCommon.SALARY_ANALYSIS_KEY.getTimeUnit(),
                RedisLockKeyCommon.SALARY_ANALYSIS_LOCK_KEY.getKey() + "getSalaryHotJob"+":"+limit,
                RedisLockKeyCommon.SALARY_ANALYSIS_LOCK_KEY.getWaitTime(),
                RedisLockKeyCommon.SALARY_ANALYSIS_LOCK_KEY.getLeaseTime(),
                RedisLockKeyCommon.SALARY_ANALYSIS_LOCK_KEY.getTimeUnit(),
                () -> bossSalaryClient.getSalaryHotJob(limit));
    }

    public List<JobAnalysisData> getTwoDimensionalAnalysisChart(String cityName, String positionName, String xAxis,
            String yAxis) {
        return redisCache.getWithMutex(RedisKeyCommon.SALARY_ANALYSIS_KEY.getKey() + "getTwoDimensionalAnalysisChart"+":"+cityName+":"+positionName+":"+xAxis+":"+yAxis,
                RedisKeyCommon.SALARY_ANALYSIS_KEY.getTimeout(), RedisKeyCommon.SALARY_ANALYSIS_KEY.getTimeUnit(),
                RedisLockKeyCommon.SALARY_ANALYSIS_LOCK_KEY.getKey() + "getTwoDimensionalAnalysisChart"+":"+cityName+":"+positionName+":"+xAxis+":"+yAxis,
                RedisLockKeyCommon.SALARY_ANALYSIS_LOCK_KEY.getWaitTime(),
                RedisLockKeyCommon.SALARY_ANALYSIS_LOCK_KEY.getLeaseTime(),
                RedisLockKeyCommon.SALARY_ANALYSIS_LOCK_KEY.getTimeUnit(),
                () -> bossSalaryClient.getTwoDimensionalAnalysisChart(cityName, positionName, xAxis, yAxis));
    }
}
