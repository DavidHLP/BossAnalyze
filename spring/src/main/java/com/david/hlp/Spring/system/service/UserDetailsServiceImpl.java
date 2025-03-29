package com.david.hlp.Spring.system.service;

// Java核心导入
import java.util.ArrayList;
import java.util.List;

// Spring框架导入
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

// 项目内部导入
import com.david.hlp.Spring.system.entity.auth.AuthUser;
import com.david.hlp.Spring.system.mapper.UserMapper;

// Lombok导入
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户详情服务实现类
 * 实现Spring Security的UserDetailsService接口
 * 用于加载用户特定数据的核心接口
 *
 * @author david
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    /**
     * 根据用户邮箱加载用户详情
     *
     * @param username 用户邮箱或用户名
     * @return UserDetails 用户详情
     * @throws UsernameNotFoundException 当用户不存在时抛出此异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Assert.hasText(username, "用户名不能为空");
        log.debug("Loading user by username: {}", username);
        // 尝试通过邮箱查找用户
        AuthUser user = userMapper.getByEmailToAuthUser(username);
        if (user == null) {
            // 如果通过邮箱没找到，尝试通过用户名查找
            user = userMapper.getByUsername(username);
        }
        if (user == null) {
            log.warn("User not found with username/email: {}", username);
            throw new UsernameNotFoundException("用户不存在");
        }

        Assert.notNull(user.getRoleId(), "用户角色ID不能为空");

        // 加载用户权限
        List<String> authorities = userMapper.listAuthoritiesByRoleId(user.getRoleId());
        if (authorities == null) {
            authorities = new ArrayList<>();
        }
        Assert.notNull(authorities, "用户权限列表不能为空");
        // 设置用户权限
        user.setAuthorities(authorities);
        log.debug("User found: {}, with {} authorities", user.getUsername(), authorities.size());
        return user;
    }
}