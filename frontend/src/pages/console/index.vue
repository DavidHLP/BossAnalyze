<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { HugeiconsIcon } from '@hugeicons/vue'
import { ArrowRight01Icon, Clock01Icon, RefreshIcon } from '@hugeicons/core-free-icons'
import { getUserRole, getUserPrivateInformation } from '@/api/auth/auth'
import { getDashboardStats } from '@/api/log/accesslog'
import { useSession } from '@/composables/useSession'
import { dashboardSnapshot, peakVisits, snapshotScope, snapshotTime } from '@/lib/dashboard'
import { menuIcon } from '@/lib/menu-icons'
import { findMenu, menuShortcuts } from '@/lib/session'
import { accessSnapshot, roleStatus, settingsFailure } from '@/lib/settings'

type State = 'loading' | 'ready' | 'error' | 'forbidden'
const { state: session } = useSession()
const router = useRouter()
const shortcuts = computed(() => menuShortcuts(session.value.menus)
  .filter(({ path }) => path !== '/console/profile' && !router.resolve({ path }).meta.placeholder))
const workspace = [
  { path: '/resumes', title: '我的简历' },
  { path: '/recruitment', title: '招聘分析', icon: 'ChartHistogramIcon' },
]
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
const shortcutDescriptions: Record<string, string> = {
  '/console/dashboard': '查看受保护 API 访问快照',
  '/console/users': '查看和管理系统用户',
  '/console/roles': '维护角色与访问授权',
  '/console/menus': '维护菜单与路由配置',
  '/console/settings': '查看账户入口和服务状态',
}
const metrics = computed(() => {
  const data = snapshot.value
  const peak = data && peakVisits(data.hours)
  const peakHour = data?.hours.find(({ count }) => count === peak)
  return [
    { title: '总访问量', value: data?.visits, description: '配置日志目录内的 API 请求' },
    { title: '独立访客', value: data?.visitors, description: '按 IP 去重' },
    {
      title: '快照内峰值小时',
      value: peak,
      hour: peakHour?.label.slice(11),
      description: '该小时访问次数',
    },
  ]
})

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

function refreshStats() {
  if (statsState.value === 'loading') return
  return loadStats()
}

function refresh() {
  if (pending.value) return
  return loadOverview()
}

onMounted(loadOverview)
</script>

<template>
  <!-- The role badge and metrics load independently after the router restores history. -->
  <section class="flex min-w-0 flex-col gap-5 max-md:[overflow-anchor:none]" aria-labelledby="workbench-title">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="min-w-0 space-y-2">
        <div class="flex flex-wrap items-center gap-x-5 gap-y-2">
          <h1 id="workbench-title" class="min-w-0 break-words text-3xl font-semibold tracking-tight">
            欢迎回来<span v-if="session.user?.name">，{{ session.user.name }}</span>
          </h1>
          <UiBadge v-if="access" variant="secondary" class="max-w-full break-all font-mono">
            {{ access.name }}
          </UiBadge>
        </div>
      </div>
      <UiButton :aria-disabled="pending" @click="refresh">
        <HugeiconsIcon
          :icon="RefreshIcon"
          class="size-4"
          :class="{ 'motion-safe:animate-spin': pending }"
          aria-hidden="true"
        />
        {{ pending ? '正在刷新…' : '刷新全部' }}
      </UiButton>
    </header>

    <nav aria-label="工作空间" class="grid grid-cols-2 gap-3 sm:w-fit">
      <UiButton v-for="item in workspace" :key="item.path" variant="outline" class="justify-start bg-card" as-child>
        <RouterLink :to="item.path">
          <HugeiconsIcon :icon="menuIcon(item.path, item.icon)" class="size-4" aria-hidden="true" />
          {{ item.title }}
        </RouterLink>
      </UiButton>
    </nav>

    <UiCard
      v-if="canViewDashboard"
      class="min-w-0 gap-3 py-4"
      role="region"
      aria-labelledby="overview-title"
      :aria-busy="statsState === 'loading'"
    >
      <UiCardHeader class="flex flex-wrap items-center justify-between gap-3">
        <h2 id="overview-title" class="text-lg font-semibold">日志访问概览</h2>
        <div class="flex flex-wrap gap-2">
          <UiButton variant="outline" size="sm" :aria-disabled="statsState === 'loading'" @click="refreshStats">
            <HugeiconsIcon :icon="RefreshIcon" class="size-4" aria-hidden="true" />
            刷新概览
          </UiButton>
          <UiButton variant="secondary" size="sm" class="bg-accent-data-surface text-accent-data-foreground" as-child>
            <RouterLink to="/console/dashboard">
              进入数据看板
              <HugeiconsIcon :icon="ArrowRight01Icon" class="size-4" aria-hidden="true" />
            </RouterLink>
          </UiButton>
        </div>
      </UiCardHeader>
      <UiCardContent>
        <p v-if="statsState === 'forbidden'" role="status" class="border-t pt-4 text-sm text-muted-foreground">
          当前账号无权读取日志概览，请联系管理员确认权限。
        </p>
        <UiAlert v-else-if="statsState === 'error'" variant="destructive" role="alert">
          <UiAlertTitle>日志概览读取失败</UiAlertTitle>
          <UiAlertDescription>请检查服务连接后重试。</UiAlertDescription>
          <UiButton variant="outline" size="sm" class="mt-3 justify-self-start" @click="refreshStats">
            重试日志概览
          </UiButton>
        </UiAlert>
        <template v-else>
          <dl class="grid gap-3 border-t py-4 sm:grid-cols-3 sm:gap-0">
            <div
              v-for="(metric, index) in metrics"
              :key="metric.title"
              class="min-w-0 space-y-1 border-b pb-3 last:border-b-0 last:pb-0 sm:border-b-0 sm:pb-0 sm:not-first:border-l sm:not-first:pl-4 sm:not-last:pr-4"
            >
              <dt class="text-sm text-muted-foreground">{{ metric.title }}</dt>
              <dd>
                <UiSkeleton v-if="statsState === 'loading'" class="my-1 h-8 w-24 max-w-full" />
                <span
                  v-else
                  class="break-words font-mono text-2xl font-semibold tabular-nums xl:text-3xl"
                  :class="{ 'text-accent-data-foreground': index === 0 }"
                >
                  <template v-if="metric.value != null">
                    <template v-if="metric.hour">{{ metric.hour }}:00 · </template>{{ number.format(metric.value) }}
                    <span v-if="metric.hour" class="text-xs font-normal text-muted-foreground">次</span>
                  </template>
                  <template v-else>—</template>
                </span>
              </dd>
              <dd class="text-xs text-muted-foreground">{{ metric.description }}</dd>
            </div>
          </dl>
          <p v-if="statsState === 'loading'" role="status" class="sr-only">正在读取日志快照…</p>
          <p v-else-if="!snapshot" role="status" class="border-t pt-3 text-sm text-muted-foreground">
            暂无可用的分析快照，请在快照生成后刷新概览。
          </p>
          <div v-else class="space-y-2 border-t pt-3 text-xs text-muted-foreground">
            <p class="flex flex-wrap items-center gap-x-2 gap-y-1">
              <HugeiconsIcon :icon="Clock01Icon" class="size-4" aria-hidden="true" />
              快照时间
              <time :datetime="snapshot.generatedAt" class="break-all font-mono tabular-nums">{{ snapshotTime(snapshot.generatedAt) }}</time>
            </p>
            <p>{{ snapshotScope }}</p>
          </div>
        </template>
      </UiCardContent>
    </UiCard>

    <section v-if="shortcuts.length" class="min-w-0 space-y-3" aria-labelledby="shortcuts-title">
      <h2 id="shortcuts-title" class="text-lg font-semibold">授权快捷入口</h2>
      <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <UiButton
          v-for="item in shortcuts"
          :key="item.path"
          variant="outline"
          class="h-auto min-h-20 min-w-0 justify-start gap-3 whitespace-normal bg-card p-3 text-left dark:bg-card"
          as-child
        >
          <RouterLink :to="item.path">
            <span class="grid size-9 shrink-0 place-items-center self-start rounded-md bg-muted text-muted-foreground">
              <HugeiconsIcon :icon="menuIcon(item.path, item.icon)" class="size-5" aria-hidden="true" />
            </span>
            <span class="min-w-0 flex-1 space-y-0.5">
              <span class="block break-words font-semibold">{{ item.title }}</span>
              <span class="block break-all font-mono text-xs font-normal text-muted-foreground">{{ item.path }}</span>
              <span v-if="shortcutDescriptions[item.path]" class="block break-words pt-1 text-xs font-normal text-muted-foreground">
                {{ shortcutDescriptions[item.path] }}
              </span>
            </span>
            <HugeiconsIcon :icon="ArrowRight01Icon" class="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          </RouterLink>
        </UiButton>
      </div>
    </section>

    <UiCard class="min-w-0 gap-3 py-4" :aria-busy="accessState === 'loading'">
      <UiCardHeader class="flex flex-wrap items-center justify-between gap-2">
        <h2 class="text-lg font-semibold">当前角色与权限概览</h2>
        <UiButton v-if="canViewProfile" variant="ghost" size="sm" as-child>
          <RouterLink to="/console/profile">
            查看权限明细
            <HugeiconsIcon :icon="ArrowRight01Icon" class="size-4" aria-hidden="true" />
          </RouterLink>
        </UiButton>
      </UiCardHeader>
      <UiCardContent>
        <div v-if="accessState === 'loading'" role="status" class="min-h-24 space-y-3">
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
        <div v-else-if="access" class="grid gap-4 md:grid-cols-[1fr_2fr]">
          <div class="min-w-0 space-y-2">
            <div class="flex flex-wrap items-center gap-3">
              <p class="min-w-0 break-all font-mono text-2xl font-semibold">{{ access.name }}</p>
              <StatusBadge :tone="currentRole.tone">{{ currentRole.label }}</StatusBadge>
            </div>
            <p class="whitespace-pre-wrap break-words text-sm text-muted-foreground">{{ access.remark || '暂无角色备注' }}</p>
          </div>
          <div class="min-w-0 space-y-2 border-t pt-3 md:border-l md:border-t-0 md:pl-6 md:pt-0">
            <p class="text-sm font-medium">
              <span class="mr-1 font-mono text-2xl font-semibold tabular-nums">{{ number.format(access.permissions.length) }}</span>
              项权限
            </p>
            <template v-if="access.permissions.length">
              <p class="text-xs text-muted-foreground">代表性权限 · 已去重</p>
              <ul class="flex flex-wrap gap-2">
                <li v-for="permission in access.permissions.slice(0, 4)" :key="permission" class="min-w-0">
                  <code class="inline-block break-all rounded-md bg-muted px-2 py-1 text-xs text-muted-foreground">{{ permission }}</code>
                </li>
              </ul>
            </template>
            <p v-else role="status" class="text-sm text-muted-foreground">尚未分配权限。</p>
          </div>
        </div>
      </UiCardContent>
    </UiCard>
  </section>
</template>
