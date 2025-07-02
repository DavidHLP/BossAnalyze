<template>
  <div ref="container" class="g6-graph-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Graph } from '@antv/g6'
import type { GraphOptions } from '@antv/g6'
import type { Commit, Branch, Tag } from '@/api/resume/types'

interface Props {
  commits: Commit[]
  branches: Branch[]
  tags: Tag[]
}

const props = defineProps<Props>()

const container = ref<HTMLDivElement | null>(null)
let graph: Graph | null = null

const calculateLayout = (commits: Commit[], branches: Branch[]) => {
  const nodes: any[] = []
  const edges: any[] = []

  // 1. 为分支分配泳道 (x 坐标)
  const branchLanes = new Map<string, number>()
  let laneIndex = 0
  // 主分支或默认分支总是在第一条泳道
  const mainBranch =
    branches.find((b) => b.isDefault) || branches.find((b) => b.name === 'main') || branches[0]
  if (mainBranch) {
    branchLanes.set(mainBranch.name, laneIndex++)
  }
  branches.forEach((branch) => {
    if (!branchLanes.has(branch.name)) {
      branchLanes.set(branch.name, laneIndex++)
    }
  })

  // 2. 按时间戳排序 commits (y 坐标)
  const sortedCommits = [...commits].sort(
    (a, b) => new Date(a.commitTime).getTime() - new Date(b.commitTime).getTime(),
  )
  const commitYPos = new Map<string, number>()
  sortedCommits.forEach((commit, index) => {
    commitYPos.set(commit.commitId, index)
  })

  const LANE_WIDTH = 150
  const NODE_HEIGHT_SEP = 80

  // 3. 创建节点并计算坐标
  commits.forEach((commit) => {
    const lane = branchLanes.get(commit.branch) ?? 0 // 如果找不到分支，默认放到主泳道
    const y = commitYPos.get(commit.commitId) ?? 0

    nodes.push({
      id: commit.commitId,
      data: {
        label: `${commit.commitMessage.substring(0, 15)}${
          commit.commitMessage.length > 15 ? '...' : ''
        }\n${commit.commitId.substring(0, 7)}`,
        isMergeCommit: commit.isMergeCommit,
        commit: commit,
      },
      style: {
        x: lane * LANE_WIDTH,
        y: y * NODE_HEIGHT_SEP,
      },
    })

    // 4. 创建边
    if (commit.parentCommitIds) {
      commit.parentCommitIds.forEach((parentId: string) => {
        edges.push({
          source: parentId,
          target: commit.commitId,
          // 对于跨泳道的合并线，使用曲线
          data: {
            type: commit.isMergeCommit ? 'cubic-horizontal' : 'line',
          },
        })
      })
    }
  })

  // 5. 添加分支和标签的"虚拟"节点，用于显示标签
  branches.forEach((branch) => {
    const headCommit = nodes.find((n) => n.id === branch.headCommitId)
    if (headCommit) {
      nodes.push({
        id: `branch-${branch.name}`,
        data: {
          label: branch.name,
          type: 'branch',
        },
        style: {
          x: headCommit.style.x,
          y: headCommit.style.y - NODE_HEIGHT_SEP / 2.5, // 显示在 commit 节点上方
          size: [80, 25],
        },
      })
      edges.push({
        source: `branch-${branch.name}`,
        target: branch.headCommitId,
        data: { type: 'line' },
      })
    }
  })

  props.tags.forEach((tag) => {
    const taggedCommit = nodes.find((n) => n.id === tag.commitId)
    if (taggedCommit) {
      nodes.push({
        id: `tag-${tag.name}`,
        data: {
          label: tag.name,
          type: 'tag',
        },
        style: {
          x: taggedCommit.style.x,
          y: taggedCommit.style.y - NODE_HEIGHT_SEP / 1.5, // 显示在 commit 节点上方，比分支低一点
          size: [80, 25],
        },
      })
      edges.push({
        source: `tag-${tag.name}`,
        target: tag.commitId,
        data: { type: 'line' },
      })
    }
  })

  return { nodes, edges }
}

onMounted(() => {
  if (container.value) {
    const { nodes, edges } = calculateLayout(props.commits, props.branches)
    const width = container.value.scrollWidth
    const height = container.value.scrollHeight || 600

    const graphOptions: GraphOptions = {
      container: container.value,
      width,
      height,
      data: {
        nodes,
        edges,
      },
      node: {
        type: 'rect',
        style: {
          size: [120, 50],
          radius: 5,
          lineWidth: 2,
          fill: (d: any) => {
            if (d.data.type === 'branch') return '#d4edda'
            if (d.data.type === 'tag') return '#fff3cd'
            if (d.data.isMergeCommit) return '#f0f0f0'
            return '#e8f7ff'
          },
          stroke: (d: any) => {
            if (d.data.type === 'branch') return '#c3e6cb'
            if (d.data.type === 'tag') return '#ffeeba'
            if (d.data.isMergeCommit) return '#d9d9d9'
            return '#a3d3ff'
          },
          labelText: (d: any) => d.data.label,
          labelFontSize: 12,
          labelFill: (d: any) =>
            d.data.type === 'branch' ? '#155724' : d.data.type === 'tag' ? '#856404' : '#333',
        },
      },
      edge: {
        type: (d: any) => d.data?.type || 'line',
        style: {
          stroke: '#a3a3a3',
          lineWidth: 2,
          endArrow: true,
        },
      },
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-node'],
    }

    graph = new Graph(graphOptions)
  }
})

onUnmounted(() => {
  if (graph) {
    graph.destroy()
    graph = null
  }
})

watch(
  () => [props.commits, props.branches, props.tags],
  () => {
    if (graph) {
      const data = calculateLayout(props.commits, props.branches)
      graph.setData(data)
    }
  },
  { deep: true },
)
</script>

<style scoped>
.g6-graph-container {
  width: 100%;
  height: 100%;
  min-height: 500px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
