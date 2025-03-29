import request from '@/utils/request/request'
import type { Role } from '@/api/auth/auth.d'

export const getRoleList = (roleName?: string): Promise<Role[]> =>
  request({
    url: `/api/role/getRoleList?roleName=${roleName}`,
    method: 'GET',
  })
