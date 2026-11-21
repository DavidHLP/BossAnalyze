<script setup lang="ts">
import AuthLayout from '@/layouts/AuthLayout.vue'
import LoginForm from '@/components/LoginForm.vue'
</script>

<template>
  <AuthLayout>
    <div class="flex w-full max-w-[24.5rem] flex-col gap-4">
      <UiAlert v-if="$route.query.registered === '1'" role="status">
        <UiAlertDescription>注册成功，请使用新账号登录。</UiAlertDescription>
      </UiAlert>
      <LoginForm />
      <p class="text-center text-sm text-muted-foreground">
        没有账号？
        <RouterLink class="font-medium text-foreground underline underline-offset-4" :to="{ path: '/register', query: { redirect: $route.query.redirect } }">注册账号</RouterLink>
      </p>
    </div>
  </AuthLayout>
</template>
