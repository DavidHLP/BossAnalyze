<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑角色' : '添加角色'"
    width="520px"
    destroy-on-close
    top="8vh"
    class="role-dialog"
    @close="handleClose"
  >
    <!-- 自定义对话框头部 -->
    <template #header>
      <div class="dialog-header-wrapper">
        <div class="header-content">
          <div class="header-icon">
            <el-icon>
              <User v-if="!isEdit" />
              <Edit v-else />
            </el-icon>
          </div>
          <div class="header-text">
            <h3 class="dialog-title">{{ isEdit ? '编辑角色' : '添加角色' }}</h3>
            <p class="dialog-subtitle">{{ isEdit ? '修改角色信息和权限设置' : '创建新的系统角色' }}</p>
          </div>
        </div>
      </div>
    </template>

    <!-- 对话框内容 -->
    <div class="dialog-content-wrapper">
      <el-form
        :model="localRoleData"
        label-width="90px"
        ref="roleFormRef"
        class="role-form"
        label-position="top"
      >
        <!-- 角色名称 -->
        <div class="form-section">
          <el-form-item
            label="角色名称"
            prop="roleName"
            :rules="[{ required: true, message: '请输入角色名称', trigger: 'blur' }]"
            class="form-item-enhanced"
          >
            <div class="input-wrapper">
              <div class="input-icon">
                <el-icon><UserFilled /></el-icon>
              </div>
              <el-input
                v-model="localRoleData.roleName"
                placeholder="请输入角色名称，如：管理员、编辑者等"
                :disabled="isEdit && (localRoleData.roleName === 'ADMIN' || localRoleData.roleName === 'USER')"
                class="enhanced-input"
                size="large"
              />
            </div>
            <div class="input-hint" v-if="isEdit && (localRoleData.roleName === 'ADMIN' || localRoleData.roleName === 'USER')">
              <el-icon><InfoFilled /></el-icon>
              <span>系统内置角色不允许修改名称</span>
            </div>
          </el-form-item>
        </div>

        <!-- 角色描述 -->
        <div class="form-section">
          <el-form-item
            label="角色描述"
            prop="remark"
            class="form-item-enhanced"
          >
            <div class="textarea-wrapper">
              <div class="textarea-icon">
                <el-icon><Document /></el-icon>
              </div>
              <el-input
                v-model="localRoleData.remark"
                type="textarea"
                :rows="4"
                placeholder="请描述该角色的职责和权限范围..."
                class="enhanced-textarea"
                maxlength="200"
                show-word-limit
              />
            </div>
          </el-form-item>
        </div>

        <!-- 角色状态 -->
        <div class="form-section">
          <el-form-item
            label="角色状态"
            class="form-item-enhanced status-form-item"
          >
            <div class="status-wrapper">
              <div class="status-info">
                <div class="status-icon" :class="{ active: localRoleData.status === 1 }">
                  <el-icon>
                    <CircleCheckFilled v-if="localRoleData.status === 1" />
                    <CircleCloseFilled v-else />
                  </el-icon>
                </div>
                <div class="status-text">
                  <div class="status-label">
                    {{ localRoleData.status === 1 ? '启用状态' : '禁用状态' }}
                  </div>
                  <div class="status-desc">
                    {{ localRoleData.status === 1 ? '角色处于活跃状态，可以正常使用' : '角色已被禁用，无法分配给用户' }}
                  </div>
                </div>
              </div>
              <el-switch
                v-model="localRoleData.status"
                :active-value="1"
                :inactive-value="0"
                class="enhanced-switch"
                size="large"
              />
            </div>
          </el-form-item>
        </div>

        <!-- 角色预览卡片 -->
        <div class="form-section">
          <div class="role-preview-card">
            <div class="preview-header">
              <el-icon><View /></el-icon>
              <span>角色预览</span>
            </div>
            <div class="preview-content">
              <div class="role-preview-info">
                <div class="role-preview-avatar">
                  <el-icon>
                    <UserFilled v-if="localRoleData.roleName !== 'ADMIN'" />
                    <Star v-else />
                  </el-icon>
                </div>
                <div class="role-preview-details">
                  <div class="preview-name">
                    {{ localRoleData.roleName || '角色名称' }}
                    <el-tag
                      :type="localRoleData.status === 1 ? 'success' : 'danger'"
                      size="small"
                      class="preview-status-tag"
                    >
                      {{ localRoleData.status === 1 ? '启用' : '禁用' }}
                    </el-tag>
                  </div>
                  <div class="preview-desc">
                    {{ localRoleData.remark || '暂无描述信息' }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-form>
    </div>

    <!-- 对话框底部操作按钮 -->
    <template #footer>
      <div class="dialog-footer-wrapper">
        <div class="action-buttons">
          <el-button
            type="default"
            size="large"
            class="cancel-btn"
            @click="handleCancel"
          >
            <el-icon><Close /></el-icon>
            <span>取消</span>
          </el-button>
          <el-button
            type="primary"
            size="large"
            class="confirm-btn"
            @click="handleConfirm"
          >
            <el-icon><Check /></el-icon>
            <span>{{ isEdit ? '保存更改' : '创建角色' }}</span>
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { FormInstance } from 'element-plus';
import type { Role } from '@/api/auth/auth.d';
import {
  Check,
  Close,
  User,
  Edit,
  UserFilled,
  Document,
  InfoFilled,
  CircleCheckFilled,
  CircleCloseFilled,
  View,
  Star
} from '@element-plus/icons-vue';

const props = defineProps<{
  visible: boolean;
  isEdit: boolean;
  roleData: Partial<Role>;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'update:roleData', value: Partial<Role>): void;
  (e: 'cancel'): void;
  (e: 'confirm', data: Partial<Role>): void;
}>();

const dialogVisible = ref(props.visible);
const roleFormRef = ref<FormInstance>();
const localRoleData = ref<Partial<Role>>({ ...props.roleData });

// 监听 visible 属性变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val;
});

// 监听 roleData 属性变化
watch(() => props.roleData, (val) => {
  localRoleData.value = { ...val };
}, { deep: true });

// 监听 dialogVisible 变化
watch(dialogVisible, (val) => {
  emit('update:visible', val);
});

// 处理关闭事件
const handleClose = () => {
  emit('update:visible', false);
};

// 处理取消事件
const handleCancel = () => {
  emit('cancel');
  emit('update:visible', false);
};

// 处理确认事件
const handleConfirm = async () => {
  if (!roleFormRef.value) return;

  try {
    await roleFormRef.value.validate();
    emit('confirm', localRoleData.value);
  } catch (error) {
    console.error('表单验证失败:', error);
  }
};
</script>

<style scoped lang="scss">
@use '@/assets/style/variables.scss' as *;

// ==============================================
// 角色对话框整体样式
// ==============================================
.role-dialog {
  :deep(.el-dialog) {
    border-radius: var(--radius-xl);
    overflow: hidden;
    @include card-shadow;
    background: var(--background-card);
  }

  :deep(.el-dialog__header) {
    padding: 0;
    margin: 0;
    border-bottom: none;
  }

  :deep(.el-dialog__body) {
    padding: 0;
  }

  :deep(.el-dialog__footer) {
    padding: 0;
    border-top: none;
  }
}

// ==============================================
// 对话框头部样式
// ==============================================
.dialog-header-wrapper {
  background: var(--gradient-primary);
  color: var(--text-inverse);
  padding: var(--spacing-xl) var(--spacing-xxxl);
  position: relative;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: rgba(255, 255, 255, 0.1);
  }

  .header-content {
    @include flex-start;
    gap: var(--spacing-lg);
    align-items: flex-start;

    .header-icon {
      @include flex-center;
      width: 54px;
      height: 54px;
      background: rgba(255, 255, 255, 0.15);
      border-radius: var(--radius-lg);
      font-size: var(--font-size-xl);
      flex-shrink: 0;
      backdrop-filter: blur(10px);
      border: 1px solid rgba(255, 255, 255, 0.2);
    }

    .header-text {
      flex: 1;

      .dialog-title {
        margin: 0 0 var(--spacing-xs) 0;
        font-size: var(--font-size-xxl);
        font-weight: var(--font-weight-bold);
        line-height: var(--line-height-tight);
        color: var(--text-inverse);
      }

      .dialog-subtitle {
        margin: 0;
        font-size: var(--font-size-md);
        font-weight: var(--font-weight-normal);
        opacity: 0.9;
        line-height: var(--line-height-normal);
      }
    }
  }
}

// ==============================================
// 对话框内容区域
// ==============================================
.dialog-content-wrapper {
  padding: var(--spacing-xxxl) var(--spacing-xxxl) var(--spacing-xl);
  background: var(--background-card);
}

.role-form {
  .form-section {
    margin-bottom: var(--spacing-xl);

    &:last-child {
      margin-bottom: 0;
    }
  }

  .form-item-enhanced {
    :deep(.el-form-item__label) {
      font-size: var(--font-size-lg);
      font-weight: var(--font-weight-semibold);
      color: var(--text-primary);
      margin-bottom: var(--spacing-md);
      line-height: var(--line-height-tight);
    }

    :deep(.el-form-item__content) {
      line-height: normal;
    }
  }
}

// ==============================================
// 输入框增强样式
// ==============================================
.input-wrapper, .textarea-wrapper {
  position: relative;

  .input-icon, .textarea-icon {
    position: absolute;
    left: var(--spacing-lg);
    top: 50%;
    transform: translateY(-50%);
    color: var(--text-secondary);
    font-size: var(--font-size-lg);
    z-index: 10;
  }

  .textarea-icon {
    top: var(--spacing-lg);
    transform: none;
  }

  .enhanced-input {
    :deep(.el-input__wrapper) {
      padding-left: calc(var(--spacing-lg) * 3);
      border-radius: var(--radius-lg);
      border: 2px solid var(--border-light);
      box-shadow: none;
      transition: all var(--transition-normal) var(--transition-timing);
      background: var(--background-card);
      min-height: 50px;

      &:hover {
        border-color: var(--border-medium);
      }

      &.is-focus {
        border-color: var(--primary-color);
        box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
      }
    }

    :deep(.el-input__inner) {
      font-size: var(--font-size-md);
      color: var(--text-primary);

      &::placeholder {
        color: var(--text-disabled);
      }
    }
  }

  .enhanced-textarea {
    :deep(.el-textarea__inner) {
      padding-left: calc(var(--spacing-lg) * 3);
      border-radius: var(--radius-lg);
      border: 2px solid var(--border-light);
      font-size: var(--font-size-md);
      color: var(--text-primary);
      transition: all var(--transition-normal) var(--transition-timing);
      resize: none;

      &:hover {
        border-color: var(--border-medium);
      }

      &:focus {
        border-color: var(--primary-color);
        box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
      }

      &::placeholder {
        color: var(--text-disabled);
      }
    }
  }
}

.input-hint {
  @include flex-start;
  gap: var(--spacing-xs);
  margin-top: var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--warning-color);
  padding: var(--spacing-xs) var(--spacing-sm);
  background: rgba(245, 158, 11, 0.1);
  border-radius: var(--radius-md);
  border: 1px solid rgba(245, 158, 11, 0.2);

  .el-icon {
    font-size: var(--font-size-md);
  }
}

// ==============================================
// 状态切换区域
// ==============================================
.status-form-item {
  .status-wrapper {
    @include flex-between;
    align-items: center;
    padding: var(--spacing-lg);
    background: var(--background-secondary);
    border-radius: var(--radius-lg);
    border: 2px solid var(--border-light);
    transition: all var(--transition-normal) var(--transition-timing);

    &:hover {
      border-color: var(--border-medium);
    }

    .status-info {
      @include flex-start;
      gap: var(--spacing-lg);
      align-items: center;
      flex: 1;

      .status-icon {
        @include flex-center;
        width: 40px;
        height: 40px;
        border-radius: var(--radius-lg);
        font-size: var(--font-size-lg);
        transition: all var(--transition-normal) var(--transition-timing);
        background: rgba(239, 68, 68, 0.1);
        color: var(--error-color);

        &.active {
          background: rgba(16, 185, 129, 0.1);
          color: var(--success-color);
        }
      }

      .status-text {
        .status-label {
          font-size: var(--font-size-md);
          font-weight: var(--font-weight-medium);
          color: var(--text-primary);
          margin-bottom: var(--spacing-xs);
        }

        .status-desc {
          font-size: var(--font-size-sm);
          color: var(--text-secondary);
          line-height: var(--line-height-normal);
        }
      }
    }

    .enhanced-switch {
      :deep(.el-switch__core) {
        width: 50px;
        height: 26px;
        border-radius: var(--radius-xl);

        &::after {
          width: 20px;
          height: 20px;
        }
      }

      :deep(.is-checked .el-switch__core) {
        background-color: var(--success-color);
      }
    }
  }
}

// ==============================================
// 角色预览卡片
// ==============================================
.role-preview-card {
  background: var(--primary-lighter);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: var(--radius-lg);
  overflow: hidden;
  @include card-shadow;

  .preview-header {
    @include flex-start;
    gap: var(--spacing-sm);
    padding: var(--spacing-md) var(--spacing-lg);
    background: rgba(59, 130, 246, 0.1);
    border-bottom: 1px solid rgba(59, 130, 246, 0.2);
    font-size: var(--font-size-sm);
    font-weight: var(--font-weight-medium);
    color: var(--primary-color);

    .el-icon {
      font-size: var(--font-size-md);
    }
  }

  .preview-content {
    padding: var(--spacing-lg);

    .role-preview-info {
      @include flex-start;
      gap: var(--spacing-lg);
      align-items: flex-start;

      .role-preview-avatar {
        @include flex-center;
        width: 48px;
        height: 48px;
        background: var(--primary-color);
        color: var(--text-inverse);
        border-radius: var(--radius-lg);
        font-size: var(--font-size-lg);
        flex-shrink: 0;
      }

      .role-preview-details {
        flex: 1;

        .preview-name {
          @include flex-start;
          gap: var(--spacing-md);
          align-items: center;
          margin-bottom: var(--spacing-sm);
          font-size: var(--font-size-lg);
          font-weight: var(--font-weight-semibold);
          color: var(--text-primary);

          .preview-status-tag {
            font-size: var(--font-size-xs);
            padding: 2px var(--spacing-sm);
          }
        }

        .preview-desc {
          font-size: var(--font-size-sm);
          color: var(--text-secondary);
          line-height: var(--line-height-relaxed);
          @include text-clamp(2);
        }
      }
    }
  }
}

// ==============================================
// 对话框底部操作区域
// ==============================================
.dialog-footer-wrapper {
  padding: var(--spacing-xl) var(--spacing-xxxl) var(--spacing-xxxl);
  background: var(--background-secondary);
  border-top: 1px solid var(--border-light);

  .action-buttons {
    @include flex-between;
    gap: var(--spacing-lg);

    .el-button {
      @include flex-center;
      gap: var(--spacing-sm);
      padding: var(--spacing-lg) var(--spacing-xxxl);
      border-radius: var(--radius-lg);
      font-weight: var(--font-weight-medium);
      font-size: var(--font-size-md);
      transition: all var(--transition-normal) var(--transition-timing);
      min-width: 140px;

      &:hover {
        @include hover-lift;
      }
    }

    .cancel-btn {
      background: var(--background-card);
      border: 2px solid var(--border-medium);
      color: var(--text-secondary);

      &:hover {
        border-color: var(--primary-color);
        color: var(--primary-color);
        background: var(--primary-lighter);
      }
    }

    .confirm-btn {
      background: var(--gradient-primary);
      border: none;
      color: var(--text-inverse);

      &:hover {
        background: var(--primary-dark);
        box-shadow: var(--shadow-lg);
      }
    }
  }
}

// ==============================================
// 响应式设计
// ==============================================
@include responsive(md) {
  .role-dialog {
    :deep(.el-dialog) {
      width: 95vw !important;
      margin: var(--spacing-lg) auto;
    }
  }

  .dialog-header-wrapper {
    padding: var(--spacing-lg) var(--spacing-xl);

    .header-content {
      gap: var(--spacing-md);

      .header-icon {
        width: 44px;
        height: 44px;
        font-size: var(--font-size-lg);
      }

      .header-text {
        .dialog-title {
          font-size: var(--font-size-xl);
        }

        .dialog-subtitle {
          font-size: var(--font-size-sm);
        }
      }
    }
  }

  .dialog-content-wrapper {
    padding: var(--spacing-xl) var(--spacing-lg) var(--spacing-lg);
  }

  .dialog-footer-wrapper {
    padding: var(--spacing-lg);

    .action-buttons {
      flex-direction: column;

      .el-button {
        width: 100%;
        min-width: auto;
      }
    }
  }

  .status-wrapper {
    flex-direction: column;
    align-items: stretch;
    gap: var(--spacing-lg);

    .status-info {
      order: 1;
    }

    .enhanced-switch {
      order: 2;
      align-self: flex-end;
    }
  }
}

// ==============================================
// 动画效果
// ==============================================
@keyframes dialogSlideIn {
  from {
    opacity: 0;
    transform: translateY(-30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.role-dialog {
  :deep(.el-dialog) {
    animation: dialogSlideIn 0.3s var(--transition-timing);
  }
}

.form-section {
  animation: fadeInUp 0.4s var(--transition-timing) both;

  @for $i from 1 through 5 {
    &:nth-child(#{$i}) {
      animation-delay: #{$i * 0.1}s;
    }
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
