<script setup lang="ts">
import RenderIcons from '@/components/renderIcons.vue'
import ColumnInput from './components/columnInput.vue'
import TableInput from './components/tableInput.vue'

import { markdownModeToolbarConfig } from './constants'
import { MulFlag } from './hook'
import { selectIcon } from './hook'
import {
  markdownModeToolbarCommandHandler,
  markdownModeInsertIcon,
  markdownModeInsertMultiColumnsLayout,
  markdownModeInsertTable
} from './hook'
import { tableFlag } from './hook'
import { useThemeConfig } from '@/common/global'

const emit = defineEmits(['toggle-editor-mode'])

const { isDark } = useThemeConfig()
</script>

<template>
  <div class="md-toolbar">
    <!-- 模式标识 -->
    <div class="toolbar-mode-badge">
      <i class="iconfont icon-markdown"></i>
      <span>MD</span>
    </div>

    <!-- 工具按钮组 -->
    <div class="toolbar-buttons">
      <div
        v-for="(toolBarItem, idx) in markdownModeToolbarConfig"
        :key="idx"
        class="toolbar-button-wrapper"
      >
        <el-tooltip :content="toolBarItem.tip" placement="bottom" effect="dark">
                    <button
            @click="markdownModeToolbarCommandHandler(toolBarItem.command, emit)"
            :data-command="toolBarItem.command"
            class="toolbar-btn"
          >
            <i :class="`iconfont icon-${toolBarItem.icon}`" class="btn-icon"></i>
          </button>
        </el-tooltip>
      </div>
    </div>

    <!-- 编辑器切换按钮 -->
    <div class="toolbar-actions">
      <el-tooltip content="切换到富文本模式" placement="bottom" effect="dark">
        <button @click="$emit('toggle-editor-mode')" class="toolbar-btn toolbar-btn--toggle">
          <i class="iconfont icon-rich-text btn-icon"></i>
          <span class="btn-text">Rich</span>
        </button>
      </el-tooltip>
    </div>
  </div>

  <!-- 模态框 -->
  <toast-modal v-if="selectIcon" :flag="selectIcon" @close="selectIcon = !selectIcon">
    <render-icons @select-icon="(icon: string) => markdownModeInsertIcon(icon)" />
  </toast-modal>

  <toast-modal v-if="MulFlag" :flag="MulFlag" @close="MulFlag = !MulFlag">
    <column-input @confirm="(c: string) => markdownModeInsertMultiColumnsLayout(c)" />
  </toast-modal>

  <toast-modal v-if="tableFlag" :flag="tableFlag" @close="tableFlag = !tableFlag">
    <table-input @confirm="(c: string, r: string) => markdownModeInsertTable(c, r)" />
  </toast-modal>
</template>

<style lang="scss" scoped>
@use '@/assets/style/variables.scss' as *;
@use '@/assets/style/darktheme.scss' as *;

.md-toolbar {
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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

  // 按钮变体
  &--primary {
    background: var(--primary-light);
    color: var(--primary-color);

    &:hover {
      background: var(--primary-color);
      color: var(--text-inverse);
    }
  }

  &--success {
    background: rgba(16, 185, 129, 0.1);
    color: var(--success-color);

    &:hover {
      background: var(--success-color);
      color: var(--text-inverse);
    }
  }

  &--warning {
    background: rgba(245, 158, 11, 0.1);
    color: var(--warning-color);

    &:hover {
      background: var(--warning-color);
      color: var(--text-inverse);
    }
  }

  // 切换按钮特殊样式
  &--toggle {
    background: var(--gradient-primary);
    color: var(--text-inverse);
    min-width: 60px;

    &:hover {
      background: var(--primary-dark);
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

// 工具栏操作区
.toolbar-actions {
  @include flex-center;
  gap: var(--spacing-sm);
}

// 响应式设计
@include responsive(md) {
  .md-toolbar {
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
}

@include responsive(sm) {
  .md-toolbar {
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

.md-toolbar {
  animation: toolbarSlideIn 0.3s var(--transition-timing);
}

// 聚焦状态优化
.toolbar-btn:focus-visible {
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
    background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
    color: var(--text-inverse);
  }

  .toolbar-btn--toggle {
    background: var(--gradient-primary);

    &:hover {
      background: var(--primary-light);
    }
  }
}
</style>
