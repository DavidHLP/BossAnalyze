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
export const commitChanges = (
  resumeId: string,
  message: string,
  content: string,
): Promise<string> => {
  return request.post(`/api/resume/${resumeId}/version/commit`, { message, content })
}

/**
 * 创建分支
 */
export const createBranch = (resumeId: string, branchName: string): Promise<void> => {
  return request.post(`/api/resume/${resumeId}/version/branch`, { branchName })
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
 * 重置到指定提交（Hard Reset）
 */
export const resetToCommit = (resumeId: string, commitId: string): Promise<void> => {
  return request({
    url: `/api/resume/${resumeId}/version/reset`,
    method: 'POST',
    data: { commitId }
  })
}

/**
 * 回滚指定提交（创建新提交撤销更改）
 */
export const revertCommit = (resumeId: string, commitId: string): Promise<string> => {
  return request({
    url: `/api/resume/${resumeId}/version/revert`,
    method: 'POST',
    data: { commitId }
  })
}

/**
 * 获取最近的提交记录（用于快速回溯）
 */
export const getRecentCommits = (resumeId: string, limit = 10): Promise<ResumeCommit[]> => {
  return request({
    url: `/api/resume/${resumeId}/version/recent`,
    method: 'GET',
    params: { limit }
  })
}

/**
 * 获取两个提交之间的差异
 */
export const getCommitsBetween = (resumeId: string, fromCommit: string, toCommit: string): Promise<ResumeCommit[]> => {
  return request({
    url: `/api/resume/${resumeId}/version/diff`,
    method: 'GET',
    params: { fromCommit, toCommit }
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

/**
 * 调试接口：获取指定提交的详细信息
 */
export const getCommitDebugInfo = (resumeId: string, commitId: string): Promise<any> => {
  return request({
    url: `/api/resume/${resumeId}/version/debug/commit/${commitId}`,
    method: 'GET'
  })
}

/**
 * 调试接口：验证版本控制数据完整性
 */
export const validateVersionControl = (resumeId: string): Promise<any> => {
  return request({
    url: `/api/resume/${resumeId}/version/debug/validate`,
    method: 'GET'
  })
}

/**
 * 修复重复的提交ID问题
 */
export const fixDuplicateCommitIds = (resumeId: string): Promise<any> => {
  return request.post(`/api/resume/${resumeId}/version/fix-duplicate-ids`)
}
