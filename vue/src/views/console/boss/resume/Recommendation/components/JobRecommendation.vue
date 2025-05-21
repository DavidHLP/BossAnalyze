<template>
  <el-space direction="vertical" fill class="step-content">
    <el-card class="content-card">
      <el-row class="title-section">
        <el-col :span="24">
          <el-space alignment="center" class="section-title">
            <el-icon><Briefcase /></el-icon>
            <span>推荐职位</span>
            <el-badge value="AI智能匹配" class="match-badge">
              <el-icon><Medal /></el-icon>
            </el-badge>
          </el-space>
        </el-col>
      </el-row>

      <div class="content-wrapper">
        <el-space v-if="loading" direction="vertical" alignment="center" fill class="loading-jobs">
          <el-space alignment="center" class="loading-spinner-container">
            <el-icon class="loading-spinner"><Loading /></el-icon>
          </el-space>
          <el-skeleton :rows="6" animated />
          <el-space alignment="center" class="loading-text">
            <el-icon><Search /></el-icon>
            <span>正在匹配最适合的职位，请稍候...</span>
          </el-space>
          <el-card class="loading-details">
            <el-progress :percentage="loadingProgress" :stroke-width="10" class="mb-3"></el-progress>
            <el-space direction="vertical" fill class="progress-info">
              <el-space alignment="center" class="progress-item">
                <el-icon class="progress-icon success"><CircleCheckFilled /></el-icon>
                <span>简历数据分析完成</span>
              </el-space>
              <el-space alignment="center" class="progress-item">
                <el-icon class="progress-icon success"><CircleCheckFilled /></el-icon>
                <span>市场职位数据加载完成</span>
              </el-space>
              <el-space alignment="center" class="progress-item">
                <el-icon class="progress-icon loading"><Loading /></el-icon>
                <span>正在进行智能匹配...</span>
              </el-space>
            </el-space>
          </el-card>
        </el-space>

        <el-empty v-else-if="jobs.length === 0" description="未找到匹配的职位" class="py-5">
          <template #image>
            <el-space alignment="center" class="empty-image">
              <el-icon :size="46" color="#909399"><DocumentDelete /></el-icon>
            </el-space>
          </template>
          <el-text class="empty-text" type="info">暂时没有与您技能匹配的职位，请尝试调整您的简历信息</el-text>
          <el-button type="primary" plain class="empty-button" @click="reset">
            <el-icon><RefreshRight /></el-icon> 重新开始
          </el-button>
        </el-empty>

        <el-space v-else direction="vertical" fill>
          <el-card class="match-summary">
            <el-space alignment="center" class="match-header">
              <el-space alignment="center" justify="center" class="match-icon">
                <el-icon><Aim /></el-icon>
              </el-space>
              <el-space direction="vertical" fill class="match-info">
                <el-space class="match-title" alignment="center">
                  <span>找到</span>
                  <el-tag type="success" size="small" round>{{ jobs.length }}</el-tag>
                  <span>个匹配职位</span>
                </el-space>
                <el-text class="match-description" type="info">基于您的简历技能和经验，我们匹配了以下职位</el-text>
              </el-space>
            </el-space>
          </el-card>

          <el-card class="filter-controls">
            <el-row :gutter="20" align="middle">
              <el-col :md="12" :sm="24" class="filter-left">
                <el-space alignment="center" class="sort-wrapper">
                  <el-icon><Filter /></el-icon>
                  <span class="sort-label">排序方式：</span>
                  <el-select v-model="sortBy" placeholder="排序方式" size="small">
                    <el-option label="匹配度从高到低" value="similarity"></el-option>
                    <el-option label="薪资从高到低" value="salary"></el-option>
                  </el-select>
                </el-space>
              </el-col>
              <el-col :md="12" :sm="24" class="filter-right">
                <el-space class="search-wrapper">
                  <el-input
                    v-model="searchKeyword"
                    placeholder="搜索职位或公司"
                    prefix-icon="Search"
                    clearable
                    size="small"
                  >
                  </el-input>
                </el-space>
              </el-col>
            </el-row>
          </el-card>

          <el-row :gutter="20" class="jobs-container">
            <el-col v-for="job in filteredJobs" :key="job.id" :lg="12" :md="24" class="job-col">
              <el-card class="job-item" shadow="hover">
                <template #header>
                  <el-row class="job-header" justify="space-between" align="middle">
                    <el-col :span="18">
                      <el-text class="job-title" truncated>{{ job.jobAnalysisData.positionName }}</el-text>
                    </el-col>
                    <el-col :span="6" style="text-align:right">
                      <el-tooltip :content="`匹配度: ${job.similarity}%`" placement="top">
                        <el-space class="similarity-gauge">
                          <svg width="36" height="36" viewBox="0 0 36 36">
                            <circle class="progress-ring__circle-bg" r="15" cx="18" cy="18" />
                            <circle
                              class="progress-ring__circle"
                              r="15"
                              cx="18"
                              cy="18"
                              :style="{
                                strokeDasharray: `${2 * Math.PI * 15}`,
                                strokeDashoffset: `${2 * Math.PI * 15 * (1 - job.similarity/100)}`
                              }"
                            />
                            <text x="18" y="22" text-anchor="middle" class="progress-text">
                              {{ job.similarity }}
                            </text>
                          </svg>
                        </el-space>
                      </el-tooltip>
                    </el-col>
                  </el-row>
                </template>

                <el-space direction="vertical" fill class="job-body">
                  <el-descriptions border :column="1" size="small" class="job-info">
                    <el-descriptions-item>
                      <template #label>
                        <el-space alignment="center">
                          <el-icon><OfficeBuilding /></el-icon>
                          <span>公司</span>
                        </el-space>
                      </template>
                      <el-text class="info-value company">{{ job.jobAnalysisData.companyName }}</el-text>
                    </el-descriptions-item>
                    <el-descriptions-item>
                      <template #label>
                        <el-space alignment="center">
                          <el-icon><Money /></el-icon>
                          <span>薪资</span>
                        </el-space>
                      </template>
                      <el-text class="info-value salary" type="success">{{ job.jobAnalysisData.salary }}</el-text>
                    </el-descriptions-item>
                    <el-descriptions-item>
                      <template #label>
                        <el-space alignment="center">
                          <el-icon><Location /></el-icon>
                          <span>地点</span>
                        </el-space>
                      </template>
                      <el-text class="info-value location">{{ job.jobAnalysisData.cityName }}</el-text>
                    </el-descriptions-item>
                  </el-descriptions>

                  <el-card class="job-requirements" shadow="hover">
                    <template #header>
                      <el-space alignment="center" class="requirements-header">
                        <el-icon><Document /></el-icon>
                        <span>岗位要求</span>
                      </el-space>
                    </template>

                    <el-scrollbar height="120px" class="requirements-scrollbar">
                      <el-space
                        v-for="(req, index) in job.jobAnalysisData.jobRequirements"
                        :key="index"
                        direction="vertical"
                        fill
                        class="requirement-item"
                      >
                        <el-space alignment="top">
                          <el-text class="requirement-bullet" type="primary">•</el-text>
                          <el-text class="requirement-text">{{ req }}</el-text>
                        </el-space>
                      </el-space>
                    </el-scrollbar>
                  </el-card>

                  <el-row class="job-actions" justify="end">
                    <el-col>
                      <el-button
                        v-if="job.jobAnalysisData.JobUrl"
                        type="primary"
                        plain
                        size="small"
                        @click="openJobLink(job.jobAnalysisData.JobUrl)"
                        class="view-button"
                      >
                        <el-icon><TopRight /></el-icon>
                        查看详情
                      </el-button>
                    </el-col>
                  </el-row>
                </el-space>
              </el-card>
            </el-col>
          </el-row>
        </el-space>
      </div>

      <div class="action-footer">
        <el-row justify="space-between">
          <el-col :xs="24" :sm="12">
            <el-button type="info" plain @click="prev" class="prev-button">
              <el-icon><ArrowLeft /></el-icon> 返回
            </el-button>
          </el-col>
          <el-col :xs="24" :sm="12" style="text-align: right">
            <el-button type="primary" @click="reset" class="reset-button">
              <el-icon><RefreshRight /></el-icon> 重新开始
            </el-button>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <el-alert
      v-if="jobs.length > 0"
      type="info"
      :closable="false"
      show-icon
      class="helper-tips"
    >
      <template #title>
        <el-text class="tips-title" tag="b">匹配提示</el-text>
      </template>
      <el-text class="tips-text">
        以上职位由AI根据您的简历技能自动匹配，考虑了技能相关性、工作经验和教育背景等多方面因素。
        职位匹配度越高，表示您的简历与该职位的匹配程度越高。
      </el-text>
    </el-alert>
  </el-space>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits, ref, computed } from 'vue'
import type { UserSimilarity } from '@/api/ai/ai.d'
import {
  Briefcase,
  Medal,
  Loading,
  Search,
  CircleCheckFilled,
  DocumentDelete,
  RefreshRight,
  Aim,
  Filter,
  OfficeBuilding,
  Money,
  Location,
  Document,
  TopRight,
  ArrowLeft
} from '@element-plus/icons-vue'

const props = defineProps<{
  jobs: UserSimilarity[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'prev'): void
  (e: 'reset'): void
  (e: 'openJobLink', url: string): void
}>()

const sortBy = ref('similarity')
const searchKeyword = ref('')
const loadingProgress = computed(() => Math.floor(Math.random() * 30) + 60)

// 根据排序和搜索过滤职位
const filteredJobs = computed(() => {
  let result = [...props.jobs]

  // 搜索过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(job =>
      job.jobAnalysisData.positionName.toLowerCase().includes(keyword) ||
      job.jobAnalysisData.companyName.toLowerCase().includes(keyword)
    )
  }

  // 排序
  if (sortBy.value === 'similarity') {
    return result.sort((a, b) => b.similarity - a.similarity)
  } else if (sortBy.value === 'salary') {
    return result.sort((a, b) => {
      return b.jobAnalysisData.salaryValue - a.jobAnalysisData.salaryValue
    })
  }

  return result
})

const prev = () => {
  emit('prev')
}

const reset = () => {
  emit('reset')
}

const openJobLink = (url: string) => {
  emit('openJobLink', url)
}
</script>

<style scoped>
.step-content {
  width: 100%;
  min-height: 600px;
  display: flex;
  flex-direction: column;
}

.content-card {
  border-radius: 8px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.title-section {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.content-wrapper {
  padding: 0 20px 20px;
  flex: 1;
  overflow-y: auto;
}

.helper-tips {
  margin-top: 15px;
}

.action-footer {
  display: flex;
  justify-content: space-between;
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  margin-top: auto;
}

.loading-spinner {
  font-size: 30px;
  color: #409EFF;
  animation: spin 1.5s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-details {
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 15px;
}

.progress-icon.success {
  color: #67C23A;
}

.progress-icon.loading {
  color: #409EFF;
  animation: spin 1.5s linear infinite;
}

.match-summary, .filter-controls {
  margin: 0 20px 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
}

.jobs-container {
  padding: 0 20px;
  flex: 1;
  overflow-y: auto;
  max-height: calc(100% - 140px);
}

.job-item {
  height: 100%;
  border-radius: 8px;
  margin-bottom: 15px;
}

.job-header {
  padding: 10px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.job-title {
  font-weight: 600;
}

.progress-ring__circle-bg {
  stroke: #e6e6e6;
  fill: transparent;
  stroke-width: 3;
}

.progress-ring__circle {
  stroke: #67c23a;
  fill: transparent;
  stroke-width: 3;
  transform: rotate(-90deg);
  transform-origin: 50% 50%;
}

.progress-text {
  font-size: 10px;
  fill: #67c23a;
  font-weight: bold;
}

.job-body {
  padding: 12px;
}

.job-requirements {
  margin-top: 12px;
}

@media (max-width: 768px) {
  .filter-left, .filter-right {
    margin-bottom: 10px;
  }

  .action-footer {
    flex-direction: column;
    gap: 10px;
  }

  .action-footer .el-button {
    width: 100%;
    margin-bottom: 8px;
  }
}
</style>
