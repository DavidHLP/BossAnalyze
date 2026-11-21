<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import { HugeiconsIcon } from '@hugeicons/vue'
import {
  RefreshIcon,
  ViewIcon,
  UserGroupIcon,
  ChartHistogramIcon,
  Clock01Icon,
  Database02Icon,
  Globe02Icon,
  ComputerTerminal01Icon,
} from '@hugeicons/core-free-icons'
import { getDashboardStats } from '@/api/log/accesslog'
import { settingsFailure } from '@/lib/settings'
import { dashboardSnapshot, peakVisits, snapshotScope, snapshotTime, visitShare } from '@/lib/dashboard'
import type { DashboardSnapshot } from '@/lib/dashboard'
import DistributionCard from '@/components/dashboard/DistributionCard.vue'
import VisitChart from '@/components/dashboard/VisitChart.vue'

const snapshot = shallowRef<DashboardSnapshot | null>(null)
const pending = ref(true)
const failed = ref<false | 'error' | 'forbidden'>(false)
const number = new Intl.NumberFormat('zh-CN')
const metrics = computed(() => {
  const data = snapshot.value
  const peak = data && peakVisits(data.hours)
  const peakHour = data?.hours.find(({ count }) => count === peak)
  return [
    { title: '总访问量', value: data?.visits ?? null, note: '快照累计访问次数', icon: ViewIcon },
    { title: '独立访客', value: data?.visitors ?? null, note: '按 IP 去重', icon: UserGroupIcon },
    {
      title: '全快照峰值小时',
      value: peak,
      hour: peakHour?.label.slice(11),
      note: '峰值小时内访问次数',
      icon: Clock01Icon,
    },
    {
      title: '活跃小时数',
      value: data?.hours.filter((row) => row.count > 0).length ?? null,
      note: '访问量大于零的小时',
      icon: ChartHistogramIcon,
    },
  ]
})

async function load() {
  pending.value = true
  failed.value = false
  snapshot.value = null
  try {
    snapshot.value = dashboardSnapshot(await getDashboardStats())
  } catch (error) {
    failed.value = settingsFailure(error)
  } finally {
    pending.value = false
  }
}

function refresh() {
  if (pending.value) return
  return load()
}

onMounted(load)
</script>

<template>
  <section class="flex min-w-0 flex-col gap-4" aria-labelledby="dashboard-title">
    <header class="space-y-2">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <h1 id="dashboard-title" class="text-3xl font-semibold tracking-tight">数据看板</h1>
        <div class="flex flex-wrap gap-2">
          <UiButton as-child variant="outline"><RouterLink to="/console/logs">扩展日志分析</RouterLink></UiButton>
          <UiButton :aria-disabled="pending" @click="refresh">
            <HugeiconsIcon
              :icon="RefreshIcon"
              :class="{ 'motion-safe:animate-spin': pending }"
              data-icon="inline-start"
              aria-hidden="true"
            />
            {{ pending ? '正在读取…' : '刷新快照' }}
          </UiButton>
        </div>
      </div>
      <div class="flex flex-wrap items-baseline gap-x-4 gap-y-2 text-xs text-muted-foreground">
        <p v-if="snapshot" class="flex flex-wrap items-center gap-x-2 gap-y-1">
          <HugeiconsIcon :icon="Clock01Icon" class="size-4" aria-hidden="true" />
          快照时间
          <time :datetime="snapshot.generatedAt" class="break-all font-mono tabular-nums text-foreground">
            {{ snapshotTime(snapshot.generatedAt) }}
          </time>
        </p>
        <p>{{ snapshotScope }}</p>
      </div>
    </header>

    <UiAlert v-if="failed" variant="destructive" role="alert">
      <UiAlertTitle>
        {{ failed === 'forbidden' ? '无权读取看板数据' : '看板数据读取失败' }}
      </UiAlertTitle>
      <UiAlertDescription>
        {{ failed === 'forbidden' ? '当前账号未获授权，请联系管理员。' : '暂时无法获取统计数据。' }}
      </UiAlertDescription>
      <UiButton v-if="failed === 'error'" size="sm" variant="outline" class="mt-2" @click="refresh">
        重试
      </UiButton>
    </UiAlert>
    <UiAlert v-else-if="!pending && !snapshot" role="status">
      <HugeiconsIcon :icon="Database02Icon" aria-hidden="true" />
      <UiAlertTitle>暂无可用的分析快照</UiAlertTitle>
      <UiAlertDescription>日志服务尚未返回分析快照，请在快照生成后刷新。</UiAlertDescription>
    </UiAlert>

    <div
      v-if="!failed"
      :aria-busy="pending"
      class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"
    >
      <UiCard v-for="(metric, index) in metrics" :key="metric.title" class="min-w-0 gap-1 py-3">
        <UiCardHeader class="flex flex-row items-center justify-between gap-2 px-4">
          <h2 class="text-sm font-medium text-muted-foreground">{{ metric.title }}</h2>
          <HugeiconsIcon :icon="metric.icon" class="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
        </UiCardHeader>
        <UiCardContent class="space-y-1 px-4">
          <UiSkeleton v-if="pending" class="h-9 w-24" />
          <p
            v-else
            class="break-words font-mono text-2xl font-semibold tabular-nums"
            :class="{ 'text-accent-data-foreground': index === 0 }"
          >
            <template v-if="metric.value != null">
              <template v-if="metric.hour">
                {{ metric.hour }} <span class="text-xs font-normal">时</span> ·
              </template>{{ number.format(metric.value) }}
            </template>
            <template v-else>
              <span aria-hidden="true">—</span>
              <span class="sr-only">暂无数据</span>
            </template>
          </p>
          <p class="text-xs text-muted-foreground">{{ metric.note }}</p>
        </UiCardContent>
      </UiCard>
    </div>

    <div v-if="pending" role="status" class="space-y-4">
      <span class="sr-only">正在加载看板数据</span>
      <UiSkeleton class="h-80" />
      <div class="grid gap-4 lg:grid-cols-2">
        <UiSkeleton class="h-40" />
        <UiSkeleton class="h-40" />
      </div>
    </div>
    <template v-else-if="snapshot">
      <VisitChart :hours="snapshot.hours" />

      <div class="grid min-w-0 items-start gap-4 lg:grid-cols-2">
        <DistributionCard
          title="浏览器分布"
          :icon="Globe02Icon"
          note="按 UA 规则，带 Chrome 标识的 Edge 访问计入 Chrome。"
          :rows="snapshot.browsers"
        />
        <DistributionCard
          title="HTTP 请求方法分布"
          :icon="ComputerTerminal01Icon"
          note="占比以该组所示请求次数为分母。"
          :rows="snapshot.methods"
        />
      </div>

      <UiCard class="min-w-0 gap-2 py-3">
        <UiCardHeader class="px-5">
          <h2 class="text-base font-semibold">热门访问路径 Top 10</h2>
        </UiCardHeader>
        <UiCardContent class="px-5">
          <UiTable v-if="snapshot.pages.length">
            <UiTableCaption class="sr-only">当前分析快照中的热门访问路径</UiTableCaption>
            <UiTableHeader>
              <UiTableRow class="bg-muted/50">
                <UiTableHead class="h-auto w-12 py-1 text-xs">序号</UiTableHead>
                <UiTableHead class="h-auto py-1 text-xs">API 请求路径</UiTableHead>
                <UiTableHead class="h-auto py-1 text-right text-xs">访问次数</UiTableHead>
                <UiTableHead class="h-auto py-1 text-right text-xs">占快照总访问量</UiTableHead>
              </UiTableRow>
            </UiTableHeader>
            <UiTableBody>
              <UiTableRow v-for="(row, index) in snapshot.pages" :key="row.label">
                <UiTableCell class="py-1 text-xs font-mono tabular-nums text-muted-foreground">
                  {{ String(index + 1).padStart(2, '0') }}
                </UiTableCell>
                <UiTableCell class="py-1 text-xs whitespace-normal break-all font-mono font-medium">
                  {{ row.label }}
                </UiTableCell>
                <UiTableCell class="py-1 text-xs text-right font-mono tabular-nums">
                  {{ number.format(row.count) }}
                </UiTableCell>
                <UiTableCell class="py-1 text-xs text-right font-mono tabular-nums text-muted-foreground">
                  {{ visitShare(row.count, snapshot.visits).toFixed(1) }}%
                </UiTableCell>
              </UiTableRow>
            </UiTableBody>
          </UiTable>
          <p v-else class="py-8 text-center text-sm text-muted-foreground">暂无访问路径数据</p>
        </UiCardContent>
      </UiCard>
    </template>
  </section>
</template>
