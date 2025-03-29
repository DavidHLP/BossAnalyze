package com.david.hlp.Spring.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.david.hlp.Spring.system.entity.router.Router;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RouterMapper extends BaseMapper<Router> {

    @Select("SELECT id, pid, menu_order, status, remark, permission, path, name, icon, " +
           "type, component, redirect, always_show, meta_title, meta_icon, meta_hidden, " +
           "meta_roles, meta_keep_alive, hidden FROM router")
    @Results(id = "routerResultMap", value = {
        @Result(property = "id", column = "id", id = true),
        @Result(property = "pid", column = "pid"),
        @Result(property = "menuOrder", column = "menu_order"),
        @Result(property = "status", column = "status"),
        @Result(property = "remark", column = "remark"),
        @Result(property = "permission", column = "permission"),
        @Result(property = "path", column = "path"),
        @Result(property = "name", column = "name"),
        @Result(property = "icon", column = "icon"),
        @Result(property = "meta.type", column = "type"),
        @Result(property = "meta.component", column = "component"),
        @Result(property = "meta.redirect", column = "redirect"),
        @Result(property = "meta.alwaysShow", column = "always_show"),
        @Result(property = "meta.metaTitle", column = "meta_title"),
        @Result(property = "meta.metaIcon", column = "meta_icon"),
        @Result(property = "meta.metaHidden", column = "meta_hidden"),
        @Result(property = "meta.metaRoles", column = "meta_roles"),
        @Result(property = "meta.metaKeepAlive", column = "meta_keep_alive"),
        @Result(property = "meta.hidden", column = "hidden")
    })
    List<Router> listAll();

    @Select("SELECT id, pid, menu_order, status, remark, permission, path, name, icon, " +
           "type, component, redirect, always_show, meta_title, meta_icon, meta_hidden, " +
           "meta_roles, meta_keep_alive, hidden FROM router WHERE id = #{id}")
    @ResultMap("routerResultMap")
    Router getById(Long id);

    @Update("<script>UPDATE router SET " +
            "<if test='pid != null'>pid = #{pid},</if> " +
            "menu_order = #{menuOrder}, " +
            "status = #{status}, " +
            "<if test='remark != null'>remark = #{remark},</if> " +
            "<if test='permission != null'>permission = #{permission},</if> " +
            "path = #{path}, " +
            "name = #{name}, " +
            "<if test='icon != null'>icon = #{icon},</if> " +
            "type = #{meta.type}, " +
            "<if test='meta.component != null'>component = #{meta.component},</if> " +
            "<if test='meta.redirect != null'>redirect = #{meta.redirect},</if> " +
            "always_show = #{meta.alwaysShow}, " +
            "meta_title = #{meta.metaTitle}, " +
            "<if test='meta.metaIcon != null'>meta_icon = #{meta.metaIcon},</if> " +
            "<if test='meta.metaHidden != null'>meta_hidden = #{meta.metaHidden},</if> " +
            "meta_roles = <choose>" +
            "<when test='meta.metaRoles != null'>#{meta.metaRoles}</when>" +
            "<otherwise>JSON_ARRAY()</otherwise>" +
            "</choose>, " +
            "<if test='meta.metaKeepAlive != null'>meta_keep_alive = #{meta.metaKeepAlive},</if> " +
            "hidden = #{meta.hidden} " +
            "WHERE id = #{id}</script>")
    void update(Router router);

    @Insert("<script>INSERT INTO router (" +
            "<if test='pid != null'>pid,</if> " +
            "menu_order, status, " +
            "<if test='remark != null'>remark,</if> " +
            "<if test='permission != null'>permission,</if> " +
            "path, name, " +
            "<if test='icon != null'>icon,</if> " +
            "type, " +
            "<if test='meta.component != null'>component,</if> " +
            "<if test='meta.redirect != null'>redirect,</if> " +
            "always_show, meta_title, " +
            "<if test='meta.metaIcon != null'>meta_icon,</if> " +
            "<if test='meta.metaHidden != null'>meta_hidden,</if> " +
            "meta_roles, " +
            "<if test='meta.metaKeepAlive != null'>meta_keep_alive,</if> " +
            "hidden" +
            ") VALUES (" +
            "<if test='pid != null'>#{pid},</if> " +
            "#{menuOrder}, #{status}, " +
            "<if test='remark != null'>#{remark},</if> " +
            "<if test='permission != null'>#{permission},</if> " +
            "#{path}, #{name}, " +
            "<if test='icon != null'>#{icon},</if> " +
            "#{meta.type}, " +
            "<if test='meta.component != null'>#{meta.component},</if> " +
            "<if test='meta.redirect != null'>#{meta.redirect},</if> " +
            "#{meta.alwaysShow}, #{meta.metaTitle}, " +
            "<if test='meta.metaIcon != null'>#{meta.metaIcon},</if> " +
            "<if test='meta.metaHidden != null'>#{meta.metaHidden},</if> " +
            "<choose>" +
            "<when test='meta.metaRoles != null'>#{meta.metaRoles}</when>" +
            "<otherwise>JSON_ARRAY()</otherwise>" +
            "</choose>, " +
            "<if test='meta.metaKeepAlive != null'>#{meta.metaKeepAlive},</if> " +
            "#{meta.hidden}" +
            ")</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(Router router);

    @Select("<script>SELECT id, pid, menu_order, status, remark, permission, path, name, icon, " +
            "type, component, redirect, always_show, meta_title, meta_icon, meta_hidden, " +
            "meta_roles, meta_keep_alive, hidden FROM router " +
            "<where>" +
            "router.id != 0" +
            "<if test='permissions != null and permissions.size() > 0'>" +
            " AND permission IN " +
            "<foreach collection='permissions' item='permission' separator=',' open='(' close=')'>" +
            "#{permission}" +
            "</foreach>" +
            "</if>" +
            "<if test='permissions == null or permissions.size() == 0'>" +
            " AND 1 = 0" +
            "</if>" +
            "</where>" +
            "</script>")
    @ResultMap("routerResultMap")
    List<Router> listByPermissions(@Param("permissions") List<String> permissions);

    @Delete("DELETE FROM router WHERE id = #{id}")
    void deleteById(Long id);

    /**
     * 根据路由ID列表获取权限标识
     * @param routerIds 路由ID列表
     * @return 权限标识列表
     */
    @Select("<script>SELECT permission FROM router WHERE id IN " +
           "<foreach collection='routerIds' item='id' separator=',' open='(' close=')'>" +
           "#{id}" +
           "</foreach>" +
           "</script>")
    List<String> listPermissionsByRouterIds(@Param("routerIds") List<Long> routerIds);

    @Select("SELECT permission FROM router WHERE id = #{id}")
    String getPermissionById(@Param("id") Long id);
}
