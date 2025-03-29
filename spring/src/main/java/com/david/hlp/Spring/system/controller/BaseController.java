package com.david.hlp.Spring.system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.david.hlp.Spring.system.entity.auth.AuthUser;
import lombok.extern.slf4j.Slf4j;

/**
 * 基础控制器类，提供通用的控制器功能
 *
 * @author david
 * @since 1.0
 */
@Slf4j
public class BaseController {

    /**
     * 获取当前登录用户的ID
     *
     * @return 当前用户ID，如果用户未登录则返回null
     */
    protected Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("No authentication found in security context");
            return null;
        }
        try {
            AuthUser user = (AuthUser) authentication.getPrincipal();
            Long userId = user.getUserId();
            log.debug("Retrieved current user ID: {}", userId);
            return userId;
        } catch (ClassCastException e) {
            log.error("Failed to cast principal to AuthUser", e);
            return null;
        }
    }
}
