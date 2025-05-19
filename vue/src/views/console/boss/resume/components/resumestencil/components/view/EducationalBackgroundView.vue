<template>
  <div class="section">
    <TitleView title="教育背景" iconName="School" />
    <div class="timeline" v-for="(edu, index) in education" :key="'edu-'+index">
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
