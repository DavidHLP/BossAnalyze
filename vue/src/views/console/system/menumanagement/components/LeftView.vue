<template>
  <!-- 左侧菜单树 -->
  <el-col :xs="24" :sm="isSidebarCollapsed ? 6 : 8" :md="isSidebarCollapsed ? 5 : 8" :lg="isSidebarCollapsed ? 4 : 8"
    class="menu-tree-col">

    <div class="menu-tree-card">
      <!-- 卡片头部 -->
      <div class="tree-header">
        <div class="header-content">
          <div class="header-icon">
            <el-icon class="icon-menu">
              <Menu />
            </el-icon>
          </div>
          <div class="header-text">
            <h3 class="tree-title">菜单结构</h3>
            <p class="tree-subtitle">拖拽调整菜单层级</p>
          </div>
        </div>
      </div>

      <!-- 搜索区域 -->
      <div class="search-section">
        <div class="search-container">
          <el-input v-model="filterText" placeholder="搜索菜单名称、路径或权限..." prefix-icon="Search" clearable size="large"
            class="search-input">
            <template #suffix>
              <el-icon v-if="filterText" class="search-clear">
                <CircleClose />
              </el-icon>
            </template>
          </el-input>
          <div class="search-stats" v-if="filterText">
            找到 {{ filteredCount }} 个匹配项
          </div>
        </div>
      </div>

      <!-- 树形菜单内容 -->
      <div class="tree-content-wrapper">
        <div class="tree-content" ref="treeContentRef">
          <EditTree ref="menuTreeRef" :tree-data="tableData as any[]" :expanded-keys="expandedKeys"
            :filter-method="filterNode" :custom-class="'modern-menu-tree'"
            :class="{ 'is-collapsed': isSidebarCollapsed }" @node-click="handleNodeClick"
            @node-drag-end="handleDragEnd">

            <!-- 使用重构后的插槽结构 -->
            <template #node-icon="{ node, data }">
              <div class="node-icon-wrapper" :class="{ 'is-current': data.id === currentNodeId }">
                <el-icon v-if="data.meta?.metaIcon" class="node-icon" :class="getNodeIconClass(data.meta.type)">
                  <component :is="data.meta.metaIcon" />
                </el-icon>
                <el-icon v-else class="node-icon default-icon" :class="getNodeIconClass(data.meta?.type)">
                  <component :is="getDefaultIcon(data.meta?.type)" />
                </el-icon>
              </div>
            </template>

            <template #node-text="{ node, data }">
              <div class="node-text-wrapper" :class="{ 'is-current': data.id === currentNodeId }">
                <div class="node-title">{{ node.label }}</div>
                <div class="node-path" v-if="data.path">{{ data.path }}</div>
              </div>
            </template>

            <template #node-badges="{ node, data }">
              <div class="node-badges-wrapper">
                <div class="type-indicator" :class="getTypeClass(data.meta?.type)">
                  {{ getTypeText(data.meta?.type) }}
                </div>
                <div v-if="data.status === 0" class="status-indicator disabled">
                  停用
                </div>
              </div>
            </template>

            <template #node-actions="{ node, data }">
              <div class="node-actions-wrapper" :class="{ 'always-show': isSidebarCollapsed }">
                <el-tooltip content="编辑菜单" placement="top">
                  <el-button type="primary" text size="small" @click.stop="handleEdit(data)" class="action-btn edit-btn"
                    :icon="Edit">
                  </el-button>
                </el-tooltip>
                <el-tooltip content="添加子菜单" placement="top" v-if="data.meta?.type !== 'F'">
                  <el-button type="success" text size="small" @click.stop="handleAddChild(data)"
                    class="action-btn add-btn" :icon="Plus">
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除菜单" placement="top">
                  <el-button type="danger" text size="small" @click.stop="handleDelete(data)"
                    class="action-btn delete-btn" :icon="Delete">
                  </el-button>
                </el-tooltip>
              </div>
            </template>
          </EditTree>

          <!-- 空状态 -->
          <div v-if="!tableData?.length" class="tree-empty">
            <div class="empty-content">
              <el-icon class="empty-icon">
                <FolderAdd />
              </el-icon>
              <p class="empty-text">暂无菜单数据</p>
              <p class="empty-hint">添加第一个菜单开始构建系统</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-col>
</template>

<script setup lang="ts" name="LeftView">
import { ref, watch, computed } from 'vue'
import EditTree from '@/components/common/EditTree.vue'
import type { ComponentPublicInstance } from 'vue'
import type { Router } from '@/router/index.d'
import {
  Menu,
  Edit,
  Delete,
  Plus,
  FolderAdd,
  CircleClose,
  Folder,
  Document,
  Setting
} from '@element-plus/icons-vue'

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
const treeContentRef = ref<HTMLElement | null>(null)
const filterText = ref('')
const currentNodeId = ref<number | null>(null)

// 定义组件接收的props
const props = defineProps<{
  tableData: Router[];
  expandedKeys: number[];
  isSidebarCollapsed: boolean;
  currentNode?: Router | null;
}>()

// 定义组件的emit事件
const emit = defineEmits<{
  (e: 'node-click', data: Router): void;
  (e: 'node-drag-end', draggingNode: DragNode, dropNode: DragNode | null, dropType: string): void;
  (e: 'edit', data: Router): void;
  (e: 'delete', data: Router): void;
  (e: 'add-child', data: Router): void;
}>()

// 计算过滤后的数量
const filteredCount = computed(() => {
  if (!filterText.value) return 0
  let count = 0
  const countNodes = (nodes: Router[]) => {
    nodes.forEach(node => {
      if (filterNode(filterText.value, node)) {
        count++
      }
      if (node.children?.length) {
        countNodes(node.children)
      }
    })
  }
  countNodes(props.tableData)
  return count
})

// 监听当前节点变化
watch(() => props.currentNode, (newNode) => {
  currentNodeId.value = newNode?.id || null
}, { immediate: true })

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

// 获取节点类型文本
const getTypeText = (type: string) => {
  const typeMap = {
    'M': '目录',
    'C': '菜单',
    'F': '按钮'
  }
  return typeMap[type as keyof typeof typeMap] || '未知'
}

// 获取节点类型样式类
const getTypeClass = (type: string) => {
  const classMap = {
    'M': 'type-directory',
    'C': 'type-menu',
    'F': 'type-button'
  }
  return classMap[type as keyof typeof classMap] || 'type-unknown'
}

// 获取节点图标样式类
const getNodeIconClass = (type: string) => {
  const classMap = {
    'M': 'icon-directory',
    'C': 'icon-menu',
    'F': 'icon-button'
  }
  return classMap[type as keyof typeof classMap] || 'icon-default'
}

// 获取默认图标
const getDefaultIcon = (type: string) => {
  const iconMap = {
    'M': 'Folder',
    'C': 'Document',
    'F': 'Setting'
  }
  return iconMap[type as keyof typeof iconMap] || 'Document'
}

// 处理节点点击
const handleNodeClick = (data: any) => {
  currentNodeId.value = data.id
  emit('node-click', data as Router)
}

// 处理拖拽结束
const handleDragEnd = (draggingNode: any, dropNode: any, dropType: string) => {
  emit('node-drag-end', draggingNode as DragNode, dropNode as DragNode | null, dropType)
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

// 处理添加子菜单
const handleAddChild = (data: Router) => {
  emit('add-child', data)
}
</script>

<style scoped lang="scss">
@use '@/assets/style/variables.scss' as *;

.menu-tree-col {
  height: 100%;
  transition: all var(--transition-normal) var(--transition-timing);
}

// ==============================================
// 菜单树卡片
// ==============================================
.menu-tree-card {
  height: 100%;
  background: var(--background-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  @include card-shadow;
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
}

// ==============================================
// 卡片头部
// ==============================================
.tree-header {
  background: linear-gradient(135deg, var(--success-color) 0%, #059669 100%);
  color: var(--text-inverse);
  padding: var(--spacing-lg);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -30%;
    left: -20%;
    width: 150px;
    height: 150px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: var(--radius-full);
    transform: rotate(25deg);
  }

  .header-content {
    @include flex-start;
    gap: var(--spacing-md);
    position: relative;
    z-index: 1;

    .header-icon {
      width: 48px;
      height: 48px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: var(--radius-lg);
      @include flex-center;
      backdrop-filter: blur(10px);

      .icon-menu {
        font-size: var(--font-size-lg);
      }
    }

    .header-text {
      .tree-title {
        margin: 0;
        font-size: var(--font-size-lg);
        font-weight: var(--font-weight-bold);
        line-height: var(--line-height-tight);
      }

      .tree-subtitle {
        margin: var(--spacing-xs) 0 0 0;
        font-size: var(--font-size-sm);
        opacity: 0.9;
      }
    }
  }
}

// ==============================================
// 搜索区域
// ==============================================
.search-section {
  padding: var(--spacing-lg);
  background: var(--background-secondary);
  border-bottom: 1px solid var(--border-light);

  .search-container {
    .search-input {
      :deep(.el-input__wrapper) {
        border-radius: var(--radius-lg);
        border: 2px solid var(--border-light);
        background: var(--background-card);
        transition: all var(--transition-normal) var(--transition-timing);

        &:hover {
          border-color: var(--success-color);
        }

        &.is-focus {
          border-color: var(--success-color);
          box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
        }
      }

      :deep(.el-input__prefix) {
        color: var(--success-color);
      }
    }

    .search-stats {
      margin-top: var(--spacing-sm);
      font-size: var(--font-size-sm);
      color: var(--text-secondary);
      text-align: center;
    }
  }
}

// ==============================================
// 树形内容
// ==============================================
.tree-content-wrapper {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.tree-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-md);

  // 自定义滚动条
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: var(--background-secondary);
    border-radius: var(--radius-sm);
  }

  &::-webkit-scrollbar-thumb {
    background: var(--border-medium);
    border-radius: var(--radius-sm);

    &:hover {
      background: var(--success-color);
    }
  }
}

// ==============================================
// 现代树形样式
// ==============================================
.modern-menu-tree {
  :deep(.el-tree-node) {
    margin-bottom: var(--spacing-xs);

    .el-tree-node__content {
      border-radius: var(--radius-md);
      border: 1px solid transparent;
      transition: all var(--transition-normal) var(--transition-timing);
      background: transparent;

      &:hover {
        background: var(--background-secondary);
        border-color: var(--success-color);
        @include hover-lift;
      }
    }

    .el-tree-node__expand-icon {
      color: var(--success-color);
      font-size: var(--font-size-md);

      &.is-leaf {
        display: none;
      }
    }
  }
}

// ==============================================
// 自定义插槽样式
// ==============================================
.node-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  @include flex-center;
  flex-shrink: 0;
  transition: all var(--transition-normal) var(--transition-timing);

  &.is-current {
    background: var(--primary-lighter);
  }

  .node-icon {
    font-size: var(--font-size-md);
    transition: all var(--transition-normal) var(--transition-timing);

    &.icon-directory {
      color: var(--warning-color);
    }

    &.icon-menu {
      color: var(--success-color);
    }

    &.icon-button {
      color: var(--info-color);
    }

    &.default-icon {
      opacity: 0.6;
    }
  }

  &:hover {
    background: var(--primary-lighter);
    transform: scale(1.1);
  }
}

.node-text-wrapper {
  @include flex-column;
  gap: 2px;
  flex: 1;
  min-width: 0;

  &.is-current {
    .node-title {
      color: var(--primary-color);
      font-weight: var(--font-weight-semibold);
    }
  }

  .node-title {
    font-size: var(--font-size-md);
    font-weight: var(--font-weight-medium);
    color: var(--text-primary);
    @include text-ellipsis;
    line-height: var(--line-height-tight);
  }

  .node-path {
    font-size: var(--font-size-xs);
    color: var(--text-secondary);
    font-family: var(--font-family-mono);
    @include text-ellipsis;
    opacity: 0.8;
  }
}

.node-badges-wrapper {
  @include flex-start;
  flex-direction: column;
  gap: 2px;
  flex-shrink: 0;

  .type-indicator {
    padding: 2px var(--spacing-xs);
    border-radius: var(--radius-sm);
    font-size: 10px;
    font-weight: var(--font-weight-semibold);
    line-height: 1;
    text-transform: uppercase;
    letter-spacing: 0.5px;

    &.type-directory {
      background: rgba(245, 158, 11, 0.1);
      color: var(--warning-color);
    }

    &.type-menu {
      background: rgba(16, 185, 129, 0.1);
      color: var(--success-color);
    }

    &.type-button {
      background: rgba(59, 130, 246, 0.1);
      color: var(--info-color);
    }
  }

  .status-indicator {
    padding: 2px var(--spacing-xs);
    border-radius: var(--radius-sm);
    font-size: 10px;
    font-weight: var(--font-weight-semibold);
    line-height: 1;

    &.disabled {
      background: rgba(239, 68, 68, 0.1);
      color: var(--error-color);
    }
  }
}

.node-actions-wrapper {
  @include flex-start;
  gap: var(--spacing-xs);
  opacity: 0.8;
  transition: opacity var(--transition-normal) var(--transition-timing);

  &.always-show {
    opacity: 1;
  }

  .action-btn {
    padding: var(--spacing-xs);
    border-radius: var(--radius-sm);
    transition: all var(--transition-fast) var(--transition-timing);
    border: none;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;



    &.edit-btn {
      color: var(--primary-color);

      &:hover {
        background: var(--primary-lighter);
      }
    }

    &.add-btn {
      color: var(--success-color);

      &:hover {
        background: rgba(16, 185, 129, 0.1);
      }
    }

    &.delete-btn {
      color: var(--error-color);

      &:hover {
        background: rgba(239, 68, 68, 0.1);
      }
    }

    &:hover {
      @include hover-lift;
    }
  }
}

// 悬浮时显示操作按钮
.modern-menu-tree :deep(.el-tree-node__content:hover) {
  .node-actions-wrapper {
    opacity: 1;
  }
}

// 确保图标能正确显示
.modern-menu-tree :deep(.el-tree-node__content) {
  .node-actions-wrapper {
    .action-btn {
      :deep(.el-button__icon) {
        margin-left: 0;
      }
    }
  }
}

// ==============================================
// 空状态
// ==============================================
.tree-empty {
  @include flex-center;
  padding: var(--spacing-xxxl) var(--spacing-lg);

  .empty-content {
    text-align: center;

    .empty-icon {
      font-size: var(--spacing-xxxl);
      color: var(--border-medium);
      margin-bottom: var(--spacing-lg);
    }

    .empty-text {
      margin: 0 0 var(--spacing-xs) 0;
      font-size: var(--font-size-lg);
      font-weight: var(--font-weight-medium);
      color: var(--text-primary);
    }

    .empty-hint {
      margin: 0;
      font-size: var(--font-size-sm);
      color: var(--text-secondary);
    }
  }
}



// ==============================================
// 响应式适配
// ==============================================
@include responsive(lg) {
  .tree-header {
    .header-content {
      flex-direction: column;
      text-align: center;
      gap: var(--spacing-sm);
    }
  }

  .search-section {
    padding: var(--spacing-md);
  }

  .node-text-wrapper {
    .node-path {
      display: none;
    }
  }

  .node-badges-wrapper {
    flex-direction: column;
    gap: 2px;
  }
}

@include responsive(md) {
  .tree-header {
    padding: var(--spacing-md);

    .header-content {
      .header-icon {
        width: 36px;
        height: 36px;

        .icon-menu {
          font-size: var(--font-size-md);
        }
      }

      .header-text {
        .tree-title {
          font-size: var(--font-size-md);
        }
      }
    }
  }

  .modern-menu-tree {
    :deep(.el-tree-node) {
      .el-tree-node__content {
        min-height: 48px;
      }
    }
  }

  .node-actions-wrapper {
    opacity: 1;
  }
}
</style>
