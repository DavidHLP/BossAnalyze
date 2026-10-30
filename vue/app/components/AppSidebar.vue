<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { Briefcase01Icon, Home01Icon } from '@hugeicons/core-free-icons'
import type { SidebarProps } from '@/components/ui/sidebar'
import { useSidebar } from '@/components/ui/sidebar'

const props = withDefaults(defineProps<SidebarProps>(), { collapsible: 'icon' })
const { state: session } = useSession()
const route = useRoute()
const { setOpenMobile } = useSidebar()
</script>

<template>
  <UiSidebar v-bind="props">
    <UiSidebarHeader>
      <UiSidebarMenu>
        <UiSidebarMenuItem>
          <UiSidebarMenuButton as-child size="lg" tooltip="BossAnalyze">
            <NuxtLink to="/console" @click="setOpenMobile(false)">
              <span
                class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground"
              >
                <HugeiconsIcon :icon="Briefcase01Icon" class="size-5" aria-hidden="true" />
              </span>
              <span class="grid text-left"
                ><span class="font-semibold">BossAnalyze</span>
                <span class="text-xs text-muted-foreground">招聘数据分析工作台</span>
              </span>
            </NuxtLink>
          </UiSidebarMenuButton>
        </UiSidebarMenuItem>
      </UiSidebarMenu>
    </UiSidebarHeader>
    <UiSidebarContent>
      <UiSidebarGroup>
        <UiSidebarMenu>
          <UiSidebarMenuItem>
            <UiSidebarMenuButton as-child tooltip="工作台" :is-active="route.path === '/console'">
              <NuxtLink to="/console" @click="setOpenMobile(false)">
                <HugeiconsIcon :icon="Home01Icon" aria-hidden="true" /><span>工作台</span>
              </NuxtLink>
            </UiSidebarMenuButton>
          </UiSidebarMenuItem>
        </UiSidebarMenu>
      </UiSidebarGroup>
      <NavMain :items="session.menus" />
    </UiSidebarContent>
    <UiSidebarFooter><NavUser v-if="session.user" :user="session.user" /></UiSidebarFooter>
    <UiSidebarRail />
  </UiSidebar>
</template>
