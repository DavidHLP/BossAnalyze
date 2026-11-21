import request from '@/utils/request/request'
import type { PageInfo } from '@/types/common'
import type { UserBaseInfo as User } from '@/api/auth/auth.d'

export const getUserManageInfo = (
  number: number,
  size: number,
  params?: User,
): Promise<PageInfo<User>> =>
  request({
    url: `/api/user/getUserManageInfo`,
    method: 'POST',
    data: {
      number,
      size,
      query: params,
    },
  })

export const deleteUser = (id: number, password: string) =>
  request({
    url: `/api/user/deleteUser`,
    method: 'POST',
    data: { id, password },
  })

export const updateUser = (data: User) =>
  request({
    url: `/api/user/updateUser`,
    method: 'POST',
    data,
  })

export const addUser = (data: User) =>
  request({
    url: `/api/user/addUser`,
    method: 'POST',
    data,
  })
