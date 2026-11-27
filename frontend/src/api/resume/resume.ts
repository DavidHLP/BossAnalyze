import request from '@/utils/request/request'
import { requireMutationSuccess } from '@/lib/rbac-mutations'
import type { ResumeThemeId } from '@/lib/resume-theme'

export interface ResumeRecord {
  id: string
  userId: number
  title: string
  content: string | null
  createdAt?: string | number
  updatedAt?: string | number
  currentBranch?: string
  headCommit?: string | null
  workspaceRevision?: number
  themeId?: ResumeThemeId
  branches?: Record<string, string | null>
  stashStack?: unknown[]
}

export interface ResumeCommit {
  id: string
  content: string | null
  message?: string
  branch?: string
  commitType?: string
  parentCommits?: string[]
  author?: string
  commitTime?: string | number
}

export interface ResumeValidation {
  isValid?: boolean
  errors?: string[]
  warnings?: string[]
  branchCount?: number
  orphanCommits?: number
  totalCommits?: number
}

type ResumeWrite =
  | ['commit', { message: string; content: string }]
  | ['branch', { name: string }]
  | ['switch', { branch: string }]
  | ['merge', { source: string; target: string }]
  | ['checkout' | 'reset' | 'revert', { commitId: string }]
  | ['stash', { content: string }]
  | ['stash/pop' | 'fix-duplicate-ids']

function resource(id: string) {
  if (!/^[A-Za-z0-9_-]+$/.test(id)) throw new Error('Invalid resume ID')
  return '/api/resume/' + id
}

function record(value: unknown): ResumeRecord {
  if (!value || typeof value !== 'object' ||
      !('id' in value) || typeof value.id !== 'string' || !/^[A-Za-z0-9_-]+$/.test(value.id) ||
      !('title' in value) || typeof value.title !== 'string' ||
      ('content' in value && value.content != null && typeof value.content !== 'string')) {
    throw new Error('Invalid resume response')
  }
  return value as ResumeRecord
}

export async function listResumes() {
  const data = await request.get<unknown, unknown>('/api/resume')
  if (!Array.isArray(data)) throw new Error('Invalid resume list')
  return data.map(record)
}

export async function getResume(id: string) {
  return record(await request.get(resource(id)))
}

export async function updateResumeAppearance(id: string, themeId: ResumeThemeId) {
  const result: unknown = await request.patch(resource(id) + '/appearance', { themeId })
  if (!result || typeof result !== 'object' || !('themeId' in result) || result.themeId !== themeId) {
    throw new Error('简历主题未确认')
  }
}

function writtenRecord(value: unknown) {
  if (value && typeof value === 'object' && 'code' in value) requireMutationSuccess(value)
  try {
    return record(value)
  } catch {
    throw Object.assign(new Error('Missing written resume record'), { resultUnknown: true })
  }
}

export async function createResume(title: string, content: string) {
  return writtenRecord(await request.post('/api/resume', { title, content }))
}

export async function renameResume(id: string, title: string) {
  return writtenRecord(await request.put(resource(id), { title }))
}

export async function deleteResume(id: string) {
  requireMutationSuccess(await request.delete(resource(id)))
}

export async function resumeHistory(id: string, recent: boolean | number = false, range?: { fromCommit: string; toCommit: string }) {
  const version = resource(id) + '/version/'
  const data = await request.get<unknown, unknown>(version + (range ? 'diff' : recent ? 'recent' : 'history'), {
    params: range ?? (recent ? { limit: recent === true ? 10 : recent } : undefined),
  })
  if (!Array.isArray(data) || data.some(item => !item || typeof item.id !== 'string' ||
    (item.content != null && typeof item.content !== 'string'))) {
    throw new Error('Invalid history response')
  }
  return data as ResumeCommit[]
}

export function inspectResume(id: string) {
  return request.get<ResumeValidation, ResumeValidation>(resource(id) + '/version/debug/validate')
}

export function inspectCommit(id: string, commitId: string) {
  return request.get<Record<string, unknown>, Record<string, unknown>>(
    resource(id) + '/version/debug/commit/' + encodeURIComponent(commitId),
  )
}

export async function changeResumeVersion(id: string, ...[action, data, workspaceRevision]: [...ResumeWrite, number?]) {
  const body = workspaceRevision === undefined
    ? data
    : { ...((data ?? {}) as Record<string, unknown>), workspaceRevision: String(workspaceRevision) }
  const result: unknown = await request.post(resource(id) + '/version/' + action, body)
  if (action === 'fix-duplicate-ids') {
    if (!result || typeof result !== 'object' || !('success' in result) || result.success !== true) {
      throw new Error('Repair was not confirmed')
    }
  } else if (action === 'commit' || action === 'merge' || action === 'revert') {
    if (typeof result !== 'string' || !result.trim()) throw new Error('Missing commit ID')
  } else if (action === 'stash/pop') {
    if (typeof result === 'string') return result
    requireMutationSuccess(result)
  } else {
    requireMutationSuccess(result)
  }
  return typeof result === 'string' ? result : undefined
}
