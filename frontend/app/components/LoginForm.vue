<script setup lang="ts">
import { AlertCircleIcon, ViewIcon, ViewOffIcon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/vue'
import { loginDestination, loginFailure } from '@/lib/session'

const session = useSession()
const route = useRoute()
const email = ref('')
const password = ref('')
const showPassword = ref(false)
const action = ref<'login' | 'restore' | null>(null)
const pending = computed(() => action.value !== null)
const error = ref('')
const canRestore = ref(false)

function updateRestoreAvailability() {
  canRestore.value = Boolean(localStorage.getItem('token'))
}

onMounted(updateRestoreAvailability)
watch(() => session.state.value.invalidated, (invalidated) => {
  if (invalidated) updateRestoreAvailability()
})

async function retrySession() {
  if (pending.value) return
  action.value = 'restore'
  error.value = ''
  try {
    if (await session.restore()) {
      await navigateTo(loginDestination(route.query.redirect, session.state.value.menus), {
        replace: true,
      })
    } else {
      error.value = '会话已失效，请重新登录。'
    }
  } catch (cause) {
    error.value = cause && typeof cause === 'object' && 'code' in cause && cause.code === 401
      ? '会话已失效，请重新登录。'
      : '暂时无法恢复会话，请重试。'
  } finally {
    updateRestoreAvailability()
    action.value = null
  }
}

async function submit() {
  if (pending.value) return
  action.value = 'login'
  error.value = ''
  try {
    await session.signIn(email.value.trim(), password.value)
    password.value = ''
    await navigateTo(loginDestination(route.query.redirect, session.state.value.menus), {
      replace: true,
    })
  } catch (cause) {
    updateRestoreAvailability()
    error.value = canRestore.value
      ? '登录凭据已获取，但会话恢复失败，请重试恢复。'
      : loginFailure(cause)
  } finally {
    action.value = null
  }
}
</script>

<template>
  <section class="space-y-7" aria-labelledby="login-title">
    <div class="space-y-2">
      <h1 id="login-title" class="text-3xl font-semibold tracking-tight">登录你的账号</h1>
      <p class="text-sm text-muted-foreground">使用系统分配的邮箱和密码登录。</p>
    </div>

    <form :aria-busy="pending" @submit.prevent="submit">
      <UiFieldGroup class="gap-5">
        <UiField>
          <UiFieldLabel for="email" class="text-sm">邮箱</UiFieldLabel>
          <UiInput
            id="email"
            v-model="email"
            name="email"
            type="email"
            autocomplete="username"
            :aria-describedby="error ? 'login-error' : undefined"
            placeholder="请输入邮箱"
            required
            autofocus
            :readonly="pending"
            :aria-disabled="pending"
          />
        </UiField>
        <UiField>
          <UiFieldLabel for="password" class="text-sm">密码</UiFieldLabel>
          <div class="relative">
            <UiInput
              id="password"
              v-model="password"
              name="password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              :aria-describedby="error ? 'login-error' : undefined"
              placeholder="请输入密码"
              class="pr-10"
              required
              :readonly="pending"
              :aria-disabled="pending"
            />
            <UiButton
              type="button"
              variant="ghost"
              size="icon"
              class="absolute top-1/2 right-1 -translate-y-1/2 text-muted-foreground aria-disabled:opacity-50"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              :aria-disabled="pending"
              aria-controls="password"
              @click="!pending && (showPassword = !showPassword)"
            >
              <HugeiconsIcon :icon="showPassword ? ViewOffIcon : ViewIcon" aria-hidden="true" />
            </UiButton>
          </div>
        </UiField>
        <p
          v-if="error"
          id="login-error"
          role="alert"
          class="flex items-start gap-2 rounded-md bg-status-danger-surface p-3 text-sm text-status-danger-foreground"
        >
          <HugeiconsIcon :icon="AlertCircleIcon" class="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <span>{{ error }}</span>
        </p>
        <p
          v-else-if="route.query.logout === 'unconfirmed'"
          role="status"
          class="rounded-md bg-status-warning-surface p-3 text-sm text-status-warning-foreground"
        >
          本机登录已清除，但服务器会话未能确认撤销。共享设备请手动联系管理员处理。
        </p>
        <p
          v-else-if="route.query.reason === 'expired'"
          role="status"
          class="rounded-md bg-status-neutral-surface p-3 text-sm text-status-neutral-foreground"
        >
          会话已失效，请重新登录。
        </p>

        <UiButton type="submit" size="lg" class="w-full aria-disabled:opacity-50" :aria-disabled="pending">
          {{ action === 'login' ? '正在登录…' : '登录' }}
        </UiButton>
      </UiFieldGroup>
    </form>

    <div v-if="canRestore" class="space-y-3 rounded-lg border bg-card p-4" :aria-busy="action === 'restore'">
      <p class="text-sm text-card-foreground">本机已有登录凭据</p>
      <UiButton
        type="button"
        variant="outline"
        class="w-full aria-disabled:opacity-50"
        :aria-disabled="pending"
        @click="retrySession"
      >
        {{ action === 'restore' ? '正在恢复会话…' : '重试恢复会话' }}
      </UiButton>
    </div>

    <p class="text-center text-sm text-muted-foreground">需要账号或访问权限？请联系系统管理员。</p>
  </section>
</template>
