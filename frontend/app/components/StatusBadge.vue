<script setup lang="ts">
const tones = {
  success: 'bg-status-success-surface text-status-success-foreground',
  warning: 'bg-status-warning-surface text-status-warning-foreground',
  danger: 'bg-status-danger-surface text-status-danger-foreground',
  neutral: 'bg-status-neutral-surface text-status-neutral-foreground',
} as const

withDefaults(defineProps<{ tone?: keyof typeof tones }>(), { tone: 'neutral' })
</script>

<template>
  <span
    :class="tones[tone]"
    class="inline-flex w-fit items-center gap-1.5 rounded-md px-2 py-0.5 text-xs font-medium"
  >
    <span aria-hidden="true" class="size-1.5 shrink-0 rounded-full bg-current" />
    <slot />
  </span>
</template>
