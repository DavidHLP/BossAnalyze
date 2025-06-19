<template>
  <div class="layout-container" :class="{
    'layout-mobile': isMobile,
    'sidebar-open': isMobile && showSidebar,
    'sidebar-collapsed': !isMobile && layoutStore.isCollapse
  }">
    <!-- 移动端遮罩层 -->
    <div
      v-if="isMobile && showSidebar"
      class="mobile-overlay"
      @click="showSidebar = false"
    ></div>

    <!-- 侧边栏区域 -->
    <aside
      class="sidebar"
      :class="{
        'sidebar-collapsed': !isMobile && layoutStore.isCollapse,
        'sidebar-mobile': isMobile
      }"
    >
      <div class="logo-container">
        <img src="@/assets/logo.png" alt="Logo" class="logo" />
        <transition name="logo-text">
          <span v-show="!layoutStore.isCollapse || isMobile" class="logo-text">管理系统</span>
        </transition>
      </div>

      <div class="menu-container">
      <el-menu
        :default-active="$route.path"
          :collapse="!isMobile && layoutStore.isCollapse"
        router
        class="sidebar-menu"
          background-color="transparent"
          text-color="var(--menu-text-color)"
          active-text-color="var(--menu-active-text-color)"
        >
        <SidebarMenuLinks :links="filteredLinks" />
      </el-menu>
      </div>
    </aside>

    <!-- 主内容区域 -->
    <div class="layout-main">
      <!-- 顶部导航栏 -->
      <header class="header-container">
        <div class="header-content">
        <div class="header-left">
            <!-- 移动端菜单按钮 / 桌面端折叠按钮 -->
            <el-button
              v-if="isMobile"
              :icon="'Menu'"
              circle
              class="mobile-menu-btn"
              @click="showSidebar = !showSidebar"
            />
            <el-button
              v-else
              :icon="layoutStore.isCollapse ? 'Expand' : 'Fold'"
              circle
              class="collapse-btn"
              @click="layoutStore.toggleCollapse"
            />

            <!-- 面包屑导航 -->
            <nav class="breadcrumb-nav" v-show="!isMobile || !showBreadcrumbCollapsed">
              <el-breadcrumb separator="/" class="breadcrumb">
                <el-breadcrumb-item :to="{ path: '/console' }">
                  <el-icon><House /></el-icon>
                  <span v-show="!isMobile">控制台</span>
                </el-breadcrumb-item>
            <el-breadcrumb-item v-for="(item, index) in breadcrumbList" :key="index">
              {{ item.meta?.title || item.name }}
            </el-breadcrumb-item>
          </el-breadcrumb>
            </nav>
        </div>

        <div class="header-right">
            <!-- 搜索框 -->
            <div class="header-search" v-show="!isMobile">
            <el-input
                placeholder="搜索功能或页面..."
              prefix-icon="Search"
              clearable
                size="default"
                class="search-input"
            />
          </div>

            <!-- 操作按钮组 -->
            <div class="action-buttons">
              <el-button link class="icon-button" title="搜索" v-show="isMobile">
                <el-icon><Search /></el-icon>
              </el-button>
              <el-button link class="icon-button" title="系统设置">
            <el-icon><Setting /></el-icon>
          </el-button>
              <el-button link class="icon-button notification-btn" title="消息通知">
            <el-badge :value="1" class="notification-badge">
              <el-icon><Bell /></el-icon>
            </el-badge>
          </el-button>

              <!-- 用户下拉菜单 -->
              <el-dropdown class="user-dropdown" placement="bottom-end">
                <div class="user-info">
                  <el-avatar size="small" icon="UserFilled" class="user-avatar" />
                  <span class="username" v-show="!isMobile">管理员</span>
                  <el-icon class="dropdown-arrow" v-show="!isMobile"><ArrowDown /></el-icon>
                </div>
            <template #dropdown>
              <el-dropdown-menu>
                    <el-dropdown-item icon="User">个人信息</el-dropdown-item>
                    <el-dropdown-item icon="Lock">修改密码</el-dropdown-item>
                    <el-dropdown-item icon="Setting">系统设置</el-dropdown-item>
                    <el-dropdown-item divided icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
        </div>
      </header>

      <!-- 页面内容区域 -->
      <main class="content-wrapper">
      <div class="content-container">
        <router-view v-slot="{ Component }">
            <transition name="page-transition" mode="out-in">
              <component :is="Component" :key="$route.fullPath" />
          </transition>
        </router-view>
      </div>
    </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import SidebarMenuLinks from '@/views/console/system/components/SidebarMenuLinks.vue'
import type { RouteRecordRaw } from 'vue-router'
import { useRouterStore } from '@/store/router/routerStore'
import type { Permissions } from '@/api/auth/auth.d'
import { useUserStore } from '@/store/user/userStore'
import { filterRoutes } from '@/router/index'
import { useLayoutStore } from '@/store/layout/layoutStore'

const route = useRoute()
const permissions: Permissions[] = useUserStore().permissions
const layoutStore = useLayoutStore()
const filteredLinks = ref<RouteRecordRaw[]>([])

// 响应式状态
const isMobile = ref(false)
const showSidebar = ref(false)
const showBreadcrumbCollapsed = ref(false)

// 检测移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  if (!isMobile.value) {
    showSidebar.value = false
  }
}

// 处理窗口大小变化
const handleResize = () => {
  checkMobile()
}

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

  // 初始化响应式检测
  checkMobile()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
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
</script>

<style scoped lang="scss">
// ==============================================
// 现代化响应式布局系统 - 使用统一设计系统
// ==============================================

// 导入设计系统
@use '@/assets/style/variables.scss' as *;

.layout-container {
  display: grid;
  grid-template-areas: "sidebar main";
  grid-template-columns: var(--layout-sidebar-width) 1fr;
  height: 100vh;
  width: 100%;
  overflow: hidden;
  background-color: var(--background-primary);
  transition: grid-template-columns var(--transition-normal) var(--transition-timing);

  // 收缩状态
  &.sidebar-collapsed {
    grid-template-columns: var(--layout-sidebar-collapsed) 1fr;
  }

  // 移动端布局
  &.layout-mobile {
    grid-template-areas: "main";
    grid-template-columns: 1fr;

    &.sidebar-open {
      overflow: hidden;
    }
  }
}

// ==============================================
// 移动端遮罩层
// ==============================================
.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--background-overlay);
  z-index: var(--z-modal-backdrop);
  backdrop-filter: blur(4px);
  animation: fadeIn var(--transition-fast) var(--transition-timing);
}

// ==============================================
// 侧边栏样式 - 完全响应式
// ==============================================
.sidebar {
  grid-area: sidebar;
  background-color: var(--sidebar-bg);
  border-right: 1px solid var(--sidebar-border);
  box-shadow: var(--sidebar-shadow);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all var(--transition-normal) var(--transition-timing);

  // 移动端侧边栏
  &.sidebar-mobile {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    width: var(--layout-sidebar-width);
    z-index: var(--z-modal);
    transform: translateX(-100%);
    transition: transform var(--transition-normal) var(--transition-timing);

    .layout-container.sidebar-open & {
      transform: translateX(0);
    }
  }

  // 桌面端折叠状态
  &.sidebar-collapsed {
    .logo-text {
      opacity: 0;
      transform: scale(0.8);
    }

    .menu-container {
      .sidebar-menu {
        :deep(.el-menu-item),
        :deep(.el-sub-menu__title) {
          justify-content: center;
          padding: 0 var(--spacing-md);

          .el-icon {
            margin-right: 0;
          }

          span {
            display: none;
          }
        }

        :deep(.el-sub-menu .el-menu-item) {
          padding-left: var(--spacing-md) !important;
        }
      }
    }
  }

  // Logo容器
  .logo-container {
    height: var(--layout-header-height);
    @include flex-center;
    padding: 0 var(--spacing-lg);
    border-bottom: 1px solid var(--border-light);
    background-color: var(--background-card);
    flex-shrink: 0;

    .logo {
      width: 36px;
      height: 36px;
      border-radius: var(--radius-lg);
      transition: transform var(--transition-normal) var(--transition-timing);
      object-fit: contain;

      &:hover {
        transform: scale(1.05);
      }
    }

    .logo-text {
      margin-left: var(--spacing-md);
      font-size: var(--font-size-xl);
      font-weight: var(--font-weight-bold);
      color: var(--text-primary);
      white-space: nowrap;
      transition: all var(--transition-normal) var(--transition-timing);
    }
  }

  // 菜单容器
  .menu-container {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;

  .sidebar-menu {
      flex: 1;
    border-right: none;
      background-color: transparent;
    overflow-y: auto;
    overflow-x: hidden;
      padding: var(--spacing-sm) 0;

      // 自定义滚动条
      &::-webkit-scrollbar {
        width: 4px;
      }

      &::-webkit-scrollbar-track {
        background: transparent;
      }

      &::-webkit-scrollbar-thumb {
        background: var(--border-medium);
        border-radius: var(--radius-sm);

      &:hover {
          background: var(--border-dark);
        }
      }
    }
  }
}

// Logo文字过渡动画
.logo-text-enter-active,
.logo-text-leave-active {
  transition: all var(--transition-normal) var(--transition-timing);
}

.logo-text-enter-from,
.logo-text-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

// ==============================================
// 主内容区域样式 - Grid布局
// ==============================================
.layout-main {
  grid-area: main;
  display: grid;
  grid-template-rows: auto 1fr;
  height: 100vh;
  overflow: hidden;
  background-color: var(--background-primary);
}

// ==============================================
// 头部样式 - 完全响应式
// ==============================================
.header-container {
  background-color: var(--navbar-bg);
  border-bottom: 1px solid var(--border-light);
  box-shadow: var(--navbar-shadow);
  backdrop-filter: blur(8px);

  .header-content {
    height: var(--layout-header-height);
    padding: 0 var(--spacing-lg);
    @include flex-between;
    max-width: 100%;

    .header-left {
      @include flex-start;
      gap: var(--spacing-lg);
      flex: 1;
      min-width: 0;

      .mobile-menu-btn,
      .collapse-btn {
        width: 40px;
        height: 40px;
        padding: 0;
        background-color: transparent;
        border: 1px solid var(--border-light);
        color: var(--text-secondary);
        transition: all var(--transition-normal) var(--transition-timing);
        flex-shrink: 0;

        &:hover {
          color: var(--primary-color);
          background-color: var(--primary-light);
          border-color: var(--primary-color);
          @include hover-lift;
        }

        .el-icon {
          font-size: var(--font-size-lg);
        }
      }

      .breadcrumb-nav {
        flex: 1;
        min-width: 0;

      .breadcrumb {
          font-size: var(--font-size-md);

        :deep(.el-breadcrumb__item) {
            @include flex-center;

            .el-breadcrumb__inner {
              color: var(--breadcrumb-text-color);
              font-weight: var(--font-weight-normal);
              transition: color var(--transition-normal) var(--transition-timing);
              @include text-ellipsis;

              &:hover {
                color: var(--primary-color);
              }

              &.is-link {
                color: var(--primary-color);

                &:hover {
                  color: var(--primary-dark);
                }
              }
            }

            &:last-child .el-breadcrumb__inner {
              color: var(--text-primary);
              font-weight: var(--font-weight-medium);
            }

            .el-icon {
              margin-right: var(--spacing-xs);
            }
          }
        }
      }
    }

    .header-right {
      @include flex-start;
      gap: var(--spacing-md);
      flex-shrink: 0;

      .header-search {
        width: 280px;
        transition: width var(--transition-normal) var(--transition-timing);

        .search-input {
        :deep(.el-input__wrapper) {
            border-radius: var(--radius-lg);
            border: 1px solid var(--border-light);
            transition: all var(--transition-normal) var(--transition-timing);
            background-color: var(--background-secondary);

            &:hover {
              border-color: var(--border-medium);
              background-color: var(--background-card);
            }

            &.is-focus {
              border-color: var(--primary-color);
              box-shadow: 0 0 0 3px var(--primary-light);
              background-color: var(--background-card);
            }
        }

        :deep(.el-input__inner) {
            height: 38px;
            font-size: var(--font-size-sm);
          }
        }
      }

      .action-buttons {
        @include flex-start;
        gap: var(--spacing-sm);

      .icon-button {
          width: 40px;
          height: 40px;
        padding: 0;
          color: var(--text-secondary);
          border: 1px solid transparent;
          border-radius: var(--radius-lg);
          transition: all var(--transition-normal) var(--transition-timing);

        &:hover {
          color: var(--primary-color);
            background-color: var(--primary-light);
            border-color: var(--primary-color);
            @include hover-lift;
          }

          .el-icon {
            font-size: var(--font-size-lg);
          }

          &.notification-btn {
            position: relative;

            .notification-badge {
              :deep(.el-badge__content) {
                top: 3px;
                right: 3px;
                background-color: var(--error-color);
                border: 2px solid var(--background-card);
                font-size: var(--font-size-xs);
                min-width: 16px;
                height: 16px;
                line-height: 12px;
              }
            }
          }
      }

      .user-dropdown {
          .user-info {
            @include flex-start;
            gap: var(--spacing-sm);
            padding: var(--spacing-xs) var(--spacing-md);
            border-radius: var(--radius-lg);
        cursor: pointer;
            transition: all var(--transition-normal) var(--transition-timing);
            border: 1px solid transparent;
            min-width: 0;

        &:hover {
              background-color: var(--background-secondary);
              border-color: var(--border-light);
            }

            .user-avatar {
              border: 2px solid var(--border-light);
              transition: border-color var(--transition-normal) var(--transition-timing);
              flex-shrink: 0;
        }

        .username {
              font-size: var(--font-size-md);
              font-weight: var(--font-weight-medium);
              color: var(--text-primary);
              @include text-ellipsis;
              max-width: 120px;
            }

            .dropdown-arrow {
              font-size: var(--font-size-sm);
              color: var(--text-secondary);
              transition: transform var(--transition-normal) var(--transition-timing);
              flex-shrink: 0;
            }

            &:hover {
              .user-avatar {
                border-color: var(--primary-color);
              }

              .dropdown-arrow {
                transform: rotate(180deg);
              }
            }
          }
        }
      }
    }
  }
}

// ==============================================
// 内容区域样式 - 完全自适应
// ==============================================
.content-wrapper {
  overflow: hidden;
  position: relative;

.content-container {
  height: 100%;
    padding: var(--layout-content-padding);
    overflow-y: auto;
    overflow-x: hidden;
    box-sizing: border-box;
    position: relative;

    // 现代化滚动条
    &::-webkit-scrollbar {
      width: 8px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--border-medium);
      border-radius: var(--radius-lg);
      border: 2px solid transparent;
      background-clip: content-box;

      &:hover {
        background: var(--border-dark);
        background-clip: content-box;
      }
    }

    // 通用内容自适应
    :deep(> *) {
      max-width: 100%;
      box-sizing: border-box;
    }

    // 响应式表格
    :deep(.el-table) {
      width: 100%;
      min-width: 0;
      border-radius: var(--radius-lg);
      overflow: hidden;
    }

    // 响应式卡片
    :deep(.el-card) {
      width: 100%;
      max-width: 100%;
      box-sizing: border-box;
      border-radius: var(--radius-lg);

      .el-card__body {
        padding: var(--spacing-lg);
      }
    }

    // 响应式表单
    :deep(.el-form) {
      max-width: 100%;

      .el-form-item {
        margin-bottom: var(--spacing-lg);
      }
    }

    // 响应式图表
    :deep([class*="chart"]) {
      width: 100% !important;
      max-width: 100%;
      min-width: 0;
      border-radius: var(--radius-lg);
      overflow: hidden;
    }

    // 响应式网格布局
    :deep(.grid-container) {
      display: grid;
      gap: var(--spacing-lg);
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    }

    // 响应式弹性布局
    :deep(.flex-container) {
      display: flex;
      flex-wrap: wrap;
      gap: var(--spacing-lg);

      > * {
        flex: 1 1 300px;
        min-width: 0;
      }
    }
  }
}

// ==============================================
// 页面过渡动画 - 现代化设计
// ==============================================
.page-transition-enter-active,
.page-transition-leave-active {
  transition: all var(--transition-normal) var(--transition-timing);
}

.page-transition-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.98);
}

.page-transition-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(1.02);
}

// 淡入动画
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.mobile-overlay {
  animation: fadeIn var(--transition-fast) var(--transition-timing);
}

// ==============================================
// 响应式断点系统 - 移动优先设计
// ==============================================

// 超大屏幕 (≥1400px)
@media (min-width: 1400px) {
  .layout-container {
    .header-content {
      .header-right {
        .header-search {
          width: 320px;
        }
      }
    }
  }

  .content-container {
    :deep(.grid-container) {
      grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
    }
  }
}

// 大屏幕 (≤1200px)
@include responsive(xl) {
  .header-content {
    .header-right {
      .header-search {
        width: 260px;
      }
    }
  }
}

// 中大屏幕 (≤992px)
@include responsive(lg) {
  .layout-container {
    &:not(.layout-mobile) {
      grid-template-columns: var(--layout-sidebar-collapsed) 1fr;

      .sidebar:not(.sidebar-collapsed) {
        width: var(--layout-sidebar-collapsed);

        .logo-text {
          display: none;
        }
      }
    }
  }

  .header-content {
    padding: 0 var(--spacing-md);

    .header-right {
      .header-search {
        width: 220px;
      }

      .action-buttons {
        .user-dropdown .user-info .username {
          display: none;
        }
      }
    }
  }

  .content-container {
    padding: var(--spacing-lg) var(--spacing-md);

    :deep(.grid-container) {
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    }

    :deep(.el-table) {
      font-size: var(--font-size-sm);

      .el-table__cell {
        padding: var(--spacing-xs) var(--spacing-sm);
      }
    }
  }
}

// 中等屏幕 (≤768px) - 平板模式
@include responsive(md) {
  .layout-container {
    &.layout-mobile {
      .header-content {
        .header-left {
          gap: var(--spacing-md);
        }

        .header-right {
          .header-search {
            display: none;
          }

          .action-buttons {
            gap: var(--spacing-xs);
          }
        }
      }
    }
  }

  .content-container {
    padding: var(--spacing-md);

    // 网格布局变为单列
    :deep(.grid-container) {
      grid-template-columns: 1fr;
      gap: var(--spacing-md);
    }

    :deep(.flex-container) {
      > * {
        flex: 1 1 100%;
      }
    }

    // 卡片和图表适配
    :deep(.metric-cards),
    :deep(.charts-row) {
      flex-direction: column;
      gap: var(--spacing-md);
    }

    :deep(.chart-card) {
      min-width: 100% !important;

      .chart-container {
        height: 280px !important;
      }
    }

    // 表格优化
    :deep(.el-table) {
      .el-table__cell {
        padding: var(--spacing-xs);
        font-size: var(--font-size-sm);
      }
    }

    // 表单布局调整
    :deep(.el-form) {
      .el-form-item__label {
        display: block;
        float: none;
        text-align: left;
        width: 100% !important;
        margin-bottom: var(--spacing-xs);
      }

      .el-form-item__content {
        margin-left: 0 !important;
        width: 100%;
      }
    }
  }
}

// 小屏幕 (≤576px) - 手机模式
@include responsive(sm) {
  .layout-container.layout-mobile {
    .header-content {
      padding: 0 var(--spacing-sm);

      .header-left {
        gap: var(--spacing-sm);

        .breadcrumb-nav {
          .breadcrumb {
            font-size: var(--font-size-sm);

            :deep(.el-breadcrumb__item:not(:last-child)) {
              display: none; // 只显示最后一个面包屑
            }
          }
        }
      }

      .header-right {
        .action-buttons {
          gap: var(--spacing-xs);

          .user-dropdown .user-info {
            padding: var(--spacing-xs);

            .username,
            .dropdown-arrow {
              display: none;
            }
          }
        }
      }
    }
  }

  .content-container {
    padding: var(--spacing-sm);

    // 移动端优化
    :deep(.metric-card) {
      min-width: 100% !important;
      height: auto;
      padding: var(--spacing-md);
    }

    :deep(.chart-container) {
      height: 220px !important;
    }

    // 表格移动端优化
    :deep(.el-table) {
      font-size: var(--font-size-xs);

      .el-table__cell {
        padding: var(--spacing-xs);
      }

      // 隐藏次要列
      .el-table__column--secondary {
        display: none;
      }
    }

    // 按钮组优化
    :deep(.el-button-group) {
      flex-direction: column;
      width: 100%;

      .el-button {
        width: 100%;
        margin: 2px 0;
      }
    }

    // 分页移动端优化
    :deep(.el-pagination) {
      justify-content: center;
      flex-wrap: wrap;

      .el-pagination__sizes,
      .el-pagination__jump {
        order: 1;
        width: 100%;
        justify-content: center;
        margin-top: var(--spacing-sm);
      }
    }
  }

  // 页面过渡动画简化
  .page-transition-enter-from {
    transform: translateY(10px) scale(0.99);
  }

  .page-transition-leave-to {
    transform: translateY(-5px) scale(1.01);
  }

  .page-transition-enter-active,
  .page-transition-leave-active {
    transition: all var(--transition-fast) var(--transition-timing);
  }
}

// ==============================================
// 增强组件样式
// ==============================================
// 侧边栏菜单项增强
.sidebar .menu-container .sidebar-menu {
  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    margin: var(--spacing-xs) var(--spacing-sm);
    border-radius: var(--radius-md);
    transition: all var(--transition-normal) var(--transition-timing);
    position: relative;

    &:hover {
      background-color: var(--menu-hover-bg);
      transform: translateX(2px);
    }

    &.is-active {
      background-color: var(--menu-active-bg);
      color: var(--menu-active-text-color);

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 3px;
        height: 24px;
        background-color: var(--primary-color);
        border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
      }
    }
  }

  :deep(.el-sub-menu) {
    .el-menu-item {
      padding-left: 48px !important;
      margin-left: var(--spacing-sm);
      margin-right: var(--spacing-lg);

      &.is-active {
        padding-left: 45px !important;
        border-left: 3px solid var(--primary-color);
        margin-left: var(--spacing-sm);
      }
    }
  }
}

// 用户下拉菜单增强
.user-dropdown {
  :deep(.el-dropdown-menu) {
    @include card-shadow;
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-light);
    padding: var(--spacing-sm);
    backdrop-filter: blur(8px);

    .el-dropdown-menu__item {
      border-radius: var(--radius-md);
      margin: 2px 0;
      padding: var(--spacing-sm) var(--spacing-md);
      transition: all var(--transition-normal) var(--transition-timing);

      &:hover {
        background-color: var(--primary-light);
        color: var(--primary-color);
        transform: translateX(2px);
      }

      &.is-divided {
        border-top: 1px solid var(--border-light);
        margin-top: var(--spacing-sm);
        padding-top: var(--spacing-md);
      }
    }
  }
}

// 面包屑导航增强
.breadcrumb-nav .breadcrumb {
  :deep(.el-breadcrumb__separator) {
    color: var(--text-disabled);
    margin: 0 var(--spacing-sm);
    font-weight: var(--font-weight-bold);
  }

  :deep(.el-breadcrumb__item) {
    &:last-child .el-breadcrumb__inner {
      background: linear-gradient(45deg, var(--primary-color), var(--primary-light));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }
}

// ==============================================
// 现代化增强功能
// ==============================================

// 容器查询支持 (现代浏览器)
@supports (container-type: inline-size) {
  .content-container {
    container-type: inline-size;
    container-name: content;
  }

  @container content (max-width: 600px) {
    :deep(.grid-container) {
      grid-template-columns: 1fr;
    }

    :deep(.flex-container > *) {
      flex: 1 1 100%;
    }

    :deep(.el-table) {
      font-size: var(--font-size-sm);
    }
  }

  @container content (max-width: 400px) {
    :deep(.chart-container) {
      height: 200px !important;
    }

    :deep(.metric-card) {
      padding: var(--spacing-md);
    }
  }
}

// ==============================================
// 深色模式与主题适配
// ==============================================
@media (prefers-color-scheme: dark) {
  .layout-container {
    .sidebar {
      border-right-color: var(--border-dark);
    }

    .header-container {
      border-bottom-color: var(--border-dark);
    }

    .logo-container {
      border-bottom-color: var(--border-dark);
    }
  }
}

// 减少动画（用户偏好设置）
@media (prefers-reduced-motion: reduce) {
  .page-transition-enter-active,
  .page-transition-leave-active,
  .logo-text-enter-active,
  .logo-text-leave-active {
    transition: none;
  }

  .mobile-overlay {
    animation: none;
  }

  .sidebar .menu-container .sidebar-menu {
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      &:hover {
        transform: none;
      }
    }
  }
}

// 高对比度模式支持
@media (prefers-contrast: high) {
  .layout-container {
    .sidebar,
    .header-container {
      border-width: 2px;
    }

    .sidebar .menu-container .sidebar-menu {
      :deep(.el-menu-item.is-active::before) {
        width: 4px;
      }
    }
  }
}
</style>
