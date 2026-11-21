<script setup lang="ts">
import { computed } from 'vue'
import type { UserRole } from '@/lib/users'
const props = defineProps<{
  modelValue: number | null
  roles: UserRole[]
  label: string
  id?: string
  allowAll?: boolean
  disabled?: boolean
  required?: boolean
  invalid?: boolean
  describedBy?: string
}>()
const emit = defineEmits<{ 'update:modelValue': [value: number | null] }>()
const options = computed(() => [
  ...(props.allowAll ? [{ value: 'all', label: '全部角色', inactive: false }] : []),
  ...props.roles.map((role) => ({
    value: String(role.id),
    label: role.roleName,
    inactive: role.status === 0,
  })),
])
const optionLabel = (option: (typeof options.value)[number]) =>
  option.label + (option.inactive ? '（停用）' : '')
const value = computed(() => (props.modelValue === null ? 'all' : String(props.modelValue)))
function choose(value: unknown) {
  if (typeof value === 'string') emit('update:modelValue', value === 'all' ? null : Number(value))
}
</script>

<template>
  <UiRadioGroup
    v-if="options.length <= 5"
    :id="id"
    :model-value="value"
    :disabled="disabled"
    :aria-label="label"
    :aria-required="required"
    :aria-invalid="invalid"
    :aria-describedby="describedBy"
    class="flex flex-wrap gap-x-4 gap-y-3"
    @update:model-value="choose"
  >
    <label
      v-for="option in options"
      :key="option.value"
      class="flex min-w-0 cursor-pointer items-center gap-2 break-all text-sm"
    >
      <UiRadioGroupItem :value="option.value" class="shrink-0" />
      <span>{{ option.label }}</span>
      <StatusBadge v-if="option.inactive" tone="warning" class="shrink-0">停用</StatusBadge>
    </label>
  </UiRadioGroup>
  <UiCombobox
    v-else
    :model-value="value"
    :disabled="disabled"
    open-on-click
    @update:model-value="choose"
  >
    <UiComboboxAnchor :id="id" class="w-full"
      ><UiComboboxInput
        :aria-label="label"
        :aria-required="required"
        :aria-invalid="invalid"
        :aria-describedby="describedBy"
        :display-value="(v) => {
          const option = options.find((item) => item.value === v)
          return option ? optionLabel(option) : ''
        }"
        placeholder="搜索并选择角色"
    /></UiComboboxAnchor>
    <UiComboboxList
      ><UiComboboxViewport
        ><UiComboboxEmpty>未找到角色</UiComboboxEmpty
        ><UiComboboxItem
          v-for="option in options"
          :key="option.value"
          :value="option.value"
          :text-value="optionLabel(option)"
        >
          <span class="min-w-0 break-all">{{ option.label }}</span>
          <StatusBadge v-if="option.inactive" tone="warning" class="shrink-0">停用</StatusBadge>
        </UiComboboxItem></UiComboboxViewport
      ></UiComboboxList
    >
  </UiCombobox>
</template>
