<script setup lang="ts">
import nav from '@/common/nav/nav'
import { refreshGuide } from '../guide/guide'

defineEmits(['export-md', 'import-md', 'export-picture'])
</script>

<template>
  <nav class="editor-nav">
    <ul class="nav-list">
      <li v-for="(navItem, idx) in nav" :key="idx" class="nav-item">
        <!-- 带子菜单的导航项 -->
      <template v-if="navItem.children">
          <el-dropdown class="nav-dropdown" trigger="hover" placement="bottom">
            <div class="nav-link nav-link--dropdown">
              <span>{{ navItem.name }}</span>
              <i class="iconfont icon-down nav-arrow"></i>
            </div>
          <template #dropdown>
              <el-dropdown-menu class="nav-dropdown-menu">
                <el-dropdown-item
                  v-for="(subNavItem, sidx) in navItem.children"
                  :key="sidx"
                  class="nav-dropdown-item"
                >
                  <!-- 导入MD特殊处理 -->
                  <label for="import_md" v-if="subNavItem.startsWith('导入')" class="nav-file-label">
                    <i class="iconfont icon-import nav-item-icon"></i>
                    <span>导入MD</span>
                    <input
                      accept=".md"
                      id="import_md"
                      type="file"
                      @change="$emit('import-md', $event)"
                      class="nav-file-input"
                    />
                </label>
                  <!-- 导出MD -->
                  <div v-else-if="subNavItem.includes('导出MD')" @click="$emit('export-md')" class="nav-action">
                    <i class="iconfont icon-export nav-item-icon"></i>
                    <span>{{ subNavItem }}</span>
                  </div>
                  <!-- 导出图片 -->
                  <div v-else @click="$emit('export-picture')" class="nav-action">
                    <i class="iconfont icon-picture nav-item-icon"></i>
                    <span>{{ subNavItem }}</span>
                  </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>

        <!-- 普通导航项 -->
      <template v-else>
          <router-link :to="navItem.path || ''" class="nav-link">
            <span>{{ navItem.name }}</span>
          </router-link>
      </template>
    </li>

      <!-- 引导按钮 -->
      <li class="nav-item nav-item--special">
        <button class="nav-guide-btn" @click="refreshGuide()">
          <i class="iconfont icon-help nav-item-icon"></i>
          <span>开启引导</span>
        </button>
      </li>
  </ul>
  </nav>
</template>

<style lang="scss" scoped>
@use '@/assets/style/variables.scss' as *;
@use '@/assets/style/darktheme.scss' as *;

.editor-nav {
  .nav-list {
    @include flex-start;
    gap: var(--spacing-sm);
    list-style: none;
    margin: 0;
    padding: 0;
  }

  .nav-item {
    position: relative;
  }

  // 导航链接基础样式
  .nav-link {
    @include flex-center;
    gap: var(--spacing-xs);
    padding: var(--spacing-sm) var(--spacing-md);
    border-radius: var(--radius-md);
    text-decoration: none;
    color: var(--text-secondary);
    font-weight: var(--font-weight-medium);
    font-size: var(--font-size-md);
    transition: all var(--transition-normal) var(--transition-timing);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background: var(--primary-light);
      opacity: 0;
      transition: opacity var(--transition-normal) var(--transition-timing);
    }

    &:hover {
      color: var(--primary-color);
      background: var(--primary-lighter);
      @include hover-lift;

      &::before {
        opacity: 1;
      }
    }

    &.router-link-active {
      color: var(--primary-color);
      background: var(--primary-light);
      font-weight: var(--font-weight-semibold);
    }

    // 下拉箭头动画
    &--dropdown {
      cursor: pointer;

      .nav-arrow {
        font-size: var(--font-size-sm);
        transition: transform var(--transition-normal) var(--transition-timing);
      }

      &:hover .nav-arrow {
        transform: rotate(180deg);
  }
}
  }

  // 导航图标
  .nav-item-icon {
    font-size: var(--font-size-md);
    transition: transform var(--transition-normal) var(--transition-timing);
  }

  .nav-link:hover .nav-item-icon {
    transform: scale(1.1);
  }

  // 下拉菜单样式
  .nav-dropdown {
    .nav-dropdown-menu {
      min-width: 180px;
      border: 1px solid var(--border-light);
      border-radius: var(--radius-md);
      @include card-shadow;
      background: var(--background-card);
      padding: var(--spacing-sm);
}

    .nav-dropdown-item {
      margin: 2px 0;
      border-radius: var(--radius-sm);
      transition: background-color var(--transition-fast) var(--transition-timing);

      &:hover {
        background: var(--primary-lighter);
      }
    }
  }

  // 下拉菜单项样式
  .nav-action,
  .nav-file-label {
    @include flex-start;
    gap: var(--spacing-sm);
    width: 100%;
    padding: var(--spacing-sm) var(--spacing-md);
    color: var(--text-primary);
    cursor: pointer;
    border-radius: var(--radius-sm);
    transition: all var(--transition-fast) var(--transition-timing);

    &:hover {
      color: var(--primary-color);
      background: var(--primary-lighter);
    }
  }

  // 文件输入框隐藏
  .nav-file-input {
    position: absolute;
    width: 1px;
    height: 1px;
    opacity: 0;
    pointer-events: none;
}

  // 引导按钮特殊样式
  .nav-item--special {
    .nav-guide-btn {
      @include flex-center;
      gap: var(--spacing-xs);
      padding: var(--spacing-sm) var(--spacing-md);
      border: none;
      border-radius: var(--radius-md);
      background: linear-gradient(135deg, var(--success-color) 0%, #059669 100%);
      color: var(--text-inverse);
      font-weight: var(--font-weight-medium);
      font-size: var(--font-size-md);
      cursor: pointer;
      transition: all var(--transition-normal) var(--transition-timing);
      font-family: var(--font-family-sans);

      &:hover {
        background: linear-gradient(135deg, #059669 0%, #047857 100%);
        @include hover-lift;
        box-shadow: var(--shadow-md);
      }

      &:active {
        transform: translateY(1px);
      }

      .nav-item-icon {
        font-size: var(--font-size-md);
      }
    }
  }
}

// 响应式设计
@include responsive(md) {
  .editor-nav {
    .nav-link {
      padding: var(--spacing-xs) var(--spacing-sm);
      font-size: var(--font-size-sm);

      span {
        display: none;
      }
    }

    .nav-guide-btn {
      padding: var(--spacing-xs) var(--spacing-sm);

      span {
        display: none;
      }
    }
  }
}

@include responsive(sm) {
  .editor-nav {
    .nav-list {
      gap: var(--spacing-xs);
    }

    .nav-link {
      padding: var(--spacing-xs);
    }

    .nav-guide-btn {
      padding: var(--spacing-xs);
    }
  }
}

// 主题适配增强
[data-theme='dark'] {
  .editor-nav {
    .nav-dropdown-menu {
      background: var(--background-card);
      border-color: var(--border-light);

      .nav-dropdown-item:hover {
        background: var(--menu-hover-bg);
      }
    }

    .nav-action:hover,
    .nav-file-label:hover {
      background: var(--menu-hover-bg);
      color: var(--menu-active-text-color);
    }
  }
}

// 动画效果
@keyframes navItemSlideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.nav-item {
  animation: navItemSlideIn 0.3s var(--transition-timing);
  animation-fill-mode: both;
}

// 延迟动画效果
@for $i from 1 through 10 {
  .nav-item:nth-child(#{$i}) {
    animation-delay: #{$i * 0.05}s;
  }
}
</style>
