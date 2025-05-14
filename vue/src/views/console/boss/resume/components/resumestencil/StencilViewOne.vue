<template>
  <div class="resume-wrapper">
    <div class="resume-container">
      <!-- 左侧栏 -->
      <div class="left-sidebar">
        <div class="sidebar-content">
          <BasicInfoView :basicInfo="resume" />
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

        <!-- 教育背景 -->
        <EducationalBackgroundView :education="resume.education" />

        <!-- 工作经验 -->
        <WorkExperienceView :workExperience="resume.workExperience" />

        <!-- 证书资质 -->
        <CertificateView
          :certificates="resume.certificates"
        />

        <!-- 自我评价 -->
        <SelfEvaluationView :evaluation="resume.selfEvaluation" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import BasicInfoView from './components/view/BasicInfoView.vue'
import InterestTagsView from './components/view/InterestTagsView.vue'
import JobIntentionView from './components/view/JobIntentionView.vue'
import CertificateView from './components/view/CertificateView.vue'
import EducationalBackgroundView from './components/view/EducationalBackgroundView.vue'
import SelfEvaluationView from './components/view/Self-EvaluationView.vue'
import WorkExperienceView from './components/view/WorkExperienceView.vue'
import type { ResumeData } from '@/mock/resumeData'

// Props
defineProps<{
  resume: ResumeData
}>()
</script>

<style scoped>
.resume-wrapper {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.resume-container {
  display: flex;
  font-family: Arial, sans-serif;
  width: 100%;
  max-width: 1000px;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
  border-radius: 5px;
  overflow: hidden;
  min-height: 100%;
  /* 确保左右两侧等高的关键属性 */
  align-items: stretch;
}

.left-sidebar {
  width: 250px;
  min-width: 250px;
  background-color: #34495e;
  color: white;
  flex-shrink: 0;
}

.sidebar-content {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.avatar-container {
  text-align: center;
  margin-bottom: 20px;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 5px;
  object-fit: cover;
}

.personal-info {
  margin-bottom: 30px;
}

.info-item {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  font-size: 14px;
  line-height: 1.6;
}

.info-item i {
  margin-right: 10px;
}

.skill-section, .interest-section {
  margin-bottom: 25px;
}

.interest-section {
  margin-top: auto; /* 将兴趣部分推到底部 */
  padding-bottom: 20px;
  flex-grow: 1; /* 使其撑满剩余空间 */
  display: flex;
  flex-direction: column;
}

.interest-section h3 {
  border-bottom: 1px solid #7f8c8d;
  padding-bottom: 8px;
  margin-bottom: 15px;
  font-size: 1.15rem;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.skill-section h3, .interest-section h3 {
  border-bottom: 1px solid #7f8c8d;
  padding-bottom: 8px;
  margin-bottom: 15px;
  font-size: 1.15rem;
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
  padding: 30px;
  background-color: white;
  min-width: 500px;
}

.header {
  margin-bottom: 30px;
}

.name {
  color: #2c3e50;
  font-size: 28px;
  margin-bottom: 15px;
}

.section {
  margin-bottom: 30px;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  border-bottom: 2px solid #3498db;
  padding-bottom: 5px;
}

.icon-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #3498db;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  margin-right: 10px;
}

.section-header h2 {
  color: #2c3e50;
  font-size: 18px;
  margin: 0;
}

.timeline-item {
  margin-bottom: 20px;
}

.time-range {
  color: #7f8c8d;
  font-size: 14px;
  margin-bottom: 5px;
}

.institution {
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 5px;
}

.position {
  color: #e74c3c;
  margin-bottom: 10px;
}

.details {
  font-size: 14px;
  line-height: 1.6;
}

.certificate-content, .self-evaluation {
  font-size: 14px;
  line-height: 1.6;
}

/* 响应式设计 */
@media screen and (max-width: 1024px) {
  .resume-container {
    flex-direction: row !important; /* 强制保持水平布局 */
    max-width: 900px;
  }

  .right-content {
    min-width: 400px;
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
}

@media print {
  .resume-wrapper {
    padding: 0;
    background-color: white;
  }

  .resume-container {
    max-width: 100%;
    overflow-x: visible;
    flex-direction: row !important; /* 打印时也保持水平布局 */
    box-shadow: none;
  }

  .left-sidebar, .right-content {
    min-width: auto;
  }
}
</style>
