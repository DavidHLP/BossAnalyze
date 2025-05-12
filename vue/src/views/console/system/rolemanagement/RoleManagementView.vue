<template>
  <div class="role-management-container">
    <el-card shadow="hover" class="custom-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">角色列表</span>
          <div class="header-buttons">
            <el-button plain size="small" icon="Document" @click="showPermissionGuide" class="guide-button">权限说明</el-button>
            <el-button type="primary" size="small" icon="Plus" @click="handleAddRole" class="add-button">添加角色</el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="roleList"
        v-loading="loading"
        :row-class-name="tableRowClassName"
        border
        :highlight-current-row="false"
        class="custom-table"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="roleName" label="角色名称" width="140" />
        <el-table-column prop="remark" label="描述" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.status === 1 ? 'success' : 'danger'" class="status-tag">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-tooltip content="编辑角色" placement="top">
                <el-button size="small" text circle @click="handleEdit(scope.row)" class="action-button">
                  <el-icon color="#409efc" ><Edit /></el-icon>
                </el-button>
              </el-tooltip>

              <el-tooltip content="设置权限" placement="top">
                <el-button size="small" text circle @click="handlePermissions(scope.row)" class="action-button">
                  <el-icon color="#409efc" ><Setting /></el-icon>
                </el-button>
              </el-tooltip>

              <el-tooltip content="删除角色" placement="top">
                <el-button
                  size="small"
                  type="danger"
                  text
                  circle
                  @click="handleDelete(scope.row)"
                  :disabled="scope.row.roleName === 'ADMIN' || scope.row.roleName === 'USER'"
                  class="action-button"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑角色' : '添加角色'"
      width="480px"
      destroy-on-close
      top="10vh"
      class="custom-dialog"
    >
      <el-form :model="currentRole" label-width="80px" ref="roleFormRef" class="custom-form">
        <el-form-item label="角色名称" prop="roleName" :rules="[{ required: true, message: '请输入角色名称', trigger: 'blur' }]">
          <el-input v-model="currentRole.roleName" placeholder="请输入角色名称" :disabled="isEdit && (currentRole.roleName === 'ADMIN' || currentRole.roleName === 'USER')" />
        </el-form-item>
        <el-form-item label="描述" prop="remark">
          <el-input v-model="currentRole.remark" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="currentRole.status" :active-value="1" :inactive-value="0" class="custom-switch" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" class="cancel-button">取消</el-button>
          <el-button type="primary" @click="isEdit ? handleEditSubmit() : handleAddSubmit()" class="confirm-button">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="permDialogVisible"
      title="权限设置"
      width="720px"
      :before-close="handleClosePermDialog"
      destroy-on-close
      top="5vh"
      class="custom-dialog"
    >
      <div v-if="currentRole.roleName" class="role-tag-container">
        <span>当前角色: </span>
        <el-tag size="small" effect="plain" class="role-tag">{{ currentRole.roleName }}</el-tag>
      </div>

      <div class="perm-alert-container">
        <el-alert type="info" :closable="false" class="custom-alert">
          <p>通过选择菜单来分配权限，系统将自动关联对应的操作权限。</p>
          <p>勾选子菜单时，父菜单会自动被选中，但勾选父菜单不会自动选中子菜单。</p>
        </el-alert>
      </div>

      <div class="menu-tree-container">
        <el-tree
          ref="menuTreeRef"
          :data="routersTree"
          show-checkbox
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          :default-checked-keys="defaultCheckedMenus"
          :check-strictly="true"
          :default-expand-all="false"
          :expand-on-click-node="false"
          highlight-current
          @check-change="handleCheckChange"
          class="custom-tree"
        />
      </div>

      <template #footer>
        <div class="dialog-footer-between">
          <div>
            <el-popconfirm
              title="检测到数据已修改，确定要关闭吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="permDialogVisible = false"
              v-if="checkForUnsavedChanges()"
            >
              <template #reference>
                <el-button class="cancel-button">取消</el-button>
              </template>
            </el-popconfirm>
            <el-button v-else @click="permDialogVisible = false" class="cancel-button">取消</el-button>
          </div>
          <div>
            <el-button plain icon="RefreshRight" @click="resetPermissions" class="reset-button">重置</el-button>
            <el-button type="primary" icon="Check" @click="savePermissions" class="save-button">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <Permissionsdocumentation v-model:dialogVisible="guideDialogVisible" />
  </div>
</template>

<script setup lang="ts" name="RoleManagementComponent">
import { ref, onMounted, watch, nextTick } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getRoleList, addRole, editRole, deleteRole, updateRoleRouters } from '@/api/auth/auth';
import { getUserRoutes } from '@/api/router/router';
import type { Router } from '@/router/index.d';
import type { Role } from '@/api/auth/auth.d';
import Permissionsdocumentation from './components/PermissionsDocumentation.vue';

interface TreeNode {
  id: number;
  name: string;
  children?: TreeNode[];
  parent?: TreeNode;
  level?: number;
}

// 状态变量
const roleList = ref<Role[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const permDialogVisible = ref(false);
const isEdit = ref(false);
const currentRole = ref<Role>({
  id: 0,
  roleName: '',
  status: 1,
  remark: '',
  permissions: [],
  routers: []
});
const routersTree = ref<Router[]>([]);
const defaultCheckedMenus = ref<number[]>([]);
const roleFormRef = ref();
const menuTreeRef = ref();
const guideDialogVisible = ref(false);
const currentRoleId = ref<number | null>(null);


// 获取角色列表
const fetchRoleList = async () => {
  loading.value = true;
  try {
    const res = await getRoleList();
    roleList.value = res || [];
  } catch (error) {
    console.error('获取角色列表失败:', error);
    ElMessage.error('获取角色列表失败');
  } finally {
    loading.value = false;
  }
};

// 获取路由树
const fetchRoutersTree = async () => {
  try {
    const res = await getUserRoutes();
    routersTree.value = res || [];
  } catch (error) {
    console.error('获取路由树失败:', error);
    ElMessage.error('获取路由树失败');
  }
};

// 添加角色
const handleAddRole = () => {
  isEdit.value = false;
  currentRole.value = {
    id: 0,
    roleName: '',
    status: 1,
    remark: '',
    permissions: [],
    routers: []
  };
  dialogVisible.value = true;
};

// 编辑角色
const handleEdit = (role: Role) => {
  isEdit.value = true;
  currentRole.value = { ...role };
  dialogVisible.value = true;
};

// 添加角色表单提交处理
const handleAddSubmit = async () => {
  roleFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;

    try {
      await addNewRole();
      dialogVisible.value = false;
      fetchRoleList();
    } catch (error) {
      console.error('添加角色失败:', error);
      ElMessage.error('添加角色失败');
    }
  });
};

// 编辑角色表单提交处理
const handleEditSubmit = async () => {
  roleFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return;

    try {
      await updateExistingRole();
      dialogVisible.value = false;
      fetchRoleList();
    } catch (error) {
      console.error('更新角色失败:', error);
      ElMessage.error('更新角色失败');
    }
  });
};

// 添加新角色 (实际API调用)
const addNewRole = async () => {
  try {
    await addRole(currentRole.value);
    ElMessage.success('角色添加成功');
  } catch (error) {
    console.error('添加角色失败:', error);
    ElMessage.error('添加角色失败');
    throw error; // 向上传递错误
  }
};

// 更新现有角色 (实际API调用)
const updateExistingRole = async () => {
  try {
    await editRole(currentRole.value);
    ElMessage.success('角色更新成功');
  } catch (error) {
    console.error('更新角色失败:', error);
    ElMessage.error('更新角色失败');
    throw error; // 向上传递错误
  }
};

// 删除角色
const handleDelete = (role: Role) => {
  if (!role.id) {
    ElMessage.warning('角色ID不存在');
    return;
  }

  ElMessageBox.confirm(`确定要删除角色"${role.roleName}"吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteRole(role.id as number);
      ElMessage.success('角色删除成功');
      fetchRoleList();
    } catch (error) {
      console.error('删除角色失败:', error);
      ElMessage.error('删除角色失败');
    }
  }).catch(() => {});
};

// 打开权限设置
const handlePermissions = async (role: Role) => {
  if (!role.id) {
    ElMessage.warning('角色ID不存在');
    return;
  }

  // 记录当前正在操作的角色ID
  currentRoleId.value = role.id;

  try {
    loading.value = true;
    // 加载角色权限数据
    await loadRolePermissionsData(role.id);

    // 先显示对话框
    permDialogVisible.value = true;

    // 使用nextTick确保树控件已渲染
    nextTick(() => {
      if (menuTreeRef.value) {
        initPermissionTree();
      }
    });
  } catch (error) {
    console.error('获取角色权限数据失败:', error);
    ElMessage.error('获取角色权限数据失败');
  } finally {
    loading.value = false;
  }
};

// 加载角色权限数据
const loadRolePermissionsData = async (roleId: number) => {
  // 先获取最新的角色数据
  await fetchRoleList();

  // 从刷新后的列表中找到当前角色
  const freshRole = roleList.value.find(r => r.id === roleId);
  if (!freshRole) {
    ElMessage.warning('未找到角色数据');
    throw new Error('未找到角色数据');
  }

  currentRole.value = { ...freshRole };

  // 设置选中的菜单
  defaultCheckedMenus.value = [];
  if (freshRole.routers) {
    defaultCheckedMenus.value = freshRole.routers
      .filter(r => r.id !== undefined)
      .map(r => r.id);
  }
};

// 初始化权限树选中状态
const initPermissionTree = () => {
  menuTreeRef.value.setCheckedKeys([]);
  menuTreeRef.value.setCheckedKeys(defaultCheckedMenus.value);

  // 确保所有选中节点的父节点也被选中
  nextTick(() => {
    // 获取树的所有节点数据
    const allNodes = getAllTreeNodes(routersTree.value);

    // 找到所有已选中的节点
    const checkedNodes = allNodes.filter(node =>
      defaultCheckedMenus.value.includes(node.id)
    );

    // 为每个选中的节点处理其父节点选中状态
    checkedNodes.forEach(node => {
      // 找到节点在树中的完整引用（包含parent属性）
      const treeNode = menuTreeRef.value.getNode(node.id);
      if (treeNode) {
        checkParentNodes(treeNode);
      }
    });

    // 确保已选中节点的视图更新
    nextTick(() => {
      ensureParentNodesChecked();
    });
  });
};

// 确保所有已选中节点的父节点都被选中
const ensureParentNodesChecked = () => {
  // 获取当前所有选中的节点
  const checkedNodes = menuTreeRef.value.getCheckedNodes();

  // 对每个选中的节点，确保其父节点也被选中
  checkedNodes.forEach((checkedNode: TreeNode) => {
    // 从树中获取完整的节点引用，包含父节点信息
    const treeNode = menuTreeRef.value.getNode(checkedNode.id);
    if (treeNode && treeNode.parent) {
      // 递归勾选父节点
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

// 重置权限设置为原始状态
const resetPermissions = async () => {
  if (!currentRole.value.id) {
    ElMessage.warning('角色ID不存在');
    return;
  }

  try {
    // 重新加载当前角色的权限数据
    await reloadRolePermissions();

    ElMessage.success('权限设置已重置');
  } catch (error) {
    console.error('重置权限设置失败:', error);
    ElMessage.error('重置权限设置失败');
  }
};

// 重新加载当前角色的权限数据
const reloadRolePermissions = async () => {
  await fetchRoleList();

  // 从刷新后的列表中找到当前角色
  const freshRole = roleList.value.find(r => r.id === currentRole.value.id);
  if (!freshRole) {
    ElMessage.warning('未找到角色数据');
    throw new Error('未找到角色数据');
  }

  // 更新当前角色为最新数据
  currentRole.value = { ...freshRole };

  // 重置选中的菜单
  defaultCheckedMenus.value = [];
  if (freshRole.routers) {
    defaultCheckedMenus.value = freshRole.routers
      .filter(r => r.id !== undefined)
      .map(r => r.id);
  }

  // 如果树已经渲染，需要重新设置选中状态
  if (menuTreeRef.value) {
    initPermissionTree();
  }
};

// 保存权限设置
const savePermissions = async () => {
  if (!currentRole.value.id) {
    ElMessage.warning('角色ID不存在');
    return;
  }

  try {
    // 获取并保存权限数据
    await saveRolePermissionsData();

    ElMessage.success('权限设置保存成功');
    permDialogVisible.value = false;
    fetchRoleList();
  } catch (error) {
    console.error('保存权限失败:', error);
    ElMessage.error('保存权限失败');
  }
};

// 获取并保存权限数据
const saveRolePermissionsData = async () => {
  // 获取树选中的节点ID（包括父节点和子节点）
  const checkedMenuIds = menuTreeRef.value?.getCheckedKeys() || [];

  console.log(checkedMenuIds);
  console.log(currentRole.value.id);

  // 保存路由权限（路由中已包含对应的操作权限）
  await updateRoleRouters({
    roleId: currentRole.value.id as number,
    routerIds: checkedMenuIds
  });
};

// 处理节点勾选状态变化事件
const handleCheckChange = (
  data: TreeNode,
  checked: boolean
) => {
  if (checked) {
    // 当节点被勾选时，获取节点的完整引用
    const currentNode = menuTreeRef.value.getNode(data.id);

    // 如果节点存在并且有父节点，勾选所有父节点
    if (currentNode && currentNode.parent) {
      checkParentNodes(currentNode);
    }

    // 使用nextTick确保DOM更新后，状态完全同步
    nextTick(() => {
      ensureParentNodesChecked();
    });
  }
};

// 递归勾选所有父节点
const checkParentNodes = (node: TreeNode) => {
  // 如果节点不存在、没有父节点或父节点没有ID，则返回
  if (!node || !node.parent || node.parent.id === undefined) return;

  // 如果是根节点或父节点没有ID，则返回
  if (node.parent.level === 0) return;

  // 检查父节点是否已勾选，避免不必要的操作
  if (!menuTreeRef.value.getCheckedKeys().includes(node.parent.id)) {
    // 勾选父节点，但不级联子节点
    menuTreeRef.value.setChecked(node.parent.id, true, false);
  }

  // 递归处理更上层的父节点
  checkParentNodes(node.parent);
};

// 显示权限说明文档
const showPermissionGuide = () => {
  guideDialogVisible.value = true;
};

// 处理对话框关闭前的确认
const handleClosePermDialog = (done?: () => void) => {
  if (checkForUnsavedChanges()) {
    showUnsavedChangesConfirmation(() => closePermissionDialog(done));
  } else {
    closePermissionDialog(done);
  }
};

// 显示未保存更改确认对话框
const showUnsavedChangesConfirmation = (onConfirm: () => void) => {
  ElMessageBox.confirm('关闭对话框将丢失未保存的修改，确定要关闭吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    onConfirm();
  }).catch(() => {
    // 用户取消关闭，不做任何操作
  });
};

// 关闭权限设置对话框
const closePermissionDialog = (done?: () => void) => {
  if (done) {
    done();
  } else {
    permDialogVisible.value = false;
  }
};

// 检查是否有未保存的修改
const checkForUnsavedChanges = () => {
  // 获取原始路由和当前选中的路由
  const originalRouterIds = getOriginalRouterIds();
  const currentRouterIds = getCurrentRouterIds();

  // 比较路由数量
  if (currentRouterIds.length !== originalRouterIds.length) {
    return true;
  }

  // 检查是否有新增的路由
  const routersDiff = currentRouterIds.filter((id: number) => !originalRouterIds.includes(id));
  return routersDiff.length > 0;
};

// 获取原始路由ID列表
const getOriginalRouterIds = () => {
  // 从当前角色列表中找到当前正在编辑的角色
  const originalRole = roleList.value.find(r => r.id === currentRole.value.id);
  if (!originalRole) return [];
  // 返回原始路由ID
  return originalRole.routers
    ?.filter(r => r.id !== undefined)
    .map(r => r.id) || [];
};

// 获取当前选中的路由ID列表
const getCurrentRouterIds = () => {
  return menuTreeRef.value?.getCheckedKeys() || [];
};

// 表格行样式
const tableRowClassName = ({ row }: { row: Role }) => {
  // 仅当有操作并且与当前高亮ID匹配时才添加高亮样式
  return row.id === currentRoleId.value && currentRoleId.value !== null ? 'highlight-row' : '';
};

// 监听权限设置对话框的关闭事件
watch(() => permDialogVisible.value, (val) => {
  // 当对话框关闭时
  if (!val) {
    cleanupPermissionDialog();
  }
});

// 清理权限设置对话框资源
const cleanupPermissionDialog = () => {
  // 清空选中的菜单
  defaultCheckedMenus.value = [];

  // 如果树已经渲染，需要清空树的选中状态
  if (menuTreeRef.value) {
    menuTreeRef.value.setCheckedKeys([]);
  }

  // 重新获取角色列表，并根据记住的角色ID高亮显示对应行
  fetchRoleList();

  // 清空当前角色对象
  resetCurrentRole();
};

// 重置当前角色对象
const resetCurrentRole = () => {
  currentRole.value = {
    id: 0,
    roleName: '',
    status: 1,
    remark: '',
    permissions: [],
    routers: []
  };
  currentRoleId.value = null;
};

onMounted(() => {
  fetchRoleList();
  fetchRoutersTree();
});
</script>

<style scoped lang="scss">
// 添加变量定义

.role-management-container {
  padding: var(--content-padding);
  font-family: 'Inter', 'PingFang SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;

  // 卡片样式
  .custom-card {
    background-color: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
    border: none;
    overflow: hidden;

    &:hover {
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
    }
  }

  // 表格样式
  .custom-table {
    .el-table__header th {
      background-color: #f8fafe;
      color: var(--text-primary);
      font-weight: 600;
      padding: 12px 0;
    }

    .el-table__row {
      transition: all 0.3s ease;

      &:hover {
        background-color: #f0f7ff;
      }
    }
  }

  // 圆角输入框
  .el-input__wrapper, .el-textarea__wrapper, .el-select .el-input__wrapper {
    border-radius: 20px !important;
  }

  // 对话框样式
  .custom-dialog {
    .el-dialog__header {
      background-color: var(--primary-color, #3B82F6);
      color: #fff;
      padding: 20px;
      border-radius: 12px 12px 0 0;

      .el-dialog__title {
        color: #fff;
        font-weight: 600;
      }
    }

    .el-dialog__body {
      padding: 24px;
    }

    .el-dialog__headerbtn .el-dialog__close {
      color: #fff;
    }
  }

  // 按钮样式
  .add-button, .confirm-button, .save-button {
    border-radius: 20px;
  }

  .cancel-button, .reset-button, .guide-button {
    border-radius: 20px;
  }

  // 状态标签样式
  .status-tag {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;

    &:before {
      content: "";
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: var(--error-color, #F44336);
      display: inline-block;
    }

    &.el-tag--success:before {
      background-color: var(--success-color, #4CAF50);
      box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2);
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--primary-dark, #344767);
}

.header-buttons {
  display: flex;
  gap: 12px;
}

.guide-button {
  border-color: #dcdfe6;
  &:hover {
    border-color: var(--primary-color, #3B82F6);
    color: var(--primary-color, #3B82F6);
  }
}

.add-button {
  background-color: var(--primary-color, #3B82F6);
  border-color: var(--primary-color, #3B82F6);
  color: white;

  &:hover {
    background-color: #2563EB;
    border-color: #2563EB;
    transform: translateY(-1px);
  }
}

.custom-table {
  margin-top: 16px;
  border-collapse: separate;
  border-spacing: 0;

  :deep(th) {
    background-color: #f7f9fc;
    color: #344767;
    font-weight: 600;
    padding: 12px 0;
  }

  :deep(td) {
    padding: 12px 0;
  }

  :deep(tr) {
    line-height: 2.5rem;
    transition: background-color 0.3s ease;

    &:hover {
      background-color: #E0F2FE !important;
    }
  }

  :deep(.el-table__row.highlight-row) {
    background-color: #E0F2FE;
  }
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.action-button {
  transition: all 0.3s ease;

  &:hover {
    transform: scale(1.1);
    background-color: #f0f9ff;
  }
}

.custom-dialog {
  :deep(.el-dialog__header) {
    border-bottom: 1px solid #f0f0f0;
    padding-bottom: 16px;
    margin-bottom: 8px;
  }

  :deep(.el-dialog__title) {
    font-weight: 600;
    color: var(--primary-dark, #344767);
  }

  :deep(.el-dialog__body) {
    padding: 24px;
  }
}

.custom-form {
  :deep(.el-form-item__label) {
    font-weight: 500;
  }

  :deep(.el-input__inner) {
    border-radius: 6px;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
}

.dialog-footer-between {
  display: flex;
  justify-content: space-between;
  padding-top: 16px;

  > div {
    display: flex;
    gap: 12px;
  }
}

.cancel-button {
  &:hover {
    color: #2563EB;
  }
}

.confirm-button, .save-button {
  background-color: var(--primary-color, #3B82F6);
  border-color: var(--primary-color, #3B82F6);

  &:hover {
    background-color: #2563EB;
    border-color: #2563EB;
  }
}

.reset-button {
  &:hover {
    color: var(--primary-color, #3B82F6);
    border-color: var(--primary-color, #3B82F6);
  }
}

.role-tag-container {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-tag {
  font-weight: 500;
}

.perm-alert-container {
  margin-bottom: 20px;
}

.custom-alert {
  background-color: #f0f9ff;
  border-color: #bae6fd;

  :deep(.el-alert__content) {
    padding: 8px 0;
  }

  p {
    margin: 6px 0;
    line-height: 1.5;
    color: #0c4a6e;
  }

  ol {
    margin: 8px 0;
    padding-left: 24px;

    li {
      margin-bottom: 4px;
      color: #0c4a6e;
    }
  }
}

.menu-tree-container {
  margin-top: 20px;
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}

.custom-tree {
  :deep(.el-tree-node__content) {
    height: 36px;

    &:hover {
      background-color: #f0f9ff;
    }
  }

  :deep(.is-current) .el-tree-node__content {
    background-color: #E0F2FE;
  }
}

.custom-divider {
  margin: 24px 0 16px;

  :deep(.el-divider__text) {
    font-weight: 600;
    color: var(--primary-dark, #344767);
  }
}

.permission-type {
  margin: 16px 0;
}

.permission-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.perm-tag {
  font-weight: 500;
}

.guide-content {
  padding: 0 4px;

  p {
    line-height: 1.6;
    color: var(--text-secondary, #67748e);
  }
}

.custom-switch {
  :deep(.el-switch__core) {
    border-color: #d1d5db;
  }

  :deep(.el-switch.is-checked .el-switch__core) {
    background-color: var(--primary-color, #3B82F6);
    border-color: var(--primary-color, #3B82F6);
  }
}
</style>
