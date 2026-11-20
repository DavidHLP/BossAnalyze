<script setup lang="ts">
import { peakVisits } from '@/lib/dashboard'
import type { CountRow } from '@/lib/dashboard'

const props = defineProps<{ hours: CountRow[] }>()
const rows = computed(() => props.hours.slice(-24))
const maximum = computed(() => Math.max(1, peakVisits(rows.value)))
const selected = ref<string | null>(null)
watch(() => props.hours, () => { selected.value = null })
const active = computed(
  () => rows.value.find((row) => row.label === selected.value) ?? rows.value.at(-1),
)
const number = new Intl.NumberFormat('zh-CN')
</script>

<template>
  <UiCard class="min-w-0 shadow-panel">
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
        <div class="relative">
          <div class="pointer-events-none absolute inset-0 flex flex-col justify-between" aria-hidden="true">
            <span v-for="line in 4" :key="line" class="border-t border-dashed border-border" />
          </div>
          <div class="relative flex h-44 items-end gap-1 px-0.5 sm:h-56 sm:gap-2" role="group" aria-label="每小时访问量">
            <button
              v-for="row in rows"
              :key="row.label"
              type="button"
              class="group flex h-full min-w-0 flex-1 items-end rounded-sm focus-visible:outline-2 focus-visible:outline-ring"
              :aria-label="`${row.label}:00，${number.format(row.count)} 次访问`"
              :aria-pressed="active.label === row.label"
              @mouseenter="selected = row.label"
              @focus="selected = row.label"
              @click="selected = row.label"
            >
              <span
                class="w-full rounded-t-sm group-aria-pressed:bg-chart-1 group-aria-pressed:ring-1 group-aria-pressed:ring-inset group-aria-pressed:ring-accent-data-foreground"
                :class="row.count ? 'bg-chart-5' : 'min-h-px bg-chart-5'"
                :style="{ height: `${(row.count / maximum) * 100}%` }"
              />
            </button>
          </div>
        </div>
        <div class="mt-3 flex flex-wrap justify-between gap-x-4 gap-y-1 font-mono text-xs tabular-nums text-muted-foreground">
          <span>{{ rows[0]?.label }}:00</span>
          <span v-if="rows.length > 1">{{ rows.at(-1)?.label }}:00</span>
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
