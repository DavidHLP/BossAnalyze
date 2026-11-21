<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { HugeiconsIcon } from '@hugeicons/vue'
import { ArrowRight01Icon } from '@hugeicons/core-free-icons'
import { useSidebar } from '@/components/ui/sidebar'
import { findMenu } from '@/lib/session'
import { menuIcon } from '@/lib/menu-icons'
import type { MenuItem } from '@/lib/session'

const props = defineProps<{ item: MenuItem }>()
const icon = computed(() => menuIcon(props.item.path, props.item.icon))
const route = useRoute()
const { state, isMobile, setOpen, setOpenMobile } = useSidebar()
const collapsed = computed(() => state.value === 'collapsed' && !isMobile.value)
const current = computed(() => props.item.path === route.path)
const selected = computed(() => current.value || (props.item.path === '/resumes' && route.path.startsWith('/resumes/')))
const active = computed(() => Boolean(findMenu([props.item], route.path)))
const title = computed(() => props.item.title.trim() || '未命名功能')
const open = ref(active.value)
watch([active, () => route.path], ([value]) => {
  if (value) open.value = true
})

async function revealGroup() {
  if (collapsed.value) {
    setOpen(true)
    await nextTick()
    open.value = true
  }
}
</script>

<template>
  <UiCollapsible v-if="item.children.length" v-model:open="open" as-child>
    <UiSidebarMenuItem>
      <div class="flex items-center gap-1">
        <UiSidebarMenuButton
          v-if="item.path"
          as-child
          :is-active="current"
          class="h-9 min-w-0 text-sm group-data-[collapsible=icon]:hidden"
        >
          <RouterLink
            :to="item.path"
            :aria-current="current ? 'page' : undefined"
            @click="setOpenMobile(false)"
          >
            <HugeiconsIcon :icon="icon" aria-hidden="true" />
            <span>{{ title }}</span>
          </RouterLink>
        </UiSidebarMenuButton>
        <UiCollapsibleTrigger as-child>
          <UiSidebarMenuButton
            type="button"
            :tooltip="title"
            :aria-label="item.path ? `${collapsed || !open ? '展开' : '折叠'}${title}子菜单` : undefined"
            :is-active="active && (collapsed || !item.path)"
            class="h-9 text-sm"
            :class="item.path && 'w-8 shrink-0 justify-center'"
            @click="revealGroup"
          >
            <template v-if="collapsed || !item.path">
              <HugeiconsIcon :icon="icon" aria-hidden="true" />
              <span class="min-w-0 truncate group-data-[collapsible=icon]:sr-only">{{ title }}</span>
            </template>
            <HugeiconsIcon
              :icon="ArrowRight01Icon"
              class="transition-transform group-data-[collapsible=icon]:hidden"
              :class="{ 'ml-auto': !item.path, 'rotate-90': open }"
              aria-hidden="true"
            />
          </UiSidebarMenuButton>
        </UiCollapsibleTrigger>
      </div>
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
    <UiSidebarMenuButton as-child :tooltip="title" :is-active="selected" class="h-9 text-sm">
      <RouterLink
        :to="item.path"
        :aria-current="current ? 'page' : undefined"
        @click="setOpenMobile(false)"
      >
        <HugeiconsIcon :icon="icon" aria-hidden="true" />
        <span class="group-data-[collapsible=icon]:sr-only">{{ title }}</span>
      </RouterLink>
    </UiSidebarMenuButton>
  </UiSidebarMenuItem>
</template>
