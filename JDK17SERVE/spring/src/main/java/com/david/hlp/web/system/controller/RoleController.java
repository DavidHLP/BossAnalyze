package com.david.hlp.web.system.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.david.hlp.web.common.result.Result;
import com.david.hlp.web.system.entity.role.Role;
import com.david.hlp.web.system.service.imp.RoleServiceImp;

import lombok.RequiredArgsConstructor;

/**
 * 角色管理控制器
 *
 * @author david
 */
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
        return Result.success(roleService.getRoleList(roleName));
    }
}
