<script setup lang="ts">
import { scatterData } from '@/lib/recruitment'
import type { JobSample } from '@/lib/recruitment'
const props = defineProps<{ jobs: JobSample[]; x: string; y: string }>()
const plot = computed(() => scatterData(props.jobs, props.x, props.y))
const page = ref(1)
const pageSize = 20
const pageCount = computed(() => Math.max(1, Math.ceil(plot.value.groups / pageSize)))
const visibleGroups = computed(() =>
  plot.value.allGroups.slice((page.value - 1) * pageSize, page.value * pageSize),
)
watch(() => [props.jobs, props.x, props.y], () => {
  page.value = 1
})
const number = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 })
</script>
<template>
  <div v-if="plot.valid" class="space-y-3">
    <svg
      viewBox="0 0 640 280"
      class="h-auto w-full text-muted-foreground"
      role="img"
      :aria-label="`${plot.xLabel}与${plot.yLabel}散点图，${plot.valid}条有效样本，${plot.groups}组坐标；全部分组及数量见下方分组明细`"
    >
      <line x1="60" y1="220" x2="600" y2="220" stroke="var(--border)" />
      <line x1="60" y1="30" x2="60" y2="220" stroke="var(--border)" />
      <line x1="60" y1="125" x2="600" y2="125" stroke="var(--border)" stroke-dasharray="4 4" />
      <circle
        v-for="(point, index) in plot.points"
        :key="index"
        :cx="point.cx"
        :cy="point.cy"
        :r="Math.min(8, 3 + Math.log2(point.count))"
        fill="var(--primary)"
        fill-opacity="0.8"
        stroke="var(--primary)"
        stroke-width="1.5"
      >
        <title>
          {{ plot.xLabel }} {{ point.x }} / {{ plot.yLabel }} {{ point.y }}：{{
            point.count
          }}
          条样本
        </title>
      </circle>
      <text x="60" y="240" text-anchor="middle" fill="currentColor" font-size="11">
        {{ number.format(plot.minX) }}
      </text>
      <text x="600" y="240" text-anchor="middle" fill="currentColor" font-size="11">
        {{ number.format(plot.maxX) }}
      </text>
      <text x="48" y="220" text-anchor="end" fill="currentColor" font-size="11">
        {{ number.format(plot.minY) }}
      </text>
      <text x="48" y="34" text-anchor="end" fill="currentColor" font-size="11">
        {{ number.format(plot.maxY) }}
      </text>
      <text x="330" y="268" text-anchor="middle" fill="currentColor" font-size="12">
        {{ plot.xLabel }}
      </text>
      <text x="60" y="16" fill="currentColor" font-size="12">{{ plot.yLabel }}</text>
    </svg>
    <p class="text-xs leading-5 text-muted-foreground">
      {{ plot.valid }} 条有效样本合并为
      {{
        plot.groups
      }}
      组坐标，样本数可通过悬停或下方分组明细查看。学历、经验数值用于比较，具体要求以职位详情为准。
    </p>
    <p v-if="plot.groups > plot.points.length" class="text-xs text-muted-foreground">
      图中等间隔展示 {{ plot.points.length }} 组坐标；分组明细仍包含全部坐标和有效样本。
    </p>
    <details class="rounded-md border p-3">
      <summary class="cursor-pointer rounded-sm text-sm focus-visible:outline-2 focus-visible:outline-ring">
        分组明细（全部 {{ plot.groups }} 组坐标，{{ plot.valid }} 条有效样本）
      </summary>
      <UiTable class="mt-3">
        <UiTableCaption class="sr-only">全部坐标分组及样本数，每页 {{ pageSize }} 组，包含图中未绘制的分组</UiTableCaption>
        <UiTableHeader>
          <UiTableRow>
            <UiTableHead>{{ plot.xLabel }}</UiTableHead>
            <UiTableHead>{{ plot.yLabel }}</UiTableHead>
            <UiTableHead class="text-right">样本数</UiTableHead>
          </UiTableRow>
        </UiTableHeader>
        <UiTableBody>
          <UiTableRow v-for="point in visibleGroups" :key="`${point.x}:${point.y}`">
            <UiTableCell>{{ number.format(point.x) }}</UiTableCell>
            <UiTableCell>{{ number.format(point.y) }}</UiTableCell>
            <UiTableCell class="text-right tabular-nums">{{ point.count }}</UiTableCell>
          </UiTableRow>
        </UiTableBody>
      </UiTable>
      <nav v-if="pageCount > 1" aria-label="坐标分组分页" class="mt-3 flex flex-wrap items-center justify-end gap-2">
        <span class="text-xs text-muted-foreground" aria-live="polite">第 {{ page }} / {{ pageCount }} 页</span>
        <UiButton variant="outline" size="sm" :disabled="page === 1" @click="page--">上一页</UiButton>
        <UiButton variant="outline" size="sm" :disabled="page === pageCount" @click="page++">下一页</UiButton>
      </nav>
    </details>
  </div>
  <p v-else class="py-12 text-center text-sm text-muted-foreground">
    当前样本没有可绘制的坐标，原始信息可在列表中查看。
  </p>
</template>
