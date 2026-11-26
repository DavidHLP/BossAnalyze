<script setup lang="ts">
import { computed } from 'vue'
import { useSession } from '@/composables/useSession'
import { HugeiconsIcon } from '@hugeicons/vue'
import { Logout01Icon, Settings02Icon, UnfoldMoreIcon, UserCircleIcon } from '@hugeicons/core-free-icons'
import { useSidebar } from '@/components/ui/sidebar'
import { findMenu } from '@/lib/session'

const props = defineProps<{ user: { name: string; email: string } }>()
const { isMobile, setOpenMobile } = useSidebar()
const { state: session, crossTabChanged, capturePage, signOut } = useSession()
const initial = computed(() => Array.from(props.user.name.trim() || props.user.email)[0])
const links = computed(() => [
  { path: '/console/profile', label: '个人中心', icon: UserCircleIcon },
  { path: '/console/settings', label: '系统设置', icon: Settings02Icon },
].filter((link) => findMenu(session.value.menus, link.path)))
</script>

<template>
  <UiSidebarMenu>
    <UiSidebarMenuItem>
      <UiDropdownMenu>
        <UiDropdownMenuTrigger as-child>
          <UiSidebarMenuButton
            size="lg"
            aria-label="账号菜单"
            class="gap-2.5 data-[state=open]:bg-sidebar-accent"
          >
            <UiAvatar class="size-8 rounded-lg">
              <UiAvatarFallback class="rounded-lg bg-accent-data-surface text-accent-data-foreground">
                {{ initial }}
              </UiAvatarFallback>
            </UiAvatar>
            <span class="grid min-w-0 flex-1 text-left text-sm leading-normal group-data-[collapsible=icon]:hidden">
              <span class="truncate font-medium">{{ user.name }}</span>
              <span class="truncate text-xs text-muted-foreground">{{ user.email }}</span>
            </span>
            <HugeiconsIcon
              :icon="UnfoldMoreIcon"
              class="ml-auto size-4 group-data-[collapsible=icon]:hidden"
              aria-hidden="true"
            />
          </UiSidebarMenuButton>
        </UiDropdownMenuTrigger>
        <UiDropdownMenuContent
          class="min-w-56 rounded-lg border shadow-panel"
          :side="isMobile ? 'bottom' : 'right'"
          align="end"
          :side-offset="4"
        >
          <UiDropdownMenuLabel class="font-normal">
            <span class="block truncate font-medium">{{ user.name }}</span>
            <span class="block truncate text-xs text-muted-foreground">{{ user.email }}</span>
          </UiDropdownMenuLabel>
          <UiDropdownMenuSeparator />
          <UiDropdownMenuItem v-for="link in links" :key="link.path" as-child>
            <RouterLink :to="link.path" @click="setOpenMobile(false)">
              <HugeiconsIcon :icon="link.icon" aria-hidden="true" />
              {{ link.label }}
            </RouterLink>
          </UiDropdownMenuItem>
          <UiDropdownMenuSeparator v-if="links.length" />
          <UiDropdownMenuItem
            :disabled="crossTabChanged !== null"
            @select="signOut(false, null, undefined, capturePage())"
          >
            <HugeiconsIcon :icon="Logout01Icon" aria-hidden="true" />退出登录
          </UiDropdownMenuItem>
        </UiDropdownMenuContent>
      </UiDropdownMenu>
    </UiSidebarMenuItem>
  </UiSidebarMenu>
</template>
