<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <div class="logo-container">
          <el-icon class="logo-icon"><svg viewBox="0 0 128 128" width="24" height="24"><path d="M115.4 30.7L67.1 2.9c-.8-.5-1.9-.7-3.1-.7-1.2 0-2.3.3-3.1.7l-48 27.9c-1.7 1-2.9 3.5-2.9 5.4v55.7c0 1.1.2 2.4 1 3.5l106.8-62c-.6-1.2-1.5-2.1-2.4-2.7z" fill="#42b883"></path><path d="M10.7 95.3c.5.8 1.2 1.5 1.9 1.9l48.2 27.9c.8.5 1.9.7 3.1.7 1.2 0 2.3-.3 3.1-.7l48-27.9c1.7-1 2.9-3.5 2.9-5.4V36.1c0-.9-.1-1.9-.6-2.8l-106.6 62z" fill="#35495e"></path></svg></el-icon>
          <h1 class="logo-text">路由管理</h1>
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
            class="register-input"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="邮箱"
            :prefix-icon="Message"
            :clearable="true"
            class="register-input"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            class="register-input"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading" class="register-button">注册</el-button>
          <el-button @click="goToLogin" class="login-link-button">返回登录</el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <p>© {{ currentYear }} 路由管理. 保留所有权利</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
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

const rules = {
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
}

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await register(form)
        ElMessage.success('注册成功')
        router.push('/login')
      } catch (error: any) {
        ElMessage.error(error.message || '注册失败')
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

<style scoped lang="scss">
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f8f9fc 0%, #eef1f8 100%);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(66, 184, 131, 0.03) 0%, rgba(53, 73, 94, 0.03) 100%);
    z-index: 0;
  }
}

.register-card {
  position: relative;
  width: 420px;
  padding: 40px;
  border-radius: 16px;
  background-color: white;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  z-index: 1;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  }
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.logo-icon {
  font-size: 36px;
  margin-right: 10px;
}

.logo-text {
  font-size: 24px;
  font-weight: 700;
  color: #35495e;
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-title {
  font-size: 28px;
  font-weight: 600;
  color: #35495e;
  margin-bottom: 8px;
}

.register-subtitle {
  font-size: 16px;
  color: #606266;
  margin-bottom: 10px;
}

.register-form {
  .el-form-item {
    margin-bottom: 24px;
  }
}

.register-input {
  height: 50px;

  :deep(.el-input__wrapper) {
    padding: 0 15px;
    border-radius: 10px;
    box-shadow: 0 0 0 1px rgba(53, 73, 94, 0.1) inset;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 0 0 1px rgba(66, 184, 131, 0.3) inset;
    }

    &.is-focus {
      box-shadow: 0 0 0 1px #42b883 inset, 0 0 0 3px rgba(66, 184, 131, 0.1);
    }
  }

  :deep(.el-input__inner) {
    font-size: 16px;
  }

  :deep(.el-input__prefix) {
    color: #909399;
  }
}

.register-button {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 10px;
  margin-bottom: 16px;
  background: linear-gradient(45deg, #42b883, #53c08e);
  border: none;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 5px 15px rgba(66, 184, 131, 0.3);
    background: linear-gradient(45deg, #3ba676, #42b883);
  }

  &:active {
    transform: translateY(0);
  }
}

.login-link-button {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 10px;
  border: 1px solid #42b883;
  color: #42b883;
  background: transparent;
  transition: all 0.3s ease;

  &:hover {
    color: #fff;
    background: #42b883;
    transform: translateY(-2px);
    box-shadow: 0 5px 15px rgba(66, 184, 131, 0.1);
  }

  &:active {
    transform: translateY(0);
  }
}

.register-footer {
  text-align: center;
  font-size: 12px;
  color: #909399;
  margin-top: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .register-card {
    width: 90%;
    max-width: 420px;
    padding: 30px 20px;
  }
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .register-container {
    background: linear-gradient(135deg, #1a1a1a 0%, #222222 100%);

    &::before {
      background: radial-gradient(circle, rgba(66, 184, 131, 0.05) 0%, rgba(53, 73, 94, 0.05) 100%);
    }
  }

  .register-card {
    background-color: #222222;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  }

  .register-title {
    color: #e0e0e0;
  }

  .register-subtitle {
    color: #aaaaaa;
  }

  .register-input {
    :deep(.el-input__wrapper) {
      background-color: rgba(35, 35, 35, 0.7);
      box-shadow: 0 0 0 1px rgba(66, 184, 131, 0.2) inset;

      &:hover {
        box-shadow: 0 0 0 1px rgba(66, 184, 131, 0.4) inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 1px #42b883 inset, 0 0 0 3px rgba(66, 184, 131, 0.2);
      }
    }

    :deep(.el-input__inner) {
      color: #e0e0e0;
    }

    :deep(.el-input__prefix) {
      color: #aaaaaa;
    }
  }

  .register-footer {
    color: #777777;
  }
}
</style>
