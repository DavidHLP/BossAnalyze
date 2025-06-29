import { defineStore } from 'pinia'

import pinia from '@/store'
import { getLocalStorage, setLocalStorage } from '@/common/localstorage'
import { templates } from '@/templates/config'
import { getResumeById, updateResume } from '@/api/resume/resume'
import { ElMessage } from 'element-plus'

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
    MDContent: '',
    resumeId: null as string | null,
    resumeTitle: '',
  }),
  actions: {
    setResumeId(id: string | null) {
      this.resumeId = id
    },
    async initMDContent(resumeType: string) {
      if (this.resumeId) {
        try {
          const resume = await getResumeById(this.resumeId)
          this.MDContent = resume.content
          this.resumeTitle = resume.title
        } catch (error) {
          ElMessage.error('加载简历失败，请返回列表重试')
          this.MDContent = '# 加载失败'
        }
      } else {
        const cacheKey = MARKDOWN_CONTENT + '-' + resumeType
        const cachedContent = getLocalStorage(cacheKey) as string
        this.MDContent = cachedContent || getCurrentTypeContent(resumeType)
        this.resumeTitle = '新简历'
      }
    },
    setMDContent(nv: string, resumeType: string) {
      this.MDContent = nv
      if (!this.resumeId) {
        // For new resumes not yet saved, still cache to localstorage
        setLocalStorage(`${MARKDOWN_CONTENT}-${resumeType}`, nv)
      }
    },
    async saveContent() {
      if (!this.resumeId) {
        ElMessage.warning('无法保存，简历ID不存在')
        return
      }
      try {
        await updateResume(this.resumeId, {
          title: this.resumeTitle,
          content: this.MDContent,
        })
        ElMessage.success('保存成功！')
      } catch (e) {
        ElMessage.error('保存失败')
      }
    }
  }
})

export default () => useEditorStore(pinia)
