<script setup lang="ts">
import { loginDestination, loginFailure } from '@/lib/session'

const session = useSession()
const route = useRoute()
const email = ref('')
const password = ref('')
const pending = ref(false)
const error = ref('')
const canRestore = ref(false)
onMounted(() => { canRestore.value = Boolean(localStorage.getItem('token')) })
async function retrySession() {
  pending.value = true
  error.value = ''
  try {
    if (await session.restore()) {
      await navigateTo(loginDestination(route.query.redirect, session.state.value.menus), { replace: true })
    } else error.value = '会话已失效，请重新登录。'
  } catch {
    error.value = '暂时无法恢复会话，请重试。'
  } finally {
    canRestore.value = Boolean(localStorage.getItem('token'))
    pending.value = false
  }
}

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
  } catch (cause) {
    canRestore.value = Boolean(localStorage.getItem('token'))
    error.value = canRestore.value ? '登录凭据已获取，但会话恢复失败，请重试恢复。' : loginFailure(cause)
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
          :aria-describedby="error ? 'login-error' : undefined"
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
          :aria-describedby="error ? 'login-error' : undefined"
          required
          :disabled="pending"
        />
      </UiField>
      <p v-if="error" id="login-error" role="alert" class="text-sm text-destructive">{{ error }}</p>
      <p
        v-else-if="route.query.logout === 'unconfirmed'"
        role="status"
        class="text-sm text-destructive"
      >
        本机登录已清除，但服务器会话未能确认撤销。共享设备请手动联系管理员处理。
      </p>
      <p
        v-else-if="route.query.reason === 'expired'"
        role="status"
        class="text-sm text-muted-foreground"
      >
        会话已失效，请重新登录。
      </p>
      <UiField>
        <UiButton type="submit" size="lg" class="w-full" :disabled="pending">
          {{ pending ? '正在登录…' : '登录' }}
        </UiButton>
        <UiButton v-if="canRestore" type="button" variant="outline" :disabled="pending" @click="retrySession">重试恢复会话</UiButton>
        <UiFieldDescription class="mt-2 text-center">
          需要账号或访问权限？请联系管理员。
        </UiFieldDescription>
      </UiField>
    </UiFieldGroup>
  </form>
</template>
