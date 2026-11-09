package com.david.hlp.web.system.auth;

import com.david.hlp.web.system.auth.entity.auth.AuthUser;
import com.david.hlp.web.system.auth.entity.role.Role;
import com.david.hlp.web.system.auth.mapper.*;
import com.david.hlp.web.system.auth.service.imp.*;
import com.david.hlp.web.system.auth.utils.JwtService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleSemanticsTest {
    @Test void refusesRegistrationWhenDefaultIsMissingAmbiguousOrDisabled() {
        RoleMapper roles = mock(RoleMapper.class);
        var service = new AuthServiceImp(mock(UserMapper.class), mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                mock(JwtService.class), roles, mock(TokenMapper.class), mock(com.david.hlp.commons.utils.RedisCacheHelper.class), mock(PermissionMapper.class));
        when(roles.getDefaultRoleId()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, service::getDefaultRoleId);
        when(roles.getDefaultRoleId()).thenReturn(9L);
        assertEquals(9L, service.getDefaultRoleId());
    }
    @Test void issuedSessionObservesRoleDisableAndNewPermissions() {
        UserMapper users = mock(UserMapper.class);
        TokenMapper tokens = mock(TokenMapper.class);
        JwtService jwt = mock(JwtService.class);
        RoleMapper roles = mock(RoleMapper.class);
        AuthUser account = AuthUser.builder().userId(1L).email("synthetic@example.invalid").roleId(7L).status(1).build();
        Role role = Role.builder().id(7L).status(1).build();
        when(tokens.checkTokenValid("synthetic-token")).thenReturn(true);
        when(jwt.extractUsername("synthetic-token")).thenReturn(account.getEmail());
        when(jwt.isTokenValid("synthetic-token", account)).thenReturn(true);
        when(users.getByEmailToAuthUser(account.getEmail())).thenReturn(account);
        when(roles.getRoleById(7L)).thenReturn(role);
        when(users.listAuthoritiesByRoleId(7L)).thenReturn(List.of("old.permission"), List.of("new.permission"));
        var service = new UserDetailsServiceImpl(users, tokens, jwt, roles);
        assertEquals("old.permission", service.loadUserByUsername("synthetic-token").getAuthorities().iterator().next().getAuthority());
        assertEquals("new.permission", service.loadUserByUsername("synthetic-token").getAuthorities().iterator().next().getAuthority());
        role.setStatus(0);
        assertThrows(RuntimeException.class, () -> service.loadUserByUsername("synthetic-token"));
        when(roles.getRoleById(7L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.loadUserByUsername("synthetic-token"));
    }
    @Test void rejectsDuplicateCreatesAndRenamesAndProtectsDefault() {
        RoleMapper mapper = mock(RoleMapper.class);
        var service = new RoleServiceImp(mapper, mock(PermissionMapper.class), mock(RouterMapper.class));
        when(mapper.countByName("taken", null)).thenReturn(1);
        assertThrows(com.david.hlp.web.common.exception.BusinessException.class, () -> service.addRole(Role.builder().id(2L).roleName(" taken ").build()));
        when(mapper.getRoleById(2L)).thenReturn(Role.builder().id(2L).roleName("custom").build());
        when(mapper.countByName("taken", 2L)).thenReturn(1);
        assertThrows(com.david.hlp.web.common.exception.BusinessException.class, () -> service.editRole(Role.builder().id(2L).roleName("taken").build()));
        when(mapper.getRoleById(1L)).thenReturn(Role.builder().id(1L).roleName("USER").build());
        assertThrows(com.david.hlp.web.common.exception.BusinessException.class, () -> service.editRole(Role.builder().id(1L).roleName("other").build()));
        assertThrows(com.david.hlp.web.common.exception.BusinessException.class, () -> service.deleteRole(1L));
        verify(mapper, never()).insertRole(any()); verify(mapper, never()).updateRole(any());
    }
}
