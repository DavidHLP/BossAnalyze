package com.david.hlp.web.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * 带LoadBalancer的RestTemplate，用于服务发现
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    /**
     * 普通的RestTemplate，用于直接HTTP调用
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}