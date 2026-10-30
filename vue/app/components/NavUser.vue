<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { Logout01Icon, UnfoldMoreIcon } from '@hugeicons/core-free-icons'
import { useSidebar } from '@/components/ui/sidebar'

defineProps<{ user: { name: string; email: string } }>()
const { isMobile } = useSidebar()
const { signOut } = useSession()
</script>

<template>
  <UiSidebarMenu>
    <UiSidebarMenuItem>
      <UiDropdownMenu>
        <UiDropdownMenuTrigger as-child>
          <UiSidebarMenuButton
            size="lg"
            aria-label="账号菜单"
            class="data-[state=open]:bg-sidebar-accent"
          >
            <UiAvatar class="size-8 rounded-lg">
              <UiAvatarFallback class="rounded-lg">{{ user.name.slice(0, 1) }}</UiAvatarFallback>
            </UiAvatar>
            <span class="grid flex-1 text-left text-sm leading-tight">
              <span class="truncate font-medium">{{ user.name }}</span>
              <span class="truncate text-xs text-muted-foreground">{{ user.email }}</span>
            </span>
            <HugeiconsIcon :icon="UnfoldMoreIcon" class="ml-auto size-4" aria-hidden="true" />
          </UiSidebarMenuButton>
        </UiDropdownMenuTrigger>
        <UiDropdownMenuContent
          class="min-w-56 rounded-lg"
          :side="isMobile ? 'bottom' : 'right'"
          align="end"
          :side-offset="4"
        >
          <UiDropdownMenuLabel class="font-normal">
            <span class="block truncate font-medium">{{ user.name }}</span>
            <span class="block truncate text-xs text-muted-foreground">{{ user.email }}</span>
          </UiDropdownMenuLabel>
          <UiDropdownMenuSeparator />
          <UiDropdownMenuItem @select="signOut">
            <HugeiconsIcon :icon="Logout01Icon" aria-hidden="true" />退出登录
          </UiDropdownMenuItem>
        </UiDropdownMenuContent>
      </UiDropdownMenu>
    </UiSidebarMenuItem>
  </UiSidebarMenu>
</template>
