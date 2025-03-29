<template>
  <div class="console-container">
    <!-- 侧边栏区域 -->
    <aside :class="['sidebar', { 'is-collapse': isCollapse }]">
      <el-menu :default-active="$route.path" :collapse="isCollapse" router class="sidebar-menu">
        <SidebarMenuLinks :links="filteredLinks" />
      </el-menu>
    </aside>

    <!-- 主内容区域 -->
    <main :class="['main-content', { 'content-collapse': isCollapse }]">
      <!-- 顶部操作栏 -->
      <div class="top-bar">
        <div class="left-actions">
          <el-button
            :icon="isCollapse ? 'Expand' : 'Fold'"
            circle
            @click="toggleCollapse"
            class="collapse-btn"
          />
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/console' }">控制台</el-breadcrumb-item>
            <el-breadcrumb-item v-for="(item, index) in breadcrumbList" :key="index">
              {{ item.meta?.title || item.name }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
      </div>

      <!-- 页面内容 -->
      <div class="content-wrapper">
        <router-view v-slot="{ Component }">
          <transition name="fade-scale" mode="out-in">
            <div class="transition-wrapper" v-if="true">
              <component :is="Component" />
            </div>
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import SidebarMenuLinks from '@/components/SidebarMenuLinks.vue'
import type { RouteRecordRaw } from 'vue-router'
import { useRouterStore } from '@/stores/router/routerStore'
import type { Permissions } from '@/api/auth/auth.d'
import { useUserStore } from '@/stores/user/userStore'
import { filterRoutes } from '@/router/index'

const route = useRoute()
const permissions: Permissions[] = useUserStore().permissions
const isCollapse = ref(false)

const filteredLinks = ref<RouteRecordRaw[]>([])

// 递归过滤隐藏的路由
const filterHiddenRoutes = (routes: RouteRecordRaw[]): RouteRecordRaw[] => {
  let result: RouteRecordRaw[] = [];
  routes.forEach(route => {
    // 处理子路由
    let filteredChildren: RouteRecordRaw[] = [];
    if (route.children && route.children.length > 0) {
      filteredChildren = filterHiddenRoutes(route.children);
    }
    if (!route.meta?.hidden && !route.meta?.metaHidden) {
      const routeCopy = { ...route };
      if (filteredChildren.length > 0) {
        routeCopy.children = filteredChildren;
      }
      result.push(routeCopy);
    }
    // 如果当前路由隐藏但有子路由，将子路由提升到当前层级
    else if (filteredChildren.length > 0) {
      result = result.concat(filteredChildren);
    }
  });
  return result;
}

onMounted(async () => {
  const routerStore = useRouterStore()
  // 获取路由并过滤隐藏的路由
  const routes = await routerStore.fetchRoutes()
  const result = filterRoutes(routes , permissions)
  const filteredRoutes = filterHiddenRoutes([...result])
  filteredLinks.value = filteredRoutes
})

const breadcrumbList = computed(() => {
  return route.matched
    .filter((item) => item.meta?.breadcrumb !== false)
    .map((item) => ({
      path: item.path,
      name: item.name,
      meta: item.meta,
    }))
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleRefresh = () => {
  window.location.reload()
}
</script>

<style scoped lang="scss">
@import '@/styles/variables.scss';

.console-container {
  display: flex;
  height: 95vh;
  width: 95vw;
  overflow: hidden;
  margin: auto;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: var(--vt-c-bg);
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);

  .sidebar {
    width: $sidebar-width;
    height: 100%;
    position: fixed;
    left: 0;
    top: 0;
    background: var(--vt-c-indigo);
    transition: width 0.3s;
    box-shadow: 2px 0 6px rgba(0, 0, 0, 0.1);
    z-index: 1000;

    &.is-collapse {
      width: $sidebar-collapse-width;
    }

    .logo-container {
      height: $header-height;
      display: flex;
      align-items: center;
      justify-content: center;
      border-bottom: 1px solid rgba(255, 255, 255, 0.05);

      .logo {
        width: 32px;
        height: 32px;
        transition: all 0.3s;
      }
    }

    .sidebar-menu {
      --el-menu-active-color: var(--vue-green);
      --el-menu-hover-bg-color: rgba(255, 255, 255, 0.05);
      border-right: none;
      height: calc(100% - $header-height);
      background-color: transparent;

      .menu-item {
        transition: all 0.3s;

        &:hover {
          background-color: var(--el-menu-hover-bg-color) !important;
        }
      }
    }
  }

  .main-content {
    flex: 1;
    overflow: hidden visible;
    transition: margin-left 0.3s;
    margin-left: $sidebar-width;
    min-width: calc(100% - $sidebar-width);

    &.content-collapse {
      margin-left: $sidebar-collapse-width;
      min-width: calc(100% - $sidebar-collapse-width);
    }

    .top-bar {
      height: $header-height;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 20px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
      background-color: var(--vt-c-bg);
      border-bottom: 1px solid var(--vt-c-divider-light);

      .left-actions {
        display: flex;
        align-items: center;
        gap: 15px;

        .collapse-btn {
          font-size: 18px;
          color: var(--el-text-color-primary);
        }

        .breadcrumb {
          font-size: 14px;
        }
      }

      .right-actions {
        display: flex;
        gap: 10px;

        .refresh-btn {
          color: var(--vt-c-green) !important;
        }
      }
    }

    .content-wrapper {
      position: relative;
      overflow: hidden;
      height: auto;
      min-height: calc(100% - $header-height);
      padding: 20px;
      background: var(--vt-c-bg);
      box-sizing: border-box;

      .transition-wrapper {
        position: relative;
        width: 100%;
        min-height: 100%;
      }
    }
  }
}

// 优化后的过渡动画
.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: all 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.fade-scale-enter-from {
  opacity: 0;
  transform: scale(0.98);
}

.fade-scale-leave-to {
  opacity: 0;
  transform: scale(1.02);
}

@media (max-width: 768px) {
  .sidebar {
    width: $sidebar-width !important;

    &.is-collapse {
      left: -$sidebar-width;
    }
  }

  .main-content {
    margin-left: 0 !important;
    min-width: 100% !important;
  }
}

@media (prefers-color-scheme: dark) {
  .console-container {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);

    .sidebar {
      background-color: rgba(25, 25, 25, 0.95);
      border-right: 1px solid rgba(66, 184, 131, 0.2);

      .sidebar-menu {
        --el-menu-text-color: #{$vue-dark-text-secondary};

        .el-menu-item.is-active {
          color: var(--vt-c-green);
          background-color: rgba(66, 184, 131, 0.1);
          border-right: 3px solid var(--vt-c-green);
        }
      }
    }

    .main-content {
      .top-bar {
        background-color: $vue-dark-surface;
        border-bottom: 1px solid $vue-dark-border;

        .collapse-btn {
          color: $vue-dark-text;
        }
      }

      .content-wrapper {
        background-color: $vue-dark-bg;
      }
    }
  }
}
</style>
