import request from '@/utils/request/request'
import type { Router } from '@/router/index.d'
export const getUserRoutes = (): Promise<Router[]> =>
  request({
    url: '/api/auth/demo/getRouters',
    method: 'GET',
  })

export const editRouter = (data: Router): Promise<void> =>
  request({
    url: '/api/auth/demo/editRouter',
    method: 'POST',
    data,
  })

export const addRouter = (data: Router): Promise<void> =>
  request({
    url: '/api/auth/demo/addRouter',
    method: 'POST',
    data,
  })

export const deleteRouter = (data: Router): Promise<void> =>
  request({
    url: '/api/auth/demo/deleteRouter',
    method: 'POST',
    data,
  })
