<template>
  <el-col :xs="24" :sm="isSidebarCollapsed ? 18 : 16" :md="isSidebarCollapsed ? 19 : 16"
    :lg="isSidebarCollapsed ? 20 : 16" class="menu-detail-col">

    <!-- 菜单详情卡片 -->
    <div class="menu-detail-card" v-if="currentNode">
      <!-- 卡片头部 -->
      <div class="detail-header">
        <div class="header-content">
          <div class="header-icon">
            <el-icon class="icon-detail">
              <InfoFilled />
            </el-icon>
          </div>
          <div class="header-text">
            <h3 class="detail-title">{{ currentNode.name }}</h3>
            <p class="detail-subtitle">{{ currentNode.path || '/' }}</p>
          </div>
        </div>
        <div class="header-actions">
          <el-tag :type="getNodeTypeColor(currentNode.meta?.type) as any" size="large" class="type-badge">
            {{ getNodeTypeText(currentNode.meta?.type) }}
          </el-tag>
        </div>
      </div>

      <!-- 内容滚动区域 -->
      <div class="detail-content">
        <!-- 基本信息区块 -->
        <div class="detail-section">
          <div class="section-title">
            <el-icon class="section-icon">
              <Document />
            </el-icon>
            <span>基本信息</span>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <label class="info-label">菜单名称</label>
              <div class="info-value">{{ currentNode.name }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">路由路径</label>
              <div class="info-value path-value">{{ currentNode.path || '-' }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">组件路径</label>
              <div class="info-value component-value">{{ currentNode.meta?.component || '-' }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">权限标识</label>
              <div class="info-value permission-value">{{ currentNode.permission || '-' }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">菜单图标</label>
              <div class="info-value icon-value">
                <el-icon v-if="currentNode.meta?.metaIcon" class="preview-icon">
                  <component :is="currentNode.meta.metaIcon" />
                </el-icon>
                <span>{{ currentNode.meta?.metaIcon || '-' }}</span>
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">排序权重</label>
              <div class="info-value order-value">{{ currentNode.menuOrder || 0 }}</div>
            </div>
          </div>
        </div>

        <!-- 状态配置区块 -->
        <div class="detail-section">
          <div class="section-title">
            <el-icon class="section-icon">
              <Setting />
            </el-icon>
            <span>状态配置</span>
          </div>
          <div class="status-grid">
            <div class="status-item">
              <div class="status-label">启用状态</div>
              <el-tag :type="currentNode.status === 1 ? 'success' : 'danger'" size="large" class="status-badge">
                <el-icon class="badge-icon">
                  <component :is="currentNode.status === 1 ? 'Check' : 'Close'" />
                </el-icon>
                {{ currentNode.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </div>
            <div class="status-item">
              <div class="status-label">总是显示</div>
              <el-switch :model-value="currentNode.meta?.alwaysShow" disabled size="large" class="status-switch" />
            </div>
            <div class="status-item">
              <div class="status-label">隐藏菜单</div>
              <el-switch :model-value="currentNode.meta?.metaHidden" disabled size="large" class="status-switch" />
            </div>
            <div class="status-item">
              <div class="status-label">保持活跃</div>
              <el-switch :model-value="currentNode.meta?.metaKeepAlive" disabled size="large" class="status-switch" />
            </div>
          </div>
        </div>

        <!-- 访问控制区块 -->
        <div class="detail-section" v-if="currentNode.meta?.metaRoles">
          <div class="section-title">
            <el-icon class="section-icon">
              <UserFilled />
            </el-icon>
            <span>访问控制</span>
          </div>
          <div class="roles-container">
            <el-tag v-for="role in getRolesArray(currentNode.meta.metaRoles)" :key="role" type="info" size="large"
              class="role-tag">
              {{ role }}
            </el-tag>
          </div>
        </div>

        <!-- 备注信息区块 -->
        <div class="detail-section" v-if="currentNode.remark">
          <div class="section-title">
            <el-icon class="section-icon">
              <ChatLineSquare />
            </el-icon>
            <span>备注信息</span>
          </div>
          <div class="remark-content">
            {{ currentNode.remark }}
          </div>
        </div>

      </div>

      <!-- 操作按钮区块 -->
      <div class="detail-actions">
        <el-button type="primary" size="large" @click="handleEdit(currentNode)" class="action-btn primary-btn">
          <el-icon class="btn-icon">
            <Edit />
          </el-icon>
          <span>编辑菜单</span>
        </el-button>
        <el-button v-if="currentNode.meta?.type !== 'F'" type="success" size="large"
          @click="handleAddChild(currentNode)" class="action-btn success-btn">
          <el-icon class="btn-icon">
            <Plus />
          </el-icon>
          <span>添加子菜单</span>
        </el-button>
        <el-button type="danger" size="large" @click="handleDelete(currentNode)" class="action-btn danger-btn">
          <el-icon class="btn-icon">
            <Delete />
          </el-icon>
          <span>删除菜单</span>
        </el-button>
      </div>
    </div>

    <!-- 空状态展示 -->
    <div v-else class="empty-state">
      <div class="empty-container">
        <div class="empty-illustration">
          <el-icon class="empty-icon">
            <FolderOpened />
          </el-icon>
          <div class="empty-decoration"></div>
        </div>
        <div class="empty-content">
          <h3 class="empty-title">选择一个菜单项</h3>
          <p class="empty-description">点击左侧菜单树中的任意节点来查看详细信息和进行管理操作</p>
          <div class="empty-tips">
            <div class="tip-item">
              <el-icon class="tip-icon">
                <InfoFilled />
              </el-icon>
              <span>查看菜单配置信息</span>
            </div>
            <div class="tip-item">
              <el-icon class="tip-icon">
                <Edit />
              </el-icon>
              <span>编辑菜单属性</span>
            </div>
            <div class="tip-item">
              <el-icon class="tip-icon">
                <Plus />
              </el-icon>
              <span>添加子级菜单</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-col>
</template>

<script setup lang="ts" name="RightView">
import type { Router } from '@/router/index.d'
import {
  InfoFilled,
  Document,
  Setting,
  UserFilled,
  ChatLineSquare,
  Edit,
  Plus,
  Delete,
  FolderOpened,
  Check,
  Close
} from '@element-plus/icons-vue'

// 定义组件接收的属性
defineProps({
  currentNode: {
    type: Object as () => Router | null,
    default: null
  },
  isSidebarCollapsed: {
    type: Boolean,
    default: false
  }
})

// 定义组件发出的事件
const emit = defineEmits(['edit', 'addChild', 'delete'])

// 获取节点类型文本
const getNodeTypeText = (type: string) => {
  const typeMap = {
    'M': '目录',
    'C': '菜单',
    'F': '按钮'
  }
  return typeMap[type as keyof typeof typeMap] || '未知'
}

// 获取节点类型颜色
const getNodeTypeColor = (type: string) => {
  const colorMap = {
    'M': 'primary',
    'C': 'success',
    'F': 'warning'
  }
  return colorMap[type as keyof typeof colorMap] || 'info'
}

// 解析角色数组
const getRolesArray = (roles: string) => {
  if (!roles) return []
  return roles.split(',').map(role => role.trim()).filter(Boolean)
}

// 处理编辑按钮点击
const handleEdit = (node: Router) => {
  emit('edit', node)
}

// 处理添加子菜单按钮点击
const handleAddChild = (node: Router) => {
  emit('addChild', node)
}

// 处理删除按钮点击
const handleDelete = (node: Router) => {
  emit('delete', node)
}
</script>

<style scoped lang="scss">
@use '@/assets/style/variables.scss' as *;

.menu-detail-col {
  height: 100%;
  transition: all var(--transition-normal) var(--transition-timing);
  display: flex;
  flex-direction: column;
}

// ==============================================
// 菜单详情卡片
// ==============================================
.menu-detail-card {
  height: 100%;
  background: var(--background-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  @include card-shadow;
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
}

// ==============================================
// 卡片头部
// ==============================================
.detail-header {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  color: var(--text-inverse);
  padding: var(--spacing-xl);
  @include flex-between;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    right: -20%;
    width: 200px;
    height: 200px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: var(--radius-full);
    transform: rotate(45deg);
  }

  .header-content {
    @include flex-start;
    gap: var(--spacing-lg);
    position: relative;
    z-index: 1;

    .header-icon {
      width: 64px;
      height: 64px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: var(--radius-xl);
      @include flex-center;
      backdrop-filter: blur(10px);

      .icon-detail {
        font-size: var(--font-size-xxl);
      }
    }

    .header-text {
      .detail-title {
        margin: 0;
        font-size: var(--font-size-xl);
        font-weight: var(--font-weight-bold);
        line-height: var(--line-height-tight);
      }

      .detail-subtitle {
        margin: var(--spacing-xs) 0 0 0;
        font-size: var(--font-size-md);
        opacity: 0.9;
        font-family: var(--font-family-mono);
      }
    }
  }

  .header-actions {
    position: relative;
    z-index: 1;

    .type-badge {
      padding: var(--spacing-sm) var(--spacing-lg);
      font-weight: var(--font-weight-semibold);
      border: 2px solid rgba(255, 255, 255, 0.3);
      background: rgba(255, 255, 255, 0.2);
      color: var(--text-inverse);
      backdrop-filter: blur(10px);
    }
  }
}

// ==============================================
// 内容滚动区域
// ==============================================
.detail-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0; // 确保flex子项能够正确收缩

  // 自定义滚动条
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: var(--background-secondary);
    border-radius: var(--radius-sm);
  }

  &::-webkit-scrollbar-thumb {
    background: var(--border-medium);
    border-radius: var(--radius-sm);

    &:hover {
      background: var(--primary-color);
    }
  }
}

// ==============================================
// 详情区块
// ==============================================
.detail-section {
  padding: var(--spacing-xl);
  border-bottom: 1px solid var(--border-light);

  &:last-of-type {
    border-bottom: none;
  }

  .section-title {
    @include flex-start;
    gap: var(--spacing-sm);
    margin-bottom: var(--spacing-lg);
    font-size: var(--font-size-lg);
    font-weight: var(--font-weight-semibold);
    color: var(--text-primary);

    .section-icon {
      color: var(--primary-color);
      font-size: var(--font-size-lg);
    }
  }
}

// ==============================================
// 信息网格
// ==============================================
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-lg);

  .info-item {
    .info-label {
      display: block;
      font-size: var(--font-size-sm);
      color: var(--text-secondary);
      font-weight: var(--font-weight-medium);
      margin-bottom: var(--spacing-xs);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .info-value {
      font-size: var(--font-size-md);
      color: var(--text-primary);
      font-weight: var(--font-weight-medium);
      padding: var(--spacing-sm) var(--spacing-md);
      background: var(--background-secondary);
      border-radius: var(--radius-md);
      border: 1px solid var(--border-light);
      min-height: 40px;
      @include flex-start;
      transition: all var(--transition-normal) var(--transition-timing);

      &:hover {
        border-color: var(--primary-color);
        background: var(--primary-lighter);
      }

      &.path-value,
      &.component-value {
        font-family: var(--font-family-mono);
        font-size: var(--font-size-sm);
        word-break: break-all;
      }

      &.permission-value {
        font-family: var(--font-family-mono);
        color: var(--primary-color);
        font-weight: var(--font-weight-semibold);
      }

      &.icon-value {
        .preview-icon {
          margin-right: var(--spacing-sm);
          font-size: var(--font-size-lg);
          color: var(--primary-color);
        }
      }

      &.order-value {
        font-weight: var(--font-weight-bold);
        color: var(--primary-color);
      }
    }
  }
}

// ==============================================
// 状态网格
// ==============================================
.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--spacing-lg);

  .status-item {
    padding: var(--spacing-lg);
    background: var(--background-secondary);
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-light);
    @include flex-between;
    transition: all var(--transition-normal) var(--transition-timing);

    &:hover {
      @include hover-lift;
      border-color: var(--primary-color);
    }

    .status-label {
      font-size: var(--font-size-md);
      font-weight: var(--font-weight-medium);
      color: var(--text-primary);
    }

    .status-badge {
      padding: var(--spacing-xs) var(--spacing-md);

      .badge-icon {
        margin-right: var(--spacing-xs);
      }
    }

    .status-switch {
      --el-switch-on-color: var(--success-color);
      --el-switch-off-color: var(--border-medium);
    }
  }
}

// ==============================================
// 角色容器
// ==============================================
.roles-container {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);

  .role-tag {
    padding: var(--spacing-sm) var(--spacing-md);
    font-weight: var(--font-weight-medium);
    border-radius: var(--radius-xl);
    background: var(--primary-lighter);
    color: var(--primary-color);
    border: 1px solid var(--primary-color);
  }
}

// ==============================================
// 备注内容
// ==============================================
.remark-content {
  padding: var(--spacing-lg);
  background: var(--background-secondary);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--primary-color);
  line-height: var(--line-height-relaxed);
  color: var(--text-primary);
  font-size: var(--font-size-md);
}

// ==============================================
// 操作按钮
// ==============================================
.detail-actions {
  padding: var(--spacing-xl);
  background: var(--background-secondary);
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  flex-shrink: 0;
  border-top: 1px solid var(--border-light);

  .action-btn {
    flex: 1;
    min-width: 140px;
    height: 48px;
    border-radius: var(--radius-md);
    font-weight: var(--font-weight-semibold);
    transition: all var(--transition-normal) var(--transition-timing);
    border: none;

    .btn-icon {
      margin-right: var(--spacing-sm);
    }

    &:hover {
      @include hover-lift;
    }

    &.primary-btn {
      background: var(--gradient-primary);
    }

    &.success-btn {
      background: var(--gradient-success);
    }

    &.danger-btn {
      background: var(--gradient-error);
    }
  }
}

// ==============================================
// 空状态
// ==============================================
.empty-state {
  height: 100%;
  @include flex-center;
  padding: var(--spacing-xl);
}

.empty-container {
  text-align: center;
  max-width: 400px;
  width: 100%;
}

.empty-illustration {
  position: relative;
  margin-bottom: var(--spacing-xl);

  .empty-icon {
    font-size: 80px;
    color: var(--border-medium);
    transition: all var(--transition-slow) var(--transition-timing);
  }

  .empty-decoration {
    position: absolute;
    top: -10px;
    right: -10px;
    width: 20px;
    height: 20px;
    background: var(--primary-color);
    border-radius: var(--radius-full);
    opacity: 0.3;
    animation: pulse 2s infinite;
  }

  &:hover .empty-icon {
    color: var(--primary-color);
    transform: scale(1.05);
  }
}

.empty-content {
  .empty-title {
    margin: 0 0 var(--spacing-md) 0;
    font-size: var(--font-size-xl);
    font-weight: var(--font-weight-semibold);
    color: var(--text-primary);
  }

  .empty-description {
    margin: 0 0 var(--spacing-xl) 0;
    color: var(--text-secondary);
    line-height: var(--line-height-relaxed);
  }

  .empty-tips {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-sm);

    .tip-item {
      @include flex-start;
      gap: var(--spacing-sm);
      padding: var(--spacing-sm) var(--spacing-md);
      background: var(--background-secondary);
      border-radius: var(--radius-md);
      font-size: var(--font-size-sm);
      color: var(--text-secondary);

      .tip-icon {
        color: var(--primary-color);
        flex-shrink: 0;
      }
    }
  }
}

// ==============================================
// 动画
// ==============================================
@keyframes pulse {

  0%,
  100% {
    opacity: 0.3;
    transform: scale(1);
  }

  50% {
    opacity: 0.6;
    transform: scale(1.2);
  }
}

// ==============================================
// 响应式适配
// ==============================================

// 大屏幕优化 (xl: 1200px+)
@include responsive(xl) {
  .info-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .status-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

// 中大屏幕 (lg: 992px+)
@include responsive(lg) {
  .detail-header {
    flex-direction: column;
    gap: var(--spacing-lg);
    text-align: center;
    padding: var(--spacing-xl) var(--spacing-lg);

    .header-content {
      justify-content: center;
    }
  }

  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .status-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .detail-actions {
    .action-btn {
      min-width: 120px;
    }
  }
}

// 中等屏幕 (md: 768px+)
@include responsive(md) {
  .detail-header {
    padding: var(--spacing-lg);

    .header-content {
      gap: var(--spacing-md);

      .header-icon {
        width: 48px;
        height: 48px;

        .icon-detail {
          font-size: var(--font-size-lg);
        }
      }

      .header-text {
        .detail-title {
          font-size: var(--font-size-lg);
        }
      }
    }

    .type-badge {
      padding: var(--spacing-xs) var(--spacing-md);
      font-size: var(--font-size-sm);
    }
  }

  .detail-section {
    padding: var(--spacing-lg);

    .section-title {
      font-size: var(--font-size-md);
      margin-bottom: var(--spacing-md);
    }
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: var(--spacing-md);
  }

  .status-grid {
    grid-template-columns: 1fr;
    gap: var(--spacing-md);
  }

  .detail-actions {
    padding: var(--spacing-md);
    flex-direction: column;

    .action-btn {
      width: 100%;
      min-width: unset;
    }
  }

  .empty-tips {
    .tip-item {
      flex-direction: column;
      text-align: center;
      gap: var(--spacing-xs);
    }
  }
}

// 小屏幕 (sm: 576px+)
@include responsive(sm) {
  .menu-detail-card {
    border-radius: var(--radius-md);
    margin: var(--spacing-xs);
  }

  .detail-header {
    padding: var(--spacing-md);

    .header-content {
      .header-icon {
        width: 40px;
        height: 40px;

        .icon-detail {
          font-size: var(--font-size-md);
        }
      }

      .header-text {
        .detail-title {
          font-size: var(--font-size-md);
        }

        .detail-subtitle {
          font-size: var(--font-size-sm);
        }
      }
    }
  }

  .detail-section {
    padding: var(--spacing-md);
  }

  .detail-actions {
    padding: var(--spacing-md);
    gap: var(--spacing-sm);

    .action-btn {
      height: 40px;
      font-size: var(--font-size-sm);

      .btn-icon {
        margin-right: var(--spacing-xs);
      }
    }
  }

  .empty-container {
    padding: var(--spacing-md);

    .empty-icon {
      font-size: 60px;
    }

    .empty-title {
      font-size: var(--font-size-lg);
    }
  }
}

// 超小屏幕 (xs: <576px)
@media (max-width: 575px) {
  .menu-detail-card {
    margin: 0;
    border-radius: 0;
  }

  .detail-header {
    padding: var(--spacing-sm);

    .header-content {
      flex-direction: column;
      gap: var(--spacing-sm);
      text-align: center;

      .header-icon {
        width: 36px;
        height: 36px;
      }
    }

    .type-badge {
      padding: 2px var(--spacing-xs);
      font-size: 10px;
    }
  }

  .detail-section {
    padding: var(--spacing-sm);

    .section-title {
      font-size: var(--font-size-sm);
      margin-bottom: var(--spacing-sm);
    }
  }

  .info-item {
    .info-label {
      font-size: 10px;
    }

    .info-value {
      font-size: var(--font-size-sm);
      padding: var(--spacing-xs);
      min-height: 32px;
    }
  }

  .status-item {
    padding: var(--spacing-sm);
    flex-direction: column;
    gap: var(--spacing-xs);
    text-align: center;

    .status-label {
      font-size: var(--font-size-sm);
    }
  }

  .detail-actions {
    padding: var(--spacing-sm);

    .action-btn {
      height: 36px;
      font-size: 12px;

      .btn-icon {
        margin-right: 2px;
      }

      span {
        display: none;
      }
    }
  }
}
</style>
