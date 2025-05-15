package com.david.hlp.web.system.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/api/user/**",
                    "/api/role/**",
                    "/api/repeater/**",
                    "/doc.html",
                    "/doc.html/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/webjars/**",
                    "/authenticate",
                    "/swagger-ui.html/**",
                    "/swagger-resources",
                    "/swagger-resources/**",
                    "/api/repeater/**",
                    "/api/boss/**",
                    "/api/proxy/**",
                    "/api/boss/basic/**",
                    "/api/boss/user/**",
                    "/api/resume/**",
                    "/api/image/**"
                ).permitAll()
                .requestMatchers("/api/user/**").hasRole("ADMIN")
                .requestMatchers("/api/role/**").hasRole("ADMIN")
                .requestMatchers("/api/auth/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}