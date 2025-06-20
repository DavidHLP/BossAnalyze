<template>
  <div class="select-tree-container">
    <div class="tree-header" v-if="showHeader">
      <div class="header-info">
        <el-icon class="header-icon"><FolderOpened /></el-icon>
        <span class="header-title">{{ headerTitle || '选择项目' }}</span>
      </div>
      <div class="header-stats" v-if="showStats">
        <span class="stats-text">
          已选择 <span class="stats-number">{{ getCheckedKeys().length }}</span> 项
        </span>
      </div>
    </div>

    <div class="tree-search" v-if="showSearch">
      <el-input
        v-model="searchText"
        placeholder="搜索..."
        clearable
        @input="handleSearch"
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <div class="tree-content">
      <el-tree
        ref="treeRef"
        :data="filteredTreeData"
        show-checkbox
        node-key="id"
        :props="defaultProps"
        :default-checked-keys="defaultCheckedKeys"
        :check-strictly="checkStrictly"
        :default-expand-all="defaultExpandAll"
        :expand-on-click-node="expandOnClickNode"
        :filter-node-method="filterNode"
        highlight-current
        @check-change="handleCheckChange"
        class="modern-tree"
        empty-text="暂无数据"
      >
        <template #default="{ node, data }">
          <div class="tree-node-content">
            <span class="node-label">{{ node.label }}</span>
            <span class="node-badge" v-if="data.children && data.children.length">
              {{ data.children.length }}
            </span>
          </div>
        </template>
      </el-tree>
    </div>

    <div class="tree-footer" v-if="showFooter">
      <div class="footer-actions">
        <el-button size="small" @click="expandAll" class="action-btn">
          <el-icon><Plus /></el-icon>
          全部展开
        </el-button>
        <el-button size="small" @click="collapseAll" class="action-btn">
          <el-icon><Minus /></el-icon>
          全部收缩
        </el-button>
        <el-button size="small" @click="clearSelection" class="action-btn">
          <el-icon><Close /></el-icon>
          清空选择
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue';
import { FolderOpened, Search, Plus, Minus, Close } from '@element-plus/icons-vue';

interface TreeNode {
  id: number;
  name: string;
  children?: TreeNode[];
  parent?: TreeNode;
  level?: number;
}

interface Props {
  treeData: TreeNode[];
  defaultCheckedKeys?: number[];
  checkStrictly?: boolean;
  defaultExpandAll?: boolean;
  expandOnClickNode?: boolean;
  showHeader?: boolean;
  showSearch?: boolean;
  showFooter?: boolean;
  showStats?: boolean;
  headerTitle?: string;
  defaultProps?: {
    label: string;
    children: string;
  };
}

const props = withDefaults(defineProps<Props>(), {
  defaultCheckedKeys: () => [],
  checkStrictly: true,
  defaultExpandAll: false,
  expandOnClickNode: false,
  showHeader: true,
  showSearch: true,
  showFooter: true,
  showStats: true,
  headerTitle: '',
  defaultProps: () => ({
    label: 'name',
    children: 'children'
  })
});

const emit = defineEmits<{
  (e: 'update:checkedKeys', keys: number[]): void;
  (e: 'check-change', data: TreeNode, checked: boolean): void;
}>();

const treeRef = ref();
const searchText = ref('');

// 过滤树数据
const filteredTreeData = computed(() => {
  if (!searchText.value) return props.treeData;
  return filterTreeData(props.treeData, searchText.value);
});

// 搜索处理
const handleSearch = (value: string) => {
  if (treeRef.value) {
    treeRef.value.filter(value);
  }
};

// 节点过滤方法
const filterNode = (value: string, data: any) => {
  if (!value) return true;
  return data.name.toLowerCase().includes(value.toLowerCase());
};

// 递归过滤树数据
const filterTreeData = (data: TreeNode[], searchValue: string): TreeNode[] => {
  const result: TreeNode[] = [];

  data.forEach(item => {
    if (item.name.toLowerCase().includes(searchValue.toLowerCase())) {
      result.push(item);
    } else if (item.children) {
      const filteredChildren = filterTreeData(item.children, searchValue);
      if (filteredChildren.length) {
        result.push({
          ...item,
          children: filteredChildren
        });
      }
    }
  });

  return result;
};

// 展开所有节点
const expandAll = () => {
  if (treeRef.value) {
    const allNodes = getAllTreeNodes(props.treeData);
    allNodes.forEach(node => {
      treeRef.value.store.getNode(node.id).expanded = true;
    });
  }
};

// 收缩所有节点
const collapseAll = () => {
  if (treeRef.value) {
    const allNodes = getAllTreeNodes(props.treeData);
    allNodes.forEach(node => {
      treeRef.value.store.getNode(node.id).expanded = false;
    });
  }
};

// 清空选择
const clearSelection = () => {
  if (treeRef.value) {
    treeRef.value.setCheckedKeys([]);
    emit('update:checkedKeys', []);
  }
};

// 初始化树形结构
const initTree = () => {
  if (!treeRef.value) return;

  treeRef.value.setCheckedKeys([]);
  treeRef.value.setCheckedKeys(props.defaultCheckedKeys);

  nextTick(() => {
    const allNodes = getAllTreeNodes(props.treeData);
    const checkedNodes = allNodes.filter(node =>
      props.defaultCheckedKeys.includes(node.id)
    );

    checkedNodes.forEach(node => {
      const treeNode = treeRef.value.getNode(node.id);
      if (treeNode) {
        checkParentNodes(treeNode);
      }
    });

    nextTick(() => {
      ensureParentNodesChecked();
    });
  });
};

// 确保所有已选中节点的父节点都被选中
const ensureParentNodesChecked = () => {
  if (!treeRef.value) return;

  const checkedNodes = treeRef.value.getCheckedNodes();
  checkedNodes.forEach((checkedNode: TreeNode) => {
    const treeNode = treeRef.value.getNode(checkedNode.id);
    if (treeNode && treeNode.parent) {
      checkParentNodes(treeNode);
    }
  });
};

// 递归获取树的所有节点
const getAllTreeNodes = (nodes: TreeNode[]): TreeNode[] => {
  let allNodes: TreeNode[] = [];

  nodes.forEach(node => {
    allNodes.push(node);
    if (node.children && node.children.length) {
      allNodes = allNodes.concat(getAllTreeNodes(node.children));
    }
  });

  return allNodes;
};

// 递归勾选所有父节点
const checkParentNodes = (node: TreeNode) => {
  if (!treeRef.value) return;

  if (!node || !node.parent || node.parent.id === undefined) return;
  if (node.parent.level === 0) return;

  if (!treeRef.value.getCheckedKeys().includes(node.parent.id)) {
    treeRef.value.setChecked(node.parent.id, true, false);
  }

  checkParentNodes(node.parent);
};

// 处理节点勾选状态变化事件
const handleCheckChange = (data: TreeNode, checked: boolean) => {
  if (checked) {
    const currentNode = treeRef.value.getNode(data.id);

    if (currentNode && currentNode.parent) {
      checkParentNodes(currentNode);
    }

    nextTick(() => {
      ensureParentNodesChecked();
    });
  }

  const checkedKeys = treeRef.value.getCheckedKeys();
  emit('update:checkedKeys', checkedKeys);
  emit('check-change', data, checked);
};

// 重置树形结构
const resetTree = () => {
  if (!treeRef.value) return;
  treeRef.value.setCheckedKeys([]);
};

// 设置选中的节点
const setCheckedKeys = (keys: number[]) => {
  if (!treeRef.value) return;
  treeRef.value.setCheckedKeys(keys);
};

// 获取选中的节点
const getCheckedKeys = () => {
  if (!treeRef.value) return [];
  return treeRef.value.getCheckedKeys();
};

// 监听默认选中值的变化
watch(() => props.defaultCheckedKeys, () => {
  if (treeRef.value) {
    initTree();
  }
}, { deep: true });

// 监听搜索文本变化
watch(searchText, () => {
  if (treeRef.value) {
    nextTick(() => {
      if (searchText.value) {
        expandAll();
      }
    });
  }
});

// 暴露方法给父组件
defineExpose({
  resetTree,
  setCheckedKeys,
  getCheckedKeys,
  initTree,
  expandAll,
  collapseAll,
  clearSelection
});
</script>

<style scoped lang="scss">
@use '@/assets/style/variables.scss' as *;

.select-tree-container {
  background: var(--background-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  @include card-shadow;
  overflow: hidden;
  transition: all var(--transition-normal) var(--transition-timing);

  &:hover {
    box-shadow: var(--shadow-lg);
  }
}

// 树头部
.tree-header {
  @include flex-between;
  padding: var(--spacing-lg) var(--spacing-xl);
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  color: var(--text-inverse);
  border-bottom: 1px solid var(--border-light);

  .header-info {
    @include flex-start;
    gap: var(--spacing-md);

    .header-icon {
      @include flex-center;
      width: 32px;
      height: 32px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: var(--radius-md);
      font-size: var(--font-size-lg);
    }

    .header-title {
      font-size: var(--font-size-lg);
      font-weight: var(--font-weight-semibold);
    }
  }

  .header-stats {
    .stats-text {
      font-size: var(--font-size-sm);
      opacity: 0.9;

      .stats-number {
        font-weight: var(--font-weight-bold);
        color: var(--text-inverse);
        background: rgba(255, 255, 255, 0.2);
        padding: 2px var(--spacing-sm);
        border-radius: var(--radius-sm);
        margin: 0 2px;
      }
    }
  }
}

// 搜索区域
.tree-search {
  padding: var(--spacing-lg) var(--spacing-xl) var(--spacing-md);
  background: var(--background-secondary);
  border-bottom: 1px solid var(--border-light);

  .search-input {
    :deep(.el-input__wrapper) {
      border-radius: var(--radius-lg);
      box-shadow: none;
      border: 1px solid var(--border-light);
      background: var(--background-card);
      transition: all var(--transition-normal) var(--transition-timing);

      &:hover {
        border-color: var(--border-medium);
      }

      &.is-focus {
        border-color: var(--primary-color);
        box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
      }

      .el-input__prefix {
        color: var(--text-secondary);
      }
    }
  }
}

// 树内容区域
.tree-content {
  padding: var(--spacing-md);
  max-height: 400px;
  overflow-y: auto;

  .modern-tree {
    background: transparent;

    :deep(.el-tree-node) {
      margin-bottom: var(--spacing-xs);

      .el-tree-node__content {
        border-radius: var(--radius-md);
        transition: all var(--transition-normal) var(--transition-timing);
        padding: var(--spacing-sm) var(--spacing-md);
        min-height: 40px;
        background: transparent;
        border: 1px solid transparent;

        &:hover {
          background: var(--primary-lighter);
          border-color: var(--primary-color);
          @include hover-lift;
        }

        .el-tree-node__expand-icon {
          color: var(--text-secondary);
          transition: all var(--transition-normal) var(--transition-timing);

          &:hover {
            color: var(--primary-color);
          }

          &.is-leaf {
            opacity: 0;
          }
        }

        .el-checkbox {
          .el-checkbox__input {
            .el-checkbox__inner {
              border-radius: var(--radius-sm);
              border: 2px solid var(--border-medium);
              transition: all var(--transition-normal) var(--transition-timing);

              &:hover {
                border-color: var(--primary-color);
                @include hover-lift;
              }
            }

            &.is-checked .el-checkbox__inner {
              background: var(--primary-color);
              border-color: var(--primary-color);
            }

            &.is-indeterminate .el-checkbox__inner {
              background: var(--primary-light);
              border-color: var(--primary-color);

              &::before {
                background: var(--primary-color);
              }
            }
          }
        }
      }

      &.is-current > .el-tree-node__content {
        background: var(--primary-light);
        border-color: var(--primary-color);
        color: var(--primary-color);
        font-weight: var(--font-weight-medium);
      }

      &.is-checked > .el-tree-node__content {
        background: var(--primary-lighter);
        border-color: var(--primary-color);
      }
    }

    :deep(.el-tree__empty-block) {
      padding: var(--spacing-xxxl);

      .el-tree__empty-text {
        color: var(--text-secondary);

        &::before {
          content: '📁';
          display: block;
          font-size: var(--font-size-xxxl);
          margin-bottom: var(--spacing-md);
          opacity: 0.6;
        }
      }
    }
  }

  // 树节点内容
  .tree-node-content {
    @include flex-between;
    width: 100%;

    .node-label {
      flex: 1;
      font-size: var(--font-size-md);
      color: var(--text-primary);
      font-weight: var(--font-weight-medium);
    }

    .node-badge {
      background: var(--primary-color);
      color: var(--text-inverse);
      padding: 2px var(--spacing-sm);
      border-radius: var(--radius-full);
      font-size: var(--font-size-xs);
      font-weight: var(--font-weight-medium);
      min-width: 20px;
      text-align: center;
      margin-left: var(--spacing-sm);
    }
  }
}

// 树底部
.tree-footer {
  padding: var(--spacing-md) var(--spacing-xl);
  background: var(--background-secondary);
  border-top: 1px solid var(--border-light);

  .footer-actions {
    @include flex-center;
    gap: var(--spacing-sm);

    .action-btn {
      @include flex-center;
      gap: var(--spacing-xs);
      border-radius: var(--radius-md);
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-medium);
      transition: all var(--transition-normal) var(--transition-timing);

      &:hover {
        @include hover-lift;
        background: var(--primary-color);
        color: var(--text-inverse);
        border-color: var(--primary-color);
      }

      .el-icon {
        font-size: var(--font-size-sm);
      }
    }
  }
}

// 滚动条样式
.tree-content::-webkit-scrollbar {
  width: 6px;
}

.tree-content::-webkit-scrollbar-track {
  background: var(--background-secondary);
  border-radius: var(--radius-sm);
}

.tree-content::-webkit-scrollbar-thumb {
  background: var(--border-medium);
  border-radius: var(--radius-sm);

  &:hover {
    background: var(--primary-color);
  }
}

// 响应式设计
@include responsive(md) {
  .tree-header {
    flex-direction: column;
    gap: var(--spacing-md);
    align-items: flex-start;

    .header-stats {
      align-self: stretch;
      text-align: right;
    }
  }

  .tree-search {
    padding: var(--spacing-md);
  }

  .tree-footer {
    .footer-actions {
      flex-wrap: wrap;
      justify-content: center;
    }
  }
}

@include responsive(sm) {
  .tree-content {
    max-height: 300px;
  }

  .footer-actions {
    .action-btn {
      flex: 1;
      justify-content: center;
    }
  }
}

// 动画效果
@keyframes treeSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.tree-content {
  animation: treeSlideIn 0.3s var(--transition-timing);
}
</style>
