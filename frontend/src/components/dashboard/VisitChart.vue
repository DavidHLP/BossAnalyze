<script setup lang="ts">
import { computed, ref, useTemplateRef, watch } from 'vue'
import { formatPercentage, peakVisits, visitShare } from '@/lib/dashboard'
import type { CountRow } from '@/lib/dashboard'

const props = defineProps<{ hours: CountRow[] }>()
const rows = computed(() => props.hours.slice(-24))
const days = computed(() => {
  const groups = new Map<string, CountRow[]>()
  for (const row of rows.value) {
    const day = row.label.slice(0, 10)
    const hours = groups.get(day)
    if (hours) hours.push(row)
    else groups.set(day, [row])
  }
  return [...groups]
})
const peak = computed(() => peakVisits(rows.value))
const yTicks = computed(() => {
  const max = peak.value
  if (!max) return [0]
  const intervals = Math.min(3, max)
  return Array.from({ length: intervals + 1 }, (_, index) =>
    max * (intervals - index) / intervals,
  )
})
const detailColumns = computed(() => {
  if (rows.value.length < 2) return [rows.value]
  const midpoint = Math.ceil(rows.value.length / 2)
  return [rows.value.slice(0, midpoint), rows.value.slice(midpoint)]
})
const xTicks = computed(() => {
  const last = rows.value.length - 1
  const intervals = Math.max(1, Math.min(6, last))
  return Array.from({ length: Math.min(7, rows.value.length) }, (_, tick) =>
    Math.round(tick * last / intervals),
  )
})
const selected = ref<string | null>(null)
watch(() => props.hours, () => { selected.value = null })
const active = computed(
  () => rows.value.find((row) => row.label === selected.value) ?? rows.value.at(-1),
)
const number = new Intl.NumberFormat('zh-CN')
const bars = useTemplateRef<HTMLDivElement>('bars')

function moveSelection(event: KeyboardEvent, index: number) {
  const length = rows.value.length
  let nextIndex: number
  switch (event.key) {
    case 'ArrowLeft':
    case 'ArrowUp':
      nextIndex = (index + length - 1) % length
      break
    case 'ArrowRight':
    case 'ArrowDown':
      nextIndex = (index + 1) % length
      break
    case 'Home':
      nextIndex = 0
      break
    case 'End':
      nextIndex = length - 1
      break
    default:
      return
  }
  event.preventDefault()
  selected.value = rows.value[nextIndex]?.label ?? null
  bars.value?.querySelectorAll('button')[nextIndex]?.focus()
}
</script>

<template>
  <UiCard class="min-w-0 gap-3 py-4">
    <UiCardHeader class="flex flex-wrap items-start justify-between gap-3">
      <div class="space-y-1">
        <h2 class="text-lg font-semibold">访问趋势</h2>
        <UiCardDescription>最近 24 个有记录的小时桶 · 缺小时不补零</UiCardDescription>
      </div>
      <div
        v-if="active"
        class="flex flex-wrap items-baseline gap-x-2 gap-y-1 rounded-md bg-accent-data-surface px-3 py-2 text-xs text-accent-data-foreground"
        aria-live="polite"
      >
        <span>已选中</span>
        <span class="font-mono tabular-nums">{{ active.label }} 时</span>
        <span><strong class="font-mono tabular-nums">{{ number.format(active.count) }}</strong> 次访问</span>
      </div>
    </UiCardHeader>
    <UiCardContent>
      <template v-if="active">
        <label class="mb-4 flex flex-col gap-1 text-sm sm:hidden">
          选择趋势时段
          <select
            :value="active.label"
            class="min-h-9 w-full rounded-md border bg-background px-2"
            @change="selected = ($event.target as HTMLSelectElement).value"
          >
            <optgroup v-for="[day, hours] in days" :key="day" :label="day">
              <option v-for="row in hours" :key="row.label" :value="row.label">
                {{ row.label.slice(11) }}:00 · {{ number.format(row.count) }} 次访问
              </option>
            </optgroup>
          </select>
        </label>
        <div class="grid grid-cols-[auto_minmax(0,1fr)] gap-x-3 gap-y-3">
          <div
            class="flex flex-col text-right font-mono text-[0.6875rem] tabular-nums text-muted-foreground"
            :class="peak ? 'justify-between' : 'justify-end'"
            aria-hidden="true"
          >
            <span v-for="tick in yTicks" :key="tick" class="h-0 -translate-y-2">
              {{ number.format(Math.round(tick)) }}
            </span>
          </div>
          <div class="relative min-w-0">
            <div
              class="pointer-events-none absolute inset-0 flex flex-col"
              :class="peak ? 'justify-between' : 'justify-end'"
              aria-hidden="true"
            >
              <span v-for="tick in yTicks" :key="tick" class="border-t border-border" />
            </div>
            <div
              ref="bars"
              class="relative flex h-28 items-end gap-0.5 sm:gap-1 lg:gap-2"
              role="radiogroup"
              aria-label="每小时访问量"
            >
              <button
                v-for="(row, index) in rows"
                :key="row.label"
                type="button"
                role="radio"
                class="group relative flex h-full min-w-0 flex-1 items-end justify-center rounded-sm focus-visible:z-10 focus-visible:outline-2 focus-visible:outline-ring"
                :aria-label="`${row.label}:00，${number.format(row.count)} 次访问`"
                :aria-checked="active.label === row.label"
                :tabindex="active.label === row.label ? 0 : -1"
                @mouseenter="selected = row.label"
                @focus="selected = row.label"
                @click="selected = row.label"
                @keydown="moveSelection($event, index)"
              >
                <span
                  aria-hidden="true"
                  class="w-full max-w-7 rounded-t-sm"
                  :class="[
                    active.label === row.label ? 'bg-accent-data' : 'bg-accent-data/75',
                    { 'min-h-[3px]': row.count > 0 },
                  ]"
                  :style="{ height: `${visitShare(row.count, peak) ?? 0}%` }"
                />
                <span v-if="active.label === row.label" aria-hidden="true" class="absolute -bottom-1 h-0.5 w-2/3 max-w-7 rounded-full bg-accent-data" />
              </button>
            </div>
          </div>
          <div
            class="col-start-2 grid font-mono text-[0.6875rem] leading-tight tabular-nums text-muted-foreground"
            :style="{ gridTemplateColumns: `repeat(${rows.length}, minmax(0, 1fr))` }"
            aria-hidden="true"
          >
            <span
              v-for="(index, position) in xTicks"
              :key="index"
              class="w-max text-center"
              :class="[
                index === 0 ? 'justify-self-start' : index === rows.length - 1 ? 'justify-self-end' : 'justify-self-center',
                { 'hidden sm:block': position !== 0 && position !== xTicks.length - 1 && position !== Math.floor(xTicks.length / 2) },
                { 'font-semibold text-accent-data-foreground': active.label === rows[index]?.label },
              ]"
              :style="{ gridColumnStart: index + 1, gridRowStart: 1 }"
            >
              {{ rows[index]?.label.slice(5, 10) }} · {{ rows[index]?.label.slice(11) }}
            </span>
          </div>
        </div>

        <details open class="mt-3 border-t pt-2 text-sm">
          <summary class="w-fit cursor-pointer rounded-sm font-semibold focus-visible:outline-2 focus-visible:outline-ring">
            小时明细
            <span class="ml-2 text-xs font-normal text-muted-foreground">
              悬停、聚焦或点击柱条可查看读数。
            </span>
          </summary>
          <div class="mt-2 grid min-w-0 gap-2 xl:grid-cols-2 xl:gap-4">
            <UiTable v-for="(column, index) in detailColumns" :key="index">
              <UiTableCaption class="sr-only">小时明细 {{ index + 1 }}</UiTableCaption>
              <UiTableHeader>
                <UiTableRow class="bg-muted/50">
                  <UiTableHead class="h-auto py-1 text-xs">小时</UiTableHead>
                  <UiTableHead class="h-auto py-1 text-right text-xs">访问次数</UiTableHead>
                  <UiTableHead class="h-auto py-1 text-right text-xs">占峰值比</UiTableHead>
                </UiTableRow>
              </UiTableHeader>
              <UiTableBody>
                <UiTableRow
                  v-for="row in column"
                  :key="row.label"
                  :class="{ 'bg-accent-data-surface font-semibold text-accent-data-foreground': active.label === row.label }"
                >
                  <UiTableCell class="py-0.5 font-mono text-xs tabular-nums">{{ row.label }}:00</UiTableCell>
                  <UiTableCell class="py-0.5 text-right font-mono text-xs tabular-nums">
                    {{ number.format(row.count) }}
                  </UiTableCell>
                  <UiTableCell class="py-0.5 text-right font-mono text-xs tabular-nums">
                    {{ formatPercentage(visitShare(row.count, peak)) }}
                  </UiTableCell>
                </UiTableRow>
              </UiTableBody>
            </UiTable>
          </div>
        </details>
      </template>
      <p v-else class="rounded-md border border-dashed py-12 text-center text-sm text-muted-foreground">
        暂无小时访问记录
      </p>
    </UiCardContent>
  </UiCard>
</template>
