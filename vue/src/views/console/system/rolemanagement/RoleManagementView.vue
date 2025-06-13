<template>
  <div class="role-management-container">
    <div class="custom-card">
      <div class="card-header">
        <h2 class="card-title">角色列表</h2>
        <div class="header-buttons">
          <el-button class="guide-button" plain @click="showPermissionGuide">
            <el-icon><Document /></el-icon>
            权限说明
          </el-button>
          <el-button type="primary" class="add-button" @click="handleAddRole">
            <el-icon><Plus /></el-icon>
            添加角色
          </el-button>
        </div>
      </div>
      <div class="divider"></div>
      <el-table
        :data="roleList"
        v-loading="loading"
        :row-class-name="tableRowClassName"
        border
        stripe
        highlight-current-row
        class="custom-table"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="roleName" label="角色名称" width="140" />
        <el-table-column prop="remark" label="描述" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="scope">
            <span class="status-tag" :class="scope.row.status === 1 ? 'status-active' : 'status-inactive'">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" text circle @click="handleEdit(scope.row)" class="action-button edit" title="编辑角色">
                <el-icon color="#409efc"><Edit /></el-icon>
              </el-button>

              <el-button size="small" text circle @click="handlePermissions(scope.row)" class="action-button permission" title="设置权限">
                <el-icon color="#409efc"><Setting /></el-icon>
              </el-button>

              <el-button
                size="small"
                type="danger"
                text
                circle
                @click="handleDelete(scope.row)"
                :disabled="scope.row.roleName === 'ADMIN' || scope.row.roleName === 'USER'"
                class="action-button delete"
                title="删除角色"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <RoleDialog
      v-model:visible="dialogVisible"
      v-model:roleData="currentRole"
      :is-edit="isEdit"
      @cancel="handleDialogCancel"
      @confirm="handleDialogConfirm"
    />

    <PermissionsDialog
      v-model:visible="permDialogVisible"
      :current-role="currentRole"
      :routers-tree="routersTree"
      :default-checked-menus="defaultCheckedMenus"
      :loading="loading"
      @save="savePermissions"
      @reset="resetPermissions"
      @close="cleanupPermissionDialog"
      @update:checked-menus="handleCheckedKeysChange"
    />

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
import Permissionsdocumentation from '@/views/console/system/rolemanagement/components/PermissionsDocumentation.vue';
import PermissionsDialog from '@/views/console/system/rolemanagement/components/PermissionsDialog.vue';
import RoleDialog from '@/views/console/system/rolemanagement/components/RoleDialog.vue';
import { Document, Plus } from '@element-plus/icons-vue';

// 状态变量
const roleList = ref<Role[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const permDialogVisible = ref(false);
const isEdit = ref(false);
const currentRole = ref<Partial<Role>>({
  id: 0,
  roleName: '',
  status: 1,
  remark: '',
  permissions: [],
  routers: [],
  createTime: '',
  updateTime: '',
  userId: 0
});
const routersTree = ref<Router[]>([]);
const defaultCheckedMenus = ref<number[]>([]);
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
    routers: [],
    createTime: '',
    updateTime: '',
    userId: 0
  };
  dialogVisible.value = true;
};

// 编辑角色
const handleEdit = (role: Role) => {
  isEdit.value = true;
  currentRole.value = { ...role };
  dialogVisible.value = true;
};

// 处理对话框取消
const handleDialogCancel = () => {
  dialogVisible.value = false;
};

// 处理对话框确认
const handleDialogConfirm = async (roleData: Partial<Role>) => {
  try {
    if (isEdit.value) {
      await editRole(roleData);
      ElMessage.success('角色更新成功');
    } else {
      await addRole(roleData);
      ElMessage.success('角色添加成功');
    }
    dialogVisible.value = false;
    fetchRoleList();
  } catch (error) {
    console.error(isEdit.value ? '更新角色失败:' : '添加角色失败:', error);
    ElMessage.error(isEdit.value ? '更新角色失败' : '添加角色失败');
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

// 处理选中节点变化
const handleCheckedKeysChange = (keys: number[]) => {
  defaultCheckedMenus.value = keys;
};

// 初始化权限树选中状态
const initPermissionTree = () => {
  if (!menuTreeRef.value) return;
  menuTreeRef.value.initTree();
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
    menuTreeRef.value.initTree();
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
  if (!defaultCheckedMenus.value || defaultCheckedMenus.value.length === 0) {
    ElMessage.warning('请至少选择一个权限');
    return;
  }

  try {
    // 保存路由权限（路由中已包含对应的操作权限）
    await updateRoleRouters({
      roleId: currentRole.value.id as number,
      routerIds: [...defaultCheckedMenus.value] // 使用展开运算符创建新数组
    });
  } catch (error) {
    console.error('保存权限数据失败:', error);
    throw error; // 将错误抛出，由上层方法处理
  }
};

// 显示权限说明文档
const showPermissionGuide = () => {
  guideDialogVisible.value = true;
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
    routers: [],
    createTime: '',
    updateTime: '',
    userId: 0
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
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 80vh;
  background: #f6f8fa;
  padding: 8px 0 0 0;
}

.custom-card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 8px 0 rgba(0,0,0,0.04);
  padding: 20px 24px 16px 24px;
  width: 100%;
  max-width: 98vw;
  min-width: 0;
  margin: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.card-title {
  font-size: 2rem;
  font-weight: 700;
  color: #222;
  margin: 0;
  letter-spacing: 1px;
}

.header-buttons {
  display: flex;
  gap: 16px;
}

.divider {
  height: 1px;
  background: #f0f0f0;
  margin: 12px 0 16px 0;
}

.custom-table {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  font-size: 16px;
}

.el-table th {
  background: #f7f9fa !important;
  color: #666;
  font-weight: 600;
  font-size: 15px;
}

.el-table .el-table__row {
  font-size: 16px;
}

.el-table .el-table__row:hover {
  background: #f5f7fa !important;
}

.status-tag {
  padding: 2px 12px;
  border-radius: 8px;
  font-size: 15px;
  &.status-active {
    background: #e6f7e6;
    color: #52c41a;
  }
  &.status-inactive {
    background: #fff1f0;
    color: #f5222d;
  }
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

@media (max-width: 1200px) {
  .custom-card {
    max-width: 100vw;
    padding: 8px 2px;
  }
}
</style>
