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
    </div>
  </AuthLayout>
</template>
