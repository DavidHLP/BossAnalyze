<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import {
  RefreshIcon,
  ArrowRight01Icon,
  ViewIcon,
  UserGroupIcon,
  ChartHistogramIcon,
  ShieldUserIcon,
} from '@hugeicons/core-free-icons'
import { getDashboardStats } from '@/api/log/accesslog'
import { getUserRole, getUserPrivateInformation } from '@/api/auth/auth'
import { dashboardSnapshot, peakVisits, snapshotScope, snapshotTime } from '@/lib/dashboard'
import { accessSnapshot, roleStatus, settingsFailure } from '@/lib/settings'
import { findMenu, menuShortcuts } from '@/lib/session'
import { menuIcon } from '@/lib/menu-icons'

definePageMeta({ layout: 'console', middleware: 'auth' })
type State = 'loading' | 'ready' | 'error' | 'forbidden'
const { state: session } = useSession()
const shortcuts = computed(() => menuShortcuts(session.value.menus))
const canViewDashboard = computed(() =>
  Boolean(findMenu(session.value.menus, '/console/dashboard')),
)
const canViewProfile = computed(() => Boolean(findMenu(session.value.menus, '/console/profile')))
const snapshot = shallowRef<ReturnType<typeof dashboardSnapshot>>(null)
const access = shallowRef<ReturnType<typeof accessSnapshot> | null>(null)
const statsState = ref<State>('loading')
const currentRole = computed(() => roleStatus(access.value?.status ?? null))
const accessState = ref<State>('loading')
const pending = computed(() => statsState.value === 'loading' || accessState.value === 'loading')
const number = new Intl.NumberFormat('zh-CN')
const metrics = computed(() => [
  {
    title: '总访问量',
    value: snapshot.value?.visits,
    description: '快照记录的访问请求数',
    icon: ViewIcon,
  },
  {
    title: '独立访客',
    value: snapshot.value?.visitors,
    description: '按访问 IP 去重',
    icon: UserGroupIcon,
  },
  {
    title: '峰值小时访问量',
    value: snapshot.value ? peakVisits(snapshot.value.hours) : undefined,
    description: '快照中单小时最高访问量',
    icon: ChartHistogramIcon,
  },
])
async function loadStats() {
  snapshot.value = null
  if (!canViewDashboard.value) {
    statsState.value = 'forbidden'
    return
  }
  statsState.value = 'loading'
  try {
    snapshot.value = dashboardSnapshot(await getDashboardStats())
    statsState.value = 'ready'
  } catch (error) {
    statsState.value = settingsFailure(error)
  }
}
async function loadAccess() {
  accessState.value = 'loading'
  access.value = null
  try {
    const [role, permissions] = await Promise.all([getUserRole(), getUserPrivateInformation()])
    access.value = accessSnapshot(role, permissions)
    accessState.value = 'ready'
  } catch (error) {
    accessState.value = settingsFailure(error)
  }
}
function loadOverview() {
  return Promise.all([loadStats(), loadAccess()])
}

function refresh() {
  if (pending.value) return
  return loadOverview()
}

onMounted(loadOverview)
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="workbench-title">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="min-w-0 space-y-2">
        <h1 id="workbench-title" class="break-words text-2xl font-semibold tracking-tight">
          欢迎回来<span v-if="session.user?.name">，{{ session.user.name }}</span>
        </h1>
        <p class="text-sm text-muted-foreground">从快捷入口进入已授权的功能。</p>
      </div>
      <UiButton variant="outline" :aria-disabled="pending" @click="refresh">
        <HugeiconsIcon
          :icon="RefreshIcon"
          class="size-4"
          :class="{ 'motion-safe:animate-spin': pending }"
          aria-hidden="true"
        />
        {{ pending ? '正在刷新…' : canViewDashboard ? '刷新概览' : '刷新角色信息' }}
      </UiButton>
    </header>

    <section class="min-w-0 space-y-3" aria-labelledby="shortcuts-title">
      <h2 id="shortcuts-title" class="text-base font-semibold">快捷入口</h2>
      <div v-if="shortcuts.length" class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <UiButton
          v-for="item in shortcuts"
          :key="item.path"
          variant="outline"
          class="h-auto min-h-16 min-w-0 justify-start gap-3 whitespace-normal bg-card p-3 text-left dark:bg-card"
          as-child
        >
          <NuxtLink :to="item.path">
            <span class="flex size-9 shrink-0 items-center justify-center rounded-md bg-accent-data-surface text-accent-data-foreground">
              <HugeiconsIcon :icon="menuIcon(item.path, item.icon)" class="size-5" aria-hidden="true" />
            </span>
            <span class="min-w-0 flex-1 break-words font-medium">{{ item.title }}</span>
            <HugeiconsIcon :icon="ArrowRight01Icon" class="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          </NuxtLink>
        </UiButton>
      </div>
      <div v-else class="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-lg border border-border bg-card p-4">
        <p role="status" class="text-sm text-muted-foreground">尚未分配管理功能，如需访问请联系管理员。</p>
        <UiButton variant="outline" size="sm" as-child>
          <NuxtLink to="/recruitment">查看招聘分析</NuxtLink>
        </UiButton>
      </div>
    </section>

    <section
      class="min-w-0 space-y-3"
      aria-labelledby="overview-title"
      :aria-busy="statsState === 'loading'"
    >
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h2 id="overview-title" class="text-base font-semibold">日志访问概览</h2>
        <UiButton v-if="canViewDashboard" variant="ghost" size="sm" as-child>
          <NuxtLink to="/console/dashboard">
            进入数据看板
            <HugeiconsIcon :icon="ArrowRight01Icon" class="size-4" aria-hidden="true" />
          </NuxtLink>
        </UiButton>
      </div>
      <p v-if="!canViewDashboard || statsState === 'forbidden'" role="status" class="text-sm text-muted-foreground">
        当前账号无权读取日志概览，请联系管理员确认权限。
      </p>
      <template v-else>
        <div
          v-if="statsState === 'loading' || snapshot"
          class="grid gap-3 sm:grid-cols-3"
        >
          <UiCard
            v-for="metric in metrics"
            :key="metric.title"
            class="min-w-0 gap-3"
          >
            <UiCardHeader class="flex flex-row items-center justify-between gap-2">
              <h3 class="text-sm font-medium text-muted-foreground">{{ metric.title }}</h3>
              <HugeiconsIcon :icon="metric.icon" class="size-4 shrink-0 text-accent-data" aria-hidden="true" />
            </UiCardHeader>
            <UiCardContent class="space-y-2">
              <UiSkeleton v-if="statsState === 'loading'" class="h-9 w-24" />
              <p v-else class="break-all font-mono text-3xl font-semibold tabular-nums">
                {{ metric.value == null ? '—' : number.format(metric.value) }}
              </p>
              <p class="text-xs text-muted-foreground">{{ metric.description }}</p>
            </UiCardContent>
          </UiCard>
        </div>
        <p v-if="statsState === 'loading'" role="status" class="text-xs text-muted-foreground">
          正在读取日志快照…
        </p>
        <UiAlert v-else-if="statsState === 'error'" variant="destructive" role="alert">
          <UiAlertTitle>日志概览读取失败</UiAlertTitle>
          <UiAlertDescription>请检查服务连接后重试。</UiAlertDescription>
          <UiButton variant="outline" size="sm" class="mt-3 justify-self-start" @click="loadStats">
            重试日志概览
          </UiButton>
        </UiAlert>
        <UiAlert v-else-if="!snapshot" role="status">
          <UiAlertTitle>暂无可用的分析快照</UiAlertTitle>
          <UiAlertDescription>当前接口尚未返回可用统计，暂不展示访问数值。</UiAlertDescription>
          <UiButton variant="outline" size="sm" class="mt-3 justify-self-start" @click="loadStats">
            重新读取快照
          </UiButton>
        </UiAlert>
        <p v-else class="break-words text-xs text-muted-foreground">
          <span class="block">快照生成时间 <span class="font-mono tabular-nums">{{ snapshotTime(snapshot.generatedAt) }}</span></span>
          <span class="mt-1 block">{{ snapshotScope }}</span>
        </p>
      </template>
    </section>

    <UiCard :aria-busy="accessState === 'loading'">
      <UiCardHeader class="flex flex-wrap items-center justify-between gap-2">
        <h2 class="flex items-center gap-2 text-base font-semibold">
          <HugeiconsIcon :icon="ShieldUserIcon" class="size-4 text-muted-foreground" aria-hidden="true" />
          当前角色与权限
        </h2>
        <UiButton v-if="canViewProfile" variant="ghost" size="sm" as-child>
          <NuxtLink to="/console/profile">
            查看权限明细
            <HugeiconsIcon :icon="ArrowRight01Icon" class="size-4" aria-hidden="true" />
          </NuxtLink>
        </UiButton>
      </UiCardHeader>
      <UiCardContent class="space-y-3">
        <div v-if="accessState === 'loading'" role="status" class="min-h-28 space-y-3">
          <span class="sr-only">正在读取角色信息</span>
          <UiSkeleton class="h-5 w-36" />
          <UiSkeleton class="h-4 w-3/4" />
          <UiSkeleton class="h-4 w-20" />
        </div>
        <UiAlert v-else-if="accessState !== 'ready'" variant="destructive" role="alert">
          <UiAlertTitle>
            {{ accessState === 'forbidden' ? '当前账号无权读取角色信息' : '角色信息读取失败' }}
          </UiAlertTitle>
          <UiAlertDescription>请重新读取角色与权限。</UiAlertDescription>
          <UiButton variant="outline" size="sm" class="mt-3 justify-self-start" @click="loadAccess">
            重试角色信息
          </UiButton>
        </UiAlert>
        <template v-else-if="access">
          <div class="flex flex-wrap items-center gap-2">
            <p class="min-w-0 break-all text-sm font-medium">{{ access.name }}</p>
            <StatusBadge :tone="currentRole.tone">{{ currentRole.label }}</StatusBadge>
          </div>
          <p class="whitespace-pre-wrap break-words text-xs leading-5 text-muted-foreground">
            {{ access.remark || '暂无角色备注' }}
          </p>
          <p class="border-t border-border pt-3 text-xs text-muted-foreground">
            <template v-if="access.permissions.length">
              已授予 <span class="font-mono font-medium tabular-nums text-foreground">{{ access.permissions.length }}</span> 项权限
            </template>
            <template v-else>尚未分配权限（<span class="font-mono tabular-nums">0</span> 项）</template>
          </p>
        </template>
      </UiCardContent>
    </UiCard>
  </section>
</template>
