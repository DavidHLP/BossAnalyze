<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { RefreshIcon, UserAccountIcon, ServerStack01Icon } from '@hugeicons/core-free-icons'
import { checkServiceHealth } from '@/api/log/accesslog'
import { healthSnapshot, settingsFailure } from '@/lib/settings'
import { findMenu } from '@/lib/session'

definePageMeta({ layout: 'console', middleware: 'auth' })
const route = useRoute()
const { state: session } = useSession()
const canViewProfile = computed(() => Boolean(findMenu(session.value.menus, '/console/profile')))
const sections = [
  { hash: '#account-access', label: '账号与访问', icon: UserAccountIcon },
  { hash: '#service-status', label: '服务状态', icon: ServerStack01Icon },
]
const activeSection = computed(() =>
  route.hash === '#service-status' ? route.hash : '#account-access',
)
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
onMounted(loadHealth)
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="settings-title">
    <h1 id="settings-title" class="text-2xl font-semibold tracking-tight">系统设置</h1>
    <div class="grid min-w-0 gap-6 lg:grid-cols-[11rem_minmax(0,1fr)]">
      <nav aria-label="设置分类" class="flex flex-wrap gap-1 self-start lg:sticky lg:top-6 lg:flex-col">
        <NuxtLink
          v-for="section in sections"
          :key="section.hash"
          :to="{ path: route.path, hash: section.hash }"
          custom
          v-slot="{ href, navigate }"
        >
          <UiButton variant="ghost" class="justify-start" as-child>
            <a
              :href="href ?? undefined"
              :aria-current="activeSection === section.hash ? 'location' : undefined"
              class="aria-[current=location]:bg-accent-data-surface aria-[current=location]:text-accent-data-foreground"
              @click="navigate"
            >
              <HugeiconsIcon :icon="section.icon" class="size-4" aria-hidden="true" />
              {{ section.label }}
            </a>
          </UiButton>
        </NuxtLink>
      </nav>
      <div class="min-w-0 space-y-5">
        <section id="account-access" class="scroll-mt-6" aria-labelledby="account-access-title">
          <UiCard class="shadow-panel">
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

        <section id="service-status" class="scroll-mt-6" aria-labelledby="service-status-title">
          <UiCard class="shadow-panel" :aria-busy="healthState === 'loading'">
            <UiCardHeader class="flex flex-wrap items-center justify-between gap-3">
              <h2 id="service-status-title" class="text-base font-semibold">服务状态</h2>
              <UiButton
                variant="outline"
                size="sm"
                :disabled="healthState === 'loading'"
                @click="loadHealth"
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
                  <div class="flex flex-wrap items-center gap-2">
                    <StatusBadge :tone="health.connected ? 'success' : 'warning'">
                      {{ health.connected ? '已连通' : '连接降级' }}
                    </StatusBadge>
                    <code class="text-xs text-muted-foreground">
                      {{ health.connected ? 'UP / UP' : 'DEGRADED / DOWN' }}
                    </code>
                  </div>
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
      </div>
    </div>
  </section>
</template>
