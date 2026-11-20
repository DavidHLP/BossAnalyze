<script setup lang="ts">
import { computed, onMounted, ref, shallowRef, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { HugeiconsIcon } from '@hugeicons/vue'
import { ArrowRight01Icon, InformationCircleIcon, RefreshIcon, ServerStack01Icon } from '@hugeicons/core-free-icons'
import { useStorage } from '@vueuse/core'
import { checkServiceHealth } from '@/api/log/accesslog'
import { useSession } from '@/composables/useSession'
import { useTheme } from '@/composables/useTheme'
import { healthSnapshot, settingsFailure } from '@/lib/settings'
import { findMenu } from '@/lib/session'
import { THEME_STORAGE_KEY } from '@/lib/theme'

const route = useRoute()
const { state: session } = useSession()
const canViewProfile = computed(() => Boolean(findMenu(session.value.menus, '/console/profile')))
const sections = [
  { hash: '#account-access', label: '账户与访问' },
  { hash: '#service-status', label: '服务状态' },
  { hash: '#appearance', label: '界面显示偏好' },
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
  <section class="flex min-w-0 flex-col gap-5" aria-labelledby="settings-title">
    <header class="space-y-2">
      <h1 id="settings-title" class="text-3xl font-semibold tracking-tight">系统设置</h1>
      <p class="text-sm text-muted-foreground">查看账户入口与 Spark 日志服务的连通性。</p>
    </header>

    <div class="grid min-w-0 gap-4 md:grid-cols-[minmax(10rem,13.75rem)_minmax(0,1fr)] md:gap-5">
      <nav aria-label="页内导航" class="h-fit rounded-lg border bg-muted/50 p-2">
        <p class="px-2 pb-2 pt-1 text-xs font-medium text-muted-foreground">页内导航</p>
        <ul class="flex flex-wrap gap-1 md:flex-col">
          <li v-for="section in sections" :key="section.hash">
            <RouterLink
              :to="{ path: route.path, query: route.query, hash: section.hash }"
              :aria-current="activeSection === section.hash ? 'location' : undefined"
              class="relative block rounded-md px-3 py-2 text-sm font-medium hover:bg-muted focus-visible:outline-2 focus-visible:outline-ring aria-[current=location]:bg-accent-data-surface aria-[current=location]:text-accent-data-foreground"
            >
              <span v-if="activeSection === section.hash" class="absolute inset-y-2 left-0 w-0.5 rounded-full bg-accent-data" aria-hidden="true" />
              {{ section.label }}
              <span class="hidden font-mono text-xs font-normal md:block">{{ section.hash }}</span>
            </RouterLink>
          </li>
        </ul>
      </nav>

      <div class="min-w-0 space-y-4">
        <section
          id="account-access"
          tabindex="-1"
          class="scroll-mt-6 rounded-lg focus-visible:outline-2 focus-visible:outline-ring"
          aria-labelledby="account-access-title"
        >
          <UiCard class="gap-4 py-4">
            <UiCardHeader class="flex flex-wrap items-start justify-between gap-3">
              <div class="min-w-0 space-y-1">
                <h2 id="account-access-title" class="text-lg font-semibold">账户与访问</h2>
                <UiCardDescription>在个人中心查看账号基本资料、关联角色与当前访问权限。</UiCardDescription>
              </div>
              <UiButton v-if="canViewProfile" variant="outline" size="sm" as-child>
                <RouterLink to="/console/profile">
                  前往个人中心
                  <HugeiconsIcon :icon="ArrowRight01Icon" class="size-4" aria-hidden="true" />
                </RouterLink>
              </UiButton>
            </UiCardHeader>
            <UiCardContent>
              <div class="flex items-start gap-2 border-t pt-3 text-sm text-muted-foreground">
                <HugeiconsIcon :icon="InformationCircleIcon" class="mt-0.5 size-4 shrink-0 text-accent-data-foreground" aria-hidden="true" />
                <p v-if="canViewProfile">个人中心入口按当前账户的访问授权显示。</p>
                <p v-else>当前账号尚未获授权访问个人中心，请联系管理员。</p>
              </div>
            </UiCardContent>
          </UiCard>
        </section>

        <section
          id="service-status"
          tabindex="-1"
          class="scroll-mt-6 rounded-lg focus-visible:outline-2 focus-visible:outline-ring"
          aria-labelledby="service-status-title"
        >
          <UiCard class="gap-4 py-4" :aria-busy="healthState === 'loading'">
            <UiCardHeader>
              <h2 id="service-status-title" class="text-lg font-semibold">服务状态</h2>
            </UiCardHeader>
            <UiCardContent class="space-y-4">
              <div class="flex flex-wrap items-center justify-between gap-3 border-t pt-4">
                <div class="flex min-w-0 items-center gap-3">
                  <span class="grid size-10 shrink-0 place-items-center rounded-md bg-muted text-muted-foreground">
                    <HugeiconsIcon :icon="ServerStack01Icon" class="size-5" aria-hidden="true" />
                  </span>
                  <div class="min-w-0 space-y-1">
                    <h3 class="text-sm font-semibold">Spark 日志服务</h3>
                    <p class="break-all font-mono text-xs text-muted-foreground">GET /api/v1/system/logs/health</p>
                  </div>
                </div>
                <UiButton size="sm" :aria-disabled="healthState === 'loading'" @click="checkHealth">
                  <HugeiconsIcon
                    :icon="RefreshIcon"
                    class="size-4"
                    :class="{ 'motion-safe:animate-spin': healthState === 'loading' }"
                    aria-hidden="true"
                  />
                  {{ healthState === 'loading' ? '正在检查…' : '重新检查' }}
                </UiButton>
              </div>

              <div v-if="healthState === 'loading'" role="status" class="flex flex-wrap gap-3">
                <span class="sr-only">正在检查日志服务连接</span>
                <UiSkeleton class="h-5 w-24" />
                <UiSkeleton class="h-5 w-48 max-w-full" />
              </div>
              <UiAlert v-else-if="healthState !== 'ready'" variant="destructive" role="alert">
                <UiAlertTitle>
                  {{ healthState === 'forbidden' ? '当前账号无权检查服务状态' : '服务检查失败' }}
                </UiAlertTitle>
                <UiAlertDescription>
                  {{ healthState === 'forbidden' ? '请联系管理员确认访问权限。' : '本次请求未取得有效结果，请重新检查。' }}
                </UiAlertDescription>
                <UiButton v-if="healthState === 'error'" variant="outline" size="sm" class="mt-2" @click="checkHealth">
                  重试
                </UiButton>
              </UiAlert>
              <div v-else-if="health" role="status" class="flex flex-wrap items-center gap-x-5 gap-y-2">
                <div class="flex flex-wrap items-center gap-2">
                  <StatusBadge :tone="health.connected ? 'success' : 'warning'">
                    {{ health.connected ? '连接正常' : '连接降级' }}
                  </StatusBadge>
                  <span class="font-mono text-xs">{{ health.connected ? 'UP / UP' : 'DEGRADED / DOWN' }}</span>
                </div>
                <p class="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-muted-foreground">
                  检查时间（本地）
                  <time class="font-mono tabular-nums text-foreground" :datetime="new Date(health.checkedAt).toISOString()">
                    {{ dateTime.format(health.checkedAt) }}
                  </time>
                </p>
              </div>
              <p class="border-t pt-3 text-xs leading-5 text-muted-foreground">
                仅表征网关至 Spark 日志服务的连通性，不代表招聘服务或快照新鲜度。
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
          <UiCard class="gap-4 py-4">
            <UiCardHeader>
              <h2 id="appearance-title" class="text-lg font-semibold">界面显示偏好</h2>
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
      </div>
    </div>
  </section>
</template>
