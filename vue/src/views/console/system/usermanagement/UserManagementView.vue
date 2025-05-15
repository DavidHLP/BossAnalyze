<template>
  <div class="user-management-container">

    <div class="search-section">
      <el-card shadow="hover" class="search-card">
        <el-form :model="searchForm" class="search-form">
          <div class="form-row">
            <el-form-item label="姓名">
              <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable class="rounded-input" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="请选择状态" clearable class="rounded-input">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item label="角色">
              <el-select v-model="searchForm.roleId" filterable remote reserve-keyword placeholder="请输入角色名称"
                :remote-method="remoteRoleSearch" :loading="roleLoading" clearable class="rounded-input">
                <el-option v-for="role in roleOptions" :key="role.value" :label="role.label" :value="role.value" />
              </el-select>
            </el-form-item>
            <div class="search-buttons">
              <el-button type="primary" @click="handleSearch" round>搜索</el-button>
              <el-button @click="handleReset" round>重置</el-button>
            </div>
            <div class="search-buttons">
              <el-button type="primary" icon="Plus" class="add-user-btn" @click="addDialogVisible = true" round>添加用户</el-button>
            </div>
          </div>
        </el-form>
      </el-card>
    </div>

    <div class="table-section">
      <el-card shadow="hover" class="table-card">
        <div class="table-header">
          <span class="table-title">用户列表</span>
          <span class="record-count">共 {{ total }} 条记录</span>
        </div>
        <el-table :data="currentPageData" border class="user-table">
          <el-table-column type="expand">
            <template #default="props">
              <div class="user-detail">
                <div class="avatar-container">
                  <el-image :src="props.row.avatar || '/assets/default-avatar.png'" class="user-avatar"
                    :preview-src-list="[props.row.avatar]" fit="cover" />
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

    <el-dialog v-model="deleteDialogVisible" title="安全验证" width="400px" center class="blue-dialog">
      <div class="delete-warning">
        <i class="el-icon-warning warning-icon"></i>
        <p>删除操作不可恢复，请确认您的操作</p>
      </div>
      <el-form label-width="100px" class="delete-form">
        <el-form-item label="当前密码" required>
          <el-input v-model="password" type="password" placeholder="请输入您的登录密码以确认操作" show-password
            class="rounded-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="deleteDialogVisible = false" round>取消</el-button>
          <el-button type="danger" @click="handleConfirmDelete" round>确认删除</el-button>
        </div>
      </template>
    </el-dialog>

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
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElSelect,
  ElMessageBox,
  ElMessage,
  ElDescriptions,
  ElDescriptionsItem,
  ElTag,
  ElButton,
  ElCard,
} from 'element-plus'
import { getUserManageInfo } from '@/api/user/user'
import { computed, ref, onMounted, reactive, watch } from 'vue'
import type { UserBaseInfo as User } from '@/api/auth/auth.d'
import type { PageInfo } from '@/types/common'
import { getRoleList } from '@/api/role/role'
import { deleteUser } from '@/api/user/user'
import EditUser from './components/EditUser.vue'
import AddUser from './components/AddUser.vue'

// 生成更多测试数据
const tableData: User[] = reactive([])
// 分页相关逻辑
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const currentPageData = computed(() => {
  return tableData.slice((pageNum.value - 1) * pageSize.value, pageNum.value * pageSize.value)
})

// 查询相关状态
const searchForm = reactive({
  name: '',
  status: '',
  roleId: undefined as number | undefined,
})

const roleOptions = ref<{ label: string; value: number }[]>([]) // 单独的角色选项列表

// 修改后的数据获取方法
const fetchData = async (page: number, size: number, params?: User) => {
  const res: PageInfo<User> = await getUserManageInfo(page, size, params)
  tableData.splice(0, tableData.length, ...res.content)
  tableData.forEach((item) => {
    item.status = Number(item.status)
  })
  total.value = res.totalElements
  pageNum.value = res.number
  pageSize.value = res.size
}

// 搜索处理
const handleSearch = () => {
  pageNum.value = 1
  fetchData(pageNum.value, pageSize.value, {
    name: searchForm.name ?? undefined,
    status: searchForm.status ?? undefined,
    roleId: searchForm.roleId ?? undefined,
  })
}

// 重置处理
const handleReset = () => {
  searchForm.name = ''
  searchForm.status = ''
  searchForm.roleId = undefined
  fetchData(1, pageSize.value)
}

// 分别监听页码和页大小变化
watch(pageNum, (newPage, oldPage) => {
  if (newPage !== oldPage) {
    fetchData(newPage, pageSize.value, {
      name: searchForm.name || undefined,
      status: searchForm.status || undefined,
      roleId: searchForm.roleId,
    })
  }
})

watch(pageSize, (newSize, oldSize) => {
  if (newSize !== oldSize) {
    pageNum.value = 1 // 页大小变化时，重置为第一页
    fetchData(1, newSize, {
      name: searchForm.name || undefined,
      status: searchForm.status || undefined,
      roleId: searchForm.roleId,
    })
  }
})

// 修改onMounted中的调用
onMounted(async () => {
  await fetchData(1, 10)
  // 初始化时加载角色选项
  const initialRolesResponse = await getRoleList()
  roleOptions.value = initialRolesResponse.map((item) => ({
    label: item.roleName ?? '',
    value: item.id ?? 0,
  }))
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

const deleteDialogVisible = ref(false)
const deletingUser = ref<User>({})
const password = ref('')

const handleDelete = (row: User) => {
  ElMessageBox.confirm('确定要永久删除该用户吗？此操作不可恢复！', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    deleteDialogVisible.value = true
    deletingUser.value = row
  })
}

// 添加远程搜索相关状态
const roleLoading = ref(false)

// 远程搜索方法
const remoteRoleSearch = async (roleName: string) => {
  if (roleName) {
    roleLoading.value = true
    try {
      // 这里替换为实际的API调用
      const response = await getRoleList(roleName)
      const formattedResponse = response.map((item) => ({
        label: item.roleName ?? '',
        value: item.id ?? 0,
      }))

      // 同时更新顶部搜索表单和编辑对话框中的角色选项
      roleOptions.value = formattedResponse

      // 如果当前正在编辑，也更新编辑表单中的角色选项
      if (editDialogVisible.value && formData.value.roleOptions) {
        formData.value.roleOptions = [...formattedResponse]
      }
    } finally {
      roleLoading.value = false
    }
  }
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
const handleConfirmDelete = async () => {
  try {
    if (!password.value) {
      ElMessage.error('请输入当前密码以确认操作')
      return
    }

    if (!deletingUser.value) {
      ElMessage.error('未选择要删除的用户')
      return
    }

    await deleteUser(deletingUser.value.id ?? 0, password.value)

    const index = tableData.findIndex((item) => item.id === deletingUser.value?.id)
    if (index !== -1) {
      tableData.splice(index, 1)
      total.value -= 1
    }

    ElMessage.success('删除成功')
    resetDeleteState()
  } catch (error) {
    console.error('删除失败:', error)
    handleDeleteError(error)
  }
}

// 重置删除状态
const resetDeleteState = () => {
  deleteDialogVisible.value = false
  password.value = ''
  deletingUser.value = {}
}

// 处理删除错误
const handleDeleteError = (error: unknown) => {
  if (error instanceof Error && error.message === '密码错误') {
    ElMessage.error('密码错误，请重新输入')
  } else {
    ElMessage.error('删除失败，请检查密码是否正确')
  }
}
</script>

<style lang="scss">
.user-management-container {
  padding: var(--content-padding);

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    .page-title {
      font-size: 24px;
      color: var(--text-primary);
      font-weight: 600;
      margin: 0;
    }

    .add-user-btn {
      background-color: $primary-color;
      border-color: $primary-color;

      &:hover {
        background-color: $primary-dark;
        border-color: $primary-dark;
      }
    }
  }

  .search-section {
    margin-bottom: 24px;
  }

  .search-card {
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

  .search-form {
    padding: 20px;

    .form-row {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      align-items: center;

      .el-form-item {
        margin-bottom: 0;
        flex: 1;
        min-width: 200px;
      }
    }
  }

  .search-buttons {
    display: flex;
    gap: 12px;
    margin-left: auto;
    flex-shrink: 0;

    .el-button {
      min-width: 90px;
    }
  }

  .table-section {
    margin-bottom: 24px;
  }

  .table-card {
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

  .table-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;

    .table-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
    }

    .record-count {
      color: var(--text-secondary);
      font-size: 14px;
    }
  }

  .user-table {
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

  .user-detail {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 20px;

    .avatar-container {
      display: flex;
      justify-content: center;
      margin-bottom: 10px;
    }

    .user-avatar {
      width: 100px;
      height: 100px;
      border-radius: 50%;
      object-fit: cover;
      border: 3px solid $primary-light;
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
    }

    .expanded-info {
      .el-descriptions__label {
        font-weight: 500;
      }
    }
  }

  .status-tag {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: $error-color;

      &.active {
        background-color: $success-color;
        box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2);
      }
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

  .pagination-container {
    display: flex;
    justify-content: flex-end;
    padding: 16px 20px;
    border-top: 1px solid #f0f0f0;
  }

  .blue-dialog {
    .el-dialog__header {
      background-color: $primary-color;
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

  .delete-warning {
    display: flex;
    align-items: center;
    gap: 12px;
    background-color: #fff8f8;
    padding: 16px;
    border-radius: 8px;
    margin-bottom: 20px;

    .warning-icon {
      color: $warning-color;
      font-size: 24px;
    }

    p {
      margin: 0;
      color: var(--text-primary);
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;

    .el-button {
      min-width: 100px;
    }
  }

  .rounded-input {

    .el-textarea__inner {
      border-radius: 20px !important;
    }

    &.el-select .el-input__wrapper {
      border-radius: 20px !important;
    }
  }

  @media (max-width: 768px) {
    .search-form .form-row {
      flex-direction: column;

      .el-form-item {
        width: 100%;
        margin-right: 0;
      }

      .search-buttons {
        width: 100%;
        margin-top: 12px;
        justify-content: flex-end;
      }
    }

    .user-detail {
      flex-direction: column;
    }
  }

  .full-width {
    width: 100%;
  }
}
</style>
