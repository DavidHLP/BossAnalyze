<script setup lang="ts">
import { findMenu } from '@/lib/session'
const route = useRoute()
const { state: session } = useSession()
const title = computed(() =>
  route.path === '/recruitment'
    ? '招聘数据分析'
    : route.path === '/console'
      ? '工作台'
      : (findMenu(session.value.menus, route.path)?.title ?? '工作台'),
)
const main = ref<HTMLElement | null>(null)
const nuxtApp = useNuxtApp()
const removeFocusHook = nuxtApp.hook('page:finish', () => {
  main.value?.focus()
})
onUnmounted(removeFocusHook)
useHead({ title: () => `${title.value} · BossAnalyze` })
</script>

<template>
  <UiSidebarProvider>
    <a
      href="#main-content"
      class="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-2 focus:z-50 focus:rounded focus:bg-background focus:p-3"
      @click.prevent="main?.focus()"
    >跳过导航</a>
    <AppSidebar />
    <UiSidebarInset>
      <header
        class="flex h-16 shrink-0 items-center gap-2 border-b px-4 transition-[height] group-has-data-[collapsible=icon]/sidebar-wrapper:h-12"
      >
        <UiSidebarTrigger class="-ml-1" />
        <UiSeparator orientation="vertical" class="mr-2 data-[orientation=vertical]:h-4" />
        <UiBreadcrumb>
          <UiBreadcrumbList>
            <UiBreadcrumbItem v-if="route.path !== '/recruitment'" class="hidden md:block">
              <UiBreadcrumbLink as-child
                ><NuxtLink to="/console">控制台</NuxtLink></UiBreadcrumbLink
              >
            </UiBreadcrumbItem>
            <UiBreadcrumbSeparator v-if="route.path !== '/recruitment'" class="hidden md:block" />
            <UiBreadcrumbItem
              ><UiBreadcrumbPage>{{ title }}</UiBreadcrumbPage></UiBreadcrumbItem
            >
          </UiBreadcrumbList>
        </UiBreadcrumb>
      </header>
      <main
        id="main-content"
        ref="main"
        tabindex="-1"
        aria-label="主内容"
        class="flex flex-1 flex-col gap-6 p-4 md:p-8"
      ><slot /></main>
    </UiSidebarInset>
  </UiSidebarProvider>
</template>
