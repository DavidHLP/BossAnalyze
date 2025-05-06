package com.david.hlp.web.system.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类。
 * 配置全局跨域资源共享（CORS）规则和静态资源映射。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置全局 CORS 过滤器。
     * 设置允许的来源、HTTP 方法、请求头和凭证传递。
     *
     * @return 配置完成的 CorsFilter 实例。
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*"); // 允许所有来源
        config.addAllowedMethod("*"); // 允许所有 HTTP 方法
        config.addAllowedHeader("*"); // 允许所有请求头
        config.setAllowCredentials(true); // 允许凭证传递

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 应用配置到所有路径

        return new CorsFilter(source);
    }
}