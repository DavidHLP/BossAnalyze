<script setup lang="ts">
import { computed } from 'vue'
import { HugeiconsIcon } from '@hugeicons/vue'
import { visitShare } from '@/lib/dashboard'
import type { CountRow } from '@/lib/dashboard'

const props = defineProps<{
  title: string
  icon: InstanceType<typeof HugeiconsIcon>['$props']['icon']
  rows: CountRow[]
  note: string
}>()
const total = computed(() => props.rows.reduce((sum, row) => sum + row.count, 0))
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
          class="grid min-w-0 grid-cols-[minmax(4.5rem,1fr)_minmax(0,4fr)_4rem] items-center gap-2 text-xs"
          :title="`${number.format(row.count)} 次访问`"
        >
          <span class="break-words font-medium">{{ row.label || '未知' }}</span>
          <div class="h-1.5 overflow-hidden rounded-full bg-muted" aria-hidden="true">
            <div
              class="h-full rounded-full"
              :class="index === 0 ? 'bg-accent-data' : 'bg-accent-data/75'"
              :style="{ width: `${visitShare(row.count, total)}%` }"
            />
          </div>
          <span class="text-right font-mono tabular-nums">
            <span class="sr-only">{{ number.format(row.count) }} 次访问，占比</span>
            {{ visitShare(row.count, total).toFixed(1) }}%
          </span>
        </li>
      </ul>
      <p v-else class="py-5 text-center text-sm text-muted-foreground">暂无{{ title }}数据</p>
      <p class="text-xs leading-5 text-muted-foreground">{{ note }}</p>
    </UiCardContent>
  </UiCard>
</template>
