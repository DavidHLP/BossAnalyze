<script setup lang="ts">
import type { CountRow } from '@/lib/dashboard'
const props = defineProps<{ title: string; description: string; rows: CountRow[] }>()
const total = computed(() => props.rows.reduce((sum, row) => sum + row.count, 0))
const number = new Intl.NumberFormat('zh-CN')
</script>

<template>
  <UiCard>
    <UiCardHeader>
      <UiCardTitle>{{ title }}</UiCardTitle>
      <UiCardDescription>{{ description }}</UiCardDescription>
    </UiCardHeader>
    <UiCardContent>
      <ul v-if="rows.length" class="space-y-5">
        <li v-for="row in rows" :key="row.label" class="space-y-2">
          <div class="flex items-center justify-between gap-4 text-sm">
            <span class="min-w-0 break-all font-medium">{{ row.label || '未知' }}</span>
            <span class="shrink-0 tabular-nums">
              {{ number.format(row.count) }}
              <span class="ml-2 text-muted-foreground">
                {{ total ? ((row.count / total) * 100).toFixed(1) : '0.0' }}%
              </span>
            </span>
          </div>
          <div class="h-1.5 overflow-hidden rounded-full bg-muted" aria-hidden="true">
            <div
              class="h-full rounded-full bg-primary"
              :style="{ width: `${total ? (row.count / total) * 100 : 0}%` }"
            />
          </div>
        </li>
      </ul>
      <p v-else class="flex min-h-32 items-center justify-center text-sm text-muted-foreground">
        暂无{{ title }}数据
      </p>
    </UiCardContent>
  </UiCard>
</template>
