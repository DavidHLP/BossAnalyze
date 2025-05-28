package com.david.hlp.web.system.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.david.hlp.web.system.mapper.TokenMapper;

import java.io.IOException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * JWT 认证过滤器。
 *
 * 该过滤器会在每次请求时运行一次，用于验证 JWT 并设置用户的认证信息到 Spring Security 的上下文中。
 */
@Slf4j
@Component
@RequiredArgsConstructor // 自动生成包含所有必需依赖项的构造函数
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  // 用于处理 JWT 的服务类
  private final JwtService jwtService;

  // 用于加载用户详细信息
  @Qualifier("userDetailsServiceImp")
  private final UserDetailsService userDetailsService;

  // 用于检查 JWT 是否在数据库中有效
  private final TokenMapper tokenMapper;

  private final String[] publicPaths = {
      "/api/auth/demo/login",
      "/api/auth/demo/register",
      "/api/auth/demo/logout",
      "/api/auth/demo/refresh-token",
      "/api/repeater/auth/login",
  };

  /**
   * 核心过滤逻辑。
   *
   * @param request     HTTP 请求对象
   * @param response    HTTP 响应对象
   * @param filterChain 过滤器链，用于继续执行后续过滤器
   * @throws ServletException 如果过滤过程中出现问题
   * @throws IOException      如果发生 I/O 错误
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {
    // 记录请求信息：IP、路径和HTTP方法，使用键值对格式方便日志分析
    String clientIP = request.getRemoteAddr();
    String path = request.getServletPath();
    String method = request.getMethod();
    String userAgent = request.getHeader("User-Agent");
    long timestamp = System.currentTimeMillis();

    // 使用键值对格式记录日志，便于后期数据分析
    log.info("ACCESS|ts={}|ip={}|path={}|method={}|ua={}",
        timestamp, clientIP, path, method, userAgent != null ? userAgent : "-");

    // 总是允许 OPTIONS 请求通过（CORS预检请求）
    if (request.getMethod().equals("OPTIONS")) {
      filterChain.doFilter(request, response);
      return;
    }

    // 1. 检查是否为公开路径
    boolean isPublicPath = Arrays.stream(publicPaths).anyMatch(path::startsWith);

    // 2. 检查Authorization头
    final String authHeader = request.getHeader("Authorization");

    // 3. 如果不是公开路径且没有有效token，直接返回401
    if (!isPublicPath && (authHeader == null || !authHeader.startsWith("Bearer "))) {
      log.warn("拒绝访问：路径 {} 需要授权但未提供有效token", path);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    // 4. 如果是公开路径，允许通过
    if (isPublicPath) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      // 5. 处理正常的带token请求
      final String jwt = authHeader.substring(7);
      final String userEmail = jwtService.extractUsername(jwt);

      // 验证用户并设置认证信息
      if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        // 从 UserDetailsService 加载用户信息
        UserDetails userDetails;
        try {
          userDetails = this.userDetailsService.loadUserByUsername(userEmail);
        } catch (Exception e) {
          log.error("加载用户信息失败: {}", userEmail);
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }

        // 检查 JWT 是否在数据库中有效，且未过期或撤销
        var isTokenValid = tokenMapper.checkTokenValid(jwt);
        if (!isTokenValid) {
          log.warn("token已失效或被撤销: {}", userEmail);
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }

        // 验证 JWT 是否有效
        if (jwtService.isTokenValid(jwt, userDetails)) {
          // 直接从UserDetails获取权限
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
              userDetails,
              null,
              userDetails.getAuthorities());

          // 设置认证请求的详细信息
          authToken.setDetails(
              new WebAuthenticationDetailsSource().buildDetails(request));

          // 确保在认证成功后设置SecurityContext
          SecurityContextHolder.getContext().setAuthentication(authToken);
        } else {
          log.warn("无效的token: {}", userEmail);
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }
      }
    } catch (Exception e) {
      log.error("JWT认证过程发生错误: {}", e.getMessage());
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    filterChain.doFilter(request, response);
  }
}