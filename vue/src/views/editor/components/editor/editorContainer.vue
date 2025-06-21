<script setup lang="ts">
import RichEditor from './rich-editor/editor.vue'
import MDEditor from './md-editor/editor.vue'
import { useResumeType, useAvatar } from '../../hook'
import { reactiveWritable, useMoveLayout, injectWritableModeAvatarEvent } from './hook'

const { resumeType } = useResumeType()
const { left, down } = useMoveLayout()

const { setAvatar } = useAvatar(resumeType.value)
const { writable } = reactiveWritable(resumeType.value)

injectWritableModeAvatarEvent(writable, setAvatar)
</script>

<template>
  <div class="editor-container">
    <!-- 编辑器内容区 -->
    <div class="editor-content">
      <RichEditor :left="left" v-if="writable" />
      <MDEditor :left="left" v-if="!writable" />
    </div>

    <!-- 分割线拖拽器 -->
    <div class="editor-resizer" @mousedown="down">
      <div class="resizer-handle">
        <div class="resizer-dots">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/style/variables.scss' as *;
@use '@/assets/style/darktheme.scss' as *;

.editor-container {
  position: relative;
  height: 100%;
  background: var(--background-primary);
  border-radius: var(--radius-lg);
  overflow: hidden;
  @include card-shadow;

  // 动画进入效果
  animation: editorSlideIn 0.3s var(--transition-timing);
}

.editor-content {
  position: relative;
  height: 100%;
  background: var(--background-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all var(--transition-normal) var(--transition-timing);

  // 编辑器模式切换时的动画
  &.mode-switching {
    opacity: 0.8;
    transform: scale(0.99);
  }
}

// 分割线拖拽器重新设计
.editor-resizer {
  position: absolute;
  top: 0;
  right: -var(--spacing-md);
  width: var(--spacing-lg);
  height: 100%;
  z-index: var(--z-sticky);
  cursor: col-resize;
  @include flex-center;

  // 悬浮时显示
  opacity: 0;
  transition: opacity var(--transition-normal) var(--transition-timing);

  &:hover {
    opacity: 1;
  }

  // 当容器悬浮时也显示
  .editor-container:hover & {
    opacity: 0.6;
  }
}

.resizer-handle {
  width: 4px;
  height: 60px;
  background: var(--primary-color);
  border-radius: var(--radius-xl);
  position: relative;
  @include flex-center;
  box-shadow: var(--shadow-md);
  transition: all var(--transition-normal) var(--transition-timing);

  &:hover {
    background: var(--primary-dark);
    width: 6px;
    height: 80px;
    box-shadow: var(--shadow-lg);
  }

  &:active {
    background: var(--primary-dark);
    width: 8px;
    height: 100px;
  }
}

.resizer-dots {
  @include flex-column;
  gap: 2px;

  .dot {
    width: 2px;
    height: 2px;
    background: var(--background-card);
    border-radius: var(--radius-full);
    opacity: 0.8;
    transition: all var(--transition-fast) var(--transition-timing);
  }

  .resizer-handle:hover & .dot {
    width: 3px;
    height: 3px;
    opacity: 1;
  }
}

// 暗色主题适配
[data-theme='dark'] {
  .editor-container {
    background: var(--background-secondary);
  }

  .editor-content {
    background: var(--background-card);
  }

  .resizer-handle {
    background: var(--primary-color);

    &:hover,
    &:active {
      background: var(--primary-light);
    }
  }

  .dot {
    background: var(--text-secondary);
  }
}

// 响应式设计
@include responsive(md) {
  .editor-resizer {
    right: -var(--spacing-sm);
    width: var(--spacing-md);
  }

  .resizer-handle {
    width: 3px;
    height: 40px;

    &:hover {
      width: 4px;
      height: 50px;
    }

    &:active {
      width: 5px;
      height: 60px;
    }
  }
}

@include responsive(sm) {
  .editor-resizer {
    display: none; // 在小屏幕上隐藏拖拽功能
  }
}

// 编辑器加载动画
@keyframes editorSlideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 模式切换动画
.editor-mode-transition {
  transition: all 0.3s var(--transition-timing);
}

// 聚焦状态增强
.editor-container:focus-within {
  box-shadow:
    var(--shadow-lg),
    0 0 0 3px rgba(59, 130, 246, 0.1);

  .editor-resizer {
    opacity: 0.8;
  }
}

// 滚动条样式统一
.editor-content {
  ::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  ::-webkit-scrollbar-track {
    background: var(--background-secondary);
    border-radius: var(--radius-sm);
  }

  ::-webkit-scrollbar-thumb {
    background: var(--border-medium);
    border-radius: var(--radius-sm);

    &:hover {
      background: var(--border-dark);
    }
  }
}
</style>
