<template>
  <div class="user-view-container dark-theme">
    <!-- 页面介绍部分 -->
    <div class="intro-section">
      <div class="intro-header">
        <h1>BOSS直聘数据分析</h1>
        <p class="intro-subtitle">探索职场趋势，把握就业方向</p>
      </div>
      <div class="intro-content">
        <div class="intro-card">
          <div class="intro-icon">
            <i class="el-icon-data-analysis"></i>
          </div>
          <div class="intro-text">
            <h3>薪资分析</h3>
            <p>全面展示各行业薪资水平、薪资区间和城市分布</p>
          </div>
        </div>
        <div class="intro-card">
          <div class="intro-icon">
            <i class="el-icon-coordinate"></i>
          </div>
          <div class="intro-text">
            <h3>职位分布</h3>
            <p>多维度分析职位分布，帮助您了解市场需求和就业趋势</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据分析视图部分 -->
    <SalaryHotJob />
    <JobScatterPlot />
  </div>
</template>

<script setup lang="ts">
import SalaryHotJob from "@/views/boss/user/SalaryAnalysisView.vue"
import JobScatterPlot from "@/views/boss/user/JobScatterPlot.vue"
</script>

<style scoped>
/* 暗色主题变量 */
:root {
  --dark-bg-color: #121212;
  --dark-card-bg: #1a1a1a;
  --dark-text-primary: #e0e0e0;
  --dark-text-secondary: #9e9e9e;
  --dark-accent: #64b5f6;
  --dark-border: #333333;
}

.user-view-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100%;
  padding-bottom: 30px;
  background-color: var(--dark-bg-color);
  color: var(--dark-text-primary);
}

/* 介绍部分样式 */
.intro-section {
  width: 100%;
  max-width: 1200px;
  padding: 30px 20px;
  margin-bottom: 20px;
}

.intro-header {
  text-align: center;
  margin-bottom: 30px;
}

.intro-header h1 {
  font-size: 2.2rem;
  color: var(--dark-accent);
  margin-bottom: 10px;
}

.intro-subtitle {
  font-size: 1.2rem;
  color: var(--dark-text-secondary);
}

.intro-content {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 20px;
}

.intro-card {
  background-color: var(--dark-card-bg);
  border-radius: 8px;
  padding: 20px;
  width: 45%;
  min-width: 280px;
  display: flex;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  border: 1px solid var(--dark-border);
}

.intro-icon {
  font-size: 2.5rem;
  color: var(--dark-accent);
  margin-right: 15px;
}

.intro-text h3 {
  margin-top: 0;
  margin-bottom: 8px;
  color: var(--dark-text-primary);
}

.intro-text p {
  margin: 0;
  color: var(--dark-text-secondary);
  line-height: 1.5;
}

/* 媒体查询，适应小屏幕 */
@media (max-width: 768px) {
  .intro-card {
    width: 100%;
  }
}
</style>