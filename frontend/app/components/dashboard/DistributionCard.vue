<script setup lang="ts">
import { visitShare } from '@/lib/dashboard'
import type { CountRow } from '@/lib/dashboard'

const props = defineProps<{ title: string; description: string; rows: CountRow[]; note?: string }>()
const total = computed(() => props.rows.reduce((sum, row) => sum + row.count, 0))
const number = new Intl.NumberFormat('zh-CN')
</script>

<template>
  <UiCard class="min-w-0">
    <UiCardHeader>
      <h2 class="text-base font-semibold">{{ title }}</h2>
      <UiCardDescription>{{ description }}</UiCardDescription>
    </UiCardHeader>
    <UiCardContent class="space-y-4">
      <ul v-if="rows.length" class="space-y-4">
        <li v-for="(row, index) in rows" :key="row.label" class="space-y-2">
          <div class="flex items-center justify-between gap-3 text-sm">
            <span class="min-w-0 break-all font-medium">{{ row.label || '未知' }}</span>
            <span class="shrink-0 font-mono tabular-nums">
              {{ number.format(row.count) }}
              <span class="ml-2 text-muted-foreground">
                {{ visitShare(row.count, total).toFixed(1) }}%
              </span>
            </span>
          </div>
          <div class="h-1.5 overflow-hidden rounded-full bg-muted" aria-hidden="true">
            <div
              class="h-full rounded-full"
              :class="index === 0 ? 'bg-chart-1' : 'bg-chart-1/45'"
              :style="{ width: `${visitShare(row.count, total)}%` }"
            />
          </div>
        </li>
      </ul>
      <p v-else class="py-8 text-center text-sm text-muted-foreground">暂无{{ title }}数据</p>
      <p v-if="note" class="border-t pt-3 text-xs leading-5 text-muted-foreground">{{ note }}</p>
    </UiCardContent>
  </UiCard>
</template>
