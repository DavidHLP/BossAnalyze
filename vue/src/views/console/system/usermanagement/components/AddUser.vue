<template>
  <el-dialog
    v-model="dialogVisible"
    title="添加用户"
    width="500px"
    :before-close="handleClose"
    class="add-user-dialog"
  >
    <el-form
      :model="formData"
      :rules="rules"
      ref="formRef"
      label-width="100px"
      class="user-form"
    >
      <el-form-item label="姓名" prop="name">
        <el-input v-model="formData.name" placeholder="请输入姓名" class="rounded-input" />
      </el-form-item>

      <el-form-item label="邮箱" prop="email">
        <el-input v-model="formData.email" placeholder="请输入邮箱" class="rounded-input" />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="formData.password"
          type="password"
          placeholder="请输入密码"
          show-password
          class="rounded-input"
        />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="formData.confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          show-password
          class="rounded-input"
        />
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
          <el-option
            v-for="role in roleOptions"
            :key="role.value"
            :label="role.label"
            :value="role.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-switch
          v-model="statusValue"
          class="status-switch"
          active-text="启用"
          inactive-text="禁用"
          inline-prompt
        />
      </el-form-item>

      <el-form-item label="地址">
        <el-input
          v-model="formData.address"
          placeholder="请输入地址"
          class="rounded-input"
        />
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

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" round>取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading" round>
          创建用户
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule } from 'element-plus'
import { getRoleList } from '@/api/role/role'
import { addUser } from '@/api/user/user'
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

// 初始化角色数据
const initRoleOptions = async () => {
  try {
    const response = await getRoleList()
    roleOptions.value = response.map((item) => ({
      label: item.roleName ?? '',
      value: item.id ?? 0,
    }))
  } catch (error) {
    console.error('加载角色数据失败:', error)
    ElMessage.error('加载角色数据失败')
  }
}

// 远程搜索角色
const remoteRoleSearch = async (roleName: string) => {
  if (roleName) {
    roleLoading.value = true
    try {
      const response = await getRoleList(roleName)
      roleOptions.value = response.map((item) => ({
        label: item.roleName ?? '',
        value: item.id ?? 0,
      }))
    } catch (error) {
      console.error('搜索角色失败:', error)
    } finally {
      roleLoading.value = false
    }
  } else {
    // 如果搜索词为空，恢复所有角色
    initRoleOptions()
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
  })
}

// 处理提交
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid, fields) => {
    if (valid) {
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
    } else {
      console.log('表单验证失败', fields)
      ElMessage.error('请正确填写表单信息')
    }
  })
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

    .el-dialog__title {
      color: #fff;
      font-weight: 500;
      font-size: 18px;
    }
  }

  :deep(.el-dialog__body) {
    padding: 24px 30px;
  }
}

.user-form {
  .el-form-item {
    margin-bottom: 20px;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .el-button {
    min-width: 100px;
  }
}

.status-switch {
  width: 80px;
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;

  .avatar-uploader {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    overflow: hidden;
    border: 1px dashed #d9d9d9;
    cursor: pointer;
    position: relative;
    margin-bottom: 10px;

    &:hover {
      border-color: var(--el-color-primary);
    }

    .avatar-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .avatar-icon {
      font-size: 28px;
      color: #8c939d;
      width: 100%;
      height: 100%;
      display: flex;
      justify-content: center;
      align-items: center;
    }
  }

  .avatar-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }
}

:deep(.rounded-input) {
  .el-input__wrapper {
    border-radius: 20px !important;
  }

  .el-textarea__inner {
    border-radius: 20px !important;
  }
}

.full-width {
  width: 100%;
}
</style>
