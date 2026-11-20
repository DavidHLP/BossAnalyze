<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import {
  RefreshIcon,
  ViewIcon,
  UserGroupIcon,
  ChartHistogramIcon,
  Clock01Icon,
  Database02Icon,
} from '@hugeicons/core-free-icons'
import { getDashboardStats } from '@/api/log/accesslog'
import { settingsFailure } from '@/lib/settings'
import { dashboardSnapshot, peakVisits, snapshotScope, snapshotTime, visitShare } from '@/lib/dashboard'
import type { DashboardSnapshot } from '@/lib/dashboard'

definePageMeta({ layout: 'console', middleware: 'auth' })
const snapshot = shallowRef<DashboardSnapshot | null>(null)
const pending = ref(true)
const failed = ref<false | 'error' | 'forbidden'>(false)
const number = new Intl.NumberFormat('zh-CN')
const metrics = computed(() => {
  const data = snapshot.value
  return [
    { title: '总访问量', value: data?.visits, note: '快照中的请求次数', icon: ViewIcon },
    { title: '独立访客', value: data?.visitors, note: '按访问 IP 去重', icon: UserGroupIcon },
    {
      title: '峰值小时访问量',
      value: data && peakVisits(data.hours),
      note: '全快照单小时最高值',
      icon: ChartHistogramIcon,
    },
    {
      title: '活跃小时数',
      value: data?.hours.filter((row) => row.count > 0).length,
      note: '全快照访问量大于 0 的小时',
      icon: Clock01Icon,
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
onMounted(load)
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="dashboard-title">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="min-w-0 space-y-2">
        <h1 id="dashboard-title" class="text-2xl font-semibold tracking-tight">数据看板</h1>
        <p class="max-w-2xl text-sm leading-6 text-muted-foreground">{{ snapshotScope }}</p>
        <p v-if="snapshot" class="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs">
          <span class="text-muted-foreground">快照生成时间</span>
          <time :datetime="snapshot.generatedAt" class="font-mono tabular-nums">
            {{ snapshotTime(snapshot.generatedAt) }}
          </time>
        </p>
      </div>
      <UiButton variant="outline" :disabled="pending" @click="load">
        <HugeiconsIcon
          :icon="RefreshIcon"
          class="size-4"
          :class="{ 'motion-safe:animate-spin': pending }"
          aria-hidden="true"
        />
        {{ pending ? '正在读取…' : '刷新快照' }}
      </UiButton>
    </header>

    <UiAlert v-if="failed" variant="destructive" role="alert">
      <UiAlertTitle>
        {{ failed === 'forbidden' ? '无权读取看板数据' : '看板数据读取失败' }}
      </UiAlertTitle>
      <UiAlertDescription>
        {{ failed === 'forbidden' ? '当前账号未获授权，请联系管理员。' : '暂时无法获取统计数据。' }}
      </UiAlertDescription>
      <UiButton v-if="failed === 'error'" size="sm" variant="outline" class="mt-2" @click="load">
        重试
      </UiButton>
    </UiAlert>
    <UiAlert v-else-if="!pending && !snapshot" role="status">
      <HugeiconsIcon :icon="Database02Icon" aria-hidden="true" />
      <UiAlertTitle>暂无可用的分析快照</UiAlertTitle>
      <UiAlertDescription>日志服务尚未返回分析快照，请在快照生成后刷新。</UiAlertDescription>
    </UiAlert>

    <div
      v-if="pending || snapshot"
      :aria-busy="pending"
      class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"
    >
      <UiCard v-for="metric in metrics" :key="metric.title" class="min-w-0 gap-3 shadow-panel">
        <UiCardHeader class="flex flex-row items-center justify-between gap-2">
          <h2 class="text-sm font-medium text-muted-foreground">{{ metric.title }}</h2>
          <HugeiconsIcon :icon="metric.icon" class="size-4 shrink-0 text-accent-data" aria-hidden="true" />
        </UiCardHeader>
        <UiCardContent class="space-y-2">
          <UiSkeleton v-if="pending" class="h-9 w-24" />
          <p v-else-if="metric.value != null" class="break-all font-mono text-3xl font-semibold tabular-nums">
            {{ number.format(metric.value) }}
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
      <DashboardVisitChart :hours="snapshot.hours" />

      <div class="grid min-w-0 items-start gap-4 lg:grid-cols-2">
        <DashboardDistributionCard
          title="浏览器分布"
          description="按浏览器统计访问次数及组内占比"
          note="按 UA 规则，带 Chrome 标识的 Edge 访问计入 Chrome。"
          :rows="snapshot.browsers"
        />
        <DashboardDistributionCard
          title="HTTP 请求方法分布"
          description="按请求方法统计访问次数及组内占比"
          :rows="snapshot.methods"
        />
      </div>

      <UiCard class="min-w-0 shadow-panel">
        <UiCardHeader>
          <h2 class="text-base font-semibold">热门访问路径</h2>
          <UiCardDescription>按访问次数排序，最多展示 10 条</UiCardDescription>
        </UiCardHeader>
        <UiCardContent>
          <UiTable v-if="snapshot.pages.length">
            <UiTableCaption class="sr-only">当前分析快照中的热门访问路径</UiTableCaption>
            <UiTableHeader>
              <UiTableRow>
                <UiTableHead class="w-12">排名</UiTableHead>
                <UiTableHead>访问路径</UiTableHead>
                <UiTableHead class="text-right">访问次数</UiTableHead>
                <UiTableHead class="text-right">占总访问量</UiTableHead>
              </UiTableRow>
            </UiTableHeader>
            <UiTableBody>
              <UiTableRow v-for="(row, index) in snapshot.pages" :key="row.label">
                <UiTableCell class="font-mono tabular-nums text-muted-foreground">
                  {{ String(index + 1).padStart(2, '0') }}
                </UiTableCell>
                <UiTableCell class="whitespace-normal break-all font-mono font-medium">
                  {{ row.label }}
                </UiTableCell>
                <UiTableCell class="text-right font-mono tabular-nums">
                  {{ number.format(row.count) }}
                </UiTableCell>
                <UiTableCell class="text-right font-mono tabular-nums text-muted-foreground">
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
