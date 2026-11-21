import { createRouter, createWebHistory } from 'vue-router'
import { useSession } from '@/composables/useSession'
import { authorizeRoute } from './guard'
import { consoleScrollBehavior } from './scroll'
import ConsoleLayout from '@/layouts/ConsoleLayout.vue'
import { findMenu } from '@/lib/session'

declare module 'vue-router' {
  interface RouteMeta {
    access?: 'session' | 'menu'
    title?: string
    placeholder?: true
  }
}

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  sensitive: true,
  strict: true,
  scrollBehavior: consoleScrollBehavior,
  routes: [
    { path: '/', redirect: '/console' },
    { path: '/login', component: () => import('../pages/login.vue'), meta: { title: '登录' } },
    { path: '/register', component: () => import('../pages/register.vue'), meta: { title: '注册' } },
    {
      path: '/console', component: ConsoleLayout, meta: { access: 'menu' },
      children: [
        { path: '', component: () => import('../pages/console/index.vue'), meta: { title: '工作台', access: 'session' } },
        { path: 'logs', component: () => import('../pages/console/logs.vue'), meta: { title: '扩展日志分析' } },
        { path: 'dashboard', component: () => import('../pages/console/dashboard.vue'), meta: { title: '数据看板' } },
        { path: 'users', component: () => import('../pages/console/users.vue'), meta: { title: '用户管理' } },
        { path: 'roles', component: () => import('../pages/console/roles.vue'), meta: { title: '角色管理' } },
        { path: 'menus', component: () => import('../pages/console/menus.vue'), meta: { title: '菜单管理' } },
        { path: 'settings', component: () => import('../pages/console/settings.vue'), meta: { title: '系统设置' } },
        { path: 'profile', component: () => import('../pages/console/profile.vue'), meta: { title: '个人中心' } },
        { path: ':slug(.*)*', component: () => import('../pages/console/AuthorizedPlaceholder.vue'), meta: { placeholder: true } },
      ],
    },
    {
      path: '/recruitment', component: ConsoleLayout, meta: { access: 'session' },
      children: [{ path: '', component: () => import('../pages/recruitment.vue'), meta: { title: '招聘数据分析' } }],
    },
    {
      path: '/resumes', component: ConsoleLayout, meta: { access: 'session' },
      children: [
        { path: '', component: () => import('../pages/resumes/index.vue'), meta: { title: '我的简历' } },
        { path: ':id([A-Za-z0-9_-]+)/edit', component: () => import('../pages/resumes/edit.vue'), meta: { title: '编辑简历' } },
        { path: ':id([A-Za-z0-9_-]+)/versions', component: () => import('../pages/resumes/versions.vue'), meta: { title: '版本历史' } },
      ],
    },
    { path: '/system-error/:code(403|503)', name: 'system-error', component: () => import('../pages/SystemError.vue') },
    { path: '/:pathMatch(.*)*', component: () => import('../pages/SystemError.vue'), meta: { title: '页面不存在' } },
  ],
})

router.beforeEach(to => to.meta.access ? authorizeRoute(useSession(router), to) : true)

router.afterEach((to, _from, failure) => {
  if (failure) return
  const menuTitle = to.meta.access === 'menu'
    ? findMenu(useSession(router).state.value.menus, to.path)?.title.trim()
    : undefined
  const title = menuTitle || to.meta.title || (to.meta.placeholder ? '未命名功能' : '')
  document.title = title ? `${title} · BossAnalyze` : 'BossAnalyze'
})
