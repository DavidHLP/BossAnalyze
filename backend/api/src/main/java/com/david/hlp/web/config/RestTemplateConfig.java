package com.david.hlp.web.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestTemplateConfig {

    /**
     * 仅向受信任的内部服务转发当前请求的 Authorization，避免写入外部 AI/网页地址。
     */
    @Value("${spark.service.url:http://localhost:8082}")
    private String sparkServiceUrl;

    /**
     * 带LoadBalancer的RestTemplate，用于服务发现
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(this::propagateToTrustedInternal);
        return restTemplate;
    }

    /**
     * 普通的RestTemplate，用于直接HTTP调用
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(this::propagateToTrustedInternal);
        return restTemplate;
    }

    private org.springframework.http.client.ClientHttpResponse propagateToTrustedInternal(
            org.springframework.http.HttpRequest request, byte[] body,
            org.springframework.http.client.ClientHttpRequestExecution execution) throws java.io.IOException {
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)
                && isTrustedInternal(request.getURI())) {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                String authorization = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null && !authorization.isEmpty()) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
                }
            }
        }
        return execution.execute(request, body);
    }

    private boolean isTrustedInternal(URI target) {
        if (target == null || sparkServiceUrl == null || sparkServiceUrl.isEmpty()) {
            return false;
        }
        try {
            URI trusted = URI.create(sparkServiceUrl);
            return trusted.getHost() != null
                    && trusted.getHost().equalsIgnoreCase(target.getHost())
                    && effectivePort(trusted) == effectivePort(target);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}
