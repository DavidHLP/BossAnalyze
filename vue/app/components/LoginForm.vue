<script setup lang="ts">
import { loginDestination } from '@/lib/session'

const session = useSession()
const route = useRoute()
const email = ref('')
const password = ref('')
const pending = ref(false)
const error = ref('')

async function submit() {
  if (pending.value) return
  pending.value = true
  error.value = ''
  try {
    await session.signIn(email.value.trim(), password.value)
    password.value = ''
    await navigateTo(loginDestination(route.query.redirect, session.state.value.menus), {
      replace: true,
    })
  } catch {
    error.value = '登录失败，请检查账号密码及服务连接后重试。'
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <form class="flex flex-col gap-6" :aria-busy="pending" @submit.prevent="submit">
    <UiFieldGroup>
      <div class="flex flex-col items-center gap-2 text-center">
        <h1 class="text-2xl font-semibold tracking-tight">登录你的账号</h1>
        <p class="text-sm text-muted-foreground">欢迎回来，继续探索招聘数据。</p>
      </div>
      <UiField>
        <UiFieldLabel for="email">邮箱</UiFieldLabel>
        <UiInput
          id="email"
          v-model="email"
          name="email"
          type="email"
          autocomplete="username"
          placeholder="请输入邮箱"
          required
          :disabled="pending"
        />
      </UiField>
      <UiField>
        <UiFieldLabel for="password">密码</UiFieldLabel>
        <UiInput
          id="password"
          v-model="password"
          name="password"
          type="password"
          autocomplete="current-password"
          required
          :disabled="pending"
        />
      </UiField>
      <p v-if="error" role="alert" class="text-sm text-destructive">{{ error }}</p>
      <p v-else-if="route.query.reason" role="status" class="text-sm text-muted-foreground">
        会话已失效或暂时无法恢复，请重新登录。
      </p>
      <UiField>
        <UiButton type="submit" size="lg" class="w-full" :disabled="pending">
          {{ pending ? '正在登录…' : '登录' }}
        </UiButton>
        <UiFieldDescription class="mt-2 text-center">
          需要账号或访问权限？请联系管理员。
        </UiFieldDescription>
      </UiField>
    </UiFieldGroup>
  </form>
</template>
