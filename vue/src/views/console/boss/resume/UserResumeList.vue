<template>
  <div class="resume-list-container">
    <div class="mb-4">
      <el-card>
        <template #header>
          <div class="card-header">
            <span class="title">简历管理</span>
            <el-button type="primary" @click="handleAddResume">
              <el-icon class="mr-1"><Plus /></el-icon>添加简历
            </el-button>
          </div>
        </template>

        <!-- 搜索区域 -->
        <div class="filter-container mb-4">
          <el-form :inline="true" :model="searchForm">
            <el-form-item label="姓名">
              <el-input
                v-model="searchForm.name"
                placeholder="请输入姓名"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item label="工作年限">
              <el-select v-model="searchForm.experience" placeholder="请选择工作年限" clearable>
                <el-option label="应届生" value="应届生" />
                <el-option label="1-3年" value="1-3年" />
                <el-option label="3-5年" value="3-5年" />
                <el-option label="5-10年" value="5-10年" />
                <el-option label="10年以上" value="10年以上" />
              </el-select>
            </el-form-item>
            <el-form-item label="目标职位">
              <el-input
                v-model="searchForm.jobTarget"
                placeholder="请输入目标职位"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">
                <el-icon class="mr-1"><Search /></el-icon>搜索
              </el-button>
              <el-button @click="resetSearch">
                <el-icon class="mr-1"><RefreshRight /></el-icon>重置
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 表格区域 -->
        <el-table
          v-loading="loading"
          :data="resumeList"
          border
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="name" label="姓名" min-width="100" />
          <el-table-column prop="age" label="年龄" width="80" />
          <el-table-column prop="gender" label="性别" width="80" />
          <el-table-column prop="location" label="所在地" min-width="100" />
          <el-table-column prop="experience" label="工作经验" min-width="100" />
          <el-table-column prop="jobTarget" label="目标职位" min-width="120" />
          <el-table-column prop="expectedSalary" label="期望薪资" min-width="120" />
          <el-table-column prop="phone" label="联系电话" min-width="140" />
          <el-table-column label="标签" min-width="200">
            <template #default="scope">
              <el-space wrap>
                <el-tag
                  v-for="(tag, index) in scope.row.interestTags"
                  :key="index"
                  type="info"
                  size="small"
                >
                  {{ tag }}
                </el-tag>
              </el-space>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="scope">
              <el-button
                type="primary"
                link
                @click="handleEdit(scope.row)"
              >
                <el-icon class="mr-1"><Edit /></el-icon>编辑
              </el-button>
              <el-button
                type="danger"
                link
                @click="handleDelete(scope.row)"
              >
                <el-icon class="mr-1"><Delete /></el-icon>删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页区域 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
          />
        </div>
      </el-card>
    </div>

    <!-- 删除确认对话框 -->
    <el-dialog
      v-model="deleteDialogVisible"
      title="删除确认"
      width="30%"
    >
      <span>确定要删除选中的简历吗？此操作不可恢复。</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmDelete">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 简历操作对话框 -->
    <el-dialog
      v-model="resumeDialogVisible"
      :title="dialogTitle"
      width="90%"
      :destroy-on-close="true"
      fullscreen
    >
      <UserResume
        v-if="resumeDialogVisible"
        :initial-resume-data="currentResume"
        :edit-mode="currentMode"
        @save="handleResumeSave"
        @cancel="resumeDialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Edit, Delete, RefreshRight } from '@element-plus/icons-vue'
import type { ResumeData } from '@/api/resume/resume.d'
import { getResumeList, getResumeData, addResume, deleteResume } from '@/api/resume/resume'
import UserResume from '@/views/console/boss/resume/components/UserResume.vue'

// 数据加载与表格相关状态
const loading = ref(false)
const resumeList = ref<ResumeData[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchForm = ref({
  name: '',
  experience: '',
  jobTarget: ''
})
const selectedRows = ref<ResumeData[]>([])
const deleteDialogVisible = ref(false)
const deleteItem = ref<ResumeData | null>(null)

// 简历操作对话框相关状态
const resumeDialogVisible = ref(false)
const currentResume = ref<ResumeData>({} as ResumeData)
const currentMode = ref<'add' | 'edit' | 'view'>('view')
const dialogTitle = computed(() => {
  if (currentMode.value === 'add') return '添加简历'
  if (currentMode.value === 'edit') return '编辑简历'
  return '查看简历'
})

// 获取简历列表数据
const fetchResumeList = async () => {
  loading.value = true
  try {
    // 使用mock接口获取简历列表数据
    const res = await getResumeList({
      page: currentPage.value - 1,
      size: pageSize.value
    })

    resumeList.value = res.content
    total.value = res.totalElements

  } catch (error) {
    console.error('获取简历列表失败', error)
    ElMessage.error('获取简历列表失败')
  } finally {
    loading.value = false
  }
}

// 分别监听页码和页大小变化
watch(currentPage, (newPage, oldPage) => {
  if (newPage !== oldPage) {
    fetchResumeList()
  }
})

watch(pageSize, (newSize, oldSize) => {
  if (newSize !== oldSize) {
    currentPage.value = 1 // 页大小变化时，重置为第一页
    fetchResumeList()
  }
})

// 页面加载时获取数据
onMounted(() => {
  fetchResumeList()
})

// 搜索处理
const handleSearch = () => {
  currentPage.value = 1
  fetchResumeList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.value = {
    name: '',
    experience: '',
    jobTarget: ''
  }
  currentPage.value = 1
  fetchResumeList()
}

// 表格多选处理
const handleSelectionChange = (val: ResumeData[]) => {
  selectedRows.value = val
}

// 添加简历
const handleAddResume = async () => {
  // 获取空白简历模板
  try {
    const emptyResume = await getResumeData()
    // 重置关键字段
    emptyResume.name = ''
    emptyResume.age = ''
    emptyResume.jobTarget = ''
    emptyResume.experience = ''
    emptyResume.phone = ''
    currentResume.value = emptyResume
    currentMode.value = 'add'
    resumeDialogVisible.value = true
  } catch (error) {
    console.error('获取简历模板失败', error)
    ElMessage.error('获取简历模板失败')
  }
}

// 编辑简历
const handleEdit = (row: ResumeData) => {
  currentResume.value = JSON.parse(JSON.stringify(row))
  currentMode.value = 'edit'
  resumeDialogVisible.value = true
}

// 处理简历保存
const handleResumeSave = (data: ResumeData) => {
  // 将保存的简历更新到列表
  if (currentMode.value === 'add') {
    // 调用mock接口添加简历
    addResume(data).then(() => {
      ElMessage.success('添加简历成功')
      fetchResumeList() // 重新获取列表数据
    }).catch(() => {
      ElMessage.error('添加简历失败')
    })
  } else if (currentMode.value === 'edit') {
    // 调用保存接口，在mockjsApi.ts中已处理更新列表数据的逻辑
    const index = resumeList.value.findIndex(item => item.name === data.name)
    if (index !== -1) {
      resumeList.value[index] = data
    }
    ElMessage.success('编辑简历成功')
  }
  resumeDialogVisible.value = false
}

// 删除简历
const handleDelete = (row: ResumeData) => {
  deleteItem.value = row
  deleteDialogVisible.value = true
}

// 确认删除
const confirmDelete = () => {
  if (deleteItem.value) {
    // 调用mock接口删除简历
    deleteResume(deleteItem.value.name).then(() => {
      ElMessage.success(`已删除 ${deleteItem.value?.name} 的简历`)
      fetchResumeList() // 重新获取列表数据
    }).catch(() => {
      ElMessage.error('删除简历失败')
    })
    deleteDialogVisible.value = false
    deleteItem.value = null
  }
}
</script>

<style scoped>
.resume-list-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 500;
}

.filter-container {
  background-color: #f9f9f9;
  padding: 16px;
  border-radius: 8px;
}

.mr-1 {
  margin-right: 4px;
}

.mb-4 {
  margin-bottom: 16px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .resume-list-container {
    padding: 12px;
  }

  .filter-container {
    padding: 12px;
  }

  .pagination-container {
    justify-content: center;
  }
}
</style>
