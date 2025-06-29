import request from '@/utils/request/request'
import type { Resume } from './resume.d'

/**
 * 获取当前用户的所有简历
 */
export const getResumes = (): Promise<Resume[]> => {
  return request({
    url: '/api/resume',
    method: 'get'
  })
}

/**
 * 根据ID获取简历详情
 * @param id 简历ID
 */
export const getResumeById = (id: string): Promise<Resume> => {
  return request({
    url: `/api/resume/${id}`,
    method: 'get'
  })
}

/**
 * 创建新简历
 * @param data 简历数据
 */
export const createResume = (data: Partial<Resume>): Promise<Resume> => {
  return request({
    url: '/api/resume',
    method: 'post',
    data
  })
}

/**
 * 更新简历
 * @param id 简历ID
 * @param data 简历数据
 */
export const updateResume = (id: string, data: Partial<Resume>): Promise<Resume> => {
  return request({
    url: `/api/resume/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除简历
 * @param id 简历ID
 */
export const deleteResume = (id: string): Promise<void> => {
  return request({
    url: `/api/resume/${id}`,
    method: 'delete'
  })
}
