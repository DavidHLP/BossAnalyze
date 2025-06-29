<script setup lang="ts">
import { wOpen } from '@/utils'
import { useFile } from './hook'
import ExportTotal from '@/components/exportTotal.vue'
import useEditorStore from '@/store/modules/editor'
import { ref, watch } from 'vue'

const emit = defineEmits([
  'download-dynamic',
  'download-native',
  'download-md',
  'import-md',
  'download-picture',
])

const { exportFile, exportPicture, exportPDF, importFile, fileName } = useFile(emit)
const editorStore = useEditorStore()

// Sync store title with local fileName for exporting
watch(() => editorStore.resumeTitle, (newTitle) => {
    if (newTitle !== fileName.value) {
        fileName.value = newTitle
    }
})

// Sync user input back to store
watch(fileName, (newFileName) => {
    if (newFileName !== editorStore.resumeTitle) {
        editorStore.resumeTitle = newFileName
    }
})

// tabs 管理
const activeTab = ref('import')
const fileInput = ref<HTMLInputElement>()

// 处理 tab 点击事件
const handleTabClick = (tab: any) => {
  const tabName = tab.paneName
  console.log('🔥 Tab被点击:', tabName)

  switch (tabName) {
    case 'import':
      // 触发文件选择
      fileInput.value?.click()
      break
    case 'export-md':
      handleExportMD()
      break
    case 'export-picture':
      handleExportPicture()
      break
    default:
      console.log('未知的tab:', tabName)
  }
}

// 主题切换功能
const isDarkMode = ref(false)
const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value
  document.documentElement.setAttribute('data-theme', isDarkMode.value ? 'dark' : 'light')
}

// 处理导入MD文件
const handleImportMD = (event: Event) => {
  console.log('🔥 导入MD按钮被点击')
  importFile(event)
}

// 调试函数
const checkStore = () => {
  const editorStore = useEditorStore()
  console.log('🔍 Store状态检查:')
  console.log('📝 MDContent长度:', editorStore.MDContent?.length || 0)
  console.log('📝 MDContent预览:', editorStore.MDContent?.substring(0, 200) || '空内容')
  console.log('📝 document.title:', document.title)
  console.log('📝 fileName.value:', fileName.value)

  if (!editorStore.MDContent || editorStore.MDContent.trim() === '') {
    console.error('🚫 Store中的内容为空！')
  } else {
    console.log('✅ Store中有内容，可以导出')
  }
}

// 处理导出MD事件
const handleExportMD = () => {
  console.log('🔥 header.vue: 收到export-md事件')
  exportFile('md')
  console.log('🔥 header.vue: 已调用exportFile(md)')
}

// 导出MD功能
const testDownload = () => {
  console.log('🔥 导出MD按钮被点击')
  const editorStore = useEditorStore()

  if (!editorStore.MDContent) {
    console.error('🚫 没有内容可下载')
    return
  }

  try {
    const content = editorStore.MDContent
    const fileName = 'test-export.md'

    console.log('🔥 创建下载，内容长度:', content.length)

    // 直接下载，不通过事件系统
    const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' })
    const url = URL.createObjectURL(blob)

    const a = document.createElement('a')
    a.download = fileName
    a.href = url
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)

    console.log('🔥 直接下载完成')
  } catch (error) {
    console.error('🚫 直接下载失败:', error)
  }
}

// 处理导出图片
const handleExportPicture = async () => {
  console.log('🔥 导出图片按钮被点击')
  await exportPicture()
}

// 处理导出PDF
const handleExportPDF = async () => {
  console.log('🔥 导出PDF按钮被点击')
  await exportPDF()
}

const handleSave = () => {
    editorStore.saveContent();
}
</script>

<template>
  <header id="editor-header" class="editor-header">
    <!-- 左侧操作区 -->
    <div class="header-left">
      <el-tooltip content="返回上一页" placement="bottom" effect="dark">
        <button class="header-btn header-btn--back" @click="$router.back()">
          <i class="iconfont icon-back"></i>
        </button>
      </el-tooltip>

      <div class="file-name-container">
        <i class="iconfont icon-file file-icon"></i>
        <input
          id="resume-name-input"
          type="text"
          v-model="fileName"
          placeholder="请输入文档标题..."
          class="file-name-input"
        />
      </div>
    </div>

    <!-- 中间导航区 -->
    <div class="header-center">
      <!-- 使用 el-tabs 管理导入导出功能 -->
      <el-tabs v-model="activeTab" class="editor-tabs" @tab-click="handleTabClick" type="card">
        <el-tab-pane label="导入MD" name="import">
          <template #label>
            <span class="tab-label">
              <i class="iconfont icon-import"></i>
              <span>导入MD</span>
            </span>
          </template>
        </el-tab-pane>

        <el-tab-pane label="导出MD" name="export-md">
          <template #label>
            <span class="tab-label">
              <i class="iconfont icon-download"></i>
              <span>导出MD</span>
            </span>
          </template>
        </el-tab-pane>

        <el-tab-pane label="导出图片" name="export-picture">
          <template #label>
            <span class="tab-label">
              <i class="iconfont icon-picture"></i>
              <span>导出图片</span>
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 隐藏的文件输入框 -->
      <input
        accept=".md"
        ref="fileInput"
        type="file"
        @change="handleImportMD"
        class="file-input-hidden"
      />
    </div>

    <!-- 右侧工具区 -->
    <div class="header-right">
      <el-tooltip content="保存到云端" placement="bottom" effect="dark">
        <button class="header-btn header-btn--secondary" @click="handleSave">
          <i class="iconfont icon-save"></i>
          <span>保存</span>
        </button>
      </el-tooltip>
      <!-- 主题切换按钮 -->
      <el-tooltip
        :content="isDarkMode ? '切换到亮色模式' : '切换到暗色模式'"
        placement="bottom"
        effect="dark"
      >
        <button class="header-btn header-btn--theme" @click="toggleTheme">
          <i :class="isDarkMode ? 'iconfont icon-sun' : 'iconfont icon-moon'"></i>
        </button>
      </el-tooltip>

      <!-- 导出组件 -->
      <ExportTotal />

      <!-- 主要操作按钮 -->
      <button class="header-btn header-btn--primary" @click="handleExportPDF">
        <i class="iconfont icon-export"></i>
        <span>导出PDF</span>
      </button>
    </div>
  </header>
</template>

<style lang="scss" scoped>
@use '@/assets/style/variables.scss' as *;
@use '@/assets/style/darktheme.scss' as *;

.editor-header {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  height: var(--layout-header-height);
  background: var(--background-card);
  border-bottom: 1px solid var(--border-light);
  @include card-shadow;

  @include flex-between;
  padding: 0 var(--spacing-xl);
  font-family: var(--font-family-sans);
  color: var(--text-primary);

  // 毛玻璃效果
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.95);

  // 暗色主题适配
  [data-theme='dark'] & {
    background: rgba(51, 65, 85, 0.95);
    border-bottom-color: var(--border-light);
  }
}

// 左侧区域
.header-left {
  @include flex-start;
  gap: var(--spacing-lg);
  flex: 0 0 auto;
}

// 中间区域
.header-center {
  @include flex-center;
  flex: 1 1 auto;
}

// 右侧区域
.header-right {
  @include flex-end;
  gap: var(--spacing-md);
  flex: 0 0 auto;
}

// 通用按钮样式
.header-btn {
  @include flex-center;
  gap: var(--spacing-sm);
  height: var(--component-height-md);
  padding: 0 var(--spacing-md);
  border: none;
  border-radius: var(--radius-md);
  font-family: var(--font-family-sans);
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-md);
  cursor: pointer;
  transition: all var(--transition-normal) var(--transition-timing);
  outline: none;
  position: relative;
  overflow: hidden;

  // 涟漪效果准备
  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    border-radius: var(--radius-full);
    background: rgba(255, 255, 255, 0.3);
    transform: translate(-50%, -50%);
    transition:
      width 0.3s,
      height 0.3s;
  }

  &:active::before {
    width: 200%;
    height: 200%;
  }

  &:hover {
    @include hover-lift;
  }

  &:focus-visible {
    outline: 2px solid var(--primary-color);
    outline-offset: 2px;
  }

  .iconfont {
    font-size: var(--font-size-lg);
    transition: transform var(--transition-normal) var(--transition-timing);
  }

  &:hover .iconfont {
    transform: scale(1.1);
  }
}

// 返回按钮
.header-btn--back {
  background: var(--background-secondary);
  color: var(--text-secondary);
  width: var(--component-height-md);

  &:hover {
    background: var(--border-light);
    color: var(--text-primary);
  }
}

// 主题切换按钮
.header-btn--theme {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: var(--text-inverse);
  width: var(--component-height-md);

  &:hover {
    background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  }

  [data-theme='dark'] & {
    background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);

    &:hover {
      background: linear-gradient(135deg, #f9a61a 0%, #d97706 100%);
    }
  }
}

// 主要按钮
.header-btn--primary {
  background: var(--gradient-primary);
  color: var(--text-inverse);
  padding: 0 var(--spacing-lg);

  &:hover {
    background: var(--primary-dark);
    box-shadow: var(--shadow-lg);
  }

  &:active {
    transform: translateY(1px);
  }
}

// 导入按钮
.header-btn--import {
  background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
  color: var(--text-inverse);
  padding: 0 var(--spacing-lg);
  position: relative;
  cursor: pointer;

  &:hover {
    background: linear-gradient(135deg, #0891b2 0%, #0e7490 100%);
    box-shadow: var(--shadow-lg);
  }

  &:active {
    transform: translateY(1px);
  }
}

// 导出图片按钮
.header-btn--picture {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: var(--text-inverse);
  padding: 0 var(--spacing-lg);
  position: relative;
  cursor: pointer;

  &:hover {
    background: linear-gradient(135deg, #059669 0%, #047857 100%);
    box-shadow: var(--shadow-lg);
  }

  &:active {
    transform: translateY(1px);
  }
}

// 隐藏文件输入框
.file-input-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

// 文件名容器
.file-name-container {
  @include flex-start;
  gap: var(--spacing-sm);
  position: relative;

  .file-icon {
    color: var(--text-secondary);
    font-size: var(--font-size-lg);
    transition: color var(--transition-normal) var(--transition-timing);
  }
}

// 文件名输入框
.file-name-input {
  border: none;
  outline: none;
  background: var(--background-secondary);
  color: var(--text-primary);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-md);
  font-family: var(--font-family-sans);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  min-width: 200px;
  transition: all var(--transition-normal) var(--transition-timing);
  border: 1px solid transparent;

  &::placeholder {
    color: var(--text-disabled);
  }

  &:hover {
    background: var(--background-primary);
    border-color: var(--border-light);
  }

  &:focus {
    background: var(--background-card);
    border-color: var(--primary-color);
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);

    + .file-icon {
      color: var(--primary-color);
    }
  }
}

// 响应式设计
@include responsive(md) {
  .editor-header {
    padding: 0 var(--spacing-md);
    height: 56px;
  }

  .header-left,
  .header-right {
    gap: var(--spacing-sm);
  }

  .file-name-input {
    min-width: 150px;
    font-size: var(--font-size-sm);
  }

  .header-btn {
    height: 36px;
    padding: 0 var(--spacing-sm);

    span {
      display: none;
    }
  }

  .header-btn--primary {
    padding: 0 var(--spacing-sm);
  }

  .header-btn--import {
    padding: 0 var(--spacing-sm);
  }

  .header-btn--picture {
    padding: 0 var(--spacing-sm);
  }
}

@include responsive(sm) {
  .editor-header {
    padding: 0 var(--spacing-sm);
  }

  .file-name-input {
    min-width: 120px;
  }

  .header-center {
    flex: 1 1 auto;
  }

  .editor-tabs {
    :deep(.el-tabs__item) {
      padding: 0 var(--spacing-sm);

      .tab-label span {
        display: none;
      }

      .tab-label .iconfont {
        font-size: var(--font-size-lg);
      }
    }
  }
}

// el-tabs 样式定制
.editor-tabs {
  :deep(.el-tabs__header) {
    margin: 0;
    border: none;
    background: transparent;
  }

  :deep(.el-tabs__nav-wrap) {
    &::after {
      display: none;
    }
  }

  :deep(.el-tabs__nav) {
    border: 1px solid var(--border-light);
    border-radius: var(--radius-lg);
    background: var(--background-secondary);
    padding: 4px;
    box-shadow: var(--shadow-sm);
  }

  :deep(.el-tabs__item) {
    border: none !important;
    border-radius: var(--radius-md);
    margin: 0 2px;
    padding: 0 var(--spacing-md);
    height: 36px;
    line-height: 36px;
    color: var(--text-secondary);
    background: transparent;
    transition: all var(--transition-normal) var(--transition-timing);
    font-weight: var(--font-weight-medium);
    font-size: var(--font-size-sm);

    &:hover {
      color: var(--text-primary);
      background: var(--background-primary);
      transform: translateY(-1px);
    }

    &.is-active {
      color: var(--text-inverse) !important;
      background: var(--gradient-primary) !important;
      box-shadow: var(--shadow-md);
      transform: translateY(-1px);
    }

    .tab-label {
      @include flex-center;
      gap: var(--spacing-xs);

      .iconfont {
        font-size: var(--font-size-md);
        transition: transform var(--transition-normal) var(--transition-timing);
      }
    }

    &:hover .tab-label .iconfont,
    &.is-active .tab-label .iconfont {
      transform: scale(1.1);
    }
  }

  :deep(.el-tabs__content) {
    display: none;
  }
}

// 暗色主题下的 tabs 样式
[data-theme='dark'] .editor-tabs {
  :deep(.el-tabs__nav) {
    background: var(--background-card);
    border-color: var(--border-light);
  }

  :deep(.el-tabs__item) {
    &:hover {
      background: var(--background-secondary);
    }

    &.is-active {
      background: var(--gradient-primary) !important;
    }
  }
}

// 动画效果
@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.editor-header {
  animation: fadeInDown 0.3s var(--transition-timing);
}

// 悬浮状态增强
.header-btn:hover {
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0) 100%);
    pointer-events: none;
  }
}

// 焦点状态优化
.header-btn:focus-visible {
  box-shadow:
    0 0 0 2px var(--background-card),
    0 0 0 4px var(--primary-color);
}

.header-btn--secondary {
  border-color: var(--primary-color);
  color: var(--primary-color);

  &:hover {
    background: var(--primary-lighter);
    border-color: var(--primary-dark);
  }
}
</style>
