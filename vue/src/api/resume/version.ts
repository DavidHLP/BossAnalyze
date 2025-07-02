import request from '@/utils/request/request'

// 简历版本数据类型
export interface ResumeVersion {
  id: string
  resumeId: string
  userId: number
  versionNumber: number
  title: string
  content: string
  changeDescription: string
  isAutoSave: boolean
  createdAt: string
  createdBy: string
}

// 获取简历版本历史
export const getVersionHistory = (resumeId: string): Promise<ResumeVersion[]> => {
  return request({
    url: `/api/resume/${resumeId}/versions`,
    method: 'get'
  })
}

// 获取特定版本内容
export const getVersion = (resumeId: string, versionNumber: number): Promise<ResumeVersion> => {
  return request({
    url: `/api/resume/${resumeId}/versions/${versionNumber}`,
    method: 'get'
  })
}

// 恢复到指定版本
export const restoreToVersion = (resumeId: string, versionNumber: number): Promise<any> => {
  return request({
    url: `/api/resume/${resumeId}/versions/${versionNumber}/restore`,
    method: 'post'
  })
}

// 手动创建版本快照
export const createVersionSnapshot = (
  resumeId: string,
  description?: string
): Promise<ResumeVersion> => {
  const params = new URLSearchParams()
  if (description) {
    params.append('description', description)
  }
  return request({
    url: `/api/resume/${resumeId}/versions?${params.toString()}`,
    method: 'post'
  })
}

// 清理旧版本
export const cleanupOldVersions = (
  resumeId: string,
  keepCount: number = 10
): Promise<void> => {
  return request({
    url: `/api/resume/${resumeId}/versions/cleanup?keepCount=${keepCount}`,
    method: 'post'
  })
}
