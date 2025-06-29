<script setup lang="ts">
import { Codemirror } from 'vue-codemirror'
import { markdownLanguage } from '@codemirror/lang-markdown'
import { oneDark } from '@codemirror/theme-one-dark'
import { useResumeType } from '../../../hook'
import useEditorStore from '@/store/modules/editor'
import { watch, ref, onMounted, nextTick } from 'vue'

import { useThemeConfig } from '@/common/global'
import MarkdownToolbar from '../toolbar/mdTool.vue'
import './md-editor.scss'

defineProps<{ left: number }>()
const { isDark } = useThemeConfig()
const { resumeType } = useResumeType()
const editorStore = useEditorStore()

// 组件挂载时初始化编辑器内容
onMounted(async () => {
  // 等待DOM完全渲染
  await nextTick()

  // 如果内容为空，则初始化默认内容
  if (!editorStore.MDContent || editorStore.MDContent.trim() === '') {
    editorStore.initMDContent(resumeType.value)
  }
})

// 监听简历类型变化，重新初始化内容
watch(() => resumeType.value, (newType) => {
  editorStore.initMDContent(newType)
})
</script>

<template>
  <div class="md-editor-container">
    <markdown-toolbar />
    <codemirror
      v-model="editorStore.MDContent"
      :style="{
        height: 'calc(100vh - 40px)',
        borderLeft: isDark ? 'none' : '1px solid var(--border-light)',
        borderBottomLeftRadius: 'var(--radius-lg)',
        borderBottomRightRadius: 'var(--radius-lg)',
        minWidth: '550px',
        width: `${left}px`,
        background: 'var(--background-card)',
        fontFamily: 'var(--font-family-mono)',
        fontSize: 'var(--font-size-md)',
        lineHeight: 'var(--line-height-relaxed)'
      }"
      :autofocus="true"
      :indent-with-tab="true"
      :extensions="isDark ? [markdownLanguage, oneDark] : [markdownLanguage]"
      @change="(nv: string) => editorStore.setMDContent(nv, resumeType)"
    />
  </div>
</template>

<style lang="scss" scoped>
@use './md-editor.scss';
</style>
