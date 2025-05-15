<template>
  <div class="dashboard-container">
    <!-- 侧边栏区域 -->
    <aside class="sidebar" :class="{ 'sidebar-collapsed': layoutStore.isCollapse }">
      <div class="logo-container">
        <img v-if="!layoutStore.isCollapse" src="@/assets/logo.png" alt="Logo" class="logo" />
        <img v-else src="@/assets/logo.png" alt="Logo" class="logo" />
        <span v-if="!layoutStore.isCollapse" class="logo-text">管理系统</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="layoutStore.isCollapse"
        router
        class="sidebar-menu"
        background-color="#ffffff"
        text-color="#67748e"
        active-text-color="#3b82f6">
        <SidebarMenuLinks :links="filteredLinks" />
      </el-menu>
    </aside>

    <!-- 主内容区域 -->
    <main class="main-content">
      <!-- 顶部操作栏 -->
      <div class="header">
        <div class="header-left">
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-button
              :icon="layoutStore.isCollapse ? 'Expand' : 'Fold'"
              circle
              class="collapse-btn"
              @click="layoutStore.toggleCollapse"
            />
            <el-breadcrumb-item :to="{ path: '/console' }">控制台</el-breadcrumb-item>
            <el-breadcrumb-item v-for="(item, index) in breadcrumbList" :key="index">
              {{ item.meta?.title || item.name }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <div class="header-search">
            <el-input
              placeholder="搜索..."
              prefix-icon="Search"
              clearable
            />
          </div>
          <el-button link class="icon-button">
            <el-icon><Setting /></el-icon>
          </el-button>
          <el-button link class="icon-button">
            <el-badge :value="1" class="notification-badge">
              <el-icon><Bell /></el-icon>
            </el-badge>
          </el-button>
          <el-dropdown>
            <span class="user-dropdown">
              <el-avatar size="small" icon="UserFilled" />
              <span class="username">管理员</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人信息</el-dropdown-item>
                <el-dropdown-item>修改密码</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 页面内容 -->
      <div class="content-container">
        <router-view v-slot="{ Component }">
          <transition name="fade-scale" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import SidebarMenuLinks from '@/views/console/system/components/SidebarMenuLinks.vue'
import type { RouteRecordRaw } from 'vue-router'
import { useRouterStore } from '@/stores/router/routerStore'
import type { Permissions } from '@/api/auth/auth.d'
import { useUserStore } from '@/stores/user/userStore'
import { filterRoutes } from '@/router/index'
import { useLayoutStore } from '@/stores/layout/layoutStore'

const route = useRoute()
const permissions: Permissions[] = useUserStore().permissions
const layoutStore = useLayoutStore()
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
    .filter((item) => item.meta?.breadcrumb !== false && !item.meta?.metaHidden)
    .map((item) => ({
      path: item.path,
      name: item.name,
      meta: item.meta,
    }))
})

// 不再需要本地的 toggleCollapse 方法，直接使用 store 中的方法
</script>

<style scoped lang="scss">
.dashboard-container {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden;
  background-color: var(--background-color);
}

.sidebar {
  height: 100%;
  background-color: var(--sidebar-bg);
  transition: all 0.3s;
  width: var(--sidebar-width);
  position: relative;
  z-index: 10;
  box-shadow: var(--box-shadow);

  &-collapsed {
    width: var(--sidebar-width-collapsed);
  }

  .logo-container {
    height: var(--header-height);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    border-bottom: 1px solid var(--border-color);

    .logo {
      width: 32px;
      height: 32px;
    }

    .logo-text {
      margin-left: 12px;
      font-size: 18px;
      font-weight: bold;
      white-space: nowrap;
      overflow: hidden;
      color: var(--logo-text-color);
    }
  }

  .sidebar-menu {
    border-right: none;
    height: calc(100% - var(--header-height));
    overflow-y: auto;
    overflow-x: hidden;

    :deep(.el-menu-item) {
      height: 50px;
      line-height: 50px;
      margin: 5px 10px;
      border-radius: 8px;
      padding: 0 15px !important;

      .el-icon {
        margin-right: 10px;
        width: 20px;
        text-align: center;
        font-size: 18px;
        vertical-align: middle;
      }

      span {
        vertical-align: middle;
      }

      &.is-active {
        background-color: var(--menu-active-bg);
        color: var(--menu-active-text-color);
      }

      &:hover {
        background-color: var(--menu-hover-bg);
      }
    }

    :deep(.el-sub-menu__title) {
      height: 50px;
      line-height: 50px;
      margin: 5px 10px;
      border-radius: 8px;
      padding: 0 15px !important;

      .el-icon {
        margin-right: 10px;
        width: 20px;
        text-align: center;
        font-size: 18px;
        vertical-align: middle;
      }

      span {
        vertical-align: middle;
      }

      &:hover {
        background-color: var(--menu-hover-bg);
      }
    }

    :deep(.el-sub-menu .el-menu-item) {
      padding-left: 45px !important;

      &.is-active {
        padding-left: 42px !important;
        border-left: 3px solid var(--menu-active-text-color);
      }
    }
  }
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--background-color);
}

.header {
  height: var(--header-height);
  padding: 0 20px;
  background-color: var(--header-bg);
  box-shadow: var(--box-shadow);
  display: flex;
  align-items: center;
  justify-content: space-between;

  &-left {
    display: flex;
    align-items: center;

    .collapse-btn {
      margin-right: 16px;
      background-color: transparent;
      color: var(--menu-text-color);
      border: none;
      width: 36px;
      height: 36px;
      padding: 0;
      display: flex;
      align-items: center;
      justify-content: center;

      .el-icon {
        font-size: 18px;
      }

      &:hover {
        color: var(--primary-color);
        background-color: rgba(59, 130, 246, 0.1);
      }
    }

    .breadcrumb {
      font-size: 14px;
      color: var(--breadcrumb-text-color);

      :deep(.el-breadcrumb__item) {
        display: inline-flex;
        align-items: center;
        line-height: normal;
      }
    }
  }

  &-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .header-search {
      width: 220px;

      :deep(.el-input__wrapper) {
        padding: 0 8px;
      }

      :deep(.el-input__inner) {
        height: 36px;
      }
    }

    .icon-button {
      font-size: 18px;
      color: var(--menu-text-color);
      cursor: pointer;
      width: 36px;
      height: 36px;
      padding: 0;
      display: flex;
      align-items: center;
      justify-content: center;

      &:hover {
        color: var(--primary-color);
      }
    }

    .notification-badge :deep(.el-badge__content) {
      top: 5px;
      right: 5px;
    }

    .user-dropdown {
      display: flex;
      align-items: center;
      cursor: pointer;
      padding: 5px 10px;
      border-radius: 4px;
      transition: background-color 0.3s;

      &:hover {
        background-color: rgba(0, 0, 0, 0.03);
      }

      .username {
        margin-left: 8px;
        font-size: 14px;
        color: var(--menu-text-color);
      }
    }
  }
}

.content-container {
  flex: 1;
  padding: var(--content-padding);
  overflow-y: auto;
}

.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: all 0.3s ease;
}

.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.98);
}
</style>
