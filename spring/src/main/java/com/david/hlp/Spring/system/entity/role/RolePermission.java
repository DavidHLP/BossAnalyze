package com.david.hlp.Spring.system.entity.role;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 角色权限关联实体类
 *
 * @author david
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RolePermission implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long roleId;       // 角色ID
    private Long permissionId; // 权限ID
}