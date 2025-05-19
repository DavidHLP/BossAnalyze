<template>
  <div class="job-info">
    <div class="job-item">
      <span class="label">求职意向：</span>
      <span class="value">{{ jobIntention.jobTarget}}</span>
    </div>
    <div class="job-item">
      <span class="label">期望薪资：</span>
      <span class="value">{{ jobIntention.expectedSalary}}</span>
    </div>
    <div class="job-item">
      <span class="label">意向城市：</span>
      <span class="value">{{ jobIntention.targetCity}}</span>
    </div>
    <div class="job-item">
      <span class="label">入职时间：</span>
      <span class="value">{{ jobIntention.availableTime}}</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
interface JobIntention {
  jobTarget?: string;
  expectedSalary?: string;
  targetCity?: string;
  availableTime?: string;
}

defineProps<{
  jobIntention: JobIntention
}>()
</script>

<style scoped>
.job-info {
  display: flex;
  flex-wrap: wrap;
  background-color: #f8f9fa;
  padding: 12px 15px;
  border-radius: 8px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
  margin-bottom: 25px;
}

.job-item {
  margin-right: 25px;
  margin-bottom: 10px;
  padding: 5px 0;
  position: relative;
}

.job-item:not(:last-child)::after {
  content: '';
  position: absolute;
  right: -12.5px;
  top: 50%;
  height: 70%;
  width: 1px;
  background-color: #e0e0e0;
  transform: translateY(-50%);
}

.label {
  color: #7f8c8d;
  font-size: 14px;
  margin-right: 4px;
}

.value {
  color: #2c3e50;
  font-weight: 600;
  font-size: 15px;
  transition: color 0.2s ease;
}

.job-item:hover .value {
  color: #3498db;
}
</style>
