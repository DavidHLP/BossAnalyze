package com.david.hlp.web.spark.boss.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.david.hlp.web.common.entity.Result;
import com.david.hlp.web.spark.boss.model.JobAnalysisData;
import com.david.hlp.web.spark.boss.model.SalaryJob;
import com.david.hlp.web.spark.boss.service.BossSalaryServiceImp;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/boss/user")
@RequiredArgsConstructor
public class BossSalaryController {
    private final BossSalaryServiceImp bossSalaryService;

    @GetMapping("/salary-hot-job")
    public Result<List<SalaryJob>> getSalaryJobAnalysis(
            @RequestParam(required = false, defaultValue = "20") Long limit) {
        List<SalaryJob> response = bossSalaryService.getSalaryHotJob(limit);
        return Result.success(response);
    }

    @GetMapping("/two-dimensional-analysis-chart")
    public Result<List<JobAnalysisData>> getTwoDimensionalAnalysisChart(
            @RequestParam(required = false, defaultValue = "all") String cityName,
            @RequestParam(required = false, defaultValue = "all") String positionName,
            @RequestParam(required = false, defaultValue = "salary_value") String xAxis,
            @RequestParam(required = false, defaultValue = "degree_value") String yAxis) {
        List<JobAnalysisData> response = bossSalaryService.getTwoDimensionalAnalysisChart(cityName, positionName, xAxis,
                yAxis);
        return Result.success(response);
    }
}
