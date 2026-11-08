<script setup lang="ts">
defineProps<{
  id: string
  modelValue: string
  options: Array<{ value: string; label: string; disabled?: boolean }>
  disabled?: boolean
}>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
</script>
<template>
  <UiCombobox
    :model-value="modelValue"
    :disabled="disabled"
    @update:model-value="
      (value) => {
        if (typeof value === 'string') emit('update:modelValue', value)
      }
    "
  >
    <UiComboboxAnchor class="w-full"
      ><UiComboboxInput
        :id="id"
        :display-value="(value) => options.find((option) => option.value === value)?.label ?? ''"
        placeholder="搜索并选择"
    /></UiComboboxAnchor>
    <UiComboboxList
      ><UiComboboxViewport
        ><UiComboboxEmpty>没有匹配选项</UiComboboxEmpty
        ><UiComboboxItem
          v-for="option in options"
          :key="option.value"
          :value="option.value"
          :disabled="option.disabled"
          >{{ option.label
          }}<span v-if="option.disabled" class="ml-2 text-muted-foreground"
            >暂不支持</span
          ></UiComboboxItem
        ></UiComboboxViewport
      ></UiComboboxList
    >
  </UiCombobox>
</template>
