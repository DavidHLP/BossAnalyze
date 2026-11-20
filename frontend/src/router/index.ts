import { createRouter, createWebHistory } from 'vue-router'
import { useSession } from '@/composables/useSession'
import { authorizeRoute } from './guard'
import ConsoleLayout from '@/layouts/ConsoleLayout.vue'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/console' },
    { path: '/login', component: () => import('../pages/login.vue'), meta: { title: '登录' } },
    {
      path: '/console', component: ConsoleLayout, meta: { requiresSession: true },
      children: [
        { path: '', component: () => import('../pages/console/index.vue'), meta: { title: '工作台' } },
        { path: 'dashboard', component: () => import('../pages/console/dashboard.vue'), meta: { title: '数据看板' } },
        { path: 'users', component: () => import('../pages/console/users.vue'), meta: { title: '用户管理' } },
        { path: 'roles', component: () => import('../pages/console/roles.vue'), meta: { title: '角色管理' } },
        { path: 'menus', component: () => import('../pages/console/menus.vue'), meta: { title: '菜单管理' } },
        { path: 'settings', component: () => import('../pages/console/settings.vue'), meta: { title: '系统设置' } },
        { path: 'profile', component: () => import('../pages/console/profile.vue'), meta: { title: '个人中心' } },
        { path: ':slug(.*)*', component: () => import('../pages/console/AuthorizedPlaceholder.vue'), meta: { title: '控制台' } },
      ],
    },
    {
      path: '/recruitment', component: ConsoleLayout, meta: { requiresSession: true },
      children: [{ path: '', component: () => import('../pages/recruitment.vue'), meta: { title: '招聘数据分析' } }],
    },
    { path: '/system-error/:code(403|503)', name: 'system-error', component: () => import('../pages/SystemError.vue') },
    { path: '/:pathMatch(.*)*', component: () => import('../pages/SystemError.vue'), meta: { title: '页面不存在' } },
  ],
})

router.beforeEach(to => to.meta.requiresSession ? authorizeRoute(useSession(router), to) : true)

router.afterEach((to, _from, failure) => {
  if (!failure) document.title = to.meta.title ? `${to.meta.title} · BossAnalyze` : 'BossAnalyze'
})
