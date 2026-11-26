<script setup lang="ts">
import { RiArrowLeftSLine, RiArrowRightSLine } from '@remixicon/vue'
import { computed, ref, useId, useTemplateRef, watch } from 'vue'
import { useElementSize, useMediaQuery } from '@vueuse/core'
import { degreeLabel, scatterData } from '@/lib/recruitment'
import type { JobSample } from '@/lib/recruitment'

const props = defineProps<{ jobs: JobSample[]; x: string; y: string }>()
const plot = computed(() => scatterData(props.jobs, props.x, props.y))
const plotFrame = useTemplateRef<HTMLElement>('plotFrame')
const { width } = useElementSize(plotFrame)
const mobile = useMediaQuery('(width < 640px)')
const plotWidth = computed(() => Math.max(220, width.value || 640))
const left = computed(() => props.y === 'salary_value' ? 64 : 80)
const right = computed(() => plotWidth.value - 24)
const chartX = (cx: number) => left.value + ((cx - 60) / 540) * (right.value - left.value)
const chartY = (cy: number) => 18 + ((cy - 30) / 190) * 118
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
  <div v-if="plot.valid" class="space-y-2 px-4 pt-2 pb-3">
    <div class="flex flex-wrap items-center justify-between gap-2">
      <p v-if="active" class="rounded-md border bg-card px-2 py-1 text-xs" aria-live="polite">
        <span class="font-mono tabular-nums">{{ pointDescription(active) }}</span>
      </p>
      <p class="text-xs text-muted-foreground">
        {{ number.format(plot.valid) }} 条有效样本 · 当前绘制 {{ number.format(plot.points.length) }} / {{ number.format(plot.groups) }} 组。
        最多绘制 {{ number.format(plot.groupLimit) }} 组，完整分组见下方明细。
      </p>
    </div>
    <div ref="plotFrame" class="min-w-0">
      <svg
        :viewBox="`0 0 ${plotWidth} 178`"
        class="block h-[178px] w-full text-muted-foreground"
        role="group"
        :aria-label="`${plot.xLabel}与${plot.yLabel}散点图。用方向键查看绘制点，全部坐标见下方分组明细。`"
      >
        <g aria-hidden="true" class="font-mono tabular-nums" font-size="11">
          <line :x1="left" y1="136" :x2="right" y2="136" class="stroke-border" />
          <line :x1="left" y1="18" :x2="left" y2="136" class="stroke-border" />
          <g v-for="tick in plot.xTicks" :key="tick.value">
            <line
              :x1="chartX(tick.x)" y1="18" :x2="chartX(tick.x)" y2="136"
              class="stroke-border/60" stroke-dasharray="3 4"
            />
            <text :x="chartX(tick.x)" y="152" text-anchor="middle" fill="currentColor">
              <template v-if="x === 'degree_value' && tick.value === 2">
                <tspan :x="chartX(tick.x)">高中</tspan><tspan :x="chartX(tick.x)" dy="13">中专</tspan>
              </template>
              <template v-else>{{ tickLabel(x, tick.value) }}</template>
            </text>
          </g>
          <g v-for="tick in plot.yTicks" :key="tick.value">
            <line
              :x1="left" :y1="chartY(tick.y)" :x2="right" :y2="chartY(tick.y)"
              class="stroke-border/60" stroke-dasharray="3 4"
            />
            <text :x="left - 10" :y="chartY(tick.y)" dy=".35em" text-anchor="end" fill="currentColor">
              {{ yTickLabel(tick.value) }}
            </text>
          </g>
          <text :x="(left + right) / 2" y="176" text-anchor="middle" fill="currentColor" font-size="12">
            {{ plot.xLabel }}
          </text>
          <text :x="left" y="11" fill="currentColor" font-size="12">{{ plot.yLabel }}</text>
        </g>
        <circle
          v-for="(point, index) in plot.points"
          :key="`${point.x}:${point.y}`"
          :data-point="index"
          :cx="chartX(point.cx)"
          :cy="chartY(point.cy)"
          :r="bubbleRadius(point.count)"
          :tabindex="tabPoint === index ? 0 : -1"
          :aria-label="pointDescription(point)"
          :aria-current="currentPoint === index ? 'true' : undefined"
          role="button"
          class="fill-chart-4/80 dark:fill-chart-2/85 stroke-card outline-none hover:fill-chart-5 aria-[current=true]:fill-chart-5 dark:hover:fill-chart-1 dark:aria-[current=true]:fill-chart-1 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
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
          :cy="chartY(active.cy)"
          :r="bubbleRadius(active.count)"
          class="pointer-events-none fill-none stroke-foreground"
          stroke-width="2"
          aria-hidden="true"
        />
      </svg>
    </div>
    <div class="flex flex-wrap items-center justify-between gap-2 text-xs text-muted-foreground">
      <p>最多等间隔绘制 1,000 组坐标。相同坐标合并，圆点越大代表样本越多。</p>
      <ul aria-label="气泡样本量" class="flex items-center gap-3 font-mono tabular-nums">
        <li v-for="count in legend" :key="count" class="flex items-center gap-1">
          <svg viewBox="0 0 16 16" class="size-4" aria-hidden="true">
            <circle cx="8" cy="8" :r="bubbleRadius(count)" class="fill-chart-4/80 dark:fill-chart-2/85" />
          </svg>
          {{ count === 32 ? '32+' : count }}
        </li>
      </ul>
    </div>
    <details :open="!mobile" class="border-t pt-2">
      <summary class="cursor-pointer rounded-sm text-xs font-semibold focus-visible:outline-2 focus-visible:outline-ring">
        全量坐标分组明细
      </summary>
      <div
        :key="page" role="region" aria-label="当前页坐标分组" tabindex="0"
        class="mt-2 max-h-20 overflow-auto [&_[data-slot=table-container]]:overflow-visible"
      >
        <UiTable class="text-xs">
          <UiTableCaption class="sr-only">全部坐标分组及样本数，每页 {{ pageSize }} 组</UiTableCaption>
          <UiTableHeader class="sticky top-0 bg-muted">
            <UiTableRow>
              <UiTableHead class="h-6">{{ plot.xLabel }}</UiTableHead>
              <UiTableHead class="h-6">{{ plot.yLabel }}</UiTableHead>
              <UiTableHead class="h-6 text-right">样本数</UiTableHead>
            </UiTableRow>
          </UiTableHeader>
          <UiTableBody>
            <UiTableRow v-for="point in visibleGroups" :key="`${point.x}:${point.y}`">
              <UiTableCell class="py-0.5 font-mono tabular-nums">{{ coordinate(x, point.x) }}</UiTableCell>
              <UiTableCell class="py-0.5 font-mono tabular-nums">{{ coordinate(y, point.y) }}</UiTableCell>
              <UiTableCell class="py-0.5 text-right font-mono tabular-nums">{{ number.format(point.count) }}</UiTableCell>
            </UiTableRow>
          </UiTableBody>
        </UiTable>
      </div>
      <div v-if="pageCount > 1" class="mt-2 flex flex-wrap items-center justify-between gap-2">
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
            <UiPaginationPrevious size="icon-sm" aria-label="上一页"><RiArrowLeftSLine aria-hidden="true" /></UiPaginationPrevious>
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
            <UiPaginationNext size="icon-sm" aria-label="下一页"><RiArrowRightSLine aria-hidden="true" /></UiPaginationNext>
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
