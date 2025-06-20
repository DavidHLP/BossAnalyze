<template>
  <div class="user-management-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <h1 class="page-title">
            <el-icon class="title-icon"><User /></el-icon>
            用户管理中心
          </h1>
          <p class="page-subtitle">管理系统用户、角色分配与权限控制</p>
        </div>
        <div class="header-stats">
          <div class="stat-card">
            <div class="stat-number">{{ total }}</div>
            <div class="stat-label">总用户数</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ tableData.filter(u => u.status === 1).length }}</div>
            <div class="stat-label">活跃用户</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ roleOptions.length }}</div>
            <div class="stat-label">角色类型</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索和操作区域 -->
    <div class="search-section">
      <el-card class="search-card" shadow="hover">
        <template #header>
          <div class="search-header">
            <div class="search-title">
              <el-icon><Search /></el-icon>
              <span>筛选查询</span>
            </div>
          </div>
        </template>

        <SearchComponents
          v-model:searchForm="searchForm"
          :roleOptions="roleOptions"
          :roleLoading="roleLoading"
          :remoteRoleSearch="remoteRoleSearch"
          @search="handleSearch"
          @reset="handleReset"
          @add-user="addDialogVisible = true"
        />
      </el-card>
    </div>

    <!-- 用户列表区域 -->
    <div class="table-section">
      <el-card class="table-card" shadow="hover">
        <template #header>
          <div class="table-header">
            <div class="header-left">
              <h2 class="table-title">
                <el-icon><List /></el-icon>
                用户列表
              </h2>
              <div class="table-meta">
                <el-tag type="info" effect="light" round>
                  共 {{ total }} 条记录
                </el-tag>
                <el-tag type="success" effect="light" round>
                  第 {{ pageNum }} 页
                </el-tag>
              </div>
            </div>
            <div class="header-actions">
              <el-tooltip content="刷新数据" placement="top">
                <el-button
                  :icon="Refresh"
                  circle
                  @click="fetchData(pageNum, pageSize)"
                  class="action-btn"
                />
              </el-tooltip>
              <el-tooltip content="导出数据" placement="top">
                <el-button
                  :icon="Download"
                  circle
                  class="action-btn"
                />
              </el-tooltip>
              <el-tooltip content="表格设置" placement="top">
                <el-button
                  :icon="Setting"
                  circle
                  class="action-btn"
                />
              </el-tooltip>
            </div>
          </div>
        </template>

        <div class="table-container">
          <el-table
            :data="tableData"
            class="user-table"
            :stripe="true"
            :border="false"
            table-layout="auto"
            :row-class-name="tableRowClassName"
          >
            <!-- 展开列 -->
            <el-table-column type="expand" width="50">
              <template #default="props">
                <div class="user-detail-card">
                  <div class="detail-header">
                    <div class="avatar-section">
                      <el-avatar
                        :size="80"
                        :src="avatarUrls[props.row.avatar]"
                        class="user-avatar-large"
                      >
                        <el-icon><User /></el-icon>
                      </el-avatar>
                      <div class="user-basic-info">
                        <h3 class="user-name">{{ props.row.name }}</h3>
                        <el-tag
                          :type="props.row.status === 1 ? 'success' : 'danger'"
                          effect="light"
                          round
                          class="status-tag-large"
                        >
                          {{ props.row.status === 1 ? '✓ 启用' : '✗ 禁用' }}
                        </el-tag>
                      </div>
                    </div>
                  </div>

                  <div class="detail-content">
                    <div class="info-grid">
                      <div class="info-item">
                        <div class="info-label">
                          <el-icon><Message /></el-icon>
                          邮箱地址
                        </div>
                        <div class="info-value">{{ props.row.email || '暂未设置' }}</div>
                      </div>

                      <div class="info-item">
                        <div class="info-label">
                          <el-icon><UserFilled /></el-icon>
                          用户角色
                        </div>
                        <div class="info-value">
                          <el-tag type="primary" effect="light">{{ props.row.roleName }}</el-tag>
                        </div>
                      </div>

                      <div class="info-item">
                        <div class="info-label">
                          <el-icon><Location /></el-icon>
                          联系地址
                        </div>
                        <div class="info-value">{{ props.row.address || '暂未填写' }}</div>
                      </div>

                      <div class="info-item">
                        <div class="info-label">
                          <el-icon><Clock /></el-icon>
                          创建时间
                        </div>
                        <div class="info-value">{{ props.row.createTime }}</div>
                      </div>
                    </div>

                    <div class="introduction-section" v-if="props.row.introduction">
                      <div class="section-title">
                        <el-icon><Document /></el-icon>
                        个人简介
                      </div>
                      <div class="introduction-content">
                        {{ props.row.introduction }}
                      </div>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <!-- 头像列 -->
            <el-table-column label="头像" width="80" align="center">
              <template #default="scope">
                <el-avatar
                  :size="40"
                  :src="avatarUrls[scope.row.avatar]"
                  class="table-avatar"
                >
                  <el-icon><User /></el-icon>
                </el-avatar>
              </template>
            </el-table-column>

            <!-- 用户信息列 -->
            <el-table-column label="用户信息" min-width="200">
              <template #default="scope">
                <div class="user-info-cell">
                  <div class="user-name-cell">{{ scope.row.name }}</div>
                  <div class="user-email-cell">{{ scope.row.email || '暂无邮箱' }}</div>
                </div>
              </template>
            </el-table-column>

            <!-- 角色列 -->
            <el-table-column label="角色" width="120" align="center">
              <template #default="scope">
                <el-tag type="primary" effect="light" round>
                  {{ scope.row.roleName }}
                </el-tag>
              </template>
            </el-table-column>

            <!-- 状态列 -->
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
                <div class="status-cell">
                  <div
                    class="status-indicator"
                    :class="{ 'active': scope.row.status === 1 }"
                  ></div>
                  <el-tag
                    :type="scope.row.status === 1 ? 'success' : 'danger'"
                    effect="light"
                    size="small"
                    round
                  >
                    {{ scope.row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>

            <!-- 创建时间列 -->
            <el-table-column label="创建时间" width="180">
              <template #default="scope">
                <div class="time-cell">
                  <el-icon><Clock /></el-icon>
                  <span>{{ scope.row.createTime }}</span>
                </div>
              </template>
            </el-table-column>

            <!-- 操作列 -->
            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default="scope">
                <div class="action-buttons">
                  <el-tooltip content="编辑用户" placement="top">
                    <el-button
                      size="small"
                      type="primary"
                      :icon="Edit"
                      circle
                      @click="handleEdit(scope.row)"
                      class="edit-btn"
                    />
                  </el-tooltip>

                  <el-tooltip content="快速查看" placement="top">
                    <el-button
                      size="small"
                      type="info"
                      :icon="View"
                      circle
                      @click="handleQuickView(scope.row)"
                      class="view-btn"
                    />
                  </el-tooltip>

                  <el-tooltip content="删除用户" placement="top">
                    <el-button
                      size="small"
                      type="danger"
                      :icon="Delete"
                      circle
                      @click="handleDelete(scope.row)"
                      class="delete-btn"
                    />
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 分页器 -->
        <div class="pagination-container">
          <div class="pagination-info">
            显示第 {{ (pageNum - 1) * pageSize + 1 }} 到 {{ Math.min(pageNum * pageSize, total) }} 条记录，共 {{ total }} 条
          </div>
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            background
            class="table-pagination"
          />
        </div>
      </el-card>
    </div>

    <!-- 对话框组件 -->
    <security-dialog
      v-model:visible="deleteDialogVisible"
      :loading="deleteLoading"
      @confirm="handleConfirmDelete"
      @cancel="resetDeleteState"
    />

    <edit-user
      v-model:visible="editDialogVisible"
      :user-data="formData"
      @user-updated="handleUserUpdated"
    />

    <add-user
      v-model:visible="addDialogVisible"
      @user-added="handleUserAdded"
    />

    <user-quick-view
      v-model:visible="quickViewDialogVisible"
      :user-data="quickViewUserData"
      @edit="handleQuickViewEdit"
      @close="handleQuickViewClose"
    />
  </div>
</template>

<script lang="ts" setup name="UserManagementComponent">
import {
  ElTable,
  ElTableColumn,
  ElPagination,
  ElMessageBox,
  ElMessage,
  ElTag,
  ElButton,
  ElCard,
  ElAvatar,
  ElTooltip,
  ElIcon,
} from 'element-plus'
import {
  User,
  UserFilled,
  Search,
  Plus,
  Edit,
  Delete,
  Refresh,
  Download,
  List,
  Message,
  Location,
  Clock,
  Document,
  Star,
  Setting,
  View,
} from '@element-plus/icons-vue'
import { getUserManageInfo } from '@/api/user/user'
import { ref, onMounted, reactive, watch } from 'vue'
import type { UserBaseInfo as UserType } from '@/api/auth/auth.d'
import type { PageInfo } from '@/types/common'
import { getRoleList } from '@/api/role/role'
import { deleteUser } from '@/api/user/user'
import EditUser from './components/EditUser.vue'
import AddUser from './components/AddUser.vue'
import SecurityDialog from './components/SecurityDialog.vue'
import { getImageUrl } from '@/api/minio/minio'
import SearchComponents from './components/SearchComponents.vue'
import UserQuickView from '@/components/common/UserQuickView.vue'

// 生成更多测试数据
const tableData: UserType[] = reactive([])
// 分页相关逻辑
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const avatarUrls = ref<Record<string, string>>({})

// 表格行类名
const tableRowClassName = ({ row, rowIndex }: { row: UserType; rowIndex: number }) => {
  if (row.status === 0) {
    return 'disabled-row'
  }
  return rowIndex % 2 === 1 ? 'stripe-row' : ''
}

// 批量加载头像URL
const batchLoadAvatarUrls = async (users: UserType[]) => {
  const promises = users
    .filter(user => user.avatar && !avatarUrls.value[user.avatar])
    .map(async user => {
      try {
        const res = await getImageUrl(user.avatar as string)
        avatarUrls.value[user.avatar as string] = res.url
      } catch (error) {
        console.error(`加载头像失败: ${user.avatar}`, error)
      }
    })

  if (promises.length > 0) {
    await Promise.all(promises)
  }
}

// 修改数据获取方法，优化分页处理
const fetchData = async (page: number, size: number, params?: UserType) => {
  try {
    const res: PageInfo<UserType> = await getUserManageInfo(page, size, params);
    tableData.splice(0, tableData.length, ...res.content);
    tableData.forEach((item) => {
      item.status = Number(item.status);
    });
    total.value = res.totalElements;
    pageNum.value = res.number;
    pageSize.value = res.size;

    // 批量加载头像
    await batchLoadAvatarUrls(res.content);
  } catch (error) {
    console.error('获取用户数据失败:', error);
    ElMessage.error('获取用户数据失败');
  }
};

// 查询相关状态
const searchForm = reactive({
  name: '',
  status: undefined as number | undefined,
  roleId: undefined as number | undefined,
})

const roleOptions = ref<{ label: string; value: number }[]>([])
const roleLoading = ref(false)

// 统一的角色加载方法
const loadRoleOptions = async (roleName?: string) => {
  roleLoading.value = true
  try {
    const response = await getRoleList(roleName)
    const formattedResponse = response.map((item) => ({
      label: item.roleName ?? '',
      value: item.id ?? 0,
    }))
    roleOptions.value = formattedResponse
    return formattedResponse
  } catch (error) {
    console.error('加载角色数据失败:', error)
    ElMessage.error('加载角色数据失败')
    return []
  } finally {
    roleLoading.value = false
  }
}

// 远程搜索方法简化
const remoteRoleSearch = async (roleName: string) => {
  if (roleName) {
    await loadRoleOptions(roleName)
  } else {
    await loadRoleOptions()
  }
}

// 搜索处理
const handleSearch = (searchParams: UserType) => {
  pageNum.value = 1;
  const params = {
    name: searchParams.name || undefined,
    status: searchParams.status,
    roleId: searchParams.roleId
  };
  fetchData(pageNum.value, pageSize.value, params);
};

// 重置处理
const handleReset = () => {
  searchForm.name = ''
  searchForm.status = undefined
  searchForm.roleId = undefined
  fetchData(1, pageSize.value)
}

// 分别监听页码和页大小变化
watch(pageNum, (newPage, oldPage) => {
  if (newPage !== oldPage) {
    fetchData(newPage, pageSize.value, {
      name: searchForm.name || undefined,
      status: searchForm.status,
      roleId: searchForm.roleId,
    })
  }
})

watch(pageSize, (newSize, oldSize) => {
  if (newSize !== oldSize) {
    pageNum.value = 1 // 页大小变化时，重置为第一页
    fetchData(1, newSize, {
      name: searchForm.name || undefined,
      status: searchForm.status,
      roleId: searchForm.roleId,
    })
  }
})

// 修改onMounted中的调用
onMounted(async () => {
  await Promise.all([
    fetchData(1, 10),
    loadRoleOptions() // 使用统一方法加载角色
  ])
})

// 添加编辑相关状态
const editDialogVisible = ref(false)
const addDialogVisible = ref(false) // 添加用户对话框可见状态
const formData = ref<UserType>({
  id: 0,
  createDate: '',
  name: '',
  address: '',
  email: '',
  status: 1,
  roleName: 'user',
  introduction: '',
  roleOptions: [],
})

// 修改后的handleEdit方法
const handleEdit = (row: UserType) => {
  formData.value = JSON.parse(JSON.stringify(row)) // 深拷贝当前行数据
  editDialogVisible.value = true
}

// 优化删除用户相关逻辑
const deleteDialogVisible = ref(false)
const deletingUser = ref<UserType>({})
const deleteLoading = ref(false)

// 快速查看相关状态
const quickViewDialogVisible = ref(false)
const quickViewUserData = ref<UserType>({
  id: 0,
  createDate: '',
  name: '',
  address: '',
  email: '',
  status: 1,
  roleName: '',
  introduction: '',
  createTime: '',
  avatar: '',
})

const handleDelete = (row: UserType) => {
  ElMessageBox.confirm('确定要永久删除该用户吗？此操作不可恢复！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    deleteDialogVisible.value = true
    deletingUser.value = row
  }).catch(() => {
    // 用户取消删除，不做任何操作
  })
}

// 给script添加handleUserUpdated声明
const handleUserUpdated = async (updatedUser: UserType) => {
  // 更新本地数据
  const index = tableData.findIndex((item) => item.id === updatedUser.id)
  if (index !== -1) {
    tableData.splice(index, 1, updatedUser)
  }
  // 刷新数据
  await fetchData(pageNum.value, pageSize.value)
}

// 添加用户成功后的处理函数
const handleUserAdded = async () => {
  // 刷新表格数据
  await fetchData(pageNum.value, pageSize.value)
  ElMessage.success('用户添加成功')
}

// 添加确认删除方法
const handleConfirmDelete = async (password: string) => {
  if (!password) {
    ElMessage.error('请输入当前密码以确认操作')
    return
  }

  if (!deletingUser.value.id) {
    ElMessage.error('未选择要删除的用户')
    return
  }

  deleteLoading.value = true
  try {
    await deleteUser(deletingUser.value.id, password)
    await fetchData(pageNum.value, pageSize.value, {
      name: searchForm.name || undefined,
      status: searchForm.status,
      roleId: searchForm.roleId,
    })
    ElMessage.success('删除成功')
    resetDeleteState()
  } catch (error) {
    console.error('删除失败:', error)
    if (error instanceof Error && error.message === '密码错误') {
      ElMessage.error('密码错误，请重新输入')
    } else {
      ElMessage.error('删除失败，请检查密码是否正确')
    }
  } finally {
    deleteLoading.value = false
  }
}

// 重置删除状态
const resetDeleteState = () => {
  deleteDialogVisible.value = false
  deletingUser.value = {}
}

// 快速查看相关方法
const handleQuickView = (row: UserType) => {
  quickViewUserData.value = JSON.parse(JSON.stringify(row)) // 深拷贝
  quickViewDialogVisible.value = true
}

const handleQuickViewEdit = (userData: UserType) => {
  // 从快速查看跳转到编辑
  formData.value = JSON.parse(JSON.stringify(userData))
  editDialogVisible.value = true
}

const handleQuickViewClose = () => {
  quickViewDialogVisible.value = false
  quickViewUserData.value = {
    id: 0,
    createDate: '',
    name: '',
    address: '',
    email: '',
    status: 1,
    roleName: '',
    introduction: '',
    createTime: '',
    avatar: '',
  }
}
</script>

<style lang="scss" scoped>
// 导入样式系统
@use '@/assets/style/variables.scss' as *;
@use '@/assets/style/theme.scss' as *;

.user-management-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: var(--spacing-lg);

  // ==============================================
  // 页面头部样式
  // ==============================================
  .page-header {
    margin-bottom: var(--spacing-xl);

    .header-content {
      @include flex-between;
      align-items: flex-start;
      gap: var(--spacing-xl);

      // 响应式设计
      @media (max-width: 992px) {
        flex-direction: column;
        align-items: stretch;
        gap: var(--spacing-lg);
      }
    }

    .title-section {
      flex: 1;

      .page-title {
        @include flex-start;
        gap: var(--spacing-md);
        font-size: var(--font-size-xxxl);
        font-weight: var(--font-weight-bold);
        color: var(--text-primary);
        margin: 0 0 var(--spacing-sm) 0;

        .title-icon {
          font-size: var(--font-size-xxxl);
          color: var(--primary-color);
          background: var(--gradient-primary);
          background-clip: text;
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }
      }

      .page-subtitle {
        font-size: var(--font-size-lg);
        color: var(--text-secondary);
        margin: 0;
        font-weight: var(--font-weight-normal);
      }
    }

    .header-stats {
      @include flex-start;
      gap: var(--spacing-lg);

      // 响应式设计
      @media (max-width: 576px) {
        flex-direction: column;
        width: 100%;
      }

      .stat-card {
        @include glass-effect;
        border-radius: var(--radius-xl);
        padding: var(--spacing-lg);
        text-align: center;
        min-width: 120px;
        box-shadow: var(--shadow-md);
        border: 1px solid rgba(255, 255, 255, 0.3);
        transition: all var(--transition-normal) var(--transition-timing);
        position: relative;
        overflow: hidden;

        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: -100%;
          width: 100%;
          height: 100%;
          background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
          transition: left var(--transition-slow) var(--transition-timing);
        }

        &:hover {
          @include hover-lift;
          box-shadow: var(--shadow-xl);
          border-color: rgba(255, 255, 255, 0.5);

          &::before {
            left: 100%;
          }

          .stat-number {
            color: var(--primary-dark);
            transform: scale(1.1);
          }

          .stat-label {
            color: var(--text-primary);
          }
        }

        .stat-number {
          font-size: var(--font-size-xxl);
          font-weight: var(--font-weight-bold);
          color: var(--primary-color);
          margin-bottom: var(--spacing-xs);
          transition: all var(--transition-normal) var(--transition-timing);
          position: relative;
          z-index: 2;
        }

        .stat-label {
          font-size: var(--font-size-sm);
          color: var(--text-secondary);
          font-weight: var(--font-weight-medium);
          transition: color var(--transition-normal) var(--transition-timing);
          position: relative;
          z-index: 2;
        }
      }
    }
  }

  // ==============================================
  // 搜索区域样式
  // ==============================================
  .search-section {
    margin-bottom: var(--spacing-xl);

    .search-card {
      border-radius: var(--radius-xl);
      border: 1px solid var(--border-light);
      overflow: hidden;
      position: relative;

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 4px;
        background: var(--gradient-primary);
        z-index: 1;
      }

      :deep(.el-card__header) {
        background: var(--gradient-primary);
        border: none;
        padding: var(--spacing-lg) var(--spacing-xl);
        position: relative;

        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          height: 1px;
          background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.5), transparent);
        }

        .search-header {
          @include flex-between;

          .search-title {
            @include flex-start;
            gap: var(--spacing-sm);
            color: var(--text-inverse);
            font-size: var(--font-size-lg);
            font-weight: var(--font-weight-semibold);
            position: relative;

            .el-icon {
              font-size: var(--font-size-xl);
              animation: pulse 2s ease-in-out infinite;
            }
          }
        }
      }

      :deep(.el-card__body) {
        padding: var(--spacing-xl);
        background: var(--background-card);
        position: relative;

        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: radial-gradient(circle at 50% 0%, rgba(59, 130, 246, 0.05) 0%, transparent 50%);
          pointer-events: none;
        }
      }
    }
  }

  // ==============================================
  // 表格区域样式
  // ==============================================
  .table-section {
    .table-card {
      border-radius: var(--radius-xl);
      border: 1px solid var(--border-light);
      overflow: hidden;

      :deep(.el-card__header) {
        background: var(--background-card);
        border-bottom: 2px solid var(--border-light);
        padding: var(--spacing-lg) var(--spacing-xl);

        .table-header {
          @include flex-between;

          .header-left {
            @include flex-start;
            gap: var(--spacing-lg);

            .table-title {
              @include flex-start;
              gap: var(--spacing-sm);
              font-size: var(--font-size-xl);
              font-weight: var(--font-weight-semibold);
              color: var(--text-primary);
              margin: 0;

              .el-icon {
                color: var(--primary-color);
              }
            }

            .table-meta {
              @include flex-start;
              gap: var(--spacing-sm);
            }
          }

          .header-actions {
            @include flex-start;
            gap: var(--spacing-sm);

            .action-btn {
              width: 40px;
              height: 40px;
              border-radius: var(--radius-full);
              border: 1px solid var(--border-light);
              background: var(--background-secondary);
              color: var(--text-secondary);
              transition: all var(--transition-normal) var(--transition-timing);

              &:hover {
                background: var(--primary-color);
                border-color: var(--primary-color);
                color: var(--text-inverse);
                @include hover-lift;
              }
            }
          }
        }
      }

      :deep(.el-card__body) {
        padding: 0;
        background: var(--background-card);
      }
    }

    .table-container {
      padding: var(--spacing-xl);

      .user-table {
        border-radius: var(--radius-lg);
        overflow: hidden;
        border: 1px solid var(--border-light);

        // 表格头部样式
        :deep(.el-table__header-wrapper) {
          .el-table__header {
            th {
              background: linear-gradient(135deg, #f8fafe 0%, #f0f7ff 100%);
              color: var(--text-primary);
              font-weight: var(--font-weight-semibold);
              border-bottom: 2px solid var(--primary-light);
              padding: var(--spacing-lg) var(--spacing-md);
              font-size: var(--font-size-md);
            }
          }
        }

        // 表格行样式
        :deep(.el-table__body-wrapper) {
          .el-table__body {
            tr {
              transition: all var(--transition-normal) var(--transition-timing);

              &:hover {
                background-color: var(--primary-lighter) !important;
                transform: scale(1.001);
              }

              &.stripe-row {
                background-color: var(--background-secondary);
              }

              &.disabled-row {
                opacity: 0.6;
                background-color: rgba(239, 68, 68, 0.05);
              }

              td {
                padding: var(--spacing-lg) var(--spacing-md);
                border-bottom: 1px solid var(--border-light);
                vertical-align: middle;
              }
            }
          }
        }

        // 表格单元格样式
        .table-avatar {
          border: 2px solid var(--border-light);
          transition: all var(--transition-normal) var(--transition-timing);

          &:hover {
            border-color: var(--primary-color);
            @include hover-lift;
          }
        }

        .user-info-cell {
          .user-name-cell {
            font-weight: var(--font-weight-semibold);
            color: var(--text-primary);
            margin-bottom: var(--spacing-xs);
            font-size: var(--font-size-md);
          }

          .user-email-cell {
            color: var(--text-secondary);
            font-size: var(--font-size-sm);
          }
        }

        .status-cell {
          @include flex-center;
          gap: var(--spacing-sm);

          .status-indicator {
            width: 8px;
            height: 8px;
            border-radius: var(--radius-full);
            background-color: var(--error-color);
            transition: all var(--transition-normal) var(--transition-timing);

            &.active {
              background-color: var(--success-color);
              box-shadow: 0 0 10px rgba(16, 185, 129, 0.3);
            }
          }
        }

        .time-cell {
          @include flex-start;
          gap: var(--spacing-xs);
          color: var(--text-secondary);
          font-size: var(--font-size-sm);

          .el-icon {
            color: var(--primary-color);
          }
        }

        .action-buttons {
          @include flex-center;
          gap: var(--spacing-sm);

          .edit-btn,
          .delete-btn,
          .view-btn {
            width: 32px;
            height: 32px;
            transition: all var(--transition-normal) var(--transition-timing);
            position: relative;
            overflow: hidden;

            &::before {
              content: '';
              position: absolute;
              top: 50%;
              left: 50%;
              width: 0;
              height: 0;
              border-radius: var(--radius-full);
              background: rgba(255, 255, 255, 0.3);
              transition: all var(--transition-fast) var(--transition-timing);
              transform: translate(-50%, -50%);
            }

            &:hover {
              @include hover-lift;

              &::before {
                width: 100%;
                height: 100%;
              }
            }

            &:active {
              transform: scale(0.95);
            }
          }

          .view-btn:hover {
            background-color: var(--info-color);
            border-color: var(--info-color);
          }
        }
      }
    }

    // 分页区域样式
    .pagination-container {
      @include flex-between;
      padding: var(--spacing-xl);
      border-top: 1px solid var(--border-light);
      background: var(--background-secondary);

      // 响应式设计
      @media (max-width: 768px) {
        flex-direction: column;
        gap: var(--spacing-md);
        text-align: center;
      }

      .pagination-info {
        color: var(--text-secondary);
        font-size: var(--font-size-sm);
        font-weight: var(--font-weight-medium);
      }

      .table-pagination {
        :deep(.el-pagination__sizes),
        :deep(.el-pagination__jump) {
          .el-input .el-input__wrapper {
            border-radius: var(--radius-md);
            border: 1px solid var(--border-light);
          }
        }

        :deep(.btn-prev),
        :deep(.btn-next),
        :deep(.el-pager li) {
          border-radius: var(--radius-md);
          transition: all var(--transition-normal) var(--transition-timing);
          margin: 0 var(--spacing-xs);
          border: 1px solid var(--border-light);

          &:hover {
            background-color: var(--primary-color);
            border-color: var(--primary-color);
            color: var(--text-inverse);
            @include hover-lift;
          }

          &.is-active {
            background: var(--gradient-primary);
            border-color: var(--primary-color);
            color: var(--text-inverse);
          }
        }
      }
    }
  }

  // ==============================================
  // 展开行详情样式
  // ==============================================
  .user-detail-card {
    background: linear-gradient(135deg, #f8fafe 0%, #f0f7ff 100%);
    border-radius: var(--radius-lg);
    margin: var(--spacing-md);
    padding: var(--spacing-xl);
    border: 1px solid var(--border-light);

    .detail-header {
      margin-bottom: var(--spacing-xl);

      .avatar-section {
        @include flex-start;
        gap: var(--spacing-lg);

        .user-avatar-large {
          border: 3px solid var(--primary-color);
          box-shadow: var(--shadow-md);
        }

        .user-basic-info {
          .user-name {
            font-size: var(--font-size-xl);
            font-weight: var(--font-weight-bold);
            color: var(--text-primary);
            margin: 0 0 var(--spacing-sm) 0;
          }

          .status-tag-large {
            font-size: var(--font-size-md);
            padding: var(--spacing-sm) var(--spacing-lg);
          }
        }
      }
    }

    .detail-content {
      .info-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: var(--spacing-lg);
        margin-bottom: var(--spacing-xl);

        .info-item {
          background: var(--background-card);
          border-radius: var(--radius-md);
          padding: var(--spacing-lg);
          border: 1px solid var(--border-light);
          transition: all var(--transition-normal) var(--transition-timing);

          &:hover {
            @include hover-lift;
            box-shadow: var(--shadow-md);
          }

          .info-label {
            @include flex-start;
            gap: var(--spacing-sm);
            font-weight: var(--font-weight-semibold);
            color: var(--text-secondary);
            margin-bottom: var(--spacing-sm);
            font-size: var(--font-size-sm);

            .el-icon {
              color: var(--primary-color);
            }
          }

          .info-value {
            color: var(--text-primary);
            font-size: var(--font-size-md);
          }
        }
      }

      .introduction-section {
        background: var(--background-card);
        border-radius: var(--radius-md);
        padding: var(--spacing-lg);
        border: 1px solid var(--border-light);

        .section-title {
          @include flex-start;
          gap: var(--spacing-sm);
          font-weight: var(--font-weight-semibold);
          color: var(--text-primary);
          margin-bottom: var(--spacing-md);

          .el-icon {
            color: var(--primary-color);
          }
        }

        .introduction-content {
          color: var(--text-secondary);
          line-height: var(--line-height-relaxed);
          font-size: var(--font-size-md);
        }
      }
    }
  }

  // ==============================================
  // 响应式设计
  // ==============================================
  @media (max-width: 768px) {
    padding: var(--spacing-md);

    .page-header {
      .header-stats {
        .stat-card {
          min-width: auto;
          flex: 1;
        }
      }
    }

    .user-detail-card {
      .detail-header {
        .avatar-section {
          flex-direction: column;
          text-align: center;
          gap: var(--spacing-md);
        }
      }

      .detail-content {
        .info-grid {
          grid-template-columns: 1fr;
          gap: var(--spacing-md);
        }
      }
    }
  }
}

// ==============================================
// 全局表格展开行样式
// ==============================================
:deep(.el-table__expanded-cell) {
  padding: 0 !important;
  background: transparent !important;
}

// ==============================================
// 动画效果
// ==============================================
@keyframes slideInDown {
  from {
    transform: translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.8;
  }
}

@keyframes shimmer {
  0% {
    background-position: -200px 0;
  }
  100% {
    background-position: calc(200px + 100%) 0;
  }
}

@keyframes glow {
  0%, 100% {
    box-shadow: 0 0 5px rgba(59, 130, 246, 0.4);
  }
  50% {
    box-shadow: 0 0 20px rgba(59, 130, 246, 0.8);
  }
}

.user-management-container {
  animation: slideInDown var(--transition-slow) var(--transition-timing);

  // 为整个容器添加微妙的阴影动画
  &::before {
    content: '';
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: radial-gradient(circle at 20% 50%, rgba(59, 130, 246, 0.02) 0%, transparent 50%),
                radial-gradient(circle at 80% 20%, rgba(59, 130, 246, 0.02) 0%, transparent 50%),
                radial-gradient(circle at 40% 80%, rgba(59, 130, 246, 0.02) 0%, transparent 50%);
    pointer-events: none;
    z-index: -1;
  }
}
</style>
