import request from '@/utils/request/request'
import type {
  Resume,
  Commit,
  Branch,
  Tag,
  CommitRequest,
  BranchRequest,
  MergeRequest,
  TagRequest,
  ResumeSnapshot // 兼容性别名
} from './types'

// Git风格版本管理相关 API

// ================== 提交管理 ==================

/**
 * 获取提交历史 (git log)
 */
export const getCommitHistory = (resumeId: string, branch?: string): Promise<Commit[]> => {
  return request({
    url: `/api/resume/${resumeId}/commits`,
    method: 'get',
    params: branch ? { branch } : undefined
  })
}

/**
 * 获取特定提交 (git show)
 */
export const getCommit = (resumeId: string, commitId: string): Promise<Commit> => {
  return request({
    url: `/api/resume/${resumeId}/commits/${commitId}`,
    method: 'get'
  })
}

/**
 * 手动提交变更 (git commit)
 */
export const commitChanges = (resumeId: string, commitData: CommitRequest): Promise<Commit> => {
  return request({
    url: `/api/resume/${resumeId}/commit`,
    method: 'post',
    data: commitData
  })
}

/**
 * 重置到指定提交 (git reset)
 */
export const resetToCommit = (resumeId: string, commitId: string): Promise<Resume> => {
  return request({
    url: `/api/resume/${resumeId}/reset/${commitId}`,
    method: 'post'
  })
}

// ================== 分支管理 ==================

/**
 * 获取所有分支 (git branch -a)
 */
export const getBranches = (resumeId: string): Promise<Branch[]> => {
  return request({
    url: `/api/resume/${resumeId}/branches`,
    method: 'get'
  })
}

/**
 * 创建新分支 (git branch)
 */
export const createBranch = (resumeId: string, branchData: BranchRequest): Promise<Branch> => {
  return request({
    url: `/api/resume/${resumeId}/branches`,
    method: 'post',
    data: branchData
  })
}

/**
 * 切换分支 (git checkout)
 */
export const checkoutBranch = (resumeId: string, branchName: string): Promise<Resume> => {
  return request({
    url: `/api/resume/${resumeId}/checkout/${branchName}`,
    method: 'post'
  })
}

/**
 * 合并分支 (git merge)
 */
export const mergeBranch = (resumeId: string, mergeData: MergeRequest): Promise<Resume> => {
  return request({
    url: `/api/resume/${resumeId}/merge`,
    method: 'post',
    data: mergeData
  })
}

/**
 * 删除分支 (git branch -d)
 */
export const deleteBranch = (resumeId: string, branchName: string): Promise<void> => {
  return request({
    url: `/api/resume/${resumeId}/branches/${branchName}`,
    method: 'delete',
  })
}

// ================== 标签管理 ==================

/**
 * 获取所有标签 (git tag -l)
 */
export const getTags = (resumeId: string): Promise<Tag[]> => {
  return request({
    url: `/api/resume/${resumeId}/tags`,
    method: 'get'
  })
}

/**
 * 创建标签 (git tag)
 */
export const createTag = (resumeId: string, tagData: TagRequest): Promise<Tag> => {
  return request({
    url: `/api/resume/${resumeId}/tags`,
    method: 'post',
    data: tagData
  })
}

// ================== 兼容性API（重定向到新API） ==================

// 为了保持向后兼容，导出别名
export type ResumeVersion = ResumeSnapshot

/**
 * 获取简历版本历史 (兼容性API，重定向到提交历史)
 */
export const getVersionHistory = (resumeId: string): Promise<ResumeVersion[]> => {
  return getCommitHistory(resumeId)
}

/**
 * 获取特定版本内容 (兼容性API，使用提交ID作为版本号)
 */
export const getVersion = (resumeId: string, commitId: string): Promise<ResumeVersion> => {
  return getCommit(resumeId, commitId)
}

/**
 * 恢复到指定版本 (兼容性API，重定向到重置提交)
 */
export const restoreToVersion = (resumeId: string, commitId: string): Promise<Resume> => {
  return resetToCommit(resumeId, commitId)
}

/**
 * 创建版本快照 (兼容性API，重定向到手动提交)
 */
export const createVersionSnapshot = (
  resumeId: string,
  description?: string
): Promise<ResumeVersion> => {
  // 注意：这个兼容性函数需要从调用方传递更多信息
  // 实际使用时应该调用 commitChanges 并传递完整的 CommitRequest
  const commitData: CommitRequest = {
    title: '手动快照',
    content: '', // 需要从调用方获取
    message: description || '手动创建快照',
    author: 'user'
  }
  return commitChanges(resumeId, commitData)
}

/**
 * 清理旧版本 (兼容性API，现在改为清理旧提交)
 */
export const cleanupOldVersions = (
  resumeId: string,
  keepCount: number = 10
): Promise<void> => {
  // 注意：新的Git风格版本管理在服务端自动处理提交清理
  // 这里返回一个空的Promise以保持兼容性
  return Promise.resolve()
}
