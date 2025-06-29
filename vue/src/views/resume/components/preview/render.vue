<script setup lang="ts">
import TabBar from '../tabbar/tabbar.vue'
import { useRenderHTML, useResumeType } from '../../hook'
import { useThemeConfig } from '@/common/global'
import { step, pageSize } from '../tabbar/hook'

defineEmits(['upload-avatar', 'html-convert'])

const { resumeType } = useResumeType()
const { isDark } = useThemeConfig()
const { renderDOM } = useRenderHTML(resumeType)
</script>

<template>
  <div class="preview-container" :class="{ 'preview-container--dark': isDark }">
    <!-- 标签栏区域 -->
    <div class="preview-header">
      <TabBar
        @html-convert="cnt => $emit('html-convert', cnt)"
        @upload-avatar="path => $emit('upload-avatar', path)"
      />
    </div>

    <!-- 预览内容区域 -->
    <div class="preview-content">
      <!-- 渲染内容容器 -->
      <div class="preview-viewport">
        <div ref="renderDOM" class="markdown-transform-html content-renderer"></div>

        <!-- 分页预览区域 -->
        <div
          class="page-preview"
          :style="{
            transform: `translateY(-${((100 - step) / 100) * 1123 * (pageSize / 2)}px) scale(${step / 100})`
          }"
        >
        </div>

        <!-- 内容遮罩层（隐藏的参考DOM） -->
        <div ref="referenceDOM" class="reference-dom">
        </div>
      </div>

      <!-- 预览装饰效果 -->
      <div class="preview-decoration">
        <div class="decoration-gradient"></div>
        <div class="decoration-dots"></div>
      </div>
    </div>

    <!-- 加载状态指示器 -->
    <div class="preview-loading" v-if="false">
      <div class="loading-spinner"></div>
      <p class="loading-text">渲染中...</p>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/style/variables.scss' as *;

// ==============================================
// 预览容器主体
// ==============================================
.preview-container {
  height: 100vh;
  background: var(--background-card);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;

  // 毛玻璃效果
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg,
      rgba(59, 130, 246, 0.03) 0%,
      rgba(59, 130, 246, 0.01) 50%,
      rgba(139, 92, 246, 0.03) 100%);
    pointer-events: none;
    z-index: 0;
  }

  // 暗色主题
  &--dark {
    background: var(--background-card);

    &::before {
      background: linear-gradient(135deg,
        rgba(59, 130, 246, 0.05) 0%,
        rgba(59, 130, 246, 0.02) 50%,
        rgba(139, 92, 246, 0.05) 100%);
    }
  }
}

// ==============================================
// 预览头部区域
// ==============================================
.preview-header {
  flex-shrink: 0;
  position: relative;
  z-index: 10;

  // 头部装饰线
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg,
      transparent 0%,
      var(--border-light) 20%,
      var(--border-light) 80%,
      transparent 100%);
  }
}

// ==============================================
// 预览内容区域
// ==============================================
.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
  z-index: 1;
}

// ==============================================
// 预览视口
// ==============================================
.preview-viewport {
  height: 100%;
  overflow: auto;
  position: relative;
  padding: var(--spacing-lg);

  // 自定义滚动条
  &::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }

  &::-webkit-scrollbar-track {
    background: var(--background-secondary);
    border-radius: var(--radius-lg);
    margin: var(--spacing-md) 0;
  }

  &::-webkit-scrollbar-thumb {
    background: linear-gradient(45deg, var(--primary-color), var(--primary-dark));
    border-radius: var(--radius-lg);
    border: 2px solid var(--background-secondary);

    &:hover {
      background: linear-gradient(45deg, var(--primary-dark), var(--primary-color));
    }
  }

  &::-webkit-scrollbar-corner {
    background: var(--background-secondary);
  }
}

// ==============================================
// 内容渲染器
// ==============================================
.content-renderer {
  background: var(--background-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-light);
  padding: var(--spacing-xxl);
  margin: 0 auto;
  max-width: 800px;
  min-height: calc(100vh - 200px);
  position: relative;
  z-index: 2;

  // 进入动画
  animation: contentFadeIn 0.6s var(--transition-timing);

  // 悬浮效果
  transition: all var(--transition-normal) var(--transition-timing);

  &:hover {
    box-shadow: var(--shadow-lg);
    transform: translateY(-2px);
  }

  // 内容样式增强
  :deep(.markdown-body) {
    font-family: var(--font-family-sans);
    line-height: var(--line-height-relaxed);
    color: var(--text-primary);

    // 标题样式
    h1, h2, h3, h4, h5, h6 {
      margin-top: var(--spacing-xl);
      margin-bottom: var(--spacing-md);
      font-weight: var(--font-weight-semibold);
      line-height: var(--line-height-tight);

      &:first-child {
        margin-top: 0;
      }
    }

    h1 {
      font-size: var(--font-size-xxxl);
      background: var(--gradient-primary);
      background-clip: text;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      position: relative;

      &::after {
        content: '';
        position: absolute;
        bottom: -var(--spacing-sm);
        left: 0;
        width: 60px;
        height: 3px;
        background: var(--gradient-primary);
        border-radius: var(--radius-sm);
      }
    }

    h2 {
      font-size: var(--font-size-xxl);
      color: var(--primary-color);
      position: relative;
      padding-left: var(--spacing-md);

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 4px;
        height: 20px;
        background: var(--gradient-primary);
        border-radius: var(--radius-sm);
      }
    }

    h3 {
      font-size: var(--font-size-xl);
      color: var(--primary-dark);
    }

    // 段落样式
    p {
      margin-bottom: var(--spacing-md);
      color: var(--text-primary);
    }

    // 列表样式
    ul, ol {
      margin: var(--spacing-md) 0;
      padding-left: var(--spacing-xl);

      li {
        margin: var(--spacing-sm) 0;
        line-height: var(--line-height-relaxed);
      }
    }

    ul li {
      position: relative;
      list-style: none;

      &::before {
        content: '';
        position: absolute;
        left: -var(--spacing-md);
        top: 10px;
        width: 6px;
        height: 6px;
        background: var(--primary-color);
        border-radius: var(--radius-full);
      }
    }

    // 引用块样式
    blockquote {
      margin: var(--spacing-lg) 0;
      padding: var(--spacing-md) var(--spacing-lg);
      background: var(--primary-lighter);
      border-left: 4px solid var(--primary-color);
      border-radius: var(--radius-md);
      position: relative;

      &::before {
        content: '"';
        position: absolute;
        top: -10px;
        left: var(--spacing-md);
        font-size: 60px;
        color: var(--primary-color);
        opacity: 0.3;
        font-family: Georgia, serif;
        line-height: 1;
      }

      p {
        margin: 0;
        font-style: italic;
        color: var(--text-primary);
      }
    }

    // 代码样式
    code {
      background: var(--background-secondary);
      color: var(--primary-color);
      padding: 2px var(--spacing-sm);
      border-radius: var(--radius-sm);
      font-family: var(--font-family-mono);
      font-size: 0.9em;
      border: 1px solid var(--border-light);
    }

    pre {
      background: var(--background-secondary);
      border: 1px solid var(--border-light);
      border-radius: var(--radius-md);
      padding: var(--spacing-lg);
      margin: var(--spacing-lg) 0;
      overflow-x: auto;
      box-shadow: var(--shadow-sm);

      code {
        background: none;
        border: none;
        padding: 0;
        color: var(--text-primary);
      }
    }

    // 表格样式
    table {
      width: 100%;
      border-collapse: collapse;
      margin: var(--spacing-lg) 0;
      background: var(--background-card);
      border-radius: var(--radius-md);
      overflow: hidden;
      box-shadow: var(--shadow-sm);

      th, td {
        padding: var(--spacing-md);
        text-align: left;
        border-bottom: 1px solid var(--border-light);
      }

      th {
        background: var(--background-secondary);
        font-weight: var(--font-weight-semibold);
        color: var(--text-primary);
      }

      tr:hover {
        background: var(--primary-lighter);
      }
    }

    // 图片样式
    img {
      max-width: 100%;
      height: auto;
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-sm);
      margin: var(--spacing-md) 0;
      transition: all var(--transition-normal) var(--transition-timing);

      &:hover {
        box-shadow: var(--shadow-lg);
        transform: scale(1.02);
      }
    }

    // 链接样式
    a {
      color: var(--primary-color);
      text-decoration: none;
      position: relative;
      transition: all var(--transition-normal) var(--transition-timing);

      &::after {
        content: '';
        position: absolute;
        bottom: -2px;
        left: 0;
        width: 0;
        height: 2px;
        background: var(--gradient-primary);
        transition: width var(--transition-normal) var(--transition-timing);
      }

      &:hover {
        color: var(--primary-dark);

        &::after {
          width: 100%;
        }
      }
    }

    // 分割线样式
    hr {
      border: none;
      height: 1px;
      background: linear-gradient(90deg,
        transparent 0%,
        var(--border-medium) 20%,
        var(--border-medium) 80%,
        transparent 100%);
      margin: var(--spacing-xxl) 0;
    }
  }
}

// ==============================================
// 分页预览
// ==============================================
.page-preview {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 1;
  transition: transform var(--transition-slow) var(--transition-timing);
  transform-origin: center top;

  // 预览边框
  &::before {
    content: '';
    position: absolute;
    top: var(--spacing-lg);
    left: var(--spacing-lg);
    right: var(--spacing-lg);
    bottom: var(--spacing-lg);
    border: 2px dashed var(--primary-color);
    border-radius: var(--radius-lg);
    opacity: 0.3;
    animation: dashMove 3s linear infinite;
  }
}

// ==============================================
// 参考DOM（隐藏）
// ==============================================
.reference-dom {
  position: absolute;
  left: -9999px;
  top: -9999px;
  visibility: hidden;
  pointer-events: none;
}

// ==============================================
// 预览装饰效果
// ==============================================
.preview-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.decoration-gradient {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at center,
    rgba(59, 130, 246, 0.1) 0%,
    transparent 50%);
  animation: gradientRotate 20s linear infinite;
  opacity: 0.5;
}

.decoration-dots {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: radial-gradient(2px 2px at 20px 30px, var(--primary-color), transparent),
                    radial-gradient(2px 2px at 40px 70px, var(--primary-light), transparent),
                    radial-gradient(1px 1px at 90px 40px, var(--primary-color), transparent);
  background-repeat: repeat;
  background-size: 120px 120px;
  opacity: 0.1;
  animation: dotsMove 30s linear infinite;
}

// ==============================================
// 加载状态
// ==============================================
.preview-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  @include flex-center;
  flex-direction: column;
  background: rgba(var(--background-card), 0.9);
  backdrop-filter: blur(10px);
  z-index: 100;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-light);
  border-top: 3px solid var(--primary-color);
  border-radius: var(--radius-full);
  animation: spin 1s linear infinite;
  margin-bottom: var(--spacing-md);
}

.loading-text {
  color: var(--text-secondary);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  margin: 0;
}

// ==============================================
// 响应式设计
// ==============================================
@include responsive(md) {
  .preview-viewport {
    padding: var(--spacing-md);
  }

  .content-renderer {
    padding: var(--spacing-lg);
    margin: 0;
    border-radius: var(--radius-md);
  }

  .page-preview::before {
    top: var(--spacing-md);
    left: var(--spacing-md);
    right: var(--spacing-md);
    bottom: var(--spacing-md);
  }
}

@include responsive(sm) {
  .preview-viewport {
    padding: var(--spacing-sm);
  }

  .content-renderer {
    padding: var(--spacing-md);
    min-height: calc(100vh - 120px);

    :deep(.markdown-body) {
      h1 {
        font-size: var(--font-size-xxl);
      }

      h2 {
        font-size: var(--font-size-xl);
      }

      h3 {
        font-size: var(--font-size-lg);
      }
    }
  }
}

// ==============================================
// 动画定义
// ==============================================
@keyframes contentFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes dashMove {
  from {
    stroke-dashoffset: 0;
  }
  to {
    stroke-dashoffset: 20px;
  }
}

@keyframes gradientRotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes dotsMove {
  from {
    background-position: 0 0;
  }
  to {
    background-position: 120px 120px;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// ==============================================
// 暗色主题适配
// ==============================================
.preview-container--dark {
  .content-renderer {
    border-color: var(--border-dark);

    :deep(.markdown-body) {
      blockquote {
        background: rgba(59, 130, 246, 0.1);
        border-left-color: var(--primary-color);

        &::before {
          color: var(--primary-light);
        }
      }

      code {
        background: var(--background-primary);
        color: var(--primary-light);
        border-color: var(--border-medium);
      }

      pre {
        background: var(--background-primary);
        border-color: var(--border-medium);
      }

      table {
        th {
          background: var(--background-primary);
        }

        tr:hover {
          background: rgba(59, 130, 246, 0.1);
        }
      }
    }
  }

  .page-preview::before {
    border-color: var(--primary-light);
  }

  .decoration-dots {
    background-image: radial-gradient(2px 2px at 20px 30px, var(--primary-light), transparent),
                      radial-gradient(2px 2px at 40px 70px, var(--primary-color), transparent),
                      radial-gradient(1px 1px at 90px 40px, var(--primary-light), transparent);
  }
}
</style>
