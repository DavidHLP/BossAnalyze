package com.david.hlp.Spring.boss.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import java.net.URI;

import com.david.hlp.Spring.common.result.Result;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import com.david.hlp.Spring.boss.model.SalaryJob;
import com.david.hlp.Spring.boss.model.JobAnalysisData;
@RestController
@RequestMapping("/api/boss/user")
@RequiredArgsConstructor
public class BossController {
    private final RestTemplate restTemplate;
    private final String BaseUrl = "http://localhost:8082/api/v1/boss/user";


    @GetMapping("/salary-hot-job")
    public Result<List<SalaryJob>> getSalaryJobAnalysis(@RequestParam(defaultValue = "20") Long limit) {
        URI url = UriComponentsBuilder.fromHttpUrl(BaseUrl)
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
        URI url = UriComponentsBuilder.fromHttpUrl(BaseUrl)
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
}
