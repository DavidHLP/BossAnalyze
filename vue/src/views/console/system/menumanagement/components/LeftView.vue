<template>
  <!-- 左侧树形菜单 -->
  <el-col :xs="24" :sm="isSidebarCollapsed ? 6 : 8" :md="isSidebarCollapsed ? 5 : 8"
    :lg="isSidebarCollapsed ? 4 : 8" class="sidebar-col" :class="{ 'is-collapsed': isSidebarCollapsed }">
    <el-card shadow="hover" class="tree-card">
      <template #header>
        <div class="card-header">
          <h3 class="card-title">菜单结构</h3>
          <div class="search-wrapper">
            <el-input v-model="filterText" placeholder="搜索菜单" prefix-icon="Search" clearable class="search-input" />
          </div>
        </div>
      </template>
      <EditTree
        ref="menuTreeRef"
        :tree-data="tableData as any[]"
        :expanded-keys="expandedKeys"
        :filter-method="filterNode"
        :custom-class="'menu-tree'"
        @node-click="handleNodeClick"
        @node-drag-end="handleDragEnd">

        <!-- 自定义节点 -->
        <template #node="{ node, data }">
          <div class="tree-node">
            <div class="node-label">
              <el-icon v-if="data.meta?.metaIcon" class="node-icon">
                <component :is="data.meta.metaIcon" />
              </el-icon>
              <span class="node-text" :class="{ 'truncated-text': isSidebarCollapsed }">{{ node.label }}</span>
              <el-tag v-if="data.meta?.type" size="small" :type="tagType(data.meta.type)">
                {{ ({ M: '目录', C: '菜单', F: '按钮' } as Record<string, string>)[data.meta.type] || '-' }}
              </el-tag>
            </div>
            <div class="node-actions">
              <el-button type="primary" link @click.stop="handleEdit(data)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button type="danger" link @click.stop="handleDelete(data)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </template>
      </EditTree>
    </el-card>
  </el-col>
</template>

<script setup lang="ts" name="LeftView">
import { ref, watch } from 'vue'
import EditTree from '@/components/common/EditTree.vue'
import type { ComponentPublicInstance } from 'vue'
import type { Router } from '@/router/index.d'

// 定义EditTree组件实例类型
interface EditTreeInstance extends ComponentPublicInstance {
  filter: (val: string) => void;
}

// 定义树节点的拖拽类型
interface DragNode {
  data: Router;
  key: number;
  label: string;
}

const menuTreeRef = ref<EditTreeInstance | null>(null)
const filterText = ref('')

// 定义组件接收的props
defineProps<{
  tableData: Router[];
  expandedKeys: number[];
  isSidebarCollapsed: boolean;
}>()

// 定义组件的emit事件
const emit = defineEmits<{
  (e: 'node-click', data: Router): void;
  (e: 'node-drag-end', draggingNode: DragNode, dropNode: DragNode | null, dropType: string): void;
  (e: 'edit', data: Router): void;
  (e: 'delete', data: Router): void;
}>()

// 监听搜索框输入变化
watch(filterText, (val) => {
  if (menuTreeRef.value) {
    menuTreeRef.value.filter(val)
  }
})

// 树节点筛选方法
const filterNode = (value: string, data: Router): boolean => {
  if (!value) return true
  return data.name.includes(value) ||
    data.path?.includes(value) ||
    String(data.permission || '').includes(value) ||
    data.meta?.component?.includes(value) || false
}

// 处理节点点击
const handleNodeClick = (data: Router) => {
  emit('node-click', data)
}

// 处理拖拽结束
const handleDragEnd = (draggingNode: DragNode, dropNode: DragNode | null, dropType: string) => {
  emit('node-drag-end', draggingNode, dropNode, dropType)
}

// 处理编辑
const handleEdit = (data: Router) => {
  emit('edit', data)
}

// 处理删除
const handleDelete = (data: Router) => {
  emit('delete', data)
}

// 根据菜单类型返回不同标签样式
const tagType = (type: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const typeMap: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    'M': 'info',
    'C': 'success',
    'F': 'warning'
  }
  return typeMap[type] || 'info'
}
</script>

<style scoped lang="scss">
.sidebar-col {
  transition: all 0.3s ease;
  height: 100%;

  &.is-collapsed {
    .tree-card {
      .card-header {
        .search-wrapper {
          .search-input {
            .el-input__wrapper {
              padding-left: 8px;
              padding-right: 8px;
            }
          }
        }
      }
    }

    .tree-node {
      .node-actions {
        flex-direction: column;
      }
    }
  }
}

.tree-card {
  height: 100%;
  border-radius: var(--border-radius-lg);
  box-shadow: var(--box-shadow);
  transition: all 0.3s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  &:hover {
    box-shadow: var(--box-shadow-lg);
  }
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) 0;

  .card-title {
    margin: 0;
    font-size: var(--font-size-lg);
    font-weight: 600;
    color: var(--text-primary);
  }

  .search-wrapper {
    margin-top: var(--spacing-sm);

    .search-input {
      border-radius: var(--border-radius-md);
    }
  }
}

.menu-tree {
  flex: 1;
  overflow: auto;
}

.tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;

  .node-label {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0; // 防止子元素溢出

    .node-icon {
      color: var(--primary-color);
      font-size: 16px;
      flex-shrink: 0;
    }

    .node-text {
      font-size: var(--font-size-md, 14px);
      color: var(--text-primary);
      flex: 1;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;

      &.truncated-text {
        max-width: 60px;
      }
    }
  }

  .node-actions {
    display: flex;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.3s ease;
    flex-shrink: 0;
  }

  &:hover .node-actions {
    opacity: 1;
  }
}
</style>
