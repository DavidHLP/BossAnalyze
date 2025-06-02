<template>
  <div class="user-management-container">

    <div class="search-section">
      <SearchComponents
        v-model:searchForm="searchForm"
        :roleOptions="roleOptions"
        :roleLoading="roleLoading"
        :remoteRoleSearch="remoteRoleSearch"
        @search="handleSearch"
        @reset="handleReset"
        @add-user="addDialogVisible = true"
      />
    </div>

    <div class="table-section">
      <el-card shadow="hover" class="table-card">
        <div class="table-header">
          <span class="table-title">用户列表</span>
          <span class="record-count">共 {{ total }} 条记录</span>
        </div>
        <el-table :data="tableData" border class="user-table">
          <el-table-column type="expand">
            <template #default="props">
              <div class="user-detail">
                <div class="avatar-container">
                  <el-image :src="avatarUrls[props.row.avatar] || ''" class="user-avatar"
                    :preview-src-list="avatarUrls[props.row.avatar] ? [avatarUrls[props.row.avatar]] : []" fit="cover" preview-teleported />
                </div>
                <el-descriptions title="详细信息" border class="expanded-info" :column="2">
                  <el-descriptions-item label="姓名">{{ props.row.name }}</el-descriptions-item>
                  <el-descriptions-item label="角色">{{ props.row.roleName }}</el-descriptions-item>
                  <el-descriptions-item label="地址">{{ props.row.address || '暂无' }}</el-descriptions-item>
                  <el-descriptions-item label="电子邮件">{{ props.row.email || '暂无' }}</el-descriptions-item>
                  <el-descriptions-item label="创建时间">{{ props.row.createTime }}</el-descriptions-item>
                  <el-descriptions-item label="状态">
                    <el-tag :type="props.row.status === 1 ? 'success' : 'danger'" effect="light" round>
                      {{ props.row.status === 1 ? '启用' : '禁用' }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="个人简介" :span="2">
                    {{ props.row.introduction || '暂无个人简介' }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="创建日期">
            <template #default="scope">{{ scope.row.createTime }}</template>
          </el-table-column>
          <el-table-column property="name" label="姓名" />
          <el-table-column label="邮箱">
            <template #default="scope">{{ scope.row.email }}</template>
          </el-table-column>
          <el-table-column property="roleName" label="角色" />
          <el-table-column label="状态" align="center">
            <template #default="scope">
              <div class="status-tag">
                <span class="status-dot" :class="{ 'active': scope.row.status === 1 }"></span>
                <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" effect="light" size="small" round>
                  {{ scope.row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center">
            <template #default="scope">
              <div class="action-buttons">
                <el-tooltip content="编辑用户" placement="top">
                  <el-button size="small" text circle @click="handleEdit(scope.row)" class="action-button">
                    <el-icon color="#409efc"><Edit /></el-icon>
                  </el-button>
                </el-tooltip>

                <el-tooltip content="删除用户" placement="top">
                  <el-button size="small" type="danger" text circle @click="handleDelete(scope.row)" class="action-button">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next" :total="total" background />
        </div>
      </el-card>
    </div>

    <security-dialog
      v-model:visible="deleteDialogVisible"
      :loading="deleteLoading"
      @confirm="handleConfirmDelete"
      @cancel="resetDeleteState"
    />

    <!-- 使用编辑用户组件 -->
    <edit-user v-model:visible="editDialogVisible" :user-data="formData" @user-updated="handleUserUpdated" />

    <!-- 添加用户组件 -->
    <add-user v-model:visible="addDialogVisible" @user-added="handleUserAdded" />
  </div>
</template>

<script lang="ts" setup name="UserManagementComponent">
import {
  ElTable,
  ElPagination,
  ElMessageBox,
  ElMessage,
  ElDescriptions,
  ElDescriptionsItem,
  ElTag,
  ElButton,
  ElCard,
} from 'element-plus'
import { getUserManageInfo } from '@/api/user/user'
import { ref, onMounted, reactive, watch } from 'vue'
import type { UserBaseInfo as User } from '@/api/auth/auth.d'
import type { PageInfo } from '@/types/common'
import { getRoleList } from '@/api/role/role'
import { deleteUser } from '@/api/user/user'
import EditUser from './components/EditUser.vue'
import AddUser from './components/AddUser.vue'
import SecurityDialog from './components/SecurityDialog.vue'
import { getImageUrl } from '@/api/minio/minio'
import SearchComponents from './components/SearchComponents.vue'

// 生成更多测试数据
const tableData: User[] = reactive([])
// 分页相关逻辑
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const avatarUrls = ref<Record<string, string>>({})

// 批量加载头像URL
const batchLoadAvatarUrls = async (users: User[]) => {
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
const fetchData = async (page: number, size: number, params?: User) => {
  try {
    const res: PageInfo<User> = await getUserManageInfo(page, size, params);
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
const handleSearch = (searchParams: User) => {
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
const formData = ref<User>({
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
const handleEdit = (row: User) => {
  formData.value = JSON.parse(JSON.stringify(row)) // 深拷贝当前行数据
  editDialogVisible.value = true
}

// 优化删除用户相关逻辑
const deleteDialogVisible = ref(false)
const deletingUser = ref<User>({})
const deleteLoading = ref(false)

const handleDelete = (row: User) => {
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
const handleUserUpdated = async (updatedUser: User) => {
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
</script>

<style lang="scss">
.user-management-container {
  padding: 16px;
  height: 100%;

  .search-section {
    margin-bottom: 20px;
  }

  .table-section {
    .table-card {
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

      .table-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;

        .table-title {
          font-size: 18px;
          font-weight: 600;
          color: #303133;
        }

        .record-count {
          color: #909399;
          font-size: 14px;
        }
      }

      .user-table {
        border-radius: 8px;
        overflow: hidden;

        .status-tag {
          display: flex;
          align-items: center;
          justify-content: center;

          .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #F56C6C;
            margin-right: 8px;

            &.active {
              background-color: #67C23A;
            }
          }
        }

        .action-buttons {
          display: flex;
          justify-content: center;
          gap: 10px;

          .action-button {
            &:hover {
              background-color: #f5f7fa;
            }
          }
        }

        .user-detail {
          padding: 20px;
          display: flex;

          .avatar-container {
            margin-right: 20px;

            .user-avatar {
              width: 100px;
              height: 100px;
              border-radius: 50%;
              object-fit: cover;
              border: 2px solid #ebeef5;
            }
          }

          .expanded-info {
            flex: 1;
          }
        }
      }

      .pagination-container {
        display: flex;
        justify-content: flex-end;
        margin-top: 20px;
      }
    }
  }
}

.el-dialog {
  border-radius: 8px;
  overflow: hidden;

  .el-dialog__header {
    border-bottom: 1px solid #f0f0f0;
  }

  .el-dialog__footer {
    border-top: 1px solid #f0f0f0;
  }
}

// 表格展开行样式
.el-table__expanded-cell {
  padding: 0 !important;
}

// 美化标签
.el-tag {
  border-radius: 20px;
  padding: 0 12px;
}

// 美化分页器
.el-pagination {
  .el-pagination__sizes {
    margin-right: 15px;
  }

  button {
    background-color: #f5f7fa;
    border-radius: 4px;
    margin: 0 3px;

    &:hover {
      color: #409EFF;
    }

    &.is-active {
      background-color: #409EFF;
      color: #fff;
    }
  }
}

// 描述列表样式
.el-descriptions {
  margin-top: 10px;

  .el-descriptions__label {
    color: #606266;
    font-weight: bold;
  }

  .el-descriptions__content {
    color: #303133;
  }
}
</style>
