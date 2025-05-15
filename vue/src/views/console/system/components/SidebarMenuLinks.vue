<template>
  <template v-for="link in filteredLinks" :key="link.path">
    <!-- 目录类型显示为子菜单 -->
    <el-sub-menu v-if="link.meta?.type === 'M'" :index="link.path">
      <template #title>
        <div class="menu-item-content">
          <el-icon><component :is="link.meta?.metaIcon || 'Document'" /></el-icon>
          <span>{{ link.name }}</span>
        </div>
      </template>
      <SidebarMenuLinks v-if="hasChildren(link)" :links="link.children || []" />
    </el-sub-menu>

    <!-- 菜单类型且有可见子菜单，显示为子菜单 -->
    <el-sub-menu v-else-if="link.meta?.type === 'C' && hasChildren(link)" :index="link.path">
      <template #title>
        <div class="menu-item-content">
          <el-icon><component :is="link.meta?.metaIcon || 'Document'" /></el-icon>
          <span>{{ link.name }}</span>
        </div>
      </template>
      <SidebarMenuLinks :links="link.children || []" />
    </el-sub-menu>

    <!-- 菜单类型且无子菜单，显示为菜单项 -->
    <el-menu-item v-else-if="link.meta?.type === 'C'" :index="link.path">
      <div class="menu-item-content">
        <el-icon><component :is="link.meta?.metaIcon || 'Document'" /></el-icon>
        <span>{{ link.name }}</span>
      </div>
    </el-menu-item>
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { RouteRecordRaw } from 'vue-router';
import type { Router } from '@/router/index.d';

// 定义props类型
interface Props {
  links: (RouteRecordRaw | Router)[];
}

const props = defineProps<Props>();

// 过滤出可见的菜单链接
const filteredLinks = computed(() => {
  return props.links.filter(link => isVisible(link));
});

// 判断链接是否可见
function isVisible(link: RouteRecordRaw | Router): boolean {
  return link.meta?.type !== 'F' && !link.meta?.metaHidden;
}

// 判断是否有可见的子菜单
function hasChildren(link: RouteRecordRaw | Router): boolean {
  if (!link.children || link.children.length === 0) {
    return false;
  }

  // 检查是否有至少一个可见的子链接
  return link.children.some(child => isVisible(child));
}
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
