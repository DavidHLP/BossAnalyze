package com.david.hlp.web.system.auth;

import com.david.hlp.web.system.auth.filter.*;
import com.david.hlp.web.system.auth.entity.router.Router;
import com.david.hlp.web.system.auth.mapper.RouterMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AccessBoundaryTest.Config.class)
@WebAppConfiguration
class AccessBoundaryTest {
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import(SecurityConfiguration.class)
    static class Config {
        @Bean RouterMapper routers() {
            RouterMapper mapper = mock(RouterMapper.class);
            Router page = new Router();
            page.setPath("/console/users"); page.setStatus(1); page.setPermission("existing.users");
            Router dashboard = new Router();
            dashboard.setPath("/console/dashboard"); dashboard.setStatus(1); dashboard.setPermission("existing.dashboard");
            Router roles = new Router(); roles.setPath("/console/roles"); roles.setStatus(1); roles.setPermission("existing.roles");
            Router menus = new Router(); menus.setPath("/console/menus"); menus.setStatus(1); menus.setPermission("existing.menus");
            when(mapper.listAll()).thenReturn(List.of(page, dashboard, roles, menus));
            return mapper;
        }
        @Bean ConsoleAccess access(RouterMapper mapper) { return new ConsoleAccess(mapper); }
        @Bean AuthenticationProvider provider() { return mock(AuthenticationProvider.class); }
        @Bean JwtAuthenticationFilter jwt() {
            UserDetailsService users = token -> {
                if (token.equals("invalid")) throw new org.springframework.security.core.userdetails.UsernameNotFoundException("invalid");
                return User.withUsername("synthetic").password("unused")
                    .authorities(token.equals("allowed") ? new String[]{"existing.users", "existing.dashboard", "existing.roles", "existing.menus"} : new String[]{"ROLE_USER"}).build();
            };
            return new JwtAuthenticationFilter(users);
        }
        @Bean Endpoints endpoints() { return new Endpoints(); }
    }
    @RestController
    static class Endpoints {
        @RequestMapping({"/api/role/getRoleList", "/api/auth/getRoleList", "/api/auth/addRole", "/api/auth/editRole",
                "/api/auth/deleteRole", "/api/auth/updateRoleRouters", "/api/auth/addRouter", "/api/auth/editRouter", "/api/auth/deleteRouter",
                "/api/user/addUser", "/api/user/deleteUser", "/api/user/getUserManageInfo", "/api/user/updateUser", "/api/auth/getUserRole",
                "/api/auth/getUserPrivateInformation", "/api/auth/getRouters", "/api/auth/getUserBaseInfo",
                "/api/v1/system/logs/dashboard/stats", "/api/v1/system/logs/analysis/trigger",
                "/api/v1/system/logs/health", "/api/v1/boss/basic/city-name-list", "/api/auth/login-suffix"})
        String ok() { return "ok"; }
    }
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build(); }

    @Test void protectsManagementLogsAndRecruitment() throws Exception {
        for (String path : List.of("/api/user/getUserManageInfo", "/api/user/updateUser", "/api/v1/system/logs/dashboard/stats",
                "/api/v1/system/logs/analysis/trigger", "/api/v1/system/logs/health", "/api/v1/boss/basic/city-name-list", "/api/auth/login-suffix")) {
            mvc.perform(get(path).servletPath(path)).andExpect(status().isUnauthorized());
        }
        for (String path : List.of("/api/user/getUserManageInfo", "/api/user/updateUser", "/api/user/addUser", "/api/user/deleteUser",
                "/api/role/getRoleList", "/api/auth/getRoleList", "/api/auth/addRole", "/api/auth/editRole", "/api/auth/deleteRole",
                "/api/auth/updateRoleRouters", "/api/auth/addRouter", "/api/auth/editRouter", "/api/auth/deleteRouter",
                "/api/v1/system/logs/dashboard/stats")) {
            mvc.perform(get(path).servletPath(path).header("Authorization", "Bearer ordinary")).andExpect(status().isForbidden());
            mvc.perform(get(path).servletPath(path).header("Authorization", "Bearer allowed")).andExpect(status().isOk());
        }
        mvc.perform(post("/api/v1/system/logs/analysis/trigger").servletPath("/api/v1/system/logs/analysis/trigger").header("Authorization", "Bearer allowed")).andExpect(status().isForbidden());
    }
    @Test void rejectsInvalidToken() throws Exception {
        mvc.perform(get("/api/v1/boss/basic/city-name-list").servletPath("/api/v1/boss/basic/city-name-list")
                .header("Authorization", "Bearer invalid")).andExpect(status().isUnauthorized());
    }
    @Test void keepsOwnReadsAndRecruitmentAvailable() throws Exception {
        for (String path : List.of("/api/auth/getUserRole", "/api/auth/getUserBaseInfo", "/api/auth/getUserPrivateInformation",
                "/api/auth/getRouters", "/api/v1/system/logs/health", "/api/v1/boss/basic/city-name-list")) {
            mvc.perform(get(path).servletPath(path).header("Authorization", "Bearer ordinary")).andExpect(status().isOk());
        }
    }
}
