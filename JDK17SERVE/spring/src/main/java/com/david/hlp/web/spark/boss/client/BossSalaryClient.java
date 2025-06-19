package com.david.hlp.web.spark.boss.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.david.hlp.web.spark.boss.model.JobAnalysisData;
import com.david.hlp.web.spark.boss.model.SalaryJob;

@FeignClient(name = "SpringSpark", contextId = "bossSalaryClient")
public interface BossSalaryClient {
        @GetMapping("/api/v1/boss/user/salary-hot-job")
        public List<SalaryJob> getSalaryHotJob(@RequestParam(required = false, defaultValue = "20") Long limit);

        @GetMapping("/api/v1/boss/user/two-dimensional-analysis-chart")
        public List<JobAnalysisData> getTwoDimensionalAnalysisChart(
                        @RequestParam(required = false, defaultValue = "all") String cityName,
                        @RequestParam(required = false, defaultValue = "all") String positionName,
                        @RequestParam(required = false, defaultValue = "salary_value") String xAxis,
                        @RequestParam(required = false, defaultValue = "degree_value") String yAxis);

        @GetMapping("/api/v1/boss/user/three-dimensional-analysis-chart")
        public List<JobAnalysisData> getThreeDimensionalAnalysisChart(
                        @RequestParam(required = false, defaultValue = "all") String cityName,
                        @RequestParam(required = false, defaultValue = "all") String positionName);
}
