<template>
  <div class="section">
    <TitleView title="教育背景" iconName="School" />
    <div class="timeline" v-for="(edu, index) in education || defaultEducation" :key="'edu-'+index">
      <div class="timeline-item">
        <div class="time-range">{{ edu.startDate }} ~ {{ edu.endDate }}</div>
        <div class="institution">{{ edu.school }}</div>
        <div class="position">{{ edu.major }}</div>
        <div class="details">
          <div v-if="edu.gpa">专业成绩：{{ edu.gpa }}</div>
          <div v-if="edu.courses">专业课程：{{ edu.courses }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { defineProps } from 'vue'
import TitleView from '../common/TitleView.vue'

interface Education {
  startDate: string;
  endDate: string;
  school: string;
  major: string;
  gpa: string;
  courses: string;
}

defineProps<{
  education?: Education[]
}>()

const defaultEducation: Education[] = [
  {
    startDate: '2012-09',
    endDate: '2016-07',
    school: '全民简历师范大学',
    major: '工商管理（本科）',
    gpa: 'GPA 3.86/4（专业前5%）',
    courses: '统计分析学、市场研究学、组织行为学、经济法概论、财务会计学、数据学管理、国际行为学、市场营销学、国际贸易理论、国际贸易实务、人力资源开发与管理、财务管理学、企业战略管理概论、资源管理学、资源管理与优化等。'
  }
]
</script>

<style scoped>
.section {
  margin-bottom: 30px;
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
</style>
