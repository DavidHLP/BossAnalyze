<template>
  <div class="markdown-editor-container" :class="{ 'is-dark': props.theme === 'dark' }" ref="editorContainer">
    <md-editor
      v-model="editorText"
      :toolbars="toolbars"
      :toolbar-config="toolbarConfig"
      :preview-theme="previewTheme"
      :editor-theme="editorTheme"
      :editor-class="editorClass"
      :placeholder="placeholder"
      :language="language"
      :code-theme="codeTheme"
      :theme="theme"
      :preview-only="previewOnly"
      :tab-width="tabWidth"
      :show-code-row-number="showCodeRowNumber"
      :disabled="disabled"
      :auto-focus="autoFocus"
      @onChange="handleChange"
      @onSave="handleSave"
      @onUploadImg="handleUploadImage"
      @onHtmlChanged="handleHtmlChanged"
      @onGetCatalog="handleGetCatalog"
      @onError="handleError"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits, watch, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames, HeadList, Themes } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入内容...'
  },
  height: {
    type: [String, Number],
    default: 'auto' // 变更为auto，可接受具体像素值或百分比
  },
  minHeight: {
    type: [String, Number],
    default: '200px' // 最小高度
  },
  maxHeight: {
    type: [String, Number],
    default: '80vh' // 最大高度为视口高度的80%
  },
  autoResize: {
    type: Boolean,
    default: true // 是否自动调整大小
  },
  theme: {
    type: String as () => Themes,
    default: 'dark' as Themes
  },
  language: {
    type: String,
    default: 'zh-CN' // 默认中文
  },
  previewOnly: {
    type: Boolean,
    default: false // 是否仅预览模式
  },
  codeTheme: {
    type: String,
    default: 'atom' // 代码块主题
  },
  tabWidth: {
    type: Number,
    default: 2 // Tab键空格数
  },
  showCodeRowNumber: {
    type: Boolean,
    default: true // 显示代码行号
  },
  disabled: {
    type: Boolean,
    default: false // 是否禁用编辑器
  },
  sanitize: {
    type: Boolean,
    default: true // 是否开启XSS过滤
  },
  autoFocus: {
    type: Boolean,
    default: false // 是否自动聚焦
  },
  mobileToolbars: {
    type: Array as () => ToolbarNames[],
    default: () => [
      'bold', 'italic', 'title', 'strikeThrough',
      'unorderedList', 'orderedList', 'codeRow',
      'link', 'image', 'save', 'preview', 'fullscreen'
    ] // 移动设备上显示的精简工具栏
  },
  mobileBreakpoint: {
    type: Number,
    default: 768 // 移动设备断点，小于此宽度使用移动配置
  }
})

const emit = defineEmits([
  'update:modelValue',
  'change',
  'save',
  'upload-image',
  'html-changed',
  'get-catalog',
  'error',
  'resize'
])

// 编辑器内容
const editorText = ref(props.modelValue)

// 编辑器主题，根据props.theme动态设置
const editorTheme = computed(() => props.theme)

// 预览主题，根据props.theme动态设置
const previewTheme = computed(() => {
  return props.theme === 'dark' ? 'vuepress' : 'default'
})

// 容器引用
const editorContainer = ref<HTMLElement | null>(null)

// 窗口尺寸状态
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 0)
const isMobile = computed(() => windowWidth.value <= props.mobileBreakpoint)

// 动态计算的编辑器高度
const calculatedHeight = ref(typeof props.height === 'number' ? `${props.height}px` : props.height)

// 动态工具栏，根据屏幕尺寸调整
const toolbars = computed(() => {
  if (isMobile.value) {
    return props.mobileToolbars
  }

  return [
    'pageFullscreen',
    'fullscreen',
    '-',
    'bold',
    'italic',
    'underline',
    'strikeThrough',
    '-',
    'title',
    'sub',
    'sup',
    'quote',
    'unorderedList',
    'orderedList',
    'task',
    '-',
    'codeRow',
    'code',
    'link',
    'image',
    'table',
    'mermaid',
    'katex',
    '-',
    'revoke',
    'next',
    'save',
    'prettier', // 代码格式化
    'preview',
    'htmlPreview', // HTML预览
    'catalog',
    'github', // GitHub风格
    'theme' // 主题切换
  ] as ToolbarNames[]
})

// 监听父组件传入的 modelValue 变化
watch(() => props.modelValue, (newValue) => {
  if (newValue !== editorText.value) {
    editorText.value = newValue
  }
})

// 监听编辑器内容变化，向父组件发射更新事件
watch(editorText, (newValue) => {
  emit('update:modelValue', newValue)
})

// 窗口大小变化处理
const handleResize = () => {
  windowWidth.value = window.innerWidth
  updateEditorSize()
}

// 更新编辑器尺寸
const updateEditorSize = () => {
  if (!editorContainer.value || !props.autoResize) return

  const containerHeight = editorContainer.value.parentElement?.clientHeight

  if (containerHeight) {
    // 根据父容器高度计算编辑器高度，减去可能的边距和边框
    const calculatedContainerHeight = containerHeight * 0.95 // 留5%的间距

    // 确保高度在minHeight和maxHeight之间
    const minHeightPixels = parseSize(props.minHeight)
    const maxHeightPixels = parseSize(props.maxHeight)

    let newHeight = Math.max(calculatedContainerHeight, minHeightPixels)
    if (maxHeightPixels > 0) {
      newHeight = Math.min(newHeight, maxHeightPixels)
    }

    calculatedHeight.value = `${newHeight}px`
    emit('resize', newHeight)
  }
}

// 辅助函数：将尺寸字符串转换为像素值
const parseSize = (size: string | number): number => {
  if (typeof size === 'number') return size

  // 处理百分比（相对于视口高度）
  if (size.endsWith('vh')) {
    const percentage = parseFloat(size)
    return (percentage / 100) * window.innerHeight
  }

  // 处理像素值
  if (size.endsWith('px')) {
    return parseFloat(size)
  }

  // 处理rem值（假设1rem = 16px）
  if (size.endsWith('rem')) {
    return parseFloat(size) * 16
  }

  // 如果是auto或其他无法解析的值，返回默认最小高度
  return 200
}

// 编辑器样式类
const editorClass = computed(() => {
  const heightClass = props.height === 'auto' ? 'h-auto' : `h-${calculatedHeight.value}`
  return `${heightClass} md-editor-${props.theme} ${isMobile.value ? 'is-mobile' : ''}`
})

// 自定义工具栏配置
const toolbarConfig = ref({
  title: {
    tip: '标题',
    key: 'title'
  },
  theme: {
    switch: true
  },
  image: {
    accept: '.jpg,.jpeg,.png,.gif,.webp',
    // 最大限制2MB
    limitSize: 2 * 1024 * 1024
  },
  link: {
    target: '_blank', // 新窗口打开链接
  },
  table: {
    maxRow: 10,
    maxCol: 10
  }
})

// 事件处理
const handleChange = (value: string) => {
  emit('change', value)
}

const handleSave = (value: string) => {
  emit('save', value)
}

const handleUploadImage = (files: FileList, callback: (urls: string[]) => void) => {
  emit('upload-image', files, callback)

  // 如果父组件未提供图片上传处理，这里提供一个简单的默认实现
  // 将图片转换为 base64 字符串
  const fileList = Array.from(files)
  const uploadPromises = fileList.map(file => {
    return new Promise<string>((resolve) => {
      const reader = new FileReader()
      reader.readAsDataURL(file)
      reader.onload = () => {
        resolve(reader.result as string)
      }
    })
  })

  Promise.all(uploadPromises).then(urls => {
    callback(urls)
  })
}

// HTML内容变化处理
const handleHtmlChanged = (html: string) => {
  emit('html-changed', html)
}

// 获取目录结构
const handleGetCatalog = (catalog: HeadList[]) => {
  emit('get-catalog', catalog)
}

// 错误处理
const handleError = (err: Error) => {
  emit('error', err)
  console.error('Markdown编辑器错误:', err)
}

// 生命周期钩子
onMounted(() => {
  // 添加窗口尺寸监听
  window.addEventListener('resize', handleResize)

  // 初始化尺寸
  nextTick(() => {
    updateEditorSize()
  })
})

onUnmounted(() => {
  // 移除窗口尺寸监听
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.markdown-editor-container {
  width: 100%;
  height: v-bind(calculatedHeight);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: height 0.3s ease;
  display: flex;
  flex-direction: column;

  &.is-dark {
    --md-bk-color: #1e1e1e !important;
    --md-color: #e0e0e0 !important;
  }

  :deep(.md-editor) {
    border-radius: 8px;
    overflow: hidden;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
    height: 100%;
    display: flex;
    flex-direction: column;
  }

  :deep(.md-editor-content) {
    min-height: v-bind('props.minHeight');
    max-height: v-bind('props.maxHeight');
    flex: 1;
    overflow: auto;
  }

  // 代码块样式优化
  :deep(.md-editor-preview pre) {
    border-radius: 4px;
    padding: 1em;
    overflow-x: auto;
  }

  // 表格样式优化
  :deep(.md-editor-preview table) {
    border-collapse: collapse;
    margin: 1em 0;
    overflow-x: auto;
    display: block;
    width: 100%;

    th, td {
      border: 1px solid #ddd;
      padding: 8px 12px;
    }

    th {
      background-color: #f2f2f2;
    }

    tr:nth-child(even) {
      background-color: #f9f9f9;
    }
  }

  // 链接样式
  :deep(.md-editor-preview a) {
    color: #0366d6;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  // 引用块样式
  :deep(.md-editor-preview blockquote) {
    border-left: 4px solid #dfe2e5;
    color: #6a737d;
    margin: 0;
    padding: 0 1em;
  }

  // 工具栏样式优化
  :deep(.md-toolbar) {
    padding: 8px;
    border-bottom: 1px solid #e1e4e8;
    flex-shrink: 0;

    .md-toolbar-item {
      margin: 0 4px;

      &:hover {
        background-color: rgba(0, 0, 0, 0.05);
      }
    }
  }

  // 按钮样式
  :deep(.md-editor button) {
    transition: all 0.2s ease;
  }

  // 移动设备响应式样式
  &.is-mobile {
    :deep(.md-toolbar) {
      padding: 4px;
      flex-wrap: wrap;
      justify-content: center;

      .md-toolbar-item {
        margin: 2px;
      }
    }

    :deep(.md-editor-content) {
      min-height: 150px;
    }
  }
}

// 媒体查询 - 平板设备
@media (max-width: 992px) {
  .markdown-editor-container {
    border-radius: 6px;

    :deep(.md-toolbar) {
      padding: 6px;
    }
  }
}

// 媒体查询 - 移动设备
@media (max-width: 768px) {
  .markdown-editor-container {
    border-radius: 4px;
    box-shadow: 0 1px 8px rgba(0, 0, 0, 0.1);

    :deep(.md-toolbar) {
      padding: 4px;
    }

    :deep(.md-editor-content) {
      min-height: 120px;
    }
  }
}
</style>