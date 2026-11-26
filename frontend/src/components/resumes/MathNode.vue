<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeMathEngine } from '@/lib/resume-math'

const props = defineProps<{
  tex: string; raw: string; display?: boolean
  engine: ResumeMathEngine | null; loading?: boolean; loadFailed?: boolean
}>()
const result = computed(() => props.engine?.render(props.tex, !!props.display))
const message = computed(() => {
  if (props.loadFailed) return '公式渲染器加载失败，已显示原文。'
  if (result.value && !result.value.ok) return result.value.message
  return props.loading ? '公式加载中，暂时显示原文。' : '公式预览不可用，已显示原文。'
})
</script>

<template>
  <span v-if="result?.ok" :class="{ 'block max-w-full overflow-x-auto': display }" v-html="result.html" />
  <component :is="display ? 'div' : 'span'" v-else role="note"
    :class="loading ? 'text-muted-foreground' : 'rounded border border-destructive px-1 text-destructive'">
    <pre v-if="display" class="overflow-x-auto whitespace-pre-wrap wrap-anywhere"><code>{{ raw }}</code></pre>
    <code v-else>{{ raw }}</code>
    <span :class="loading ? 'sr-only' : 'text-xs'">{{ message }}</span>
  </component>
</template>
