package com.david.hlp.web.spark.boss.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.david.hlp.web.common.enums.RedisKeyEnum;
import com.david.hlp.web.spark.boss.client.BossSalaryClient;
import com.david.hlp.web.spark.boss.model.JobAnalysisData;
import com.david.hlp.web.spark.boss.model.SalaryJob;
import com.david.hlp.commons.utils.RedisCacheHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BossSalaryServiceImp {
        private final BossSalaryClient bossSalaryClient;
        private final RedisCacheHelper redisCacheHelper;

        private static final String CACHE_KEY_SALARY_HOT_JOB = "getSalaryHotJob";
        private static final String CACHE_KEY_TWO_DIMENSIONAL_ANALYSIS = "getTwoDimensionalAnalysisChart";

        public List<SalaryJob> getSalaryHotJob(Long limit) {
                String cacheKey = String.format("%s%s:%d",
                                RedisKeyEnum.SALARY_ANALYSIS_KEY.getKey(), CACHE_KEY_SALARY_HOT_JOB, limit);

                return redisCacheHelper.getOrLoadList(
                                cacheKey,
                                SalaryJob.class,
                                RedisKeyEnum.SALARY_ANALYSIS_KEY.getTimeout(),
                                () -> bossSalaryClient.getSalaryHotJob(limit));
        }

        public List<JobAnalysisData> getTwoDimensionalAnalysisChart(String cityName, String positionName, String xAxis,
                        String yAxis) {
                String cacheKey = String.format("%s%s:%s:%s:%s:%s",
                                RedisKeyEnum.SALARY_ANALYSIS_KEY.getKey(), CACHE_KEY_TWO_DIMENSIONAL_ANALYSIS,
                                cityName, positionName, xAxis, yAxis);

                return redisCacheHelper.getOrLoadList(
                                cacheKey,
                                JobAnalysisData.class,
                                RedisKeyEnum.SALARY_ANALYSIS_KEY.getTimeout(),
                                () -> bossSalaryClient.getTwoDimensionalAnalysisChart(cityName, positionName, xAxis,
                                                yAxis));
        }
}
