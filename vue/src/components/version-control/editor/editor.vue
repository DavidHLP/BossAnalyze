<template>
  <div class="editor-wrapper">
    <textarea
      :value="modelValue"
      @input="onInput"
      class="editor-textarea"
      placeholder="在这里开始您的创作..."
    ></textarea>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const onInput = (event: Event) => {
  const target = event.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
}
</script>

<style scoped lang="scss">
.editor-wrapper {
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 8px;
}

.editor-textarea {
  width: 100%;
  height: 480px;
  border: none;
  resize: none;
  outline: none;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
}
</style>
