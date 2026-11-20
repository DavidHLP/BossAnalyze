import { createRouter, createWebHistory } from 'vue-router'
import { useSession } from '@/composables/useSession'
import { authorizeRoute } from './guard'
import ConsoleLayout from '@/layouts/ConsoleLayout.vue'

const portalModules = import.meta.glob('../pages/**/*.vue', { import: 'default' })
function view(name: string) {
  return portalModules[`../pages/${name}.vue`] as () => Promise<import('vue').Component>
}

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/console' },
    { path: '/login', component: view('login'), meta: { title: '登录' } },
    {
      path: '/console', component: ConsoleLayout, meta: { requiresSession: true },
      children: [
        { path: '', component: view('console/index'), meta: { title: '工作台' } },
        { path: 'dashboard', component: view('console/dashboard'), meta: { title: '数据看板' } },
        { path: 'users', component: view('console/users'), meta: { title: '用户管理' } },
        { path: 'roles', component: view('console/roles'), meta: { title: '角色管理' } },
        { path: 'menus', component: view('console/menus'), meta: { title: '菜单管理' } },
        { path: 'settings', component: view('console/settings'), meta: { title: '系统设置' } },
        { path: 'profile', component: view('console/profile'), meta: { title: '个人中心' } },
        { path: ':slug(.*)*', component: view('console/AuthorizedPlaceholder'), meta: { title: '控制台' } },
      ],
    },
    {
      path: '/recruitment', component: ConsoleLayout, meta: { requiresSession: true },
      children: [{ path: '', component: view('recruitment'), meta: { title: '招聘数据分析' } }],
    },
    { path: '/system-error/:code(403|503)', name: 'system-error', component: view('SystemError') },
    { path: '/:pathMatch(.*)*', component: view('SystemError'), meta: { title: '页面不存在' } },
  ],
})

router.beforeEach(to => to.meta.requiresSession ? authorizeRoute(useSession(router), to) : true)

router.afterEach((to, _from, failure) => {
  if (!failure) document.title = to.meta.title ? `${to.meta.title} · BossAnalyze` : 'BossAnalyze'
})
