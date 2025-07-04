package com.david.hlp.web.system.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import com.david.hlp.web.common.controller.BaseController;
import com.david.hlp.web.common.entity.Result;
import com.david.hlp.web.common.enums.RedisKeyEnum;
import com.david.hlp.web.common.enums.ResultCode;
import com.david.hlp.web.common.exception.BusinessException;
import com.david.hlp.web.common.service.EmailService;
import com.david.hlp.web.utils.CodeUtil;
import com.david.hlp.web.system.auth.entity.auth.LoginDTO;
import com.david.hlp.web.system.auth.entity.auth.RegistrationDTO;
import com.david.hlp.web.system.auth.entity.role.Role;
import com.david.hlp.web.system.auth.entity.role.RolePermissionUpdateResponse;
import com.david.hlp.web.system.auth.entity.router.Router;
import com.david.hlp.web.system.auth.entity.user.User;
import com.david.hlp.web.system.auth.service.PermissionService;
import com.david.hlp.web.system.auth.service.imp.AuthServiceImp;
import com.david.hlp.web.system.auth.service.imp.RoleServiceImp;
import com.david.hlp.web.system.auth.service.imp.RouterServiceImp;
import com.david.hlp.web.system.auth.service.imp.UserServiceImp;
import com.david.hlp.web.system.auth.token.Token;
import com.david.hlp.commons.utils.RedisCacheHelper;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import java.util.Objects;

/**
 * 认证控制器
 * 处理用户认证、权限和角色管理相关的请求
 *
 * @author david
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthServiceImp authService;
    private final RouterServiceImp routerService;
    private final PermissionService permissionService;
    private final RoleServiceImp roleService;
    private final UserServiceImp userService;
    private final EmailService emailService;
    private final RedisCacheHelper redisCacheHelper;

    /**
     * 发送注册验证码邮件
     *
     * @param request 注册请求信息
     * @return 发送结果
     */
    @PostMapping("/sendRegisterEmail")
    public Result<Void> registerSendEmail(@RequestBody final RegistrationDTO request) {
        if (userService.getByEmail(request.getEmail()) != null) {
            return Result.error(ResultCode.USER_EXISTS);
        }
        try {
            redisCacheHelper.executeWithLock(request.getEmail(), () -> {
                String code = CodeUtil.generateVerificationCode();
                emailService.sendSimpleMail(request.getEmail(), "注册验证码", code);
                redisCacheHelper.setString(
                        RedisKeyEnum.REGISTER_CODE_KEY.getKey() + request.getEmail(),
                        code,
                        RedisKeyEnum.REGISTER_CODE_KEY.getTimeout(),
                        RedisKeyEnum.REGISTER_CODE_KEY.getTimeUnit());
            });
            return Result.success("验证码已发送");
        } catch (Exception e) {
            log.error("注册失败: 操作频繁，请稍后再试");
            throw new BusinessException(ResultCode.LOCK_HAS_USED);
        }
    }

    /**
     * 用户注册
     *
     * @param request 注册请求信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<String> registerUser(@RequestBody final RegistrationDTO request) {
        try {
            Objects.requireNonNull(request, "注册请求不能为空");
            authService.registerUser(request);
            return Result.success("注册成功");
        } catch (final DuplicateKeyException e) {
            log.warn("用户注册失败: 用户已存在, email={}", request.getEmail());
            throw new BusinessException(ResultCode.USER_EXISTS);
        } catch (final Exception e) {
            log.error("用户注册异常: {}", e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "注册失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录
     *
     * @param request 登录请求信息
     * @return 登录令牌
     */
    @PostMapping("/login")
    public Result<Token> login(@RequestBody final LoginDTO request) {
        Objects.requireNonNull(request, "登录请求不能为空");
        if (Objects.isNull(request.getEmail()) || Objects.isNull(request.getPassword())) {
            log.warn("登录失败: 请求参数不完整, email={}", request.getEmail());
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        try {
            final Token token = authService.login(request);
            redisCacheHelper.setObject(
                    RedisKeyEnum.TOKEN_REFRESH_KEY.getKey() + token.getAuthUser().getUserId(),
                    token,
                    Token.class,
                    RedisKeyEnum.TOKEN_REFRESH_KEY.getTimeout(),
                    RedisKeyEnum.TOKEN_REFRESH_KEY.getTimeUnit());
            return Result.success(token);
        } catch (final Exception e) {
            log.error("用户登录异常: email={}, 错误={}", request.getEmail(), e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "登录失败: " + e.getMessage());
        }
    }

    /**
     * 获取路由信息
     *
     * @return 路由列表
     */
    @GetMapping("/getRouters")
    public Result<List<Router>> getRouters() {
        final List<Router> routers = routerService.getRouters();
        return Result.success(routers);
    }

    /**
     * 获取用户私有信息
     *
     * @return 用户权限列表
     */
    @GetMapping("/getUserPrivateInformation")
    public Result<List<String>> getUserPrivateInformation() {
        final List<String> permissions = permissionService.getUserPermissions(getCurrentUserId());
        return Result.success(permissions);
    }

    /**
     * 获取用户角色信息
     *
     * @return 用户角色
     */
    @GetMapping("/getUserRole")
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
    @GetMapping("/getUserBaseInfo")
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
    @PostMapping("/editRouter")
    public Result<Void> editRouter(@RequestBody final Router router) {
        try {
            Objects.requireNonNull(router, "路由信息不能为空");
            routerService.editRouter(router);
            return Result.success("编辑成功");
        } catch (Exception e) {
            log.error("编辑路由异常: routerId={}, 错误={}", router != null ? router.getId() : "null", e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "编辑失败: " + e.getMessage());
        }
    }

    /**
     * 添加路由
     *
     * @param router 路由信息
     * @return 操作结果
     */
    @PostMapping("/addRouter")
    public Result<Void> addRouter(@RequestBody final Router router) {
        try {
            Objects.requireNonNull(router, "路由信息不能为空");
            routerService.addRouter(router);
            return Result.success("添加成功");
        } catch (Exception e) {
            log.error("添加路由异常: 错误={}", e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "添加失败: " + e.getMessage());
        }
    }

    /**
     * 删除路由
     *
     * @param router 路由信息
     * @return 操作结果
     */
    @PostMapping("/deleteRouter")
    public Result<Void> deleteRouter(@RequestBody final Router router) {
        try {
            Objects.requireNonNull(router, "路由信息不能为空");
            routerService.deleteRouter(router);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除路由异常: routerId={}, 错误={}", router != null ? router.getId() : "null", e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取角色列表
     *
     * @return 角色列表
     */
    @GetMapping("/getRoleList")
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
    @PostMapping("/addRole")
    public Result<Void> addRole(@RequestBody final Role role) {
        try {
            Objects.requireNonNull(role, "角色信息不能为空");
            roleService.addRole(role);
            return Result.success("添加成功");
        } catch (Exception e) {
            log.error("添加角色异常: 错误={}", e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "添加失败: " + e.getMessage());
        }
    }

    /**
     * 编辑角色
     *
     * @param role 角色信息
     * @return 操作结果
     */
    @PostMapping("/editRole")
    public Result<Void> editRole(@RequestBody final Role role) {
        try {
            Objects.requireNonNull(role, "角色信息不能为空");
            roleService.editRole(role);
            return Result.success("编辑成功");
        } catch (Exception e) {
            log.error("编辑角色异常: roleId={}, 错误={}", role != null ? role.getId() : "null", e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "编辑失败: " + e.getMessage());
        }
    }

    /**
     * 更新角色权限
     *
     * @param rolePermissionUpdateResponse 角色权限更新信息
     * @return 操作结果
     */
    @PostMapping("/updateRoleRouters")
    public Result<Void> updateRolePermissions(
            @RequestBody final RolePermissionUpdateResponse rolePermissionUpdateResponse) {
        try {
            Objects.requireNonNull(rolePermissionUpdateResponse, "角色权限更新信息不能为空");
            System.out.println("rolePermissionUpdateResponse:" + rolePermissionUpdateResponse);
            roleService.updateRolePermissions(rolePermissionUpdateResponse);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新角色权限异常: roleId={}, 错误={}",
                    rolePermissionUpdateResponse != null ? rolePermissionUpdateResponse.getRoleId() : "null",
                    e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除角色
     *
     * @param role 角色信息
     * @return 操作结果
     */
    @PostMapping("/deleteRole")
    public Result<Void> deleteRole(@RequestBody final Role role) {
        try {
            Objects.requireNonNull(role, "角色信息不能为空");
            roleService.deleteRole(role.getId());
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除角色异常: roleId={}, 错误={}", role != null ? role.getId() : "null", e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "删除失败: " + e.getMessage());
        }
    }
}