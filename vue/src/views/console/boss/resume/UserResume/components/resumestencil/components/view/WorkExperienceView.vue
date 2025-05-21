<template>
  <div class="section">
    <TitleView title="工作经验" iconName="OfficeBuilding" />
    <div class="timeline" v-for="(exp, index) in workExperience" :key="'work-'+index">
      <div class="timeline-item">
        <div class="time-range">{{ exp.startDate }} ~ {{ exp.endDate }}</div>
        <div class="institution">{{ exp.company }}</div>
        <div class="position">{{ exp.position }}</div>
        <div class="details">
          <div v-for="(duty, dIndex) in exp.duties" :key="'duty-'+dIndex">
            • {{ duty }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { defineProps } from 'vue'
import TitleView from '../common/TitleView.vue'

// 类型定义
interface WorkExperience {
  startDate: string;
  endDate: string;
  company: string;
  position: string;
  duties: string[];
}

// Props 定义
defineProps<{
  workExperience?: WorkExperience[]
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
