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
import { dashboardSnapshot } from '@/lib/dashboard'
import type { DashboardSnapshot } from '@/lib/dashboard'

definePageMeta({ layout: 'console', middleware: 'auth' })
const snapshot = shallowRef<DashboardSnapshot | null>(null)
const pending = ref(true)
const failed = ref(false)
const number = new Intl.NumberFormat('zh-CN')
const metrics = computed(() => [
  {
    title: '总访问量',
    value: snapshot.value?.visits,
    description: '快照中的访问请求总数',
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
    value: snapshot.value
      ? Math.max(0, ...snapshot.value.hours.map((row) => row.count))
      : undefined,
    description: '单个小时的最高访问量',
    icon: ChartHistogramIcon,
  },
  {
    title: '活跃小时',
    value: snapshot.value?.hours.filter((row) => row.count > 0).length,
    description: '有访问记录的小时数',
    icon: Clock01Icon,
  },
])

async function load() {
  pending.value = true
  failed.value = false
  snapshot.value = null
  try {
    snapshot.value = dashboardSnapshot(await getDashboardStats())
  } catch {
    failed.value = true
  } finally {
    pending.value = false
  }
}
onMounted(load)
</script>

<template>
  <section
    class="flex min-w-0 flex-col gap-6"
    aria-labelledby="dashboard-title"
    :aria-busy="pending"
  >
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div class="space-y-2">
        <div class="flex items-center gap-3">
          <h1 id="dashboard-title" class="text-2xl font-semibold tracking-tight">数据看板</h1>
          <UiBadge variant="secondary">访问日志</UiBadge>
        </div>
        <p class="text-sm text-muted-foreground">了解访问变化、热门路径与访客使用的浏览器。</p>
      </div>
      <UiButton variant="outline" :disabled="pending" @click="load">
        <HugeiconsIcon
          :icon="RefreshIcon"
          class="size-4"
          :class="{ 'motion-safe:animate-spin': pending }"
          aria-hidden="true"
        />{{ pending ? '正在读取…' : '刷新数据' }}
      </UiButton>
    </div>

    <UiAlert v-if="failed" variant="destructive" role="alert">
      <UiAlertTitle>看板数据读取失败</UiAlertTitle>
      <UiAlertDescription>暂时无法获取统计数据，请检查服务连接后重试。</UiAlertDescription>
      <div class="mt-3"><UiButton size="sm" variant="outline" @click="load">重试</UiButton></div>
    </UiAlert>
    <UiAlert v-else-if="!pending && !snapshot" role="status">
      <HugeiconsIcon :icon="Database02Icon" aria-hidden="true" />
      <UiAlertTitle>暂无可用的分析快照</UiAlertTitle>
      <UiAlertDescription
        >当前响应未提供完整统计。请在已有分析结果可用后刷新查看；这不代表实际访问量为零。</UiAlertDescription
      >
    </UiAlert>

    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <UiCard v-for="metric in metrics" :key="metric.title">
        <UiCardHeader class="flex flex-row items-center justify-between gap-3">
          <UiCardDescription class="text-sm">{{ metric.title }}</UiCardDescription>
          <HugeiconsIcon
            :icon="metric.icon"
            class="size-4 text-muted-foreground"
            aria-hidden="true"
          />
        </UiCardHeader>
        <UiCardContent>
          <UiSkeleton v-if="pending" class="mb-3 h-9 w-24" />
          <p v-else class="mb-3 text-3xl font-semibold tracking-tight tabular-nums">
            {{ metric.value === undefined ? '—' : number.format(metric.value) }}
          </p>
          <p class="text-xs text-muted-foreground">{{ metric.description }}</p>
        </UiCardContent>
      </UiCard>
    </div>

    <div v-if="pending" class="grid gap-4 lg:grid-cols-3" role="status" aria-label="正在加载看板">
      <UiSkeleton class="h-80 lg:col-span-2" /><UiSkeleton class="h-80" />
      <span class="sr-only">正在加载统计数据</span>
    </div>
    <template v-else-if="!failed">
      <div class="grid min-w-0 gap-4 lg:grid-cols-3">
        <DashboardVisitChart :hours="snapshot?.hours ?? []" class="lg:col-span-2" />
        <DashboardDistributionCard
          title="浏览器分布"
          description="按浏览器统计访问次数及占比"
          :rows="snapshot?.browsers ?? []"
        />
      </div>
      <div class="grid min-w-0 gap-4 lg:grid-cols-3">
        <UiCard class="min-w-0 lg:col-span-2">
          <UiCardHeader
            ><UiCardTitle>热门访问路径</UiCardTitle
            ><UiCardDescription>按访问次数排序，最多展示 10 条</UiCardDescription></UiCardHeader
          >
          <UiCardContent>
            <UiTable v-if="snapshot?.pages.length">
              <UiTableCaption class="sr-only">当前分析快照中的热门访问路径</UiTableCaption>
              <UiTableHeader
                ><UiTableRow
                  ><UiTableHead class="w-12">排名</UiTableHead><UiTableHead>访问路径</UiTableHead
                  ><UiTableHead class="text-right">访问次数</UiTableHead
                  ><UiTableHead class="text-right">占总访问量</UiTableHead></UiTableRow
                ></UiTableHeader
              >
              <UiTableBody>
                <UiTableRow v-for="(row, index) in snapshot.pages" :key="row.label">
                  <UiTableCell class="text-muted-foreground">{{
                    String(index + 1).padStart(2, '0')
                  }}</UiTableCell>
                  <UiTableCell class="min-w-32 max-w-xs whitespace-normal break-all font-medium">{{
                    row.label
                  }}</UiTableCell>
                  <UiTableCell class="text-right tabular-nums">{{
                    number.format(row.count)
                  }}</UiTableCell>
                  <UiTableCell class="text-right tabular-nums text-muted-foreground"
                    >{{
                      snapshot.visits ? ((row.count / snapshot.visits) * 100).toFixed(1) : '0.0'
                    }}%</UiTableCell
                  >
                </UiTableRow>
              </UiTableBody>
            </UiTable>
            <p
              v-else
              class="flex min-h-32 items-center justify-center text-sm text-muted-foreground"
            >
              暂无访问路径数据
            </p>
          </UiCardContent>
        </UiCard>
        <DashboardDistributionCard
          title="请求方法"
          description="按 HTTP 方法统计访问次数及占比"
          :rows="snapshot?.methods ?? []"
        />
      </div>
      <p class="text-xs leading-6 text-muted-foreground">
        <template v-if="snapshot"
          >快照生成于 {{ snapshot.generatedAt.replace('T', ' ') }}（接口时间）。</template
        >
        仅展示已有日志分析结果，刷新不会启动新的分析。在线人数、性能与安全指标暂未纳入实测展示。
      </p>
    </template>
  </section>
</template>
