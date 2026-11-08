<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import {
  RefreshIcon,
  UserAccountIcon,
  ServerStack01Icon,
} from '@hugeicons/core-free-icons'
import { checkServiceHealth } from '@/api/log/accesslog'
import { healthSnapshot, settingsFailure } from '@/lib/settings'
import { findMenu } from '@/lib/session'

definePageMeta({ layout: 'console', middleware: 'auth' })
const route = useRoute()
const { state: session } = useSession()
const canViewProfile = computed(() => Boolean(findMenu(session.value.menus, '/console/profile')))
type State = 'loading' | 'ready' | 'error' | 'forbidden'
const health = shallowRef<ReturnType<typeof healthSnapshot> | null>(null)
const healthState = ref<State>('loading')
const checkedAt = computed(() =>
  health.value
    ? new Intl.DateTimeFormat('zh-CN', {
        dateStyle: 'medium',
        timeStyle: 'medium',
      }).format(health.value.checkedAt)
    : '—',
)
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
  <section class="flex min-w-0 flex-col gap-8" aria-labelledby="settings-title">
    <header class="space-y-2">
      <h1 id="settings-title" class="text-2xl font-semibold tracking-tight">设置</h1>
      <p class="text-sm text-muted-foreground">查看服务连接状态，前往个人中心了解账号与访问权限。</p>
    </header>
    <div class="grid min-w-0 gap-6 lg:grid-cols-[180px_minmax(0,1fr)] lg:gap-10">
      <nav aria-label="设置分类" class="flex gap-1 self-start lg:sticky lg:top-6 lg:flex-col">
        <UiButton variant="ghost" class="justify-start" as-child
          ><a
            href="#account-access"
            :aria-current="route.hash !== '#service-status' ? 'location' : undefined"
            class="aria-[current=location]:bg-muted"
            ><HugeiconsIcon
              :icon="UserAccountIcon"
              class="size-4"
              aria-hidden="true"
            />账号与访问</a
          ></UiButton
        >
        <UiButton variant="ghost" class="justify-start" as-child
          ><a
            href="#service-status"
            :aria-current="route.hash === '#service-status' ? 'location' : undefined"
            class="aria-[current=location]:bg-muted"
            ><HugeiconsIcon
              :icon="ServerStack01Icon"
              class="size-4"
              aria-hidden="true"
            />服务状态</a
          ></UiButton
        >
      </nav>
      <div class="min-w-0 space-y-8">
        <section
          id="account-access"
          class="scroll-mt-6 space-y-5"
          aria-labelledby="account-access-title"
        >
          <div class="space-y-1.5">
            <h2 id="account-access-title" class="text-base font-semibold">账号与访问</h2>
            <p class="text-xs text-muted-foreground">账号资料与角色授权由管理员维护。</p>
          </div>
          <p class="text-sm text-muted-foreground">账号资料、当前角色与权限统一在个人中心查看。</p>
          <UiButton v-if="canViewProfile" variant="outline" as-child>
            <NuxtLink to="/console/profile">查看个人中心</NuxtLink>
          </UiButton>
          <p v-else class="text-sm text-muted-foreground">
            当前账号尚未获授权访问个人中心，请联系管理员。
          </p>
        </section>
        <section
          id="service-status"
          class="scroll-mt-6 space-y-5"
          aria-labelledby="service-status-title"
        >
          <div class="space-y-1.5">
            <h2 id="service-status-title" class="text-base font-semibold">服务状态</h2>
            <p class="text-xs text-muted-foreground">手动检查日志分析服务的连接情况。</p>
          </div>
          <UiCard
            class="gap-0 border-[0.5px] border-border/70 py-0 shadow-none ring-0"
            :aria-busy="healthState === 'loading'"
          >
            <UiCardHeader
              class="flex flex-row items-center justify-between gap-3 border-b-[0.5px] border-border/70 px-5 py-4"
              ><UiCardTitle class="flex items-center gap-2 text-sm"
                ><HugeiconsIcon
                  :icon="ServerStack01Icon"
                  class="size-4 text-muted-foreground"
                  aria-hidden="true"
                />日志服务连通性</UiCardTitle
              ><UiButton
                variant="outline"
                size="sm"
                :disabled="healthState === 'loading'"
                @click="loadHealth"
                ><HugeiconsIcon
                  :icon="RefreshIcon"
                  class="size-4"
                  :class="{ 'motion-safe:animate-spin': healthState === 'loading' }"
                  aria-hidden="true"
                />{{ healthState === 'loading' ? '正在检查…' : '重新检查' }}</UiButton
              ></UiCardHeader
            >
            <UiCardContent class="space-y-5 p-5">
              <div v-if="healthState === 'loading'" role="status" class="min-h-28 space-y-4">
                <span class="sr-only">正在检查日志服务连接</span
                ><UiSkeleton class="h-5 w-24" /><UiSkeleton class="h-4 w-48 max-w-full" />
              </div>
              <UiAlert v-else-if="healthState !== 'ready'" role="alert"
                ><UiAlertTitle>{{
                  healthState === 'forbidden' ? '当前账号无权检查服务状态' : '无法读取服务状态'
                }}</UiAlertTitle
                ><UiAlertDescription>{{
                  healthState === 'forbidden'
                    ? '请联系管理员确认访问权限。'
                    : '本次请求未取得有效结果，请重新检查。'
                }}</UiAlertDescription></UiAlert
              >
              <template v-else-if="health"
                ><div class="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p class="text-sm font-medium">网关 → Spark 日志服务</p>
                    <p class="mt-1 text-xs text-muted-foreground">
                      {{
                        health.connected
                          ? '网关已收到日志服务响应。'
                          : '网关可响应，但未能连接日志服务。'
                      }}
                    </p>
                  </div>
                  <UiBadge
                    variant="secondary"
                    :class="
                      health.connected
                        ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                        : 'bg-amber-50 text-amber-800 dark:bg-amber-950/40 dark:text-amber-300'
                    "
                    >{{ health.connected ? '连接正常' : '连接降级' }}</UiBadge
                  >
                </div>
                <dl class="text-sm">
                  <dt class="text-xs text-muted-foreground">服务端检查时间（浏览器本地时间）</dt>
                  <dd class="mt-1.5 tabular-nums">{{ checkedAt }}</dd>
                </dl></template
              >
              <p
                class="border-t-[0.5px] border-border/70 pt-4 text-xs leading-5 text-muted-foreground"
              >
                此检查仅反映日志服务连通性，不代表数据库、采集任务或分析结果可用。检查不会启动分析任务。
              </p>
            </UiCardContent>
          </UiCard>
        </section>
      </div>
    </div>
  </section>
</template>
