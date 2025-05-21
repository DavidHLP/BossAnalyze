<template>
  <div class="select-resume-container">
    <div class="resume-header">
      <div class="section-title">
        <el-icon><Document /></el-icon>
        <span>请选择要分析的简历</span>
        <el-tooltip content="选择一份简历进行AI分析和职位匹配" placement="top">
          <el-icon class="help-icon"><QuestionFilled /></el-icon>
        </el-tooltip>
      </div>

      <div class="resume-sort" v-if="resumeList.length > 0">
        <el-dropdown trigger="click">
          <div class="sort-dropdown">
            <el-icon><Sort /></el-icon>
            <span>按更新时间排序</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>按更新时间排序</el-dropdown-item>
              <el-dropdown-item>按创建时间排序</el-dropdown-item>
              <el-dropdown-item>按字母排序</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-empty v-if="resumeList.length === 0" description="暂无简历数据" class="resume-empty">
      <template #image>
        <el-icon :size="50" color="#909399"><Document /></el-icon>
      </template>
      <div>您还没有创建任何简历，请先创建一份简历</div>
      <el-button type="primary" class="empty-action-btn" @click="onCreateResume">
        <el-icon><Plus /></el-icon> 创建简历
      </el-button>
    </el-empty>

    <div v-else class="resume-content">
      <div class="resume-count">
        <el-badge :value="resumeList.length" class="count-badge" type="primary" />
        <span class="count-text">份简历可用</span>
      </div>

      <el-scrollbar height="350px" class="resume-scrollbar">
        <div class="resume-grid">
          <div v-for="resume in resumeList" :key="resume.id" class="resume-item-wrapper">
            <el-card
              :class="{'resume-item-active': selectedResumeId === resume.id}"
              class="resume-item"
              shadow="hover"
              @click="onSelectResume(resume.id as string)"
            >
              <div class="resume-header-row">
                <span class="resume-name" :title="resume.name">{{ resume.name }}</span>
                <el-tag v-if="selectedResumeId === resume.id" type="success" size="small" class="selected-tag">已选择</el-tag>
              </div>

              <div class="resume-info-grid">
                <div class="info-row">
                  <el-icon class="info-icon"><Briefcase /></el-icon>
                  <div class="info-content">
                    <div class="info-label">期望职位</div>
                    <div class="info-value">{{ resume.jobTarget || '未设置' }}</div>
                  </div>
                </div>
                <div class="info-row">
                  <el-icon class="info-icon"><Location /></el-icon>
                  <div class="info-content">
                    <div class="info-label">期望城市</div>
                    <div class="info-value">{{ Array.isArray(resume.targetCity) && resume.targetCity.length > 0 ? resume.targetCity.join('/') : '未设置' }}</div>
                  </div>
                </div>
                <div class="info-row">
                  <el-icon class="info-icon"><Calendar /></el-icon>
                  <div class="info-content">
                    <div class="info-label">更新时间</div>
                    <div class="info-value">{{ formatResumeDate(resume.updatedAt) }}</div>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </el-scrollbar>

      <!-- 分页组件 -->
      <el-pagination
        v-if="pagination.total > pagination.size"
        background
        layout="prev, pager, next"
        :total="pagination.total"
        :page-size="pagination.size"
        :current-page="pagination.page"
        @current-change="onPageChange"
        class="resume-pagination"
      />
    </div>

    <div class="action-footer">
      <el-button plain @click="onCreateResume">
        <el-icon><Plus /></el-icon>添加简历
      </el-button>
      <el-button
        type="primary"
        :disabled="!selectedResumeId"
        @click="onNext"
      >
        下一步<el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits, ref } from 'vue'
import type { ResumeData } from '@/api/resume/resume.d'
import {
  Document,
  QuestionFilled,
  Plus,
  Sort,
  ArrowDown,
  Briefcase,
  Location,
  Calendar,
  ArrowRight
} from '@element-plus/icons-vue'

const props = defineProps<{
  resumeList: ResumeData[]
  selectedResumeId: string
  pagination: {
    page: number
    size: number
    total: number
  }
}>()

const emit = defineEmits<{
  (e: 'update:selectedResumeId', id: string): void
  (e: 'pageChange', page: number): void
  (e: 'createResume'): void
  (e: 'next'): void
}>()

// 本地状态，用于绑定选择
const selectedResumeId = ref(props.selectedResumeId)

// 选择简历
const onSelectResume = (id: string) => {
  selectedResumeId.value = id
  emit('update:selectedResumeId', id as string)
}

// 处理分页变化
const onPageChange = (page: number) => {
  emit('pageChange', page)
}

// 跳转到创建简历页面
const onCreateResume = () => {
  emit('createResume')
}

// 下一步
const onNext = () => {
  emit('next')
}

// 格式化日期
const formatResumeDate = (date?: Date) => {
  if (!date) return '未知'
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.select-resume-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  background-color: #fff;
  border: 1px solid #ebeef5;
  overflow: hidden;
}

.resume-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
}

.section-title .el-icon {
  margin-right: 8px;
  color: #409eff;
}

.help-icon {
  font-size: 14px;
  color: #909399;
  margin-left: 8px;
}

.resume-sort {
  cursor: pointer;
  font-size: 13px;
  color: #606266;
}

.sort-dropdown {
  display: flex;
  align-items: center;
}

.sort-dropdown .el-icon {
  margin-right: 4px;
}

.resume-empty {
  padding: 30px 0;
}

.empty-action-btn {
  margin-top: 16px;
}

.resume-content {
  padding: 12px 16px;
}

.resume-count {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.count-text {
  font-size: 13px;
  color: #606266;
  margin-left: 8px;
}

.resume-scrollbar {
  width: 100%;
  padding: 10px 0;
}

.resume-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  padding: 0 10px;
}

.resume-item-wrapper {
  margin-bottom: 0;
}

.resume-item {
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  padding: 12px 16px;
  height: auto;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  border: 1px solid #EBEEF5;
}

.resume-item:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.resume-item-active {
  border: 2px solid #409EFF;
  background-color: rgba(64, 158, 255, 0.05);
}

.resume-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.resume-name {
  font-weight: 600;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%;
}

.selected-tag {
  margin-left: 4px;
}

.resume-info-grid {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 16px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  min-height: 42px;
  margin-bottom: 2px;
}

.info-icon {
  margin-right: 10px;
  color: #409eff;
  margin-top: 2px;
  flex-shrink: 0;
  font-size: 18px;
}

.info-content {
  flex: 1;
  overflow: visible;
  display: flex;
  flex-direction: column;
}

.info-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  line-height: 1;
}

.info-value {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
  overflow: visible;
}

.resume-pagination {
  margin-top: 16px;
  text-align: center;
}

.action-footer {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  margin-top: auto;
}

@media (max-width: 768px) {
  .resume-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .resume-sort {
    margin-top: 8px;
    align-self: flex-end;
  }

  .action-footer {
    flex-direction: column;
    gap: 10px;
  }

  .action-footer .el-button {
    width: 100%;
  }
}
</style>
