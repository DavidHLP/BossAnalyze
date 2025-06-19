<template>
  <el-dialog
    v-model="dialogVisible"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    destroy-on-close
    center
    class="security-dialog"
  >
    <template #header>
      <div class="dialog-title">
        <el-icon class="title-icon"><WarningFilled /></el-icon>
        <span>安全验证</span>
      </div>
    </template>

    <div class="security-content">
      <!-- 警告区域 -->
      <div class="warning-section">
        <div class="warning-icon-wrapper">
          <el-icon class="warning-icon"><Warning /></el-icon>
        </div>
        <h4 class="warning-title">操作风险提醒</h4>
        <p class="warning-message">{{ warningMessage }}</p>
      </div>

      <!-- 表单区域 -->
      <el-form class="security-form" label-position="top">
        <el-form-item class="security-field">
          <template #label>
            <el-icon class="field-icon"><Lock /></el-icon>
            <span>当前密码</span>
          </template>
          <el-input
            v-model="password"
            type="password"
            :placeholder="passwordPlaceholder"
            show-password
            class="security-input"
            :class="{ 'security-input-error': inputError }"
            @keyup.enter="handleConfirm"
            @input="clearError"
            clearable
            autofocus
          >
            <template #prefix>
              <el-icon><Key /></el-icon>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <!-- 安全提示 -->
      <div class="security-tips">
        <div class="tips-title">
          <el-icon class="tips-icon"><InfoFilled /></el-icon>
          <span>安全提示</span>
        </div>
        <p class="tips-content">
          为了您的账户安全，请输入当前登录密码以确认此操作。此操作执行后将无法撤销。
        </p>
      </div>
    </div>

    <template #footer>
      <div class="security-footer">
        <el-button
          @click="handleCancel"
          class="security-btn cancel-btn"
          size="large"
        >
          <el-icon class="btn-icon"><Close /></el-icon>
          取消操作
        </el-button>
        <el-button
          @click="handleConfirm"
          :loading="loading"
          class="security-btn danger-btn"
          size="large"
          :disabled="!password.trim()"
        >
          <el-icon class="btn-icon" v-if="!loading"><Check /></el-icon>
          {{ loading ? '验证中...' : confirmButtonText }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, watch, computed } from 'vue'
import {
  WarningFilled,
  Warning,
  Lock,
  Key,
  InfoFilled,
  Close,
  Check
} from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  warningMessage: {
    type: String,
    default: '此操作将永久删除相关数据，删除后无法恢复，请谨慎操作！'
  },
  passwordPlaceholder: {
    type: String,
    default: '请输入您的当前登录密码以确认此操作'
  },
  confirmButtonText: {
    type: String,
    default: '确认执行'
  }
})

const emit = defineEmits(['update:visible', 'confirm', 'cancel'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const password = ref('')
const inputError = ref(false)

// 清除输入错误状态
const clearError = () => {
  inputError.value = false
}

// 显示输入错误
const showError = () => {
  inputError.value = true
  setTimeout(() => {
    inputError.value = false
  }, 600)
}

// 处理确认操作
const handleConfirm = () => {
  if (!password.value.trim()) {
    showError()
    return
  }
  emit('confirm', password.value)
}

// 处理取消操作
const handleCancel = () => {
  password.value = ''
  inputError.value = false
  emit('cancel')
  dialogVisible.value = false
}

// 监听对话框关闭，重置状态
watch(() => props.visible, (newVal) => {
  if (!newVal) {
    password.value = ''
    inputError.value = false
  }
})
</script>

<style lang="scss" scoped>
// 引用集中管理的样式文件
@use '../usermanagement.scss' as *;
</style>
