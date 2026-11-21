<script setup lang="ts">
import { RiExpandUpDownLine } from '@remixicon/vue'
import { computed } from 'vue'
import UiComboboxTrigger from '@/components/ui/combobox/ComboboxTrigger.vue'

const props = defineProps<{
  id: string
  modelValue: string
  options: Array<{ value: string; label: string; disabled?: boolean }>
  searchLabel: string
  disabled?: boolean
  describedBy?: string
}>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const selectedLabel = computed(() => props.options.find((option) => option.value === props.modelValue)?.label)

function select(value: unknown) {
  if (typeof value === 'string') emit('update:modelValue', value)
}

function deferComposingInput(event: Event) {
  // Android can report Latin preedit text before the Chinese candidate is committed.
  if (event instanceof InputEvent && event.isComposing) event.stopImmediatePropagation()
}
</script>

<template>
  <UiCombobox :model-value="modelValue" :disabled="disabled" @update:model-value="select">
    <UiComboboxAnchor class="min-w-0">
      <UiComboboxTrigger
        as-child
        :id="id"
        :aria-labelledby="`${id}-label`"
        :aria-describedby="`${id}-value ${describedBy ?? ''}`"
      >
        <UiButton type="button" variant="outline" class="w-full justify-between bg-background px-2.5 font-normal">
          <span :id="`${id}-value`" class="truncate">{{ selectedLabel }}</span>
          <RiExpandUpDownLine class="text-muted-foreground" aria-hidden="true" />
        </UiButton>
      </UiComboboxTrigger>
    </UiComboboxAnchor>
    <UiComboboxList>
      <UiComboboxInput
        :aria-label="searchLabel"
        :placeholder="searchLabel"
        :display-value="() => ''"
        class="min-w-0"
        @input.capture="deferComposingInput"
      />
      <UiComboboxViewport>
        <UiComboboxEmpty>没有匹配选项</UiComboboxEmpty>
        <UiComboboxItem
          v-for="option in options"
          :key="option.value"
          :value="option.value"
          :text-value="option.label"
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
