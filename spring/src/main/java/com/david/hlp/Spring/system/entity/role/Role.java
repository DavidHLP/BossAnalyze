package com.david.hlp.Spring.system.entity.role;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import com.david.hlp.Spring.system.entity.permission.Permission;
import com.david.hlp.Spring.system.entity.router.Router;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/**
 * 对应数据库中的 role 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;         // BIGINT 主键
    private Long userId;     // BIGINT 用户ID
    private String roleName; // 角色名称
    private Integer status;  // 状态
    private String remark;   // 备注信息
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private List<Permission> permissions; // 权限列表
    private List<Router> routers; // 路由列表
}