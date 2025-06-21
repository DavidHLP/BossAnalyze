<script setup lang="ts">
import {
  useToolBarConfig,
  useHeading,
  insertIcon,
  selectIcon,
  linkFlag,
  insertLink,
  MulFlag,
  tableFlag,
  InsertTable,
  insertMulticolumn
} from './hook'
import { toolbarConfig, headings } from './constants'
import RenderIcons from '@/components/renderIcons.vue'
import ToastModal from '@/components/toast-modal/toastModal.vue'
import LinkInput from './components/linkInput/linkInput.vue'
import ColumnInput from './components/columnInput.vue'
import TableInput from './components/tableInput.vue'

const emit = defineEmits(['content-change', 'toggle-editor-mode'])

const { setHeading, level } = useHeading(emit)
useToolBarConfig(emit)
</script>

<template>
  <div class="rich-toolbar">
    <!-- 模式标识 -->
    <div class="toolbar-mode-badge">
      <i class="iconfont icon-rich-text"></i>
      <span>Rich</span>
    </div>

    <!-- 工具按钮组 -->
    <div class="toolbar-buttons">
      <div
        v-for="(toolBarItem, idx) in toolbarConfig"
        :key="idx"
        class="toolbar-button-wrapper"
      >
        <el-tooltip :content="toolBarItem.tip" placement="bottom" effect="dark">
    <button
      :data-command="toolBarItem.command"
            class="toolbar-btn"
    >
            <i :class="`iconfont icon-${toolBarItem.icon}`" class="btn-icon"></i>
          </button>
      </el-tooltip>
      </div>

      <!-- 标题选择器 -->
      <div class="heading-selector-wrapper">
        <el-tooltip content="选择标题级别" placement="bottom" effect="dark">
          <select
            id="headingButton"
            v-model="level"
            @change="setHeading"
            class="heading-selector"
          >
            <option
              :value="heading.value"
              v-for="(heading, idx) in headings"
              :key="idx"
            >
        {{ heading.label }}
      </option>
    </select>
        </el-tooltip>
      </div>
    </div>

    <!-- 编辑器切换按钮 -->
    <div class="toolbar-actions">
      <el-tooltip content="切换到Markdown模式" placement="bottom" effect="dark">
        <button @click="$emit('toggle-editor-mode')" class="toolbar-btn toolbar-btn--toggle">
          <i class="iconfont icon-markdown btn-icon"></i>
          <span class="btn-text">MD</span>
        </button>
      </el-tooltip>
    </div>
  </div>

  <!-- 模态框 -->
  <toast-modal v-if="selectIcon" :flag="selectIcon" @close="selectIcon = !selectIcon">
    <render-icons @select-icon="(icon: string) => insertIcon(icon, emit)" />
  </toast-modal>

  <toast-modal v-if="linkFlag" :flag="linkFlag" @close="linkFlag = !linkFlag">
    <link-input @confirm="(l: string, t: string) => insertLink(l,t, emit)" />
  </toast-modal>

  <toast-modal v-if="MulFlag" :flag="MulFlag" @close="MulFlag = !MulFlag">
    <column-input @confirm="(c: string) => insertMulticolumn(c, emit)" />
  </toast-modal>

  <toast-modal v-if="tableFlag" :flag="tableFlag" @close="tableFlag = !tableFlag">
    <table-input @confirm="(c: string, r: string) => InsertTable(c, r, emit)" />
  </toast-modal>
</template>

<style lang="scss" scoped>
@use '@/assets/style/variables.scss' as *;
@use '@/assets/style/darktheme.scss' as *;

.rich-toolbar {
  @include flex-between;
  height: var(--component-height-lg);
  background: var(--background-card);
  border-top-left-radius: var(--radius-lg);
  border-top-right-radius: var(--radius-lg);
  border-bottom: 1px solid var(--border-light);
  padding: 0 var(--spacing-lg);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);

  // 毛玻璃效果
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.95);

  font-family: var(--font-family-sans);
  transition: all var(--transition-normal) var(--transition-timing);

  // 暗色主题适配
  [data-theme='dark'] & {
    background: rgba(51, 65, 85, 0.95);
    border-bottom-color: var(--border-light);
  }
}

// 模式标识徽章
.toolbar-mode-badge {
  @include flex-center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-sm);
  background: linear-gradient(135deg, var(--success-color) 0%, #059669 100%);
  color: var(--text-inverse);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, transparent 100%);
    pointer-events: none;
  }

  .iconfont {
    font-size: var(--font-size-md);
  }

  span {
    font-size: var(--font-size-xs);
    letter-spacing: 0.5px;
  }
}

// 工具按钮容器
.toolbar-buttons {
  @include flex-center;
  gap: var(--spacing-xs);
  flex: 1;
  padding: 0 var(--spacing-lg);
}

.toolbar-button-wrapper {
  position: relative;
}

// 工具按钮基础样式
.toolbar-btn {
  @include flex-center;
  gap: var(--spacing-xs);
  width: var(--component-height-md);
  height: var(--component-height-md);
    border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-normal) var(--transition-timing);
  position: relative;
  overflow: hidden;

  // 悬浮效果准备
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: var(--primary-light);
    opacity: 0;
    transition: opacity var(--transition-normal) var(--transition-timing);
  }

    &:hover {
    color: var(--primary-color);
    background: var(--primary-lighter);
    @include hover-lift;

    &::before {
      opacity: 1;
    }
  }

  &:active {
    transform: translateY(1px);
  }

  // 切换按钮特殊样式
  &--toggle {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: var(--text-inverse);
    min-width: 60px;

    &:hover {
      background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
      box-shadow: var(--shadow-md);
    }

    .btn-text {
      font-size: var(--font-size-xs);
      font-weight: var(--font-weight-medium);
    }
  }
}

// 按钮图标
.btn-icon {
  font-size: var(--font-size-md);
  transition: transform var(--transition-normal) var(--transition-timing);

  .toolbar-btn:hover & {
    transform: scale(1.1);
  }
}

// 标题选择器容器
.heading-selector-wrapper {
  position: relative;
  margin-left: var(--spacing-sm);
}

// 标题选择器样式
.heading-selector {
  background: var(--background-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  padding: var(--spacing-sm) var(--spacing-md);
  font-family: var(--font-family-sans);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
    outline: none;
  transition: all var(--transition-normal) var(--transition-timing);
  min-width: 100px;

  &:hover {
    border-color: var(--border-medium);
    background: var(--background-card);
  }

  &:focus {
    border-color: var(--primary-color);
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
  }

  // 自定义下拉箭头
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath fill='%23666' d='M6 8L0 2h12z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right var(--spacing-sm) center;
  background-size: 12px;
  padding-right: var(--spacing-xl);

  // 选项样式
  option {
    background: var(--background-card);
    color: var(--text-primary);
    padding: var(--spacing-sm);

    &:hover {
      background: var(--primary-lighter);
    }
  }
}

// 工具栏操作区
.toolbar-actions {
  @include flex-center;
  gap: var(--spacing-sm);
}

// 响应式设计
@include responsive(md) {
  .rich-toolbar {
    padding: 0 var(--spacing-md);
    height: var(--component-height-md);
  }

  .toolbar-buttons {
    gap: 2px;
    padding: 0 var(--spacing-sm);
  }

  .toolbar-btn {
    width: 32px;
    height: 32px;

    .btn-text {
      display: none;
    }
  }

  .toolbar-mode-badge {
    span {
      display: none;
    }
  }

  .heading-selector {
    min-width: 80px;
    font-size: var(--font-size-xs);
  }
}

@include responsive(sm) {
  .rich-toolbar {
    padding: 0 var(--spacing-sm);
  }

  .toolbar-buttons {
    gap: 1px;
    padding: 0;
  }

  .toolbar-btn {
    width: 28px;
    height: 28px;
  }

  .btn-icon {
    font-size: var(--font-size-sm);
  }

  .heading-selector {
    min-width: 60px;
    padding: var(--spacing-xs);
  }

  .heading-selector-wrapper {
    margin-left: 2px;
  }
}

// 动画效果
@keyframes toolbarSlideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.rich-toolbar {
  animation: toolbarSlideIn 0.3s var(--transition-timing);
}

// 聚焦状态优化
.toolbar-btn:focus-visible,
.heading-selector:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
  box-shadow:
    0 0 0 2px var(--background-card),
    0 0 0 4px var(--primary-color);
}

// 活跃状态
.toolbar-btn.active {
  background: var(--primary-color);
  color: var(--text-inverse);

  &:hover {
    background: var(--primary-dark);
  }
}

// 工具提示增强
.toolbar-button-wrapper {
  position: relative;

  &:hover::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 50%;
    transform: translateX(-50%);
    width: 20px;
    height: 2px;
    background: var(--primary-color);
    border-radius: var(--radius-sm);
    opacity: 0.6;
  }
}

// 暗色主题特殊适配
[data-theme='dark'] {
  .toolbar-mode-badge {
    background: linear-gradient(135deg, var(--success-color) 0%, #047857 100%);
  }

  .toolbar-btn--toggle {
    background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);

    &:hover {
      background: linear-gradient(135deg, #f9a61a 0%, #d97706 100%);
    }
  }

  .heading-selector {
    background: var(--background-secondary);
    border-color: var(--border-light);
    color: var(--text-primary);

    // 暗色主题下拉箭头
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath fill='%23cbd5e1' d='M6 8L0 2h12z'/%3E%3C/svg%3E");

    option {
      background: var(--background-card);
      color: var(--text-primary);
    }
  }
}

// 按钮图标
.btn-icon {
  font-size: var(--font-size-md);
  transition: transform var(--transition-normal) var(--transition-timing);

  .toolbar-btn:hover & {
    transform: scale(1.1);
  }
}

// 标题选择器容器
.heading-selector-wrapper {
  position: relative;
  margin-left: var(--spacing-sm);
}

// 标题选择器样式
.heading-selector {
  background: var(--background-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  padding: var(--spacing-sm) var(--spacing-md);
  font-family: var(--font-family-sans);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
    outline: none;
  transition: all var(--transition-normal) var(--transition-timing);
  min-width: 100px;

  &:hover {
    border-color: var(--border-medium);
    background: var(--background-card);
  }

  &:focus {
    border-color: var(--primary-color);
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
  }

  // 自定义下拉箭头
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath fill='%23666' d='M6 8L0 2h12z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right var(--spacing-sm) center;
  background-size: 12px;
  padding-right: var(--spacing-xl);

  // 选项样式
  option {
    background: var(--background-card);
    color: var(--text-primary);
    padding: var(--spacing-sm);

    &:hover {
      background: var(--primary-lighter);
    }
  }
}

// 工具栏操作区
.toolbar-actions {
  @include flex-center;
  gap: var(--spacing-sm);
}

// 响应式设计
@include responsive(md) {
  .rich-toolbar {
    padding: 0 var(--spacing-md);
    height: var(--component-height-md);
  }

  .toolbar-buttons {
    gap: 2px;
    padding: 0 var(--spacing-sm);
  }

  .toolbar-btn {
    width: 32px;
    height: 32px;

    .btn-text {
      display: none;
    }
  }

  .toolbar-mode-badge {
    span {
      display: none;
    }
  }

  .heading-selector {
    min-width: 80px;
    font-size: var(--font-size-xs);
  }
}

@include responsive(sm) {
  .rich-toolbar {
    padding: 0 var(--spacing-sm);
  }

  .toolbar-buttons {
    gap: 1px;
    padding: 0;
  }

  .toolbar-btn {
    width: 28px;
    height: 28px;
  }

  .btn-icon {
    font-size: var(--font-size-sm);
  }

  .heading-selector {
    min-width: 60px;
    padding: var(--spacing-xs);
  }

  .heading-selector-wrapper {
    margin-left: 2px;
  }
}

// 动画效果
@keyframes toolbarSlideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.rich-toolbar {
  animation: toolbarSlideIn 0.3s var(--transition-timing);
}

// 聚焦状态优化
.toolbar-btn:focus-visible,
.heading-selector:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
  box-shadow:
    0 0 0 2px var(--background-card),
    0 0 0 4px var(--primary-color);
}

// 活跃状态
.toolbar-btn.active {
  background: var(--primary-color);
  color: var(--text-inverse);

  &:hover {
    background: var(--primary-dark);
  }
}

// 工具提示增强
.toolbar-button-wrapper {
  position: relative;

  &:hover::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 50%;
    transform: translateX(-50%);
    width: 20px;
    height: 2px;
    background: var(--primary-color);
    border-radius: var(--radius-sm);
    opacity: 0.6;
  }
}

// 暗色主题特殊适配
[data-theme='dark'] {
  .toolbar-mode-badge {
    background: linear-gradient(135deg, var(--success-color) 0%, #047857 100%);
  }

  .toolbar-btn--toggle {
    background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);

    &:hover {
      background: linear-gradient(135deg, #f9a61a 0%, #d97706 100%);
    }
  }

  .heading-selector {
    background: var(--background-secondary);
    border-color: var(--border-light);
    color: var(--text-primary);

    // 暗色主题下拉箭头
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath fill='%23cbd5e1' d='M6 8L0 2h12z'/%3E%3C/svg%3E");

    option {
      background: var(--background-card);
      color: var(--text-primary);
    }
  }
}
</style>
