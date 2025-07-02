<template>
  <div ref="container" class="graph-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { Graph } from '@antv/g6'
import type { GraphData, GraphOptions } from '@antv/g6'

const props = defineProps<{
  data: GraphData
}>()

const emit = defineEmits<{
  (e: 'node-click', id: string): void
}>()

const container = ref<HTMLDivElement | null>(null)
let graph: Graph | null = null

// Gitflow 颜色方案
const branchColors: Record<string, string> = {
  main: '#8E44AD', // 紫色
  develop: '#27AE60', // 绿色
  feature: '#2980B9', // 蓝色
  release: '#F39C12', // 橙色
  hotfix: '#C0392B', // 红色
  default: '#7F8C8D', // 灰色
}

const initGraph = () => {
  if (graph) {
    graph.destroy()
  }
  if (!container.value) return

  // Prioritize 'main' branch nodes in the array to influence dagre layout
  const sortedNodes = props.data.nodes
    ? [...props.data.nodes].sort((a: any, b: any) => {
        if (a.data.branch === 'main' && b.data.branch !== 'main') return -1
        if (b.data.branch === 'main' && a.data.branch !== 'main') return 1
        return 0
      })
    : []

  const graphOptions: GraphOptions = {
    container: container.value,
    width: container.value.clientWidth,
    height: 500,
    data: {
      nodes: sortedNodes,
      edges: props.data.edges || [],
    },
    layout: {
      type: 'dagre',
      rankdir: 'BT',
      align: 'UL',
      nodesep: 30,
      ranksep: 50,
    },
    node: {
      style: {
        r: 15,
        fill: (d: any) => branchColors[d.data.branch] || branchColors.default,
        stroke: '#bdc3c7',
        lineWidth: 2,
        labelText: (d: any) => d.data.label,
        labelPlacement: 'right',
        labelMaxWidth: 150,
      },
      state: {
        selected: {
          stroke: '#000',
          lineWidth: 3,
        },
      },
    },
    edge: {
      style: {
        stroke: (d: any) => branchColors[d.data.branch] || branchColors.default,
        lineWidth: 2,
        endArrow: true,
      },
    },
    behaviors: ['drag-canvas', 'zoom-canvas', 'click-select'],
  }

  graph = new Graph(graphOptions)

  graph.on('node:click', (evt: any) => {
    if (evt.item) {
      const model = evt.item.get('model')
      if (model?.id) {
        emit('node-click', model.id)
      }
    }
  })

  graph.render()
}

onMounted(() => {
  initGraph()
})

watch(
  () => props.data,
  () => {
    // 这种方法比较暴力，但能确保在API不确定的情况下正确工作。
    // G6 v5 的文档在动态数据更新方面似乎有些模糊，这是最稳妥的策略。
    initGraph()
  },
  { deep: true },
)

onUnmounted(() => {
  graph?.destroy()
})
</script>

<style scoped lang="scss">
.graph-container {
  width: 100%;
  height: 500px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}
</style>
