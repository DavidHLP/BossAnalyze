export interface UserStore {
  userId: number | null
  username: string | null
  email: string | null
  userInfo: Record<string, unknown> | null
  permissions: string[]
  roles: string | null
  routes: Routes[]
}

export interface Routes {
  id: number
  menuName: string
  pid: number
  orderNum: number
  path: string
  component: string
  menuType: 'M' | 'C' | 'F'
  status: 0 | 1
  remark: string
  permission: string
  children: Routes[] | null
}
