package com.david.hlp.commons.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers downstream request authentication only when a service opts in.
 * The primary API keeps this disabled because it is the identity source.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "security.downstream-auth", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DownstreamAuthProperties.class)
public class DownstreamAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DownstreamAuthFilter downstreamAuthFilter(DownstreamAuthProperties properties) {
        return new DownstreamAuthFilter(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "downstreamAuthFilterRegistration")
    public FilterRegistrationBean<DownstreamAuthFilter> downstreamAuthFilterRegistration(
            DownstreamAuthFilter filter) {
        FilterRegistrationBean<DownstreamAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(0);
        return registration;
    }
}
