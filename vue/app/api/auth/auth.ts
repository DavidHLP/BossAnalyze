import request from '@/utils/request/request'
import type { Token, Permissions, Role, UserBaseInfo } from '@/api/auth/auth.d'

export const login = (data: { email: string; password: string }): Promise<Token> =>
  request({
    url: '/api/auth/login',
    method: 'post',
    data: { ...data },
  })

export const getUserPrivateInformation = (): Promise<Array<Permissions>> =>
  request({
    url: '/api/auth/getUserPrivateInformation',
    method: 'GET',
  })

export const getUserRole = (): Promise<Role> =>
  request({
    url: '/api/auth/getUserRole',
    method: 'GET',
  })

export const getUserBaseInfo = (): Promise<UserBaseInfo> =>
  request({
    url: '/api/auth/getUserBaseInfo',
    method: 'GET',
  })

export const getRoleList = (): Promise<Role[]> =>
  request({
    url: '/api/auth/getRoleList',
    method: 'GET',
  })

// 添加角色
export const addRole = (data: Role): Promise<void> =>
  request({
    url: '/api/auth/addRole',
    method: 'POST',
    data,
  })

// 编辑角色
export const editRole = (data: Role): Promise<void> =>
  request({
    url: '/api/auth/editRole',
    method: 'POST',
    data,
  })

// 删除角色
export const deleteRole = (roleId: number): Promise<void> =>
  request({
    url: '/api/auth/deleteRole',
    method: 'POST',
    data: { id: roleId },
  })

// 更新角色权限
export const updateRolePermissions = (data: {
  roleId: number
  permissionIds: number[]
}): Promise<void> =>
  request({
    url: '/api/auth/updateRolePermissions',
    method: 'POST',
    data,
  })

// 更新角色路由权限
export const updateRoleRouters = (data: { roleId: number; routerIds: number[] }): Promise<void> =>
  request({
    url: '/api/auth/updateRoleRouters',
    method: 'POST',
    data,
  })

// 更新用户角色
export const updateUserRole = (data: { userId: number; roleId: number }): Promise<void> =>
  request({
    url: '/api/auth/updateUserRole',
    method: 'POST',
    data,
  })

export const register = (data: {
  name: string
  email: string
  password: string
  code: string
}): Promise<void> =>
  request({
    url: '/api/auth/register',
    method: 'post',
    data: { ...data },
  })

export const sendRegisterEmail = (data: { email: string }): Promise<void> =>
  request({
    url: '/api/auth/sendRegisterEmail',
    method: 'post',
    data: { email: data.email },
  })
