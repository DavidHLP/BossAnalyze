<template>
  <el-dialog v-model="dialogVisible" title="安全验证" width="400px" center class="blue-dialog">
    <div class="delete-warning">
      <i class="el-icon-warning warning-icon"></i>
      <p>{{ warningMessage }}</p>
    </div>
    <el-form label-width="100px" class="delete-form">
      <el-form-item label="当前密码" required>
        <el-input v-model="password" type="password" :placeholder="passwordPlaceholder" show-password
          class="rounded-input" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel" round>取消</el-button>
        <el-button type="danger" @click="handleConfirm" :loading="loading" round>{{ confirmButtonText }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'

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
    default: '删除操作不可恢复，请确认您的操作'
  },
  passwordPlaceholder: {
    type: String,
    default: '请输入您的登录密码以确认操作'
  },
  confirmButtonText: {
    type: String,
    default: '确认删除'
  }
})

const emit = defineEmits(['update:visible', 'confirm', 'cancel'])

const dialogVisible = ref(props.visible)
const password = ref('')

watch(() => props.visible, (newVal) => {
  dialogVisible.value = newVal
})

watch(dialogVisible, (newVal) => {
  emit('update:visible', newVal)
})

const handleConfirm = () => {
  emit('confirm', password.value)
}

const handleCancel = () => {
  password.value = ''
  emit('cancel')
  dialogVisible.value = false
}
</script>

<style lang="scss" scoped>
.blue-dialog {
  :deep(.el-dialog__header) {
    padding: 20px;
    margin: 0;
    text-align: center;
    border-bottom: 1px solid #f0f0f0;
    font-size: 18px;
    font-weight: bold;
    color: #303133;
  }

  :deep(.el-dialog__body) {
    padding: 20px;
  }

  :deep(.el-dialog__footer) {
    padding: 15px 20px;
    border-top: 1px solid #f0f0f0;
  }
}

.delete-warning {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background-color: rgba(245, 108, 108, 0.1);
  border-radius: 4px;

  .warning-icon {
    font-size: 32px;
    color: #F56C6C;
    margin-bottom: 10px;
  }

  p {
    color: #606266;
    text-align: center;
    line-height: 1.5;
    margin: 0;
  }
}

.delete-form {
  margin-top: 20px;
}

.rounded-input {
  border-radius: 20px;

  :deep(.el-input__wrapper) {
    border-radius: 20px;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
