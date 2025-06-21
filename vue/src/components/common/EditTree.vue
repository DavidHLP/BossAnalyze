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
      :class="['modern-tree', customClass]">
      <template #default="{ node, data }">
        <slot name="node" :node="node" :data="data">
          <!-- 默认节点布局 -->
          <div class="default-tree-node">
            <div class="node-main-content">
              <div class="node-info-section">
                <slot name="node-icon" :node="node" :data="data">
                  <div class="default-node-icon" v-if="data.meta?.metaIcon">
                    <el-icon class="icon">
                      <component :is="data.meta.metaIcon" />
                    </el-icon>
                  </div>
                </slot>
                <div class="node-text-section">
                  <slot name="node-text" :node="node" :data="data">
                    <div class="node-title">{{ node.label }}</div>
                    <div class="node-subtitle" v-if="data.path">{{ data.path }}</div>
                  </slot>
                </div>
              </div>
              <div class="node-badges-section">
                <slot name="node-badges" :node="node" :data="data">
                  <div class="default-badge" v-if="data.meta?.type">
                    {{ getTypeText(data.meta.type) }}
                  </div>
                </slot>
              </div>
            </div>
            <div class="node-actions-section">
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
  },
  // 新增：是否启用现代布局模式
  modernLayout: {
    type: Boolean,
    default: true
  }
})

// 定义Emits
const emit = defineEmits<{
  'node-click': [data: TreeNodeData]
  'node-drag-end': [draggingNode: TreeNode, dropNode: TreeNode | null, dropType: string]
  'update:expandedKeys': [keys: (string | number)[]]
}>()

// 内部状态
const treeRef = ref<TreeInstance | null>(null)
const defaultProps = ref(props.nodeProps)

// 获取类型文本
const getTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'M': '目录',
    'C': '菜单',
    'F': '按钮'
  }
  return typeMap[type] || '未知'
}

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

<style scoped lang="scss">
.edit-tree-container {
  height: 100%;

  // 现代树形样式基础
  .modern-tree {
    :deep(.el-tree-node) {
      .el-tree-node__content {
        padding: 0 !important;
        height: auto !important;
        overflow: visible;
      }
    }
  }
}

// ==============================================
// 默认节点布局样式
// ==============================================
.default-tree-node {
  width: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 8px 12px;
  gap: 8px;
  min-height: 48px;

  .node-main-content {
    flex: 1;
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
    min-width: 0; // 防止 flex 子元素溢出
  }

  .node-info-section {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    flex: 1;
    min-width: 0; // 防止溢出

    .default-node-icon {
      flex-shrink: 0;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      background-color: #f5f7fa;

      .icon {
        font-size: 14px;
        color: #409eff;
      }
    }

    .node-text-section {
      flex: 1;
      min-width: 0; // 防止溢出
      display: flex;
      flex-direction: column;
      gap: 2px;

      .node-title {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        line-height: 1.4;
        word-break: break-word;
        overflow-wrap: break-word;
      }

      .node-subtitle {
        font-size: 12px;
        color: #909399;
        line-height: 1.2;
        font-family: 'Monaco', 'Consolas', 'Courier New', monospace;
        word-break: break-all;
        overflow-wrap: break-word;
        opacity: 0.8;
      }
    }
  }

  .node-badges-section {
    flex-shrink: 0;
    display: flex;
    align-items: flex-start;
    gap: 4px;
    margin-top: 2px;

    .default-badge {
      padding: 2px 6px;
      font-size: 10px;
      font-weight: 600;
      color: #606266;
      background-color: #f0f2f5;
      border-radius: 4px;
      line-height: 1;
      white-space: nowrap;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
  }

  .node-actions-section {
    flex-shrink: 0;
    display: flex;
    align-items: flex-start;
    gap: 4px;
    margin-top: 2px;
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  &:hover {
    .node-actions-section {
      opacity: 1;
    }
  }
}

// ==============================================
// 响应式适配
// ==============================================
@media (max-width: 768px) {
  .default-tree-node {
    .node-main-content {
      flex-direction: column;
      align-items: flex-start;
      gap: 6px;
    }

    .node-info-section {
      width: 100%;

      .node-text-section {
        .node-subtitle {
          font-size: 11px;
        }
      }
    }

    .node-badges-section {
      margin-top: 0;
    }

    .node-actions-section {
      opacity: 1; // 在移动端始终显示操作按钮
      margin-top: 0;
    }
  }
}

// ==============================================
// 紧凑布局变体
// ==============================================
.edit-tree-container.compact {
  .default-tree-node {
    padding: 4px 8px;
    min-height: 36px;

    .node-info-section {
      gap: 6px;

      .default-node-icon {
        width: 20px;
        height: 20px;

        .icon {
          font-size: 12px;
        }
      }

      .node-text-section {
        .node-title {
          font-size: 13px;
        }

        .node-subtitle {
          font-size: 11px;
        }
      }
    }

    .node-badges-section {
      .default-badge {
        padding: 1px 4px;
        font-size: 9px;
      }
    }
  }
}
</style>
