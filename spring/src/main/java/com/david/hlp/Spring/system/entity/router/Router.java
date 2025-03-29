package com.david.hlp.Spring.system.entity.router;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;
import lombok.ToString;
import java.io.Serializable;

import java.util.List;
import java.util.ArrayList;

/**
 * 路由实体类
 *
 * @author david
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Router implements Serializable {

    private static final long serialVersionUID = 2L;

    /**
     * 主键ID
     */
    private Long id;            // BIGINT 主键

    /**
     * 父路由ID
     */
    private Long pid;           // 父路由 ID

    /**
     * 菜单顺序
     */
    private Integer menuOrder;  // 菜单顺序

    /**
     * 状态
     */
    private Integer status;     // 状态字段

    /**
     * 备注信息
     */
    private String remark;      // 备注信息

    /**
     * 关联的权限标识
     */
    private String permission;  // 关联的权限标识

    /**
     * 路由路径
     */
    private String path;        // 路由路径

    /**
     * 路由名称
     */
    private String name;        // 路由名称

    /**
     * 图标
     */
    private String icon;        // 图标

    /**
     * 路由元数据
     */
    private Meta meta;

    /**
     * 子路由列表
     */
    @Default
    private List<Router> children = new ArrayList<>(); // 子路由
    // 如需记录创建/更新时间，可在表结构或实体里继续添加
    // private LocalDateTime createTime;
    // private LocalDateTime updateTime;
}