<template>
  <div class="user-management-container">
    <div class="content-card">
      <div class="search-form-container">
        <el-form :model="searchForm" class="search-form">
          <div class="form-inputs-row">
            <el-form-item label="姓名" class="form-item double-width">
              <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
            </el-form-item>
            <el-form-item label="状态" class="form-item equal-width">
              <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
                <el-option label="启用" value="1" />
                <el-option label="禁用" value="0" />
              </el-select>
            </el-form-item>
            <el-form-item label="角色" class="form-item double-width">
              <el-select
                v-model="searchForm.roleId"
                filterable
                remote
                reserve-keyword
                placeholder="请输入角色名称"
                :remote-method="remoteRoleSearch"
                :loading="roleLoading"
                clearable
              >
                <el-option
                  v-for="role in roleOptions"
                  :key="role.value"
                  :label="role.label"
                  :value="role.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item class="form-item equal-width action-item">
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </div>
        </el-form>
      </div>

      <el-table :data="currentPageData" class="user-table" border>
        <el-table-column type="expand">
          <template #default="props">
            <el-descriptions title="详细信息" border class="user-details">
              <el-descriptions-item label="头像">
                <el-image :src="props.row.avatar" class="user-avatar" />
              </el-descriptions-item>
              <el-descriptions-item label="地址">{{ props.row.address }}</el-descriptions-item>
              <el-descriptions-item label="电子邮件">{{ props.row.email }}</el-descriptions-item>
              <el-descriptions-item label="个人简介">
                {{ props.row.introduction }}
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-table-column>
        <el-table-column label="创建日期">
          <template #default="scope">{{ scope.row.createTime }}</template>
        </el-table-column>
        <el-table-column property="name" label="姓名" />
        <el-table-column property="roleName" label="角色" />
        <el-table-column label="状态">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 1" type="success" effect="dark">启用</el-tag>
            <el-tag v-else type="danger" effect="dark">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="handleEdit(scope.row)" plain>编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete(scope.row)" plain>删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="editDialogVisible" title="编辑用户" class="user-dialog" width="600px">
        <el-form :model="formData" label-width="80px" class="edit-form">
          <el-form-item label="姓名">
            <el-input v-model="formData.name" />
          </el-form-item>
          <el-form-item label="角色">
            <el-select
              v-model="formData.roleId"
              filterable
              remote
              reserve-keyword
              placeholder="请输入角色名称"
              :remote-method="remoteRoleSearch"
              :loading="roleLoading"
            >
              <el-option
                v-for="role in formData.roleOptions"
                :key="role.value"
                :label="role.label"
                :value="role.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="formData.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="地址">
            <el-input v-model="formData.address" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="formData.email" />
          </el-form-item>
          <el-form-item label="个人简介">
            <el-input v-model="formData.introduction" type="textarea" :rows="4" />
          </el-form-item>
          <el-form-item label="头像" class="avatar-form-item">
            <el-upload
              class="avatar-uploader"
              action="/api/upload/avatar"
              :on-success="handleAvatarSuccess"
              :file-list="formData.avatar ? [{ name: 'avatar', url: formData.avatar }] : []"
              list-type="picture-card"
            >
              <i v-if="!formData.avatar" class="el-icon-plus"></i>
            </el-upload>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="editDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleConfirmEdit">确认</el-button>
          </div>
        </template>
      </el-dialog>

      <el-dialog v-model="deleteDialogVisible" title="安全验证" width="400px" class="delete-dialog">
        <el-form label-width="100px" class="password-form">
          <el-form-item label="当前密码" required>
            <el-input
              v-model="password"
              type="password"
              placeholder="请输入您的登录密码以确认操作"
              show-password
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="deleteDialogVisible = false">取消</el-button>
            <el-button type="danger" @click="handleConfirmDelete">确认删除</el-button>
          </div>
        </template>
      </el-dialog>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
        class="pagination"
      />
    </div>
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
  ElRadioGroup,
  ElRadio,
  ElMessageBox,
  ElMessage,
} from 'element-plus'
import { getUserManageInfo } from '@/api/user/user'
import { computed, ref, onMounted, reactive } from 'vue'
import type { UserBaseInfo as User } from '@/api/auth/auth.d'
import type { PageInfo } from '@/utils/types/common'
import { getRoleList } from '@/api/role/role'
import { deleteUser, updateUser } from '@/api/user/user'

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
  status: '1',
  roleId: undefined as number | undefined,
})

const roleOptions = ref<{ label: string; value: number }[]>([]) // 单独的角色选项列表

// 修改后的数据获取方法
const fetchData = async (page: number, size: number, params?: User) => {
  const res: PageInfo<User> = await getUserManageInfo(page, size, params)
  tableData.splice(0, tableData.length, ...res.items)
  total.value = res.total
  pageNum.value = res.pageNum
  pageSize.value = res.pageSize
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

const handleCurrentChange = (val: number) => {
  pageNum.value = val
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  pageNum.value = 1
}

// 添加编辑相关状态
const editDialogVisible = ref(false)
const formData = ref<User>({
  id: 0,
  createDate: '',
  name: '',
  address: '',
  email: '',
  status: '1',
  roleName: 'user',
  introduction: '',
  roleOptions: [],
})

// 修改后的handleEdit方法
const handleEdit = (row: User) => {
  formData.value = JSON.parse(JSON.stringify(row)) // 深拷贝当前行数据
  // 确保 roleOptions 已初始化
  if (!formData.value.roleOptions) {
    formData.value.roleOptions = []
  }
  // 将全局角色选项复制到表单的角色选项中
  formData.value.roleOptions = [...roleOptions.value]
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

// 添加确认编辑方法
const handleConfirmEdit = async () => {
  try {
    // 这里替换为实际的API调用
    await updateUser(formData.value)

    // 更新本地数据
    const index = tableData.findIndex((item) => item.id === formData.value.id)
    if (index !== -1) {
      tableData.splice(index, 1, formData.value)
    }

    ElMessage.success('用户信息更新成功')
    await fetchData(pageNum.value, pageSize.value)
    editDialogVisible.value = false
  } catch (error) {
    console.error('更新失败:', error)
    ElMessage.error('更新失败，请稍后重试')
  }
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

// 添加头像上传成功处理方法
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handleAvatarSuccess = (response: any, file: any) => {
  // 假设上传成功后，服务器返回的response中包含文件URL
  formData.value.avatar = response.url || file.url
  ElMessage.success('头像上传成功')
}
</script>

<style lang="scss">
/* 基础容器样式 */
.user-management-container {
  padding: 24px;
  width: 100%;
  background-color: #f8f9fc;
  min-height: 100vh;
}

.content-card {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  padding: 24px;
  transition: all 0.3s ease;
}

/* 搜索表单相关 */
.search-form-container {
  margin-bottom: 24px;
  padding: 20px;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(240, 249, 255, 0.8), rgba(240, 249, 255, 0.4));
  backdrop-filter: blur(10px);
  border: 1px solid rgba(66, 184, 131, 0.2);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
    transform: translateY(-2px);
  }
}

.form-inputs-row {
  display: flex;
  gap: 16px;
  margin-bottom: 15px;
  flex-wrap: wrap;
  align-items: flex-end;
}

.form-item {
  &.equal-width {
    flex: 1;
    min-width: 150px;
  }

  &.double-width {
    flex: 2;
    min-width: 300px;
  }

  &.action-item {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
}

/* 按钮样式增强 */
.el-button {
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.3s;

  &.el-button--primary {
    background: linear-gradient(45deg, #42b883, #35495e);
    border: none;

    &:hover {
      box-shadow: 0 4px 12px rgba(66, 184, 131, 0.4);
      transform: translateY(-1px);
    }

    &.is-plain {
      background: transparent;
      border: 1px solid #42b883;
      color: #42b883;

      &:hover {
        background-color: rgba(66, 184, 131, 0.1);
      }
    }
  }

  &.el-button--danger {
    &:hover {
      box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);
    }

    &.is-plain {
      &:hover {
        background-color: rgba(245, 108, 108, 0.1);
      }
    }
  }
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 10px;
}

/* 表单控件样式 */
.el-input, .el-select {
  .el-input__inner {
    border-radius: 6px;
    transition: all 0.3s;
  }

  &:hover .el-input__inner {
    border-color: #42b883;
  }

  &:focus-within .el-input__inner {
    box-shadow: 0 0 0 2px rgba(66, 184, 131, 0.2);
  }
}

/* 头像 */
.user-avatar {
  width: 70px;
  height: 70px;
  object-fit: cover;
  border-radius: 8px;
  transition: all 0.3s;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);

  &:hover {
    transform: scale(1.08);
    box-shadow: 0 6px 15px rgba(0, 0, 0, 0.15);
  }
}

/* 头像上传组件 */
.avatar-uploader :deep(.el-upload--picture-card) {
  width: 110px;
  height: 110px;
  border-radius: 10px;
  transition: all 0.3s;
  border: 2px dashed rgba(66, 184, 131, 0.4);
  background-color: rgba(66, 184, 131, 0.05);

  &:hover {
    border-color: #42b883;
    background-color: rgba(66, 184, 131, 0.1);
    transform: scale(1.03);
  }
}

/* 分页器 */
.pagination {
  margin-top: 26px;
  text-align: right;
  display: flex;
  justify-content: flex-end;
}

/* 编辑表单 */
.edit-form {
  padding: 5px 15px;

  :deep(.el-form-item__label) {
    font-weight: 500;
    color: #606266;
  }

  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
}

/* 对话框样式增强 */
.user-dialog, .delete-dialog {
  max-width: 95vw;

  :deep(.el-dialog__header) {
    padding: 20px 24px;
    margin: 0;
    border-bottom: 1px solid #f0f0f0;
  }

  :deep(.el-dialog__body) {
    padding: 24px;
  }

  :deep(.el-dialog__footer) {
    padding: 16px 24px;
    border-top: 1px solid #f0f0f0;
  }

  :deep(.el-dialog__title) {
    font-weight: 600;
    color: #303133;
    font-size: 18px;
  }

  :deep(.el-dialog__headerbtn) {
    top: 20px;
    right: 20px;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 用户表格 */
.user-table {
  --el-table-header-bg-color: rgba(66, 184, 131, 0.08);
  --el-table-row-hover-bg-color: rgba(66, 184, 131, 0.05);
  --el-table-border-color: #ebeef5;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);

  :deep(th) {
    font-weight: 600;
    color: #454d64;
    padding: 16px 12px;
  }

  :deep(td) {
    padding: 14px 12px;
  }

  :deep(.el-table__row) {
    transition: all 0.2s ease;
  }

  :deep(.el-table__expanded-cell) {
    padding: 20px 50px;
    background-color: rgba(250, 250, 250, 0.7);
  }
}

/* 标签样式优化 */
.el-tag {
  font-weight: 500;
  padding: 4px 8px;
  border: none;
  border-radius: 4px;

  &.el-tag--success {
    background: rgba(103, 194, 58, 0.1);
    color: #67c23a;
  }

  &.el-tag--danger {
    background: rgba(245, 108, 108, 0.1);
    color: #f56c6c;
  }
}

/* 用户详情 */
.user-details {
  padding: 16px;
  margin-top: 10px;
  border-radius: 8px;
  background: white;

  :deep(.el-descriptions__title) {
    font-weight: 600;
    font-size: 16px;
    margin-bottom: 16px;
    color: #303133;
  }

  :deep(.el-descriptions__cell) {
    padding: 12px 16px;
  }

  :deep(.el-descriptions__label) {
    font-weight: 500;
    color: #606266;
  }
}

/* 响应式样式 */
@media screen and (max-width: 768px) {
  .user-management-container {
    padding: 16px;
  }

  .content-card {
    padding: 16px;
    border-radius: 8px;
  }

  .search-form-container {
    padding: 16px;
  }

  .form-inputs-row {
    flex-direction: column;
    gap: 10px;
  }

  .form-item {
    &.equal-width,
    &.double-width {
      width: 100%;
      min-width: auto;
    }

    &.action-item {
      justify-content: flex-start;
      margin-top: 8px;
    }
  }

  .user-dialog {
    :deep(.el-dialog__body) {
      padding: 16px;
    }
  }
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .user-management-container {
    background-color: #1a1a1a;
  }

  .content-card {
    background-color: #252525;
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  }

  .search-form-container {
    background: linear-gradient(135deg, rgba(35, 35, 35, 0.8), rgba(45, 45, 45, 0.6));
    border: 1px solid rgba(66, 184, 131, 0.15);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  }

  .user-table {
    --el-table-header-bg-color: rgba(66, 184, 131, 0.15);
    --el-table-bg-color: #252525 !important;
    --el-table-tr-bg-color: #252525 !important;
    --el-table-row-hover-bg-color: rgba(66, 184, 131, 0.08);
    --el-table-border-color: #383838;
    --el-table-text-color: #e0e0e0;

    :deep(th) {
      background-color: rgba(66, 184, 131, 0.15) !important;
      color: #e0e0e0;
      border-bottom-color: #383838;
    }

    :deep(td) {
      border-bottom-color: #383838;
    }

    :deep(.el-table__expanded-cell) {
      background-color: rgba(45, 45, 45, 0.8);
    }
  }

  .el-input :deep(.el-input__inner),
  .el-select :deep(.el-input__inner),
  .el-textarea :deep(.el-textarea__inner) {
    background-color: #333;
    border-color: #444;
    color: #e0e0e0;
  }

  .user-details {
    background: #2c2c2c;
    border: 1px solid #383838;

    :deep(.el-descriptions__title) {
      color: #e0e0e0;
    }

    :deep(.el-descriptions__cell) {
      border-color: #383838;
    }

    :deep(.el-descriptions__label) {
      color: #b0b0b0;
    }
  }

  .user-dialog, .delete-dialog {
    :deep(.el-dialog__header),
    :deep(.el-dialog__footer) {
      border-color: #383838;
    }

    :deep(.el-dialog__title) {
      color: #e0e0e0;
    }
  }

  .user-avatar {
    border: 2px solid rgba(66, 184, 131, 0.2);
    box-shadow: 0 3px 10px rgba(0, 0, 0, 0.3);
  }

  .avatar-uploader :deep(.el-upload--picture-card) {
    background-color: rgba(66, 184, 131, 0.1);
    border-color: rgba(66, 184, 131, 0.3);
  }

  .el-button:not(.el-button--primary):not(.el-button--danger) {
    background-color: #333;
    border-color: #444;
    color: #e0e0e0;

    &:hover {
      background-color: #3a3a3a;
      border-color: #505050;
    }
  }

  .el-tag {
    &.el-tag--success {
      background: rgba(103, 194, 58, 0.15);
    }

    &.el-tag--danger {
      background: rgba(245, 108, 108, 0.15);
    }
  }
}
</style>
