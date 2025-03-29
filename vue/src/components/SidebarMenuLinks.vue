<template>
  <template v-for="link in links" :key="link.path">
    <!-- 只有当meta.type不是'F'时才显示节点 -->
    <template v-if="link.meta?.type !== 'F'">
      <el-sub-menu v-if="link.meta?.type === 'M'" :index="link.path">
        <template #title>
          <el-icon><component :is="link.meta?.icon || (link as any).icon" /></el-icon>
          <span>{{ link.name }}</span>
        </template>
        <SidebarMenuLinks :links="link.children || []" />
      </el-sub-menu>
      <el-sub-menu v-if="link.meta?.type === 'C' && (link.children?.length || 0) > 0 && link.children?.some(child => child.meta?.type !== 'F')" :index="link.path">
        <template #title>
          <el-icon><component :is="link.meta?.icon || (link as any).icon" /></el-icon>
          <span>{{ link.name }}</span>
        </template>
        <SidebarMenuLinks :links="link.children || []" />
      </el-sub-menu>
      <el-menu-item v-else-if="link.meta?.type === 'C'" :index="link.path">
        <el-icon><component :is="link.meta?.icon || (link as any).icon" /></el-icon>
        <template #title>
          <span>{{ link.name }}</span>
        </template>
      </el-menu-item>
    </template>
  </template>
</template>

<script setup lang="ts">
import type { RouteRecordRaw } from 'vue-router';
import type { Router } from '@/router/index.d';

// 定义接受的props类型为RouteRecordRaw数组或后端返回的Router数组
const props = defineProps<{
  links: (RouteRecordRaw | Router)[]
}>();
</script>

<style lang="scss">
// 导航菜单项样式
.el-menu-item {
  &.is-active {
    color: var(--vt-c-green);
    background-color: rgba(66, 184, 131, 0.1);
  }

  &:hover {
    background-color: var(--el-menu-hover-bg-color);
  }
}
</style>
