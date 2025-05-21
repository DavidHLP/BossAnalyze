package com.david.hlp.web.boss.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.david.hlp.web.boss.model.JobAnalysisData;
import com.david.hlp.web.boss.model.SalaryJob;
import com.david.hlp.web.common.result.Result;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
@RestController
@RequestMapping("/api/boss/user")
@RequiredArgsConstructor
public class BossController {
    private final RestTemplate restTemplate;
    private final String BaseUrl = "http://localhost:8082/api/v1/boss/user";


    @GetMapping("/salary-hot-job")
    public Result<List<SalaryJob>> getSalaryJobAnalysis(@RequestParam(required = false, defaultValue = "20") Long limit) {
        URI url = UriComponentsBuilder.fromUriString(BaseUrl)
                                         .path("/salary-hot-job")
                                         .queryParam("limit", limit)
                                         .build().encode().toUri();
        ResponseEntity<List<SalaryJob>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<SalaryJob>>() {}
        );
        List<SalaryJob> response = responseEntity.getBody();
        return Result.success(response);
    }

    @GetMapping("/two-dimensional-analysis-chart")
    public Result<List<JobAnalysisData>> getTwoDimensionalAnalysisChart(@RequestParam(required = false, defaultValue = "all") String cityName,
                                                                        @RequestParam(required = false, defaultValue = "all") String positionName,
                                                                        @RequestParam(required = false, defaultValue = "salary_value") String xAxis,
                                                                        @RequestParam(required = false, defaultValue = "degree_value") String yAxis) {
        URI url = UriComponentsBuilder.fromUriString(BaseUrl)
                                         .path("/two-dimensional-analysis-chart")
                                         .queryParam("cityName", cityName)
                                         .queryParam("positionName", positionName)
                                         .queryParam("xAxis", xAxis)
                                         .queryParam("yAxis", yAxis)
                                         .build().encode().toUri();
        ResponseEntity<List<JobAnalysisData>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<JobAnalysisData>>() {}
        );
        List<JobAnalysisData> response = responseEntity.getBody();
        return Result.success(response);
    }

    @GetMapping("/three-dimensional-analysis-chart")
    public Result<List<JobAnalysisData>> getThreeDimensionalAnalysisChart(@RequestParam(required = false, defaultValue = "all") String cityName,
                                                                         @RequestParam(required = false, defaultValue = "all") String positionName) {
        URI url = UriComponentsBuilder.fromUriString(BaseUrl)
                                         .path("/three-dimensional-analysis-chart")
                                         .queryParam("cityName", cityName)
                                         .queryParam("positionName", positionName)
                                         .build().encode().toUri();
        ResponseEntity<List<JobAnalysisData>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<JobAnalysisData>>() {}
        );
        List<JobAnalysisData> response = responseEntity.getBody();
        return Result.success(response);
    }
}
