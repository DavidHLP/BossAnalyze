<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import Header from './components/header/header.vue'
import MarkdownRender from '@/views/resume/components/preview/render.vue'
import Editor from '@/views/resume/components/editor/editorContainer.vue'
import { useResumeType, useDownLoad, useImportMD, useAvatar, useShowExport } from './hook'
import { startGuide } from './components/guide/guide'
import useEditorStore from '@/store/modules/editor'

const { resumeType } = useResumeType()
const { downloadDynamic, downloadNative, downloadMD } = useDownLoad(resumeType)
const { importMD } = useImportMD(resumeType.value)
const { setAvatar } = useAvatar(resumeType.value)
const { showExport } = useShowExport()
startGuide()

const route = useRoute()
const editorStore = useEditorStore()

onMounted(() => {
  const resumeId = route.query.id as string | null
  editorStore.setResumeId(resumeId)
  editorStore.initMDContent(resumeType.value)
})
</script>

<template>
  <Header
    @download-dynamic="(filename: string) => downloadDynamic(true, filename)"
    @download-picture="(filename: string) => downloadDynamic(false, filename)"
    @download-native="downloadNative"
    @download-md="downloadMD"
    @import-md="importMD"
  />
  <div id="editor">
    <Editor />
    <markdown-render class="markdown-render" @upload-avatar="setAvatar" />
    <el-tooltip content="导出PDF文件" v-if="showExport">
      <i
        data-aos="fade-in"
        data-aos-duration="800"
        data-aos-offset="50"
        class="iconfont icon-export hover pointer standby-export"
        @click="downloadDynamic(true)"
      ></i>
    </el-tooltip>
  </div>
</template>

<style lang="scss" scoped>
#editor {
  display: flex;
  position: relative;

  .markdown-render {
    flex: 1;
    margin: 0 10px;
    border-radius: 10px;
  }

  .standby-export {
    position: absolute;
    top: 120px;
    right: 30px;
    z-index: 3;
    color: #f8f8f8;
    background: var(--theme);
    text-align: center;
    line-height: 45px;
    padding-left: 2px;
    font-size: 22px;
    width: 45px;
    height: 45px;
    border-radius: 50%;

    &:hover {
      color: var(--theme);
      background: #f8f8f8;
    }
  }
}
</style>
