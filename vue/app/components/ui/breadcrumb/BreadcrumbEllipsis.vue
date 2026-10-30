<script lang="ts" setup>
import type { HTMLAttributes } from 'vue'

import { HugeiconsIcon } from '@hugeicons/vue'
import { MoreHorizontalCircle01Icon } from '@hugeicons/core-free-icons'
import { cn } from 'cn'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <span
    data-slot="breadcrumb-ellipsis"
    role="presentation"
    aria-hidden="true"
    :class="cn('size-4 [&>svg]:size-3.5 flex items-center justify-center', props.class)"
  >
    <slot>
      <HugeiconsIcon :icon="MoreHorizontalCircle01Icon" />
    </slot>
    <span class="sr-only">More</span>
  </span>
</template>
