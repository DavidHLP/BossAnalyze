<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { heatLevel } from '@/lib/log-analysis'

const props = defineProps<{
  buckets: { hours: { label: string; count: number }[]; minutes: { label: string; count: number }[] }
}>()
const mode = ref<'hours' | 'minutes'>('hours')
const minuteHour = ref('')
const selected = ref('')
const cells = computed(() => new Map(props.buckets[mode.value].map(({ label, count }) => [label, count])))
const dates = computed(() => [...new Set(props.buckets.hours.map(({ label }) => label.slice(0, 10)))].slice(-7))
const minuteHours = computed(() => [...new Set(props.buckets.minutes.map(({ label }) => label.slice(0, 13)))].reverse())
const rows = computed(() => mode.value === 'hours' ? dates.value : minuteHour.value ? [minuteHour.value] : [])
const axis = computed(() => Array.from({ length: mode.value === 'hours' ? 24 : 60 }, (_, index) => String(index).padStart(2, '0')))
const tones = ['bg-muted/55', 'bg-background', 'bg-chart-1/15', 'bg-chart-1/30', 'bg-chart-1/50', 'bg-chart-1/70', 'bg-chart-1']
const labels = ['无记录', '0', '1–99', '100–399', '400–799', '800–1,499', '≥1,500']
const count = new Intl.NumberFormat('zh-CN')
const key = (row: string, value: string) => row + (mode.value === 'hours' ? ' ' : ':') + value
const description = (value: string) => {
  const visits = cells.value.get(value)
  return value + ' · ' + (visits === undefined ? '无记录' : count.format(visits) + ' 次访问')
}
watch(minuteHours, (items) => {
  if (!items.includes(minuteHour.value)) minuteHour.value = items[0] ?? ''
}, { immediate: true })
watch([rows, mode, cells], () => {
  const row = rows.value.at(-1)
  selected.value = row ? axis.value.map(value => key(row, value)).find(value => cells.value.has(value)) ?? key(row, axis.value[0]!) : ''
}, { immediate: true })

function move(event: KeyboardEvent, rowIndex: number, columnIndex: number) {
  const offset = { ArrowUp: [-1, 0], ArrowDown: [1, 0], ArrowLeft: [0, -1], ArrowRight: [0, 1] }[event.key]
  if (!offset) return
  event.preventDefault()
  const row = rows.value[rowIndex + offset[0]!]
  const column = axis.value[columnIndex + offset[1]!]
  if (!row || !column) return
  const table = (event.currentTarget as HTMLElement).closest('table')
  table?.querySelector<HTMLButtonElement>('[data-row="' + (rowIndex + offset[0]!) + '"][data-col="' + (columnIndex + offset[1]!) + '"]')?.focus()
}
</script>

<template>
  <div class="flex min-w-0 flex-col gap-4">
    <div class="flex flex-wrap items-center gap-2">
      <div class="flex gap-2" role="group" aria-label="时间粒度">
        <UiButton size="sm" :variant="mode === 'hours' ? 'secondary' : 'outline'" :aria-pressed="mode === 'hours'" @click="mode = 'hours'">小时热力</UiButton>
        <UiButton size="sm" :variant="mode === 'minutes' ? 'secondary' : 'outline'" :aria-pressed="mode === 'minutes'" @click="mode = 'minutes'">分钟热力</UiButton>
      </div>
      <select v-if="mode === 'minutes' && minuteHours.length" v-model="minuteHour" aria-label="选择分钟热力的小时" class="min-h-9 max-w-full rounded-md border bg-background px-2 text-sm">
        <option v-for="hour in minuteHours" :key="hour" :value="hour">{{ hour }} 时</option>
      </select>
      <span v-if="mode === 'hours'" class="text-xs text-muted-foreground">最近 7 个有记录日期</span>
    </div>
    <p v-if="!rows.length" role="status" class="text-sm text-muted-foreground">当前粒度没有可展示的缓存数据。</p>
    <template v-else>
      <label class="flex flex-col gap-1 text-sm sm:hidden">
        选择热力图时段
        <select v-model="selected" class="min-h-9 w-full rounded-md border bg-background px-2">
          <optgroup v-for="row in rows" :key="row" :label="row">
            <option v-for="column in axis" :key="column" :value="key(row, column)">
              {{ description(key(row, column)) }}
            </option>
          </optgroup>
        </select>
      </label>
      <p id="heatmap-scroll-hint" class="text-xs text-muted-foreground sm:hidden">左右滑动查看更多时间</p>
      <div tabindex="0" role="region" aria-label="可横向滚动的访问热力表" aria-describedby="heatmap-scroll-hint" class="max-w-full overflow-auto scroll-ps-30">
        <table class="w-max border-separate border-spacing-1 text-xs tabular-nums">
          <caption class="sr-only">访问时间热力图，使用方向键浏览时间桶</caption>
          <thead><tr>
            <th scope="col" class="sticky left-0 z-10 bg-card px-1 text-left font-medium">{{ mode === 'hours' ? '日期' : '时段' }}</th>
            <th v-for="column in axis" :key="column" scope="col" class="font-normal text-muted-foreground">{{ column }}</th>
          </tr></thead>
          <tbody><tr v-for="(row, ri) in rows" :key="row">
            <th scope="row" class="sticky left-0 z-10 bg-card whitespace-nowrap px-1 text-left font-normal">{{ row }}</th>
            <td v-for="(column, ci) in axis" :key="column">
              <button type="button" :data-row="ri" :data-col="ci" :tabindex="selected === key(row, column) ? 0 : -1"
                :title="description(key(row, column))" :aria-label="description(key(row, column))"
                class="block size-6 border focus-visible:outline-2 focus-visible:outline-offset-1"
                :class="tones[heatLevel(cells.get(key(row, column)))]"
                @focus="selected = key(row, column)" @click="selected = key(row, column)" @keydown="move($event, ri, ci)" />
            </td>
          </tr></tbody>
        </table>
      </div>
      <div class="flex flex-wrap gap-x-3 gap-y-2 text-xs text-muted-foreground" aria-label="访问次数色阶">
        <span v-for="(label, index) in labels" :key="label" class="inline-flex items-center gap-1"><span class="size-3 border" :class="tones[index]" aria-hidden="true" />{{ label }}</span>
      </div>
      <p v-if="selected" role="status" class="text-sm tabular-nums">当前焦点 · {{ description(selected) }}</p>
    </template>
  </div>
</template>
