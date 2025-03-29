package com.david.hlp.Spring.system.mapper;

import com.david.hlp.Spring.system.entity.auth.AuthUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;
import com.david.hlp.Spring.system.entity.user.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT CONCAT('ROLE_', r.`role_name`) AS `authority` " +
            "FROM `role` r WHERE r.`id` = #{roleId} " +
            "UNION ALL " +
            "SELECT p.`permission` AS `authority` " +
            "FROM `permission` p " +
            "JOIN `role_permission` rp ON p.`id` = rp.`permission_id` " +
            "WHERE rp.`role_id` = #{roleId}")
    @Cacheable(value = "userAuthorities", key = "#roleId")
    List<String> listAuthoritiesByRoleId(Long roleId);

    @Select("SELECT `id`, `name`, `email`, `password`, `status`, `role_id` as roleId " +
            "FROM `user` WHERE `name` = #{username} AND `status` = 1")
    AuthUser getByUsername(String username);

    @Insert("INSERT INTO `user` (`name`, `email`, `password`, `status`, `role_id`, `avatar`, " +
            "`introduction`, `address`, `create_time`) " +
            "VALUES(#{name}, #{email}, #{password}, #{status}, #{roleId}, #{avatar}, " +
            "#{introduction}, #{address}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT `id` as user_id, `name` as username, `password`, `status`, `role_id`, `email` " +
            "FROM `user` WHERE `email` = #{email} AND `status`= 1")
    @Results({
        @Result(property = "userId", column = "user_id"),
        @Result(property = "username", column = "username"),
        @Result(property = "password", column = "password"),
        @Result(property = "status", column = "status"),
        @Result(property = "roleId", column = "role_id"),
        @Result(property = "email", column = "email")
    })
    AuthUser getByEmailToAuthUser(String email);

    @Select("SELECT `id`, `name`, `email`, `password`, `status`, `role_id` as roleId " +
            "FROM `user` WHERE `email` = #{email} AND `status`= 1")
    User getByEmailToUser(String email);

    @Select("SELECT `id`, `name`, `email`, `password`, `status`, `role_id` as roleId " +
            "FROM `user` WHERE `id` = #{userId} AND `status`= 1")
    User getByUserIdToUser(Long userId);

    @Select("<script>" +
            "SELECT u.`id`, u.`name`, u.`email`, u.`status`, u.`role_id`, u.`avatar`, " +
            "r.`role_name`, u.`address`, u.`introduction`, u.`last_login_ip`, " +
            "u.`last_login`, u.`create_time` " +
            "FROM `user` u " +
            "LEFT JOIN `role` r ON u.`role_id` = r.`id` " +
            "<where> " +
            "<if test='query.status != null'>AND u.`status` = #{query.status} </if> " +
            "<if test='query.roleId != null'>AND u.`role_id` = #{query.roleId} </if> " +
            "<if test='query.name != null'>AND u.`name` LIKE CONCAT('%', #{query.name}, '%') </if> " +
            "</where> " +
            "ORDER BY u.`create_time` DESC " +
            "LIMIT #{pageNum}, #{pageSize}" +
            "</script>")
    List<User> listByPage(@Param("pageNum") int pageNum, @Param("pageSize") int pageSize, @Param("query") User query);

    @Select("<script>" +
            "SELECT COUNT(1) " +
            "FROM `user` u " +
            "<where> " +
            "<if test='query.status != null'>AND u.`status` = #{query.status} </if> " +
            "<if test='query.roleId != null'>AND u.`role_id` = #{query.roleId} </if> " +
            "<if test='query.name != null'>AND u.`name` LIKE CONCAT('%', #{query.name}, '%') </if> " +
            "</where>" +
            "</script>")
    Long count(@Param("query") User query);

    @Delete("DELETE FROM `user` WHERE `id` = #{id}")
    void deleteById(@Param("id") Long id);

    @Select("SELECT `password` FROM `user` WHERE `id` = #{id}")
    String getPasswordById(@Param("id") Long id);

    @Update("<script>" +
            "UPDATE `user` " +
            "<set> " +
            "<if test='user.name != null'>`name` = #{user.name}, </if> " +
            "<if test='user.email != null'>`email` = #{user.email}, </if> " +
            "<if test='user.password != null'>`password` = #{user.password}, </if> " +
            "<if test='user.roleId != null'>`role_id` = #{user.roleId}, </if> " +
            "<if test='user.address != null'>`address` = #{user.address}, </if> " +
            "<if test='user.introduction != null'>`introduction` = #{user.introduction}, </if> " +
            "<if test='user.avatar != null'>`avatar` = #{user.avatar}, </if> " +
            "<if test='user.lastLoginIp != null'>`last_login_ip` = #{user.lastLoginIp}, </if> " +
            "<if test='user.lastLogin != null'>`last_login` = #{user.lastLogin}, </if> " +
            "</set> " +
            "WHERE `id` = #{user.id}" +
            "</script>")
    int updateById(@Param("user") User user);
}