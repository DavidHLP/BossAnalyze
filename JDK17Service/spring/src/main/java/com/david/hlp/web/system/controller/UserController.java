package com.david.hlp.web.system.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.david.hlp.web.common.controller.BaseController;
import com.david.hlp.web.common.enums.ResultCode;
import com.david.hlp.web.common.exception.ApiException;
import com.david.hlp.web.common.result.PageInfo;
import com.david.hlp.web.common.result.Result;
import com.david.hlp.web.system.entity.user.DelUser;
import com.david.hlp.web.system.entity.user.User;
import com.david.hlp.web.system.service.PasswordService;
import com.david.hlp.web.system.service.UserServiceImp;
import com.david.hlp.web.system.service.imp.AuthServiceImp;

/**
 * 用户管理控制器
 */
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
        return Result.success(userService.getUserManageInfo(
            pageInfo.getNumber(),
            pageInfo.getSize(),
            pageInfo.getQuery()
        ));
    }

    /**
     * 删除用户
     *
     * @param user 待删除的用户信息
     * @return 操作结果
     */
    @PostMapping("/deleteUser")
    public Result<Void> deleteUser(@RequestBody DelUser user) {
        if (!passwordService.matches(user.getPassword(), authService.getPassword(getCurrentUserId()))) {
            throw new ApiException(ResultCode.PASSWORD_ERROR);
        }
        userService.deleteUser(user);
        return Result.success("删除成功");
    }

    /**
     * 更新用户信息
     *
     * @param user 待更新的用户信息
     * @return 操作结果
     */
    @PostMapping("/updateUser")
    public Result<Void> updateUser(@RequestBody User user) {
        userService.updateUser(user);
        return Result.success("更新成功");
    }

    @PostMapping("/addUser")
    public Result<Void> addUser(@RequestBody User user) {
        userService.addUser(user);
        return Result.success("添加成功");
    }
}