package com.david.hlp.web.boss.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.core.ParameterizedTypeReference;

@RestController
@RequestMapping("/api/boss/basic")
@RequiredArgsConstructor
public class ListBasicSetController {

    private final RestTemplate restTemplate;
    private final String BaseUrl = "http://localhost:8082/api/v1/boss/basic";

    @GetMapping("/city-name-list")
    public List<String> getCityNameList() {
        URI url = UriComponentsBuilder.fromHttpUrl(BaseUrl)
                .path("/city-name-list")
                .build().encode().toUri();
        ResponseEntity<List<String>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {
                });
        return responseEntity.getBody();
    }

    @GetMapping("/position-name-list")
    public List<String> getPositionNameList(@RequestParam(defaultValue = "all") String cityName) {
        URI url = UriComponentsBuilder.fromHttpUrl(BaseUrl)
                .path("/position-name-list")
                .queryParam("cityName", cityName)
                .build().encode().toUri();
        ResponseEntity<List<String>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {
                });
        return responseEntity.getBody();
    }

}