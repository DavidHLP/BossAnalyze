import 'element-plus/theme-chalk/dark/css-vars.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
// 引入Pinia持久化插件
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

import App from './App.vue'
import router from './router'
import { useUserStore } from '@/stores/user/userStore'
import { setupAsyncRoutes, startPermissionMonitor, hasPermission } from '@/router'

const app = createApp(App)

// 创建Pinia实例并使用持久化插件
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// 使用增强后的pinia
app.use(pinia)
app.use(router)

// 注册 ElementPlusIconsVue 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus, {
  locale: zhCn, // 一定要加上，不然无效
})

// 全局注册hasPermission方法
app.config.globalProperties.$hasPermission = hasPermission

router.isReady().then(async () => {
  window.addEventListener('unauthorized', () => {
    router.push('/login?redirect=' + encodeURIComponent(router.currentRoute.value.fullPath))
  })

  const userStore = useUserStore()
  if (localStorage.getItem('token')) {
    try {
      // 记录token时间戳，用于检查有效期
      if (!localStorage.getItem('token_timestamp')) {
        localStorage.setItem('token_timestamp', Date.now().toString())
      }

      await userStore.getUserPermissions()
      await setupAsyncRoutes()

      // 启动权限监控
      startPermissionMonitor()

      router.push(router.currentRoute.value.fullPath)
    } catch (error) {
      console.error('初始化路由失败', error)
      localStorage.removeItem('token')
      localStorage.removeItem('token_timestamp')
      router.push('/login')
    }
  }
  app.mount('#app')
})
