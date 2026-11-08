package com.david.hlp.web.system.auth.filter;

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

        private final ConsoleAccess consoleAccess;

        static final String[] PUBLIC_PATHS = {
                "/api/auth/login", "/api/auth/register", "/api/auth/logout",
                "/api/auth/refresh-token", "/api/repeater/auth/login", "/api/auth/sendRegisterEmail",
                "/actuator/health", "/actuator/health/readiness", "/actuator/health/liveness"
        };

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(requests -> requests
                                                .requestMatchers(PUBLIC_PATHS).permitAll()
                                                .requestMatchers("/api/user/**").access((auth, ctx) -> consoleAccess.check(auth.get(), "users"))
                                                .requestMatchers("/api/role/**").access((auth, ctx) -> consoleAccess.checkAny(auth.get(), "roles", "users"))
                                                .requestMatchers("/api/auth/getRoleList").access((auth, ctx) -> consoleAccess.checkAny(auth.get(), "roles", "menus"))
                                                .requestMatchers("/api/auth/addRole", "/api/auth/editRole", "/api/auth/deleteRole", "/api/auth/updateRoleRouters")
                                                    .access((auth, ctx) -> consoleAccess.check(auth.get(), "roles"))
                                                .requestMatchers("/api/auth/addRouter", "/api/auth/editRouter", "/api/auth/deleteRouter")
                                                    .access((auth, ctx) -> consoleAccess.check(auth.get(), "menus"))
                                                .requestMatchers("/api/v1/system/logs/analysis/trigger").hasAuthority("logs.analysis.trigger")
                                                .requestMatchers("/api/v1/system/logs/health").authenticated()
                                                .requestMatchers("/api/v1/system/logs/**").access((auth, ctx) -> consoleAccess.check(auth.get(), "dashboard"))
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(errors -> errors
                                                .authenticationEntryPoint((req, res, ex) -> res.sendError(401))
                                                .accessDeniedHandler((req, res, ex) -> res.sendError(403)))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                                .csrf(csrf -> csrf.disable());

                return http.build();
        }
}
