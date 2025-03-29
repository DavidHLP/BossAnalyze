import axios from 'axios'
import type { Request } from '@/utils/types/auth'

// 创建一个 axios 实例
const service = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 5000,
  withCredentials: true,
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 排除登录和注册接口
    const publicPaths = ['/api/auth/demo/login', '/api/auth/demo/register'];
    const isPublicPath = publicPaths.some(path => config.url?.endsWith(path));

    const token = localStorage.getItem('token');

    // 如果不是公开路径且没有token，直接拒绝请求
    if (!isPublicPath && !token) {
      window.location.href = '/login';
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
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      // 触发全局登出逻辑
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
