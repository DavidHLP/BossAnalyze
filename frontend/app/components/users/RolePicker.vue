<script setup lang="ts">
import type { UserRole } from '@/lib/users'
const props = defineProps<{
  modelValue: number | null
  roles: UserRole[]
  label: string
  allowAll?: boolean
  disabled?: boolean
}>()
const emit = defineEmits<{ 'update:modelValue': [value: number | null] }>()
const options = computed(() => [
  ...(props.allowAll ? [{ value: 'all', label: '全部角色' }] : []),
  ...props.roles.map((role) => ({
    value: String(role.id),
    label: role.roleName + (role.status === 0 ? '（停用）' : ''),
  })),
])
const value = computed(() => (props.modelValue === null ? 'all' : String(props.modelValue)))
function choose(value: unknown) {
  if (typeof value === 'string') emit('update:modelValue', value === 'all' ? null : Number(value))
}
</script>

<template>
  <UiRadioGroup
    v-if="options.length <= 5"
    :model-value="value"
    :disabled="disabled"
    :aria-label="label"
    class="flex flex-wrap gap-x-4 gap-y-3"
    @update:model-value="choose"
  >
    <label
      v-for="option in options"
      :key="option.value"
      class="flex cursor-pointer items-center gap-2 text-sm"
    >
      <UiRadioGroupItem :value="option.value" />{{ option.label }}
    </label>
  </UiRadioGroup>
  <UiCombobox v-else :model-value="value" :disabled="disabled" @update:model-value="choose">
    <UiComboboxAnchor class="w-full"
      ><UiComboboxInput
        :aria-label="label"
        :display-value="(v) => options.find((option) => option.value === v)?.label ?? ''"
        placeholder="搜索并选择角色"
    /></UiComboboxAnchor>
    <UiComboboxList
      ><UiComboboxViewport
        ><UiComboboxEmpty>未找到角色</UiComboboxEmpty
        ><UiComboboxItem v-for="option in options" :key="option.value" :value="option.value">{{
          option.label
        }}</UiComboboxItem></UiComboboxViewport
      ></UiComboboxList
    >
  </UiCombobox>
</template>
