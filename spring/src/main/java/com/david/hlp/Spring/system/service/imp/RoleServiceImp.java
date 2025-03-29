package com.david.hlp.Spring.system.service.imp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.Assert;
import com.david.hlp.Spring.system.mapper.RoleMapper;
import com.david.hlp.Spring.system.entity.role.Role;
import com.david.hlp.Spring.system.entity.permission.Permission;
import com.david.hlp.Spring.system.mapper.PermissionMapper;
import com.david.hlp.Spring.system.mapper.RouterMapper;
import com.david.hlp.Spring.system.entity.role.RolePermissionUpdateResponse;
import com.david.hlp.Spring.system.entity.role.RolePermission;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.Arrays;
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
        Assert.notNull(roleId, "角色ID不能为空");
        return roleMapper.getRoleById(roleId);
    }

    /**
     * 根据角色名称查询角色列表
     *
     * @param roleName 角色名称
     * @return 角色列表
     */
    public List<Role> getRoleList(String roleName) {
        return roleMapper.listRoles(roleName);
    }

    /**
     * 获取所有角色列表，包含权限和路由信息
     *
     * @return 角色列表
     */
    public List<Role> getRoleList() {
        List<Role> roleList = roleMapper.listRoles(null);
        log.info("获取所有角色列表，包含权限和路由信息: {}", Arrays.toString(roleList.toArray()));
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
    }

    /**
     * 添加角色
     *
     * @param role 角色信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void addRole(Role role) {
        Assert.notNull(role, "角色信息不能为空");
        Assert.hasText(role.getRoleName(), "角色名称不能为空");
        roleMapper.insertRole(role);
    }

    /**
     * 编辑角色信息
     *
     * @param role 角色信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void editRole(Role role) {
        Assert.notNull(role, "角色信息不能为空");
        Assert.notNull(role.getId(), "角色ID不能为空");
        Assert.hasText(role.getRoleName(), "角色名称不能为空");
        roleMapper.updateRole(role);
    }

    /**
     * 更新角色权限
     *
     * @param rolePermissionUpdateResponse 角色权限更新请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRolePermissions(RolePermissionUpdateResponse rolePermissionUpdateResponse) {
        Assert.notNull(rolePermissionUpdateResponse, "角色权限更新信息不能为空");
        Assert.notNull(rolePermissionUpdateResponse.getRoleId(), "角色ID不能为空");

        Long roleId = rolePermissionUpdateResponse.getRoleId();
        List<RolePermission> existingPermissions = roleMapper.listRolePermissions(roleId);
        List<Long> existingPermissionIds = existingPermissions.stream()
            .map(RolePermission::getPermissionId)
            .collect(Collectors.toList());

        List<Long> newPermissionIds = getNewPermissionIds(rolePermissionUpdateResponse);

        updateRolePermissionRelations(roleId, existingPermissionIds, newPermissionIds);
    }

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        Assert.notNull(roleId, "角色ID不能为空");
        roleMapper.deleteRolePermissionByRoleId(roleId);
        roleMapper.deleteRole(roleId);
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
                    }
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
    }
}
