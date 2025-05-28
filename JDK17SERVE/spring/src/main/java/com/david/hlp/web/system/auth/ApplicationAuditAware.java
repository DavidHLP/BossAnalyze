package com.david.hlp.web.system.auth;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.david.hlp.web.system.entity.auth.AuthUser;

import org.springframework.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

@Slf4j
public class ApplicationAuditAware implements AuditorAware<Integer> {

    @Override
    @NonNull
    public Optional<Integer> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("获取审计用户失败: 认证为空或未通过认证");
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthUser) {
            return Optional.of(((AuthUser) principal).getUserId().intValue());
        }

        log.error("获取审计用户失败: Principal类型不匹配, 类型为: {}", principal.getClass().getName());
        return Optional.empty();
    }
}