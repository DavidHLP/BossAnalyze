<script setup lang="ts">
import ToastModal from '@/components/toast-modal/toastModal.vue'
import Empty from '@/components/empty.vue'
import { Codemirror } from 'vue-codemirror'
import { cssLanguage } from '@codemirror/lang-css'
import { marks } from './constant'
import {
  step,
  setStep,
  useAvatar,
  usePrimaryBGColor,
  useCustomFont,
  useCustomCSS,
  usePrimaryColor,
  useAdjust,
  useFollowRoll,
  restResumeContent,
} from './hook'

import { useResumeType } from '../../hook'

const emits = defineEmits(['upload-avatar', 'html-convert'])

const { resumeType } = useResumeType()
const { cssDialog, cssText, toggleDialog, setStyle, removeStyle } = useCustomCSS(resumeType.value)
const { color, setColor } = usePrimaryColor(resumeType.value)
const { fontOptions, font, setFont } = useCustomFont(resumeType.value)
const { setAvatar } = useAvatar(emits)
const { primaryColor, setPrimaryColor } = usePrimaryBGColor(resumeType.value)
const { adjustMargin, visible, confirmAdjustment, properties } = useAdjust(resumeType.value)
const { followRoll, setFollowRoll } = useFollowRoll()
</script>

<template>
  <div class="compact-tabbar">
    <!-- 缩放控制区 -->
    <div class="zoom-control-bar">
      <el-slider
        v-model="step"
        @change="setStep"
        :step="10"
        :marks="marks"
        show-stops
        size="small"
        class="zoom-slider"
      />
      <span class="zoom-value">{{ step }}%</span>
    </div>

    <!-- 工具按钮栏 -->
    <div class="tools-bar">
      <!-- 头像上传 -->
      <el-tooltip content="上传头像" placement="top" effect="dark">
        <label class="tool-btn">
          <i class="iconfont icon-zhengjian"></i>
          <input type="file" id="upload-avatar" accept=".png,.jpg,.jpeg" @change="setAvatar" />
        </label>
      </el-tooltip>

      <!-- 跟随滚动 -->
      <el-tooltip content="跟随滚动" placement="top" effect="dark">
        <div class="tool-btn switch-btn">
          <i class="iconfont icon-scroll"></i>
          <el-switch
            v-model="followRoll"
            @change="setFollowRoll"
            size="small"
            class="mini-switch"
          />
        </div>
      </el-tooltip>

      <!-- 重置 -->
      <el-popconfirm
        title="确定重置所有内容？"
        @confirm="restResumeContent(resumeType)"
        placement="top"
        width="200"
      >
        <template #reference>
          <div class="tool-btn reset-btn">
            <i class="iconfont icon-refresh"></i>
          </div>
        </template>
      </el-popconfirm>
    </div>
  </div>

  <!-- CSS编辑器对话框 -->
  <ToastModal
    v-if="cssDialog"
    :flag="cssDialog"
    @close="cssDialog = false"
    width="480px"
    class="css-editor-modal"
  >
    <template #header>
      <div class="modal-header">
        <div class="header-icon">
          <i class="iconfont icon-diy"></i>
        </div>
        <div class="header-content">
          <h3 class="header-title">自定义CSS样式</h3>
          <p class="header-description">编写CSS让它应用到简历模板上</p>
        </div>
      </div>
    </template>

    <div class="css-editor-container">
      <codemirror
        v-model="cssText"
        autofocus
        :style="{ minHeight: '320px', maxHeight: '480px' }"
        :indent-with-tab="true"
        :extensions="[cssLanguage]"
        placeholder="例如: .jufe h2 { color: #3b82f6; font-weight: bold; }"
        class="css-editor"
      />
    </div>

    <div class="modal-actions">
      <button class="action-button primary-button" @click="setStyle">
        <i class="iconfont icon-check"></i>
        应用样式
      </button>
      <button class="action-button secondary-button" @click="removeStyle">
        <i class="iconfont icon-refresh"></i>
        重置样式
      </button>
    </div>
  </ToastModal>

  <!-- 边距调整对话框 -->
  <ToastModal
    v-if="visible"
    :flag="visible"
    @close="confirmAdjustment"
    :width="properties.length ? '580px' : '360px'"
    class="margin-adjust-modal"
  >
    <template #header>
      <div class="modal-header">
        <div class="header-icon">
          <i class="iconfont icon-adjust"></i>
        </div>
        <div class="header-content">
          <h3 class="header-title">边距调整</h3>
          <p class="header-description">调整简历元素的上下边距</p>
        </div>
      </div>
    </template>

    <div class="margin-adjust-container" v-if="properties.length">
      <div class="properties-table">
        <div class="table-header">
          <div class="header-cell element-name">元素名称</div>
          <div class="header-cell margin-top">上边距 (px)</div>
          <div class="header-cell margin-bottom">下边距 (px)</div>
        </div>

        <div class="table-body">
          <div class="table-row" v-for="(property, idx) in properties" :key="idx">
            <div class="table-cell element-info">
              <div class="element-name">{{ property.name }}</div>
              <div class="element-meta">{{ property.className || property.tagName }}</div>
            </div>
            <div class="table-cell input-cell">
              <el-input-number
                v-model="property.marginTop"
                size="small"
                :min="0"
                :step="1"
                class="margin-input"
              />
            </div>
            <div class="table-cell input-cell">
              <el-input-number
                v-model="property.marginBottom"
                size="small"
                :min="0"
                :step="1"
                class="margin-input"
              />
            </div>
          </div>
        </div>
      </div>

      <div class="adjust-tips">
        <i class="iconfont icon-info"></i>
        <span>只显示简历模板中已经使用的元素</span>
      </div>
    </div>

    <Empty v-else title="简历中还没有内容" description="可以先在编辑器中写点内容哦" />
  </ToastModal>
</template>

<style lang="scss" scoped>
@use '@/assets/style/variables.scss' as *;

// ==============================================
// 紧凑型工具栏
// ==============================================
.compact-tabbar {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  padding: 16px 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;

  // 暗色主题
  [data-theme='dark'] & {
    background: rgba(31, 41, 55, 0.98);
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }
}

// ==============================================
// 缩放控制区
// ==============================================
.zoom-control-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 6px 0;

  .zoom-slider {
    flex: 1;

    :deep(.el-slider__runway) {
      background: rgba(0, 0, 0, 0.06);
      height: 6px;
      border-radius: 3px;
      border: none;
    }

    :deep(.el-slider__bar) {
      background: linear-gradient(90deg, #3b82f6, #1d4ed8);
      border-radius: 3px;
    }

    :deep(.el-slider__button) {
      background: #ffffff;
      border: 2px solid #3b82f6;
      width: 16px;
      height: 16px;
      box-shadow: 0 2px 6px rgba(59, 130, 246, 0.2);
      transition: all 0.2s ease;

      &:hover {
        transform: scale(1.1);
        box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
      }
    }

    :deep(.el-slider__stop) {
      background: #3b82f6;
      width: 2px;
      height: 2px;
      border-radius: 1px;
    }

    :deep(.el-slider__marks-text) {
      font-size: 10px;
      color: rgba(0, 0, 0, 0.6);
      font-weight: 500;
    }

    // 暗色主题
    [data-theme='dark'] & {
      :deep(.el-slider__runway) {
        background: rgba(255, 255, 255, 0.1);
      }

      :deep(.el-slider__button) {
        background: #1f2937;
        border-color: #60a5fa;
      }

      :deep(.el-slider__marks-text) {
        color: rgba(255, 255, 255, 0.6);
      }
    }
  }

  .zoom-value {
    font-size: 12px;
    font-weight: 600;
    color: #3b82f6;
    background: rgba(59, 130, 246, 0.1);
    padding: 4px 8px;
    border-radius: 4px;
    min-width: 40px;
    text-align: center;
    border: 1px solid rgba(59, 130, 246, 0.2);

    [data-theme='dark'] & {
      color: #60a5fa;
      background: rgba(96, 165, 250, 0.1);
      border-color: rgba(96, 165, 250, 0.2);
    }
  }
}

// ==============================================
// 工具按钮栏
// ==============================================
.tools-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #374151;
  font-size: 14px;
  position: relative;

  &:hover {
    background: rgba(59, 130, 246, 0.1);
    border-color: rgba(59, 130, 246, 0.2);
    color: #3b82f6;
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  }

  // 隐藏文件上传input
  input[type="file"] {
    position: absolute;
    opacity: 0;
    width: 0;
    height: 0;
    pointer-events: none;
  }

  // 暗色主题
  [data-theme='dark'] & {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(255, 255, 255, 0.1);
    color: #d1d5db;

    &:hover {
      background: rgba(96, 165, 250, 0.1);
      border-color: rgba(96, 165, 250, 0.2);
      color: #60a5fa;
    }
  }
}

// 开关按钮特殊样式
.switch-btn {
  width: auto;
  padding: 0 8px;
  gap: 6px;

  .mini-switch {
    :deep(.el-switch__core) {
      width: 28px;
      height: 16px;
      border-radius: 8px;
      border: none;
      background: rgba(0, 0, 0, 0.2);

      &::after {
        width: 12px;
        height: 12px;
        border-radius: 6px;
      }

      &.is-checked {
        background: #3b82f6;
      }
    }

    [data-theme='dark'] & {
      :deep(.el-switch__core) {
        background: rgba(255, 255, 255, 0.2);

        &.is-checked {
          background: #60a5fa;
        }
      }
    }
  }
}

// 重置按钮特殊样式
.reset-btn {
  &:hover {
    background: rgba(239, 68, 68, 0.1);
    border-color: rgba(239, 68, 68, 0.2);
    color: #ef4444;
  }

  [data-theme='dark'] & {
    &:hover {
      background: rgba(248, 113, 113, 0.1);
      border-color: rgba(248, 113, 113, 0.2);
      color: #f87171;
    }
  }
}

// ==============================================
// 响应式设计
// ==============================================
@media (max-width: 768px) {
  .compact-tabbar {
    padding: 12px 12px 8px;
    gap: 10px;
  }

  .zoom-control-bar {
    gap: 8px;
    padding: 4px 0;
  }

  .tools-bar {
    gap: 6px;
  }

  .tool-btn {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }

  .switch-btn {
    padding: 0 6px;
  }
}

@media (max-width: 480px) {
  .compact-tabbar {
    padding: 10px 8px 8px;
    gap: 8px;
  }

  .zoom-control-bar {
    gap: 6px;
    padding: 3px 0;
  }
}

// ==============================================
// 对话框样式
// ==============================================
.css-editor-modal,
.margin-adjust-modal {
  :deep(.toast-modal) {
    .modal-content {
      border-radius: var(--radius-lg);
      overflow: hidden;
      box-shadow: var(--shadow-xl);
    }
  }
}

.modal-header {
  @include flex-start;
  gap: var(--spacing-md);
  padding: var(--spacing-xl);
  background: var(--gradient-primary);
  color: var(--text-inverse);
  border-bottom: 1px solid var(--border-light);
}

.header-icon {
  @include flex-center;
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-lg);

  i {
    font-size: 24px;
    color: var(--text-inverse);
  }
}

.header-content {
  flex: 1;

  .header-title {
    margin: 0 0 var(--spacing-xs) 0;
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    color: var(--text-inverse);
  }

  .header-description {
    margin: 0;
    font-size: var(--font-size-sm);
    color: rgba(255, 255, 255, 0.9);
    opacity: 0.9;
  }
}

.css-editor-container {
  padding: var(--spacing-xl);

  .css-editor {
    border: 1px solid var(--border-light);
    border-radius: var(--radius-md);
    overflow: hidden;

    :deep(.cm-editor) {
      font-family: var(--font-family-mono);
    }
  }
}

.modal-actions {
  @include flex-center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg) var(--spacing-xl);
  background: var(--background-secondary);
  border-top: 1px solid var(--border-light);
}

.action-button {
  @include flex-center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-lg);
  border: none;
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--transition-normal) var(--transition-timing);

  i {
    font-size: var(--font-size-sm);
  }

  &:hover {
    @include hover-lift;
  }
}

.primary-button {
  background: var(--gradient-primary);
  color: var(--text-inverse);

  &:hover {
    opacity: 0.9;
  }
}

.secondary-button {
  background: var(--background-card);
  color: var(--text-primary);
  border: 1px solid var(--border-light);

  &:hover {
    border-color: var(--border-medium);
    background: var(--background-primary);
  }
}

.margin-adjust-container {
  padding: var(--spacing-xl);
}

.properties-table {
  background: var(--background-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  margin-bottom: var(--spacing-lg);
}

.table-header {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: var(--spacing-md);
  background: var(--background-secondary);
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border-light);

  .header-cell {
    font-weight: var(--font-weight-semibold);
    color: var(--text-primary);
    font-size: var(--font-size-sm);
  }
}

.table-body {
  max-height: 300px;
  overflow-y: auto;
}

.table-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border-light);
  transition: background-color var(--transition-fast) var(--transition-timing);

  &:hover {
    background: var(--primary-lighter);
  }

  &:last-child {
    border-bottom: none;
  }
}

.table-cell {
  @include flex-center;

  &.element-info {
    justify-content: flex-start;

    .element-name {
      font-weight: var(--font-weight-medium);
      color: var(--text-primary);
      margin-bottom: 2px;
    }

    .element-meta {
      font-size: var(--font-size-xs);
      color: var(--text-secondary);
    }
  }

  &.input-cell {
    .margin-input {
      width: 100%;

      :deep(.el-input-number) {
        .el-input__wrapper {
          border-radius: var(--radius-sm);
        }
      }
    }
  }
}

.adjust-tips {
  @include flex-start;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background: var(--primary-lighter);
  border: 1px solid var(--primary-color);
  border-radius: var(--radius-md);
  color: var(--primary-color);

  i {
    font-size: var(--font-size-sm);
  }

  span {
    font-size: var(--font-size-sm);
    font-weight: var(--font-weight-medium);
  }
}
</style>
