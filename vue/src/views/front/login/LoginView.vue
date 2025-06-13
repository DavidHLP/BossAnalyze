<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="logo">
          <el-icon class="logo-icon"><svg viewBox="0 0 128 128" width="32" height="32"><path d="M115.4 30.7L67.1 2.9c-.8-.5-1.9-.7-3.1-.7-1.2 0-2.3.3-3.1.7l-48 27.9c-1.7 1-2.9 3.5-2.9 5.4v55.7c0 1.1.2 2.4 1 3.5l106.8-62c-.6-1.2-1.5-2.1-2.4-2.7z" fill="#42b883"></path><path d="M10.7 95.3c.5.8 1.2 1.5 1.9 1.9l48.2 27.9c.8.5 1.9.7 3.1.7 1.2 0 2.3-.3 3.1-.7l48-27.9c1.7-1 2.9-3.5 2.9-5.4V36.1c0-.9-.1-1.9-.6-2.8l-106.6 62z" fill="#35495e"></path></svg></el-icon>
          <h1 class="logo-text">推荐系统</h1>
        </div>
        <h2 class="welcome-text">欢迎回来</h2>
        <p class="subtitle">请登录您的账户继续访问</p>
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

        <el-form-item class="login-buttons">
          <el-button type="primary" @click="handleLogin" :loading="isLoading" class="login-button">登录</el-button>
        </el-form-item>
        <el-form-item class="login-buttons">
          <el-button @click="goToRegister" class="register-button">立即注册</el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <p>© {{ currentYear }} 推荐系统. 保留所有权利</p>
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
const password = ref('#Alone117')
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

<style lang="scss">
@use './style/login.scss' as *;
// 登录页样式
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: $background-color;
}

.login-box {
  width: $login-box-width;
  background-color: $card-background;
  border-radius: $login-box-radius;
  box-shadow: $box-shadow;
  padding: $login-box-padding;
  text-align: center;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;

  .logo {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24px;

    .logo-icon {
      font-size: $login-icon-size;
      color: $logo-icon-color;
      margin-right: 8px;
    }

    .logo-text {
      color: $primary-color;
      margin: 0;
      font-size: $login-logo-size;
      font-weight: 600;
    }
  }

  .welcome-text {
    color: $text-primary;
    font-size: $login-title-size;
    font-weight: 600;
    margin: 0 0 8px;
  }

  .subtitle {
    color: $text-primary;
    margin: 0;
    font-size: $login-subtitle-size;
    font-weight: 400;
    opacity: $secondary-opacity;
  }
}

.login-form {
  margin-bottom: 25px;

  .login-input {
    :deep(.el-input__wrapper) {
      border-radius: $form-radius;
      box-shadow: 0 0 0 $border-width $border-color inset;
      padding: 1px 11px;

      &.is-focus {
        box-shadow: 0 0 0 $border-width $primary-color inset;
      }
    }

    :deep(.el-input__prefix-inner .el-icon) {
      color: $primary-color;
      font-size: $input-font-size + 2;
    }

    :deep(.el-input__inner) {
      color: $text-primary;
      font-size: $input-font-size;
      height: $input-height;
    }
  }
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  :deep(.el-checkbox__label) {
    font-size: $input-font-size;
    color: $text-primary;
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: $el-checkbox-color;
    border-color: $el-checkbox-color;
  }

  .forgot-password {
    color: $primary-color;
    font-size: $input-font-size;

    &:hover {
      color: $primary-dark;
    }
  }
}

.login-buttons {
  margin-bottom: 16px;

  .login-button, .register-button {
    width: 100%;
    height: $button-height;
    border-radius: $form-radius;
    font-size: $button-font-size;
    font-weight: 500;
    transition: $el-button-transition;
    padding: 0;
    box-sizing: border-box;
  }

  .login-button {
    background-color: $primary-color;
    border-color: $primary-color;
    color: $text-on-primary;

    &:hover, &:focus {
      background-color: $primary-dark;
      border-color: $primary-dark;
    }
  }

  .register-button {
    color: $primary-color;
    border: $border-width solid $border-color;
    background-color: $card-background;

    &:hover, &:focus {
      color: $primary-color;
      border-color: $primary-color;
    }
  }
}

.login-footer {
  text-align: center;
  color: $text-primary;
  font-size: $footer-font-size;
  opacity: $secondary-opacity;

  p {
    margin: 0;
  }
}

:deep(.el-form-item__content) {
  margin-bottom: 20px;
}
</style>
