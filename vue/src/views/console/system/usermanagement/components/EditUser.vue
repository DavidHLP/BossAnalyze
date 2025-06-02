<template>
  <el-dialog v-model="dialogVisible" title="编辑用户" width="600px" destroy-on-close center class="user-edit-dialog">
    <BCard no-body class="border-0 shadow-sm">
      <BCardBody>
        <el-form :model="formData" :rules="rules" ref="formRef" label-width="80px" class="edit-form">
          <div class="avatar-upload-section">
            <el-upload :http-request="handleUploadRequest" :on-success="handleAvatarSuccess"
             list-type="picture-card"
              class="avatar-uploader" :auto-upload="true">
              <div class="avatar-inner">
                <template v-if="formData.avatar">
                  <el-image :src="avatarUrl" class="uploaded-avatar" fit="cover" />
                </template>
                <div v-else class="upload-placeholder">
                  <el-icon>
                    <Plus />
                  </el-icon>
                  <span>上传头像</span>
                </div>
              </div>
            </el-upload>
          </div>

          <el-form-item label="姓名" prop="name">
            <el-input v-model="formData.name" class="rounded-input" />
          </el-form-item>
          <el-form-item label="角色" prop="roleId">
            <el-select v-model="formData.roleId" filterable remote reserve-keyword placeholder="请输入角色名称"
              :remote-method="remoteRoleSearch" :loading="roleLoading" class="rounded-input full-width">
              <template #prefix>
                <el-icon class="text-primary"><UserIcon /></el-icon>
              </template>
              <el-option v-for="role in roleOptions" :key="role.value" :label="role.label" :value="role.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <div class="d-flex align-items-center">
              <el-switch v-model="statusValue" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用"
                active-color="#1E88E5" inactive-color="#F44336" class="me-3" />
              <BBadge :variant="statusValue === 1 ? 'success' : 'danger'">
                {{ statusValue === 1 ? '用户可登录系统' : '用户被禁止登录' }}
              </BBadge>
            </div>
          </el-form-item>
          <el-form-item label="地址" prop="address">
            <el-input v-model="formData.address" class="rounded-input">
              <template #prefix>
                <el-icon class="text-primary"><Location /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" class="rounded-input">
              <template #prefix>
                <el-icon class="text-primary"><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="个人简介" prop="introduction">
            <el-input v-model="formData.introduction" type="textarea" :rows="4" class="rounded-input" />
          </el-form-item>
        </el-form>
      </BCardBody>
    </BCard>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" round>取消</el-button>
        <el-button type="primary" @click="handleConfirm" :loading="submitLoading" round>确认</el-button>
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
import { Plus, Message, User as UserIcon, Location } from '@element-plus/icons-vue'
import { computed, ref, defineEmits, defineProps, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { UserBaseInfo as User } from '@/api/auth/auth.d'
import { getRoleList } from '@/api/role/role'
import { updateUser } from '@/api/user/user'
import { uploadImage, getImageUrl } from '@/api/minio/minio'

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

const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formData = ref<User>({
  id: 0,
  createDate: '',
  name: '',
  address: '',
  email: '',
  status: 1,
  roleName: '',
  roleId: 0,
  introduction: '',
})

const avatarUrl = ref('')

const roleOptions = ref<{ label: string; value: number }[]>([])
const roleLoading = ref(false)

// 表单验证规则
const rules = ref<FormRules>({
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  roleId: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
})

// 状态值的计算属性
const statusValue = computed({
  get: () => formData.value.status,
  set: (val) => {
    formData.value.status = val
  }
})

const loadRoleOptions = async () => {
  try {
    roleLoading.value = true
    const response = await getRoleList()
    roleOptions.value = response.map((item) => ({
      label: item.roleName ?? '',
      value: Number(item.id) || 0,
    }))
  } catch (error) {
    console.error('加载角色选项失败:', error)
    ElMessage.error('加载角色选项失败')
  } finally {
    roleLoading.value = false
  }
}

watch(() => props.userData, (newValue) => {
  if (newValue && Object.keys(newValue).length > 0) {
    // 深拷贝用户数据，避免直接修改props
    const userData = JSON.parse(JSON.stringify(newValue))
    // 确保 roleId 是数字类型
    if (userData.roleId !== undefined) {
      userData.roleId = Number(userData.roleId) || 0
    }
    formData.value = userData
  }
}, { immediate: true, deep: true })

// 监听对话框打开，加载角色选项
watch(() => dialogVisible.value, (val) => {
  if (val) {
    loadRoleOptions()
    updateAvatarUrl()
  }
})

const remoteRoleSearch = async (roleName: string) => {
  if (roleName) {
    roleLoading.value = true
    try {
      const response = await getRoleList(roleName)
      roleOptions.value = response.map((item) => ({
        label: item.roleName ?? '',
        value: Number(item.id) || 0,
      }))
    } finally {
      roleLoading.value = false
    }
  } else {
    loadRoleOptions()
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handleAvatarSuccess = async (response: any) => {
  formData.value.avatar = response.fileName
  ElMessage.success('头像上传成功')
  await updateAvatarUrl()
}

const getImageUrlByFileName = async (fileName: string) => {
  try {
    const response = await getImageUrl(fileName)
    return response.url
  } catch (error) {
    console.error('获取图片URL失败:', error)
    return ''
  }
}

const updateAvatarUrl = async () => {
  if (formData.value.avatar) {
    avatarUrl.value = await getImageUrlByFileName(formData.value.avatar)
  } else {
    avatarUrl.value = ''
  }
}

// 自定义上传请求处理
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handleUploadRequest = async (options: any) => {
  try {
    const response = await uploadImage(options.file)
    handleAvatarSuccess(response)
    if (options.onSuccess) {
      options.onSuccess(response)
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败，请稍后重试')
    if (options.onError) {
      options.onError(error)
    }
  }
}

const handleClose = () => {
  dialogVisible.value = false
}

const handleConfirm = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate(async (valid, fields) => {
      if (!valid) {
        console.error('表单验证失败:', fields)
        return
      }

      submitLoading.value = true
      try {
        await updateUser(formData.value)
        ElMessage.success('用户信息更新成功')
        emit('user-updated', formData.value)
        dialogVisible.value = false
      } catch (error) {
        console.error('更新失败:', error)
        ElMessage.error('更新失败，请稍后重试')
      }
    })
  } finally {
    submitLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.user-edit-dialog {
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

.edit-form {
  .el-form-item {
    margin-bottom: 18px;
  }
}

.avatar-upload-section {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;

  :deep(.avatar-uploader) {
    .el-upload {
      border: 1px dashed #d9d9d9;
      border-radius: 50%;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: all 0.3s;
      width: 100px;
      height: 100px;

      &:hover {
        border-color: #409EFF;
      }
    }
  }
}

.avatar-inner {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
}

.uploaded-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8c939d;

  .el-icon {
    font-size: 24px;
    margin-bottom: 5px;
  }

  span {
    font-size: 12px;
  }
}

.rounded-input {
  border-radius: 20px;

  :deep(.el-input__wrapper) {
    border-radius: 20px;
  }
}

.full-width {
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
