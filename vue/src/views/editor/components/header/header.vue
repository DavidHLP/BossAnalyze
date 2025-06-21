<script setup lang="ts">
import navMenu from './nav.vue'
import { wOpen } from '@/utils'
import { useFile } from './hook'
import ExportTotal from '@/components/exportTotal.vue'
import { ref } from 'vue'

const emit = defineEmits([
  'download-dynamic',
  'download-native',
  'download-md',
  'import-md',
  'download-picture',
])

const { exportFile, importFile, fileName } = useFile(emit)

// 主题切换功能
const isDarkMode = ref(false)
const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value
  document.documentElement.setAttribute('data-theme', isDarkMode.value ? 'dark' : 'light')
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
      <nav-menu
        @export-md="exportFile('md')"
        @import-md="importFile"
        @export-picture="exportFile('picture')"
      />
    </div>

    <!-- 右侧工具区 -->
    <div class="header-right">
      <!-- 主题切换按钮 -->
      <el-tooltip :content="isDarkMode ? '切换到亮色模式' : '切换到暗色模式'" placement="bottom" effect="dark">
        <button class="header-btn header-btn--theme" @click="toggleTheme">
          <i :class="isDarkMode ? 'iconfont icon-sun' : 'iconfont icon-moon'"></i>
        </button>
      </el-tooltip>

      <!-- 导出组件 -->
      <ExportTotal />

      <!-- 主要操作按钮 -->
      <button class="header-btn header-btn--primary" @click="exportFile('dynamic')">
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
    transition: width 0.3s, height 0.3s;
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
}

@include responsive(sm) {
  .editor-header {
    padding: 0 var(--spacing-sm);
  }

  .file-name-input {
    min-width: 120px;
  }

  .header-center {
    display: none;
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
    background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0) 100%);
    pointer-events: none;
  }
}

// 焦点状态优化
.header-btn:focus-visible {
  box-shadow:
    0 0 0 2px var(--background-card),
    0 0 0 4px var(--primary-color);
}
</style>
