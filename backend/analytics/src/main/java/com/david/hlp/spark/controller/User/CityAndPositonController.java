package com.david.hlp.spark.controller.User;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import com.david.hlp.spark.model.User.SalaryJob;
import com.david.hlp.spark.service.User.SalaryHotJob;
import com.david.hlp.spark.model.User.JobAnalysisData;
import com.david.hlp.spark.service.User.TwoDimensionalAnalysisChart;
@RestController
@RequestMapping("/api/v1/boss/user")
@RequiredArgsConstructor
public class CityAndPositonController {

    private final SalaryHotJob salaryHotJob;

    @GetMapping("/salary-hot-job")
    public List<SalaryJob> getSalaryHotJob(@RequestParam(defaultValue = "10") Long limit) {
        return salaryHotJob.analyzeSalaryHotJob(limit);
    }

    private final TwoDimensionalAnalysisChart twoDimensionalAnalysisChart;
    /**
     * 获取职位二维分析数据
     */
    @GetMapping("/two-dimensional-analysis-chart")
    public List<JobAnalysisData> getJobAnalysisData(
            @RequestParam(required = false, defaultValue = "all") String cityName,
            @RequestParam(required = false, defaultValue = "all") String positionName,
            @RequestParam(required = false, defaultValue = "salary_value") String xAxis,
            @RequestParam(required = false, defaultValue = "degree_value") String yAxis) {
        return twoDimensionalAnalysisChart.getJobAnalysisData(
                cityName, positionName, xAxis, yAxis);
    }
}
