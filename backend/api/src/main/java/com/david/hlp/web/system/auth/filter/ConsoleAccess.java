package com.david.hlp.web.system.auth.filter;

import com.david.hlp.web.system.auth.mapper.RouterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.Arrays;

/** Uses the same configured page permission as the console, without assuming a role name. */
@Component
@RequiredArgsConstructor
public class ConsoleAccess {
    private final RouterMapper routerMapper;

    public AuthorizationDecision check(Authentication auth, String page) {
        return checkAny(auth, page);
    }

    public AuthorizationDecision checkAny(Authentication auth, String... pages) {
        if (auth == null || !auth.isAuthenticated()) return new AuthorizationDecision(false);
        boolean allowed = routerMapper.listAll().stream().anyMatch(router ->
                Integer.valueOf(1).equals(router.getStatus()) &&
                Arrays.stream(pages).anyMatch(page -> ("/console/" + page).equals(router.getPath()) ||
                        ("/" + page).equals(router.getPath()) || page.equals(router.getPath())) &&
                router.getPermission() != null && !router.getPermission().isBlank() &&
                auth.getAuthorities().stream().anyMatch(grant -> grant.getAuthority().equals(router.getPermission())));
        return new AuthorizationDecision(allowed);
    }
}
