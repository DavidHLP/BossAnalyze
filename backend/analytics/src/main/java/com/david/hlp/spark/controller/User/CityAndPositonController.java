package com.david.hlp.spark.controller.User;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import com.david.hlp.spark.model.User.SalaryJob;
import com.david.hlp.spark.service.User.SalaryHotJob;
import com.david.hlp.spark.model.User.JobAnalysisData;
import com.david.hlp.spark.service.User.TwoDimensionalAnalysisChart;
@RestController
@RequestMapping("/api/v1/boss/user")
@RequiredArgsConstructor
public class CityAndPositonController {

    private static final Set<String> ANALYSIS_AXES = Set.of("salary_value", "degree_value", "experience_value");
    private static final long MAX_SALARY_HOT_JOB_LIMIT = 1000L;

    private final SalaryHotJob salaryHotJob;

    @GetMapping("/salary-hot-job")
    public List<SalaryJob> getSalaryHotJob(@RequestParam(defaultValue = "10") Long limit) {
        if (limit == null || limit < 1 || limit > MAX_SALARY_HOT_JOB_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "limit must be between 1 and " + MAX_SALARY_HOT_JOB_LIMIT);
        }
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
        if (!ANALYSIS_AXES.contains(xAxis) || !ANALYSIS_AXES.contains(yAxis)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "xAxis and yAxis must be one of " + ANALYSIS_AXES);
        }
        return twoDimensionalAnalysisChart.getJobAnalysisData(
                cityName, positionName, xAxis, yAxis);
    }
}
