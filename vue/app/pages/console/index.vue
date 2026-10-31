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
import { dashboardSnapshot } from '@/lib/dashboard'
import { accessSnapshot, settingsFailure } from '@/lib/settings'
import { findMenu, menuShortcuts } from '@/lib/session'
import { menuIcon } from '@/lib/menu-icons'

definePageMeta({ layout: 'console', middleware: 'auth' })
type State = 'loading' | 'ready' | 'error' | 'forbidden'
const { state: session } = useSession()
const shortcuts = computed(() => menuShortcuts(session.value.menus))
const canViewDashboard = computed(() =>
  Boolean(findMenu(session.value.menus, '/console/dashboard')),
)
const snapshot = shallowRef<ReturnType<typeof dashboardSnapshot>>(null)
const access = shallowRef<ReturnType<typeof accessSnapshot> | null>(null)
const statsState = ref<State>('loading')
const accessState = ref<State>('loading')
const pending = computed(() => statsState.value === 'loading' || accessState.value === 'loading')
const number = new Intl.NumberFormat('zh-CN')
const metrics = computed(() => [
  {
    title: '累计访问量',
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
    value: snapshot.value?.hours.reduce((peak, hour) => Math.max(peak, hour.count), 0),
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
async function refresh() {
  await Promise.all([loadStats(), loadAccess()])
}
onMounted(refresh)
</script>

<template>
  <section class="flex min-w-0 flex-col gap-8" aria-labelledby="workbench-title">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="min-w-0 space-y-2">
        <p class="text-xs text-muted-foreground">工作台</p>
        <h1 id="workbench-title" class="break-words text-2xl font-semibold tracking-tight">
          欢迎回来<span v-if="session.user?.name">，{{ session.user.name }}</span>
        </h1>
        <p class="text-sm text-muted-foreground">查看访问概况，从常用功能开始工作。</p>
      </div>
      <UiButton variant="outline" :disabled="pending" @click="refresh"
        ><HugeiconsIcon
          :icon="RefreshIcon"
          class="size-4 -ml-0.5"
          :class="{ 'motion-safe:animate-spin': pending }"
          aria-hidden="true"
        />{{ pending ? '正在刷新…' : '刷新概览' }}</UiButton
      >
    </header>
    <section
      class="min-w-0 space-y-4"
      aria-labelledby="overview-title"
      :aria-busy="statsState === 'loading'"
    >
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h2 id="overview-title" class="text-sm font-semibold">日志访问概览</h2>
        <UiButton v-if="canViewDashboard" variant="ghost" size="sm" as-child
          ><NuxtLink to="/console/dashboard"
            >查看数据看板<HugeiconsIcon
              :icon="ArrowRight01Icon"
              class="size-4"
              aria-hidden="true" /></NuxtLink
        ></UiButton>
      </div>
      <UiAlert v-if="statsState === 'forbidden'" role="status"
        ><UiAlertTitle>日志概览暂不可访问</UiAlertTitle
        ><UiAlertDescription
          >当前账号未获授权访问数据看板。如需访问，请联系管理员。</UiAlertDescription
        ></UiAlert
      >
      <template v-else>
        <div class="grid gap-4 sm:grid-cols-3">
          <UiCard
            v-for="metric in metrics"
            :key="metric.title"
            class="gap-0 border-[0.5px] border-border/70 py-0 shadow-none ring-0"
            ><UiCardContent class="p-5"
              ><div class="flex items-center justify-between gap-3">
                <h3 class="text-xs text-muted-foreground">{{ metric.title }}</h3>
                <HugeiconsIcon
                  :icon="metric.icon"
                  class="size-4 text-muted-foreground"
                  aria-hidden="true"
                />
              </div>
              <UiSkeleton v-if="statsState === 'loading'" class="mt-4 h-8 w-24" />
              <p v-else class="mt-3 text-3xl font-semibold tabular-nums tracking-tight">
                {{ metric.value == null ? '—' : number.format(metric.value) }}
              </p>
              <p class="mt-2 text-xs text-muted-foreground">
                {{ metric.description }}
              </p></UiCardContent
            ></UiCard
          >
        </div>
        <p v-if="statsState === 'loading'" role="status" class="text-xs text-muted-foreground">
          正在读取日志快照…
        </p>
        <UiAlert v-else-if="statsState === 'error'" role="alert"
          ><UiAlertTitle>日志概览读取失败</UiAlertTitle
          ><UiAlertDescription>请检查服务连接后重试。</UiAlertDescription
          ><UiButton variant="outline" size="sm" class="mt-3 justify-self-start" @click="loadStats"
            >重试日志概览</UiButton
          ></UiAlert
        >
        <UiAlert v-else-if="!snapshot" role="status"
          ><UiAlertTitle>暂无可用的分析快照</UiAlertTitle
          ><UiAlertDescription>当前接口尚未返回可用统计，暂不展示访问数值。</UiAlertDescription
          ><UiButton variant="outline" size="sm" class="mt-3 justify-self-start" @click="loadStats"
            >重新读取快照</UiButton
          ></UiAlert
        >
        <p v-else class="break-words text-xs text-muted-foreground">
          数据范围：当前日志分析快照 · 生成时间 {{ snapshot.generatedAt }}
        </p>
      </template>
    </section>
    <div class="grid min-w-0 items-start gap-6 xl:grid-cols-[minmax(0,1fr)_300px]">
      <UiCard class="gap-0 border-[0.5px] border-border/70 py-0 shadow-none ring-0">
        <UiCardHeader class="border-b-[0.5px] border-border/70 px-5 py-4"
          ><div class="flex flex-wrap items-center justify-between gap-3">
            <h2 class="text-sm font-semibold">快捷入口</h2>
            <span class="text-xs text-muted-foreground">{{ shortcuts.length }} 个可访问功能</span>
          </div></UiCardHeader
        >
        <UiCardContent class="p-3"
          ><div v-if="shortcuts.length" class="grid gap-2 sm:grid-cols-2">
            <UiButton
              v-for="item in shortcuts"
              :key="item.path"
              variant="ghost"
              class="h-auto min-h-20 justify-start gap-3 whitespace-normal rounded-md p-4 text-left"
              as-child
              ><NuxtLink :to="item.path"
                ><span
                  class="flex size-10 shrink-0 items-center justify-center rounded-md bg-primary/5 text-primary"
                  ><HugeiconsIcon
                    :icon="menuIcon(item.path)"
                    class="size-5"
                    aria-hidden="true" /></span
                ><span class="min-w-0 flex-1 break-words text-sm font-medium">{{ item.title }}</span
                ><HugeiconsIcon
                  :icon="ArrowRight01Icon"
                  class="size-4 shrink-0 text-muted-foreground"
                  aria-hidden="true" /></NuxtLink
            ></UiButton>
          </div>
          <div v-else role="status" class="space-y-2 px-3 py-8 text-center">
            <h3 class="text-sm font-medium">暂无可用功能</h3>
            <p class="text-sm text-muted-foreground">
              当前账号尚未获得功能菜单，请联系管理员分配访问权限。
            </p>
          </div></UiCardContent
        >
      </UiCard>
      <UiCard
        class="gap-0 border-[0.5px] border-border/70 py-0 shadow-none ring-0"
        :aria-busy="accessState === 'loading'"
      >
        <UiCardHeader class="border-b-[0.5px] border-border/70 px-5 py-4"
          ><h2 class="flex items-center gap-2 text-sm font-semibold">
            <HugeiconsIcon
              :icon="ShieldUserIcon"
              class="size-4 text-muted-foreground"
              aria-hidden="true"
            />当前角色
          </h2></UiCardHeader
        >
        <UiCardContent class="space-y-4 p-5">
          <div v-if="accessState === 'loading'" role="status" class="min-h-28 space-y-4">
            <span class="sr-only">正在读取角色信息</span><UiSkeleton class="h-5 w-36" /><UiSkeleton
              class="h-4 w-3/4"
            /><UiSkeleton class="h-4 w-20" />
          </div>
          <UiAlert v-else-if="accessState !== 'ready'" role="alert"
            ><UiAlertTitle>{{
              accessState === 'forbidden' ? '当前账号无权读取角色信息' : '角色信息读取失败'
            }}</UiAlertTitle
            ><UiAlertDescription>请重新读取角色与权限。</UiAlertDescription
            ><UiButton
              variant="outline"
              size="sm"
              class="mt-3 justify-self-start"
              @click="loadAccess"
              >重试角色信息</UiButton
            ></UiAlert
          >
          <template v-else-if="access"
            ><div class="flex flex-wrap items-center gap-2">
              <p class="min-w-0 break-all text-sm font-medium">{{ access.name }}</p>
              <UiBadge
                variant="secondary"
                :class="
                  access.status === 1
                    ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                    : 'bg-muted text-muted-foreground'
                "
                >{{
                  access.status === 1 ? '启用' : access.status === 0 ? '停用' : '状态未知'
                }}</UiBadge
              >
            </div>
            <p
              v-if="access.remark"
              class="whitespace-pre-wrap break-words text-xs leading-5 text-muted-foreground"
            >
              {{ access.remark }}
            </p>
            <p class="border-t-[0.5px] border-border/70 pt-4 text-xs text-muted-foreground">
              已授予
              <span class="font-medium tabular-nums text-foreground">{{
                access.permissions.length
              }}</span>
              项权限
            </p></template
          >
        </UiCardContent>
      </UiCard>
    </div>
  </section>
</template>
