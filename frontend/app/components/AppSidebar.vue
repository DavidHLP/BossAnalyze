<script setup lang="ts">
import { ChartHistogramIcon, Home01Icon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/vue'
import type { SidebarProps } from '@/components/ui/sidebar'
import { useSidebar } from '@/components/ui/sidebar'
import { navigationMenus } from '@/lib/session'

const props = withDefaults(defineProps<SidebarProps>(), { collapsible: 'icon' })
const { state: session } = useSession()
const route = useRoute()
const { setOpenMobile } = useSidebar()
const workspace = [
  { path: '/console', title: '工作台', icon: Home01Icon },
  { path: '/recruitment', title: '招聘数据分析', icon: ChartHistogramIcon },
]
const menus = computed(() => navigationMenus(session.value.menus))
</script>

<template>
  <UiSidebar v-bind="props">
    <UiSidebarHeader class="h-14 justify-center border-b border-sidebar-border pr-14 md:pr-2">
      <UiSidebarMenu>
        <UiSidebarMenuItem>
          <UiSidebarMenuButton as-child size="lg" class="h-10 text-sm" tooltip="BossAnalyze">
            <NuxtLink to="/console" @click="setOpenMobile(false)">
              <AppBrand />
            </NuxtLink>
          </UiSidebarMenuButton>
        </UiSidebarMenuItem>
      </UiSidebarMenu>
    </UiSidebarHeader>
    <UiSidebarContent aria-label="控制台导航" class="py-2">
      <UiSidebarGroup role="group" aria-labelledby="workspace-label">
        <UiSidebarGroupLabel id="workspace-label" class="font-medium text-muted-foreground">
          工作空间
        </UiSidebarGroupLabel>
        <UiSidebarMenu class="gap-1">
          <UiSidebarMenuItem v-for="item in workspace" :key="item.path">
            <UiSidebarMenuButton
              as-child
              :tooltip="item.title"
              :is-active="route.path === item.path"
              class="h-9 text-sm"
            >
              <NuxtLink
                :to="item.path"
                :aria-current="route.path === item.path ? 'page' : undefined"
                @click="setOpenMobile(false)"
              >
                <HugeiconsIcon :icon="item.icon" aria-hidden="true" />
                <span class="group-data-[collapsible=icon]:sr-only">{{ item.title }}</span>
              </NuxtLink>
            </UiSidebarMenuButton>
          </UiSidebarMenuItem>
        </UiSidebarMenu>
      </UiSidebarGroup>
      <NavMain :items="menus" />
    </UiSidebarContent>
    <UiSidebarFooter class="border-t border-sidebar-border">
      <div class="flex items-center justify-between gap-2">
        <ThemeToggle class="hover:bg-sidebar-accent hover:text-sidebar-accent-foreground" />
        <kbd
          class="hidden rounded-sm border border-sidebar-border px-1.5 py-0.5 text-[0.625rem] text-muted-foreground md:inline-block group-data-[collapsible=icon]:hidden"
        >
          ⌘B / Ctrl+B
        </kbd>
      </div>
      <NavUser v-if="session.user" :user="session.user" />
    </UiSidebarFooter>
    <UiSidebarRail />
  </UiSidebar>
</template>
