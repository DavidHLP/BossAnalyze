<script setup lang="ts">
import type { CountRow } from '@/lib/dashboard'
const props = defineProps<{ hours: CountRow[] }>()
const rows = computed(() => props.hours.slice(-24))
const maximum = computed(() => Math.max(1, ...rows.value.map((row) => row.count)))
const selected = ref<string | null>(null)
const active = computed(
  () => rows.value.find((row) => row.label === selected.value) ?? rows.value.at(-1),
)
const number = new Intl.NumberFormat('zh-CN')
</script>

<template>
  <UiCard class="min-w-0">
    <UiCardHeader>
      <UiCardTitle>访问趋势</UiCardTitle>
      <UiCardDescription>最近 24 个有记录的小时 · 按接口原始时间显示</UiCardDescription>
    </UiCardHeader>
    <UiCardContent>
      <template v-if="rows.length">
        <div
          class="mb-5 flex flex-wrap items-baseline justify-between gap-2 text-sm"
          aria-live="polite"
        >
          <span class="text-muted-foreground">{{ active?.label }}:00</span>
          <span class="font-medium tabular-nums"
            >{{ number.format(active?.count ?? 0) }} 次访问</span
          >
        </div>
        <div
          class="flex h-48 items-end gap-1 border-b border-border pt-2"
          aria-label="每小时访问量"
        >
          <button
            v-for="row in rows"
            :key="row.label"
            type="button"
            class="group relative flex h-full min-w-0 flex-1 items-end rounded-t-sm outline-offset-4 focus-visible:outline-2 focus-visible:outline-ring"
            :aria-label="`${row.label}:00，${number.format(row.count)} 次访问`"
            :aria-pressed="active?.label === row.label"
            @mouseenter="selected = row.label"
            @focus="selected = row.label"
            @click="selected = row.label"
          >
            <span
              class="w-full rounded-t-sm bg-primary/35 group-hover:bg-primary group-focus-visible:bg-primary motion-safe:transition-colors"
              :class="{ 'bg-primary!': active?.label === row.label }"
              :style="{ height: `${(row.count / maximum) * 100}%` }"
            />
          </button>
        </div>
        <div class="mt-3 flex justify-between gap-3 text-xs text-muted-foreground">
          <span>{{ rows[0]?.label }}:00</span><span>{{ rows.at(-1)?.label }}:00</span>
        </div>
        <details class="mt-5 text-xs text-muted-foreground">
          <summary
            class="w-fit cursor-pointer rounded-sm focus-visible:outline-2 focus-visible:outline-ring"
          >
            查看小时明细
          </summary>
          <UiTable class="mt-3">
            <UiTableHeader
              ><UiTableRow
                ><UiTableHead>小时</UiTableHead
                ><UiTableHead class="text-right">访问量</UiTableHead></UiTableRow
              ></UiTableHeader
            >
            <UiTableBody
              ><UiTableRow v-for="row in rows" :key="row.label"
                ><UiTableCell>{{ row.label }}:00</UiTableCell
                ><UiTableCell class="text-right tabular-nums">{{
                  number.format(row.count)
                }}</UiTableCell></UiTableRow
              ></UiTableBody
            >
          </UiTable>
        </details>
      </template>
      <div
        v-else
        class="flex h-64 items-center justify-center rounded-md border border-dashed text-sm text-muted-foreground"
      >
        暂无小时访问记录
      </div>
    </UiCardContent>
  </UiCard>
</template>
