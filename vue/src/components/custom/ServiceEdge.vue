<script setup lang="ts">
import { BaseEdge, EdgeLabelRenderer, getBezierPath, type EdgeProps } from '@vue-flow/core'
import { computed } from 'vue'

const props = defineProps<EdgeProps>()

const path = computed(() => getBezierPath(props))

defineOptions({
  inheritAttrs: false
})
</script>

<template>
  <BaseEdge :path="path[0]" class="service-edge" />
  <EdgeLabelRenderer>
    <div
      :style="{
        pointerEvents: 'all',
        position: 'absolute',
        transform: `translate(-50%, -50%) translate(${path[1]}px,${path[2]}px)`,
      }"
      class="edge-label nodrag nopan"
    >
      {{ data.label || '调用' }}
    </div>
  </EdgeLabelRenderer>
</template>

<style scoped>
.service-edge {
  stroke: var(--el-color-primary);
  stroke-width: 2;
}

.edge-label {
  background: var(--el-bg-color-overlay);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--el-text-color-regular);
  border: 1px solid var(--el-border-color);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}
</style>
