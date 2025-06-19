import { defineStore } from 'pinia'
import type { Router } from '@/router/index.d'
import { getUserBaseInfo, getUserPrivateInformation, getUserRole } from '@/api/auth/auth'
import type { Permissions, Role, UserBaseInfo } from '@/api/auth/auth.d'
import { getUserRoutes } from '@/api/router/router'
export const useUserStore = defineStore('user', {
  state: () => ({
    userId: null as number | null,
    username: null as string | null,
    email: null as string | null,
    permissions: [] as Permissions[],
    role: null as Role | null,
    routes: [] as Router[],
  }),
  actions: {
    async getUserPermissions() {
      try {
        const res: Array<Permissions> = await getUserPrivateInformation()
        this.permissions = res
      } catch (error) {
        localStorage.removeItem('token')
        throw error
      }
    },
    async getUserRoles() {
      try {
        const res: Role = await getUserRole()
        this.role = res
      } catch (error) {
        localStorage.removeItem('token')
        throw error
      }
    },
    async getUserRoutes() {
      try {
        const res: Router[] = await getUserRoutes()
        this.routes = res
      } catch (error) {
        localStorage.removeItem('token')
        throw error
      }
    },
    async getUserBaseInfo() {
      try {
        const res: UserBaseInfo = await getUserBaseInfo()
        this.userId = res.userId ?? null
        this.username = res.username ?? null
        this.email = res.email ?? null
      } catch (error) {
        localStorage.removeItem('token')
        throw error
      }
    },
  },
})
