<template>
  <div class="blog-tag">
    <el-card class="tag-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <h3>博客标签管理</h3>
          <el-button type="primary" @click="openCreateDialog" class="create-btn">
            <el-icon><Plus /></el-icon>
            新建标签
          </el-button>
        </div>
      </template>

      <div class="search-bar">
        <el-row :gutter="20">
          <el-col :xs="24" :md="16">
            <el-input
              v-model="searchQuery"
              placeholder="搜索标签名称"
              clearable
              @input="onSearch"
              class="search-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :md="4">
            <el-button type="info" @click="loadTags" class="refresh-btn">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </el-col>
        </el-row>
      </div>

      <!-- 标签统计信息 -->
      <div class="stats-row" v-if="tags.length > 0">
        <el-alert type="info" :closable="false">
          <el-icon><InfoFilled /></el-icon>
          <span class="stats-text">共有 <strong>{{ totalItems }}</strong> 个标签</span>
        </el-alert>
      </div>

      <!-- 标签卡片网格 -->
      <div v-if="viewMode === 'grid'" class="tag-grid">
        <el-empty v-if="tags.length === 0" description="暂无标签" />
        <div v-else class="tag-grid-container">
          <el-card
            v-for="tag in tags"
            :key="tag.id"
            class="tag-item-card"
            :body-style="{ backgroundColor:'#23362d', color: '#fff' }"
            shadow="hover"
          >
            <div class="tag-item-content">
              <div class="tag-item-header">
                <h4 class="tag-name">{{ tag.name }}</h4>
                <div class="tag-actions">
                  <el-button
                    type="info"
                    size="small"
                    circle
                    @click.stop="openEditDialog(tag)"
                    class="tag-action-btn"
                  >
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    circle
                    @click.stop="confirmDelete(tag)"
                    class="tag-action-btn"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>

              <div class="tag-item-details">
                <p v-if="tag.description" class="tag-description">{{ tag.description }}</p>
                <div class="tag-meta">
                  <span class="tag-count">
                    <el-icon><Document /></el-icon> {{ tag.count || 0 }} 篇文章
                  </span>
                  <span class="tag-date">
                    <el-icon><Calendar /></el-icon> {{ formatDateTime(tag.createTime) }}
                  </span>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 标签列表表格 -->
      <el-table
        v-if="viewMode === 'table'"
        :data="tags"
        style="width: 100%"
        v-loading="loading"
        border
        class="tag-table"
      >
        <el-table-column label="标签名称" prop="name" sortable>
          <template #default="{ row }">
            <el-tag :color="row.color" effect="dark">{{ row.name }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="描述" prop="description" show-overflow-tooltip />

        <el-table-column label="创建时间" prop="createTime" sortable>
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="文章数" prop="count" sortable>
          <template #default="{ row }">
            <el-badge :value="row.count || 0" type="primary" class="article-count-badge" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              circle
              @click="openEditDialog(row)"
            >
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button
              type="danger"
              size="small"
              circle
              @click="confirmDelete(row)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 视图切换和分页 -->
      <div class="table-footer">
        <div class="view-toggle">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="grid">
              <el-icon><Grid /></el-icon> 网格视图
            </el-radio-button>
            <el-radio-button label="table">
              <el-icon><List /></el-icon> 表格视图
            </el-radio-button>
          </el-radio-group>
        </div>

        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalItems"
          @size-change="loadTags"
          @current-change="loadTags"
          class="pagination"
        />
      </div>
    </el-card>

    <!-- 新建/编辑标签对话框 -->
    <el-dialog
      v-model="dialog"
      :title="isEdit ? '编辑标签' : '新建标签'"
      width="600px"
      class="tag-dialog"
    >
      <el-form ref="formRef" :model="editedItem" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :xs="24" :md="14">
            <el-form-item label="标签名称" prop="name">
              <el-input v-model="editedItem.name" placeholder="输入标签名称" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="10">
            <el-form-item label="标签颜色">
              <el-color-picker
                v-model="editedItem.color"
                show-alpha
                class="color-picker"
              />
              <div class="color-preview" :style="{ backgroundColor: editedItem.color || '#409EFF' }">
                <span class="preview-text">{{ editedItem.name || '预览' }}</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标签描述">
          <el-input
            v-model="editedItem.description"
            type="textarea"
            :rows="3"
            placeholder="输入标签描述信息（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeDialog">取消</el-button>
          <el-button type="primary" @click="saveTag" :loading="saving">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 确认删除对话框 -->
    <el-dialog
      v-model="deleteDialog"
      title="确认删除"
      width="400px"
      class="delete-dialog"
    >
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        title="危险操作"
        description="删除标签将影响已使用该标签的博客文章"
        class="delete-warning"
      />
      <div class="delete-confirm-content">
        <p>确定要删除标签 <strong>"{{ deleteItem?.name }}"</strong> 吗？</p>
        <p>此操作不可撤销。</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialog = false">取消</el-button>
          <el-button type="danger" @click="deleteTagItem" :loading="deleting">确认删除</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useSnackbar } from '@/composables/useSnackbar'
import {
  getBlogTags,
  createBlogTag,
  updateBlogTag,
  deleteBlogTag
} from '@/api/blog/blog'
import {
  Plus,
  Edit,
  Delete,
  Search,
  Refresh,
  Grid,
  List,
  InfoFilled,
  Document,
  Calendar
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const { showSnackbar } = useSnackbar()

// 定义标签类型
interface Tag {
  id: string
  name: string
  description: string
  color: string
  createTime: string
  count?: number
}

// 数据与状态
const tags = ref<Tag[]>([])
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const page = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)
const searchQuery = ref('')
const viewMode = ref('grid') // 默认使用网格视图

// 对话框状态
const dialog = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance | null>(null)
const deleteDialog = ref(false)
const deleteItem = ref<Tag | null>(null)

// 表单规则
const rules = reactive<FormRules>({
  name: [{ required: true, message: '标签名称不能为空', trigger: 'blur' }]
})

// 编辑项
const defaultItem: Tag = {
  id: '',
  name: '',
  description: '',
  color: '#409EFF',
  createTime: ''
}
const editedItem = reactive<Tag>({ ...defaultItem })

// 格式化日期时间
const formatDateTime = (dateString: string): string => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

// 加载标签列表
const loadTags = async () => {
  try {
    loading.value = true
    // 这里修复API调用，getBlogTags不需要参数
    const response = await getBlogTags()
    if (response && typeof response === 'object') {
      if ('data' in response && Array.isArray(response.data)) {
        const allTags = response.data as Tag[]

        // 根据搜索条件过滤
        let filteredTags = allTags
        if (searchQuery.value) {
          const query = searchQuery.value.toLowerCase()
          filteredTags = allTags.filter(tag =>
            tag.name.toLowerCase().includes(query) ||
            (tag.description && tag.description.toLowerCase().includes(query))
          )
        }

        // 计算分页
        totalItems.value = filteredTags.length
        const startIndex = (page.value - 1) * pageSize.value
        tags.value = filteredTags.slice(startIndex, startIndex + pageSize.value)
      } else if ('content' in response && 'totalElements' in response) {
        tags.value = (response.content as Tag[])
        totalItems.value = (response.totalElements as number)
      }
    }
  } catch (error) {
    console.error('加载标签列表失败', error)
    showSnackbar('加载标签列表失败', 'error')
  } finally {
    loading.value = false
  }
}

// 搜索
const onSearch = () => {
  page.value = 1
  loadTags()
}

// 打开新建对话框
const openCreateDialog = () => {
  isEdit.value = false
  Object.assign(editedItem, defaultItem)
  dialog.value = true
}

// 打开编辑对话框
const openEditDialog = (item: Tag) => {
  isEdit.value = true
  Object.assign(editedItem, item)
  dialog.value = true
}

// 关闭对话框
const closeDialog = () => {
  dialog.value = false
  setTimeout(() => {
    Object.assign(editedItem, defaultItem)
  }, 300)
}

// 保存标签
const saveTag = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      saving.value = true
      const tagData = { ...editedItem }

      if (isEdit.value) {
        await updateBlogTag(tagData.id, tagData)
        showSnackbar('标签更新成功', 'success')
      } else {
        await createBlogTag(tagData)
        showSnackbar('标签创建成功', 'success')
      }

      closeDialog()
      loadTags()
    } catch (error) {
      console.error('保存标签失败', error)
      showSnackbar('保存标签失败', 'error')
    } finally {
      saving.value = false
    }
  })
}

// 确认删除
const confirmDelete = (item: Tag) => {
  deleteItem.value = item
  deleteDialog.value = true
}

// 删除标签
const deleteTagItem = async () => {
  if (!deleteItem.value) return

  try {
    deleting.value = true
    await deleteBlogTag(deleteItem.value.id)
    showSnackbar('标签删除成功', 'success')
    deleteDialog.value = false
    loadTags()
  } catch (error) {
    console.error('删除标签失败', error)
    showSnackbar('删除标签失败', 'error')
  } finally {
    deleting.value = false
  }
}

// 初始化
onMounted(() => {
  loadTags()
})

// 监听筛选条件变化
watch([searchQuery], () => {
  if (searchQuery.value === '') {
    loadTags()
  }
})
</script>

<style scoped>
.blog-tag {
  padding: 16px;
}

.tag-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.tag-card:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.create-btn {
  background: linear-gradient(45deg, var(--vue-color-primary-dark-1), var(--vue-color-primary));
  border: none;
  box-shadow: 0 2px 6px rgba(66, 184, 131, 0.2);
  transition: all 0.3s;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(66, 184, 131, 0.3);
}

.search-bar {
  margin-bottom: 24px;
}

.search-input {
  border-radius: 8px;
  overflow: hidden;
}

.refresh-btn {
  width: 100%;
  border-radius: 8px;
}

.stats-row {
  margin-bottom: 24px;
}

.stats-text {
  margin-left: 8px;
}

.tag-grid {
  margin-bottom: 24px;
}

.tag-grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.tag-item-card {
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
  height: 100%;
}

.tag-item-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.tag-item-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tag-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.tag-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-actions {
  display: flex;
  gap: 8px;
}

.tag-action-btn {
  opacity: 0.8;
  transition: all 0.3s;
}

.tag-action-btn:hover {
  opacity: 1;
  transform: scale(1.1);
}

.tag-item-details {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  justify-content: space-between;
}

.tag-description {
  margin: 8px 0;
  font-size: 14px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
}

.tag-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  opacity: 0.8;
}

.tag-count, .tag-date {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tag-table {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
}

.article-count-badge :deep(.el-badge__content) {
  border-radius: 12px;
  padding: 0 8px;
}

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 20px;
}

.view-toggle {
  margin-bottom: 16px;
}

.pagination {
  margin-bottom: 16px;
}

.tag-dialog :deep(.el-dialog__header) {
  background-color: var(--vue-color-primary-light-9);
  padding: 20px;
  margin-right: 0;
  border-bottom: 1px solid var(--vue-color-primary-light-7);
}

.tag-dialog :deep(.el-dialog__title) {
  color: var(--vue-color-secondary);
  font-weight: 600;
}

.tag-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.tag-dialog :deep(.el-dialog__footer) {
  padding: 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.color-picker {
  vertical-align: middle;
}

.color-preview {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  border-radius: 4px;
  padding: 0 12px;
  margin-left: 12px;
  color: #fff;
  vertical-align: middle;
  transition: all 0.3s;
}

.preview-text {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;
}

.delete-dialog :deep(.el-dialog__header) {
  background-color: #fef0f0;
  border-bottom: 1px solid #fde2e2;
}

.delete-dialog :deep(.el-dialog__title) {
  color: #f56c6c;
}

.delete-warning {
  margin-bottom: 16px;
}

.delete-confirm-content {
  padding: 8px 0;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .table-footer {
    flex-direction: column;
    align-items: center;
  }

  .view-toggle {
    margin-bottom: 16px;
  }

  .tag-grid-container {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .tag-card {
    background-color: var(--el-bg-color-overlay);
  }

  .tag-item-card {
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .tag-dialog :deep(.el-dialog__header),
  .delete-dialog :deep(.el-dialog__header) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .tag-dialog :deep(.el-dialog__footer) {
    border-top: 1px solid rgba(255, 255, 255, 0.1);
  }
}
</style>
