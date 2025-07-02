<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock } from '@element-plus/icons-vue'
import Header from './components/header/header.vue'
import MarkdownRender from '@/views/resume/components/preview/render.vue'
import Editor from '@/views/resume/components/editor/editorContainer.vue'
import VersionHistory from './components/VersionHistory.vue'
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

// 版本管理相关
const versionDrawerVisible = ref(false)
const currentResumeId = ref<string | null>(null)

// 版本刷新回调
const onVersionRefresh = () => {
  ElMessage.success('版本操作完成！')
  // 刷新编辑器内容
  editorStore.initMDContent(resumeType.value)
}

onMounted(() => {
  const resumeId = route.query.id as string | null
  currentResumeId.value = resumeId
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

    <!-- 版本管理按钮 -->
    <el-tooltip content="版本历史" v-if="currentResumeId">
      <el-button
        type="primary"
        :icon="Clock"
        class="version-btn"
        @click="versionDrawerVisible = true"
        circle
      />
    </el-tooltip>
  </div>

  <!-- 版本管理抽屉 -->
  <el-drawer v-model="versionDrawerVisible" title="Git版本管理" direction="rtl" size="600px">
    <VersionHistory
      v-if="currentResumeId"
      :resume-id="currentResumeId"
      :visible="versionDrawerVisible"
      @update:visible="versionDrawerVisible = $event"
      @refresh="onVersionRefresh"
    />
  </el-drawer>
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

  .version-btn {
    position: absolute;
    top: 180px;
    right: 30px;
    z-index: 3;
    width: 45px;
    height: 45px;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);

    &:hover {
      transform: scale(1.1);
      transition: transform 0.2s;
    }
  }
}
</style>
