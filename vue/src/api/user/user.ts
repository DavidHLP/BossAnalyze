import request from '@/utils/request/request'
import type { PageInfo } from '@/utils/types/common'
import type { UserBaseInfo as User } from '@/api/auth/auth.d'
export const getUserManageInfo = (
  pageNum: number,
  pageSize: number,
  params?: User,
): Promise<PageInfo<User>> =>
  request({
    url: `/api/user/getUserManageInfo`,
    method: 'POST',
    data: {
      pageNum,
      pageSize,
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
