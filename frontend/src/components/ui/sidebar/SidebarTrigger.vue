<script setup lang="ts">
import type { HTMLAttributes } from 'vue'

import { RiSideBarLine } from '@remixicon/vue'
import { cn } from 'cn'
import { Button } from '@/components/ui/button'
import { SheetTrigger } from '@/components/ui/sheet'
import { useSidebar } from './utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()

const { isMobile, open, toggleSidebar } = useSidebar()
</script>

<template>
  <Button
    :as="isMobile ? SheetTrigger : 'button'"
    data-sidebar="trigger"
    data-slot="sidebar-trigger"
    variant="ghost"
    size="icon-sm"
    :class="cn('', props.class)"
    v-bind="isMobile ? {} : { 'aria-expanded': open }"
    @click="!isMobile && toggleSidebar()"
  >
    <RiSideBarLine class="cn-rtl-flip" />
    <span class="sr-only">切换侧栏</span>
  </Button>
</template>
