package com.david.hlp.Spring.system.auth;

import com.david.hlp.Spring.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.david.hlp.Spring.system.service.UserDetailsServiceImpl;

/**
 * 应用程序配置类，提供 Spring Security 核心配置和审计功能。
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

  private final UserMapper userMapper;

  /**
   * 配置 UserDetailsService，从数据库加载用户信息。
   *
   * @return UserDetailsService 实例。
   */
  @Bean
  public UserDetailsService userDetailsService() {
    return new UserDetailsServiceImpl(userMapper);
  }

  /**
   * 配置 AuthenticationProvider，支持基于数据库的用户认证。
   *
   * @return AuthenticationProvider 实例。
   */
  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService());
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  /**
   * 配置 AuditorAware，用于审计实体的创建者和修改者。
   *
   * @return AuditorAware 实例。
   */
  @Bean
  public AuditorAware<Integer> auditorAware() {
    return new ApplicationAuditAware();
  }

  /**
   * 配置 AuthenticationManager，管理用户认证。
   *
   * @param config AuthenticationConfiguration 实例。
   * @return AuthenticationManager 实例。
   * @throws Exception 如果获取失败。
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * 配置 PasswordEncoder，使用 BCrypt 算法加密密码。
   *
   * @return PasswordEncoder 实例。
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}