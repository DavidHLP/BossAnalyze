<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { RefreshIcon } from '@hugeicons/core-free-icons'
import { useStorage } from '@vueuse/core'
import { checkServiceHealth } from '@/api/log/accesslog'
import { healthSnapshot, settingsFailure } from '@/lib/settings'
import { findMenu } from '@/lib/session'
import { THEME_STORAGE_KEY } from '@/lib/theme'

definePageMeta({ layout: 'console', middleware: 'auth' })
const route = useRoute()
const { state: session } = useSession()
const canViewProfile = computed(() => Boolean(findMenu(session.value.menus, '/console/profile')))
const sections = [
  { hash: '#account-access', label: '账号与访问' },
  { hash: '#appearance', label: '界面显示偏好' },
  { hash: '#service-status', label: '服务状态' },
]
const activeSection = computed(() =>
  sections.find(({ hash }) => hash === route.hash)?.hash ?? '#account-access',
)
const themeOptions = [
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' },
  { value: 'system', label: '跟随系统' },
] as const

// useTheme and the settings control synchronize through the same VueUse storage binding.
useTheme()
const themeStorageUnavailable = ref(false)
const preference = useStorage(THEME_STORAGE_KEY, '', undefined, {
  initOnMounted: true,
  writeDefaults: false,
  onError: () => { themeStorageUnavailable.value = true },
})
const themeMode = computed(() =>
  preference.value === 'light' || preference.value === 'dark' ? preference.value : 'system',
)

function updateThemeMode(mode: unknown) {
  if (mode === 'light' || mode === 'dark' || mode === 'system') {
    preference.value = mode === 'system' ? '' : mode
  }
}

function focusSection(hash: string) {
  if (!sections.some((section) => section.hash === hash)) return
  document.getElementById(hash.slice(1))?.focus()
}

watch(() => route.hash, focusSection, { flush: 'post' })

type State = 'loading' | 'ready' | 'error' | 'forbidden'
const health = shallowRef<ReturnType<typeof healthSnapshot> | null>(null)
const healthState = ref<State>('loading')
const dateTime = new Intl.DateTimeFormat('zh-CN', {
  dateStyle: 'medium',
  timeStyle: 'medium',
})

async function loadHealth() {
  healthState.value = 'loading'
  health.value = null
  try {
    health.value = healthSnapshot(await checkServiceHealth())
    healthState.value = 'ready'
  } catch (error) {
    healthState.value = settingsFailure(error)
  }
}

function checkHealth() {
  if (healthState.value === 'loading') return
  return loadHealth()
}

onMounted(() => {
  focusSection(route.hash)
  return loadHealth()
})
</script>

<template>
  <section class="flex w-full min-w-0 max-w-3xl flex-col gap-6" aria-labelledby="settings-title">
    <h1 id="settings-title" class="text-2xl font-semibold tracking-tight">系统设置</h1>
    <nav aria-label="设置分类" class="flex flex-wrap gap-1 border-b border-border pb-2">
      <UiButton v-for="section in sections" :key="section.hash" variant="ghost" size="sm" as-child>
        <a
          :href="section.hash"
          :aria-current="activeSection === section.hash ? 'location' : undefined"
          class="aria-[current=location]:bg-accent-data-surface aria-[current=location]:text-accent-data-foreground"
        >
          {{ section.label }}
        </a>
      </UiButton>
    </nav>

    <section
      id="account-access"
      tabindex="-1"
      class="scroll-mt-6 rounded-lg focus-visible:outline-2 focus-visible:outline-ring"
      aria-labelledby="account-access-title"
    >
      <UiCard>
        <UiCardHeader>
          <h2 id="account-access-title" class="text-base font-semibold">账号与访问</h2>
        </UiCardHeader>
        <UiCardContent class="space-y-3">
          <template v-if="canViewProfile">
            <p class="text-sm text-muted-foreground">在个人中心查看账号资料、角色与权限。</p>
            <UiButton variant="outline" as-child>
              <NuxtLink to="/console/profile">前往个人中心</NuxtLink>
            </UiButton>
          </template>
          <p v-else class="text-sm text-muted-foreground">
            当前账号尚未获授权访问个人中心，请联系管理员。
          </p>
        </UiCardContent>
      </UiCard>
    </section>

    <section
      id="appearance"
      tabindex="-1"
      class="scroll-mt-6 rounded-lg focus-visible:outline-2 focus-visible:outline-ring"
      aria-labelledby="appearance-title"
    >
      <UiCard>
        <UiCardHeader>
          <h2 id="appearance-title" class="text-base font-semibold">界面显示偏好</h2>
          <UiCardDescription>外观偏好仅保存在当前浏览器，即时生效。</UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <p v-if="themeStorageUnavailable" role="status" class="text-sm text-muted-foreground">
            当前浏览器无法保存外观偏好。
          </p>
          <UiRadioGroup
            v-else
            :model-value="themeMode"
            @update:model-value="updateThemeMode"
            aria-label="外观模式"
            class="flex flex-wrap gap-2"
          >
            <label
              v-for="option in themeOptions"
              :key="option.value"
              class="flex min-h-9 cursor-pointer items-center gap-2 rounded-md border px-3 text-sm"
              :class="themeMode === option.value ? 'border-primary bg-accent-data-surface' : 'border-border'"
            >
              <UiRadioGroupItem :value="option.value" />
              {{ option.label }}
            </label>
          </UiRadioGroup>
        </UiCardContent>
      </UiCard>
    </section>

    <section
      id="service-status"
      tabindex="-1"
      class="scroll-mt-6 rounded-lg focus-visible:outline-2 focus-visible:outline-ring"
      aria-labelledby="service-status-title"
    >
      <UiCard :aria-busy="healthState === 'loading'">
        <UiCardHeader class="flex flex-wrap items-center justify-between gap-3">
          <h2 id="service-status-title" class="text-base font-semibold">服务状态</h2>
          <UiButton
            variant="outline"
            size="sm"
            :aria-disabled="healthState === 'loading'"
            @click="checkHealth"
          >
            <HugeiconsIcon
              :icon="RefreshIcon"
              class="size-4"
              :class="{ 'motion-safe:animate-spin': healthState === 'loading' }"
              aria-hidden="true"
            />
            {{ healthState === 'loading' ? '正在检查…' : '重新检查' }}
          </UiButton>
        </UiCardHeader>
        <UiCardContent class="space-y-4">
          <div v-if="healthState === 'loading'" role="status" class="min-h-24 space-y-4">
            <span class="sr-only">正在检查日志服务连接</span>
            <UiSkeleton class="h-5 w-24" />
            <UiSkeleton class="h-4 w-48 max-w-full" />
          </div>
          <UiAlert v-else-if="healthState !== 'ready'" variant="destructive" role="alert">
            <UiAlertTitle>
              {{ healthState === 'forbidden' ? '当前账号无权检查服务状态' : '无法读取服务状态' }}
            </UiAlertTitle>
            <UiAlertDescription>
              {{ healthState === 'forbidden' ? '请联系管理员确认访问权限。' : '本次请求未取得有效结果，请重新检查。' }}
            </UiAlertDescription>
          </UiAlert>
          <div v-else-if="health" role="status" class="space-y-4 rounded-md border bg-muted/30 p-4">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <p class="text-sm font-medium">网关 → Spark 日志服务</p>
              <StatusBadge :tone="health.connected ? 'success' : 'warning'">
                {{ health.connected ? '已连通' : '连接降级' }}
              </StatusBadge>
            </div>
            <dl class="text-sm">
              <dt class="text-xs text-muted-foreground">服务端检查时间（浏览器本地时间）</dt>
              <dd class="mt-1">
                <time class="font-mono tabular-nums" :datetime="new Date(health.checkedAt).toISOString()">
                  {{ dateTime.format(health.checkedAt) }}
                </time>
              </dd>
            </dl>
          </div>
          <p class="text-xs leading-5 text-muted-foreground">
            仅表征网关至 Spark 日志服务的连通性，不代表招聘服务或快照新鲜度。
          </p>
        </UiCardContent>
      </UiCard>
    </section>
  </section>
</template>
