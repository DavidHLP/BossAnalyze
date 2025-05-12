<template>
  <template v-for="link in links" :key="link.path">
    <!-- 只有当meta.type不是'F'时才显示节点 -->
    <template v-if="link.meta?.type !== 'F'">
      <el-sub-menu v-if="link.meta?.type === 'M'" :index="link.path">
        <template #title>
          <div class="menu-item-content">
            <el-icon><component :is="link.meta?.metaIcon || (link as any).icon || 'Document'" /></el-icon>
            <span>{{ link.name }}</span>
          </div>
        </template>
        <SidebarMenuLinks :links="link.children || []" />
      </el-sub-menu>
      <el-sub-menu v-if="link.meta?.type === 'C' && (link.children?.length || 0) > 0 && link.children?.some(child => child.meta?.type !== 'F')" :index="link.path">
        <template #title>
          <div class="menu-item-content">
            <el-icon><component :is="link.meta?.metaIcon || (link as any).icon || 'Document'" /></el-icon>
            <span>{{ link.name }}</span>
          </div>
        </template>
        <SidebarMenuLinks :links="link.children || []" />
      </el-sub-menu>
      <el-menu-item v-else-if="link.meta?.type === 'C'" :index="link.path">
        <div class="menu-item-content">
          <el-icon><component :is="link.meta?.metaIcon || (link as any).icon || 'Document'" /></el-icon>
          <span>{{ link.name }}</span>
        </div>
      </el-menu-item>
    </template>
  </template>
</template>

<script setup lang="ts">
import type { RouteRecordRaw } from 'vue-router';
import type { Router } from '@/router/index.d';

// 定义接受的props类型为RouteRecordRaw数组或后端返回的Router数组
defineProps<{
  links: (RouteRecordRaw | Router)[]
}>();
</script>

<style lang="scss">
.menu-item-content {
  display: flex;
  align-items: center;
  width: 100%;

  .el-icon {
    margin-right: 10px;
    width: 20px;
    text-align: center;
    font-size: 18px;
  }

  span {
    font-size: 14px;
    font-weight: 500;
    line-height: 1.5;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

:deep(.el-menu-item) {
  &.is-active {
    color: var(--menu-active-text-color);

    .menu-item-content {
      .el-icon {
        color: var(--menu-active-text-color);
      }
    }
  }
}
</style>
