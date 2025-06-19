package com.david.hlp.web.system.auth.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.david.hlp.web.common.entity.Result;
import com.david.hlp.web.common.enums.ResultCode;
import com.david.hlp.web.system.auth.entity.role.Role;
import com.david.hlp.web.system.auth.service.imp.RoleServiceImp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 角色管理控制器
 *
 * @author david
 */
@Slf4j
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleServiceImp roleService;

    /**
     * 获取角色列表
     *
     * @param roleName 角色名称（可选）
     * @return 角色列表结果
     */
    @GetMapping("/getRoleList")
    public Result<List<Role>> getRoleList(@RequestParam(required = false)String roleName) {
        try {
            return Result.success(roleService.getRoleList(roleName));
        } catch (Exception e) {
            log.error("获取角色列表异常: roleName={}, 错误={}", roleName, e.getMessage(), e);
            return Result.error(ResultCode.INTERNAL_ERROR, "获取角色列表失败: " + e.getMessage());
        }
    }
}
