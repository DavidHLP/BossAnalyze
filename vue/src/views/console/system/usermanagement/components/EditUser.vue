<template>
  <el-dialog
    v-model="dialogVisible"
    title="编辑用户信息"
    width="700px"
    destroy-on-close
    center
    class="user-edit-dialog"
    :close-on-click-modal="false"
  >
    <div class="edit-user-container">
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
          class="edit-form"
          label-position="top"
        >
          <div class="form-row">
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
          </div>

          <div class="form-row">
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
                  :active-value="1"
                  :inactive-value="0"
                  active-text="启用"
                  inactive-text="禁用"
                  class="status-switch"
                />
                <el-tag
                  :type="statusValue === 1 ? 'success' : 'danger'"
                  effect="light"
                  class="status-tag"
                >
                  {{ statusValue === 1 ? '用户可正常登录' : '用户被禁止登录' }}
                </el-tag>
              </div>
            </el-form-item>
          </div>

          <el-form-item label="联系地址" prop="address" class="form-field full-width">
            <el-input
              v-model="formData.address"
              placeholder="请输入联系地址"
              class="form-input"
              clearable
            >
              <template #prefix>
                <el-icon class="input-icon"><Location /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="个人简介" prop="introduction" class="form-field full-width">
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
          @click="handleConfirm"
          :loading="submitLoading"
          class="footer-btn confirm-btn"
          size="large"
        >
          <el-icon><Check /></el-icon>
          确认修改
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
  User as UserIcon,
  Location,
  Camera,
  Avatar,
  Close,
  Check
} from '@element-plus/icons-vue'
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
    { required: true, message: '请输入用户姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为 2-20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
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
// 引用集中管理的样式文件
@use '../usermanagement.scss' as *;
</style>
