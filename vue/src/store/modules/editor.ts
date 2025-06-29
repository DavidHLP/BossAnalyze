import { defineStore } from 'pinia'

import pinia from '@/store'
import { getLocalStorage, setLocalStorage } from '@/common/localstorage'
import { templates } from '@/templates/config'

const MARKDOWN_CONTENT = 'markdown-content'

export const getCurrentTypeContent = (type: string): string => {
  for (const template of templates.value) {
    if (type === template.type) {
      return template.content
    }
  }
  return ''
}

const useEditorStore = defineStore('editorStore', {
  state: () => ({
    MDContent: ''
  }),
  actions: {
    // 初始化编辑器内容（默认为Markdown模式）
    initMDContent(resumeType: string) {
      const cacheKey = MARKDOWN_CONTENT + '-' + resumeType
      this.MDContent = getLocalStorage(cacheKey)
        ? (getLocalStorage(cacheKey) as string)
        : getCurrentTypeContent(resumeType)
    },
    setMDContent(nv: string, resumeType: string) {
      this.MDContent = nv
      // 处理之后的操作
      if (!nv) return
      setLocalStorage(`${MARKDOWN_CONTENT}-${resumeType}`, nv)
    }
  }
})

export default () => useEditorStore(pinia)
