<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AppBrand from '@/components/AppBrand.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { loginLocation, safeReturnPath, sessionOwner, validSession } from '@/lib/session'

const route = useRoute()
const router = useRouter()
const retrying = ref(false)
const canRetry = ref(false)
const code = computed(() => route.params.code === '403' || route.params.code === '503' ? route.params.code : '404')
const retryPath = computed(() => code.value === '503' ? safeReturnPath(route.query.redirect) ?? '/console' : null)
const loginTarget = computed(() => retryPath.value ? loginLocation(retryPath.value) : '/login')
const titles = { 403: '无权访问此页面', 404: '页面不存在', 503: '暂时无法恢复会话' }
const title = computed(() => titles[code.value])
watchEffect(() => { document.title = `${title.value} · BossAnalyze` })

function hasLocalCredentials() {
  const { token, timestamp } = sessionOwner()
  return validSession(token, timestamp)
}

watchEffect(() => { canRetry.value = Boolean(retryPath.value) && hasLocalCredentials() })

async function retry() {
  if (!retryPath.value || retrying.value) return
  canRetry.value = hasLocalCredentials()
  if (!canRetry.value) return
  retrying.value = true
  try {
    await router.replace(retryPath.value)
  } finally {
    retrying.value = false
  }
}
</script>

<template>
  <div class="flex min-h-svh flex-col bg-background px-6 sm:px-10">
    <header class="flex h-16 items-center justify-between">
      <AppBrand />
      <ThemeToggle />
    </header>
    <main class="grid flex-1 place-items-center pb-16">
      <section class="flex w-full max-w-lg flex-col gap-5 rounded-lg border bg-card p-6 text-card-foreground sm:p-8">
        <p class="font-mono text-5xl font-semibold text-accent-data-foreground">{{ code }}</p>
        <h1 class="text-2xl font-semibold tracking-tight">{{ title }}</h1>
        <UiAlert v-if="canRetry">
          <UiAlertTitle>已保留当前登录凭据</UiAlertTitle>
          <UiAlertDescription>重试后将返回原页面，并重新确认访问权限。</UiAlertDescription>
        </UiAlert>
        <p v-else-if="retryPath" class="text-sm text-muted-foreground">
          本机没有有效的登录凭据，请重新登录。
        </p>
        <p v-else class="text-sm text-muted-foreground">
          {{ code === '403' ? '当前账号没有此页面的访问权限。' : '请检查页面地址，或返回工作台继续操作。' }}
        </p>
        <div class="flex flex-wrap gap-2">
          <UiButton v-if="canRetry" type="button" :aria-disabled="retrying" @click="retry">
            {{ retrying ? '正在重试…' : '重试恢复会话' }}
          </UiButton>
          <UiButton :variant="retryPath ? 'outline' : 'default'" as-child><RouterLink to="/console">返回工作台</RouterLink></UiButton>
          <UiButton :variant="retryPath && !canRetry ? 'default' : 'outline'" as-child>
            <RouterLink :to="loginTarget">前往登录</RouterLink>
          </UiButton>
        </div>
      </section>
    </main>
  </div>
</template>
