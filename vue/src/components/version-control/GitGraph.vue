<template>
  <div ref="container" class="git-graph-container"></div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Graph } from '@antv/g6'

const container = ref<HTMLDivElement | null>(null)

// Gitflow 颜色方案
const branchColors: Record<string, string> = {
  main: '#8E44AD', // 紫色
  develop: '#27AE60', // 绿色
  feature: '#2980B9', // 蓝色
  release: '#F39C12', // 橙色
  hotfix: '#C0392B', // 红色
  default: '#7F8C8D', // 灰色
}

// 模拟的 Gitflow 提交数据
const gitData = {
  nodes: [
    // main 分支
    { id: 'm1', data: { label: 'v1.0.0', branch: 'main' } },
    { id: 'm2', data: { label: 'v1.0.1', branch: 'main' } },
    { id: 'm3', data: { label: 'v1.1.0', branch: 'main' } },
    { id: 'm4', data: { label: 'v2.0.0', branch: 'main' } },

    // develop 分支
    { id: 'd1', data: { label: 'Start develop', branch: 'develop' } },
    { id: 'd2', data: { label: 'Merge feat/A', branch: 'develop' } },
    { id: 'd3', data: { label: 'Merge release/1.1', branch: 'develop' } },
    { id: 'd4', data: { label: 'Merge feat/B', branch: 'develop' } },

    // feature 分支
    { id: 'fA1', data: { label: 'Feat A - Start', branch: 'feature' } },
    { id: 'fA2', data: { label: 'Feat A - Finish', branch: 'feature' } },
    { id: 'fB1', data: { label: 'Feat B - Start', branch: 'feature' } },
    { id: 'fB2', data: { label: 'Feat B - Finish', branch: 'feature' } },

    // release 分支
    { id: 'r1', data: { label: 'Release 1.1.0', branch: 'release' } },
    { id: 'r2', data: { label: 'Release 2.0.0', branch: 'release' } },

    // hotfix 分支
    { id: 'h1', data: { label: 'Hotfix 1.0.1', branch: 'hotfix' } },
  ],
  edges: [
    // Main & Develop Branches
    { source: 'm1', target: 'd1' },
    { source: 'd1', target: 'd2' },
    { source: 'd2', target: 'd3' },
    { source: 'd3', target: 'd4' },
    { source: 'm1', target: 'm2' },
    { source: 'm2', target: 'm3' },
    { source: 'm3', target: 'm4' },

    // Feature A
    { source: 'd1', target: 'fA1' },
    { source: 'fA1', target: 'fA2' },
    { source: 'fA2', target: 'd2' }, // Merge into develop

    // Hotfix
    { source: 'm1', target: 'h1' },
    { source: 'h1', target: 'm2' }, // Merge into main

    // Release 1.1
    { source: 'd2', target: 'r1' },
    { source: 'r1', target: 'm3' }, // Merge into main
    { source: 'r1', target: 'd3' }, // Merge into develop

    // Feature B
    { source: 'd3', target: 'fB1' },
    { source: 'fB1', target: 'fB2' },
    { source: 'fB2', target: 'd4' }, // Merge into develop

    // Release 2.0
    { source: 'd4', target: 'r2' },
    { source: 'r2', target: 'm4' }, // Merge into main
  ],
}

// 为边动态赋予其目标节点的分支类型，以便正确着色
const nodesMap = new Map(gitData.nodes.map((node) => [node.id, node]))
const processedEdges = gitData.edges.map((edge) => {
  const targetNode = nodesMap.get(edge.target)
  return {
    ...edge,
    data: {
      branch: targetNode?.data.branch || 'default',
    },
  }
})

const processedData = { nodes: gitData.nodes, edges: processedEdges }

onMounted(() => {
  if (container.value) {
    const graph = new Graph({
      container: container.value,
      width: container.value.clientWidth,
      height: container.value.clientHeight,
      data: processedData,
      layout: {
        type: 'dagre',
        rankdir: 'LR',
        align: 'UL',
        nodesep: 20,
        ranksep: 50,
      },
      node: {
        type: 'circle',
        style: (d: any) => {
          const color = branchColors[d.data.branch] || branchColors.default
          return {
            r: 15,
            fill: color,
            stroke: '#bdc3c7',
            lineWidth: 2,
            labelText: d.data.label,
            labelPlacement: 'right',
            labelMaxWidth: 150,
          }
        },
        state: {
          selected: {
            stroke: '#000',
            lineWidth: 3,
          },
        },
      },
      edge: {
        type: 'polyline',
        style: (d: any) => {
          const color = branchColors[d.data.branch] || branchColors.default
          return {
            stroke: color,
            lineWidth: 2,
            endArrow: true,
          }
        },
        state: {
          selected: {
            stroke: '#000',
            lineWidth: 3,
          },
        },
      },
      behaviors: ['drag-canvas', 'zoom-canvas', 'click-select'],
    })

    graph.render()

    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect
        graph.setSize(width, height)
        graph.fitView()
      }
    })
    resizeObserver.observe(container.value)
  }
})
</script>

<style scoped lang="scss">
.git-graph-container {
  width: 100%;
  height: 500px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}
</style>
