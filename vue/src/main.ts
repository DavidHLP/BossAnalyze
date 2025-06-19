// Vue 核心
import { createApp } from 'vue'

// 第三方UI组件库
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { createBootstrap } from 'bootstrap-vue-next'

// 状态管理
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

// 项目核心模块
import App from './App.vue'
import router from './router'
import { setupAsyncRoutes, startPermissionMonitor, hasPermission } from '@/router'
import { useUserStore } from '@/store/user/userStore'

// 样式文件
import '@/assets/global.scss'
import 'element-plus/dist/index.css'
// import 'element-plus/theme-chalk/dark/css-vars.css'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue-next/dist/bootstrap-vue-next.css'

// Mock服务 (开发环境)
// import '@/mock'

// 创建应用实例
const app = createApp(App)

// 配置状态管理
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// 注册插件
app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.use(createBootstrap())

// 注册Element Plus图标组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局属性注册
app.config.globalProperties.$hasPermission = hasPermission

// 应用初始化
router.isReady().then(async () => {
  // 监听未授权事件
  window.addEventListener('unauthorized', () => {
    const currentPath = router.currentRoute.value.fullPath
    router.push(`/login?redirect=${encodeURIComponent(currentPath)}`)
  })

  // 用户认证和权限初始化
  const userStore = useUserStore()
  const token = localStorage.getItem('token')

  if (token) {
    try {
      // 设置token时间戳
      if (!localStorage.getItem('token_timestamp')) {
        localStorage.setItem('token_timestamp', Date.now().toString())
      }

      // 初始化用户权限和路由
      await userStore.getUserPermissions()
      await setupAsyncRoutes()

      // 启动权限监控
      startPermissionMonitor()

      // 跳转到当前路由
      router.push(router.currentRoute.value.fullPath)
    } catch (error) {
      console.error('初始化路由失败:', error)

      // 清理无效token
      localStorage.removeItem('token')
      localStorage.removeItem('token_timestamp')

      router.push('/login')
    }
  }

  // 挂载应用
  app.mount('#app')
})
