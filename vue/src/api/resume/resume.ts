import request from '@/utils/request/request'
import type { ResumeData } from './resume.d'
import type { PageInfo } from '@/types/common'
export const getResumeList = (params: { page: number; size: number}): Promise<PageInfo<ResumeData>> => {
  return request({
    url: '/api/resume/list',
    method: 'GET',
    params
  })
}

export const getResumeDetail = (id: string): Promise<ResumeData> => {
  return request({
    url: `/api/resume/get`,
    method: 'GET',
    params: {
      id
    }
  })
}

// 获取用户简历数据
export const getResumesByUserId = (userId: string): Promise<ResumeData[]> => {
  return request({
    url: '/api/resume/user',
    method: 'GET',
    params: {
      userId
    }
  })
}

// 获取最新一份简历数据 (用于创建新简历时使用的模板)
export const getResumeData = (): Promise<ResumeData> => {
  return request({
    url: '/api/resume/user/latest/one',
    method: 'GET',
    params: {
      userId: 'current' // 默认获取当前登录用户的最新简历
    }
  })
}

// 保存简历数据
export const saveResumeData = (data: ResumeData): Promise<boolean> => {
  return request({
    url: '/api/resume/update',
    method: 'PUT',
    data
  })
}

// 重置简历数据
export const resetResumeData = (): Promise<ResumeData> => {
  return request({
    url: '/api/resume/user/latest/one',
    method: 'GET',
    params: {
      userId: 'current'
    }
  })
}

// 添加新简历
export const addResume = (data: ResumeData): Promise<ResumeData> => {
  return request({
    url: '/api/resume/create',
    method: 'POST',
    data
  })
}

// 删除简历
export const deleteResume = (id: string): Promise<boolean> => {
  return request({
    url: '/api/resume/delete',
    method: 'DELETE',
    params: {
      id
    }
  })
}
