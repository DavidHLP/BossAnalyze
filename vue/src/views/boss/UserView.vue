<template>
  <div class="user-view-container">
    <div class="page-title">招聘数据分析</div>

    <div class="chart-grid">
      <div class="chart-card">
        <div class="chart-title">热门城市招聘统计</div>
        <HotCity />
      </div>

      <div class="chart-card">
        <div class="chart-title">热门职位招聘统计</div>
        <HotJob />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import HotCity from './user/HotCity.vue';
import HotJob from './user/HotJob.vue';
</script>

<style scoped>
.user-view-container {
  padding: 20px;
  background-color: #121212;
  min-height: 100vh;
  color: #fff;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 24px;
  text-align: center;
  width: 100%;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  flex: 1;
  width: 100%;
}

.chart-card {
  background-color: #1e1e1e;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  padding: 16px;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  height: 450px;
}

.chart-card:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
  transform: translateY(-2px);
}

.chart-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 16px;
  text-align: center;
  color: #f0f0f0;
}

@media (min-width: 992px) {
  .chart-grid {
    grid-template-columns: 1fr 1fr;
  }

  .user-view-container {
    max-width: 100%;
    margin: 0 auto;
  }
}
</style>
