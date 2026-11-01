export interface MenuRecord {
  id: number
  pid: number | null
  menuOrder: number
  status: number
  remark: string
  permission: string | null
  path: string
  name: string
  meta: {
    type: string
    component: string | null
    redirect: string | null
    alwaysShow: boolean
    metaTitle: string
    metaIcon: string | null
    metaHidden: boolean | null
    metaRoles: string | null
    metaKeepAlive: boolean | null
    hidden: boolean
  }
  children: MenuRecord[]
}
export interface MenuDraft {
  title: string
  name: string
  type: string
  path: string
  component: string
  permission: string
  order: string
  status: string
  remark: string
  redirect: string
  icon: string
  alwaysShow: boolean
  metaHidden: boolean
  keepAlive: boolean
  hidden: boolean
}
const object = (value: unknown): value is Record<string, unknown> =>
  value !== null && typeof value === 'object' && !Array.isArray(value)
const validId = (value: unknown): value is number =>
  typeof value === 'number' && Number.isSafeInteger(value) && value > 0
export function managedMenus(value: unknown): MenuRecord[] {
  const seen = new Set<number>()
  function parse(raw: unknown, parent: number | null): MenuRecord[] {
    if (!Array.isArray(raw)) throw new Error('Invalid menus')
    return raw
      .map((item): MenuRecord => {
        if (
          !object(item) ||
          !validId(item.id) ||
          seen.has(item.id) ||
          item.pid !== parent ||
          !Number.isInteger(item.menuOrder) ||
          !Number.isInteger(item.status) ||
          typeof item.name !== 'string' ||
          typeof item.path !== 'string' ||
          !object(item.meta)
        )
          throw new Error('Invalid menu')
        seen.add(item.id)
        const meta = item.meta
        for (const key of ['type', 'metaTitle'])
          if (typeof meta[key] !== 'string') throw new Error('Invalid menu metadata')
        for (const key of ['alwaysShow', 'hidden'])
          if (typeof meta[key] !== 'boolean') throw new Error('Invalid menu visibility')
        for (const key of ['metaHidden', 'metaKeepAlive'])
          if (meta[key] != null && typeof meta[key] !== 'boolean')
            throw new Error('Invalid menu visibility')
        for (const field of [
          item.permission,
          meta.component,
          meta.redirect,
          meta.metaIcon,
          meta.metaRoles,
        ])
          if (field != null && typeof field !== 'string') throw new Error('Invalid menu field')
        return {
          id: item.id,
          pid: parent,
          menuOrder: item.menuOrder as number,
          status: item.status as number,
          name: item.name,
          path: item.path,
          remark: typeof item.remark === 'string' ? item.remark : '',
          permission: (item.permission as string | null) ?? null,
          meta: {
            type: meta.type as string,
            metaTitle: meta.metaTitle as string,
            component: (meta.component as string | null) ?? null,
            redirect: (meta.redirect as string | null) ?? null,
            metaIcon: (meta.metaIcon as string | null) ?? null,
            metaRoles: (meta.metaRoles as string | null) ?? null,
            alwaysShow: meta.alwaysShow as boolean,
            metaHidden: (meta.metaHidden as boolean | null) ?? null,
            metaKeepAlive: (meta.metaKeepAlive as boolean | null) ?? null,
            hidden: meta.hidden as boolean,
          },
          children: parse(item.children ?? [], item.id),
        }
      })
      .sort((a, b) => a.menuOrder - b.menuOrder || a.id - b.id)
  }
  return parse(value, null)
}
export function menuRows(
  nodes: MenuRecord[],
  depth = 0,
): Array<{ node: MenuRecord; depth: number }> {
  return nodes.flatMap((node) => [{ node, depth }, ...menuRows(node.children, depth + 1)])
}
export function visibleMenus(
  nodes: MenuRecord[],
  query: string,
  status: string,
  collapsed: number[],
) {
  const filtering = Boolean(query || status !== 'all')
  function walk(
    branch: MenuRecord[],
    depth: number,
  ): Array<{ node: MenuRecord; depth: number; match: boolean }> {
    return branch.flatMap((node) => {
      const match =
        (!query ||
          [node.name, node.meta.metaTitle, node.path, node.permission ?? ''].some((value) =>
            value.toLocaleLowerCase().includes(query.toLocaleLowerCase()),
          )) &&
        (status === 'all' || String(node.status) === status)
      const children =
        filtering || !collapsed.includes(node.id) ? walk(node.children, depth + 1) : []
      return !filtering || match || children.length ? [{ node, depth, match }, ...children] : []
    })
  }
  return walk(nodes, 0)
}
export function menuDraft(node?: MenuRecord | null): MenuDraft {
  return {
    title: node?.meta.metaTitle ?? '',
    name: node?.name ?? '',
    type: node?.meta.type ?? 'M',
    path: node?.path ?? '',
    component: node?.meta.component ?? '',
    permission: node?.permission ?? '',
    order: String(node?.menuOrder ?? 0),
    status: String(node?.status ?? 1),
    remark: node?.remark ?? '',
    redirect: node?.meta.redirect ?? '',
    icon: node?.meta.metaIcon ?? '',
    alwaysShow: node?.meta.alwaysShow ?? false,
    metaHidden: node?.meta.metaHidden ?? false,
    keepAlive: node?.meta.metaKeepAlive ?? false,
    hidden: node?.meta.hidden ?? false,
  }
}
export function menuErrors(draft: MenuDraft, nodes: MenuRecord[], current?: MenuRecord | null) {
  const errors: Record<string, string> = {}
  const rows = menuRows(nodes)
  for (const key of ['title', 'name', 'path', 'permission'] as const)
    if (!draft[key].trim() || draft[key].trim().length > 255) errors[key] = '请输入 1–255 个字符'
  for (const key of ['component', 'remark', 'redirect', 'icon'] as const)
    if (draft[key].length > 255) errors[key] = '不能超过 255 个字符'
  if (!['M', 'C', 'F'].includes(draft.type)) errors.type = '请选择菜单类型'
  if (draft.type === 'F' && current?.children.length) errors.type = '请先处理子菜单，再改为操作类型'
  if (!['0', '1'].includes(draft.status)) errors.status = '请选择状态'
  if (
    !/^-?\d+$/.test(draft.order) ||
    !Number.isInteger(Number(draft.order)) ||
    Number(draft.order) < -2147483648 ||
    Number(draft.order) > 2147483647
  )
    errors.order = '请输入有效的整数排序值'
  if (rows.some(({ node }) => node.id !== current?.id && node.name === draft.name.trim()))
    errors.name = '路由名称已存在'
  if (!current && rows.some(({ node }) => node.permission === draft.permission.trim()))
    errors.permission = '权限标识已存在，请使用独立标识以免继承已有授权'
  if (current && draft.permission !== current.permission) errors.permission = '已有权限标识不可修改'
  return errors
}
export function menuPayload(
  draft: MenuDraft,
  nodes: MenuRecord[],
  current: MenuRecord | null,
  parent: MenuRecord | null,
) {
  if (Object.keys(menuErrors(draft, nodes, current)).length) throw new Error('Invalid menu form')
  if (
    !current &&
    parent &&
    (parent.meta.type === 'F' || (parent.pid !== null && parent.pid >= parent.id))
  )
    throw new Error('Unsupported parent')
  return {
    ...(current ? { id: current.id } : {}),
    pid: current ? current.pid : (parent?.id ?? null),
    name: draft.name.trim(),
    path: draft.path.trim(),
    permission: draft.permission.trim(),
    menuOrder: Number(draft.order),
    status: Number(draft.status),
    remark: draft.remark.trim(),
    meta: {
      type: draft.type,
      metaTitle: draft.title.trim(),
      component: draft.component.trim(),
      redirect: draft.redirect.trim(),
      metaIcon: draft.icon.trim(),
      alwaysShow: draft.alwaysShow,
      metaHidden: current?.meta.metaHidden === null && !draft.metaHidden ? null : draft.metaHidden,
      metaKeepAlive:
        current?.meta.metaKeepAlive === null && !draft.keepAlive ? null : draft.keepAlive,
      hidden: draft.hidden,
      metaRoles: current?.meta.metaRoles ?? '[]',
    },
  }
}
export function menuDeleteReason(node: MenuRecord, nodes: MenuRecord[], rolePermissions: string[]) {
  if (node.children.length) return '请先逐项处理子菜单；不支持级联删除。'
  if (!node.permission) return '该菜单缺少有效权限标识，暂不能安全删除。'
  if (
    menuRows(nodes).some(
      (row) => row.node.id !== node.id && row.node.permission === node.permission,
    )
  )
    return '其他菜单共享此权限标识，暂不能删除。'
  if (rolePermissions.includes(node.permission))
    return '角色仍引用此权限，请先在角色管理中移除授权。'
  return ''
}
export function menuWriteSucceeded(value: unknown) {
  if (!object(value) || value.code !== 200) throw new Error('Menu operation failed')
}
