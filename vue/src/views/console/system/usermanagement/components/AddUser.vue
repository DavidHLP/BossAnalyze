<template>
  <el-dialog
    v-model="dialogVisible"
    title="添加用户"
    width="550px"
    :before-close="handleClose"
    class="add-user-dialog"
  >
    <BCard no-body class="border-0 shadow-sm">
      <BCardBody>
        <el-form
          :model="formData"
          :rules="rules"
          ref="formRef"
          label-width="100px"
          class="user-form"
        >
          <div class="avatar-upload-section">
            <el-upload
              :http-request="handleUploadRequest"
              :on-success="handleAvatarSuccess"
              :file-list="formData.avatar ? [{ name: 'avatar', url: avatarUrl || '' }] : []"
              list-type="picture-card"
              class="avatar-uploader"
              :auto-upload="true"
            >
              <div class="avatar-inner">
                <template v-if="formData.avatar">
                  <el-image :src="avatarUrl" class="uploaded-avatar" fit="cover" />
                </template>
                <div v-else class="upload-placeholder">
                  <el-icon><Plus /></el-icon>
                  <span>上传头像</span>
                </div>
              </div>
            </el-upload>
          </div>

          <el-form-item label="姓名" prop="name">
            <el-input v-model="formData.name" placeholder="请输入姓名" class="rounded-input" />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" placeholder="请输入邮箱" class="rounded-input">
              <template #prefix>
                <el-icon class="text-primary"><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码"
              show-password
              class="rounded-input"
            >
              <template #prefix>
                <el-icon class="text-primary"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="formData.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              show-password
              class="rounded-input"
            >
              <template #prefix>
                <el-icon class="text-primary"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="角色" prop="roleId">
            <el-select
              v-model="formData.roleId"
              filterable
              remote
              reserve-keyword
              placeholder="请输入角色名称"
              :remote-method="remoteRoleSearch"
              :loading="roleLoading"
              class="rounded-input full-width"
            >
              <template #prefix>
                <el-icon class="text-primary"><UserIcon /></el-icon>
              </template>
              <el-option
                v-for="role in roleOptions"
                :key="role.value"
                :label="role.label"
                :value="role.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="状态" prop="status">
            <div class="d-flex align-items-center">
              <el-switch
                v-model="statusValue"
                class="status-switch me-3"
                active-text="启用"
                inactive-text="禁用"
                inline-prompt
              />
              <BBadge :variant="statusValue ? 'success' : 'danger'">{{ statusValue ? '用户可登录系统' : '用户被禁止登录' }}</BBadge>
            </div>
          </el-form-item>

          <el-form-item label="地址">
            <el-input
              v-model="formData.address"
              placeholder="请输入地址"
              class="rounded-input"
            >
              <template #prefix>
                <el-icon class="text-primary"><Location /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="个人简介">
            <el-input
              v-model="formData.introduction"
              type="textarea"
              placeholder="请输入个人简介"
              :rows="3"
              class="rounded-input"
            />
          </el-form-item>
        </el-form>
      </BCardBody>
    </BCard>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" round>取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading" round>
          {{ submitLoading ? '创建中...' : '创建用户' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Message, Lock, User as UserIcon, Location } from '@element-plus/icons-vue'
import type { FormInstance, FormItemRule } from 'element-plus'
import { getRoleList } from '@/api/role/role'
import { addUser } from '@/api/user/user'
import { uploadImage, getImageUrl } from '@/api/minio/minio'
import type { UserBaseInfo as User } from '@/api/auth/auth.d'

// 父组件传入的props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

// 向父组件发出的事件
const emit = defineEmits(['update:visible', 'user-added'])

// 对话框的可见性，与父组件的visible属性双向绑定
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// 表单引用
const formRef = ref<FormInstance>()

// 角色选项
const roleOptions = ref<{ label: string; value: number }[]>([])
const roleLoading = ref(false)

// 提交状态
const submitLoading = ref(false)

// 头像URL
const avatarUrl = ref('')

// 表单数据
const formData = reactive<User & { password?: string; confirmPassword?: string }>({
  name: '',
  email: '',
  password: '',
  confirmPassword: '',
  roleId: undefined,
  status: '1',
  address: '',
  introduction: '',
  avatar: '',
})

// 状态值的计算属性，用于switch组件
const statusValue = computed({
  get: () => formData.status === '1',
  set: (val) => {
    formData.status = val ? '1' : '0'
  }
})

// 表单校验规则
const rules = reactive<Record<string, FormItemRule[]>>({
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== formData.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  roleId: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
})

// 初始化角色数据和远程搜索简化
const initRoleOptions = async () => {
  roleLoading.value = true
  try {
    const response = await getRoleList()
    roleOptions.value = response.map((item) => ({
      label: item.roleName ?? '',
      value: item.id ?? 0,
    }))
  } catch (error) {
    console.error('加载角色数据失败:', error)
    ElMessage.error('加载角色数据失败')
  } finally {
    roleLoading.value = false
  }
}

// 远程搜索角色
const remoteRoleSearch = async (roleName: string) => {
  roleLoading.value = true
  try {
    const response = await getRoleList(roleName || '')
    roleOptions.value = response.map((item) => ({
      label: item.roleName ?? '',
      value: item.id ?? 0,
    }))
  } catch (error) {
    console.error('搜索角色失败:', error)
    ElMessage.error('搜索角色失败')
  } finally {
    roleLoading.value = false
  }
}

// 处理关闭对话框
const handleClose = () => {
  dialogVisible.value = false
  resetForm()
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    roleId: undefined,
    status: '1',
    address: '',
    introduction: '',
    avatar: '',
  })
}

// 处理提交
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid, fields) => {
    if (!valid) {
      console.log('表单验证失败', fields)
      ElMessage.error('请正确填写表单信息')
      return
    }

    submitLoading.value = true
    try {
      // 准备提交的数据（移除确认密码字段）
      const userData = { ...formData }
      delete userData.confirmPassword

      // 调用API添加用户
      const response = await addUser(userData)

      ElMessage.success('添加用户成功')
      emit('user-added', response)
      dialogVisible.value = false
      resetForm()
    } catch (error: unknown) {
      console.error('添加用户失败:', error)
      ElMessage.error(error instanceof Error ? error.message : '添加用户失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 根据文件名获取图片URL
const getImageUrlByFileName = async (fileName: string) => {
  try {
    const response = await getImageUrl(fileName)
    return response.url
  } catch (error) {
    console.error('获取头像URL失败:', error)
    return ''
  }
}

// 更新头像URL
const updateAvatarUrl = async () => {
  if (formData.avatar) {
    avatarUrl.value = await getImageUrlByFileName(formData.avatar)
  } else {
    avatarUrl.value = ''
  }
}

// 监听头像变化
watch(() => formData.avatar, () => {
  updateAvatarUrl()
}, { immediate: true })

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
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败，请稍后重试')
    if (options.onError) {
      options.onError(error)
    }
  }
}

// 处理头像上传成功
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handleAvatarSuccess = async (response: any) => {
  formData.avatar = response.fileName
  await updateAvatarUrl()
  ElMessage.success('头像上传成功')
}

// 监听对话框打开，初始化角色数据
watch(() => dialogVisible.value, (val) => {
  if (val) {
    initRoleOptions()
  }
})
</script>

<style lang="scss" scoped>
.add-user-dialog {
  :deep(.el-dialog__header) {
    background-color: $primary-color;
    color: #fff;
    padding: 16px 20px;
    border-radius: 8px 8px 0 0;
    margin-right: 0;

    .el-dialog__title {
      color: #fff;
      font-weight: 500;
      font-size: 18px;
    }

    .el-dialog__close {
      color: rgba(255, 255, 255, 0.8);
      &:hover {
        color: #fff;
      }
    }
  }

  :deep(.el-dialog__body) {
    padding: 20px;
  }

  :deep(.el-dialog__footer) {
    padding: 10px 20px 20px;
    border-top: none;
  }
}

.user-form {
  .el-form-item {
    margin-bottom: 20px;
  }

  :deep(.el-form-item__label) {
    font-weight: 500;
  }

  :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1) inset;
    &:hover {
      box-shadow: 0 0 0 1px $primary-color inset;
    }
    &.is-focus {
      box-shadow: 0 0 0 1px $primary-color inset !important;
    }
  }

  :deep(.el-input__prefix) {
    margin-right: 8px;
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

.status-switch {
  width: 80px;
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

  .avatar-inner {
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

:deep(.rounded-input) {
  .el-input__wrapper {
    border-radius: 20px !important;
  }

  .el-textarea__inner {
    border-radius: 16px !important;
    padding: 12px 16px;
    transition: all 0.3s;

    &:focus {
      box-shadow: 0 0 0 1px $primary-color !important;
    }
  }
}

.full-width {
  width: 100%;
}

:deep(.text-primary) {
  color: $primary-color;
}
</style>
