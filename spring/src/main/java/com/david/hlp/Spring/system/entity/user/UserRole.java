package com.david.hlp.Spring.system.entity.user;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 用户角色关联实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRole {
    /** 用户ID */
    private Long userId;

    /** 角色ID */
    private Long roleId;
}