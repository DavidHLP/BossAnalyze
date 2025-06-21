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
  restResumeContent
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
  <div class="tabbar-container">
    <!-- 工具栏背景装饰 -->
    <div class="tabbar-decoration">
      <div class="decoration-gradient"></div>
      <div class="decoration-pattern"></div>
    </div>

    <!-- 缩放滑块区域 -->
    <div class="slider-section">
      <div class="slider-wrapper">
        <div class="slider-label">
          <i class="iconfont icon-zoom"></i>
          <span>预览缩放</span>
        </div>
    <el-slider
      v-model="step"
      @change="setStep"
      :step="10"
          :marks="marks"
      show-stops
          size="small"
          class="tabbar-slider"
        />
        <div class="slider-value">{{ step }}%</div>
      </div>
    </div>

    <!-- 工具按钮区域 -->
    <div class="tools-section">
      <div class="tools-wrapper">
        <!-- 调整边距 -->
        <el-tooltip content="调整元素上下边距" effect="light" placement="top">
          <div class="tool-item tool-button" @click="adjustMargin">
            <i class="iconfont icon-adjust"></i>
            <span class="tool-label">边距</span>
          </div>
      </el-tooltip>

        <!-- 上传头像 -->
      <el-tooltip
          content="上传前请确保编辑器中存在 ![个人头像](...) 占位符"
        effect="light"
          placement="top"
      >
          <label for="upload-avatar" class="tool-item tool-button">
          <i class="iconfont icon-zhengjian"></i>
            <span class="tool-label">头像</span>
            <input type="file" id="upload-avatar" accept=".png,.jpg,.jpeg" @change="setAvatar" />
        </label>
      </el-tooltip>

        <!-- 自定义CSS -->
        <el-tooltip content="编写自定义CSS样式" effect="light" placement="top">
          <div class="tool-item tool-button" @click="toggleDialog">
            <i class="iconfont icon-diy"></i>
            <span class="tool-label">样式</span>
          </div>
        </el-tooltip>

        <!-- 字体颜色选择器 -->
        <div class="tool-item color-picker-wrapper">
          <div class="color-picker-label">
            <i class="iconfont icon-font"></i>
            <span>字体色</span>
          </div>
          <div class="color-picker-container">
            <el-color-picker
              v-model="color"
              @change="setColor"
              size="small"
              class="custom-color-picker"
            />
          </div>
      </div>

        <!-- 主题颜色选择器 -->
        <div class="tool-item color-picker-wrapper">
          <div class="color-picker-label">
            <i class="iconfont icon-palette"></i>
            <span>主题色</span>
      </div>
          <div class="color-picker-container">
            <el-color-picker
              v-model="primaryColor"
              @change="setPrimaryColor"
          size="small"
              class="custom-color-picker"
            />
          </div>
        </div>

        <!-- 字体选择器 -->
        <div class="tool-item font-selector-wrapper">
          <el-tooltip content="选择字体" effect="light" placement="top">
        <el-select
          v-model="font"
          @change="setFont"
          placement="bottom"
          size="small"
              class="font-selector"
        >
              <template #prefix>
                <i class="iconfont icon-font-family"></i>
              </template>
          <el-option
            v-for="item in fontOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-tooltip>
    </div>

        <!-- 跟随滚动开关 -->
        <div class="tool-item switch-wrapper">
          <el-tooltip content="跟随滚动预览" effect="light" placement="top">
            <div class="switch-container">
              <i class="iconfont icon-scroll"></i>
              <span class="switch-label">跟随</span>
              <el-switch
                v-model="followRoll"
                @change="setFollowRoll"
                size="small"
                class="custom-switch"
              />
            </div>
          </el-tooltip>
        </div>

        <!-- 重置按钮 -->
        <el-popconfirm
          width="280"
          confirm-button-text="确认重置"
          cancel-button-text="取消"
          title="确定要重置简历的所有内容和样式吗？"
          @confirm="restResumeContent(resumeType)"
          placement="top"
        >
          <template #reference>
            <div class="tool-item tool-button reset-button">
              <i class="iconfont icon-refresh"></i>
              <span class="tool-label">重置</span>
            </div>
          </template>
        </el-popconfirm>
      </div>
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
          <div
            class="table-row"
            v-for="(property, idx) in properties"
            :key="idx"
          >
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
// 工具栏容器
// ==============================================
.tabbar-container {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  background: var(--background-card);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-light);
  padding: var(--spacing-md) var(--spacing-lg);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal) var(--transition-timing);
  overflow: hidden;

  // 毛玻璃效果增强
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg,
      rgba(255, 255, 255, 0.95) 0%,
      rgba(255, 255, 255, 0.9) 100%);
    pointer-events: none;
    z-index: -1;
  }
}

// ==============================================
// 背景装饰
// ==============================================
.tabbar-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: -2;
  overflow: hidden;
}

.decoration-gradient {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at center,
    rgba(59, 130, 246, 0.08) 0%,
    transparent 50%);
  animation: decorationRotate 30s linear infinite;
}

.decoration-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    radial-gradient(1px 1px at 20px 15px, var(--primary-color), transparent),
    radial-gradient(1px 1px at 40px 35px, var(--primary-light), transparent);
  background-repeat: repeat;
  background-size: 80px 80px;
  opacity: 0.06;
}

// ==============================================
// 滑块区域
// ==============================================
.slider-section {
  margin-bottom: var(--spacing-md);
}

.slider-wrapper {
  @include flex-between;
  align-items: center;
  max-width: 600px;
  margin: 0 auto;
  gap: var(--spacing-lg);
}

.slider-label {
  @include flex-start;
  gap: var(--spacing-sm);
  color: var(--text-primary);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
  font-size: var(--font-size-md);

  i {
    font-size: var(--font-size-lg);
    color: var(--primary-color);
  }
}

.tabbar-slider {
  flex: 1;
  margin: 0 var(--spacing-md);

  :deep(.el-slider__runway) {
    background: var(--background-secondary);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-lg);
    height: 8px;
  }

  :deep(.el-slider__bar) {
    background: var(--gradient-primary);
    border-radius: var(--radius-lg);
  }

  :deep(.el-slider__button) {
    background: var(--background-card);
    border: 2px solid var(--primary-color);
    box-shadow: var(--shadow-md);
    width: 20px;
    height: 20px;
    transition: all var(--transition-normal) var(--transition-timing);

    &:hover {
      transform: scale(1.1);
      box-shadow: var(--shadow-lg);
    }
  }

  :deep(.el-slider__stop) {
    background: var(--primary-color);
    border-radius: var(--radius-full);
  }

  :deep(.el-slider__marks-text) {
    color: var(--text-secondary);
    font-size: var(--font-size-xs);
    font-weight: var(--font-weight-medium);
  }
}

.slider-value {
  background: var(--primary-light);
  color: var(--primary-color);
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  min-width: 50px;
  text-align: center;
  border: 1px solid var(--primary-color);
}

// ==============================================
// 工具区域
// ==============================================
.tools-section {
  border-top: 1px solid var(--border-light);
  padding-top: var(--spacing-md);
}

  .tools-wrapper {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: var(--spacing-xl);
    flex-wrap: wrap;
    padding: var(--spacing-sm) 0;
  }

// ==============================================
// 工具项通用样式
// ==============================================
.tool-item {
  position: relative;
  transition: all var(--transition-normal) var(--transition-timing);
}

  // ==============================================
  // 工具按钮
  // ==============================================
  .tool-button {
    @include flex-center;
    flex-direction: column;
    gap: var(--spacing-sm);
    padding: var(--spacing-md) var(--spacing-lg);
    background: var(--background-card);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-md);
    cursor: pointer;
    min-width: 80px;
    min-height: 60px;
    box-shadow: var(--shadow-sm);
    text-decoration: none;
    color: inherit;

  i {
    font-size: 18px;
    color: var(--primary-color);
    transition: all var(--transition-normal) var(--transition-timing);
  }

  .tool-label {
    font-size: var(--font-size-xs);
    color: var(--text-secondary);
    font-weight: var(--font-weight-medium);
  }

  &:hover {
    @include hover-lift;
    border-color: var(--primary-color);
    box-shadow: var(--shadow-md);

    i {
      color: var(--primary-dark);
      transform: scale(1.1);
    }

    .tool-label {
      color: var(--text-primary);
    }
  }

  // 隐藏文件上传input
  input[type="file"] {
    position: absolute;
    opacity: 0;
    width: 0;
    height: 0;
    pointer-events: none;
  }
}

// 重置按钮特殊样式
.reset-button {
  border-color: var(--error-color);

  i {
    color: var(--error-color);
  }

  &:hover {
    border-color: var(--error-color);
    background: rgba(239, 68, 68, 0.05);

    i {
      color: var(--error-color);
    }
  }
}

// ==============================================
// 颜色选择器
// ==============================================
  .color-picker-wrapper {
    @include flex-center;
    flex-direction: column;
    gap: var(--spacing-sm);
    padding: var(--spacing-md) var(--spacing-lg);
    background: var(--background-card);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);
    min-width: 90px;
    min-height: 60px;
  }

.color-picker-label {
  @include flex-center;
  gap: var(--spacing-xs);

  i {
    font-size: var(--font-size-sm);
    color: var(--primary-color);
  }

  span {
    font-size: var(--font-size-xs);
    color: var(--text-secondary);
    font-weight: var(--font-weight-medium);
  }
}

.color-picker-container {
  :deep(.el-color-picker) {
    .el-color-picker__trigger {
      border: 2px solid var(--border-light);
      border-radius: var(--radius-md);
      transition: all var(--transition-normal) var(--transition-timing);
      width: 28px;
      height: 28px;

      &:hover {
        border-color: var(--primary-color);
        transform: scale(1.05);
      }
    }
  }
}

// ==============================================
// 字体选择器
// ==============================================
  .font-selector-wrapper {
    padding: var(--spacing-md) var(--spacing-lg);
    background: var(--background-card);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);
    min-height: 60px;
    @include flex-center;

    .font-selector {
      min-width: 140px;

            :deep(.el-input) {
          .el-input__wrapper {
            border-radius: var(--radius-md);
            border: none;
            box-shadow: none;
            background: transparent;
            transition: all var(--transition-normal) var(--transition-timing);
            height: 36px;

        &:hover {
          border-color: var(--border-medium);
        }

        &.is-focus {
          border-color: var(--primary-color);
          box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
        }

        .el-input__prefix {
          color: var(--primary-color);
        }

        .el-input__inner {
          font-size: var(--font-size-sm);
        }
      }
    }
  }
}

// ==============================================
// 开关组件
// ==============================================
  .switch-wrapper {
    padding: var(--spacing-md) var(--spacing-lg);
    background: var(--background-card);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);
    min-height: 60px;
    @include flex-center;
  }

.switch-container {
  @include flex-center;
  gap: var(--spacing-sm);

  i {
    font-size: var(--font-size-sm);
    color: var(--primary-color);
  }

  .switch-label {
    font-size: var(--font-size-xs);
    color: var(--text-secondary);
    font-weight: var(--font-weight-medium);
  }

  .custom-switch {
    :deep(.el-switch__core) {
      border: 1px solid var(--border-light);
      background: var(--background-secondary);
      height: 20px;
      min-width: 36px;

      &::after {
        width: 16px;
        height: 16px;
      }

      &.is-checked {
        background: var(--primary-color);
        border-color: var(--primary-color);
      }
    }
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

// 模态框头部
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

// CSS编辑器
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

// 模态框操作按钮
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

// ==============================================
// 边距调整表格
// ==============================================
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

// ==============================================
// 响应式设计
// ==============================================
@include responsive(lg) {
  .tabbar-container {
    padding: var(--spacing-md) var(--spacing-lg);
  }

  .tools-wrapper {
    gap: var(--spacing-md);
  }

  .tool-button {
    min-width: 60px;
    padding: var(--spacing-sm);

    i {
      font-size: 18px;
    }

    .tool-label {
      font-size: 10px;
    }
  }

  .color-picker-wrapper {
    min-width: 70px;
    padding: var(--spacing-sm);
  }
}

@include responsive(md) {
  .slider-wrapper {
    flex-direction: column;
    gap: var(--spacing-md);
  }

  .tabbar-slider {
    width: 100%;
    margin: 0;
  }

  .tools-wrapper {
    justify-content: center;
    gap: var(--spacing-sm);
  }

  .font-selector {
    min-width: 120px;
  }
}

@include responsive(sm) {
  .tabbar-container {
    padding: var(--spacing-sm) var(--spacing-lg);
  }

  .tools-wrapper {
    gap: var(--spacing-lg);
    padding: var(--spacing-md) 0;
  }

  .tool-button {
    min-width: 60px;
    min-height: 55px;
    padding: var(--spacing-sm) var(--spacing-md);

    .tool-label {
      display: none;
    }
  }

  .color-picker-wrapper {
    min-width: 60px;
    min-height: 55px;
    padding: var(--spacing-sm) var(--spacing-md);

    .color-picker-label span {
      display: none;
    }
  }

  .switch-wrapper {
    min-height: 55px;
    padding: var(--spacing-sm) var(--spacing-md);
  }

  .switch-container .switch-label {
    display: none;
  }

  .font-selector-wrapper {
    min-height: 55px;
    padding: var(--spacing-sm) var(--spacing-md);
  }

  .font-selector {
    min-width: 120px;
  }
}

// ==============================================
// 动画定义
// ==============================================
@keyframes decorationRotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
