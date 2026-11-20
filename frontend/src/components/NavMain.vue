<script setup lang="ts">
import { useId } from 'vue'
import NavMenuItem from './NavMenuItem.vue'
import type { MenuItem } from '@/lib/session'

defineProps<{ label: string; items: MenuItem[] }>()
const labelId = useId()
</script>

<template>
  <UiSidebarGroup v-if="items.length" role="group" :aria-labelledby="labelId">
    <UiSidebarGroupLabel :id="labelId" class="font-medium text-muted-foreground">
      {{ label }}
    </UiSidebarGroupLabel>
    <UiSidebarMenu class="gap-1">
      <NavMenuItem v-for="item in items" :key="item.path ?? item.title" :item="item" />
    </UiSidebarMenu>
  </UiSidebarGroup>
</template>
