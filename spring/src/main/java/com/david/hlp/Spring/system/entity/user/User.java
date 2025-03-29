package com.david.hlp.Spring.system.entity.user;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 用户实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    /** 主键ID */
    private Long id;             // BIGINT 主键

    /** 用户名 */
    private String name;         // 用户名

    /** 头像URL */
    private String avatar;       // 头像

    /** 用户简介 */
    @Default
    private String introduction = "用户未填写";      // 简介

    /** 邮箱地址（唯一） */
    private String email;        // 邮箱（唯一）

    /** 用户状态 */
    private Integer status;      // 用户状态

    /** 用户地址 */
    @Default
    private String address = "用户未填写";      // 地址

    /** 上次登录IP */
    private String lastLoginIp;  // 上次登录 IP

    /** 上次登录时间 */
    private LocalDateTime lastLogin;   // 上次登录时间

    /** 角色ID */
    private Long roleId;         // 角色 ID

    /** 角色名称 */
    private String roleName;     // 角色名称

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createTime;  // 创建时间

    /** 加密后的密码 */
    private String password;     // 加密后的密码

    /**
     * 获取用户ID
     * @return 用户ID
     */
    public Long getUserId() {
        return id;
    }

    /**
     * 获取用户名
     * @return 用户名
     */
    public String getUsername() {
        return name;
    }
}