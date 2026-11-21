<script setup lang="ts">
import { computed } from 'vue'
import { useSession } from '@/composables/useSession'
import AppBrand from './AppBrand.vue'
import NavMain from './NavMain.vue'
import NavUser from './NavUser.vue'
import type { SidebarProps } from '@/components/ui/sidebar'
import { useSidebar } from '@/components/ui/sidebar'
import { findMenu, navigationMenus, type MenuItem } from '@/lib/session'

const props = withDefaults(defineProps<SidebarProps>(), { collapsible: 'icon' })
const { state: session } = useSession()
const { setOpenMobile } = useSidebar()
const workspace: MenuItem[] = [
  { path: '/console', title: '工作台', children: [] },
  { path: '/recruitment', title: '招聘数据分析', icon: 'ChartHistogramIcon', children: [] },
  { path: '/resumes', title: '我的简历', children: [] },
]
const menus = computed(() => {
  const seen = new Set(workspace.map((item) => item.path))
  function uniqueMenus(items: MenuItem[]): MenuItem[] {
    return items.flatMap((item) => {
      const duplicate = item.path !== null && seen.has(item.path)
      if (item.path) seen.add(item.path)
      const children = uniqueMenus(item.children)
      if (duplicate) return children
      return item.path || children.length ? [{ ...item, children }] : []
    })
  }
  const allowed = navigationMenus(session.value.menus)
  const logsAllowed = Boolean(findMenu(allowed, '/console/dashboard'))
  if (!logsAllowed) seen.add('/console/logs')
  const unique = uniqueMenus(allowed)
  if (logsAllowed && !seen.has('/console/logs')) unique.push({
    path: '/console/logs', title: '扩展日志分析', icon: 'ChartHistogramIcon', children: [],
  })
  return unique
})
</script>

<template>
  <UiSidebar v-bind="props">
    <UiSidebarHeader class="h-14 justify-center border-b border-sidebar-border pr-14 md:pr-2">
      <UiSidebarMenu>
        <UiSidebarMenuItem>
          <UiSidebarMenuButton as-child size="lg" class="h-10 text-sm" tooltip="BossAnalyze">
            <RouterLink to="/console" @click="setOpenMobile(false)">
              <AppBrand />
            </RouterLink>
          </UiSidebarMenuButton>
        </UiSidebarMenuItem>
      </UiSidebarMenu>
    </UiSidebarHeader>
    <UiSidebarContent aria-label="控制台导航" class="py-2">
      <NavMain label="工作空间" :items="workspace" />
      <NavMain label="系统管理" :items="menus" />
    </UiSidebarContent>
    <UiSidebarFooter class="border-t border-sidebar-border">
      <p class="hidden items-center justify-between gap-2 px-2 text-xs text-muted-foreground md:flex group-data-[collapsible=icon]:hidden">
        切换侧栏
        <kbd title="侧栏快捷键：Mac 按 ⌘B，其他平台按 Ctrl+B" class="rounded-sm border border-sidebar-border px-1.5 py-0.5">
          ⌘B / Ctrl+B
        </kbd>
      </p>
      <NavUser v-if="session.user" :user="session.user" />
    </UiSidebarFooter>
    <UiSidebarRail />
  </UiSidebar>
</template>
