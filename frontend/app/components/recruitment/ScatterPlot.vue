<script setup lang="ts">
import { useElementSize } from '@vueuse/core'
import { degreeLabel, scatterData } from '@/lib/recruitment'
import type { JobSample } from '@/lib/recruitment'

const props = defineProps<{ jobs: JobSample[]; x: string; y: string }>()
const plot = computed(() => scatterData(props.jobs, props.x, props.y))
const plotFrame = useTemplateRef<HTMLElement>('plotFrame')
const { width } = useElementSize(plotFrame)
const plotWidth = computed(() => Math.max(220, width.value || 640))
const left = computed(() => props.y === 'salary_value' ? 64 : 80)
const right = computed(() => plotWidth.value - 24)
const chartX = (cx: number) => left.value + ((cx - 60) / 540) * (right.value - left.value)
const bubbleRadius = (count: number) => Math.min(8, 3 + Math.log2(count))
const legend = [1, 10, 32]
const page = ref(1)
const jumpPage = ref<string | number>(1)
const jumpPageId = useId()
const currentPoint = ref(0)
const tabPoint = ref(0)
const pageSize = 20
const pageCount = computed(() => Math.max(1, Math.ceil(plot.value.groups / pageSize)))
const visibleGroups = computed(() =>
  plot.value.allGroups.slice((page.value - 1) * pageSize, page.value * pageSize),
)
const active = computed(() => plot.value.points[currentPoint.value])
const number = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 })
const experienceLabels: Record<number, string> = {
  0: '应届 / 不限',
  1: '1 年以内',
  2: '1–3 年',
  4: '3–5 年',
  7: '5–10 年',
  10: '10 年以上',
}

watch(() => [props.jobs, props.x, props.y], () => {
  page.value = 1
  jumpPage.value = 1
  currentPoint.value = 0
  tabPoint.value = 0
})

watch(page, (value) => { jumpPage.value = value })

function goToPage() {
  const target = Number(jumpPage.value)
  if (Number.isInteger(target) && target >= 1 && target <= pageCount.value) page.value = target
}

function coordinate(axis: string, value: number) {
  const formatted = number.format(value)
  if (axis === 'salary_value') return `${formatted} K`
  if (axis === 'degree_value' && degreeLabel(value)) {
    return `${degreeLabel(value)}（${formatted}）`
  }
  return formatted
}

function pointDescription(point: { x: number; y: number; count: number }) {
  return `${plot.value.xLabel} ${coordinate(props.x, point.x)}，${plot.value.yLabel} ${coordinate(props.y, point.y)}，${number.format(point.count)} 条样本`
}

function tickLabel(axis: string, value: number) {
  return (axis === 'degree_value' && degreeLabel(value)) || number.format(value)
}

function yTickLabel(value: number) {
  return (props.y === 'experience_value' && experienceLabels[value]) || tickLabel(props.y, value)
}

function focusPoint(index: number) {
  tabPoint.value = index
  currentPoint.value = index
}

function pointKey(event: KeyboardEvent, index: number) {
  const last = plot.value.points.length - 1
  const targets: Record<string, number> = {
    ArrowLeft: index - 1,
    ArrowUp: index - 1,
    ArrowRight: index + 1,
    ArrowDown: index + 1,
    Home: 0,
    End: last,
    Enter: index,
    ' ': index,
  }
  if (!Object.hasOwn(targets, event.key)) return
  event.preventDefault()
  const next = Math.max(0, Math.min(last, targets[event.key]!))
  focusPoint(next)
  plotFrame.value?.querySelector<SVGCircleElement>(`[data-point="${next}"]`)?.focus()
}
</script>

<template>
  <div v-if="plot.valid" class="space-y-4">
    <div class="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-2">
      <div class="space-y-1">
        <h3 class="text-sm font-semibold">职位样本分布</h3>
        <p class="text-xs text-muted-foreground">
          <span class="font-mono tabular-nums">{{ number.format(plot.valid) }}</span> 条有效样本 ·
          <span class="font-mono tabular-nums">{{ number.format(plot.groups) }}</span> 组坐标
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
        <span>气泡样本量</span>
        <ul class="flex items-center gap-2 font-mono tabular-nums">
          <li v-for="count in legend" :key="count" class="flex items-center gap-1">
            <svg viewBox="0 0 16 16" class="size-4" aria-hidden="true">
              <circle cx="8" cy="8" :r="bubbleRadius(count)" class="fill-chart-1/65" />
            </svg>
            {{ count === 32 ? '32+' : count }}
          </li>
        </ul>
      </div>
    </div>
    <div
      v-if="active"
      class="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-2 rounded-md bg-accent-data-surface px-3 py-2 text-sm text-accent-data-foreground"
      aria-live="polite"
    >
      <span>
        {{ plot.xLabel }} <strong class="font-mono tabular-nums">{{ coordinate(x, active.x) }}</strong>
      </span>
      <span>
        {{ plot.yLabel }} <strong class="font-mono tabular-nums">{{ coordinate(y, active.y) }}</strong>
      </span>
      <span><strong class="font-mono tabular-nums">{{ number.format(active.count) }}</strong> 条样本</span>
    </div>
    <div ref="plotFrame" class="min-w-0">
      <svg
        :viewBox="`0 0 ${plotWidth} 280`"
        class="block h-[280px] w-full text-muted-foreground"
        role="group"
        :aria-label="`${plot.xLabel}与${plot.yLabel}散点图。用方向键查看绘制点，全部坐标见下方分组明细。`"
      >
        <g aria-hidden="true" class="font-mono tabular-nums" font-size="11">
          <line :x1="left" y1="220" :x2="right" y2="220" class="stroke-chart-5" />
          <line :x1="left" y1="30" :x2="left" y2="220" class="stroke-chart-5" />
          <g v-for="tick in plot.xTicks" :key="tick.value">
            <line
              :x1="chartX(tick.x)" y1="30" :x2="chartX(tick.x)" y2="220"
              class="stroke-border/60" stroke-dasharray="3 4"
            />
            <text :x="chartX(tick.x)" y="240" text-anchor="middle" fill="currentColor">
              <template v-if="x === 'degree_value' && tick.value === 2">
                <tspan :x="chartX(tick.x)">高中</tspan><tspan :x="chartX(tick.x)" dy="13">中专</tspan>
              </template>
              <template v-else>{{ tickLabel(x, tick.value) }}</template>
            </text>
          </g>
          <g v-for="tick in plot.yTicks" :key="tick.value">
            <line
              :x1="left" :y1="tick.y" :x2="right" :y2="tick.y"
              class="stroke-border/60" stroke-dasharray="3 4"
            />
            <text :x="left - 10" :y="tick.y" dy=".35em" text-anchor="end" fill="currentColor">
              {{ yTickLabel(tick.value) }}
            </text>
          </g>
          <text :x="(left + right) / 2" y="273" text-anchor="middle" fill="currentColor" font-size="12">
            {{ plot.xLabel }}
          </text>
          <text :x="left" y="16" fill="currentColor" font-size="12">{{ plot.yLabel }}</text>
        </g>
        <circle
          v-for="(point, index) in plot.points"
          :key="`${point.x}:${point.y}`"
          :data-point="index"
          :cx="chartX(point.cx)"
          :cy="point.cy"
          :r="bubbleRadius(point.count)"
          :tabindex="tabPoint === index ? 0 : -1"
          :aria-label="pointDescription(point)"
          :aria-current="currentPoint === index ? 'true' : undefined"
          role="button"
          class="fill-chart-1/65 stroke-card outline-none hover:fill-chart-1 aria-[current=true]:fill-chart-1 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
          @mouseenter="currentPoint = index"
          @focus="focusPoint(index)"
          @click="focusPoint(index)"
          @keydown="pointKey($event, index)"
        >
          <title>{{ pointDescription(point) }}</title>
        </circle>
        <circle
          v-if="active"
          :cx="chartX(active.cx)"
          :cy="active.cy"
          :r="bubbleRadius(active.count)"
          class="pointer-events-none fill-none stroke-accent-data-foreground"
          stroke-width="2"
          aria-hidden="true"
        />
      </svg>
    </div>
    <p class="text-xs leading-5 text-muted-foreground">
      最多等间隔绘制 1,000 组坐标；下方明细表保留全部分组。学历、经验的具体要求见职位详情。
    </p>
    <details class="rounded-md border p-3">
      <summary class="cursor-pointer rounded-sm text-sm focus-visible:outline-2 focus-visible:outline-ring">
        分组明细（全部 {{ number.format(plot.groups) }} 组坐标）
      </summary>
      <UiTable class="mt-3">
        <UiTableCaption class="sr-only">全部坐标分组及样本数，每页 {{ pageSize }} 组</UiTableCaption>
        <UiTableHeader>
          <UiTableRow>
            <UiTableHead>{{ plot.xLabel }}</UiTableHead>
            <UiTableHead>{{ plot.yLabel }}</UiTableHead>
            <UiTableHead class="text-right">样本数</UiTableHead>
          </UiTableRow>
        </UiTableHeader>
        <UiTableBody>
          <UiTableRow v-for="point in visibleGroups" :key="`${point.x}:${point.y}`">
            <UiTableCell class="font-mono tabular-nums">{{ coordinate(x, point.x) }}</UiTableCell>
            <UiTableCell class="font-mono tabular-nums">{{ coordinate(y, point.y) }}</UiTableCell>
            <UiTableCell class="text-right font-mono tabular-nums">{{ number.format(point.count) }}</UiTableCell>
          </UiTableRow>
        </UiTableBody>
      </UiTable>
      <div v-if="pageCount > 1" class="mt-3 flex flex-wrap items-center justify-between gap-3">
        <span class="font-mono text-xs tabular-nums text-muted-foreground" aria-live="polite">
          第 {{ page }} / {{ pageCount }} 页
        </span>
        <UiPagination
          v-model:page="page"
          :total="plot.groups"
          :items-per-page="pageSize"
          :sibling-count="1"
          show-edges
          aria-label="坐标分组分页"
          class="mx-0 w-auto max-w-full"
        >
          <UiPaginationContent v-slot="{ items }" class="flex-wrap">
            <UiPaginationPrevious aria-label="上一页" />
            <template v-for="(item, index) in items" :key="index">
              <UiPaginationItem
                v-if="item.type === 'page'"
                :value="item.value"
                :is-active="item.value === page"
                :aria-label="`第 ${item.value} 页`"
                :aria-current="item.value === page ? 'page' : undefined"
                size="icon-sm"
              >{{ item.value }}</UiPaginationItem>
              <UiPaginationEllipsis v-else />
            </template>
            <UiPaginationNext aria-label="下一页" />
          </UiPaginationContent>
        </UiPagination>
        <form class="flex items-center gap-2" @submit.prevent="goToPage">
          <label :for="jumpPageId" class="text-xs text-muted-foreground">跳至页码</label>
          <UiInput
            :id="jumpPageId"
            v-model="jumpPage"
            type="number"
            min="1"
            :max="pageCount"
            step="1"
            required
            class="h-8 w-20 font-mono tabular-nums"
          />
          <UiButton type="submit" size="sm" variant="outline">前往</UiButton>
        </form>
      </div>
    </details>
  </div>
  <p v-else class="py-12 text-center text-sm text-muted-foreground">
    当前样本没有可绘制的坐标，原始信息可在列表中查看。
  </p>
</template>
