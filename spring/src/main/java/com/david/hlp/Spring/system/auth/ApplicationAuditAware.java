package com.david.hlp.Spring.system.auth;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.lang.NonNull;

import java.util.Optional;

import com.david.hlp.Spring.system.entity.auth.AuthUser;

public class ApplicationAuditAware implements AuditorAware<Integer> {

    @Override
    @NonNull
    public Optional<Integer> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthUser) {
            return Optional.of(((AuthUser) principal).getUserId().intValue());
        }

        return Optional.empty();
    }
}