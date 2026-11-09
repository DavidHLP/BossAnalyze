package com.david.hlp.web.system.auth;

import com.david.hlp.web.system.auth.filter.JwtAuthenticationFilter;
import com.david.hlp.web.system.auth.mapper.*;
import com.david.hlp.web.system.auth.entity.auth.AuthUser;
import com.david.hlp.web.system.auth.entity.role.Role;
import com.david.hlp.web.system.auth.entity.user.User;
import com.david.hlp.web.system.auth.service.imp.*;
import com.david.hlp.web.system.auth.service.PasswordService;
import com.david.hlp.web.system.auth.utils.JwtService;
import com.david.hlp.web.common.entity.Result;
import com.david.hlp.web.common.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IdentitySecurityTest {
    final UserDetailsService details = mock(UserDetailsService.class);
    final RoleMapper roles = mock(RoleMapper.class);
    final ObjectMapper json = new ObjectMapper();
    final RouterMapper routers = mock(RouterMapper.class);
    final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(details, new com.david.hlp.web.system.auth.filter.ConsoleAccess(routers), json);
    void page(String path, String permission) {
        var router = new com.david.hlp.web.system.auth.entity.router.Router();
        router.setPath(path); router.setPermission(permission); router.setStatus(1);
        when(routers.listAll()).thenReturn(List.of(router));
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    AuthUser principal(String... grants) {
        return AuthUser.builder().userId(1L).email("synthetic@example.test").status(1).roleId(2L)
                .password("synthetic-hash").authorities(List.of(grants)).build();
    }

    int request(String path, String token) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        if (token != null) request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        AtomicBoolean reached = new AtomicBoolean();
        filter.doFilter(request, response, (a,b) -> reached.set(true));
        assertEquals(response.getStatus() == 200, reached.get(), "denied request must not reach controller/mapper");
        return response.getStatus();
    }

    @Test void managementAndLogsFailClosedAndResolveConfiguredPermission() throws Exception {
        when(details.loadUserByUsername("test-token")).thenReturn(principal("ROLE_USER"));
        for (String path : List.of("/api/user/updateUser", "/api/role/getRoleList", "/api/auth/addRole",
                "/api/auth/updateRoleRouters", "/api/auth/addRouter",
                "/api/v1/system/logs/analysis/trigger")) {
            assertEquals(401, request(path, null));
            assertEquals(403, request(path, "test-token"));
        }
        page("/console/users", "users:manage");
        when(details.loadUserByUsername("test-token")).thenReturn(principal("users:manage"));
        assertEquals(200, request("/api/user/updateUser", "test-token"));
        when(details.loadUserByUsername("test-token")).thenReturn(principal("ROLE_USER"));
        assertEquals(403, request("/api/user/updateUser", "test-token"));
    }

    @Test void dependentRoleReadsUseUsersOrMenusGrantsAndTriggerStillNeedsDashboard() throws Exception {
        page("/users", "users:manage");
        when(details.loadUserByUsername("t")).thenReturn(principal("users:manage"));
        assertEquals(200, request("/api/role/getRoleList", "t"));
        assertEquals(403, request("/api/auth/addRole", "t"));
        page("menus", "menus:manage");
        when(details.loadUserByUsername("t")).thenReturn(principal("menus:manage"));
        assertEquals(200, request("/api/auth/getRoleList", "t"));
        assertEquals(403, request("/api/auth/addRole", "t"));
        when(details.loadUserByUsername("t")).thenReturn(principal("logs.analysis.trigger"));
        assertEquals(403, request("/api/v1/system/logs/analysis/trigger", "t"));
        assertEquals(200, request("/api/v1/system/logs/health", "t"));
    }

    @Test void logTriggerRequiresPageAndSeparateGrant() throws Exception {
        page("/console/dashboard", "dashboard:read");
        when(details.loadUserByUsername("t")).thenReturn(principal("dashboard:read"));
        assertEquals(200, request("/api/v1/system/logs/stats", "t"));
        assertEquals(403, request("/api/v1/system/logs/analysis/trigger", "t"));
        when(details.loadUserByUsername("t")).thenReturn(principal("dashboard:read", "logs.analysis.trigger"));
        assertEquals(200, request("/api/v1/system/logs/analysis/trigger", "t"));
    }

    @Test void publicPathsAreExactAndLookupFailureIsNotInvalidSession() throws Exception {
        assertEquals(200, request("/api/auth/logout", null));
        assertEquals(401, request("/api/auth/login-extra", null));
        assertEquals(401, request("/api/auth/getUserPrivateInformation", null));
        when(details.loadUserByUsername("t")).thenThrow(new DataAccessResourceFailureException("private database detail"));
        assertEquals(503, request("/api/auth/getUserPrivateInformation", "t"));
    }

    @Test void disabledRoleAndRevokedTokenRejectOldCredentials() {
        var users = mock(UserMapper.class); var tokens = mock(TokenMapper.class); var jwt = mock(JwtService.class);
        var service = new UserDetailsServiceImpl(users, tokens, jwt, roles);
        when(tokens.checkTokenValid("t")).thenReturn(true);
        when(jwt.extractUsername("t")).thenReturn("synthetic@example.test");
        when(users.getByEmailToAuthUser(anyString())).thenReturn(principal());
        when(roles.getRoleById(2L)).thenReturn(Role.builder().id(2L).status(0).build());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("t"));
        verify(users, never()).listAuthoritiesByRoleId(anyLong());
        when(tokens.checkTokenValid("t")).thenReturn(false);
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("t"));
    }

    @Test void logoutRevokesAndIsIdempotent() {
        var tokens = mock(TokenMapper.class); var service = new LogoutService(tokens);
        var request = new MockHttpServletRequest(); var response = new MockHttpServletResponse();
        service.logout(request,response,null); verifyNoInteractions(tokens);
        request.addHeader("Authorization", "Bearer synthetic-token");
        service.logout(request,response,null); service.logout(request,response,null);
        verify(tokens,times(2)).revoke("synthetic-token");
    }

    @Test void hashesNeverSerializeButRemainAvailableToPasswordAuthentication() throws Exception {
        AuthUser user = principal();
        assertEquals("synthetic-hash", user.getPassword());
        String serialized = json.writeValueAsString(user);
        assertFalse(serialized.contains("password")); assertFalse(serialized.contains("synthetic-hash"));
    }

    @Test void userEditRejectsAnyPasswordWithoutWriting() {
        var users = mock(UserMapper.class);
        var service = new UserServiceImp(users,mock(PasswordService.class));
        var edit = User.builder().id(1L).password("plain").build();
        assertThrows(BusinessException.class, () -> service.updateUser(edit)); verifyNoInteractions(users);
        edit.setPassword(null); service.updateUser(edit); verify(users).updateById(edit);
    }

    @Test void defaultRoleAndDuplicateNamesAreProtectedBeforeWrites() {
        var service = new RoleServiceImp(roles,mock(PermissionMapper.class),mock(RouterMapper.class));
        when(roles.getRoleById(2L)).thenReturn(Role.builder().id(2L).roleName("USER").status(1).build());
        assertThrows(BusinessException.class, () -> service.deleteRole(2L));
        assertThrows(BusinessException.class, () -> service.editRole(Role.builder().id(2L).roleName("other").status(1).build()));
        when(roles.countByName("USER", null)).thenReturn(1);
        assertThrows(BusinessException.class, () -> service.addRole(Role.builder().roleName("USER").build()));
        verify(roles,never()).insertRole(any()); verify(roles,never()).updateRole(any());
        verify(roles,never()).deleteRolePermissionByRoleId(anyLong());
    }

    @Test void loginReturnsAndPersistsOnlySanitizedPrincipal() throws Exception {
        var users = mock(UserMapper.class); var tokens = mock(TokenMapper.class);
        var passwords = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        var jwt = mock(JwtService.class);
        var service = new AuthServiceImp(users, passwords, jwt, roles, tokens,
                mock(com.david.hlp.commons.utils.RedisCacheHelper.class), mock(PermissionMapper.class));
        when(users.getByEmailToAuthUser(anyString())).thenReturn(principal());
        when(users.listAuthoritiesByRoleId(2L)).thenReturn(List.of("read"));
        when(roles.getRoleById(2L)).thenReturn(Role.builder().id(2L).status(1).build());
        when(passwords.matches("synthetic-input", "synthetic-hash")).thenReturn(true);
        when(jwt.generateToken(any(AuthUser.class))).thenReturn("synthetic-token");
        var login = new com.david.hlp.web.system.auth.entity.auth.LoginDTO();
        login.setEmail("synthetic@example.test"); login.setPassword("synthetic-input");
        var token = service.login(login);
        assertNull(token.getAuthUser().getPassword());
        assertFalse(json.writeValueAsString(token).contains("synthetic-hash"));
        verify(tokens).save(argThat(saved -> saved.getAuthUser().getPassword() == null));
    }

    @Test void failedAuthenticationLogsNeverContainCredential() {
        var users = mock(UserMapper.class); var tokens = mock(TokenMapper.class);
        var service = new UserDetailsServiceImpl(users, tokens, mock(JwtService.class), roles);
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(UserDetailsServiceImpl.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("sensitive-synthetic-token"));
            assertFalse(appender.list.isEmpty());
            assertTrue(appender.list.stream().noneMatch(event -> event.getFormattedMessage().contains("sensitive-synthetic-token")));
        } finally { logger.detachAppender(appender); appender.stop(); }
    }

    @Test void routerControllerPreservesResourceWorkersBusinessStatuses() throws Exception {
        var routers = mock(RouterServiceImp.class);
        var controller = mock(com.david.hlp.web.system.auth.controller.AuthController.class, CALLS_REAL_METHODS);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "routerService", routers);
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        for (int code : List.of(403,404,409)) {
            doThrow(new BusinessException(code, "synthetic resource rejection")).when(routers).editRouter(any());
            doThrow(new BusinessException(code, "synthetic resource rejection")).when(routers).deleteRouter(any());
            doThrow(new BusinessException(code, "synthetic resource rejection")).when(routers).addRouter(any());
            for (String operation : List.of("editRouter", "deleteRouter", "addRouter")) {
                mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/" + operation)
                        .contentType("application/json").content("{}"))
                        .andExpect(status().is(code)).andExpect(jsonPath("$.code").value(code));
            }
        }
    }

    @RestController static class ErrorEndpoint {
        @GetMapping("/error-test") Result<Void> error(@RequestParam int code) { throw new BusinessException(code,"synthetic"); }
        @GetMapping("/api-error-test") Result<Void> api() { throw new ApiException(com.david.hlp.web.common.enums.ResultCode.FORBIDDEN); }
    }
    @Test void httpAndEnvelopeErrorsAgree() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ErrorEndpoint()).setControllerAdvice(new GlobalExceptionHandler()).build();
        for (int code : List.of(400,401,403,404,409,500,503)) {
            mvc.perform(get("/error-test").param("code",Integer.toString(code)))
                    .andExpect(status().is(code)).andExpect(jsonPath("$.code").value(code));
        }
        mvc.perform(get("/api-error-test")).andExpect(status().isForbidden());
    }

    @Test void wrongDeletionConfirmationDoesNotInvalidateTheSessionOrDelete() throws Exception {
        var users = mock(UserServiceImp.class);
        var passwords = mock(PasswordService.class);
        var auth = mock(AuthServiceImp.class);
        when(auth.getPassword(1L)).thenReturn("synthetic-hash");
        when(passwords.matches("wrong-confirmation", "synthetic-hash")).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal(), null, List.of()));
        var mvc = MockMvcBuilders.standaloneSetup(
                new com.david.hlp.web.system.auth.controller.UserController(users, passwords, auth))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/user/deleteUser")
                .contentType("application/json").content("{\"id\":2,\"password\":\"wrong-confirmation\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1006));
        verify(users, never()).deleteUser(any());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
