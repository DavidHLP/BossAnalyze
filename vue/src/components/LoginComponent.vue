<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-container">
          <el-icon class="logo-icon"><svg viewBox="0 0 128 128" width="24" height="24"><path d="M115.4 30.7L67.1 2.9c-.8-.5-1.9-.7-3.1-.7-1.2 0-2.3.3-3.1.7l-48 27.9c-1.7 1-2.9 3.5-2.9 5.4v55.7c0 1.1.2 2.4 1 3.5l106.8-62c-.6-1.2-1.5-2.1-2.4-2.7z" fill="#42b883"></path><path d="M10.7 95.3c.5.8 1.2 1.5 1.9 1.9l48.2 27.9c.8.5 1.9.7 3.1.7 1.2 0 2.3-.3 3.1-.7l48-27.9c1.7-1 2.9-3.5 2.9-5.4V36.1c0-.9-.1-1.9-.6-2.8l-106.6 62z" fill="#35495e"></path></svg></el-icon>
          <h1 class="logo-text">路由管理</h1>
        </div>
        <h2 class="login-title">欢迎回来</h2>
        <p class="login-subtitle">请登录您的账户继续访问</p>
      </div>

      <el-form @submit.prevent="handleLogin" class="login-form">
        <el-form-item>
          <el-input
            v-model="email"
            placeholder="用户名/邮箱/手机号"
            :prefix-icon="User"
            :clearable="true"
            class="login-input"
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            class="login-input"
          />
        </el-form-item>

        <div class="login-options">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          <el-link type="primary" :underline="false" class="forgot-password">忘记密码?</el-link>
        </div>

        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="isLoading" class="login-button">登录</el-button>
          <el-button @click="goToRegister" class="register-link-button">立即注册</el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <p>© {{ currentYear }} 路由管理. 保留所有权利</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="LoginComponent">
import { ref, computed } from 'vue'
import { ElForm, ElFormItem, ElInput, ElButton, ElMessage, ElCheckbox, ElLink, ElIcon } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth/auth'
import { useUserStore } from '@/stores/user/userStore'
import { setupAsyncRoutes } from '@/router/index'
import { useRouter } from 'vue-router'
import type { Token } from '@/api/auth/auth.d'
import { useTokenStore } from '@/stores/token/tokenStore'
import { useRouterStore } from '@/stores/router/routerStore'

const email = ref('lysf15520112973@163.com')
const password = ref('admin')
const rememberMe = ref(false)
const isLoading = ref(false)
const userStore = useUserStore()
const router = useRouter()
const routerStore = useRouterStore()
const tokenStore = useTokenStore()
const currentYear = computed(() => new Date().getFullYear())

const handleLogin = async () => {
  if (!email.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  try {
    isLoading.value = true
    const res: Token = await login({
      email: email.value,
      password: password.value,
    })

    if (res?.token) {
      tokenStore.setToken(res.token)

      if (rememberMe.value) {
        localStorage.setItem('token_timestamp', Date.now().toString())
      } else {
        sessionStorage.setItem('token_timestamp', Date.now().toString())
      }

      await userStore.getUserBaseInfo()
      await userStore.getUserPermissions()
      await userStore.getUserRoles()
      await userStore.getUserRoutes()
      await routerStore.fetchRoutes()
      await setupAsyncRoutes()

      // 处理重定向逻辑
      const redirect = router.currentRoute.value.query.redirect?.toString() || '/'
      router.push(redirect)

      ElMessage.success('登录成功')
    }
  } catch (error) {
    ElMessage.error('登录失败，请检查凭证')
    console.error('登录失败', error)
    localStorage.removeItem('token')
  } finally {
    isLoading.value = false
  }
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped lang="scss">
.login-container {
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

.login-card {
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

  span {
    color: #42b883;
  }
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #35495e;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 16px;
  color: #606266;
  margin-bottom: 10px;
}

.login-form {
  .el-form-item {
    margin-bottom: 24px;
  }
}

.login-input {
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

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: #42b883;
    border-color: #42b883;
  }

  :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
    color: #606266;
  }

  .forgot-password {
    font-size: 14px;
    color: #42b883;

    &:hover {
      color: #3ba676;
    }
  }
}

.login-button {
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

.register-link-button {
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

.login-footer {
  text-align: center;
  font-size: 12px;
  color: #909399;
  margin-top: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-card {
    width: 90%;
    max-width: 420px;
    padding: 30px 20px;
  }
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .login-container {
    background: linear-gradient(135deg, #1a1a1a 0%, #222222 100%);

    &::before {
      background: radial-gradient(circle, rgba(66, 184, 131, 0.05) 0%, rgba(53, 73, 94, 0.05) 100%);
    }
  }

  .login-card {
    background-color: #222222;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  }

  .login-title {
    color: #e0e0e0;
  }

  .login-subtitle {
    color: #aaaaaa;
  }

  .login-input {
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

  .login-options {
    :deep(.el-checkbox__label) {
      color: #aaaaaa;
    }
  }

  .register-section {
    color: #aaaaaa;
  }

  .login-footer {
    color: #777777;
  }
}
</style>
