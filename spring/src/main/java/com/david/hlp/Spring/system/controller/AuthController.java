package com.david.hlp.Spring.system.controller;

import com.david.hlp.Spring.system.entity.auth.RegistrationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.david.hlp.Spring.system.entity.role.Role;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;

import com.david.hlp.Spring.common.enums.ResultCode;
import com.david.hlp.Spring.common.exception.BusinessException;
import com.david.hlp.Spring.common.result.Result;
import com.david.hlp.Spring.system.entity.auth.LoginDTO;
import com.david.hlp.Spring.system.token.Token;
import com.david.hlp.Spring.system.entity.router.Router;
import com.david.hlp.Spring.system.service.UserServiceImp;
import com.david.hlp.Spring.system.service.imp.AuthServiceImp;
import com.david.hlp.Spring.system.service.imp.RoleServiceImp;
import com.david.hlp.Spring.system.service.imp.RouterServiceImp;
import com.david.hlp.Spring.system.entity.user.User;
import com.david.hlp.Spring.system.entity.role.RolePermissionUpdateResponse;
import com.david.hlp.Spring.system.service.PermissionService;
import java.util.Objects;

/**
 * 认证控制器
 * 处理用户认证、权限和角色管理相关的请求
 *
 * @author david
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthServiceImp authService;
    private final RouterServiceImp routerService;
    private final PermissionService permissionService;
    private final RoleServiceImp roleService;
    private final UserServiceImp userService;

    /**
     * 用户注册
     *
     * @param request 注册请求信息
     * @return 注册结果
     */
    @PostMapping("/demo/register")
    public Result<String> registerUser(@RequestBody final RegistrationDTO request) {
        try {
            Objects.requireNonNull(request, "注册请求不能为空");
            authService.addUser(request);
            return Result.success("注册成功");
        } catch (final DuplicateKeyException e) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        } catch (final Exception e) {
            return Result.error(ResultCode.INTERNAL_ERROR, "注册失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录
     *
     * @param request 登录请求信息
     * @return 登录令牌
     */
    @PostMapping("/demo/login")
    public Result<Token> login(@RequestBody final LoginDTO request) {
        Objects.requireNonNull(request, "登录请求不能为空");
        if (Objects.isNull(request.getEmail()) || Objects.isNull(request.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        try {
            final Token token = authService.login(request);
            return Result.success(token);
        } catch (final Exception e) {
            return Result.error(ResultCode.INTERNAL_ERROR, "登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取路由信息
     *
     * @return 路由列表
     */
    @GetMapping("/demo/getRouters")
    public Result<List<Router>> getRouters() {
        final List<Router> routers = routerService.getRouters();
        return Result.success(routers);
    }

    /**
     * 获取用户私有信息
     *
     * @return 用户权限列表
     */
    @GetMapping("/demo/getUserPrivateInformation")
    public Result<List<String>> getUserPrivateInformation() {
        final List<String> permissions = permissionService.getUserPermissions(getCurrentUserId());
        return Result.success(permissions);
    }

    /**
     * 获取用户角色信息
     *
     * @return 用户角色
     */
    @GetMapping("/demo/getUserRole")
    public Result<Role> getUserRole() {
        final User user = userService.getUserBaseInfo(getCurrentUserId());
        final Role role = roleService.getRole(user.getRoleId());
        return Result.success(role);
    }

    /**
     * 获取用户基本信息
     *
     * @return 用户信息
     */
    @GetMapping("/demo/getUserBaseInfo")
    public Result<User> getUserBaseInfo() {
        final User user = userService.getUserBaseInfo(getCurrentUserId());
        return Result.success(user);
    }

    /**
     * 编辑路由
     *
     * @param router 路由信息
     * @return 操作结果
     */
    @PostMapping("/demo/editRouter")
    public Result<Void> editRouter(@RequestBody final Router router) {
        Objects.requireNonNull(router, "路由信息不能为空");
        routerService.editRouter(router);
        return Result.success("编辑成功");
    }

    /**
     * 添加路由
     *
     * @param router 路由信息
     * @return 操作结果
     */
    @PostMapping("/demo/addRouter")
    public Result<Void> addRouter(@RequestBody final Router router) {
        Objects.requireNonNull(router, "路由信息不能为空");
        routerService.addRouter(router);
        return Result.success("添加成功");
    }

    /**
     * 删除路由
     *
     * @param router 路由信息
     * @return 操作结果
     */
    @PostMapping("/demo/deleteRouter")
    public Result<Void> deleteRouter(@RequestBody final Router router) {
        Objects.requireNonNull(router, "路由信息不能为空");
        routerService.deleteRouter(router);
        return Result.success("删除成功");
    }

    /**
     * 获取角色列表
     *
     * @return 角色列表
     */
    @GetMapping("/demo/getRoleList")
    public Result<List<Role>> getRoleList() {
        final List<Role> roleList = roleService.getRoleList();
        return Result.success(roleList);
    }

    /**
     * 添加角色
     *
     * @param role 角色信息
     * @return 操作结果
     */
    @PostMapping("/demo/addRole")
    public Result<Void> addRole(@RequestBody final Role role) {
        Objects.requireNonNull(role, "角色信息不能为空");
        roleService.addRole(role);
        return Result.success("添加成功");
    }

    /**
     * 编辑角色
     *
     * @param role 角色信息
     * @return 操作结果
     */
    @PostMapping("/demo/editRole")
    public Result<Void> editRole(@RequestBody final Role role) {
        Objects.requireNonNull(role, "角色信息不能为空");
        roleService.editRole(role);
        return Result.success("编辑成功");
    }

    /**
     * 更新角色权限
     *
     * @param rolePermissionUpdateResponse 角色权限更新信息
     * @return 操作结果
     */
    @PostMapping("/demo/updateRoleRouters")
    public Result<Void> updateRolePermissions(@RequestBody final RolePermissionUpdateResponse rolePermissionUpdateResponse) {
        Objects.requireNonNull(rolePermissionUpdateResponse, "角色权限更新信息不能为空");
        roleService.updateRolePermissions(rolePermissionUpdateResponse);
        return Result.success("更新成功");
    }

    /**
     * 删除角色
     *
     * @param role 角色信息
     * @return 操作结果
     */
    @PostMapping("/demo/deleteRole")
    public Result<Void> deleteRole(@RequestBody final Role role) {
        Objects.requireNonNull(role, "角色信息不能为空");
        roleService.deleteRole(role.getId());
        return Result.success("删除成功");
    }
}