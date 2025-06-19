<template>
  <div class="role-management-layout">
    <!-- 页面头部 -->
    <section class="page-header">
      <div class="header-content">
        <div class="title-section">
          <h1 class="page-title">
            <el-icon class="title-icon">
              <UserFilled />
            </el-icon>
            角色权限管理
          </h1>
          <p class="page-description">统一管理系统角色权限，确保数据安全与访问控制</p>
        </div>
        <div class="header-actions">
          <el-button class="guide-btn" @click="showPermissionGuide">
            <el-icon>
              <QuestionFilled />
            </el-icon>
            权限说明
          </el-button>
          <el-button type="primary" class="add-role-btn" @click="handleAddRole">
            <el-icon>
              <Plus />
            </el-icon>
            新建角色
          </el-button>
        </div>
      </div>
    </section>

    <!-- 统计信息卡片 -->
    <section class="stats-section">
      <div class="stats-grid">
        <div class="stat-card total">
          <div class="stat-icon">
            <el-icon>
              <Collection />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ roleList.length }}</div>
            <div class="stat-label">总角色数</div>
          </div>
        </div>

        <div class="stat-card active">
          <div class="stat-icon">
            <el-icon>
              <Check />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ activeRoleCount }}</div>
            <div class="stat-label">启用角色</div>
          </div>
        </div>

        <div class="stat-card system">
          <div class="stat-icon">
            <el-icon>
              <Lock />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ systemRoleCount }}</div>
            <div class="stat-label">系统角色</div>
          </div>
        </div>

        <div class="stat-card custom">
          <div class="stat-icon">
            <el-icon>
              <User />
            </el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-number">{{ customRoleCount }}</div>
            <div class="stat-label">自定义角色</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 主内容区域 -->
    <section class="main-content">
      <div class="content-card">
        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="search-section">
            <el-input v-model="searchQuery" placeholder="搜索角色名称或描述..." class="search-input" clearable
              @input="handleSearch">
              <template #prefix>
                <el-icon>
                  <Search />
                </el-icon>
              </template>
            </el-input>
          </div>

          <div class="filter-section">
            <el-select v-model="statusFilter" placeholder="状态筛选" class="status-filter" clearable @change="handleFilter">
              <el-option label="全部状态" value="" />
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>

            <el-select v-model="typeFilter" placeholder="类型筛选" class="type-filter" clearable @change="handleFilter">
              <el-option label="全部类型" value="" />
              <el-option label="系统角色" value="system" />
              <el-option label="自定义角色" value="custom" />
            </el-select>
          </div>

          <div class="view-controls">
            <el-radio-group v-model="viewMode" class="view-toggle">
              <el-radio-button label="table">
                <el-icon>
                  <Grid />
                </el-icon>
              </el-radio-button>
              <el-radio-button label="card">
                <el-icon>
                  <Menu />
                </el-icon>
              </el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 表格视图 -->
        <div v-if="viewMode === 'table'" class="table-container">
          <el-table :data="filteredRoleList" v-loading="loading" :row-class-name="tableRowClassName"
            class="modern-table" empty-text="暂无角色数据" :default-sort="{ prop: 'createTime', order: 'descending' }"
            @sort-change="handleSortChange">
            <!-- 复选框列 -->
            <el-table-column type="selection" width="50" align="center" />

            <!-- 角色信息 -->
            <el-table-column label="角色信息" min-width="200">
              <template #default="{ row }">
                <div class="role-info-cell">
                  <div class="role-avatar">
                    <el-icon v-if="isSystemRole(row.roleName)" class="system-icon">
                      <Lock />
                    </el-icon>
                    <el-icon v-else class="custom-icon">
                      <User />
                    </el-icon>
                  </div>
                  <div class="role-details">
                    <div class="role-name-row">
                      <span class="role-name">{{ row.roleName }}</span>
                      <el-tag v-if="isSystemRole(row.roleName)" type="danger" size="small" class="role-tag">
                        系统
                      </el-tag>
                    </div>
                    <div class="role-description">{{ row.remark || '暂无描述' }}</div>
                    <div class="role-meta">
                      <span class="role-id">ID: {{ row.id }}</span>
                      <span class="role-time">{{ formatTime(row.createTime) }}</span>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <!-- 权限统计 -->
            <el-table-column label="权限统计" width="120" align="center">
              <template #default="{ row }">
                <div class="permission-stats">
                  <div class="permission-count">
                    <span class="count-number">{{ row.routers?.length || 0 }}</span>
                    <span class="count-label">个权限</span>
                  </div>
                  <el-progress :percentage="getPermissionPercentage(row)" :stroke-width="6" :show-text="false"
                    :color="getProgressColor(row)" />
                </div>
              </template>
            </el-table-column>

            <!-- 状态 -->
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.status" :active-value="1" :inactive-value="0"
                  :disabled="isSystemRole(row.roleName)" @change="handleStatusChange(row)" />
              </template>
            </el-table-column>

            <!-- 操作 -->
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <div class="action-group">
                  <el-tooltip content="查看详情" placement="top">
                    <el-button type="primary" link @click="handleViewDetail(row)" class="action-btn">
                      <el-icon>
                        <View />
                      </el-icon>
                    </el-button>
                  </el-tooltip>

                  <el-tooltip content="编辑角色" placement="top">
                    <el-button type="warning" link @click="handleEdit(row)" class="action-btn">
                      <el-icon>
                        <Edit />
                      </el-icon>
                    </el-button>
                  </el-tooltip>

                  <el-tooltip content="权限设置" placement="top">
                    <el-button type="success" link @click="handlePermissions(row)">
                      <el-icon>
                        <Setting />
                      </el-icon>
                    </el-button>
                  </el-tooltip>

                  <el-tooltip :content="isSystemRole(row.roleName) ? '系统角色不能删除' : '删除角色'" placement="top">
                    <el-button type="danger" link @click="handleDelete(row)" :disabled="isSystemRole(row.roleName)">
                      <el-icon>
                        <Delete />
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 卡片视图 -->
        <div v-else class="card-view-container">
          <div class="role-cards-grid">
            <div v-for="role in filteredRoleList" :key="role.id" class="role-card"
              :class="{ 'system-role': isSystemRole(role.roleName || '') }">
              <div class="card-header">
                <div class="role-avatar-large">
                  <el-icon v-if="isSystemRole(role.roleName || '')">
                    <Lock />
                  </el-icon>
                  <el-icon v-else>
                    <User />
                  </el-icon>
                </div>
                <div class="role-status">
                  <el-switch v-model="role.status" :active-value="1" :inactive-value="0"
                    :disabled="isSystemRole(role.roleName || '')" @change="handleStatusChange(role)" size="small" />
                </div>
              </div>

              <div class="card-content">
                <div class="role-title">
                  <h3 class="role-name">{{ role.roleName }}</h3>
                  <el-tag v-if="isSystemRole(role.roleName || '')" type="danger" size="small">
                    系统
                  </el-tag>
                </div>

                <p class="role-desc">{{ role.remark || '暂无描述' }}</p>

                <div class="role-stats">
                  <div class="stat-item">
                    <span class="stat-value">{{ role.routers?.length || 0 }}</span>
                    <span class="stat-text">权限</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ formatTime(role.createTime || '') }}</span>
                    <span class="stat-text">创建时间</span>
                  </div>
                </div>
              </div>

              <div class="card-actions">
                <el-button-group class="action-group">
                  <el-button size="small" @click="handleEdit(role)">
                    <el-icon>
                      <Edit />
                    </el-icon>
                    编辑
                  </el-button>
                  <el-button size="small" @click="handlePermissions(role)">
                    <el-icon>
                      <Setting />
                    </el-icon>
                    权限
                  </el-button>
                  <el-button size="small" type="danger" :disabled="isSystemRole(role.roleName || '')"
                    @click="handleDelete(role)">
                    <el-icon>
                      <Delete />
                    </el-icon>
                    删除
                  </el-button>
                </el-button-group>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 对话框组件 -->
    <RoleDialog v-model:visible="dialogVisible" v-model:roleData="currentRole" :is-edit="isEdit"
      @cancel="handleDialogCancel" @confirm="handleDialogConfirm" />

    <PermissionsDialog v-model:visible="permDialogVisible" :current-role="currentRole" :routers-tree="routersTree"
      :default-checked-menus="defaultCheckedMenus" :loading="loading" @save="savePermissions" @reset="resetPermissions"
      @close="cleanupPermissionDialog" @update:checked-menus="handleCheckedKeysChange" />

    <Permissionsdocumentation v-model:dialogVisible="guideDialogVisible" />
  </div>
</template>

<script setup lang="ts" name="RoleManagementComponent">
import { ref, onMounted, watch, nextTick, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getRoleList, addRole, editRole, deleteRole, updateRoleRouters } from '@/api/auth/auth';
import { getUserRoutes } from '@/api/router/router';
import type { Router } from '@/router/index.d';
import type { Role } from '@/api/auth/auth.d';
import Permissionsdocumentation from '@/views/console/system/rolemanagement/components/PermissionsDocumentation.vue';
import PermissionsDialog from '@/views/console/system/rolemanagement/components/PermissionsDialog.vue';
import RoleDialog from '@/views/console/system/rolemanagement/components/RoleDialog.vue';
import {
  UserFilled,
  QuestionFilled,
  Plus,
  Collection,
  Check,
  Lock,
  User,
  Search,
  Grid,
  Menu,
  View,
  Edit,
  Setting,
  Delete
} from '@element-plus/icons-vue';

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

// 新增状态变量
const searchQuery = ref('');
const statusFilter = ref('');
const typeFilter = ref('');
const viewMode = ref('table');

// 计算属性
const activeRoleCount = computed(() =>
  roleList.value.filter(role => role.status === 1).length
);

const systemRoleCount = computed(() =>
  roleList.value.filter(role => isSystemRole(role.roleName || '')).length
);

const customRoleCount = computed(() =>
  roleList.value.filter(role => !isSystemRole(role.roleName || '')).length
);

const filteredRoleList = computed(() => {
  let filtered = roleList.value;

  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    filtered = filtered.filter(role =>
      role.roleName?.toLowerCase().includes(query) ||
      (role.remark && role.remark.toLowerCase().includes(query))
    );
  }

  // 状态过滤
  if (statusFilter.value !== '') {
    filtered = filtered.filter(role => role.status === Number(statusFilter.value));
  }

  // 类型过滤
  if (typeFilter.value) {
    if (typeFilter.value === 'system') {
      filtered = filtered.filter(role => isSystemRole(role.roleName || ''));
    } else if (typeFilter.value === 'custom') {
      filtered = filtered.filter(role => !isSystemRole(role.roleName || ''));
    }
  }

  return filtered;
});

// 工具函数
const isSystemRole = (roleName: string) => {
  return roleName === 'ADMIN' || roleName === 'USER';
};

const formatTime = (time: string) => {
  if (!time) return '--';
  return new Date(time).toLocaleDateString('zh-CN');
};

const getPermissionPercentage = (role: Role) => {
  const maxPermissions = 20; // 假设最大权限数为20
  const currentPermissions = role.routers?.length || 0;
  return Math.min((currentPermissions / maxPermissions) * 100, 100);
};

const getProgressColor = (role: Role) => {
  const percentage = getPermissionPercentage(role);
  if (percentage >= 80) return '#67c23a';
  if (percentage >= 50) return '#e6a23c';
  return '#f56c6c';
};

// 事件处理函数
const handleSearch = () => {
  // 搜索逻辑在计算属性中处理
};

const handleFilter = () => {
  // 过滤逻辑在计算属性中处理
};

const handleSortChange = (sortInfo: any) => {
  // 处理排序
  console.log('排序变化:', sortInfo);
};

const handleStatusChange = async (role: Role) => {
  try {
    await editRole({ ...role });
    ElMessage.success('状态更新成功');
    fetchRoleList();
  } catch (error) {
    console.error('状态更新失败:', error);
    ElMessage.error('状态更新失败');
    // 恢复原状态
    role.status = role.status === 1 ? 0 : 1;
  }
};

const handleViewDetail = (role: Role) => {
  // 查看详情逻辑
  console.log('查看详情:', role);
};

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
  }).catch(() => { });
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
@use './rolemanagement.scss' as *;
</style>
