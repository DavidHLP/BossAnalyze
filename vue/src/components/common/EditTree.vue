<template>
  <div class="edit-tree-container">
    <el-tree
      ref="treeRef"
      :data="treeData"
      :props="defaultProps"
      node-key="id"
      :filter-node-method="filterNode"
      :expand-on-click-node="false"
      :default-expanded-keys="expandedKeys"
      :draggable="draggable"
      @node-drag-end="handleDragEnd"
      highlight-current
      @node-click="handleNodeClick"
      :class="customClass">
      <template #default="{ node, data }">
        <slot name="node" :node="node" :data="data">
          <div class="tree-node">
            <div class="node-label">
              <slot name="node-icon" :node="node" :data="data">
                <el-icon v-if="data.meta?.metaIcon" class="node-icon">
                  <component :is="data.meta.metaIcon" />
                </el-icon>
              </slot>
              <slot name="node-text" :node="node" :data="data">
                <span class="node-text">{{ node.label }}</span>
              </slot>
              <slot name="node-suffix" :node="node" :data="data"></slot>
            </div>
            <div class="node-actions">
              <slot name="node-actions" :node="node" :data="data"></slot>
            </div>
          </div>
        </slot>
      </template>
    </el-tree>
  </div>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits, watch, nextTick } from 'vue'
import type { TreeInstance, TreeNode } from 'element-plus'

// 定义接口
interface TreeNodeData {
  id?: number | string;
  name?: string;
  path?: string;
  permission?: string;
  children?: TreeNodeData[];
  meta?: {
    metaIcon?: string | null;
    component?: string;
    type?: string;
    [key: string]: string | boolean | null | undefined | TreeNodeData[] | { [key: string]: string | boolean | null };
  };
  [key: string]: string | number | boolean | null | undefined | TreeNodeData[] | {
    [key: string]: string | boolean | null | undefined | TreeNodeData[] | Record<string, unknown>;
  };
}

// 定义Props
const props = defineProps({
  treeData: {
    type: Array as () => TreeNodeData[],
    required: true,
    default: () => []
  },
  nodeProps: {
    type: Object as () => {
      label: string;
      children: string;
      [key: string]: string;
    },
    default: () => ({
      label: 'name',
      children: 'children'
    })
  },
  expandedKeys: {
    type: Array as () => (string | number)[],
    default: () => []
  },
  draggable: {
    type: Boolean,
    default: true
  },
  customClass: {
    type: String,
    default: ''
  },
  filterMethod: {
    type: Function,
    default: undefined
  }
})

// 定义Emits
const emit = defineEmits([
  'node-click',
  'node-drag-end',
  'update:expandedKeys'
])

// 内部状态
const treeRef = ref<TreeInstance | null>(null)
const defaultProps = ref(props.nodeProps)

// 树节点筛选方法
const filterNode = (value: string, data: TreeNodeData): boolean => {
  if (props.filterMethod && typeof props.filterMethod === 'function') {
    return props.filterMethod(value, data)
  }

  if (!value) return true
  // 默认通过name属性筛选，可以根据需要调整
  return Boolean(data.name?.includes(value) ||
         data.path?.includes(value) ||
         String(data.permission || '').includes(value) ||
         data.meta?.component?.includes(value))
}

// 处理节点点击
const handleNodeClick = (data: TreeNodeData) => {
  emit('node-click', data)
}

// 处理拖拽结束
const handleDragEnd = (draggingNode: TreeNode, dropNode: TreeNode | null, dropType: string) => {
  // 发送拖拽结束事件到父组件处理
  emit('node-drag-end', draggingNode, dropNode, dropType)
}

// 监听 expandedKeys 变化，动态折叠/展开树节点
watch(() => props.expandedKeys, (val) => {
  nextTick(() => {
    if (treeRef.value) {
      // 先全部折叠
      treeRef.value.store.setDefaultExpandedKeys(val)
      // 强制刷新
      if (treeRef.value.store.nodesMap) {
        Object.values(treeRef.value.store.nodesMap).forEach((node: { key: string | number; expanded: boolean }) => {
          node.expanded = val.includes(node.key)
        })
      }
    }
  })
})

// 对外暴露方法
defineExpose({
  treeRef,
  filter: (val: string) => treeRef.value?.filter(val),
  getNode: (key: string | number) => treeRef.value?.getNode(key),
  setCurrentKey: (key: string | number) => treeRef.value?.setCurrentKey(key),
  getCurrentNode: () => treeRef.value?.getCurrentNode(),
  getCurrentKey: () => treeRef.value?.getCurrentKey()
})
</script>

<style scoped>
/* 最小化样式，只保留必要的结构样式 */
.edit-tree-container {
  height: 100%;
}
</style>
