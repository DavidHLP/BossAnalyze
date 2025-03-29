package com.david.hlp.Spring.system.entity.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.AllArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Spring Security 用的用户类，实现 UserDetails 接口。
 */
@Builder
@AllArgsConstructor
public class AuthUser implements UserDetails {

    private final Long userId;               // 数据库中的用户主键ID
    private final String username;           // 登录名（在此示例中使用 email 或其他字段）
    private final String email;              // 邮箱
    private final String password;           // 加密后的密码
    private final Integer status;            // 用户状态：0-禁用，1-启用
    private final Long roleId;               // 角色ID
    private List<String> authorities;

    public AuthUser(
            Long userId,
            String username,
            String password,
            Integer status,
            Long roleId,
            String email
    ) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.status = status;
        this.roleId = roleId;
        this.email = email;
    }

    // ========== 公共方法 ==========
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Objects.nonNull(status) && status == 1;
    }

    // ========== getter/setter方法 ==========

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public String getEmail() {
        return email;
    }

    public Integer getStatus() {
        return status;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    @Override
    public String toString() {
        return "AuthUser{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", roleId=" + roleId +
                ", authorities=" + authorities +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthUser)) {
            return false;
        }
        AuthUser authUser = (AuthUser) o;
        return Objects.equals(userId, authUser.userId) &&
                Objects.equals(email, authUser.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email);
    }
}