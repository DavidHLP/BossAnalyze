package com.david.hlp.web.ai.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.david.hlp.web.ai.model.UserSimilarity;
import com.david.hlp.web.common.result.Result;
import com.david.hlp.web.ai.model.SimilarityRequest;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIRequirementsController {
        private final RestTemplate restTemplate;
        private final String BaseUrl = "http://localhost:8081/api/v1/ai";

        @PostMapping("/get-user-similarity")
        public Result<List<UserSimilarity>> getUserSimilarity(@RequestBody SimilarityRequest similarityRequest) {
                URI url = UriComponentsBuilder.fromUriString(BaseUrl)
                                .path("/get-user-similarity")
                                .build().encode().toUri();

                ResponseEntity<List<UserSimilarity>> responseEntity = restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                new HttpEntity<>(similarityRequest),
                                new ParameterizedTypeReference<List<UserSimilarity>>() {
                                });
                List<UserSimilarity> response = responseEntity.getBody();
                return Result.success(response);
        }
}
