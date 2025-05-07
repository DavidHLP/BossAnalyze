package com.david.hlp.web.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import org.springframework.core.ParameterizedTypeReference;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIRequirementsController {
    private final RestTemplate restTemplate;
    private final String BaseUrl = "http://localhost:8082/api/v1/ai/";

    @GetMapping("/get-core-requirements")
    public List<String> getCoreRequirements(@RequestParam(required = false) String city, @RequestParam(required = false) String position) {
        URI url = UriComponentsBuilder.fromHttpUrl(BaseUrl)
                                         .path("/get-core-requirements")
                                         .queryParam("city", city)
                                         .queryParam("position", position)
                                         .build().encode().toUri();
        ResponseEntity<List<String>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {});
        return responseEntity.getBody();
    }
}
