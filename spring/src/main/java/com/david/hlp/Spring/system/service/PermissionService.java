package com.david.hlp.Spring.system.service;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService {
    /**
     * 根据用户ID获取用户权限信息
     *
     * @param userId 用户ID
     * @return 权限列表
     * @throws IllegalArgumentException 当userId为null时抛出
     */
    List<String> getUserPermissions(Long userId);
}