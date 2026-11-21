<script setup lang="ts">
import { computed } from 'vue'
import { useTheme } from '@/composables/useTheme'
import { Moon02Icon, Sun01Icon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/vue'

const { theme, toggleTheme } = useTheme()
const label = computed(() => theme.value === 'dark' ? '切换到浅色主题' : '切换到深色主题')
</script>

<template>
  <UiButton
    type="button"
    variant="ghost"
    size="icon-lg"
    :aria-label="label"
    :title="label"
    @click="toggleTheme"
  >
    <HugeiconsIcon :icon="Sun01Icon" class="hidden dark:block" aria-hidden="true" />
    <HugeiconsIcon :icon="Moon02Icon" class="dark:hidden" aria-hidden="true" />
  </UiButton>
</template>
