<script setup lang="ts">
import { peakVisits } from '@/lib/dashboard'
import type { CountRow } from '@/lib/dashboard'

const props = defineProps<{ hours: CountRow[] }>()
const rows = computed(() => props.hours.slice(-24))
const maximum = computed(() => Math.max(1, peakVisits(rows.value)))
const yTicks = computed(() => [maximum.value, maximum.value / 2, 0])
const xTicks = computed(() => {
  const last = rows.value.length - 1
  const intervals = Math.max(1, Math.min(4, last))
  return Array.from({ length: Math.min(5, rows.value.length) }, (_, tick) =>
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
  <UiCard class="min-w-0">
    <UiCardHeader>
      <h2 class="text-base font-semibold">访问趋势</h2>
      <UiCardDescription>最近 24 个有记录的小时 · 按接口原始时间显示</UiCardDescription>
    </UiCardHeader>
    <UiCardContent>
      <template v-if="active">
        <div
          class="mb-4 flex flex-wrap items-baseline justify-between gap-2 rounded-md bg-accent-data-surface px-3 py-2 text-sm text-accent-data-foreground"
          aria-live="polite"
        >
          <span class="font-mono tabular-nums">{{ active.label }}:00</span>
          <span><strong class="font-mono tabular-nums">{{ number.format(active.count) }}</strong> 次访问</span>
        </div>
        <div class="grid grid-cols-[auto_minmax(0,1fr)] gap-x-2 gap-y-3">
          <div class="flex flex-col justify-between text-right font-mono text-xs tabular-nums text-muted-foreground" aria-hidden="true">
            <span v-for="tick in yTicks" :key="tick" class="h-0 -translate-y-2">
              {{ number.format(tick) }}
            </span>
          </div>
          <div class="relative min-w-0">
            <div class="pointer-events-none absolute inset-0 flex flex-col justify-between" aria-hidden="true">
              <span v-for="tick in yTicks" :key="tick" class="border-t border-dashed border-border" />
            </div>
            <div
              ref="bars"
              class="relative flex h-44 items-end gap-1 sm:h-56 sm:gap-2"
              role="radiogroup"
              aria-label="每小时访问量"
            >
              <button
                v-for="(row, index) in rows"
                :key="row.label"
                type="button"
                role="radio"
                class="group flex h-full min-w-0 flex-1 items-end rounded-sm focus-visible:z-10 focus-visible:outline-2 focus-visible:outline-ring"
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
                  class="w-full rounded-t-sm"
                  :class="[
                    active.label === row.label ? 'bg-chart-1' : 'bg-chart-1/45 group-hover:bg-chart-1/75',
                    { 'min-h-[3px]': row.count > 0 },
                  ]"
                  :style="{ height: row.count ? `${(row.count / maximum) * 100}%` : '0px' }"
                />
              </button>
            </div>
          </div>
          <div
            class="col-start-2 grid gap-1 font-mono text-[0.6875rem] leading-tight tabular-nums text-muted-foreground sm:gap-2 sm:text-xs"
            :style="{ gridTemplateColumns: `repeat(${rows.length}, minmax(0, 1fr))` }"
            aria-hidden="true"
          >
            <span
              v-for="index in xTicks"
              :key="index"
              class="w-max text-center"
              :class="index === 0 ? 'justify-self-start' : index === rows.length - 1 ? 'justify-self-end' : 'justify-self-center'"
              :style="{ gridColumnStart: index + 1, gridRowStart: 1 }"
            >
              <span class="block">{{ rows[index]?.label.slice(5, 10) }}</span>
              <span class="block">{{ rows[index]?.label.slice(11) }}:00</span>
            </span>
          </div>
        </div>

        <details class="mt-5 rounded-md border px-3 py-2 text-sm">
          <summary class="w-fit cursor-pointer rounded-sm focus-visible:outline-2 focus-visible:outline-ring">
            查看小时明细
          </summary>
          <UiTable class="mt-3">
            <UiTableHeader>
              <UiTableRow>
                <UiTableHead>小时</UiTableHead>
                <UiTableHead class="text-right">访问量</UiTableHead>
              </UiTableRow>
            </UiTableHeader>
            <UiTableBody>
              <UiTableRow v-for="row in rows" :key="row.label">
                <UiTableCell class="font-mono tabular-nums">{{ row.label }}:00</UiTableCell>
                <UiTableCell class="text-right font-mono tabular-nums">
                  {{ number.format(row.count) }}
                </UiTableCell>
              </UiTableRow>
            </UiTableBody>
          </UiTable>
        </details>
      </template>
      <p v-else class="rounded-md border border-dashed py-12 text-center text-sm text-muted-foreground">
        暂无小时访问记录
      </p>
    </UiCardContent>
  </UiCard>
</template>
