<script setup lang="ts">
import { nextTick, ref, useTemplateRef } from 'vue'
import { ViewIcon, ViewOffIcon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/vue'

defineOptions({ inheritAttrs: false })
const props = defineProps<{ id: string; label?: string; disabled?: boolean; pending?: boolean }>()
const password = defineModel<string>({ required: true })
const visible = ref(false)
const field = useTemplateRef<HTMLDivElement>('field')

function keepInputFocus(event: PointerEvent) {
  if (document.activeElement === field.value?.querySelector('input')) event.preventDefault()
}

async function toggle() {
  const input = field.value?.querySelector('input')
  if (!input || props.disabled || props.pending) return
  const { selectionStart, selectionEnd, selectionDirection } = input
  visible.value = !visible.value
  await nextTick()
  if (selectionStart !== null && selectionEnd !== null) {
    input.setSelectionRange(selectionStart, selectionEnd, selectionDirection ?? undefined)
  }
}
</script>

<template>
  <div ref="field" class="relative">
    <UiInput
      v-bind="$attrs"
      :id="id"
      v-model="password"
      :type="visible ? 'text' : 'password'"
      class="pr-10"
      :disabled="disabled"
      :readonly="pending"
      :aria-disabled="pending || undefined"
    />
    <UiButton
      type="button"
      variant="ghost"
      size="icon"
      class="absolute top-1/2 right-1 -translate-y-1/2 text-muted-foreground aria-disabled:opacity-50"
      :aria-label="`${visible ? '隐藏' : '显示'}${label ?? '密码'}`"
      :aria-controls="id"
      :disabled="disabled"
      :aria-disabled="pending || undefined"
      @pointerdown="keepInputFocus"
      @click="toggle"
    >
      <HugeiconsIcon :icon="visible ? ViewOffIcon : ViewIcon" aria-hidden="true" />
    </UiButton>
  </div>
</template>
