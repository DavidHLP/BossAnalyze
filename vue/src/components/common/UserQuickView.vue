<template>
  <el-dialog
    v-model="visible"
    title="用户详细信息"
    width="600px"
    center
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    append-to-body
    class="user-quick-view-dialog"
  >
    <template #header>
      <div class="dialog-header">
        <div class="header-left">
          <el-icon class="header-icon"><User /></el-icon>
          <span class="dialog-title">用户详细信息</span>
        </div>
        <div class="header-right">
          <el-tag
            :type="userData.status === 1 ? 'success' : 'danger'"
            effect="light"
            round
            size="large"
          >
            {{ userData.status === 1 ? '✓ 启用' : '✗ 禁用' }}
          </el-tag>
        </div>
      </div>
    </template>

    <div class="user-quick-view-content" v-loading="loading">
      <!-- 用户基本信息 -->
      <div class="user-profile-section">
        <div class="avatar-container">
          <el-avatar
            :size="100"
            :src="avatarUrl"
            class="user-avatar"
          >
            <el-icon><User /></el-icon>
          </el-avatar>
          <div class="avatar-overlay">
            <el-icon><View /></el-icon>
          </div>
        </div>

        <div class="user-basic-info">
          <h2 class="user-name">{{ userData.name || '未设置用户名' }}</h2>
          <div class="user-role">
            <el-tag type="primary" effect="light" size="large">
              <el-icon><UserFilled /></el-icon>
              {{ userData.roleName || '未分配角色' }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 详细信息卡片 -->
      <div class="info-cards-grid">
        <!-- 联系信息 -->
        <div class="info-card">
          <div class="card-header">
            <el-icon class="card-icon"><Message /></el-icon>
            <span class="card-title">联系信息</span>
          </div>
          <div class="card-content">
            <div class="info-row">
              <span class="info-label">邮箱地址</span>
              <span class="info-value">{{ userData.email || '暂未设置' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">联系地址</span>
              <span class="info-value">{{ userData.address || '暂未填写' }}</span>
            </div>
          </div>
        </div>

        <!-- 账户信息 -->
        <div class="info-card">
          <div class="card-header">
            <el-icon class="card-icon"><Clock /></el-icon>
            <span class="card-title">账户信息</span>
          </div>
          <div class="card-content">
            <div class="info-row">
              <span class="info-label">用户ID</span>
              <span class="info-value">#{{ userData.id }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">创建时间</span>
              <span class="info-value">{{ userData.createTime || '暂无记录' }}</span>
            </div>
            <div class="info-row" v-if="userData.createDate">
              <span class="info-label">注册日期</span>
              <span class="info-value">{{ userData.createDate }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 个人简介 -->
      <div class="introduction-card" v-if="userData.introduction">
        <div class="card-header">
          <el-icon class="card-icon"><Document /></el-icon>
          <span class="card-title">个人简介</span>
        </div>
        <div class="card-content">
          <p class="introduction-text">{{ userData.introduction }}</p>
        </div>
      </div>

      <!-- 操作历史或统计信息 -->
      <div class="stats-section">
        <div class="stats-title">
          <el-icon><DataLine /></el-icon>
          <span>账户统计</span>
        </div>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-value">{{ getAccountAge() }}</div>
            <div class="stat-label">账户年龄</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ userData.status === 1 ? '正常' : '禁用' }}</div>
            <div class="stat-label">账户状态</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ userData.roleName ? '已分配' : '未分配' }}</div>
            <div class="stat-label">角色状态</div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button
          @click="handleEdit"
          type="primary"
          :icon="Edit"
          class="footer-btn"
        >
          编辑用户
        </el-button>
        <el-button
          @click="handleClose"
          class="footer-btn"
        >
          关闭
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup name="UserQuickView">
import {
  ElDialog,
  ElAvatar,
  ElTag,
  ElButton,
  ElIcon,
} from 'element-plus'
import {
  User,
  UserFilled,
  Message,
  Clock,
  Document,
  View,
  Edit,
  DataLine,
} from '@element-plus/icons-vue'
import { ref, watch, computed } from 'vue'
import type { UserBaseInfo } from '@/api/auth/auth.d'
import { getImageUrl } from '@/api/minio/minio'

// Props 定义
interface Props {
  visible: boolean
  userData: UserBaseInfo
  loading?: boolean
}

// Emits 定义
interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'edit', userData: UserBaseInfo): void
  (e: 'close'): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  userData: () => ({
    id: 0,
    createDate: '',
    name: '',
    address: '',
    email: '',
    status: 1,
    roleName: '',
    introduction: '',
    createTime: '',
    avatar: '',
  }),
})

const emit = defineEmits<Emits>()

// 响应式数据
const avatarUrl = ref<string>('')

// 计算属性
const visible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// 方法
const handleClose = () => {
  emit('close')
  emit('update:visible', false)
}

const handleEdit = () => {
  emit('edit', props.userData)
  handleClose()
}

// 计算账户年龄
const getAccountAge = () => {
  if (!props.userData.createTime && !props.userData.createDate) {
    return '未知'
  }

  const createDate = new Date(props.userData.createTime || props.userData.createDate)
  const now = new Date()
  const diffTime = Math.abs(now.getTime() - createDate.getTime())
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays < 30) {
    return `${diffDays} 天`
  } else if (diffDays < 365) {
    return `${Math.floor(diffDays / 30)} 个月`
  } else {
    return `${Math.floor(diffDays / 365)} 年`
  }
}

// 加载头像
const loadAvatar = async () => {
  if (props.userData.avatar) {
    try {
      const res = await getImageUrl(props.userData.avatar)
      avatarUrl.value = res.url
    } catch (error) {
      console.error('加载头像失败:', error)
      avatarUrl.value = ''
    }
  } else {
    avatarUrl.value = ''
  }
}

// 监听用户数据变化
watch(
  () => props.userData,
  (newUserData) => {
    if (newUserData && newUserData.avatar) {
      loadAvatar()
    }
  },
  { immediate: true, deep: true }
)

// 监听弹窗显示状态
watch(
  () => props.visible,
  (newVisible) => {
    if (newVisible && props.userData.avatar) {
      loadAvatar()
    }
  }
)
</script>

<style lang="scss" scoped>
// 导入样式系统
@use '@/assets/style/variables.scss' as *;
@use '@/assets/style/theme.scss' as *;

.user-quick-view-dialog {
  :deep(.el-dialog) {
    border-radius: var(--radius-xl);
    overflow: hidden;
    box-shadow: var(--shadow-2xl);
    border: 1px solid var(--border-light);
  }

  :deep(.el-dialog__header) {
    padding: 0;
    margin: 0;
    border-bottom: 1px solid var(--border-light);

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--spacing-lg) var(--spacing-xl);
      background: var(--gradient-primary);
      color: var(--text-inverse);

      .header-left {
        display: flex;
        align-items: center;
        gap: var(--spacing-sm);

        .header-icon {
          font-size: var(--font-size-xl);
        }

        .dialog-title {
          font-size: var(--font-size-lg);
          font-weight: var(--font-weight-semibold);
        }
      }

      .header-right {
        .el-tag {
          font-weight: var(--font-weight-medium);
          border: 1px solid rgba(255, 255, 255, 0.3);
          background: rgba(255, 255, 255, 0.1);
          backdrop-filter: blur(10px);
        }
      }
    }
  }

  :deep(.el-dialog__body) {
    padding: 0;
    background: var(--background-card);
  }

  :deep(.el-dialog__footer) {
    padding: 0;
    margin: 0;
  }
}

.user-quick-view-content {
  padding: var(--spacing-xl);
  min-height: 400px;

  // 用户资料部分
  .user-profile-section {
    display: flex;
    align-items: flex-start;
    gap: var(--spacing-xl);
    margin-bottom: var(--spacing-xl);
    padding: var(--spacing-xl);
    background: linear-gradient(135deg, #f8fafe 0%, #f0f7ff 100%);
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-light);
    position: relative;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: radial-gradient(circle at 30% 30%, rgba(59, 130, 246, 0.1) 0%, transparent 50%);
      border-radius: var(--radius-lg);
      pointer-events: none;
    }

    .avatar-container {
      position: relative;
      flex-shrink: 0;

      .user-avatar {
        border: 3px solid var(--primary-color);
        box-shadow: var(--shadow-lg);
        transition: all var(--transition-normal) var(--transition-timing);
      }

      .avatar-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.3);
        border-radius: var(--radius-full);
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity var(--transition-normal) var(--transition-timing);
        cursor: pointer;

        .el-icon {
          color: var(--text-inverse);
          font-size: var(--font-size-xl);
        }

        &:hover {
          opacity: 1;
        }
      }
    }

    .user-basic-info {
      flex: 1;
      position: relative;
      z-index: 1;

      .user-name {
        font-size: var(--font-size-xxl);
        font-weight: var(--font-weight-bold);
        color: var(--text-primary);
        margin: 0 0 var(--spacing-md) 0;
        background: var(--gradient-primary);
        background-clip: text;
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }

      .user-role {
        .el-tag {
          font-size: var(--font-size-md);
          padding: var(--spacing-sm) var(--spacing-lg);

          .el-icon {
            margin-right: var(--spacing-xs);
          }
        }
      }
    }
  }

  // 信息卡片网格
  .info-cards-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: var(--spacing-lg);
    margin-bottom: var(--spacing-xl);
  }

  .info-card {
    background: var(--background-card);
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-light);
    overflow: hidden;
    transition: all var(--transition-normal) var(--transition-timing);

    &:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-md);
      border-color: var(--primary-light);
    }

    .card-header {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
      padding: var(--spacing-lg);
      background: linear-gradient(135deg, var(--primary-lighter) 0%, var(--background-secondary) 100%);
      border-bottom: 1px solid var(--border-light);

      .card-icon {
        color: var(--primary-color);
        font-size: var(--font-size-lg);
      }

      .card-title {
        font-weight: var(--font-weight-semibold);
        color: var(--text-primary);
        font-size: var(--font-size-md);
      }
    }

    .card-content {
      padding: var(--spacing-lg);

      .info-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: var(--spacing-sm) 0;
        border-bottom: 1px solid var(--border-lighter);

        &:last-child {
          border-bottom: none;
          padding-bottom: 0;
        }

        .info-label {
          color: var(--text-secondary);
          font-size: var(--font-size-sm);
          font-weight: var(--font-weight-medium);
        }

        .info-value {
          color: var(--text-primary);
          font-size: var(--font-size-md);
          font-weight: var(--font-weight-medium);
          text-align: right;
          max-width: 60%;
          word-break: break-word;
        }
      }
    }
  }

  // 个人简介卡片
  .introduction-card {
    background: var(--background-card);
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-light);
    overflow: hidden;
    margin-bottom: var(--spacing-xl);

    .card-header {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
      padding: var(--spacing-lg);
      background: linear-gradient(135deg, var(--success-lighter) 0%, var(--background-secondary) 100%);
      border-bottom: 1px solid var(--border-light);

      .card-icon {
        color: var(--success-color);
        font-size: var(--font-size-lg);
      }

      .card-title {
        font-weight: var(--font-weight-semibold);
        color: var(--text-primary);
        font-size: var(--font-size-md);
      }
    }

    .card-content {
      padding: var(--spacing-lg);

      .introduction-text {
        color: var(--text-secondary);
        line-height: var(--line-height-relaxed);
        font-size: var(--font-size-md);
        margin: 0;
      }
    }
  }

  // 统计信息部分
  .stats-section {
    background: var(--background-card);
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-light);
    padding: var(--spacing-lg);

    .stats-title {
      display: flex;
      align-items: center;
      gap: var(--spacing-sm);
      margin-bottom: var(--spacing-lg);
      color: var(--text-primary);
      font-weight: var(--font-weight-semibold);
      font-size: var(--font-size-lg);

      .el-icon {
        color: var(--info-color);
        font-size: var(--font-size-xl);
      }
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: var(--spacing-lg);

      @media (max-width: 480px) {
        grid-template-columns: 1fr;
      }

      .stat-item {
        text-align: center;
        padding: var(--spacing-lg);
        background: linear-gradient(135deg, var(--background-secondary) 0%, var(--primary-lighter) 100%);
        border-radius: var(--radius-md);
        border: 1px solid var(--border-light);
        transition: all var(--transition-normal) var(--transition-timing);

        &:hover {
          transform: translateY(-2px);
          box-shadow: var(--shadow-sm);
        }

        .stat-value {
          font-size: var(--font-size-lg);
          font-weight: var(--font-weight-bold);
          color: var(--primary-color);
          margin-bottom: var(--spacing-xs);
        }

        .stat-label {
          font-size: var(--font-size-sm);
          color: var(--text-secondary);
          font-weight: var(--font-weight-medium);
        }
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-md);
  padding: var(--spacing-lg) var(--spacing-xl);
  background: var(--background-secondary);
  border-top: 1px solid var(--border-light);

  .footer-btn {
    padding: var(--spacing-md) var(--spacing-lg);
    border-radius: var(--radius-md);
    font-weight: var(--font-weight-medium);
    transition: all var(--transition-normal) var(--transition-timing);

    &:hover {
      transform: translateY(-1px);
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .user-quick-view-dialog {
    :deep(.el-dialog) {
      width: 95% !important;
      margin: var(--spacing-md) auto !important;
    }
  }

  .user-quick-view-content {
    padding: var(--spacing-lg);

    .user-profile-section {
      flex-direction: column;
      text-align: center;
      gap: var(--spacing-lg);

      .user-basic-info {
        .user-name {
          font-size: var(--font-size-xl);
        }
      }
    }

    .info-cards-grid {
      grid-template-columns: 1fr;
      gap: var(--spacing-md);
    }
  }

  .dialog-footer {
    flex-direction: column;
    gap: var(--spacing-sm);

    .footer-btn {
      width: 100%;
    }
  }
}

// 动画效果
@keyframes slideInUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.user-quick-view-content {
  animation: slideInUp var(--transition-normal) var(--transition-timing);
}
</style>
