import type { Router } from '@/router/index.d'
export interface Token {
  expired?: boolean
  id?: number
  revoked?: boolean
  token?: string
  tokenType?: string
  userId?: number
  [property: string]
}

export interface Permissions {
  [index: number]: string
}
export interface Role {
  createTime?: string
  id?: number
  remark?: string
  roleName?: string
  status?: number
  updateTime?: string
  userId?: number
  permissions?: Permissions[]
  routers?: Router[]
  [property: string]
}

export interface UserBaseInfo {
  address?: string
  avatar?: string
  createTime?: string
  email?: string
  id?: number
  introduction?: string
  lastLogin?: string
  lastLoginIp?: string
  name?: string
  roleId?: number
  status?: number | string
  userId?: number
  username?: string
  roleName?: string
  [property: string]
}
