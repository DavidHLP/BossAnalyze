<template>
  <el-dialog
    v-model="dialogVisible"
    title="添加新用户"
    width="700px"
    destroy-on-close
    center
    class="add-user-dialog"
    :close-on-click-modal="false"
  >
    <div class="add-user-container">
      <!-- 头像上传区域 -->
      <div class="avatar-section">
        <div class="avatar-upload-wrapper">
          <el-upload
            :http-request="handleUploadRequest"
            :on-success="handleAvatarSuccess"
            list-type="picture-card"
            class="avatar-uploader"
            :auto-upload="true"
            :show-file-list="false"
          >
            <div class="avatar-container">
              <template v-if="formData.avatar && avatarUrl">
                <el-image
                  :src="avatarUrl"
                  class="uploaded-avatar"
                  fit="cover"
                  :preview-src-list="[avatarUrl]"
                  preview-teleported
                />
                <div class="avatar-overlay">
                  <el-icon><Camera /></el-icon>
                  <span>更换头像</span>
                </div>
              </template>
              <div v-else class="upload-placeholder">
                <el-icon><Plus /></el-icon>
                <span>上传头像</span>
              </div>
            </div>
          </el-upload>
        </div>
        <p class="avatar-tip">点击上传用户头像，支持 JPG、PNG 格式</p>
      </div>

      <!-- 表单区域 -->
      <div class="form-section">
        <el-form
          :model="formData"
          :rules="rules"
          ref="formRef"
          label-width="100px"
          class="add-form"
          label-position="top"
        >
          <div class="form-grid">
            <el-form-item label="用户姓名" prop="name" class="form-field">
              <el-input
                v-model="formData.name"
                placeholder="请输入用户姓名"
                class="form-input"
                clearable
              >
                <template #prefix>
                  <el-icon class="input-icon"><UserIcon /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="用户邮箱" prop="email" class="form-field">
              <el-input
                v-model="formData.email"
                placeholder="请输入邮箱地址"
                class="form-input"
                clearable
              >
                <template #prefix>
                  <el-icon class="input-icon"><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="登录密码" prop="password" class="form-field">
              <el-input
                v-model="formData.password"
                type="password"
                placeholder="请输入登录密码"
                show-password
                class="form-input"
                clearable
              >
                <template #prefix>
                  <el-icon class="input-icon"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword" class="form-field">
              <el-input
                v-model="formData.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                show-password
                class="form-input"
                clearable
              >
                <template #prefix>
                  <el-icon class="input-icon"><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="用户角色" prop="roleId" class="form-field">
              <el-select
                v-model="formData.roleId"
                filterable
                remote
                reserve-keyword
                placeholder="请选择用户角色"
                :remote-method="remoteRoleSearch"
                :loading="roleLoading"
                class="form-select"
                clearable
              >
                <template #prefix>
                  <el-icon class="input-icon"><Avatar /></el-icon>
                </template>
                <el-option
                  v-for="role in roleOptions"
                  :key="role.value"
                  :label="role.label"
                  :value="role.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="账户状态" prop="status" class="form-field">
              <div class="status-control">
                <el-switch
                  v-model="statusValue"
                  active-text="启用"
                  inactive-text="禁用"
                  class="status-switch"
                />
                <el-tag
                  :type="statusValue ? 'success' : 'danger'"
                  effect="light"
                  class="status-tag"
                >
                  {{ statusValue ? '用户可正常登录' : '用户被禁止登录' }}
                </el-tag>
              </div>
            </el-form-item>

            <el-form-item label="联系地址" prop="address" class="form-field form-field--full">
              <el-input
                v-model="formData.address"
                placeholder="请输入联系地址（选填）"
                class="form-input"
                clearable
              >
                <template #prefix>
                  <el-icon class="input-icon"><Location /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="个人简介" prop="introduction" class="form-field form-field--full">
              <el-input
                v-model="formData.introduction"
                type="textarea"
                :rows="4"
                placeholder="请输入个人简介（选填）"
                class="form-textarea"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </div>
        </el-form>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button
          @click="handleClose"
          class="footer-btn cancel-btn"
          size="large"
        >
          <el-icon><Close /></el-icon>
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
          :loading="submitLoading"
          class="footer-btn confirm-btn"
          size="large"
        >
          <el-icon><UserFilled /></el-icon>
          {{ submitLoading ? '创建中...' : '创建用户' }}
        </el-button>
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
  ElTag,
} from 'element-plus'
import {
  Plus,
  Message,
  Lock,
  User as UserIcon,
  Location,
  Camera,
  Avatar,
  Close,
  UserFilled
} from '@element-plus/icons-vue'
import { ref, reactive, computed, watch, nextTick } from 'vue'
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
    { required: true, message: '请输入用户姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为 2-20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' },
    { max: 20, message: '密码长度不能超过20个字符', trigger: 'blur' }
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
    { required: true, message: '请选择用户角色', trigger: 'change' }
  ],
  address: [
    { max: 100, message: '地址长度不能超过 100 个字符', trigger: 'blur' }
  ],
  introduction: [
    { max: 200, message: '个人简介不能超过 200 个字符', trigger: 'blur' }
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
  avatarUrl.value = ''
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
    // 重置滚动位置
    nextTick(() => {
      const dialogBody = document.querySelector('.add-user-dialog .el-dialog__body')
      if (dialogBody) {
        dialogBody.scrollTop = 0
      }
    })
  }
}, { immediate: false })
</script>

<style lang="scss" scoped>
// 引用集中管理的样式文件
@use '../usermanagement.scss' as *;
</style>
