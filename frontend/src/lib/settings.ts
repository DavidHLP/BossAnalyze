import { permissionRows, permissionTree } from './roles.ts'

function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value))
    throw new Error('Invalid settings response')
  const result = value as Record<string, unknown>
  if ('code' in result && result.code !== 200) throw result
  return result
}
function id(value: unknown): value is number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0
}
export function accountSnapshot(value: unknown) {
  const data = record(value)
  if (
    !id(data.id) ||
    typeof data.email !== 'string' ||
    !data.email.trim() ||
    typeof data.name !== 'string'
  )
    throw new Error('Invalid account')
  return {
    id: data.id,
    name: data.name,
    email: data.email,
    roleId: id(data.roleId) ? data.roleId : null,
  }
}
export function accessSnapshot(role: unknown, permissions: unknown) {
  const data = record(role)
  if (!id(data.id) || typeof data.roleName !== 'string' || !data.roleName.trim())
    throw new Error('Invalid role')
  if (
    !Array.isArray(permissions) ||
    !permissions.every((value) => typeof value === 'string' && value.trim())
  ) {
    if (permissions && typeof permissions === 'object' && !Array.isArray(permissions))
      record(permissions)
    throw new Error('Invalid permissions')
  }
  return {
    id: data.id,
    name: data.roleName,
    status: data.status === 0 || data.status === 1 ? data.status : null,
    remark: typeof data.remark === 'string' ? data.remark : '',
    permissions: [...new Set(permissions as string[])].sort(),
  }
}
export function roleStatus(status: number | null) {
  if (status === 1) return { label: '启用', tone: 'success' } as const
  if (status === 0) return { label: '停用', tone: 'danger' } as const
  return { label: '状态未知', tone: 'neutral' } as const
}

export function permissionLabels(routes: unknown, permissions: string[]) {
  const granted = new Set(permissions)
  const matches = new Map<string, Set<string>>()
  for (const { node } of permissionRows(permissionTree(routes))) {
    const title = node.title.trim()
    if (node.status !== 1 || !granted.has(node.permission) || !title) continue
    const labels = matches.get(node.permission) ?? new Set<string>()
    labels.add(title)
    matches.set(node.permission, labels)
  }
  return new Map(Array.from(matches, ([permission, labels]) => [permission, [...labels].join(' / ')]))
}

export function healthSnapshot(value: unknown) {
  const data = record(value)
  if (
    (data.status !== 'UP' || data.sparkService !== 'UP') &&
    (data.status !== 'DEGRADED' || data.sparkService !== 'DOWN')
  )
    throw new Error('Invalid service health')
  if (
    typeof data.timestamp !== 'number' ||
    !Number.isSafeInteger(data.timestamp) ||
    data.timestamp <= 0 ||
    !Number.isFinite(new Date(data.timestamp).getTime())
  )
    throw new Error('Invalid health timestamp')
  return { connected: data.status === 'UP', checkedAt: data.timestamp }
}

export function degradedHealthResponse(error: unknown) {
  if (!error || typeof error !== 'object' || !('code' in error) || !('data' in error)) return null
  const { code, data } = error
  if (typeof code !== 'number' || code === 200 || code === 401 || code === 403) return null
  try {
    const health = healthSnapshot(data)
    if (!health.connected) {
      return { status: 'DEGRADED', sparkService: 'DOWN', timestamp: health.checkedAt } as const
    }
  } catch {
    // The request error still owns malformed and missing health responses.
  }
  return null
}

export function settingsFailure(error: unknown): 'error' | 'forbidden' {
  return error && typeof error === 'object' && 'code' in error && error.code === 403
    ? 'forbidden'
    : 'error'
}
