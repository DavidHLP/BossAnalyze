package com.david.hlp.web.system.auth.filter;

import com.david.hlp.web.system.auth.mapper.RoleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.david.hlp.web.common.entity.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserDetailsService userDetailsService;
    private final RoleMapper roleMapper;
    private final ObjectMapper objectMapper;

    static final Set<String> PUBLIC_PATHS = Set.of("/api/auth/login", "/api/auth/register",
            "/api/auth/logout", "/api/auth/sendRegisterEmail", "/api/repeater/auth/login",
            "/actuator/health", "/actuator/health/readiness", "/actuator/health/liveness");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        long timestamp = System.currentTimeMillis();
        String path = request.getServletPath();
        if ("OPTIONS".equals(request.getMethod()) || PUBLIC_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || header.substring(7).isBlank()) {
            reject(response, 401);
            return;
        }
        try {
            // Always reload: role disablement and permission revocation apply to existing tokens.
            var user = userDetailsService.loadUserByUsername(header.substring(7));
            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String page = requiredPage(path);
            if (page != null && roleMapper.permissionsForPath(page).stream().noneMatch(permission ->
                    authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(permission)))) {
                reject(response, 403);
                return;
            }
            if (path.equals("/api/v1/system/logs/analysis/trigger") && authentication.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("logs.analysis.trigger"))) {
                reject(response, 403);
                return;
            }
        } catch (AuthenticationException | io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            log.warn("Authentication rejected: {}", e.getClass().getSimpleName());
            reject(response, 401);
            return;
        } catch (Exception e) {
            log.error("Identity lookup unavailable: {}", e.getClass().getSimpleName());
            reject(response, 503);
            return;
        }
        log.info("ACCESS|ts={}|ip={}|path={}|method={}|ua={}", timestamp, request.getRemoteAddr(),
                path, request.getMethod(), request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "-");
        chain.doFilter(request, response);
    }

    private static String requiredPage(String path) {
        if (path.startsWith("/api/user/")) return "/console/users";
        if (path.startsWith("/api/role/") || Set.of("/api/auth/getRoleList", "/api/auth/addRole",
                "/api/auth/editRole", "/api/auth/deleteRole", "/api/auth/updateRoleRouters").contains(path)) return "/console/roles";
        if (Set.of("/api/auth/editRouter", "/api/auth/addRouter", "/api/auth/deleteRouter").contains(path)) return "/console/menus";
        if (path.equals("/api/v1/system/logs") || path.startsWith("/api/v1/system/logs/")) return "/console/dashboard";
        return null;
    }

    private void reject(HttpServletResponse response, int status) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.error(status,
                status == 503 ? "身份服务暂不可用" : status == 403 ? "禁止访问" : "未授权访问").getBody());
    }
}
