<script setup lang="ts">
import { computed } from 'vue'
import { HugeiconsIcon } from '@hugeicons/vue'
import { formatPercentage, visitShare } from '@/lib/dashboard'
import type { CountRow } from '@/lib/dashboard'

const props = defineProps<{
  title: string
  icon: InstanceType<typeof HugeiconsIcon>['$props']['icon']
  rows: CountRow[]
  note: string
  totalVisits?: number
}>()
const total = computed(() => props.totalVisits ?? props.rows.reduce((sum, row) => sum + row.count, 0))
const number = new Intl.NumberFormat('zh-CN')
</script>

<template>
  <UiCard class="min-w-0 gap-3 py-3">
    <UiCardHeader class="px-5">
      <h2 class="flex items-center gap-2 text-base font-semibold">
        <HugeiconsIcon :icon="icon" class="size-4 text-muted-foreground" aria-hidden="true" />
        {{ title }}
      </h2>
    </UiCardHeader>
    <UiCardContent class="space-y-3 px-5">
      <ul v-if="rows.length" class="space-y-2">
        <li
          v-for="(row, index) in rows"
          :key="row.label"
          class="grid min-w-0 grid-cols-[minmax(0,1fr)_auto] items-baseline gap-x-2 gap-y-1 text-xs"
        >
          <div class="min-w-0 break-words">
            <span class="font-medium">{{ row.label || '未知' }}</span>
            <p v-if="row.detail" class="text-muted-foreground">{{ row.detail }}</p>
          </div>
          <span class="text-right font-mono tabular-nums">
            {{ number.format(row.count) }} 次 · {{ formatPercentage(visitShare(row.count, total)) }}
          </span>
          <div class="col-span-full h-1.5 overflow-hidden rounded-full bg-muted" aria-hidden="true">
            <div
              class="h-full rounded-full"
              :class="index === 0 ? 'bg-accent-data' : 'bg-accent-data/75'"
              :style="{ width: `${visitShare(row.count, total) ?? 0}%` }"
            />
          </div>
        </li>
      </ul>
      <p v-else class="py-5 text-center text-sm text-muted-foreground">暂无{{ title }}数据</p>
      <p class="text-xs leading-5 text-muted-foreground">{{ note }}</p>
    </UiCardContent>
  </UiCard>
</template>
