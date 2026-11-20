<script setup lang="ts">
import { isNuxtError, type NuxtError } from '#app'

const props = defineProps<{ error: NuxtError }>()
const router = useRouter()
const nuxtApp = useNuxtApp()
const activeError = useError()
const pending = ref(false)
const retryPath = computed(() => {
  const data = props.error.data as { sessionRestore?: boolean; redirect?: string } | undefined
  return props.error.statusCode === 503 && data?.sessionRestore === true ? data.redirect : null
})
const title = computed(() => {
  if (retryPath.value) return '暂时无法恢复会话'
  if (props.error.statusCode === 403) return '无权访问此页面'
  if (props.error.statusCode === 404) return '页面不存在'
  return '页面暂时无法加载'
})
const description = computed(() => {
  if (props.error.statusCode === 403) {
    return '当前账号没有此页面的访问权限，请返回工作台或使用其他账号登录。'
  }
  if (props.error.statusCode === 404) return '请检查页面地址，或返回工作台继续操作。'
  return '请返回工作台继续操作，或稍后重试。'
})
useHead({ title: () => `${title.value} · BossAnalyze` })

async function leaveError(redirect: string) {
  if (pending.value) return
  pending.value = true
  try {
    const { fullPath, path, query, hash } = router.resolve(redirect)
    if (fullPath === router.currentRoute.value.fullPath) {
      // Same-location retries must run the route middleware again.
      const failure = await router.replace({ path, query, hash, force: true })
      if (!failure && activeError.value) await nuxtApp.runWithContext(() => clearError())
    } else {
      await clearError({ redirect })
    }
  } catch (cause) {
    if (!isNuxtError(cause)) throw cause
    activeError.value = cause
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div class="flex min-h-svh flex-col bg-background px-6 text-foreground sm:px-10">
    <header class="flex h-16 items-center justify-between gap-4">
      <AppBrand />
      <ThemeToggle />
    </header>
    <main class="grid flex-1 place-items-center pb-16">
      <section
        class="w-full max-w-lg space-y-5 rounded-lg border bg-card p-6 text-card-foreground sm:p-8"
        aria-labelledby="error-title"
      >
        <p class="font-mono text-5xl font-semibold tracking-tight text-accent-data-foreground tabular-nums">
          {{ error.statusCode }}
        </p>
        <h1 id="error-title" class="text-2xl font-semibold tracking-tight">{{ title }}</h1>
        <div
          v-if="retryPath"
          class="space-y-1 rounded-md bg-status-warning-surface p-3 text-sm text-status-warning-foreground"
        >
          <h2 class="font-semibold">已保留当前登录凭据</h2>
          <p>重试后将返回原页面，并重新确认访问权限。</p>
        </div>
        <p v-else class="text-sm leading-relaxed text-muted-foreground">{{ description }}</p>
        <div class="flex flex-wrap gap-2" :aria-busy="pending">
          <UiButton
            v-if="retryPath"
            :disabled="pending"
            @click="leaveError(retryPath)"
          >
            {{ pending ? '正在重试…' : '重试恢复会话' }}
          </UiButton>
          <UiButton
            :variant="retryPath ? 'outline' : 'default'"
            :disabled="pending"
            @click="leaveError('/console')"
          >
            返回工作台
          </UiButton>
          <UiButton
            variant="outline"
            :disabled="pending"
            @click="leaveError('/login')"
          >
            前往登录
          </UiButton>
        </div>
      </section>
    </main>
  </div>
</template>
