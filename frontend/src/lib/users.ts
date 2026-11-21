import { mutationBusinessCode, mutationError, mutationResultUnknown, permissionDenied } from './rbac-mutations.ts'

export { requireMutationSuccess as userWriteSucceeded } from './rbac-mutations.ts'

export interface ManagedUser {
  id: number
  name: string
  email: string
  status: 0 | 1 | null
  roleId: number | null
  roleName: string
  address: string
  introduction: string
  createTime: string
  lastLogin: string
  lastLoginIp: string
}

export interface UserRole {
  id: number
  roleName: string
  status: number | null
}
export interface UserDraft {
  name: string
  email: string
  password: string
  roleId: number | null
  status: string
  address: string
  introduction: string
}

function record(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}
function id(value: unknown): value is number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0
}
function text(value: unknown) {
  return typeof value === 'string' ? value : ''
}

export function userPageNumber(page: number, size: number) {
  if (!id(page) || !id(size)) throw new Error('Invalid pagination')
  // The existing service subtracts one, then passes the value directly to SQL LIMIT offset.
  return (page - 1) * size + 1
}

export function userPage(value: unknown) {
  if (
    !record(value) ||
    !Array.isArray(value.content) ||
    typeof value.totalElements !== 'number' ||
    !Number.isSafeInteger(value.totalElements) ||
    value.totalElements < 0
  ) {
    throw new Error('Invalid user page')
  }
  const users: ManagedUser[] = value.content.map((item) => {
    if (!record(item) || !id(item.id)) throw new Error('Invalid user')
    return {
      id: item.id,
      name: text(item.name),
      email: text(item.email),
      status: item.status === 0 || item.status === 1 ? item.status : null,
      roleId: id(item.roleId) ? item.roleId : null,
      roleName: text(item.roleName),
      address: text(item.address),
      introduction: text(item.introduction),
      createTime: text(item.createTime),
      lastLogin: text(item.lastLogin),
      lastLoginIp: text(item.lastLoginIp),
    }
  })
  return { users, total: value.totalElements }
}

export function userRoles(value: unknown): UserRole[] {
  if (!Array.isArray(value)) throw new Error('Invalid roles')
  return value.map((item) => {
    if (!record(item) || !id(item.id) || typeof item.roleName !== 'string')
      throw new Error('Invalid role')
    return {
      id: item.id,
      roleName: item.roleName,
      status: typeof item.status === 'number' ? item.status : null,
    }
  })
}

export function roleAssignmentError(role: UserRole | undefined) {
  if (role?.status === 1) return
  return role?.status === 0
    ? '该角色已停用，请选择启用的角色。'
    : '该角色无法指派，请选择启用的角色。'
}

export function userDraft(user?: ManagedUser | null): UserDraft {
  return {
    name: user?.name ?? '',
    email: user?.email ?? '',
    password: '',
    roleId: user?.roleId ?? null,
    status: user ? String(user.status ?? '') : '1',
    address: user?.address ?? '',
    introduction: user?.introduction ?? '',
  }
}

export function userErrors(draft: UserDraft, creating: boolean) {
  const errors: Record<string, string> = {}
  if (!draft.name.trim() || draft.name.trim().length > 50) errors.name = '请输入 1–50 个字符的姓名'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(draft.email.trim()) || draft.email.trim().length > 254)
    errors.email = '请输入有效的邮箱地址'
  if (!id(draft.roleId)) errors.roleId = '请选择用户角色'
  if (!['0', '1'].includes(draft.status)) errors.status = '请选择用户状态'
  if (creating && (draft.password.length < 8 || draft.password.length > 64))
    errors.password = '请输入 8–64 个字符的初始密码'
  if (draft.address.length > 255) errors.address = '地址不能超过 255 个字符'
  if (draft.introduction.length > 255) errors.introduction = '简介不能超过 255 个字符'
  return errors
}

export function userPayload(draft: UserDraft, userId?: number) {
  if (
    (userId !== undefined && !id(userId)) ||
    Object.keys(userErrors(draft, userId === undefined)).length
  ) {
    throw new Error('Invalid user form')
  }
  return {
    ...(userId === undefined ? { password: draft.password } : { id: userId }),
    name: draft.name.trim(),
    email: draft.email.trim(),
    roleId: draft.roleId!,
    status: Number(draft.status),
    address: draft.address.trim(),
    introduction: draft.introduction.trim(),
  }
}

function userSaveCode(value: unknown) {
  return mutationBusinessCode(value) ?? (record(value) ? value.code : undefined)
}

export function userSaveValidationRejected(value: unknown) {
  const code = userSaveCode(value)
  return !permissionDenied(value) && !mutationResultUnknown(value) &&
    (code === 400 || code === 409 || code === 1007 || code === 1008)
}

export function userSaveError(value: unknown, emailInUse = false) {
  const businessCode = mutationBusinessCode(value) ??
    (record(value) && value.resultUnknown === false ? value.code : undefined)
  // For user writes, a duplicate key in the business envelope identifies the unique email.
  if (!permissionDenied(value) && (userSaveCode(value) === 1001 ||
    businessCode === 409 || (emailInUse && userSaveValidationRejected(value)))) {
    return { field: 'email' as const, message: '邮箱已被其他账号使用，请更换邮箱。' }
  }
  return { field: null, message: mutationError(value, '保存') }
}

export function userDeleteError(value: unknown) {
  if (
    mutationBusinessCode(value) === 1006 ||
    (record(value) && value.code === 1006)
  ) return '操作者密码不正确，请重试'
  return mutationError(value, '删除')
}
