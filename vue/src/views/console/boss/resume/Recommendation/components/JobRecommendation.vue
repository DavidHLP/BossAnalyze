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

          <!-- 使用类似SelectResume的网格布局 -->
          <div class="jobs-content">

            <el-scrollbar height="400px" class="jobs-scrollbar">
              <div class="jobs-grid">
                <div v-for="job in filteredJobs" :key="job.id" class="job-item-wrapper">
                  <el-card
                    :class="{'job-item-active': selectedJobId === String(job.id)}"
                    class="job-item"
                    shadow="hover"
                    @click="onSelectJob(String(job.id))"
                  >
                    <div class="job-header-row">
                      <span class="job-title" :title="job.jobAnalysisData.positionName">
                        {{ job.jobAnalysisData.positionName }}
                      </span>
                      <el-tooltip :content="`匹配度: ${job.similarity}%`" placement="top">
                        <el-space class="similarity-gauge">
                          <svg width="30" height="30" viewBox="0 0 36 36">
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
                    </div>

                    <div class="job-info-grid">
                      <div class="info-row">
                        <el-icon class="info-icon"><OfficeBuilding /></el-icon>
                        <div class="info-content">
                          <div class="info-label">公司</div>
                          <div class="info-value">{{ job.jobAnalysisData.companyName }}</div>
                        </div>
                      </div>
                      <div class="info-row">
                        <el-icon class="info-icon"><Money /></el-icon>
                        <div class="info-content">
                          <div class="info-label">薪资</div>
                          <div class="info-value salary">{{ job.jobAnalysisData.salary }}</div>
                        </div>
                      </div>
                      <div class="info-row">
                        <el-icon class="info-icon"><Location /></el-icon>
                        <div class="info-content">
                          <div class="info-label">城市</div>
                          <div class="info-value">{{ job.jobAnalysisData.cityName }}</div>
                        </div>
                      </div>
                      <div class="info-row">
                        <el-icon class="info-icon"><View /></el-icon>
                        <div class="info-content">
                          <div class="info-label">操作</div>
                          <div class="info-value">
                            <el-space>
                              <el-button type="primary" size="small" plain @click.stop="showJobDetails(job)">
                                详情
                              </el-button>
                              <el-button
                                v-if="job.jobAnalysisData.JobUrl"
                                type="success"
                                size="small"
                                plain
                                @click.stop="openJobLink(job.jobAnalysisData.JobUrl)"
                              >
                                投递
                              </el-button>
                            </el-space>
                          </div>
                        </div>
                      </div>
                    </div>
                  </el-card>
                </div>
              </div>
            </el-scrollbar>
          </div>
        </el-space>
      </div>

      <el-divider />
      <el-row justify="space-between" class="action-footer">
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
    </el-card>

    <!-- 职位详情抽屉 -->
    <el-drawer
      v-model="showDetailsDrawer"
      title="职位详情"
      direction="rtl"
      size="450px"
      :with-header="false"
      class="job-details-drawer"
    >
      <CompanyDetailsCard
        v-if="selectedJobDetails"
        :company-info="selectedJobDetails"
        @openCompanyUrl="openCompanyUrl"
        @openJobUrl="openJobUrl"
        @openGoogleMaps="openGoogleMaps"
      />
    </el-drawer>
  </el-space>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits, ref, computed } from 'vue'
import type { UserSimilarity } from '@/api/ai/ai.d'
import type { CompanyInfo } from '@/api/boss/user/user.d'
import CompanyDetailsCard from '@/views/console/boss/user/components/CompanyDetailsCard.vue'
import {
  Briefcase,
  Medal,
  Loading,
  Search,
  CircleCheckFilled,
  DocumentDelete,
  RefreshRight,
  OfficeBuilding,
  Money,
  Location,
  ArrowLeft,
  View
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
const selectedJobId = ref('')
const showDetailsDrawer = ref(false)
const selectedJobDetails = ref<CompanyInfo | null>(null)
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

// 选择职位
const onSelectJob = (id: string) => {
  selectedJobId.value = id
}

// 显示职位详情
const showJobDetails = (job: UserSimilarity) => {
  selectedJobId.value = String(job.id)
  // 转换数据格式以适配CompanyDetailsCard
  selectedJobDetails.value = {
    companyName: job.jobAnalysisData.companyName,
    positionName: job.jobAnalysisData.positionName,
    cityName: job.jobAnalysisData.cityName,
    salary: job.jobAnalysisData.salary,
    degree: job.jobAnalysisData.degree || '不限',
    experience: job.jobAnalysisData.experience || '不限',
    companySize: job.jobAnalysisData.companySize || '未知',
    financingStage: job.jobAnalysisData.financingStage || '未知',
    address: job.jobAnalysisData.address || '',
    employeeBenefits: job.jobAnalysisData.employeeBenefits || [],
    jobRequirements: job.jobAnalysisData.jobRequirements || [],
    companyUrl: job.jobAnalysisData.companyUrl || '',
    jobUrl: job.jobAnalysisData.JobUrl || ''
  }
  showDetailsDrawer.value = true
}

const prev = () => {
  emit('prev')
}

const reset = () => {
  emit('reset')
}

const openJobLink = (url: string) => {
  emit('openJobLink', url)
}

// 新增的方法
const openCompanyUrl = () => {
  if (selectedJobDetails.value?.companyUrl) {
    window.open(selectedJobDetails.value.companyUrl, '_blank')
  }
}

const openJobUrl = () => {
  if (selectedJobDetails.value?.jobUrl) {
    window.open(selectedJobDetails.value.jobUrl, '_blank')
  }
}

const openGoogleMaps = () => {
  if (selectedJobDetails.value?.address) {
    const encodedAddress = encodeURIComponent(selectedJobDetails.value.address)
    window.open(`https://www.google.com/maps/search/?api=1&query=${encodedAddress}`, '_blank')
  }
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
  padding: 16px 20px;
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

.jobs-content {
  padding: 0 20px;
}

.jobs-count {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.count-text {
  font-size: 13px;
  color: #606266;
  margin-left: 8px;
}

.jobs-scrollbar {
  width: 100%;
  padding: 10px 0;
}

.jobs-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  padding: 0 10px;
}

.job-item-wrapper {
  margin-bottom: 0;
  flex: 1 1 280px;
  min-width: 280px;
  max-width: 350px;
}

.job-item {
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  padding: 12px 16px;
  height: auto;
  min-height: 280px;
  display: flex;
  flex-direction: column;
  border: 1px solid #EBEEF5;
}

.job-item:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.job-item-active {
  border: 2px solid #409EFF;
  background-color: rgba(64, 158, 255, 0.05);
}

.job-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.job-title {
  font-weight: 600;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%;
}

.job-info-grid {
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

.info-value.salary {
  color: #67C23A;
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

@media (max-width: 768px) {
  .jobs-grid {
    gap: 15px;
  }

  .job-item-wrapper {
    flex: 1 1 100%;
    min-width: 100%;
    max-width: 100%;
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
