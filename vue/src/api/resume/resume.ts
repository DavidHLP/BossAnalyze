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

// 保存简历数据
export const saveResumeData = (data: ResumeData): Promise<boolean> => {
  return request({
    url: '/api/resume',
    method: 'PUT',
    data
  })
}

// 添加新简历
export const addResume = (data: ResumeData): Promise<ResumeData> => {
  return request({
    url: '/api/resume',
    method: 'POST',
    data
  })
}

// 删除简历
export const deleteResume = (id: string): Promise<boolean> => {
  return request({
    url: '/api/resume',
    method: 'DELETE',
    params: {
      id
    }
  })
}
