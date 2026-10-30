<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { ArrowRight01Icon, File01Icon } from '@hugeicons/core-free-icons'
import { useSidebar } from '@/components/ui/sidebar'
import { findMenu } from '@/lib/session'
import type { MenuItem } from '@/lib/session'

const props = defineProps<{ item: MenuItem }>()
const route = useRoute()
const { state, setOpen, setOpenMobile } = useSidebar()
const active = computed(() => Boolean(findMenu([props.item], route.path)))
const open = ref(active.value)
watch(active, (value) => {
  if (value) open.value = true
})

async function revealGroup() {
  if (state.value === 'collapsed') {
    setOpen(true)
    await nextTick()
    open.value = true
  }
}
</script>

<template>
  <UiCollapsible v-if="item.children.length" v-model:open="open" as-child>
    <UiSidebarMenuItem>
      <UiCollapsibleTrigger as-child>
        <UiSidebarMenuButton
          :tooltip="item.title"
          :is-active="active"
          @click="revealGroup"
        >
          <HugeiconsIcon :icon="File01Icon" aria-hidden="true" /><span>{{ item.title }}</span>
          <HugeiconsIcon
            :icon="ArrowRight01Icon"
            class="ml-auto transition-transform"
            :class="{ 'rotate-90': open }"
            aria-hidden="true"
          />
        </UiSidebarMenuButton>
      </UiCollapsibleTrigger>
      <UiCollapsibleContent>
        <UiSidebarMenuSub>
          <NavMenuItem
            v-for="child in item.children"
            :key="child.path ?? child.title"
            :item="child"
          />
        </UiSidebarMenuSub>
      </UiCollapsibleContent>
    </UiSidebarMenuItem>
  </UiCollapsible>
  <UiSidebarMenuItem v-else-if="item.path">
    <UiSidebarMenuButton as-child :tooltip="item.title" :is-active="active">
      <NuxtLink
        :to="item.path"
        :aria-current="active ? 'page' : undefined"
        @click="setOpenMobile(false)"
      >
        <HugeiconsIcon :icon="File01Icon" aria-hidden="true" /><span>{{ item.title }}</span>
      </NuxtLink>
    </UiSidebarMenuButton>
  </UiSidebarMenuItem>
</template>
