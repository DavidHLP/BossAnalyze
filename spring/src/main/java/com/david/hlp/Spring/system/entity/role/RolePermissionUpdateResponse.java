package com.david.hlp.Spring.system.entity.role;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * 角色权限更新响应实体类
 *
 * @author david
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RolePermissionUpdateResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long roleId;
    private List<Integer> routerIds;
}
