<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  MoreFilled,
  Edit,
  Delete,
  Clock,
  Search,
  DocumentCopy,
  Star,
  Grid,
  List,
} from '@element-plus/icons-vue'
import {
  getResumes,
  createResume,
  deleteResume as apiDeleteResume,
  updateResume,
} from '@/api/resume/resume'
import type { Resume } from '@/api/resume/types'
import { default as template } from '@/templates/modules/10front_end/index'

const router = useRouter()

const resumeList = ref<Resume[]>([])
const loading = ref(false)
const searchQuery = ref('')
const selectedResumeIds = ref<string[]>([])
const editingTitleId = ref<string | null>(null)
const editingTitle = ref('')
const tableRef = ref()
const viewMode = ref<'table' | 'grid'>('grid')

// 分页状态
const currentPage = ref(1)
const pageSize = ref(10)
const pageSizes = [10, 20, 50, 100]

// 表格排序状态
const sortField = ref('updatedAt')
const sortOrder = ref<'ascending' | 'descending'>('descending')

// 计算属性
const filteredResumes = computed(() => {
  let filtered = resumeList.value

  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter((resume) => resume.title.toLowerCase().includes(query))
  }

  return filtered
})

const paginatedResumes = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredResumes.value.slice(start, end)
})

const totalCount = computed(() => filteredResumes.value.length)

const isAllSelected = computed(() => {
  return (
    paginatedResumes.value.length > 0 &&
    paginatedResumes.value.every((resume) => selectedResumeIds.value.includes(resume.id))
  )
})

const hasSelectedResumes = computed(() => selectedResumeIds.value.length > 0)

// 统计信息
const stats = computed(() => ({
  total: resumeList.value.length,
  thisMonth: resumeList.value.filter((resume) => {
    const createdDate = new Date(resume.createdAt)
    const now = new Date()
    return (
      createdDate.getMonth() === now.getMonth() && createdDate.getFullYear() === now.getFullYear()
    )
  }).length,
  lastWeek: resumeList.value.filter((resume) => {
    const createdDate = new Date(resume.createdAt)
    const weekAgo = new Date()
    weekAgo.setDate(weekAgo.getDate() - 7)
    return createdDate >= weekAgo
  }).length,
}))

// 方法
const getResumeList = async () => {
  loading.value = true
  try {
    resumeList.value = await getResumes()
  } catch (error) {
    ElMessage.error('获取简历列表失败')
  } finally {
    loading.value = false
  }
}

const handleCreateResume = async () => {
  try {
    const newResume = await createResume({ title: '我的新简历', content: template.content })
    if (newResume && newResume.id) {
      ElMessage.success('创建成功！')
      router.push(`/editor?type=create&id=${newResume.id}`)
    }
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const editResume = (id: string) => {
  router.push(`/editor?type=create&id=${id}`)
}

const handleDeleteResume = (resume: Resume) => {
  ElMessageBox.confirm(`确定要删除简历"${resume.title}"吗？删除后无法恢复哦！`, '温馨提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await apiDeleteResume(resume.id)
      ElMessage.success('删除成功！')
      getResumeList()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

const handleCopyResume = async (resume: Resume) => {
  try {
    const newResume = await createResume({
      title: `${resume.title} - 副本`,
      content: resume.content,
    })
    if (newResume && newResume.id) {
      ElMessage.success('复制成功！')
      getResumeList()
    }
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

const startEditTitle = async (resume: Resume) => {
  editingTitleId.value = resume.id
  editingTitle.value = resume.title
  await nextTick()
  const input = document.querySelector('.title-input') as HTMLInputElement
  input?.focus()
}

const saveTitle = async (resume: Resume) => {
  if (editingTitle.value.trim() === '') {
    ElMessage.warning('标题不能为空')
    return
  }

  if (editingTitle.value === resume.title) {
    editingTitleId.value = null
    return
  }

  try {
    await updateResume(resume.id, { ...resume, title: editingTitle.value })
    resume.title = editingTitle.value
    editingTitleId.value = null
    ElMessage.success('标题修改成功！')
  } catch (error) {
    ElMessage.error('标题修改失败')
  }
}

const cancelEditTitle = () => {
  editingTitleId.value = null
  editingTitle.value = ''
}

const handleSelectionChange = (selection: Resume[]) => {
  selectedResumeIds.value = selection.map((item) => item.id)
}

const toggleCardSelection = (resumeId: string) => {
  const index = selectedResumeIds.value.indexOf(resumeId)
  if (index > -1) {
    selectedResumeIds.value.splice(index, 1)
  } else {
    selectedResumeIds.value.push(resumeId)
  }
}

const handleBatchDelete = () => {
  if (selectedResumeIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的简历')
    return
  }

  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedResumeIds.value.length} 份简历吗？删除后无法恢复！`,
    '批量删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    },
  ).then(async () => {
    try {
      const deletePromises = selectedResumeIds.value.map((id) => apiDeleteResume(id))
      await Promise.all(deletePromises)
      ElMessage.success(`成功删除 ${selectedResumeIds.value.length} 份简历！`)
      selectedResumeIds.value = []
      getResumeList()
    } catch (error) {
      ElMessage.error('批量删除失败')
    }
  })
}

const handleSortChange = ({ prop, order }: { prop: string; order: string | null }) => {
  sortField.value = prop || 'updatedAt'
  sortOrder.value = (order as 'ascending' | 'descending') || 'descending'
}

const handleSearch = () => {
  currentPage.value = 1
}

const clearSearch = () => {
  searchQuery.value = ''
  currentPage.value = 1
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const formatRelativeTime = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return `${Math.floor(days / 30)}个月前`
}

// 处理下拉菜单命令
const handleActionCommand = (command: string, row: Resume) => {
  switch (command) {
    case 'copy':
      handleCopyResume(row)
      break
    case 'delete':
      handleDeleteResume(row)
      break
  }
}

onMounted(() => {
  getResumeList()
})
</script>

<template>
  <div class="resume-list-container">
    <div class="container-content">
      <!-- 优化页面头部 -->
      <div class="page-header">
        <div class="header-content">
          <div class="header-left">
            <div class="title-section">
              <h1 class="page-title">简历管理</h1>
              <p class="page-description">管理和编辑您的个人简历</p>
            </div>
            <div class="stats-row">
              <div class="stat-item">
                <span class="stat-number">{{ stats.total }}</span>
                <span class="stat-label">总计</span>
              </div>
              <div v-if="stats.thisMonth > 0" class="stat-item highlight">
                <span class="stat-number">{{ stats.thisMonth }}</span>
                <span class="stat-label">本月新增</span>
              </div>
              <div v-if="stats.lastWeek > 0" class="stat-item">
                <span class="stat-number">{{ stats.lastWeek }}</span>
                <span class="stat-label">近7天</span>
              </div>
            </div>
          </div>

          <div class="header-actions">
            <el-button
              type="primary"
              :icon="Plus"
              @click="handleCreateResume"
              class="create-button"
            >
              新建简历
            </el-button>
          </div>
        </div>
      </div>

      <!-- 主要内容区域 -->
      <div class="main-content">
        <div class="content-wrapper">
          <!-- 优化工具栏 -->
          <div class="toolbar">
            <div class="toolbar-section">
              <div class="search-section">
                <el-input
                  v-model="searchQuery"
                  placeholder="搜索简历标题、内容..."
                  :prefix-icon="Search"
                  clearable
                  @input="handleSearch"
                  @clear="clearSearch"
                  class="search-input"
                  size="large"
                />
              </div>

              <div class="filter-section">
                <el-select
                  v-model="sortField"
                  @change="handleSortChange({ prop: sortField, order: 'descending' })"
                  class="sort-select"
                  placeholder="排序方式"
                  size="large"
                >
                  <el-option label="最近更新" value="updatedAt" />
                  <el-option label="最新创建" value="createdAt" />
                  <el-option label="按标题" value="title" />
                </el-select>
              </div>
            </div>

            <div class="toolbar-actions">
              <!-- 批量操作提示 -->
              <transition name="slide-fade">
                <div v-if="hasSelectedResumes" class="batch-indicator">
                  <div class="selection-info">
                    <span class="selection-count">{{ selectedResumeIds.length }}</span>
                    <span class="selection-text">项已选择</span>
                  </div>
                  <el-button
                    type="danger"
                    size="small"
                    :icon="Delete"
                    @click="handleBatchDelete"
                    class="batch-delete"
                  >
                    批量删除
                  </el-button>
                </div>
              </transition>

              <!-- 视图切换 -->
              <div class="view-switcher">
                <div class="view-toggle-group">
                  <button
                    class="view-toggle-btn"
                    :class="{ active: viewMode === 'grid' }"
                    @click="viewMode = 'grid'"
                    title="网格视图"
                  >
                    <el-icon><Grid /></el-icon>
                    <span>网格</span>
                  </button>
                  <button
                    class="view-toggle-btn"
                    :class="{ active: viewMode === 'table' }"
                    @click="viewMode = 'table'"
                    title="列表视图"
                  >
                    <el-icon><List /></el-icon>
                    <span>列表</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="!loading && resumeList.length === 0" class="empty-container">
            <el-empty description="还没有简历" :image-size="100">
              <template #description>
                <h3>还没有简历</h3>
                <p>创建您的第一份简历，开始您的职业之旅</p>
              </template>
              <el-button type="primary" @click="handleCreateResume" class="empty-action-btn">
                立即创建
              </el-button>
            </el-empty>
          </div>

          <!-- 搜索无结果 -->
          <div
            v-else-if="!loading && filteredResumes.length === 0 && searchQuery"
            class="empty-container"
          >
            <el-empty description="没有找到相关简历" :image-size="100">
              <template #description>
                <h3>没有找到相关简历</h3>
                <p>尝试修改搜索关键词</p>
              </template>
              <el-button @click="clearSearch" class="empty-action-btn"> 清空搜索 </el-button>
            </el-empty>
          </div>

          <!-- 简洁卡片视图 -->
          <div v-else-if="viewMode === 'grid'" class="resume-cards-container">
            <div class="cards-grid">
              <div
                v-for="resume in paginatedResumes"
                :key="resume.id"
                class="resume-card"
                :class="{ selected: selectedResumeIds.includes(resume.id) }"
                @click="editResume(resume.id)"
              >
                <!-- 选择器 -->
                <div class="card-selector" @click.stop>
                  <el-checkbox
                    :model-value="selectedResumeIds.includes(resume.id)"
                    @change="toggleCardSelection(resume.id)"
                    size="small"
                  />
                </div>

                <!-- 操作菜单 -->
                <div class="card-menu" @click.stop>
                  <el-dropdown @command="(command: string) => handleActionCommand(command, resume)">
                    <el-button text size="small" :icon="MoreFilled" />
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="copy">复制</el-dropdown-item>
                        <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>

                <!-- 卡片主体 -->
                <div class="card-body">
                  <!-- 卡片图标和状态 -->
                  <div class="card-icon-section">
                    <div class="card-icon">
                      <el-icon><DocumentCopy /></el-icon>
                    </div>
                    <div class="card-status">
                      <span class="status-dot"></span>
                      <span class="status-text">已保存</span>
                    </div>
                  </div>

                  <!-- 卡片内容 -->
                  <div class="card-content">
                    <div class="card-title-section">
                      <el-input
                        v-if="editingTitleId === resume.id"
                        v-model="editingTitle"
                        size="small"
                        @keyup.enter="saveTitle(resume)"
                        @keyup.esc="cancelEditTitle"
                        @blur="saveTitle(resume)"
                        @click.stop
                        class="title-editor"
                      />
                      <h3 v-else class="card-title" @dblclick.stop="startEditTitle(resume)">
                        {{ resume.title }}
                      </h3>
                      <p class="card-subtitle">双击编辑标题</p>
                    </div>

                    <div class="card-meta">
                      <div class="meta-item">
                        <el-icon class="meta-icon"><Clock /></el-icon>
                        <span class="meta-text"
                          >{{ formatRelativeTime(resume.updatedAt) }}更新</span
                        >
                      </div>
                      <div class="meta-item">
                        <span class="meta-date">{{
                          formatDate(resume.createdAt).split(' ')[0]
                        }}</span>
                      </div>
                    </div>
                  </div>

                  <!-- 卡片操作 -->
                  <div class="card-actions">
                    <el-button
                      type="primary"
                      :icon="Edit"
                      @click.stop="editResume(resume.id)"
                      class="primary-action"
                    >
                      编辑简历
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 表格视图 -->
          <div v-else-if="viewMode === 'table'" class="resume-table-container">
            <el-table
              ref="tableRef"
              :data="paginatedResumes"
              v-loading="loading"
              @selection-change="handleSelectionChange"
              @sort-change="handleSortChange"
              :default-sort="{ prop: sortField, order: sortOrder }"
              class="resume-table"
              stripe
            >
              <!-- 选择列 -->
              <el-table-column type="selection" width="60" align="center" />

              <!-- 标题列 -->
              <el-table-column prop="title" label="简历标题" sortable min-width="300">
                <template #default="{ row }">
                  <div class="title-cell">
                    <el-input
                      v-if="editingTitleId === row.id"
                      v-model="editingTitle"
                      size="small"
                      maxlength="50"
                      show-word-limit
                      @keyup.enter="saveTitle(row)"
                      @keyup.esc="cancelEditTitle"
                      @blur="saveTitle(row)"
                      class="title-input"
                    />
                    <div v-else class="title-display" @dblclick="startEditTitle(row)">
                      <div class="resume-icon">
                        <el-icon><DocumentCopy /></el-icon>
                      </div>
                      <div class="title-content">
                        <div class="title-text">{{ row.title }}</div>
                        <div class="title-meta">{{ formatRelativeTime(row.updatedAt) }}更新</div>
                      </div>
                      <div class="edit-hint">双击编辑</div>
                    </div>
                  </div>
                </template>
              </el-table-column>

              <!-- 创建时间列 -->
              <el-table-column
                prop="createdAt"
                label="创建时间"
                sortable
                width="180"
                align="center"
              >
                <template #default="{ row }">
                  <div class="time-cell">
                    <el-icon><Clock /></el-icon>
                    <span>{{ formatDate(row.createdAt) }}</span>
                  </div>
                </template>
              </el-table-column>

              <!-- 更新时间列 -->
              <el-table-column
                prop="updatedAt"
                label="更新时间"
                sortable
                width="180"
                align="center"
              >
                <template #default="{ row }">
                  <div class="time-cell">
                    <el-icon><Clock /></el-icon>
                    <span>{{ formatDate(row.updatedAt) }}</span>
                  </div>
                </template>
              </el-table-column>

              <!-- 状态列 -->
              <el-table-column label="状态" width="120" align="center">
                <template #default="{ row }">
                  <div class="status-cell">
                    <div class="status-tag success">
                      <el-icon><Star /></el-icon>
                      已保存
                    </div>
                  </div>
                </template>
              </el-table-column>

              <!-- 操作列 -->
              <el-table-column label="操作" width="160" align="center" fixed="right">
                <template #default="{ row }">
                  <div class="action-cell">
                    <el-button
                      size="small"
                      :icon="Edit"
                      @click="editResume(row.id)"
                      class="action-btn primary"
                    >
                      编辑
                    </el-button>

                    <el-dropdown @command="(command: string) => handleActionCommand(command, row)">
                      <el-button size="small" :icon="MoreFilled" class="action-btn more" />
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="copy">
                            <el-icon><DocumentCopy /></el-icon>
                            复制简历
                          </el-dropdown-item>
                          <el-dropdown-item command="delete" divided class="danger-item">
                            <el-icon><Delete /></el-icon>
                            删除简历
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 分页 -->
          <div v-if="filteredResumes.length > 0" class="pagination-container">
            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="pageSizes"
                :total="totalCount"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                background
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use './resume.scss';
</style>
