package com.david.hlp.web.system.service.imp;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.david.hlp.web.system.mapper.PermissionMapper;
import com.david.hlp.web.system.mapper.UserMapper;
import com.david.hlp.web.system.service.PermissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

/**
 * 权限服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImp implements PermissionService {
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;

    /**
     * 根据用户ID获取用户权限信息
     *
     * @param userId 用户ID
     * @return 权限列表
     * @throws IllegalArgumentException 当userId为null时抛出
     */
    @Override
    public List<String> getUserPermissions(Long userId) {
        try {
            Assert.notNull(userId, "用户ID不能为空");
        } catch (IllegalArgumentException e) {
            log.warn("获取用户权限失败: 用户ID为空");
            throw e;
        }

        try {
            Long roleId = userMapper.getByUserIdToUser(userId).getRoleId();
            return permissionMapper.listPermissionNamesByRoleId(roleId);
        } catch (NullPointerException e) {
            log.error("获取用户权限失败: 用户ID={}不存在或没有关联角色", userId);
            throw e;
        } catch (Exception e) {
            log.error("获取用户权限异常: 用户ID={}", userId, e);
            throw e;
        }
    }
}
