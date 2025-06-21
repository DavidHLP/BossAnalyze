<script setup lang="ts">
import { useToggleEditorMode } from './hook'
import { checkMouseSelect } from '../toolbar/hook'
import { useResumeType } from '../../../hook'
import RichToolbar from '../toolbar/richTool.vue'
import './writable.scss'

defineProps<{ left: number }>()

const { resumeType } = useResumeType()
const { DOMTree, ObserverContent, editorStore } = useToggleEditorMode(resumeType.value)
</script>

<template>
  <rich-toolbar
    @toggle-editor-mode="editorStore.setWritableMode"
    @content-change="ObserverContent"
  />
  <div
    ref="DOMTree"
    @click="checkMouseSelect"
    @input="ObserverContent"
    class="writable-edit-mode"
    contenteditable
    spellcheck="false"
    :style="{ height: 'calc(100vh - 40px)', width: `${left}px`, overflowY: 'scroll' }"
  ></div>
</template>

<style lang="scss" scoped>
@use '@/assets/style/variables.scss' as *;

.writable-edit-mode {
  padding: var(--spacing-xl);
  padding-top: 0;
  min-width: 600px;
  border-bottom-left-radius: var(--radius-lg);
  border-bottom-right-radius: var(--radius-lg);
  margin: 0 auto;
  background: var(--background-card);
  transition: all var(--transition-normal) var(--transition-timing);

  &:focus {
    outline: none;
    box-shadow: inset 0 0 0 2px rgba(59, 130, 246, 0.1);
  }

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: var(--background-secondary);
    border-radius: var(--radius-sm);
  }

  &::-webkit-scrollbar-thumb {
    background: var(--border-medium);
    border-radius: var(--radius-sm);

    &:hover {
      background: var(--border-dark);
    }
  }
}

// 响应式设计
@include responsive(md) {
  .writable-edit-mode {
    padding: var(--spacing-lg);
    padding-top: 0;
    min-width: 500px;
  }
}

@include responsive(sm) {
  .writable-edit-mode {
    padding: var(--spacing-md);
    padding-top: 0;
    min-width: 300px;
  }
}
</style>
