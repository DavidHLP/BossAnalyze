<template>
  <div class="section-header">
    <div class="icon-circle">
      <el-icon><component :is="iconName" /></el-icon>
    </div>
    <h2>{{ title }}</h2>
  </div>
</template>

<script lang="ts" setup>
import { defineProps } from 'vue'

defineProps<{
  title: string;
  iconClass?: string;
  iconName?: string;
}>()
</script>

<style scoped>
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
</style>
