<script setup lang="ts">
import { ChartHistogramIcon, Home01Icon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/vue'
import { findMenu } from '@/lib/session'

const route = useRoute()
const { state: session } = useSession()
const isRecruitment = computed(() => route.path === '/recruitment')
const title = computed(() =>
  isRecruitment.value
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
    <UiSidebarInset class="min-w-0">
      <header class="flex h-14 shrink-0 items-center gap-2 border-b px-4 sm:gap-3 md:px-6">
        <UiSidebarTrigger class="-ml-1" />
        <UiSeparator orientation="vertical" class="mr-2 data-[orientation=vertical]:h-4" />
        <UiBreadcrumb class="min-w-0 flex-1">
          <UiBreadcrumbList class="flex-nowrap text-sm">
            <UiBreadcrumbItem v-if="!isRecruitment" class="hidden shrink-0 md:block">
              <UiBreadcrumbLink as-child>
                <NuxtLink to="/console">控制台</NuxtLink>
              </UiBreadcrumbLink>
            </UiBreadcrumbItem>
            <UiBreadcrumbSeparator v-if="!isRecruitment" class="hidden shrink-0 md:block" />
            <UiBreadcrumbItem class="min-w-0">
              <UiBreadcrumbPage class="truncate font-medium">{{ title }}</UiBreadcrumbPage>
            </UiBreadcrumbItem>
          </UiBreadcrumbList>
        </UiBreadcrumb>
        <UiButton as-child variant="ghost" class="h-8 gap-2 px-2 text-muted-foreground">
          <NuxtLink :to="isRecruitment ? '/console' : '/recruitment'">
            <HugeiconsIcon :icon="isRecruitment ? Home01Icon : ChartHistogramIcon" aria-hidden="true" />
            <span class="sr-only sm:not-sr-only">{{ isRecruitment ? '工作台' : '招聘数据分析' }}</span>
          </NuxtLink>
        </UiButton>
      </header>
      <main
        id="main-content"
        ref="main"
        tabindex="-1"
        aria-label="主内容"
        class="flex flex-1 flex-col gap-6 p-4 md:p-8"
      >
        <slot />
      </main>
    </UiSidebarInset>
  </UiSidebarProvider>
</template>
