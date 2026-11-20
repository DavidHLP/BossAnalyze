import request from '@/utils/request/request'
import type { Router } from '@/router/index.d'
import type { MenuRecord, menuPayload } from '@/lib/menus'

type MenuWrite = ReturnType<typeof menuPayload>
type MenuDeletion = Pick<MenuRecord, 'id' | 'name' | 'permission'> & { children: [] }

export const getUserRoutes = (): Promise<Router[]> =>
  request({
    url: '/api/auth/getRouters',
    method: 'GET',
  })

export const editRouter = (data: MenuWrite): Promise<unknown> =>
  request({
    url: '/api/auth/editRouter',
    method: 'POST',
    data,
  })

export const addRouter = (data: MenuWrite): Promise<unknown> =>
  request({
    url: '/api/auth/addRouter',
    method: 'POST',
    data,
  })

export const deleteRouter = (data: MenuDeletion): Promise<unknown> =>
  request({
    url: '/api/auth/deleteRouter',
    method: 'POST',
    data,
  })
