<script setup lang="ts">
import type { MenuItem } from '@/lib/session'
defineProps<{ items: MenuItem[] }>()
</script>

<template>
  <UiSidebarGroup v-if="items.length">
    <UiSidebarGroupLabel>功能导航</UiSidebarGroupLabel>
    <UiSidebarMenu
      ><NavMenuItem v-for="item in items" :key="item.path ?? item.title" :item="item"
    /></UiSidebarMenu>
  </UiSidebarGroup>
</template>
