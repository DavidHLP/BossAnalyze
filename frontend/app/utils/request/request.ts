import axios from 'axios'
import { loginLocation, ownsSession, requestSessionOwner } from '../../lib/session.ts'
import type { SessionOwner } from '../../lib/session.ts'
import type { Request, RequestFailure } from '@/utils/request/request.d'

const owners = new WeakMap<object, SessionOwner>()

export function requestFailure(status: number | undefined, data: unknown, message?: string): RequestFailure {
  const body = data && typeof data === 'object' && 'message' in data ? data.message : undefined
  const businessRejection = data && typeof data === 'object' && 'code' in data &&
    typeof data.code === 'number' && data.code !== 200
  const empty = data == null || (typeof data === 'string' && !data.trim())
  const resultUnknown = status === undefined || (!businessRejection && (
    status === 408 || status === 502 || status === 504 ||
    (empty && (status < 400 || status >= 500))
  ))
  return {
    code: status ?? 500,
    message: (typeof body === 'string' ? body : message) || '请求失败',
    data,
    resultUnknown,
  }
}

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
      throw new Error('The API client requires a browser session')
    }

    // 排除无需鉴权的接口（登录、注册、发送注册验证码、注销等）
    const publicPaths = [
      '/api/auth/login',
      '/api/auth/register',
      '/api/auth/sendRegisterEmail',
      '/api/auth/logout',
    ];
    const isPublicPath = publicPaths.some(path => config.url?.endsWith(path));

    const owner = requestSessionOwner()
    const token = owner.token
    owners.set(config, owner)

    // 如果不是公开路径且没有token，直接拒绝请求
    if (!isPublicPath && !token) {
      const target = loginLocation(`${window.location.pathname ?? ''}${window.location.search ?? ''}${window.location.hash ?? ''}`);
      window.location.href = `/login?${new URLSearchParams(target.query)}`;
      throw new Error('No token available')
    }

    // 添加token到请求头
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => { throw error },
  { synchronous: true },
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    if (response.status === 200 && !requestFailure(200, response.data).resultUnknown) {
      return response.data?.data ?? (response.data as Request<unknown>)
    }
    return Promise.reject(requestFailure(response.status, response.data))
  },
  (error) => {
    const isLogout = error.config?.url?.endsWith('/api/auth/logout')
    const owner = error.config && owners.get(error.config)
    if (!isLogout && error.response?.status === 401 && owner?.token &&
        error.config.headers?.Authorization === `Bearer ${owner.token}` && ownsSession(owner)) {
      localStorage.removeItem('token')
      // 触发全局登出逻辑；注销请求自行处理失败，避免与登录页跳转竞争
      window.dispatchEvent(new CustomEvent('unauthorized'))
    }
    return Promise.reject(requestFailure(error.response?.status, error.response?.data, error.message))
  },
)
export default service
