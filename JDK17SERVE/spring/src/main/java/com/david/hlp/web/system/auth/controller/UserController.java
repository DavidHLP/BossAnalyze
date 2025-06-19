package com.david.hlp.web.system.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.david.hlp.web.common.controller.BaseController;
import com.david.hlp.web.common.entity.PageInfo;
import com.david.hlp.web.common.entity.Result;
import com.david.hlp.web.common.enums.ResultCode;
import com.david.hlp.web.common.exception.ApiException;
import com.david.hlp.web.system.auth.entity.user.DelUser;
import com.david.hlp.web.system.auth.entity.user.User;
import com.david.hlp.web.system.auth.service.PasswordService;
import com.david.hlp.web.system.auth.service.imp.AuthServiceImp;
import com.david.hlp.web.system.auth.service.imp.UserServiceImp;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserServiceImp userService;
    private final PasswordService passwordService;
    private final AuthServiceImp authService;

    /**
     * 获取用户管理信息
     *
     * @param pageInfo 分页查询参数
     * @return 用户信息分页结果
     */
    @PostMapping("/getUserManageInfo")
    public Result<PageInfo<User>> getUserManageInfo(@RequestBody PageInfo<User> pageInfo) {
        try {
            return Result.success(userService.getUserManageInfo(
                pageInfo.getNumber(),
                pageInfo.getSize(),
                pageInfo.getQuery()
            ));
        } catch (Exception e) {
            log.error("获取用户管理信息异常: page={}, size={}, 错误={}", 
                pageInfo.getNumber(), pageInfo.getSize(), e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户
     *
     * @param user 待删除的用户信息
     * @return 操作结果
     */
    @PostMapping("/deleteUser")
    public Result<Void> deleteUser(@RequestBody DelUser user) {
        try {
            if (!passwordService.matches(user.getPassword(), authService.getPassword(getCurrentUserId()))) {
                log.warn("删除用户操作失败: 管理员密码错误, 尝试删除的用户ID={}", user.getId());
                throw new ApiException(ResultCode.PASSWORD_ERROR);
            }
            userService.deleteUser(user);
            return Result.success("删除成功");
        } catch (ApiException e) {
            throw e; // 已记录日志的异常直接抛出
        } catch (Exception e) {
            log.error("删除用户异常: userId={}, 错误={}", user.getId(), e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "删除用户失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     *
     * @param user 待更新的用户信息
     * @return 操作结果
     */
    @PostMapping("/updateUser")
    public Result<Void> updateUser(@RequestBody User user) {
        try {
            userService.updateUser(user);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新用户信息异常: userId={}, 错误={}", user.getId(), e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "更新用户信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/addUser")
    public Result<Void> addUser(@RequestBody User user) {
        try {
            userService.addUser(user);
            return Result.success("添加成功");
        } catch (Exception e) {
            log.error("添加用户异常: email={}, 错误={}", user.getEmail(), e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "添加用户失败: " + e.getMessage());
        }
    }
}