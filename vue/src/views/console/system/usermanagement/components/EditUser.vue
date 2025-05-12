<template>
  <el-dialog v-model="dialogVisible" title="编辑用户" width="600px" destroy-on-close center class="user-edit-dialog">
    <el-form :model="formData" label-width="80px" class="edit-form">
      <div class="avatar-upload-section">
        <el-upload action="/api/upload/avatar" :on-success="handleAvatarSuccess"
          :file-list="formData.avatar ? [{ name: 'avatar', url: formData.avatar }] : []" list-type="picture-card"
          class="avatar-uploader">
          <div class="upload-inner">
            <template v-if="formData.avatar">
              <el-image :src="formData.avatar" class="uploaded-avatar" fit="cover" />
            </template>
            <div v-else class="upload-placeholder">
              <el-icon><Plus /></el-icon>
              <span>上传头像</span>
            </div>
          </div>
        </el-upload>
      </div>

      <el-form-item label="姓名">
        <el-input v-model="formData.name" class="form-input" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="formData.roleId" filterable remote reserve-keyword placeholder="请输入角色名称"
          :remote-method="remoteRoleSearch" :loading="roleLoading" class="form-input">
          <el-option v-for="role in formData.roleOptions" :key="role.value" :label="role.label" :value="role.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" active-text="启用"
          inactive-text="禁用" active-color="#1E88E5" inactive-color="#F44336" />
      </el-form-item>
      <el-form-item label="地址">
        <el-input v-model="formData.address" class="form-input" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="formData.email" class="form-input" />
      </el-form-item>
      <el-form-item label="个人简介">
        <el-input v-model="formData.introduction" type="textarea" :rows="4" class="form-input" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" round>取消</el-button>
        <el-button type="primary" @click="handleConfirm" round>确认</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElSelect,
  ElSwitch,
  ElButton,
  ElUpload,
  ElImage,
  ElMessage,
} from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { computed, ref, defineEmits, defineProps, watch } from 'vue'
import type { UserBaseInfo as User } from '@/api/auth/auth.d'
import { getRoleList } from '@/api/role/role'
import { updateUser } from '@/api/user/user'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  userData: {
    type: Object as () => User,
    default: () => ({})
  }
})

const emit = defineEmits(['update:visible', 'user-updated'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const formData = ref<User>({
  id: 0,
  createDate: '',
  name: '',
  address: '',
  email: '',
  status: 1,
  roleName: 'user',
  roleId: 0,
  introduction: '',
  roleOptions: [],
})

const roleLoading = ref(false)

const loadRoleOptions = async () => {
  try {
    const response = await getRoleList()
    const formattedResponse = response.map((item) => ({
      label: item.roleName ?? '',
      value: Number(item.id) || 0,
    }))
    formData.value.roleOptions = formattedResponse
    console.log(formattedResponse)
  } catch (error) {
    console.error('加载角色选项失败:', error)
  }
}

watch(() => props.userData, (newValue) => {
  if (newValue && Object.keys(newValue).length > 0) {
    const userData = JSON.parse(JSON.stringify(newValue))
    // 确保 roleId 是数字类型
    if (userData.roleId !== undefined) {
      userData.roleId = Number(userData.roleId) || 0
    }
    formData.value = userData
    if (!formData.value.roleOptions) {
      formData.value.roleOptions = []
    }
    // 当对话框打开时加载角色选项
    loadRoleOptions()
  }
}, { immediate: true, deep: true })

const remoteRoleSearch = async (roleName: string) => {
  if (roleName) {
    roleLoading.value = true
    try {
      const response = await getRoleList(roleName)
      const formattedResponse = response.map((item) => ({
        label: item.roleName ?? '',
        value: Number(item.id) || 0,
      }))
      formData.value.roleOptions = formattedResponse
    } finally {
      roleLoading.value = false
    }
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handleAvatarSuccess = (response: any, file: any) => {
  formData.value.avatar = response.url || file.url
  ElMessage.success('头像上传成功')
}

const handleClose = () => {
  dialogVisible.value = false
}

const handleConfirm = async () => {
  try {
    await updateUser(formData.value)
    ElMessage.success('用户信息更新成功')
    emit('user-updated', formData.value)
    dialogVisible.value = false
  } catch (error) {
    console.error('更新失败:', error)
    ElMessage.error('更新失败，请稍后重试')
  }
}
</script>

<style lang="scss" scoped>
.user-edit-dialog {
  :deep(.el-dialog__header) {
    background-color: $primary-color;
    color: #fff;
    padding: 16px 20px;
    border-radius: 8px 8px 0 0;

    .el-dialog__title {
      color: #fff;
      font-weight: 500;
      font-size: 18px;
    }
  }

  :deep(.el-dialog__body) {
    padding: 24px 30px;
  }

  :deep(.el-dialog__headerbtn .el-dialog__close) {
    color: #fff;
  }
}

.edit-form {
  .el-form-item {
    margin-bottom: 22px;

    &:last-child {
      margin-bottom: 0;
    }

    :deep(.el-form-item__label) {
      color: #606266;
      font-weight: normal;
    }
  }

  .form-input {
    :deep(.el-input__wrapper) {
      border-radius: 4px;
      box-shadow: 0 0 0 1px #dcdfe6 inset;
      transition: all 0.2s;

      &:hover {
        box-shadow: 0 0 0 1px #c0c4cc inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 1px $primary-color inset;
      }
    }

    :deep(.el-textarea__inner) {
      border-radius: 4px;
    }
  }
}

.avatar-upload-section {
  display: flex;
  justify-content: center;
  margin-bottom: 28px;
}

.avatar-uploader {
  :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 50%;
    overflow: hidden;
    cursor: pointer;
    position: relative;
    transition: all 0.3s ease;

    &:hover {
      border-color: $primary-color;
    }
  }

  .upload-inner {
    width: 110px;
    height: 110px;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
  }

  .uploaded-avatar {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .upload-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: #8c8c8c;

    .el-icon {
      font-size: 22px;
    }

    span {
      font-size: 14px;
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 10px;

  .el-button {
    min-width: 80px;
    font-weight: normal;
  }
}
</style>
