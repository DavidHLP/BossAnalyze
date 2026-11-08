import axios from 'axios'
import { loginLocation } from '../../lib/session.ts'
import type { Request } from '@/utils/request/request.d'

// 创建一个 axios 实例
const service = axios.create({
  baseURL: '/',
  timeout: 5000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // Authentication uses browser storage; server-side callers must use a server-aware client.
    if (typeof window === 'undefined') {
      return Promise.reject(new Error('The API client requires a browser session'))
    }

    // 排除无需鉴权的接口（登录、注册、发送注册验证码、注销等）
    const publicPaths = [
      '/api/auth/login',
      '/api/auth/register',
      '/api/auth/sendRegisterEmail',
      '/api/auth/logout',
    ];
    const isPublicPath = publicPaths.some(path => config.url?.endsWith(path));

    const token = localStorage.getItem('token');

    // 如果不是公开路径且没有token，直接拒绝请求
    if (!isPublicPath && !token) {
      const target = loginLocation(`${window.location.pathname ?? ''}${window.location.search ?? ''}${window.location.hash ?? ''}`);
      window.location.href = `/login?${new URLSearchParams(target.query)}`;
      return Promise.reject('No token available');
    }

    // 添加token到请求头
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    console.error(error)
    return Promise.reject(error)
  },
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    if (response.status === 200) {
      return response.data?.data ?? (response.data as Request<unknown>)
    } else {
      return Promise.reject({
        code: response.status,
        message: response.data?.message || '请求失败',
        data: response.data,
      })
    }
  },
  (error) => {
    const token = typeof window === 'undefined' ? null : localStorage.getItem('token')
    const isLogout = error.config?.url?.endsWith('/api/auth/logout')
    if (!isLogout && error.response?.status === 401 && token &&
        error.config?.headers?.Authorization === `Bearer ${token}`) {
      localStorage.removeItem('token')
      // 触发全局登出逻辑；注销请求自行处理失败，避免与登录页跳转竞争
      window.dispatchEvent(new CustomEvent('unauthorized'))
    }
    return Promise.reject({
      code: error.response?.status || 500,
      message: error.response?.data?.message || error.message,
      data: error.response?.data,
    })
  },
)
export default service
