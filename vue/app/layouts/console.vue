<script setup lang="ts">
import { findMenu } from '@/lib/session'
const route = useRoute()
const { state: session } = useSession()
const title = computed(() => findMenu(session.value.menus, route.path)?.title ?? '工作台')
useHead({ title: () => `${title.value} · BossAnalyze` })
</script>

<template>
  <UiSidebarProvider>
    <AppSidebar />
    <UiSidebarInset>
      <header
        class="flex h-16 shrink-0 items-center gap-2 border-b px-4 transition-[height] group-has-data-[collapsible=icon]/sidebar-wrapper:h-12"
      >
        <UiSidebarTrigger class="-ml-1" />
        <UiSeparator orientation="vertical" class="mr-2 data-[orientation=vertical]:h-4" />
        <UiBreadcrumb>
          <UiBreadcrumbList>
            <UiBreadcrumbItem class="hidden md:block">
              <UiBreadcrumbLink as-child
                ><NuxtLink to="/console">控制台</NuxtLink></UiBreadcrumbLink
              >
            </UiBreadcrumbItem>
            <UiBreadcrumbSeparator class="hidden md:block" />
            <UiBreadcrumbItem
              ><UiBreadcrumbPage>{{ title }}</UiBreadcrumbPage></UiBreadcrumbItem
            >
          </UiBreadcrumbList>
        </UiBreadcrumb>
      </header>
      <main class="flex flex-1 flex-col gap-6 p-4 md:p-8"><slot /></main>
    </UiSidebarInset>
  </UiSidebarProvider>
</template>
