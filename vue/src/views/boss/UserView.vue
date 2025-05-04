<template>
  <div class="user-view-container">
    <SalaryHotJob />
    <JobScatterPlot />
  </div>
</template>

<script setup lang="ts">
import SalaryHotJob from "@/views/boss/user/SalaryAnalysisView.vue"
import JobScatterPlot from "@/views/boss/user/JobScatterPlot.vue"
</script>

<style scoped>
.user-view-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100%;
  padding-bottom: 30px; /* 添加底部内边距 */
}
</style>