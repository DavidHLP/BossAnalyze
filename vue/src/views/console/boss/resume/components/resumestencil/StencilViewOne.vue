<template>
  <div class="resume-wrapper">
    <div class="resume-container">
      <!-- 左侧栏 -->
      <div class="left-sidebar">
        <div class="sidebar-content">
          <BasicInfoView :basicInfo="resume" />
          <!-- 兴趣爱好 -->
          <div class="interest-section">
            <h3>兴趣爱好</h3>
            <InterestTagsView :interestTags="resume.interestTags || []" />
          </div>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="right-content">
        <div class="header">
          <h1 class="name">{{ resume.name || '' }}</h1>
          <JobIntentionView :jobIntention="resume" />
        </div>

        <!-- 动态渲染各部分，根据sectionOrder排序 -->
        <template v-for="sectionType in orderedSections" :key="sectionType">
          <!-- 教育背景 -->
          <EducationalBackgroundView
            v-if="sectionType === 'education' && resume.education && resume.education.length > 0"
            :education="resume.education"
          />

          <!-- 工作经验 -->
          <WorkExperienceView
            v-if="sectionType === 'workExperience' && resume.workExperience && resume.workExperience.length > 0"
            :workExperience="resume.workExperience"
          />

          <!-- 证书资质 -->
          <CertificateView
            v-if="sectionType === 'certificates' && resume.certificates && resume.certificates.length > 0"
            :certificates="resume.certificates"
          />
          <!-- 自我评价 -->
          <SelfEvaluationView
            v-if="sectionType === 'selfEvaluation' && resume.selfEvaluation"
            :evaluation="resume.selfEvaluation || ''"
          />
        </template>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import BasicInfoView from './components/view/BasicInfoView.vue'
import InterestTagsView from './components/view/InterestTagsView.vue'
import JobIntentionView from './components/view/JobIntentionView.vue'
import CertificateView from './components/view/CertificateView.vue'
import EducationalBackgroundView from './components/view/EducationalBackgroundView.vue'
import SelfEvaluationView from './components/view/Self-EvaluationView.vue'
import WorkExperienceView from './components/view/WorkExperienceView.vue'
import type { ResumeData } from '@/api/resume/resume.d'

// Props
const props = defineProps<{
  resume: ResumeData
}>()

// 根据sectionOrder计算组件显示顺序
const orderedSections = computed(() => {
  // 若未提供顺序，则使用默认顺序
  const defaultOrder = ['education', 'workExperience', 'certificates', 'interestTags', 'selfEvaluation'];
  return props.resume.sectionOrder || defaultOrder;
})
</script>

<style scoped>
.resume-wrapper {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100%;
  width: 100%;
}

.resume-container {
  display: flex;
  width: 100%;
  max-width: 1000px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  overflow: hidden;
  min-height: 100%;
  background-color: white;
  align-items: stretch;
}

.left-sidebar {
  width: 250px;
  min-width: 250px;
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  color: white;
  flex-shrink: 0;
}

.sidebar-content {
  padding: 30px 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.avatar-container {
  text-align: center;
  margin-bottom: 25px;
}

.avatar {
  width: 140px;
  height: 140px;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
  border: 3px solid rgba(255, 255, 255, 0.2);
}

.personal-info {
  margin-bottom: 30px;
}

.info-item {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  font-size: 14px;
  line-height: 1.6;
}

.info-item i {
  margin-right: 12px;
}

.skill-section, .interest-section {
  margin-bottom: 30px;
}

.interest-section {
  margin-top: auto;
  padding-bottom: 20px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.skill-section h3, .interest-section h3 {
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  padding-bottom: 10px;
  margin-bottom: 18px;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.skill-content {
  font-size: 14px;
  line-height: 1.6;
}

.right-content {
  flex: 1;
  padding: 40px;
  background-color: white;
  min-width: 500px;
}

.header {
  margin-bottom: 35px;
}

.name {
  color: #2c3e50;
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 15px;
  letter-spacing: 0.5px;
}

.section {
  margin-bottom: 35px;
  padding-bottom: 5px;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 2px solid #3B82F6;
  padding-bottom: 8px;
}

.icon-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #3B82F6;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  margin-right: 12px;
  box-shadow: 0 2px 5px rgba(59, 130, 246, 0.3);
}

.section-header h2 {
  color: #2c3e50;
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.timeline-item {
  margin-bottom: 25px;
  padding-left: 20px;
  border-left: 2px solid #e2e8f0;
  position: relative;
}

.timeline-item:last-child {
  margin-bottom: 0;
}

.timeline-item:before {
  content: "";
  position: absolute;
  left: -6px;
  top: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #3B82F6;
}

.time-range {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 8px;
  font-weight: 500;
}

.institution {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
  font-size: 16px;
}

.position {
  color: #3B82F6;
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 15px;
}

.details {
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
}

.certificate-content, .self-evaluation {
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
}

:deep(.el-tag) {
  margin: 4px;
  padding: 6px 12px;
  border-radius: 16px;
}

:deep(.el-icon) {
  vertical-align: middle;
}

/* 响应式设计 */
@media screen and (max-width: 1024px) {
  .resume-container {
    flex-direction: row !important;
    max-width: 900px;
  }

  .right-content {
    min-width: 400px;
    padding: 30px;
  }
}

@media screen and (max-width: 768px) {
  .resume-wrapper {
    padding: 10px;
  }

  .resume-container {
    max-width: 100%;
    overflow-x: auto;
  }

  .left-sidebar {
    width: 220px;
    min-width: 220px;
  }

  .right-content {
    padding: 25px;
  }

  .name {
    font-size: 28px;
  }
}

@media print {
  .resume-wrapper {
    padding: 0;
    background-color: white;
  }

  .resume-container {
    max-width: 100%;
    overflow-x: visible;
    flex-direction: row !important;
    box-shadow: none;
  }

  .left-sidebar, .right-content {
    min-width: auto;
  }
}
</style>
