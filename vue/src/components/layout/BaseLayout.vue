<template>
  <div class="base-layout">
    <el-container>
      <!-- 头部区域 -->
      <el-header class="layout-header">
        <div class="header-content">
          <div class="header-left">
            <h2 class="page-title">{{ title }}</h2>
            <p class="page-desc">{{ description }}</p>
          </div>
          <div class="header-right">
            <slot name="header-actions"></slot>
          </div>
        </div>
      </el-header>

      <!-- 主要内容区域 -->
      <el-main class="layout-main">
        <!-- 搜索区域 -->
        <div v-if="$slots.search" class="search-section">
          <slot name="search"></slot>
        </div>

        <!-- 内容区域 -->
        <div class="content-section">
          <slot></slot>
        </div>

        <!-- 分页区域 -->
        <div v-if="$slots.pagination" class="pagination-section">
          <slot name="pagination"></slot>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description: string
}>()
</script>

<style scoped>
.base-layout {
  height: 100%;
  background-color: #f5f7fa;
}

.layout-header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 16px 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1400px;
  margin: 0 auto;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.page-desc {
  margin: 4px 0 0;
  font-size: 14px;
  color: #909399;
}

.layout-main {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.search-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 4px;
  margin-bottom: 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.content-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 4px;
  min-height: 400px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.pagination-section {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
</style>
