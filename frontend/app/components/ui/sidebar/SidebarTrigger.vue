<script setup lang="ts">
import type { HTMLAttributes } from 'vue'

import { HugeiconsIcon } from '@hugeicons/vue'
import { LayoutLeftIcon } from '@hugeicons/core-free-icons'
import { cn } from 'cn'
import { Button } from '@/components/ui/button'
import { useSidebar } from './utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()

const { isMobile, open, openMobile, triggerId, toggleSidebar } = useSidebar()
</script>

<template>
  <Button
    :id="triggerId"
    type="button"
    data-sidebar="trigger"
    data-slot="sidebar-trigger"
    variant="ghost"
    size="icon-lg"
    :aria-expanded="isMobile ? openMobile : open"
    aria-keyshortcuts="Control+B Meta+B"
    :class="cn('', props.class)"
    @click="toggleSidebar"
  >
    <HugeiconsIcon :icon="LayoutLeftIcon" class="cn-rtl-flip" aria-hidden="true" />
    <span class="sr-only">切换侧栏</span>
  </Button>
</template>
