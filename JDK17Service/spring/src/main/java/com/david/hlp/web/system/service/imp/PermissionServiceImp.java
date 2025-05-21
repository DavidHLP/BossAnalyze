package com.david.hlp.web.system.service.imp;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.david.hlp.web.system.mapper.PermissionMapper;
import com.david.hlp.web.system.mapper.UserMapper;
import com.david.hlp.web.system.service.PermissionService;

import lombok.RequiredArgsConstructor;
import java.util.List;

/**
 * 权限服务实现类
 */
@Service
@RequiredArgsConstructor
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
        Assert.notNull(userId, "用户ID不能为空");

        Long roleId = userMapper.getByUserIdToUser(userId).getRoleId();
        return permissionMapper.listPermissionNamesByRoleId(roleId);
    }
}
