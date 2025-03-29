<script setup lang="ts">
import { computed } from 'vue'
import { Position, Handle } from '@vue-flow/core'
import type { NodeProps } from '@vue-flow/core'

const props = defineProps<NodeProps>()

const x = computed(() => `${Math.round(props.position.x)}px`)
const y = computed(() => `${Math.round(props.position.y)}px`)
</script>

<template>
  <div class="service-node">
    <div class="node-content">
      <div class="node-label">{{ data.label }}</div>
      <div class="node-type">{{ data.type || '服务' }}</div>
    </div>

    <Handle type="source" :position="Position.Bottom" class="handle handle-bottom" />
    <Handle type="target" :position="Position.Top" class="handle handle-top" />
  </div>
</template>

<style scoped>
.service-node {
  padding: 10px;
  border-radius: 8px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  min-width: 120px;
  text-align: center;
}

.node-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.node-label {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.node-type {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.handle {
  width: 8px;
  height: 8px;
  background: var(--el-color-primary);
  border: 2px solid var(--el-bg-color);
}

.handle-bottom {
  bottom: -4px;
}

.handle-top {
  top: -4px;
}
</style>
