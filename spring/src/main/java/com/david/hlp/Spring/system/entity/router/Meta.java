package com.david.hlp.Spring.system.entity.router;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import java.io.Serializable;

/**
 * 路由元数据实体类
 *
 * @author david
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Meta implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 类型
     */
    private String type;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 重定向路径
     */
    private String redirect;

    /**
     * 是否总是显示
     */
    private Boolean alwaysShow;

    /**
     * 元标题
     */
    private String metaTitle;

    /**
     * 元图标
     */
    private String metaIcon;

    /**
     * 是否在菜单中隐藏
     */
    private Boolean metaHidden;

    /**
     * 存储角色名称数组的 JSON
     */
    private String metaRoles;

    /**
     * 是否缓存页面
     */
    private Boolean metaKeepAlive;

    /**
     * 是否整体隐藏
     */
    private Boolean hidden;
}
