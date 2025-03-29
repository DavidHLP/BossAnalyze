export interface RolePath {
  id?: number
  roleId: number
  pathId: number
  path?: {
    id: number
    path: string
    createdAt: string | null
  }
  createdAt?: string | null
  description?: string | null
}
