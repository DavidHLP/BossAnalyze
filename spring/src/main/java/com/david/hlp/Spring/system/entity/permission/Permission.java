package com.david.hlp.Spring.system.entity.permission;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限实体类
 *
 * @author david
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Permission implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;            // 对应 BIGINT 主键

    /**
     * 权限状态：0-禁用，1-启用
     */
    private Boolean enabled;

    /**
     * 权限备注
     */
    private String remark;      // 备注信息

    /**
     * 权限标识符
     */
    private String permission;  // 权限标识

    /**
     * 创建时间
     */
    private LocalDateTime createTime; // 创建时间

    /**
     * 更新时间
     */
    private LocalDateTime updateTime; // 更新时间
}