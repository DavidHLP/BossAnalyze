<template>
  <!-- 左侧树形菜单 -->
  <el-col :xs="24" :sm="isSidebarCollapsed ? 6 : 8" :md="isSidebarCollapsed ? 5 : 8" :lg="isSidebarCollapsed ? 4 : 8"
    class="sidebar-col" :class="{ 'is-collapsed': isSidebarCollapsed }">
    <el-card shadow="hover" class="tree-card">
      <template #header>
        <div class="card-header">
          <h3 class="card-title">菜单结构</h3>
          <div class="search-wrapper">
            <el-input v-model="filterText" placeholder="搜索菜单" prefix-icon="Search" clearable class="search-input" />
          </div>
        </div>
      </template>
      <EditTree ref="menuTreeRef" :tree-data="tableData as any[]" :expanded-keys="expandedKeys"
        :filter-method="filterNode" :custom-class="'menu-tree'" @node-click="handleNodeClick"
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
                <el-icon>
                  <Edit />
                </el-icon>
              </el-button>
              <el-button type="danger" link @click.stop="handleDelete(data)">
                <el-icon>
                  <Delete />
                </el-icon>
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
  // 确保传递完整的数据对象
  const completeData = JSON.parse(JSON.stringify(data));

  // 确保meta对象完整
  if (!completeData.meta) {
    completeData.meta = {
      type: 'C',
      component: '',
      redirect: null,
      alwaysShow: false,
      metaTitle: '',
      metaIcon: null,
      metaHidden: false,
      metaRoles: null,
      metaKeepAlive: false,
      hidden: false
    };
  }

  emit('edit', completeData);
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

  &.is-collapsed {
    .tree-node {
      .node-label {
        .truncated-text {
          max-width: 80px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .node-actions {
        opacity: 0;
      }

      &:hover {
        .node-actions {
          opacity: 1;
        }
      }
    }
  }
}

.tree-card {
  height: 100%;

  .card-header {
    display: flex;
    flex-direction: column;

    .card-title {
      margin-bottom: 10px;
    }
  }
}

.menu-tree {
  margin-top: 10px;

  :deep(.el-tree-node) {
    position: relative;
  }
  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    background-color: #ecf5ff;
    border-radius: 4px;
  }
  :deep(.el-tree-node__content) {
    border-radius: 4px;
    margin: 2px 0;
    transition: all 0.2s;
    height: auto !important;
    min-height: 36px;
    padding: 4px 0;
    &:hover {
      background-color: #f5f7fa;
    }
  }
}

.tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;

  .node-label {
    display: flex;
    align-items: center;
    max-width: 80%;

    .node-icon {
      margin-right: 6px;
      font-size: 16px;
    }

    .node-text {
      margin-right: 8px;
      transition: all 0.3s;
    }

    .el-tag {
      margin-left: 6px;
      font-size: 10px;
      height: 20px;
      line-height: 20px;
    }
  }

  .node-actions {
    display: flex;
    align-items: center;
    transition: opacity 0.3s;

    .el-button {
      padding: 4px;

      .el-icon {
        font-size: 14px;
      }
    }
  }
}

@media (max-width: 768px) {
  .sidebar-col {
    margin-bottom: 16px;
  }

  .tree-node {
    .node-actions {
      opacity: 1;
    }
  }
}
</style>
