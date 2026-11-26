<script setup lang="ts">
import { computed, nextTick, provide, ref, watch } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { useSession } from '@/composables/useSession'
import AppSidebar from '@/components/AppSidebar.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { isResumePath, type MenuItem } from '@/lib/session'

const route = useRoute()
const { state: session, crossTabChanged } = useSession()

function reloadPage() {
  window.location.reload()
}

function menuTrail(items: MenuItem[], path: string): MenuItem[] | undefined {
  for (const item of items) {
    if (item.path === path) return [item]
    const descendants = menuTrail(item.children, path)
    if (descendants) return [item, ...descendants]
  }
}

const trail = computed(() => menuTrail(session.value.menus, route.path) ?? [])
provide('consoleMenuTrail', trail)

const breadcrumbs = computed(() => {
  if (route.path === '/console') return [{ title: '工作台', path: '/console' }]
  if (route.path === '/recruitment') return [{ title: '招聘数据分析', path: '/recruitment' }]
  if (isResumePath(route.path)) {
    const root = { title: '我的简历', path: '/resumes' }
    return route.path === root.path ? [root] : [root, { title: route.meta.title ?? '简历', path: route.path }]
  }
  if (route.path === '/console/logs') return [
    { title: '数据看板', path: '/console/dashboard' },
    { title: '扩展日志分析', path: route.path },
  ]
  if (!trail.value.length) return [{ title: '未命名功能', path: route.path }]
  return trail.value.map(({ title, path }) => ({ title: title.trim() || '未命名功能', path }))
})

const main = ref<HTMLElement | null>(null)
let sidebarPath = ''
function restoreNavigationFocus(event: Event) {
  if (route.path === sidebarPath) return
  event.preventDefault()
  main.value?.focus({ preventScroll: true })
}

watch(() => route.path, async () => {
  await nextTick()
  const anchor = route.hash && document.getElementById(route.hash.slice(1))
  if (anchor && anchor.contains(document.activeElement)) return
  main.value?.focus({ preventScroll: true })
}, { immediate: true })
</script>

<template>
  <UiSidebarProvider>
    <a
      href="#main-content"
      class="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-2 focus:z-50 focus:rounded focus:bg-background focus:p-3"
      @click.prevent="main?.focus()"
    >跳过导航</a>
    <AppSidebar @open-auto-focus="sidebarPath = route.path" @close-auto-focus="restoreNavigationFocus" />
    <UiSidebarInset class="min-w-0 max-md:pt-(--console-header-height)">
      <header class="console-header flex h-(--console-header-height) shrink-0 items-center gap-2 border-b px-4 sm:gap-3 md:px-6">
        <UiSidebarTrigger class="-ml-1" title="切换侧栏（⌘B / Ctrl+B）" />
        <UiSeparator orientation="vertical" class="mr-2 data-[orientation=vertical]:h-4" />
        <UiBreadcrumb class="min-w-0 flex-1">
          <UiBreadcrumbList class="flex-nowrap text-sm">
            <template v-for="(item, index) in breadcrumbs" :key="index">
              <UiBreadcrumbItem v-if="index === breadcrumbs.length - 1" class="min-w-0">
                <UiBreadcrumbPage class="truncate font-medium">{{ item.title }}</UiBreadcrumbPage>
              </UiBreadcrumbItem>
              <template v-else>
                <UiBreadcrumbItem class="hidden min-w-0 md:inline-flex">
                  <UiBreadcrumbLink v-if="item.path" as-child class="truncate">
                    <RouterLink :to="item.path">{{ item.title }}</RouterLink>
                  </UiBreadcrumbLink>
                  <span v-else class="truncate">{{ item.title }}</span>
                </UiBreadcrumbItem>
                <UiBreadcrumbSeparator class="hidden shrink-0 md:block" />
              </template>
            </template>
          </UiBreadcrumbList>
        </UiBreadcrumb>
        <ThemeToggle />
      </header>
      <main
        id="main-content"
        ref="main"
        tabindex="-1"
        aria-label="主内容"
        class="flex flex-1 flex-col gap-6 p-4 md:p-8"
      >
        <div
          v-if="crossTabChanged"
          class="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-status-warning-surface p-4 text-sm text-status-warning-foreground"
        >
          <p role="alert" class="min-w-0 flex-1 basis-64">
            {{ crossTabChanged === 'cleared' ? '其他标签页已清除登录凭据。' : '其他标签页已变更登录状态。' }}
            已锁定提交，当前草稿仍保留。请先留存草稿，再刷新页面。
          </p>
          <UiButton type="button" variant="outline" size="sm" @click="reloadPage">立即刷新页面</UiButton>
        </div>
        <RouterView v-slot="{ Component }">
          <component :is="Component" :key="isResumePath(route.path) ? route.path : undefined" />
        </RouterView>
      </main>
    </UiSidebarInset>
  </UiSidebarProvider>
</template>

<style>
:root:has(.console-header) {
  --console-header-height: calc(var(--spacing) * 14);
}

@media (width < 48rem) {
  :root:has(.console-header) {
    scroll-padding-top: var(--console-header-height);
  }

  :root[data-console-history-scroll] {
    overflow-anchor: none;
  }

  .console-header {
    /* Keep the trigger above the page even while the drawer locks body scrolling. */
    position: fixed;
    top: 0;
    inset-inline: 0;
    z-index: 20;
    background-color: var(--background);
  }
}
</style>
