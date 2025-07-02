import request from '@/utils/request/request'
import type { Resume } from './types'

export interface ResumeCommit {
  id: string
  resumeId: string
  userId: number
  message: string
  content: string
  branch: string
  parentCommits: string[]
  commitType: string
  author?: string
  authorEmail?: string
  commitTime: Date
  treeHash: string
  metadata?: Record<string, unknown>
}

/**
 * 初始化版本控制
 */
export const initVersionControl = (resumeId: string, content: string = ''): Promise<Resume> => {
  return request({
    url: `/api/resume/${resumeId}/version/init`,
    method: 'POST',
    data: { content }
  })
}

/**
 * 提交更改
 */
export const commitChanges = (resumeId: string, message: string, content: string): Promise<string> => {
  return request({
    url: `/api/resume/${resumeId}/version/commit`,
    method: 'POST',
    data: { message, content }
  })
}

/**
 * 创建分支
 */
export const createBranch = (resumeId: string, name: string): Promise<void> => {
  return request({
    url: `/api/resume/${resumeId}/version/branch`,
    method: 'POST',
    data: { name }
  })
}

/**
 * 切换分支
 */
export const switchBranch = (resumeId: string, branch: string): Promise<void> => {
  return request({
    url: `/api/resume/${resumeId}/version/switch`,
    method: 'POST',
    data: { branch }
  })
}

/**
 * 合并分支
 */
export const mergeBranch = (resumeId: string, source: string, target: string): Promise<string> => {
  return request({
    url: `/api/resume/${resumeId}/version/merge`,
    method: 'POST',
    data: { source, target }
  })
}

/**
 * 获取提交历史
 */
export const getCommitHistory = (resumeId: string): Promise<ResumeCommit[]> => {
  return request({
    url: `/api/resume/${resumeId}/version/history`,
    method: 'GET'
  })
}

/**
 * 检出到指定提交
 */
export const checkoutCommit = (resumeId: string, commitId: string): Promise<void> => {
  return request({
    url: `/api/resume/${resumeId}/version/checkout`,
    method: 'POST',
    data: { commitId }
  })
}

/**
 * 储藏当前更改
 */
export const stashChanges = (resumeId: string, content: string): Promise<void> => {
  return request({
    url: `/api/resume/${resumeId}/version/stash`,
    method: 'POST',
    data: { content }
  })
}

/**
 * 弹出储藏
 */
export const stashPop = (resumeId: string): Promise<string> => {
  return request({
    url: `/api/resume/${resumeId}/version/stash/pop`,
    method: 'POST'
  })
}
