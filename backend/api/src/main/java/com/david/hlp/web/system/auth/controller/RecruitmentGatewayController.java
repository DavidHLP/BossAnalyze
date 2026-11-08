package com.david.hlp.web.system.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import java.net.URI;

/** Authenticated read gateway; the analytics listener must stay on a trusted network. */
@RestController
@RequiredArgsConstructor
public class RecruitmentGatewayController {
    private final RestTemplate restTemplate;

    @Value("${spark.service.url:http://localhost:8082}")
    private String sparkServiceUrl;

    @GetMapping({"/api/v1/boss/basic/city-name-list", "/api/v1/boss/basic/position-name-list",
            "/api/v1/boss/user/salary-hot-job", "/api/v1/boss/user/two-dimensional-analysis-chart"})
    public ResponseEntity<String> read(HttpServletRequest request) {
        // Preserve encoded query values; never forward credentials or accept a target URL.
        String query = request.getQueryString();
        URI uri = URI.create(sparkServiceUrl + request.getRequestURI() + (query == null ? "" : "?" + query));
        try {
            return restTemplate.getForEntity(uri, String.class);
        } catch (RestClientResponseException error) {
            return ResponseEntity.status(error.getStatusCode()).body(error.getResponseBodyAsString());
        } catch (ResourceAccessException error) {
            return ResponseEntity.status(502).build();
        }
    }
}
