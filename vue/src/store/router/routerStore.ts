import { defineStore } from 'pinia'
import type { Router } from '@/router/index.d'
import { getUserRoutes } from '@/api/router/router'
import { transformRoutes } from '@/router/index'

export const useRouterStore = defineStore('router', {
  state: () => ({
    routes: [] as Router[],
  }),
  actions: {
    setRoutes(routes: Router[]) {
      this.routes = routes
    },
    async fetchRoutes() {
      try {
        // 先检查本地存储是否有缓存
        const cachedRoutes = localStorage.getItem('cachedRoutes')
        if (cachedRoutes && this.routes.length === 0) {
          this.routes = JSON.parse(cachedRoutes)
        }

        if (this.routes.length === 0) {
          const backendRoutes = await getUserRoutes()
          backendRoutes.forEach((route) => {
            route.meta.permission = route.permission
          })
          this.setRoutes(backendRoutes)
          // 缓存到本地存储
          localStorage.setItem('cachedRoutes', JSON.stringify(backendRoutes))
        }
        return transformRoutes(this.routes)
      } catch (error) {
        console.error('路由获取失败', error)
        return []
      }
    },
    async refreshRoutes() {
      if (this.routes.length === 0) {
        await this.fetchRoutes()
      }
    },
  },
})
