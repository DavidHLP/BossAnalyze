<template>
  <div class="base-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ 'collapsed': isCollapsed }">
      <div class="sidebar-header">
        <img src="@/assets/logo.png" alt="Logo" class="logo" />
        <h1 class="title" v-show="!isCollapsed">BossAnalyze</h1>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        :collapse="isCollapsed"
        :unique-opened="true"
        :collapse-transition="false"
      >
        <slot name="menu"></slot>
      </el-menu>
    </aside>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 顶部导航栏 -->
      <header class="header">
        <div class="header-left">
          <el-button
            type="text"
            class="collapse-btn"
            @click="toggleCollapse"
          >
            <i :class="isCollapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
          </el-button>
          <el-breadcrumb separator="/">
            <slot name="breadcrumb"></slot>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <slot name="header-right"></slot>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="content">
        <slot></slot>
      </main>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: 'BaseLayout',
  data() {
    return {
      isCollapsed: false
    }
  },
  computed: {
    activeMenu() {
      return this.$route.path
    }
  },
  methods: {
    toggleCollapse() {
      this.isCollapsed = !this.isCollapsed
    }
  }
}
</script>

<style lang="scss" scoped>
.base-layout {
  display: flex;
  min-height: 100vh;
  background-color: var(--background-color);
}

.sidebar {
  width: var(--sidebar-width);
  background-color: var(--card-color);
  box-shadow: var(--box-shadow);
  transition: width var(--transition-duration) var(--transition-timing);
  display: flex;
  flex-direction: column;

  &.collapsed {
    width: var(--sidebar-width-collapsed);
  }
}

.sidebar-header {
  height: var(--header-height);
  padding: 0 var(--spacing-md);
  @include flex-start;

  .logo {
    width: 32px;
    height: 32px;
  }

  .title {
    margin-left: var(--spacing-md);
    font-size: var(--font-size-lg);
    color: var(--text-primary);
    white-space: nowrap;
  }
}

.sidebar-menu {
  flex: 1;
  border-right: none;

  :deep(.el-menu-item) {
    height: var(--menu-item-height);
    line-height: var(--menu-item-height);

    &.is-active {
      background-color: var(--menu-active-bg);
      color: var(--menu-active-text-color);
    }

    &:hover {
      background-color: var(--menu-hover-bg);
    }
  }
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header {
  height: var(--header-height);
  background-color: var(--card-color);
  box-shadow: var(--box-shadow);
  padding: 0 var(--spacing-lg);
  @include flex-between;

  .header-left {
    @include flex-start;

    .collapse-btn {
      margin-right: var(--spacing-md);
      font-size: var(--font-size-lg);
    }
  }

  .header-right {
    @include flex-end;
  }
}

.content {
  flex: 1;
  padding: var(--content-padding);
  overflow-y: auto;
}

// 响应式布局
@include responsive(lg) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
    transform: translateX(0);
    transition: transform var(--transition-duration) var(--transition-timing);

    &.collapsed {
      transform: translateX(-100%);
    }
  }

  .main-content {
    margin-left: 0;
  }
}
</style>
