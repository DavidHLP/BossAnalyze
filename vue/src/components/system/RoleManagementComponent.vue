<template>
  <div class="role-management-container">
    <div class="header-section">
      <h1 class="title">角色管理</h1>
      <el-alert
        class="info-alert"
        type="info"
        show-icon
        :closable="false"
      >
        <template #title>
          <span class="alert-title">角色权限管理说明</span>
        </template>
        <div class="alert-content">
          本系统支持多角色权限管理，可以为不同角色分配不同的菜单和操作权限。系统内置ADMIN和USER角色，ADMIN角色拥有所有权限，USER角色为基础权限。
        </div>
      </el-alert>
    </div>

    <el-card class="role-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="section-title">角色列表</span>
          <div class="header-actions">
            <el-button plain size="small" icon="Document" @click="showPermissionGuide">权限说明</el-button>
            <el-button type="primary" size="small" icon="Plus" @click="handleAddRole">添加角色</el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="roleList"
        style="width: 100%"
        v-loading="loading"
        :row-class-name="tableRowClassName"
        border
        :highlight-current-row="false"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="roleName" label="角色名称" width="140" />
        <el-table-column prop="remark" label="描述" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.status === 1 ? 'success' : 'danger'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-tooltip content="编辑角色" placement="top">
                <el-button size="small" type="info" text circle @click="handleEdit(scope.row)">
                  <el-icon><Edit /></el-icon>
                </el-button>
              </el-tooltip>

              <el-tooltip content="设置权限" placement="top">
                <el-button size="small" type="info" text circle @click="handlePermissions(scope.row)">
                  <el-icon><Setting /></el-icon>
                </el-button>
              </el-tooltip>

              <el-tooltip content="删除角色" placement="top">
                <el-button
                  size="small"
                  type="info"
                  text
                  circle
                  @click="handleDelete(scope.row)"
                  :disabled="scope.row.roleName === 'ADMIN' || scope.row.roleName === 'USER'"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 角色编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑角色' : '添加角色'"
      width="480px"
      destroy-on-close
      top="10vh"
    >
      <el-form :model="currentRole" label-width="80px" ref="roleFormRef">
        <el-form-item label="角色名称" prop="roleName" :rules="[{ required: true, message: '请输入角色名称', trigger: 'blur' }]">
          <el-input v-model="currentRole.roleName" placeholder="请输入角色名称" :disabled="isEdit && (currentRole.roleName === 'ADMIN' || currentRole.roleName === 'USER')" />
        </el-form-item>
        <el-form-item label="描述" prop="remark">
          <el-input v-model="currentRole.remark" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="currentRole.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="isEdit ? handleEditSubmit() : handleAddSubmit()">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 权限设置对话框 -->
    <el-dialog
      v-model="permDialogVisible"
      title="权限设置"
      width="720px"
      :before-close="handleClosePermDialog"
      destroy-on-close
      top="5vh"
    >
      <div class="permission-info" v-if="currentRole.roleName">
        <span class="role-label">当前角色: </span>
        <el-tag size="small" effect="plain">{{ currentRole.roleName }}</el-tag>
      </div>

      <div class="permission-description">
        <el-alert type="info" :closable="false">
          <p>通过选择菜单来分配权限，系统将自动关联对应的操作权限。</p>
          <p>勾选子菜单时，父菜单会自动被选中，但勾选父菜单不会自动选中子菜单。</p>
        </el-alert>
      </div>

      <div class="tree-container">
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
          class="menu-tree"
        />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <div class="left-actions">
            <el-popconfirm
              title="检测到数据已修改，确定要关闭吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="permDialogVisible = false"
              v-if="checkForUnsavedChanges()"
            >
              <template #reference>
                <el-button>取消</el-button>
              </template>
            </el-popconfirm>
            <el-button v-else @click="permDialogVisible = false">取消</el-button>
          </div>
          <div class="right-actions">
            <el-button plain icon="RefreshRight" @click="resetPermissions">重置</el-button>
            <el-button type="primary" icon="Check" @click="savePermissions">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 权限说明文档对话框 -->
    <el-dialog v-model="guideDialogVisible" title="权限说明文档" width="720px" destroy-on-close top="5vh">
      <div class="permission-guide">
        <el-divider content-position="left">
          <span class="divider-title">系统权限说明</span>
        </el-divider>

        <p class="guide-intro">本系统的权限管理以菜单为基础，每个菜单项关联相应的功能权限：</p>
        <div class="guide-types">
          <div class="guide-type-item">
            <el-tag type="success" effect="plain">菜单权限</el-tag>
            <span class="guide-type-desc">控制用户可以访问哪些菜单和页面，同时自动关联相应的操作权限</span>
          </div>
        </div>

        <el-alert type="info" :closable="false" style="margin: 16px 0">
          <p>当您为角色分配菜单权限时，系统会自动处理以下内容：</p>
          <ol class="guide-points">
            <li>授予该菜单的访问权限</li>
            <li>授予该菜单下所有相关功能的操作权限</li>
            <li>自动处理权限的父子级关系</li>
          </ol>
        </el-alert>

        <el-divider content-position="left">
          <span class="divider-title">系统内置角色</span>
        </el-divider>

        <el-table :data="[
          { name: 'ADMIN', desc: '系统管理员，拥有所有权限', editable: false },
          { name: 'USER', desc: '普通用户，拥有基础浏览权限', editable: false }
        ]" border style="width: 100%" class="roles-table">
          <el-table-column prop="name" label="角色名称" width="160" />
          <el-table-column prop="desc" label="描述" />
          <el-table-column prop="editable" label="可编辑性" width="120" align="center">
            <template #default="scope">
              <el-tag size="small" :type="scope.row.editable ? 'success' : 'danger'">
                {{ scope.row.editable ? '可编辑' : '不可编辑' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="RoleManagementComponent">
import { ref, onMounted, watch, nextTick, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getRoleList, addRole, editRole, deleteRole, updateRoleRouters } from '@/api/auth/auth';
import { getUserRoutes } from '@/api/router/router';
import type { Router } from '@/router/index.d';
import type { Role } from '@/api/auth/auth.d';

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

// 计算属性
const hasChanges = computed(() => checkForUnsavedChanges());

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
  checkedNodes.forEach((checkedNode: any) => {
    // 从树中获取完整的节点引用，包含父节点信息
    const treeNode = menuTreeRef.value.getNode(checkedNode.id);
    if (treeNode && treeNode.parent) {
      // 递归勾选父节点
      checkParentNodes(treeNode);
    }
  });
};

// 递归获取树的所有节点
const getAllTreeNodes = (nodes: any[]): any[] => {
  let allNodes: any[] = [];

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
  data: any,
  checked: boolean,
  indeterminate: boolean
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
const checkParentNodes = (node: any) => {
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
.role-management-container {
  padding: 16px;

  .header-section {
    margin-bottom: 20px;

    .title {
      font-size: 24px;
      margin-bottom: 16px;
      font-weight: 500;
      color: #303133;
    }

    .info-alert {
      margin-bottom: 16px;

      .alert-title {
        font-weight: 500;
      }

      .alert-content {
        margin-top: 8px;
        line-height: 1.5;
      }
    }
  }

  .role-card {
    margin-bottom: 20px;
    transition: all 0.3s;
    border-radius: 8px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .section-title {
        font-size: 16px;
        font-weight: 500;
        color: #303133;
      }

      .header-actions {
        display: flex;
        gap: 8px;
      }
    }
  }

  // 权限设置对话框样式
  .permission-info {
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 8px;

    .role-label {
      font-weight: 500;
      color: #606266;
    }
  }

  .permission-description {
    margin-bottom: 16px;
  }

  .tree-container {
    max-height: 400px;
    overflow-y: auto;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    padding: 12px;

    .menu-tree {
      width: 100%;
    }
  }

  // 对话框底部按钮样式
  .dialog-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .left-actions, .right-actions {
      display: flex;
      gap: 8px;
    }
  }

  // 权限说明文档样式
  .permission-guide {
    padding: 0 4px;

    .divider-title {
      font-size: 16px;
      font-weight: 500;
      color: #409eff;
    }

    .guide-intro {
      margin: 16px 0;
      line-height: 1.6;
    }

    .guide-types {
      margin: 16px 0;
      display: flex;
      flex-direction: column;
      gap: 12px;

      .guide-type-item {
        display: flex;
        align-items: center;
        gap: 12px;

        .guide-type-desc {
          color: #606266;
        }
      }
    }

    .guide-points {
      margin: 12px 0;
      padding-left: 20px;

      li {
        margin-bottom: 8px;
      }
    }

    .roles-table, .permissions-table {
      margin: 16px 0;
    }
  }

  // 表格样式
  :deep(.el-table) {
    border-radius: 4px;
    overflow: hidden;

    .el-button + .el-button {
      margin-left: 4px;
    }

    .action-buttons {
      display: flex;
      justify-content: center;
      gap: 12px;
    }
  }
}

// 表格高亮行样式
:deep(.highlight-row) {
  background-color: #1b2d24 !important;
  transition: background-color 0.3s;
}

// 卡片悬停效果
.role-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
</style>
