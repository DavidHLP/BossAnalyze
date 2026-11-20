<script setup lang="ts">
import { RiExpandUpDownLine } from '@remixicon/vue'

defineProps<{
  id: string
  modelValue: string
  options: Array<{ value: string; label: string; disabled?: boolean }>
  disabled?: boolean
  describedBy?: string
}>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

function select(value: unknown) {
  if (typeof value === 'string') emit('update:modelValue', value)
}
</script>

<template>
  <UiCombobox :model-value="modelValue" :disabled="disabled" open-on-click @update:model-value="select">
    <UiComboboxAnchor class="relative w-full min-w-0">
      <UiComboboxInput
        :id="id"
        :aria-describedby="describedBy"
        :display-value="(value) => options.find((option) => option.value === value)?.label ?? ''"
        class="min-w-0 pr-6"
        placeholder="搜索并选择"
      />
      <RiExpandUpDownLine class="pointer-events-none absolute top-1/2 right-2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
    </UiComboboxAnchor>
    <UiComboboxList>
      <UiComboboxViewport>
        <UiComboboxEmpty>没有匹配选项</UiComboboxEmpty>
        <UiComboboxItem
          v-for="option in options"
          :key="option.value"
          :value="option.value"
          :disabled="option.disabled"
          :aria-disabled="option.disabled || undefined"
        >
          <span class="min-w-0 [overflow-wrap:anywhere]">{{ option.label }}</span>
          <span v-if="option.disabled" class="ml-2 shrink-0 text-xs">暂不支持</span>
        </UiComboboxItem>
      </UiComboboxViewport>
    </UiComboboxList>
  </UiCombobox>
</template>
