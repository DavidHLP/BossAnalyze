package com.david.hlp.Spring.boss.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/boss")
@RequiredArgsConstructor
public class BossController {
    private final RestTemplate restTemplate;
    private final String BaseUrl = "http://localhost:8082/api/v1/boss";
    @GetMapping("/boss")
    public String boss() {
        String url = BaseUrl + "/boss";
        String response = restTemplate.getForObject(url, String.class);
        return response;
    }
}
