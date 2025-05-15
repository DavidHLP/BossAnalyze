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
    "/swagger-ui/",
    "/doc.html",
    "/doc.html/",
    "/v3/api-docs",
    "/v3/api-docs/",
    "/webjars/",
    "/authenticate",
    "/swagger-ui.html/",
    "/swagger-resources",
    "/swagger-resources/",
    "/api/proxy/",
    "/api/boss/basic/",
    "/api/boss/user/",
    "/api/resume/",
    "/api/image/"
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
          @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    // 总是允许 OPTIONS 请求通过（CORS预检请求）
    if (request.getMethod().equals("OPTIONS")) {
        filterChain.doFilter(request, response);
        return;
    }

    // 1. 检查是否为公开路径
    String path = request.getServletPath();
    boolean isPublicPath = Arrays.stream(publicPaths).anyMatch(path::startsWith);

    // 2. 检查Authorization头
    final String authHeader = request.getHeader("Authorization");

    // 3. 如果不是公开路径且没有有效token，直接返回401
    if (!isPublicPath && (authHeader == null || !authHeader.startsWith("Bearer "))) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
    }

    // 4. 如果是公开路径，允许通过
    if (isPublicPath) {
        filterChain.doFilter(request, response);
        return;
    }

    // 5. 处理正常的带token请求
    final String jwt = authHeader.substring(7);
    final String userEmail = jwtService.extractUsername(jwt);

    // 验证用户并设置认证信息
    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      // 从 UserDetailsService 加载用户信息
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

      // 检查 JWT 是否在数据库中有效，且未过期或撤销
      var isTokenValid = tokenMapper.checkTokenValid(jwt);

      // 验证 JWT 是否有效
      if (jwtService.isTokenValid(jwt, userDetails) && isTokenValid) {
        // 直接从UserDetails获取权限
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        // 设置认证请求的详细信息
        authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        log.info("认证成功，设置认证信息: {}", authToken);

        // 确保在认证成功后设置SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }

    filterChain.doFilter(request, response);
  }

  // @Override
  // protected boolean shouldNotFilter(HttpServletRequest request) {
  //   return request.getServletPath().startsWith("/api/auth/");
  // }
}