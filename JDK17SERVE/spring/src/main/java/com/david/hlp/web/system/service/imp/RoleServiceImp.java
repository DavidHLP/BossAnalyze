package com.david.hlp.web.system.service.imp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.david.hlp.web.system.entity.permission.Permission;
import com.david.hlp.web.system.entity.role.Role;
import com.david.hlp.web.system.entity.role.RolePermission;
import com.david.hlp.web.system.entity.role.RolePermissionUpdateResponse;
import com.david.hlp.web.system.mapper.PermissionMapper;
import com.david.hlp.web.system.mapper.RoleMapper;
import com.david.hlp.web.system.mapper.RouterMapper;

import org.springframework.util.Assert;

import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
/**
 * 角色服务实现类
 *
 * @author david
 * @version 1.0
 * @since 2024/01/01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImp {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RouterMapper routerMapper;

    /**
     * 根据角色ID获取角色信息
     *
     * @param roleId 角色ID
     * @return 角色信息
     */
    public Role getRole(Long roleId) {
        try {
            Assert.notNull(roleId, "角色ID不能为空");
            return roleMapper.getRoleById(roleId);
        } catch (IllegalArgumentException e) {
            log.warn("获取角色信息失败: 角色ID为空");
            throw e;
        } catch (Exception e) {
            log.error("获取角色信息异常: 角色ID={}", roleId, e);
            throw e;
        }
    }

    /**
     * 根据角色名称查询角色列表
     *
     * @param roleName 角色名称
     * @return 角色列表
     */
    public List<Role> getRoleList(String roleName) {
        try {
            return roleMapper.listRoles(roleName);
        } catch (Exception e) {
            log.error("根据角色名称查询角色列表异常: roleName={}", roleName, e);
            throw e;
        }
    }

    /**
     * 获取所有角色列表，包含权限和路由信息
     *
     * @return 角色列表
     */
    public List<Role> getRoleList() {
        try {
            List<Role> roleList = roleMapper.listRoles(null);
            if (CollectionUtils.isEmpty(roleList)) {
                return new ArrayList<>();
            }

            for (Role role : roleList) {
                List<Permission> permissions = permissionMapper.listPermissionDetailsByRoleId(role.getId());
                role.setPermissions(permissions);
                if (!CollectionUtils.isEmpty(permissions)) {
                    List<String> permissionNames = permissions.stream()
                        .map(Permission::getPermission)
                        .collect(Collectors.toList());
                    role.setRouters(routerMapper.listByPermissions(permissionNames));
                }
            }
            return roleList;
        } catch (Exception e) {
            log.error("获取角色列表异常", e);
            throw e;
        }
    }

    /**
     * 添加角色
     *
     * @param role 角色信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void addRole(Role role) {
        try {
            Assert.notNull(role, "角色信息不能为空");
            Assert.hasText(role.getRoleName(), "角色名称不能为空");
            roleMapper.insertRole(role);
        } catch (IllegalArgumentException e) {
            log.warn("添加角色失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("添加角色异常: roleName={}", role != null ? role.getRoleName() : "null", e);
            throw e;
        }
    }

    /**
     * 编辑角色信息
     *
     * @param role 角色信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void editRole(Role role) {
        try {
            Assert.notNull(role, "角色信息不能为空");
            Assert.notNull(role.getId(), "角色ID不能为空");
            Assert.hasText(role.getRoleName(), "角色名称不能为空");
            roleMapper.updateRole(role);
        } catch (IllegalArgumentException e) {
            log.warn("编辑角色失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("编辑角色异常: roleId={}, roleName={}", 
                role != null ? role.getId() : "null", 
                role != null ? role.getRoleName() : "null", e);
            throw e;
        }
    }

    /**
     * 更新角色权限
     *
     * @param rolePermissionUpdateResponse 角色权限更新请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRolePermissions(RolePermissionUpdateResponse rolePermissionUpdateResponse) {
        try {
            Assert.notNull(rolePermissionUpdateResponse, "角色权限更新信息不能为空");
            Assert.notNull(rolePermissionUpdateResponse.getRoleId(), "角色ID不能为空");

            Long roleId = rolePermissionUpdateResponse.getRoleId();
            List<RolePermission> existingPermissions = roleMapper.listRolePermissions(roleId);
            List<Long> existingPermissionIds = existingPermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());

            List<Long> newPermissionIds = getNewPermissionIds(rolePermissionUpdateResponse);

            updateRolePermissionRelations(roleId, existingPermissionIds, newPermissionIds);
        } catch (IllegalArgumentException e) {
            log.warn("更新角色权限失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("更新角色权限异常: roleId={}", 
                rolePermissionUpdateResponse != null ? rolePermissionUpdateResponse.getRoleId() : "null", e);
            throw e;
        }
    }

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        try {
            Assert.notNull(roleId, "角色ID不能为空");
            roleMapper.deleteRolePermissionByRoleId(roleId);
            roleMapper.deleteRole(roleId);
        } catch (IllegalArgumentException e) {
            log.warn("删除角色失败: 角色ID为空");
            throw e;
        } catch (Exception e) {
            log.error("删除角色异常: roleId={}", roleId, e);
            throw e;
        }
    }

    /**
     * 获取新的权限ID列表
     *
     * @param rolePermissionUpdateResponse 角色权限更新请求
     * @return 新的权限ID列表
     */
    private List<Long> getNewPermissionIds(RolePermissionUpdateResponse rolePermissionUpdateResponse) {
        List<Long> newPermissionIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(rolePermissionUpdateResponse.getRouterIds())) {
            for (Integer routerId : rolePermissionUpdateResponse.getRouterIds()) {
                String permission = routerMapper.getPermissionById(routerId.longValue());
                if (permission != null) {
                    Long permissionId = permissionMapper.getIdByPermissionName(permission);
                    if (permissionId != null) {
                        newPermissionIds.add(permissionId);
                    } else {
                        log.warn("未找到权限ID: permission={}", permission);
                    }
                } else {
                    log.warn("未找到路由权限: routerId={}", routerId);
                }
            }
        }
        return newPermissionIds;
    }

    /**
     * 更新角色权限关系
     *
     * @param roleId 角色ID
     * @param existingPermissionIds 现有权限ID列表
     * @param newPermissionIds 新的权限ID列表
     */
    private void updateRolePermissionRelations(Long roleId, List<Long> existingPermissionIds, List<Long> newPermissionIds) {
        try {
            List<Long> toDeletePermissionIds = existingPermissionIds.stream()
                .filter(id -> !newPermissionIds.contains(id))
                .collect(Collectors.toList());

            List<Long> toAddPermissionIds = newPermissionIds.stream()
                .filter(id -> !existingPermissionIds.contains(id))
                .distinct()
                .collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(toDeletePermissionIds)) {
                for (Long permissionId : toDeletePermissionIds) {
                    roleMapper.deleteRolePermission(roleId, permissionId);
                }
            }

            if (!CollectionUtils.isEmpty(toAddPermissionIds)) {
                roleMapper.insertRolePermissions(roleId, toAddPermissionIds);
            }
        } catch (Exception e) {
            log.error("更新角色权限关系异常: roleId={}", roleId, e);
            throw e;
        }
    }
}
