<script setup lang="ts">
import type { MenuItem } from '@/lib/session'

defineProps<{ items: MenuItem[] }>()
</script>

<template>
  <UiSidebarGroup v-if="items.length" role="group" aria-labelledby="system-menu-label">
    <UiSidebarGroupLabel id="system-menu-label" class="font-medium text-muted-foreground">
      系统管理
    </UiSidebarGroupLabel>
    <UiSidebarMenu class="gap-1">
      <NavMenuItem v-for="item in items" :key="item.path ?? item.title" :item="item" />
    </UiSidebarMenu>
  </UiSidebarGroup>
</template>
