<script setup lang="ts">
import { scatterData } from '@/lib/recruitment'
import type { JobSample } from '@/lib/recruitment'
const props = defineProps<{ jobs: JobSample[]; x: string; y: string }>()
const plot = computed(() => scatterData(props.jobs, props.x, props.y))
const number = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 })
</script>
<template>
  <div v-if="plot.valid" class="space-y-3">
    <svg
      viewBox="0 0 640 280"
      class="h-auto w-full text-muted-foreground"
      role="img"
      :aria-label="`${plot.xLabel}与${plot.yLabel}散点图，${plot.valid}条有效样本，${plot.groups}组坐标；明细见下方职位表格`"
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
        fill-opacity="0.5"
        stroke="var(--primary)"
        stroke-width="0.5"
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
      组坐标，相同坐标的样本数可通过悬停查看。学历、经验数值用于比较，具体要求以职位详情为准。
    </p>
    <p v-if="plot.groups > plot.points.length" class="text-xs text-muted-foreground">
      图中等间隔展示 {{ plot.points.length }} 组坐标；统计和列表仍包含全部返回样本。
    </p>
  </div>
  <p v-else class="py-12 text-center text-sm text-muted-foreground">
    当前样本没有可绘制的坐标，原始信息可在列表中查看。
  </p>
</template>
