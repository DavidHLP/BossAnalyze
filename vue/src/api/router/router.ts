import request from '@/utils/request/request'
import type { Router } from '@/router/index.d'
export const getUserRoutes = (): Promise<Router[]> =>
  request({
    url: '/api/auth/getRouters',
    method: 'GET',
  })

export const editRouter = (data: Router): Promise<void> =>
  request({
    url: '/api/auth/editRouter',
    method: 'POST',
    data,
  })

export const addRouter = (data: Router): Promise<void> =>
  request({
    url: '/api/auth/addRouter',
    method: 'POST',
    data,
  })

export const deleteRouter = (data: Router): Promise<void> =>
  request({
    url: '/api/auth/deleteRouter',
    method: 'POST',
    data,
  })
