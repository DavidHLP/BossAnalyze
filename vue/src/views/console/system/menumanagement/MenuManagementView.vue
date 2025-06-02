<template>
  <div class="menu-management-container">
    <!-- 顶部操作区 -->
    <div class="action-bar">
      <div class="action-left">
        <el-button type="primary" @click="handleAdd" class="action-button">
          <el-icon>
            <Plus />
          </el-icon>
          <span>添加菜单</span>
        </el-button>
        <el-button type="success" @click="expandAll" class="action-button">
          <el-icon>
            <Expand />
          </el-icon>
          <span>展开全部</span>
        </el-button>
        <el-button type="info" @click="collapseAll" class="action-button">
          <el-icon>
            <Fold />
          </el-icon>
          <span>折叠全部</span>
        </el-button>
      </div>
      <div class="action-right">
        <el-button type="primary" plain @click="toggleSidebar" class="toggle-sidebar-btn">
          <el-icon>
            <component :is="isSidebarCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>
          <span class="hidden-sm-and-down">{{ isSidebarCollapsed ? '展开侧栏' : '折叠侧栏' }}</span>
        </el-button>
      </div>
    </div>

    <!-- 主内容区 - 分割面板 -->
    <el-row :gutter="20" class="content-panel">
      <!-- 左侧树形菜单 -->
      <LeftView
        :table-data="tableData"
        :expanded-keys="expandedKeys"
        :is-sidebar-collapsed="isSidebarCollapsed"
        @node-click="handleNodeClick"
        @node-drag-end="handleDragEnd"
        @edit="handleEdit"
        @delete="handleDelete" />

      <!-- 右侧详情信息 -->
      <RightView
        :current-node="currentNode"
        :is-sidebar-collapsed="isSidebarCollapsed"
        :description-columns="descriptionColumns"
        @edit="handleEdit"
        @add-child="handleAddChild"
        @delete="handleDelete" />
    </el-row>

    <!-- 编辑对话框 - 使用独立组件 -->
    <MenuDialog
      v-model="editDialogVisible"
      :title="dialogTitle"
      :is-add-mode="isAddMode"
      :initial-form-data="formData"
      @submit="handleSubmit"
      @cancel="editDialogVisible = false"
    />

    <!-- 添加拖拽确认对话框 -->
    <el-dialog v-model="dragConfirmVisible" title="确认菜单位置修改" width="30%" :close-on-click-modal="false"
      :close-on-press-escape="false" :show-close="false" class="confirm-dialog">
      <div class="confirm-content">
        <el-icon class="warning-icon">
          <Warning />
        </el-icon>
        <span>
          确定要将菜单 "<strong>{{ dragInfo?.nodeName || '' }}</strong>"
          {{ dragInfo?.dropType === 'inner' ? '移动到' : '移动到与' }}
          "<strong>{{ dragInfo?.targetName || '' }}</strong>"
          {{ dragInfo?.dropType === 'inner' ? '内部' : '同级' }}吗？
        </span>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelDragChange">取消</el-button>
          <el-button type="primary" @click="confirmDragChange">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="MenuManagementComponents">
import { ref, computed, onMounted, onBeforeMount, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Router } from '@/router/index.d'
import { getUserRoutes, editRouter, addRouter, deleteRouter } from '@/api/router/router'
import { useRouterStore } from '@/stores/router/routerStore'
import { setupAsyncRoutes } from '@/router'
import { useWindowSize } from '@vueuse/core'
import LeftView from './components/LeftView.vue'
import MenuDialog from './components/MenuDialog.vue'
import RightView from './components/RightView.vue'

// 响应式数据
const tableData = ref<Router[]>([])
const expandedKeys = ref<number[]>([])
const editDialogVisible = ref(false)
const dialogTitle = ref('编辑菜单')
const currentNode = ref<Router | null>(null)
const isAddMode = ref(false) // 新增标识，区分添加和编辑模式
const isSidebarCollapsed = ref(false) // 侧边栏折叠状态

// 响应式窗口尺寸
const { width } = useWindowSize()

// 根据窗口宽度和侧边栏状态计算描述列表的列数
const descriptionColumns = computed(() => {
  if (width.value < 768) return 1
  if (width.value < 1200 || isSidebarCollapsed.value) return 1
  return 2
})

// 监听屏幕尺寸变化，自动调整侧边栏状态
watch(width, (newWidth) => {
  if (newWidth < 768 && !isSidebarCollapsed.value) {
    isSidebarCollapsed.value = true
  }
})

// 添加Router类型的扩展
interface RouterWithMeta {
  id?: number;
  name: string;
  path: string;
  permission: string;
  menuOrder: number;
  status: number;
  remark: string;
  parentId?: number;  // 前端使用parentId
  pid?: number;      // 后端使用pid
  children?: RouterWithMeta[];
  meta: {
    type: string;
    component: string;
    redirect: string | null;
    alwaysShow: boolean;
    metaTitle: string;
    metaIcon: string | null;
    metaHidden: boolean;
    metaRoles: string | null;
    metaKeepAlive: boolean;
    hidden: boolean;
  }
}

// 表单数据
const formData = ref<RouterWithMeta>({
  name: '',
  path: '',
  permission: '',
  menuOrder: 0,
  status: 1,
  remark: '',
  meta: {
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
  }
})

// 生命周期
onBeforeMount(() => {
  // 在小屏幕上默认折叠侧边栏
  if (width.value < 768) {
    isSidebarCollapsed.value = true
  }
})

onMounted(() => {
  loadData()
})

// 方法
const loadData = async () => {
  try {
    const result = (await getUserRoutes()) as unknown as Router[]
    tableData.value = result

    // 默认展开第一级
    if (result.length > 0) {
      // 设置默认展开的节点
      expandedKeys.value = result.map(item => item.id as number)
    }
  } catch (error) {
    console.error('数据加载失败', error)
    ElMessage.error('数据加载失败')
  }
}

// 处理节点点击
const handleNodeClick = (data: Router) => {
  currentNode.value = data

  // 在移动设备上点击节点后自动折叠侧边栏，提供更大的详情显示空间
  if (width.value < 768) {
    isSidebarCollapsed.value = true
  }
}

// 展开所有节点
const expandAll = () => {
  // 递归获取所有节点ID
  const getAllIds = (nodes: Router[]): number[] => {
    let ids: number[] = []
    nodes.forEach(node => {
      if (node.id) ids.push(node.id as number)
      if (node.children && node.children.length > 0) {
        ids = [...ids, ...getAllIds(node.children)]
      }
    })
    return ids
  }

  expandedKeys.value = getAllIds(tableData.value)
}

// 折叠所有节点
const collapseAll = () => {
  expandedKeys.value = []
}

// 添加拖拽确认相关的状态变量
const dragConfirmVisible = ref(false)
const dragPendingChanges = ref<Router | null>(null)
const dragInfo = ref<{
  nodeName: string;
  targetName: string;
  dropType: string;
}>({
  nodeName: '',
  targetName: '',
  dropType: ''
})

interface DragNode {
  data: Router;
  key: number;
  label: string;
}

// 拖拽结束处理
const handleDragEnd = async (draggingNode: DragNode, dropNode: DragNode | null, dropType: string) => {

  // 如果没有目标节点，说明是无效的拖拽
  if (!dropNode) return;

  try {
    // 深拷贝拖拽节点数据，避免直接修改原引用
    const draggingData = JSON.parse(JSON.stringify(draggingNode.data));

    // 获取父节点ID - 确保使用原始ID
    let newParentId: number | null = null;
    let parentNodeName: string = '根节点';

    // 确定新的父节点ID
    if (dropType === 'inner') {
      // 情况1: 拖拽到节点内部，成为其子节点
      newParentId = dropNode.data.id;
      parentNodeName = dropNode.data.name;
    } else if (dropType === 'before' || dropType === 'after') {
      // 情况2: 拖拽到节点前面或后面，成为兄弟节点 - 共享同一个父节点
      newParentId = dropNode.data.pid || null;

      // 如果目标节点有父节点，尝试查找父节点名称
      if (newParentId) {
        const parentNode = findNodeById(tableData.value, newParentId);
        if (parentNode) {
          parentNodeName = parentNode.name;
        }
      }
    }

    // 保存拖拽操作的信息，用于确认对话框
    dragInfo.value = {
      nodeName: draggingData.name,
      targetName: dropType === 'inner' ? dropNode.data.name : parentNodeName,
      dropType: dropType
    };

    // 明确设置新的父节点ID
    draggingData.pid = newParentId;
    draggingData.parentId = newParentId; // 前端结构使用

    // 处理可能的空字符串permission
    if (draggingData.permission === '') {
      draggingData.permission = null;
    }

    // 存储待确认的变更，而不是直接应用
    dragPendingChanges.value = draggingData;

    // 显示确认对话框
    dragConfirmVisible.value = true;

  } catch (error) {
    console.error('处理拖拽数据失败', error);
    ElMessage.error('处理拖拽数据失败');
    // 重新加载数据以恢复原始状态
    await loadData();
  }
}

// 确认拖拽更改
const confirmDragChange = async () => {
  try {
    if (!dragPendingChanges.value) {
      ElMessage.warning('没有待处理的更改');
      dragConfirmVisible.value = false;
      return;
    }

    // 应用变更 - 调用API更新节点数据
    await editRouter(dragPendingChanges.value as unknown as Router);

    // 清除路由缓存并重新加载路由
    localStorage.removeItem('cachedRoutes')
    const routerStore = useRouterStore()
    routerStore.setRoutes([]) // 清空当前路由
    await setupAsyncRoutes() // 重新设置路由

    // 重新加载数据以确保树结构正确
    await loadData();

    ElMessage.success('菜单位置已更新');
    dragConfirmVisible.value = false;
    dragPendingChanges.value = null;
  } catch (error) {
    console.error('更新菜单位置失败', error);
    ElMessage.error('更新菜单位置失败');
    // 重新加载数据以恢复原始状态
    await loadData();
    dragConfirmVisible.value = false;
    dragPendingChanges.value = null;
  }
}

// 取消拖拽更改
const cancelDragChange = async () => {
  dragConfirmVisible.value = false;
  dragPendingChanges.value = null;
  // 重新加载数据以恢复原始状态
  await loadData();
  ElMessage.info('已取消菜单位置修改');
}

// 根据ID查找节点的辅助函数
const findNodeById = (nodes: Router[], id: number): Router | null => {
  for (const node of nodes) {
    if (node.id === id) {
      return node;
    }

    if (node.children && node.children.length > 0) {
      const foundNode = findNodeById(node.children, id);
      if (foundNode) {
        return foundNode;
      }
    }
  }

  return null;
}

// 切换侧边栏折叠状态
const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

// 添加菜单
const handleAdd = () => {
  dialogTitle.value = '添加菜单'
  isAddMode.value = true // 设置为添加模式
  formData.value = {
    name: '',
    path: '',
    permission: '',
    menuOrder: 0,
    status: 1,
    remark: '',
    meta: {
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
    }
  }
  editDialogVisible.value = true
}

// 添加子菜单
const handleAddChild = (row: Router) => {
  dialogTitle.value = '添加子菜单'
  isAddMode.value = true // 设置为添加模式
  formData.value = {
    name: '',
    path: '',
    permission: '',
    menuOrder: 0,
    status: 1,
    remark: '',
    parentId: row.id,  // 设置父ID
    meta: {
      type: row.meta?.type === 'M' ? 'C' : 'F',  // 如果父节点是目录，子节点默认为菜单
      component: '',
      redirect: null,
      alwaysShow: false,
      metaTitle: '',
      metaIcon: null,
      metaHidden: false,
      metaRoles: null,
      metaKeepAlive: false,
      hidden: false
    }
  }
  editDialogVisible.value = true
}

// 编辑菜单
const handleEdit = (row: Router) => {
  dialogTitle.value = '编辑菜单'
  isAddMode.value = false // 设置为编辑模式
  // 深拷贝防止直接修改原对象
  const rowCopy = JSON.parse(JSON.stringify(row)) as unknown as RouterWithMeta
  // 确保meta对象存在
  if (!rowCopy.meta) {
    rowCopy.meta = {
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
    }
  }
  formData.value = rowCopy
  editDialogVisible.value = true
}

// 删除菜单
const handleDelete = (row: Router) => {
  ElMessageBox.confirm(
    `确定要删除菜单 "${row.name}" 吗？${row.children?.length ? '此操作将同时删除所有子菜单！' : ''}`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 调用真实API删除
      await deleteRouter(row)
      ElMessage.success('删除成功')

      // 清除路由缓存并重新加载路由
      localStorage.removeItem('cachedRoutes')
      const routerStore = useRouterStore()
      routerStore.setRoutes([]) // 清空当前路由
      await setupAsyncRoutes() // 重新设置路由

      // 刷新数据
      loadData()
      // 如果删除的是当前选中节点，清空选择
      if (currentNode.value && currentNode.value.id === row.id) {
        currentNode.value = null
      }
    } catch (error) {
      console.error('删除失败', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 用户取消删除
  })
}

// 处理表单提交 (由MenuDialog组件触发)
const handleSubmit = async (data: RouterWithMeta, isAdd: boolean) => {
  try {
    // 处理数据格式，确保与后端兼容
    const submittingData = { ...data }

    // 转换parentId为pid格式
    if (submittingData.parentId) {
      submittingData.pid = submittingData.parentId
      delete submittingData.parentId
    }

    if (isAdd) {
      // 添加新菜单
      await addRouter(submittingData as unknown as Router)
      ElMessage.success('添加成功')
    } else {
      // 编辑现有菜单
      await editRouter(submittingData as unknown as Router)
      ElMessage.success('修改成功')

      // 如果是编辑当前选中的节点，更新当前节点详情
      if (currentNode.value && currentNode.value.id === submittingData.id) {
        currentNode.value = JSON.parse(JSON.stringify(submittingData))
      }
    }

    editDialogVisible.value = false

    // 清除路由缓存并重新加载路由
    localStorage.removeItem('cachedRoutes')
    const routerStore = useRouterStore()
    routerStore.setRoutes([]) // 清空当前路由
    await setupAsyncRoutes() // 重新设置路由

    loadData() // 刷新数据
  } catch (error) {
    console.error(isAdd ? '添加失败' : '修改失败', error)
    ElMessage.error(isAdd ? '添加失败' : '修改失败')
  }
}
</script>

<style scoped lang="scss">
.menu-management-container {
  padding: var(--spacing-xl);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.action-bar {
  margin-bottom: var(--spacing-xl);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--spacing-md);

  .action-left,
  .action-right {
    display: flex;
    gap: var(--spacing-md);
    flex-wrap: wrap;
  }

  .action-button,
  .toggle-sidebar-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    border-radius: var(--border-radius-md);
    transition: transform 0.2s ease;

    &:hover {
      transform: translateY(-2px);
    }
  }
}

.content-panel {
  flex: 1;
  overflow: hidden;
  margin-bottom: var(--spacing-xl);
  min-height: 0; // 修复Flex布局下的溢出问题
}

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

.content-col {
  height: 100%;
  transition: all 0.3s ease;
}

.tree-card,
.detail-card {
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

.details-container {
  height: 100%;
  display: flex;
  flex-direction: column;

  .details-info {
    margin-bottom: var(--spacing-lg);
  }

  .icon-preview {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .status-tag,
  .feature-tag {
    font-weight: 500;
  }
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  margin-top: auto;
  padding-top: var(--spacing-lg);

  .detail-button {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    border-radius: var(--border-radius-md);
    transition: all 0.2s ease;
    margin-bottom: var(--spacing-sm);

    &:hover {
      transform: translateY(-2px);
    }
  }
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-dialog {
  .menu-form {
    padding: var(--spacing-md) 0;
  }

  .number-input,
  .type-select {
    width: 100%;
  }

  .switch-item {
    display: flex;
    align-items: center;
  }

  .textarea {
    border-radius: var(--border-radius-md);
  }
}

.icon-selector {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);

  .icon-search {
    padding-bottom: var(--spacing-sm);
    border-bottom: 1px solid var(--border-color);
  }

  .icon-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(70px, 1fr));
    gap: var(--spacing-md);
    padding: var(--spacing-sm);

    .icon-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      padding: 8px;
      border-radius: var(--border-radius-md);
      cursor: pointer;
      transition: all 0.2s ease;

      .el-icon {
        font-size: 20px;
      }

      .icon-name {
        font-size: var(--font-size-sm);
        text-align: center;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 100%;
      }

      &:hover {
        background-color: var(--primary-light);
        color: var(--primary-color);
      }

      &.icon-selected {
        background-color: var(--primary-color);
        color: white;
      }
    }
  }

  .icon-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: var(--spacing-sm);
    border-top: 1px solid var(--border-color);

    .selected-icon {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}

.confirm-dialog {
  .confirm-content {
    display: flex;
    align-items: flex-start;
    gap: var(--spacing-md);
    padding: var(--spacing-md) 0;

    .warning-icon {
      font-size: 24px;
      color: var(--warning-color);
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-md);
}

// 响应式样式
@media (max-width: 768px) {
  .menu-management-container {
    padding: var(--spacing-md);
  }

  .action-bar {
    flex-direction: column;
    align-items: stretch;

    .action-left,
    .action-right {
      justify-content: space-between;
    }
  }

  .sidebar-col,
  .content-col {
    margin-bottom: var(--spacing-md);
  }

  .content-panel {
    height: auto;
    min-height: calc(100vh - 150px);
  }

  .hidden-sm-and-down {
    display: none;
  }
}
</style>
