package com.david.hlp.spark.controller.User;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.david.hlp.spark.model.User.CityJobAnalysis;
import com.david.hlp.spark.model.User.CityJobRecommendation;
import com.david.hlp.spark.model.User.HotCity;
import com.david.hlp.spark.service.User.HotCityAnalyzer;
import com.david.hlp.spark.service.User.JobCityRequirementAnalysis;
import com.david.hlp.spark.service.User.JobCityRecommender;
import com.david.hlp.spark.service.User.JobHeatCalculator;
import com.david.hlp.spark.model.User.CityHotJob;
import com.david.hlp.spark.service.User.JobRequirementAnalysis;
import com.david.hlp.spark.model.User.JobRequirement;
import lombok.RequiredArgsConstructor;
import com.david.hlp.spark.service.User.HotJobAnalyzer;
import com.david.hlp.spark.model.User.HotJob;

@RestController
@RequestMapping("/api/v1/boss/user")
@RequiredArgsConstructor
public class CityAndPositonController {

    private final HotCityAnalyzer hotCityAnalyzer;

    @GetMapping("/hot-cities")
    public List<HotCity> getHotCities(@RequestParam(defaultValue = "10") Long limit) {
        return hotCityAnalyzer.analyzeHotCities(limit);
    }

    private final JobCityRequirementAnalysis jobCityRequirementAnalysis;

    @GetMapping("/job-city-requirement-analysis")
    public List<CityJobAnalysis> getJobCityRequirementAnalysis(@RequestParam String positionName, @RequestParam String cityName) {
        return jobCityRequirementAnalysis.jobCityRequirementAnalysis(positionName, cityName);
    }

    private final JobCityRecommender jobCityRecommender;

    @GetMapping("/job-city-recommender")
    public List<CityJobRecommendation> getJobCityRecommender(@RequestParam String positionName, @RequestParam(defaultValue = "10") int limit) {
        return jobCityRecommender.recommendCities(positionName, limit);
    }

    private final JobHeatCalculator jobHeatCalculator;

    @GetMapping("/job-heat-calculator")
    public List<CityHotJob> getJobHeatCalculator(@RequestParam String cityName) {
        return jobHeatCalculator.calculateJobHeat(cityName);
    }

    private final JobRequirementAnalysis jobRequirementAnalysis;

    @GetMapping("/job-requirement-analysis")
    public JobRequirement getJobRequirementAnalysis(@RequestParam String positionName) {
        return jobRequirementAnalysis.analyzeJobRequirements(positionName);
    }

    private final HotJobAnalyzer hotJobAnalyzer;

    @GetMapping("/hot-jobs")
    public List<HotJob> getHotJobs(@RequestParam(defaultValue = "10") Long limit) {
        return hotJobAnalyzer.listHotJobs(limit);
    }
}
