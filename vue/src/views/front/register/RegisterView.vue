<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <div class="logo-container">
          <el-icon class="logo-icon"><svg viewBox="0 0 128 128" width="24" height="24"><path d="M115.4 30.7L67.1 2.9c-.8-.5-1.9-.7-3.1-.7-1.2 0-2.3.3-3.1.7l-48 27.9c-1.7 1-2.9 3.5-2.9 5.4v55.7c0 1.1.2 2.4 1 3.5l106.8-62c-.6-1.2-1.5-2.1-2.4-2.7z" fill="#42b883"></path><path d="M10.7 95.3c.5.8 1.2 1.5 1.9 1.9l48.2 27.9c.8.5 1.9.7 3.1.7 1.2 0 2.3-.3 3.1-.7l48-27.9c1.7-1 2.9-3.5 2.9-5.4V36.1c0-.9-.1-1.9-.6-2.8l-106.6 62z" fill="#35495e"></path></svg></el-icon>
          <h1 class="app-title">推荐系统</h1>
        </div>
        <h2 class="register-title">创建账号</h2>
        <p class="register-subtitle">请填写以下信息完成注册</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" class="register-form">
        <el-form-item prop="name">
          <el-input
            v-model="form.name"
            placeholder="用户名"
            :prefix-icon="User"
            :clearable="true"
            class="custom-input"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="邮箱"
            :prefix-icon="Message"
            :clearable="true"
            class="custom-input"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            class="custom-input"
          />
        </el-form-item>

        <el-form-item class="button-group">
          <el-button type="primary" @click="handleRegister" :loading="loading" class="register-button">注册</el-button>
          <el-button @click="goToLogin" class="register-button">返回登录</el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <p>© {{ currentYear }} 推荐系统. 保留所有权利</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { register } from '@/api/auth/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const currentYear = computed(() => new Date().getFullYear())

const form = reactive({
  name: '',
  email: '',
  password: ''
})

const rules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
})

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await register(form)
        ElMessage.success('注册成功')
        router.push('/login')
      } catch (error: Error | unknown) {
        ElMessage.error(error instanceof Error ? error.message : '注册失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}
</script>
<style lang="scss">
@use './style/register.scss' as *;
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: $background-color;
  padding: 20px;

  .register-box {
    width: $register-box-width;
    background-color: $card-background;
    border-radius: $register-box-radius;
    padding: $register-box-padding;
    box-shadow: $box-shadow;
    transition: box-shadow 0.3s ease;

    &:hover {
      box-shadow: $box-shadow-hover;
    }
  }

  .register-header {
    text-align: center;
    margin-bottom: 30px;

    .logo-container {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;

      .logo-icon {
        font-size: $register-icon-size;
        margin-right: 10px;
        color: $logo-icon-color;
      }

      .app-title {
        font-size: $register-title-size;
        color: $primary-color;
        margin: 0;
      }
    }

    .register-title {
      font-size: 20px;
      color: $text-primary;
      margin-bottom: 10px;
    }

    .register-subtitle {
      font-size: $register-subtitle-size;
      color: $text-secondary;
      opacity: $secondary-opacity;
    }
  }

  .register-form {
    margin-bottom: 20px;

    .custom-input {
      height: $input-height;
      font-size: $input-font-size;

      :deep(.el-input__wrapper) {
        border-radius: $form-radius;

        &:focus-within {
          border-color: $primary-color;
        }
      }
    }

    .button-group {
      display: flex;
      gap: 10px;
      margin-top: 20px;

      .register-button {
        flex: 1;
        height: $button-height;
        font-size: $button-font-size;
        background-color: $primary-color;
        border-color: $primary-color;

        &:hover, &:focus {
          background-color: $primary-dark;
          border-color: $primary-dark;
        }
      }

      .register-button {
        flex: 1;
        height: $button-height;
        font-size: $button-font-size;
      }
    }
  }

  .register-footer {
    text-align: center;
    font-size: $footer-font-size;
    color: $text-secondary;
    opacity: $secondary-opacity;
    margin-top: 20px;
  }
}
</style>
