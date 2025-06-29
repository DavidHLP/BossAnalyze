<script setup lang="ts">
import { Codemirror } from 'vue-codemirror'
import { markdownLanguage } from '@codemirror/lang-markdown'
import { oneDark } from '@codemirror/theme-one-dark'
import { useResumeType } from '../../../hook'
import useEditorStore from '@/store/modules/editor'
import { watch, ref } from 'vue'

import { useThemeConfig } from '@/common/global'
import MarkdownToolbar from '../toolbar/mdTool.vue'
import './md-editor.scss'

defineProps<{ left: number }>()
const { isDark } = useThemeConfig()
const { resumeType } = useResumeType()
const editorStore = useEditorStore()
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
