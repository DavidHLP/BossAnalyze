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
import com.david.hlp.Spring.boss.model.HotCity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import com.david.hlp.Spring.boss.model.CityJobAnalysis;
import com.david.hlp.Spring.boss.model.CityJobRecommendation;
import com.david.hlp.Spring.boss.model.CityHotJob;
import com.david.hlp.Spring.boss.model.JobRequirement;
import com.david.hlp.Spring.boss.model.HotJob;

@RestController
@RequestMapping("/api/boss/spark/user")
@RequiredArgsConstructor
public class BossController {
    private final RestTemplate restTemplate;
    private final String BaseUrl = "http://localhost:8082/api/v1/boss/user";
    @GetMapping("/hot-cities")
    public Result<List<HotCity>> getHotCities(@RequestParam(defaultValue = "10") Long limit) {
        URI url = UriComponentsBuilder.fromHttpUrl(BaseUrl)
                                         .path("/hot-cities")
                                         .queryParam("limit", limit)
                                         .build().encode().toUri();

        ResponseEntity<List<HotCity>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<HotCity>>() {}
        );
        List<HotCity> response = responseEntity.getBody();
        return Result.success(response);
    }

    @GetMapping("/job-city-requirement-analysis")
    public Result<List<CityJobAnalysis>> getJobCityRequirementAnalysis(@RequestParam String positionName, @RequestParam String cityName) {
        URI url = UriComponentsBuilder.fromHttpUrl(BaseUrl)
                                         .path("/job-city-requirement-analysis")
                                         .queryParam("positionName", positionName)
                                         .queryParam("cityName", cityName)
                                         .build().encode().toUri();
        ResponseEntity<List<CityJobAnalysis>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CityJobAnalysis>>() {}
        );
        List<CityJobAnalysis> response = responseEntity.getBody();
        return Result.success(response);
    }

    @GetMapping("/job-city-recommender")
    public Result<List<CityJobRecommendation>> getJobCityRecommender(@RequestParam String positionName, @RequestParam(defaultValue = "10") int limit) {
        URI url = UriComponentsBuilder.fromHttpUrl(BaseUrl)
                                         .path("/job-city-recommender")
                                         .queryParam("positionName", positionName)
                                         .queryParam("limit", limit)
                                         .build().encode().toUri();
        ResponseEntity<List<CityJobRecommendation>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CityJobRecommendation>>() {}
        );
        List<CityJobRecommendation> response = responseEntity.getBody();
        return Result.success(response);
    }

    @GetMapping("/job-heat-calculator")
    public Result<List<CityHotJob>> getJobHeatCalculator(@RequestParam String cityName) {
        URI url = UriComponentsBuilder.fromHttpUrl(BaseUrl)
                                         .path("/job-heat-calculator")
                                         .queryParam("cityName", cityName)
                                         .build().encode().toUri();
        ResponseEntity<List<CityHotJob>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CityHotJob>>() {}
        );
        List<CityHotJob> response = responseEntity.getBody();
        return Result.success(response);
    }

    @GetMapping("/job-requirement-analysis")
    public Result<JobRequirement> getJobRequirementAnalysis(@RequestParam String positionName) {
        URI url = UriComponentsBuilder.fromHttpUrl(BaseUrl)
                                         .path("/job-requirement-analysis")
                                         .queryParam("positionName", positionName)
                                         .build().encode().toUri();
        ResponseEntity<JobRequirement> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<JobRequirement>() {}
        );
        JobRequirement response = responseEntity.getBody();
        return Result.success(response);
    }

    @GetMapping("/hot-jobs")
    public Result<List<HotJob>> getHotJobs(@RequestParam(defaultValue = "10") Long limit) {
        URI url = UriComponentsBuilder.fromHttpUrl(BaseUrl)
                                         .path("/hot-jobs")
                                         .queryParam("limit", limit)
                                         .build().encode().toUri();
        ResponseEntity<List<HotJob>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<HotJob>>() {}
        );
        List<HotJob> response = responseEntity.getBody();
        return Result.success(response);
    }
}
