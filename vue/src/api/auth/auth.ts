import request from '@/utils/request/request'
import type { Token, Permissions, Role, UserBaseInfo } from '@/api/auth/auth.d'

export const login = (data: { email: string; password: string }): Promise<Token> =>
  request({
    url: '/api/auth/demo/login',
    method: 'post',
    data: { ...data },
  })

export const getUserPrivateInformation = (): Promise<Array<Permissions>> =>
  request({
    url: '/api/auth/demo/getUserPrivateInformation',
    method: 'GET',
  })

export const getUserRole = (): Promise<Role> =>
  request({
    url: '/api/auth/demo/getUserRole',
    method: 'GET',
  })

export const getUserBaseInfo = (): Promise<UserBaseInfo> =>
  request({
    url: '/api/auth/demo/getUserBaseInfo',
    method: 'GET',
  })

export const getRoleList = (): Promise<Role[]> =>
  request({
    url: '/api/auth/demo/getRoleList',
    method: 'GET',
  })

// 添加角色
export const addRole = (data: Role): Promise<void> =>
  request({
    url: '/api/auth/demo/addRole',
    method: 'POST',
    data,
  })

// 编辑角色
export const editRole = (data: Role): Promise<void> =>
  request({
    url: '/api/auth/demo/editRole',
    method: 'POST',
    data,
  })

// 删除角色
export const deleteRole = (roleId: number): Promise<void> =>
  request({
    url: '/api/auth/demo/deleteRole',
    method: 'POST',
    data: { id: roleId },
  })

// 更新角色权限
export const updateRolePermissions = (data: { roleId: number; permissionIds: number[] }): Promise<void> =>
  request({
    url: '/api/auth/demo/updateRolePermissions',
    method: 'POST',
    data,
  })

// 更新角色路由权限
export const updateRoleRouters = (data: { roleId: number; routerIds: number[] }): Promise<void> =>
  request({
    url: '/api/auth/demo/updateRoleRouters',
    method: 'POST',
    data,
  })

// 更新用户角色
export const updateUserRole = (data: { userId: number; roleId: number }): Promise<void> =>
  request({
    url: '/api/auth/demo/updateUserRole',
    method: 'POST',
    data,
  })

export const register = (data: { name: string; email: string; password: string }): Promise<void> =>
  request({
    url: '/api/auth/demo/register',
    method: 'post',
    data: { ...data },
  })
