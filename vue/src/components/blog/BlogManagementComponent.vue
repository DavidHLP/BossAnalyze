<template>
  <div class="blog-management">
    <el-card class="blog-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <h3>博客文章管理</h3>
          <el-button type="primary" @click="navigateToCreate" class="create-btn">
            <el-icon><Plus /></el-icon>
            新建博客
          </el-button>
        </div>
      </template>

      <div class="search-bar">
        <el-row :gutter="20">
          <el-col :xs="24" :md="8">
            <el-input
              v-model="searchQuery"
              placeholder="搜索博客标题"
              clearable
              @input="onSearch"
              class="search-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-col>
          <el-col :xs="24" :md="5">
            <el-select
              v-model="selectedCategory"
              placeholder="分类筛选"
              clearable
              @change="onFilterChange"
              style="width: 100%"
              class="filter-select"
            >
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </el-col>
          <el-col :xs="24" :md="5">
            <el-select
              v-model="selectedTag"
              placeholder="标签筛选"
              clearable
              @change="onFilterChange"
              style="width: 100%"
              class="filter-select"
            >
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.id"
              />
            </el-select>
          </el-col>
          <el-col :xs="24" :md="3">
            <el-select
              v-model="status"
              placeholder="状态筛选"
              clearable
              @change="onFilterChange"
              style="width: 100%"
              class="filter-select"
            >
              <el-option
                v-for="option in statusOptions"
                :key="option.value"
                :label="option.title"
                :value="option.value"
              />
            </el-select>
          </el-col>
          <el-col :xs="24" :md="3">
            <el-button type="info" @click="loadBlogList" class="refresh-btn">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </el-col>
        </el-row>
      </div>

      <!-- 博客统计信息 -->
      <div class="stats-row" v-if="blogs.length > 0">
        <el-alert type="info" :closable="false">
          <el-icon><InfoFilled /></el-icon>
          <span class="stats-text">共有 <strong>{{ totalItems }}</strong> 篇博客文章，其中已发布 <strong>{{ publishedCount }}</strong> 篇，草稿 <strong>{{ draftCount }}</strong> 篇</span>
        </el-alert>
      </div>

      <!-- 博客卡片网格视图 -->
      <div v-if="viewMode === 'grid'" class="blog-grid">
        <el-empty v-if="blogs.length === 0" description="暂无博客" />
        <div v-else class="blog-grid-container">
          <el-card
            v-for="blog in blogs"
            :key="blog.id"
            class="blog-item-card"
            shadow="hover"
          >
            <div class="blog-item-content">
              <div class="blog-item-header">
                <h4 class="blog-title">{{ blog.title }}</h4>
                <el-tag :type="blog.status === 1 ? 'success' : 'info'" class="blog-status-tag">
                  {{ blog.status === 1 ? '已发布' : '草稿' }}
                </el-tag>
              </div>

              <div class="blog-item-details">
                <p class="blog-summary">{{ blog.summary }}</p>

                <div class="blog-tags">
                  <el-tag type="primary" class="blog-category-tag">{{ blog.categoryName }}</el-tag>
                  <el-tag
                    v-for="(tag, index) in blog.tags"
                    :key="index"
                    size="small"
                    class="blog-tag-item"
                  >
                    {{ tag.name }}
                  </el-tag>
                </div>

                <div class="blog-meta">
                  <span class="blog-date">
                    <el-icon><Calendar /></el-icon> {{ formatDateTime(blog.createTime) }}
                  </span>
                  <span class="blog-views">
                    <el-icon><View /></el-icon> {{ blog.viewCount || 0 }} 阅读
                  </span>
                </div>
              </div>

              <div class="blog-actions">
                <el-tooltip content="编辑" placement="top">
                  <el-button
                    type="primary"
                    circle
                    size="small"
                    @click="navigateToEdit(blog)"
                    class="blog-action-btn"
                  >
                    <el-icon><Edit /></el-icon>
                  </el-button>
                </el-tooltip>

                <el-tooltip content="删除" placement="top">
                  <el-button
                    type="danger"
                    circle
                    size="small"
                    @click="confirmDelete(blog)"
                    class="blog-action-btn"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>

                <el-tooltip :content="blog.status === 1 ? '设为草稿' : '发布'" placement="top">
                  <el-button
                    :type="blog.status === 1 ? 'warning' : 'success'"
                    circle
                    size="small"
                    @click="toggleStatus(blog)"
                    class="blog-action-btn"
                  >
                    <el-icon>
                      <component :is="blog.status === 1 ? 'Hide' : 'View'" />
                    </el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 博客列表表格视图 -->
      <el-table
        v-if="viewMode === 'table'"
        :data="blogs"
        :loading="loading"
        style="width: 100%"
        border
        class="blog-table"
      >
        <el-table-column prop="title" label="标题" min-width="150">
          <template #default="{ row }">
            <div class="text-truncate" style="max-width: 250px;">{{ row.title }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="summary" label="摘要" min-width="200">
          <template #default="{ row }">
            <div class="text-truncate" style="max-width: 250px;">{{ row.summary }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="categoryName" label="分类" width="120">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.categoryName }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="160" sortable>
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="viewCount" label="阅读量" width="100" sortable>
          <template #default="{ row }">
            <el-badge :value="row.viewCount || 0" type="primary" />
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="编辑" placement="top">
              <el-button
                type="primary"
                circle
                size="small"
                @click="navigateToEdit(row)"
              >
                <el-icon><Edit /></el-icon>
              </el-button>
            </el-tooltip>

            <el-tooltip content="删除" placement="top">
              <el-button
                type="danger"
                circle
                size="small"
                @click="confirmDelete(row)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </el-tooltip>

            <el-tooltip :content="row.status === 1 ? '设为草稿' : '发布'" placement="top">
              <el-button
                :type="row.status === 1 ? 'warning' : 'success'"
                circle
                size="small"
                @click="toggleStatus(row)"
              >
                <el-icon>
                  <component :is="row.status === 1 ? 'Hide' : 'View'" />
                </el-icon>
              </el-button>
            </el-tooltip>
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
          @size-change="onPageSizeChange"
          @current-change="onPageChange"
          class="pagination"
        />
      </div>
    </el-card>

    <!-- 确认删除对话框 -->
    <el-dialog
      v-model="deleteDialog"
      title="确认删除"
      width="400px"
      class="delete-dialog"
      destroy-on-close
    >
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        title="危险操作"
        description="删除博客将永久移除该文章及其相关数据"
        class="delete-warning"
      />
      <div class="delete-confirm-content">
        <p>确定要删除博客 <strong>"{{ deleteItem.title }}"</strong> 吗？</p>
        <p>此操作不可撤销。</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialog = false">取消</el-button>
          <el-button type="danger" @click="deleteBlogItem" :loading="deleting">确认删除</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup name="BlogManagementComponent">
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useSnackbar } from '@/composables/useSnackbar'
import {
  Plus,
  Search,
  Refresh,
  Edit,
  Delete,
  View,
  Hide,
  Grid,
  List,
  InfoFilled,
  Calendar
} from '@element-plus/icons-vue'
import {
  getBlogList,
  deleteBlog,
  getBlogCategories,
  getBlogTags,
  updateBlog,
  type Blog,
  type Category,
  type Tag
} from '@/api/blog/blog'

// 扩展Blog类型以包含viewCount属性
interface ExtendedBlog extends Blog {
  viewCount?: number;
}

const router = useRouter()
const { showSnackbar } = useSnackbar()

// 数据与状态
const blogs = ref<ExtendedBlog[]>([])
const loading = ref(false)
const deleting = ref(false)
const page = ref(1)
const pageSize = ref(10)
const totalItems = ref(0)
const searchQuery = ref('')
const selectedCategory = ref<string | null>(null)
const selectedTag = ref<string | null>(null)
const status = ref<number | null>(null)
const viewMode = ref('grid') // 默认使用网格视图
const statusOptions = [
  { title: '已发布', value: 1 },
  { title: '草稿', value: 0 }
]

// 计算属性
const publishedCount = computed(() => {
  return blogs.value.filter(blog => blog.status === 1).length
})

const draftCount = computed(() => {
  return blogs.value.filter(blog => blog.status === 0).length
})

// 分类和标签
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])

// 删除对话框状态
const deleteDialog = ref(false)
const deleteItem = ref<Blog>({} as Blog)

// 格式化日期时间
const formatDateTime = (dateString: string) => {
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

// 加载博客列表
const loadBlogList = async () => {
  try {
    loading.value = true
    const params = {
      page: page.value,
      size: pageSize.value,
      keyword: searchQuery.value || undefined,
      categoryId: selectedCategory.value || undefined,
      tagId: selectedTag.value || undefined,
      status: status.value !== null ? status.value : undefined
    }

    const response = await getBlogList(params)
    blogs.value = response.data.content
    totalItems.value = response.data.totalElements
  } catch (error) {
    console.error('加载博客列表失败', error)
    showSnackbar('加载博客列表失败', 'error')
  } finally {
    loading.value = false
  }
}

// 分页处理
const onPageChange = (currentPage: number) => {
  page.value = currentPage
  loadBlogList()
}

const onPageSizeChange = (newSize: number) => {
  pageSize.value = newSize
  loadBlogList()
}

// 加载分类
const loadCategories = async () => {
  try {
    const response = await getBlogCategories()
    categories.value = response.data
  } catch (error) {
    console.error('加载分类失败', error)
    showSnackbar('加载分类失败', 'error')
  }
}

// 加载标签
const loadTags = async () => {
  try {
    const response = await getBlogTags()
    tags.value = response.data
  } catch (error) {
    console.error('加载标签失败', error)
    showSnackbar('加载标签失败', 'error')
  }
}

// 搜索
const onSearch = () => {
  page.value = 1
  loadBlogList()
}

// 筛选更改
const onFilterChange = () => {
  page.value = 1
  loadBlogList()
}

// 导航到新建博客页面
const navigateToCreate = () => {
  router.push('/blog/edit/new')
}

// 导航到编辑博客页面
const navigateToEdit = (item: Blog) => {
  router.push(`/console/blog/edit/${item.id}`)
}

// 确认删除
const confirmDelete = (item: Blog) => {
  deleteItem.value = item
  deleteDialog.value = true
}

// 删除博客
const deleteBlogItem = async () => {
  try {
    deleting.value = true
    await deleteBlog(deleteItem.value.id)
    showSnackbar('博客删除成功', 'success')
    deleteDialog.value = false
    loadBlogList()
  } catch (error) {
    console.error('删除博客失败', error)
    showSnackbar('删除博客失败', 'error')
  } finally {
    deleting.value = false
  }
}

// 切换博客状态
const toggleStatus = async (item: Blog) => {
  try {
    loading.value = true
    const newStatus = item.status === 1 ? 0 : 1
    await updateBlog(item.id, { status: newStatus })
    showSnackbar(`博客已${newStatus === 1 ? '发布' : '设为草稿'}`, 'success')
    loadBlogList()
  } catch (error) {
    console.error('更新博客状态失败', error)
    showSnackbar('更新博客状态失败', 'error')
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  loadBlogList()
  loadCategories()
  loadTags()
})

// 监听筛选条件变化
watch([searchQuery], () => {
  if (searchQuery.value === '') {
    loadBlogList()
  }
})
</script>

<style scoped>
.blog-management {
  padding: 16px;
}

.blog-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.blog-card:hover {
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

.filter-select {
  border-radius: 8px;
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

.blog-grid {
  margin-bottom: 24px;
}

.blog-grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.blog-item-card {
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
  height: 100%;
}

.blog-item-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.blog-item-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.blog-item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.blog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.blog-status-tag {
  margin-left: 8px;
  flex-shrink: 0;
}

.blog-item-details {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}

.blog-summary {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  line-height: 1.5;
}

.blog-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.blog-category-tag {
  margin-right: 8px;
}

.blog-tag-item {
  background-color: rgba(64, 158, 255, 0.1);
  color: var(--el-color-primary);
  border-color: rgba(64, 158, 255, 0.2);
}

.blog-meta {
  display: flex;
  justify-content: space-between;
  margin-top: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.blog-date, .blog-views {
  display: flex;
  align-items: center;
  gap: 4px;
}

.blog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.blog-action-btn {
  opacity: 0.8;
  transition: all 0.3s;
}

.blog-action-btn:hover {
  opacity: 1;
  transform: scale(1.1);
}

.blog-table {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
}

.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .el-col {
    margin-bottom: 12px;
  }

  .table-footer {
    flex-direction: column;
    align-items: center;
  }

  .view-toggle {
    margin-bottom: 16px;
  }

  .blog-grid-container {
    grid-template-columns: 1fr;
  }

  .pagination-container {
    overflow-x: auto;
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .blog-card {
    background-color: var(--el-bg-color-overlay);
  }

  .blog-item-card {
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .delete-dialog :deep(.el-dialog__header) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }
}
</style>
