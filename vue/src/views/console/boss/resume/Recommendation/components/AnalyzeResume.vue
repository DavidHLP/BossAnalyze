<template>
  <el-space direction="vertical" fill class="step-content">
    <el-card class="content-card">
      <el-row class="title-section">
        <el-col :span="24">
          <el-space alignment="center" class="section-title">
            <el-icon><DataAnalysis /></el-icon>
            <span>简历分析</span>
            <el-badge value="AI" class="ai-badge">
              <el-icon><Lightning /></el-icon>
            </el-badge>
          </el-space>
        </el-col>
      </el-row>

      <div v-if="!analyzing" class="analysis-status-section">
        <div class="analysis-status-row">
          <div class="analysis-complete">
            <div class="status-wrapper">
              <el-icon class="status-icon"><CircleCheck /></el-icon>
              <span class="status-text">分析完成</span>
              <el-tag size="small" effect="dark" type="success" class="status-tag">100%</el-tag>
            </div>
            <div class="analysis-description">您的简历已成功分析，以下是简历分析结果</div>
          </div>
        </div>
      </div>

      <el-container class="content-wrapper">
        <template v-if="analyzing">
          <el-result icon="info" title="正在分析简历数据">
            <template #icon>
              <el-icon class="analyzing-spinner"><Loading /></el-icon>
            </template>
            <template #extra>
              <el-skeleton :rows="6" animated />
              <el-text class="analyzing-text">
                <el-icon><Timer /></el-icon>
                正在分析简历数据，请稍候...
              </el-text>
              <el-card class="analysis-details">
                <el-progress :percentage="analysisProgress" :stroke-width="10" class="mb-3"></el-progress>
                <el-space direction="vertical" alignment="flex-start" size="small">
                  <el-space alignment="center">
                    <el-icon class="progress-icon success"><CircleCheckFilled /></el-icon>
                    <span>基本信息分析</span>
                  </el-space>
                  <el-space alignment="center">
                    <el-icon class="progress-icon success"><CircleCheckFilled /></el-icon>
                    <span>技能匹配分析</span>
                  </el-space>
                  <el-space alignment="center">
                    <el-icon class="progress-icon loading"><Loading /></el-icon>
                    <span>职位需求对比中...</span>
                  </el-space>
                </el-space>
              </el-card>
            </template>
          </el-result>
        </template>

        <template v-else>
          <el-row class="analysis-result" :gutter="20">
            <el-col :lg="10" :md="12">
              <el-card class="resume-profile" shadow="hover">
                <div class="profile-info-section">
                  <div class="profile-name">
                    <el-icon><User /></el-icon>
                    {{ resumeData.name }}
                  </div>
                  <div class="profile-position">
                    <el-icon><Briefcase /></el-icon>
                    <span class="info-label">期望职位:</span>
                    <el-tag size="small" effect="plain" type="primary">
                      {{ resumeData.jobTarget || '未设置' }}
                    </el-tag>
                  </div>
                  <div class="profile-city">
                    <el-icon><Location /></el-icon>
                    <span class="info-label">期望城市:</span>
                    <el-tag size="small" effect="plain" type="info">
                      {{ Array.isArray(resumeData.targetCity) && resumeData.targetCity.length > 0 ? resumeData.targetCity.join('/') : '未设置' }}
                    </el-tag>
                  </div>
                  <div class="profile-experience">
                    <el-icon><Clock /></el-icon>
                    <span class="info-label">工作经验:</span>
                    <el-tag type="primary" size="small" round>{{ resumeData.experience || 0 }}年</el-tag>
                  </div>
                  <div class="profile-education">
                    <el-icon><School /></el-icon>
                    <span class="info-label">教育背景:</span>
                    <el-popover
                      placement="right"
                      :width="320"
                      trigger="hover"
                      v-if="isEducationArray(resumeData.education)"
                    >
                      <template #reference>
                        <el-tag size="small" effect="plain" type="warning">
                          {{ getLatestEducation(resumeData.education) }}
                        </el-tag>
                      </template>
                      <el-space v-for="(edu, index) in resumeData.education" :key="index" direction="vertical" fill>
                        <el-descriptions :column="1" border>
                          <el-descriptions-item label="学校">{{ edu.school }}</el-descriptions-item>
                          <el-descriptions-item label="时间">{{ edu.startDate }} - {{ edu.endDate }}</el-descriptions-item>
                          <el-descriptions-item label="专业">
                            <el-tag size="small" type="primary">{{ edu.major }}</el-tag>
                            <el-tag size="small" type="success" v-if="edu.gpa">{{ edu.gpa }}</el-tag>
                          </el-descriptions-item>
                          <el-descriptions-item label="相关课程" v-if="edu.courses">
                            {{ edu.courses }}
                          </el-descriptions-item>
                        </el-descriptions>
                        <el-divider v-if="index < resumeData.education.length - 1" />
                      </el-space>
                    </el-popover>
                    <el-tag v-else size="small" effect="plain" type="warning">
                      {{ resumeData.education || '未设置' }}
                    </el-tag>
                  </div>
                </div>

                <div class="completion-rate">
                  <div class="rate-header">
                    简历完整度
                    <span class="rate-value">{{ calculateCompletionRate() }}%</span>
                  </div>
                  <el-progress
                    :percentage="calculateCompletionRate()"
                    :status="getProgressStatus()"
                    :stroke-width="6"
                  ></el-progress>
                </div>
              </el-card>
            </el-col>

            <el-col :lg="10" :md="12">
              <el-card class="skills-section" shadow="hover">
                <div class="skills-header">
                  <el-icon><Lightning /></el-icon>
                  <span>核心技能与优势</span>
                </div>

                <template v-if="coreSkills.length > 0">
                  <el-space wrap class="skill-tags">
                    <el-tag
                      v-for="(skill, index) in coreSkills"
                      :key="index"
                      class="skill-tag"
                      effect="light"
                      round
                    >
                      <el-icon><Check /></el-icon>
                      {{ skill }}
                    </el-tag>
                  </el-space>

                  <el-card shadow="hover" class="analysis-card">
                    <div class="card-header">
                      <el-icon><DataLine /></el-icon>
                      <span>技能匹配分析</span>
                    </div>

                    <div class="analysis-metrics">
                      <div class="metric-item">
                        <div class="metric-header">
                          <span>技术技能与职位匹配度</span>
                          <el-tag type="success" size="small" round>78%</el-tag>
                        </div>
                        <el-progress :percentage="78" :stroke-width="6" status="success" class="mb-2"></el-progress>
                      </div>

                      <div class="metric-item">
                        <div class="metric-header">
                          <span>核心能力覆盖率</span>
                          <el-tag type="primary" size="small" round>85%</el-tag>
                        </div>
                        <el-progress :percentage="85" :stroke-width="6" class="mb-2"></el-progress>
                      </div>

                      <div class="metric-item">
                        <div class="metric-header">
                          <span>市场竞争力评估</span>
                          <el-tag type="warning" size="small" round>66%</el-tag>
                        </div>
                        <el-progress :percentage="66" :stroke-width="6" status="warning"></el-progress>
                      </div>
                    </div>
                  </el-card>

                  <el-alert
                    type="info"
                    :closable="false"
                    class="recommendation-alert"
                    show-icon
                  >
                    <template #title>
                      <span class="alert-title">AI 推荐建议</span>
                    </template>
                    <el-text class="alert-content">
                      基于您的技能分析，建议强化市场竞争力较高的核心技能，提高简历中的项目经验描述，以增强求职竞争力。
                    </el-text>
                  </el-alert>
                </template>

                <div v-else class="no-skills">
                  <div class="warning-icon">
                    <el-icon style="font-size: 40px; color: #E6A23C"><WarningFilled /></el-icon>
                  </div>
                  <div class="warning-text">
                    未能识别核心技能，请确保简历中包含相关技能信息
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </template>
      </el-container>

      <el-divider />
      <el-row justify="space-between" class="action-footer">
        <el-col :xs="24" :sm="12">
          <el-button type="info" plain @click="prev" class="prev-button">
            <el-icon><ArrowLeft /></el-icon> 上一步
          </el-button>
        </el-col>
        <el-col :xs="24" :sm="12" style="text-align: right">
          <el-button type="primary" :disabled="analyzing" @click="next" class="next-button">
            查看推荐职位 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-alert
      v-if="!analyzing"
      type="info"
      :closable="false"
      show-icon
      class="helper-tips"
    >
      <template #title>
        <el-text class="tips-title" tag="b">分析提示</el-text>
      </template>
      <el-text class="tips-text">AI分析已完成，您可以查看推荐的职位匹配结果，或返回上一步重新选择简历。确保您的简历信息完整以获得更准确的推荐。</el-text>
    </el-alert>
  </el-space>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits, computed } from 'vue'
import type { ResumeData } from '@/api/resume/resume.d'
import {
  DataAnalysis,
  Lightning,
  Loading,
  Timer,
  CircleCheckFilled,
  CircleCheck,
  User,
  Briefcase,
  Location,
  Clock,
  School,
  Check,
  DataLine,
  WarningFilled,
  ArrowLeft,
  ArrowRight
} from '@element-plus/icons-vue'

// 定义教育经历接口
interface Education {
  startDate: string;
  endDate: string;
  school: string;
  major: string;
  gpa?: string;
  courses?: string;
}

defineProps<{
  resumeData: ResumeData
  analyzing: boolean
  coreSkills: string[]
}>()

const emit = defineEmits<{
  (e: 'prev'): void
  (e: 'next'): void
}>()

const analysisProgress = computed(() => {
  return 75
})

// 计算简历完整度
const calculateCompletionRate = () => {
  return 85
}

// 获取进度条状态
const getProgressStatus = () => {
  const rate = calculateCompletionRate()
  if (rate > 80) return 'success'
  if (rate > 60) return ''
  if (rate > 40) return 'warning'
  return 'exception'
}

// 判断是否为教育经历数组
const isEducationArray = (education: unknown): boolean => {
  return Array.isArray(education) && education.length > 0
}

// 获取最新的教育经历
const getLatestEducation = (education: Education[]): string => {
  if (Array.isArray(education) && education.length > 0) {
    const latest = education[0]
    return `${latest.school} - ${latest.major}`
  }
  return '未设置'
}

const prev = () => {
  emit('prev')
}

const next = () => {
  emit('next')
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
  margin-bottom: 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.analysis-status-section {
  background-color: #f8f9fa;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 20px;
}

.analysis-status-row {
  padding: 12px 0 8px 0;
}

.analysis-complete {
  text-align: left;
}

.status-wrapper {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.status-icon {
  color: #67C23A;
  font-size: 18px;
  margin-right: 6px;
}

.status-text {
  font-size: 15px;
  font-weight: 500;
  margin-right: 8px;
}

.status-tag {
  background-color: #67C23A;
  padding: 0 5px;
  height: 20px;
  line-height: 18px;
}

.analysis-description {
  color: #606266;
  font-size: 13px;
  margin-left: 24px;
}

.content-wrapper {
  padding: 0 20px 20px;
  flex: 1;
  overflow-y: auto;
}

.analysis-result {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin: 20px 0;
}

.resume-profile, .skills-section {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  height: 100%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.skills-header, .profile-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  margin-bottom: 15px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.profile-info-section {
  margin-bottom: 20px;
}

.profile-position, .profile-city, .profile-experience, .profile-education {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed rgba(0, 0, 0, 0.05);
}

.info-label {
  margin: 0 8px;
  color: #606266;
  min-width: 70px;
}

.completion-rate {
  margin-top: 15px;
}

.rate-header {
  display: flex;
  justify-content: space-between;
  font-weight: 500;
  margin-bottom: 10px;
}

.rate-value {
  color: #409EFF;
  font-weight: 600;
}

.analyzing-spinner {
  font-size: 30px;
  color: #409EFF;
  animation: spin 1.5s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.analyzing-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: 15px 0;
}

.progress-icon.success {
  color: #67C23A;
}

.progress-icon.loading {
  color: #409EFF;
  animation: spin 1.5s linear infinite;
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 15px;
}

.skill-tag {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 15px;
  padding-bottom: 8px;
  border-bottom: 1px dashed rgba(0, 0, 0, 0.05);
}

.analysis-metrics {
  margin-top: 10px;
}

.metric-item {
  margin-bottom: 15px;
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.mb-2 {
  margin-bottom: 10px;
}

.no-skills {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  text-align: center;
}

.warning-icon {
  margin-bottom: 15px;
}

.warning-text {
  color: #E6A23C;
  font-size: 14px;
}

.helper-tips {
  margin-top: 15px;
}

.action-footer {
  padding: 16px 20px;
  margin-top: auto;
}

@media (max-width: 768px) {
  .action-footer {
    flex-direction: column;
    gap: 10px;
  }

  .action-footer .el-button {
    width: 100%;
    margin-bottom: 8px;
  }

  .resume-profile, .skills-section {
    margin-bottom: 15px;
  }
}
</style>
