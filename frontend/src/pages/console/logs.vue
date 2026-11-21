<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, shallowReactive } from 'vue'
import { RiPlayLine, RiRefreshLine } from '@remixicon/vue'
import { getCompleteLogStats, getDashboardStats, getHeatMapData, getIpStatistics, triggerLogAnalysis } from '@/api/log/accesslog'
import LogHeatmap from '@/components/dashboard/LogHeatmap.vue'
import { useSession } from '@/composables/useSession'
import { heatBuckets, ipDistribution, logTotals, weekdayAverages, weekdayDistribution } from '@/lib/log-analysis'
import { permissionDenied } from '@/lib/rbac-mutations'

const session = useSession()
const allowed = computed(() => session.state.value.permissions.includes('logs.analysis.trigger'))

function logResource<T>(read: () => Promise<T>) {
  const state = shallowReactive<{ data: T | null; loading: boolean; error: string }>({ data: null, loading: false, error: '' })
  async function load() {
    if (state.loading) return
    const owner = session.capture()
    state.loading = true
    state.error = ''
    try {
      const result = await read()
      if (session.owns(owner)) state.data = result
    } catch (cause) {
      if (session.owns(owner)) {
        const forbidden = permissionDenied(cause)
        if (forbidden) state.data = null
        state.error = forbidden ? '当前账号缺少日志读取权限。' : '日志服务暂时不可用，请重试读取。'
      }
    } finally {
      state.loading = false
    }
  }
  return { state, load }
}

const { state: snapshot, load: readSnapshot } = logResource(async () => {
  const data = await getDashboardStats()
  return data === null ? null : weekdayDistribution(data)
})
const { state: totals, load: readTotals } = logResource(async () => logTotals(await getCompleteLogStats()))
const { state: heat, load: readHeat } = logResource(async () => heatBuckets(await getHeatMapData()))
const { state: geo, load: readGeo } = logResource(async () => ipDistribution(await getIpStatistics()))
const refreshPending = computed(() => [snapshot, totals, heat, geo].some(part => part.loading))
const peak = computed(() => heat.data?.hours.reduce<{ label: string; count: number } | null>(
  (highest, point) => !highest || point.count > highest.count ? point : highest, null,
))
const averages = computed(() => weekdayAverages(heat.data?.hours ?? []))
const number = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 1 })
const display = (value: number | null | undefined) => value == null ? '—' : number.format(value)
const weekdayMax = computed(() => Math.max(1, ...(snapshot.data?.days.map(day => day.count ?? 0) ?? [])))
const metrics = computed(() => [
  { name: '快照总访问量', value: display(totals.data?.visits), note: '快照累计访问次数' },
  { name: '独立 IP 数', value: display(totals.data?.ips), note: '按来源 IP 去重' },
  { name: '热力峰值时段', value: peak.value?.label ?? '—', note: peak.value ? display(peak.value.count) + ' 次访问' : '全快照峰值小时' },
  { name: '覆盖地理区域', value: geo.data?.records.length ? display(geo.data.known) : '—', note: '已知国家 / 地区' },
])
const groups = computed(() => [
  { name: '国家 / 地区', items: geo.data?.countries ?? [] },
  { name: '城市', items: geo.data?.cities ?? [] },
])
const geoCount = computed(() => geo.data?.records.reduce((total, row) => total + row.count, 0) ?? 0)
function share(count: number, total: number | null | undefined) {
  if (total == null || total === 0) return '—'
  return (count / total * 100).toFixed(1) + '%'
}
function refresh() {
  void Promise.all([readSnapshot(), readTotals(), readHeat(), readGeo()])
}

const confirmation = ref(false)
const starting = ref(false)
const startError = ref('')
const receipt = ref(false)
const remaining = ref(0)
let active = true
let cooldownUntil = 0
let timer: ReturnType<typeof setInterval> | undefined

function tick() {
  remaining.value = Math.max(0, Math.ceil((cooldownUntil - Date.now()) / 1000))
  if (!remaining.value) clearInterval(timer)
}
function cooldown() {
  cooldownUntil = Date.now() + 30_000
  tick()
  timer = setInterval(tick, 250)
}
async function start() {
  if (!allowed.value || starting.value || remaining.value) return
  const owner = session.capture()
  starting.value = true
  receipt.value = false
  startError.value = ''
  try {
    await triggerLogAnalysis()
    if (active && session.owns(owner)) {
      confirmation.value = false
      receipt.value = true
      cooldown()
    }
  } catch (cause) {
    if (!active || !session.owns(owner)) return
    confirmation.value = false
    if (permissionDenied(cause)) startError.value = '当前账号缺少手动触发权限。'
    else {
      startError.value = '启动结果未确认。请稍后刷新缓存，避免重复触发。'
      cooldown()
    }
  } finally {
    starting.value = false
  }
}
onMounted(refresh)
onUnmounted(() => {
  active = false
  clearInterval(timer)
})
</script>

<template>
  <section class="flex min-w-0 flex-col gap-5" aria-labelledby="logs-title">
    <header class="flex flex-wrap items-start justify-between gap-3">
      <div class="flex flex-col gap-2">
        <h1 id="logs-title" class="text-3xl font-semibold tracking-tight">扩展日志分析</h1>
        <p class="text-sm text-muted-foreground">受保护 API 访问日志全量快照。</p>
        <p v-if="snapshot.data?.time" class="text-sm tabular-nums text-muted-foreground">
          快照时间 {{ snapshot.data.time }}
          <span v-if="!/Z$|[+-]\d{2}:\d{2}$/.test(snapshot.data.time)">（服务端时间，时区未提供）</span>
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <UiButton variant="outline" :disabled="refreshPending" @click="refresh"><RiRefreshLine data-icon="inline-start" />刷新缓存</UiButton>
        <UiButton :disabled="!allowed || starting || remaining > 0" @click="confirmation = true">
          <RiPlayLine data-icon="inline-start" />
          {{ remaining ? `冷却中 · ${remaining}s` : '手动触发日志分析' }}
        </UiButton>
        <span v-if="!allowed" class="text-xs text-muted-foreground">缺少触发权限</span>
      </div>
    </header>

    <UiAlert v-if="receipt" role="status"><UiAlertTitle>后台分析任务已启动</UiAlertTitle><UiAlertDescription>请稍后刷新缓存查看最新快照。</UiAlertDescription></UiAlert>
    <UiAlert v-if="startError" role="alert" variant="destructive"><UiAlertDescription>{{ startError }}</UiAlertDescription></UiAlert>
    <UiAlert v-if="totals.error" role="alert" variant="destructive">
      <UiAlertTitle>访问概览暂不可用</UiAlertTitle>
      <UiAlertDescription class="flex flex-wrap items-center gap-2">{{ totals.error }}<UiButton variant="outline" size="sm" @click="readTotals">重试读取概览</UiButton></UiAlertDescription>
    </UiAlert>

    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4" aria-label="快照指标">
      <UiCard v-for="metric in metrics" :key="metric.name" class="min-w-0 gap-2 py-4">
        <UiCardHeader><h2 class="text-sm font-medium text-muted-foreground">{{ metric.name }}</h2></UiCardHeader>
        <UiCardContent class="flex flex-col gap-2">
          <p class="break-words text-xl font-semibold tabular-nums">{{ metric.value }}</p><p class="text-xs text-muted-foreground">{{ metric.note }}</p>
        </UiCardContent>
      </UiCard>
    </div>

    <UiCard class="min-w-0">
      <UiCardHeader><h2 class="text-lg font-semibold">访问时间热力图</h2><UiCardDescription>按服务端小时或分钟查看访问集中时段。</UiCardDescription></UiCardHeader>
      <UiCardContent class="flex min-w-0 flex-col gap-4">
        <UiAlert v-if="heat.error" role="alert" variant="destructive"><UiAlertDescription class="flex flex-wrap items-center gap-2">{{ heat.error }}<UiButton variant="outline" size="sm" @click="readHeat">重试读取热力图</UiButton></UiAlertDescription></UiAlert>
        <LogHeatmap v-if="heat.data" :buckets="heat.data" />
        <UiSkeleton v-else-if="heat.loading" class="h-40" role="status" aria-label="正在读取热力图" />
      </UiCardContent>
    </UiCard>

    <UiCard class="min-w-0">
      <UiCardHeader><h2 class="text-lg font-semibold">IP 地理分布</h2></UiCardHeader>
      <UiCardContent class="flex min-w-0 flex-col gap-4">
        <UiAlert v-if="geo.error" role="alert" variant="destructive"><UiAlertDescription class="flex flex-wrap items-center gap-2">{{ geo.error }}<UiButton variant="outline" size="sm" @click="readGeo">重试读取 IP 分布</UiButton></UiAlertDescription></UiAlert>
        <div v-if="geo.data?.records.length" class="grid min-w-0 gap-5 xl:grid-cols-[1fr_1fr_2fr]">
          <section v-for="group in groups" :key="group.name" class="min-w-0">
            <h3 class="mb-3 text-sm font-semibold">{{ group.name }}</h3>
            <ul class="flex flex-col gap-2">
              <li v-for="item in group.items.slice(0, 6)" :key="item.name" class="flex flex-col gap-1 text-sm">
                <span class="flex items-center justify-between gap-2"><span>{{ item.name }}</span><span class="tabular-nums">{{ share(item.count, geoCount) }}</span></span>
                <span class="h-1.5 bg-muted" aria-hidden="true"><span class="block h-full bg-chart-1" :style="{ width: geoCount ? item.count / geoCount * 100 + '%' : '0%' }" /></span>
              </li>
            </ul>
          </section>
          <div class="min-w-0 overflow-x-auto">
            <h3 class="mb-3 text-sm font-semibold">高频访问 IP</h3>
            <UiTable>
              <UiTableCaption>占比以所读快照总访问量为分母。</UiTableCaption>
              <UiTableHeader><UiTableRow><UiTableHead>IP 地址</UiTableHead><UiTableHead>国家 / 城市</UiTableHead><UiTableHead class="text-right">访问次数</UiTableHead><UiTableHead class="text-right">占比</UiTableHead></UiTableRow></UiTableHeader>
              <UiTableBody>
                <UiTableRow v-for="entry in geo.data.records.slice(0, 10)" :key="entry.ip">
                  <UiTableCell class="font-mono text-xs">{{ entry.ip }}</UiTableCell>
                  <UiTableCell>{{ entry.country }} / {{ entry.city }}</UiTableCell>
                  <UiTableCell class="text-right tabular-nums">{{ display(entry.count) }}</UiTableCell>
                  <UiTableCell class="text-right tabular-nums">{{ share(entry.count, totals.data?.visits) }}</UiTableCell>
                </UiTableRow>
              </UiTableBody>
            </UiTable>
          </div>
        </div>
        <p v-else-if="geo.data" role="status" class="text-sm text-muted-foreground">当前分项没有可展示的缓存数据。</p>
        <UiSkeleton v-else-if="geo.loading" class="h-32" role="status" aria-label="正在读取 IP 分布" />
      </UiCardContent>
    </UiCard>

    <UiCard class="min-w-0">
      <UiCardHeader><h2 class="text-lg font-semibold">星期访问规律</h2><UiCardDescription>工作日与周末日均仅按有记录的日期计算。</UiCardDescription></UiCardHeader>
      <UiCardContent class="flex min-w-0 flex-col gap-4">
        <UiAlert v-if="snapshot.error" role="alert" variant="destructive"><UiAlertDescription class="flex flex-wrap items-center gap-2">{{ snapshot.error }}<UiButton variant="outline" size="sm" @click="readSnapshot">重试读取星期规律</UiButton></UiAlertDescription></UiAlert>
        <UiSkeleton v-if="snapshot.loading && !snapshot.data" class="h-36" role="status" aria-label="正在读取星期规律" />
        <template v-else-if="snapshot.data">
          <ul class="flex flex-col gap-2">
            <li v-for="day in snapshot.data.days" :key="day.name" class="grid grid-cols-[2.5rem_minmax(0,1fr)_4.5rem] items-center gap-2 text-sm">
              <span>{{ day.name }}</span><span class="h-2 bg-muted" aria-hidden="true"><span class="block h-full bg-chart-1" :style="{ width: (day.count ?? 0) / weekdayMax * 100 + '%' }" /></span>
              <span class="text-right tabular-nums">{{ display(day.count) }}</span>
            </li>
          </ul>
          <dl class="flex flex-wrap gap-5 border-t pt-4 text-sm">
            <div><dt class="text-muted-foreground">工作日日均</dt><dd class="mt-1 font-semibold tabular-nums">{{ display(averages.workdays) }}</dd></div>
            <div><dt class="text-muted-foreground">周末日均</dt><dd class="mt-1 font-semibold tabular-nums">{{ display(averages.weekends) }}</dd></div>
          </dl>
        </template>
        <p v-else-if="!snapshot.error" role="status" class="text-sm text-muted-foreground">尚未生成快照。生成后可刷新读取。</p>
        <p class="text-xs text-muted-foreground">日志快照不包含响应状态码和耗时。无法定位的 IP 显示为未知。</p>
      </UiCardContent>
    </UiCard>

    <UiDialog :open="confirmation" @update:open="!starting && (confirmation = $event)">
      <UiDialogContent @interact-outside.prevent @escape-key-down="starting && $event.preventDefault()">
        <UiDialogHeader><UiDialogTitle>手动触发日志分析？</UiDialogTitle><UiDialogDescription>这会在后台启动 Spark 日志重算。请稍后刷新缓存查看结果。</UiDialogDescription></UiDialogHeader>
        <UiDialogFooter>
          <UiButton variant="outline" :disabled="starting" @click="confirmation = false">取消</UiButton>
          <UiButton :disabled="starting || !allowed || remaining > 0" @click="start">{{ starting ? '正在触发…' : '确认触发' }}</UiButton>
        </UiDialogFooter>
      </UiDialogContent>
    </UiDialog>
  </section>
</template>
