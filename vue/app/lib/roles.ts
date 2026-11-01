export interface ManagedRole {
  id: number
  roleName: string
  status: 0 | 1 | null
  remark: string
  permissions: string[]
  routerIds: number[]
}
export interface PermissionNode {
  id: number
  title: string
  permission: string
  status: number | null
  children: PermissionNode[]
}
export interface RoleDraft {
  roleName: string
  status: string
  remark: string
}
function object(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}
function id(value: unknown): value is number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0
}
export function currentRoleId(value: unknown) {
  if (!object(value) || !id(value.id)) throw new Error('Invalid current role')
  return value.id
}
export function managedRoles(value: unknown): ManagedRole[] {
  if (!Array.isArray(value)) throw new Error('Invalid roles')
  const seen = new Set<number>()
  return value
    .map((item): ManagedRole => {
      if (!object(item) || !id(item.id) || seen.has(item.id) || typeof item.roleName !== 'string')
        throw new Error('Invalid role')
      const permissions = item.permissions ?? []
      const routers = item.routers ?? []
      if (!Array.isArray(permissions) || !Array.isArray(routers))
        throw new Error('Invalid role grants')
      seen.add(item.id)
      return {
        id: item.id,
        roleName: item.roleName,
        status: item.status === 0 || item.status === 1 ? item.status : null,
        remark: typeof item.remark === 'string' ? item.remark : '',
        permissions: [
          ...new Set(
            permissions.map((permission: unknown) => {
              if (
                !object(permission) ||
                typeof permission.permission !== 'string' ||
                !permission.permission
              )
                throw new Error('Invalid permission')
              return permission.permission
            }),
          ),
        ],
        routerIds: [
          ...new Set(
            routers.map((route: unknown) => {
              if (!object(route) || !id(route.id)) throw new Error('Invalid role router')
              return route.id
            }),
          ),
        ],
      }
    })
    .sort((a, b) => b.id - a.id)
}
export function permissionTree(value: unknown): PermissionNode[] {
  const seen = new Set<number>()
  function parse(raw: unknown): PermissionNode[] {
    if (!Array.isArray(raw)) throw new Error('Invalid router tree')
    return raw.map((item) => {
      if (
        !object(item) ||
        !id(item.id) ||
        item.id > 2147483647 ||
        seen.has(item.id) ||
        (item.permission != null && typeof item.permission !== 'string')
      )
        throw new Error('Invalid router')
      seen.add(item.id)
      const meta = object(item.meta) ? item.meta : {}
      return {
        id: item.id,
        title:
          typeof meta.metaTitle === 'string'
            ? meta.metaTitle
            : typeof item.name === 'string'
              ? item.name
              : `路由 #${item.id}`,
        permission: typeof item.permission === 'string' ? item.permission : '',
        status: typeof item.status === 'number' ? item.status : null,
        children: parse(item.children ?? []),
      }
    })
  }
  return parse(value)
}
export function permissionRows(
  nodes: PermissionNode[],
  parent: number | null = null,
): Array<{ node: PermissionNode; parent: number | null }> {
  return nodes.flatMap((node) => [{ node, parent }, ...permissionRows(node.children, node.id)])
}
export function canonicalSelection(nodes: PermissionNode[], ids: number[]): number[] {
  const rows = permissionRows(nodes)
  if (ids.some((value) => !rows.some((row) => row.node.id === value)))
    throw new Error('Unknown router selection')
  const selected = new Set(ids)
  let previous = -1
  while (previous !== selected.size) {
    previous = selected.size
    for (const { node, parent } of rows) {
      if (!selected.has(node.id)) continue
      if (parent !== null) selected.add(parent)
      if (node.permission)
        for (const alias of rows)
          if (alias.node.permission === node.permission) selected.add(alias.node.id)
    }
  }
  return [...selected].sort((a, b) => a - b)
}
export function togglePermission(
  nodes: PermissionNode[],
  selected: number[],
  target: number,
  checked: boolean,
) {
  const rows = permissionRows(nodes)
  if (checked) return canonicalSelection(nodes, [...selected, target])
  const blocked = new Set([target])
  let previous = -1
  while (previous !== blocked.size) {
    previous = blocked.size
    const permissions = new Set(
      rows
        .filter((row) => blocked.has(row.node.id) && row.node.permission)
        .map((row) => row.node.permission),
    )
    for (const { node, parent } of rows) {
      if (
        (parent !== null && blocked.has(parent)) ||
        (node.permission && permissions.has(node.permission))
      )
        blocked.add(node.id)
    }
  }
  return canonicalSelection(
    nodes,
    selected.filter((value) => !blocked.has(value)),
  )
}
export function selectionPermissions(nodes: PermissionNode[], ids: number[]) {
  const selected = new Set(canonicalSelection(nodes, ids))
  return [
    ...new Set(
      permissionRows(nodes)
        .filter((row) => selected.has(row.node.id) && row.node.permission)
        .map((row) => row.node.permission),
    ),
  ].sort()
}
export function unrepresentedGrants(role: ManagedRole, nodes: PermissionNode[]) {
  const rows = permissionRows(nodes)
  return [
    ...role.permissions.filter(
      (permission) => !rows.some((row) => row.node.permission === permission),
    ),
    ...role.routerIds
      .filter((routeId) => !rows.some((row) => row.node.id === routeId))
      .map((routeId) => `路由 #${routeId}`),
  ]
}
export function roleDraft(role?: ManagedRole | null): RoleDraft {
  return {
    roleName: role?.roleName ?? '',
    status: role ? String(role.status ?? '') : '1',
    remark: role?.remark ?? '',
  }
}
export function roleErrors(draft: RoleDraft) {
  const errors: Record<string, string> = {}
  if (!draft.roleName.trim() || draft.roleName.trim().length > 255)
    errors.roleName = '请输入 1–255 个字符的角色名称'
  if (!['0', '1'].includes(draft.status)) errors.status = '请选择角色状态'
  if (draft.remark.length > 255) errors.remark = '备注不能超过 255 个字符'
  return errors
}
export function rolePayload(draft: RoleDraft, role?: ManagedRole | null) {
  if (
    Object.keys(roleErrors(draft)).length ||
    (role?.roleName === 'USER' && draft.roleName.trim() !== 'USER')
  )
    throw new Error('Invalid role form')
  return {
    ...(role ? { id: role.id } : {}),
    roleName: draft.roleName.trim(),
    status: Number(draft.status),
    remark: draft.remark.trim(),
  }
}
export function roleWriteSucceeded(value: unknown) {
  if (!object(value) || value.code !== 200) throw new Error('Role operation failed')
}
