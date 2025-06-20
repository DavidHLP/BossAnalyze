<template>
  <el-dialog
    :model-value="visible"
    title=""
    width="800px"
    :before-close="handleClosePermDialog"
    destroy-on-close
    top="5vh"
    class="permissions-dialog"
    append-to-body
  >
    <!-- 自定义对话框头部 -->
    <template #header>
      <div class="dialog-header">
        <div class="header-content">
          <div class="header-icon">
            <el-icon><Setting /></el-icon>
          </div>
          <div class="header-text">
            <h3 class="dialog-title">权限设置</h3>
            <p class="dialog-subtitle">为角色配置系统权限</p>
          </div>
        </div>
        <el-button
          type="text"
          @click="closeDialog"
          class="close-btn"
          size="large"
        >
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </template>

    <!-- 对话框内容 -->
    <div class="dialog-content">
      <!-- 角色信息卡片 -->
      <div class="role-info-card" v-if="props.currentRole?.roleName">
        <div class="role-avatar">
          <el-icon class="avatar-icon"><User /></el-icon>
        </div>
        <div class="role-details">
          <h4 class="role-name">{{ props.currentRole?.roleName }}</h4>
          <p class="role-desc">当前正在为此角色配置权限</p>
        </div>
        <div class="role-stats">
          <div class="stat-item">
            <span class="stat-number">{{ getCurrentPermissionCount() }}</span>
            <span class="stat-label">已配置权限</span>
          </div>
        </div>
      </div>

      <!-- 权限配置标签页 -->
      <div class="permissions-tabs">
        <el-tabs v-model="activeTab" type="border-card" class="custom-tabs">
          <el-tab-pane label="菜单权限" name="menu">
            <div class="tab-content">
              <div class="section-header">
                <div class="section-info">
                  <el-icon class="section-icon"><Menu /></el-icon>
                  <div>
                    <h5 class="section-title">菜单访问权限</h5>
                    <p class="section-desc">配置角色可以访问的系统菜单和页面</p>
                  </div>
                </div>
                <div class="section-actions">
                  <el-button size="small" @click="expandAllMenus" class="action-btn">
                    <el-icon><Plus /></el-icon>
                    展开全部
                  </el-button>
                  <el-button size="small" @click="collapseAllMenus" class="action-btn">
                    <el-icon><Minus /></el-icon>
                    收起全部
                  </el-button>
                </div>
              </div>

              <div class="tree-container">
                <SelectTree
                  ref="menuTreeRef"
                  :tree-data="props.routersTree"
                  :default-checked-keys="props.defaultCheckedMenus"
                  :check-strictly="true"
                  :default-expand-all="false"
                  :expand-on-click-node="false"
                  :show-header="false"
                  :show-search="true"
                  :show-footer="false"
                  header-title="菜单权限"
                  @update:checked-keys="handleCheckedKeysChange"
                  @check-change="handleCheckChange"
                />
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="操作权限" name="operation">
            <div class="tab-content">
              <div class="section-header">
                <div class="section-info">
                  <el-icon class="section-icon"><Tools /></el-icon>
                  <div>
                    <h5 class="section-title">操作权限配置</h5>
                    <p class="section-desc">配置角色在各页面的具体操作权限</p>
                  </div>
                </div>
              </div>

              <div class="operation-grid">
                <div class="operation-card" v-for="operation in operationPermissions" :key="operation.id">
                  <div class="card-header">
                    <el-icon :class="operation.icon"></el-icon>
                    <span class="card-title">{{ operation.name }}</span>
                  </div>
                  <div class="card-content">
                    <el-checkbox-group v-model="operation.selected">
                      <el-checkbox
                        v-for="perm in operation.permissions"
                        :key="perm.value"
                        :value="perm.value"
                        class="permission-checkbox"
                      >
                        {{ perm.label }}
                      </el-checkbox>
                    </el-checkbox-group>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="数据权限" name="data">
            <div class="tab-content">
              <div class="section-header">
                <div class="section-info">
                  <el-icon class="section-icon"><DataAnalysis /></el-icon>
                  <div>
                    <h5 class="section-title">数据访问权限</h5>
                    <p class="section-desc">配置角色可以访问的数据范围</p>
                  </div>
                </div>
              </div>

              <div class="data-permission-grid">
                <div class="permission-group" v-for="group in dataPermissions" :key="group.id">
                  <div class="group-header">
                    <h6 class="group-title">{{ group.name }}</h6>
                    <p class="group-desc">{{ group.description }}</p>
                  </div>
                  <div class="group-content">
                    <el-radio-group v-model="group.selected" class="permission-radios">
                      <el-radio
                        v-for="option in group.options"
                        :key="option.value"
                        :value="option.value"
                        class="permission-radio"
                      >
                        <div class="radio-content">
                          <span class="radio-label">{{ option.label }}</span>
                          <span class="radio-desc">{{ option.description }}</span>
                        </div>
                      </el-radio>
                    </el-radio-group>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 对话框底部 -->
    <template #footer>
      <div class="dialog-footer">
        <div class="footer-left">
          <el-button @click="showPermissionPreview" class="preview-btn">
            <el-icon><View /></el-icon>
            预览权限
          </el-button>
        </div>
        <div class="footer-right">
          <el-popconfirm
            title="检测到数据已修改，确定要关闭吗？"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="closeDialog"
            v-if="checkForUnsavedChanges()"
          >
            <template #reference>
              <el-button class="cancel-btn">取消</el-button>
            </template>
          </el-popconfirm>
          <el-button v-else @click="closeDialog" class="cancel-btn">取消</el-button>

          <el-button @click="resetPermissions" class="reset-btn">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
          <el-button type="primary" @click="savePermissions" class="save-btn" :loading="props.loading">
            <el-icon><Check /></el-icon>
            保存权限
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>

  <!-- 权限预览对话框 -->
  <el-dialog
    v-model="previewVisible"
    title="权限预览"
    width="600px"
    class="preview-dialog"
  >
    <div class="preview-content">
      <div class="preview-section">
        <h6>菜单权限</h6>
        <el-tag v-for="menu in getSelectedMenus()" :key="menu" size="small" class="permission-tag">
          {{ menu }}
        </el-tag>
      </div>
      <div class="preview-section">
        <h6>操作权限</h6>
        <el-tag v-for="op in getSelectedOperations()" :key="op" size="small" type="success" class="permission-tag">
          {{ op }}
        </el-tag>
      </div>
      <div class="preview-section">
        <h6>数据权限</h6>
        <el-tag v-for="data in getSelectedDataPermissions()" :key="data" size="small" type="warning" class="permission-tag">
          {{ data }}
        </el-tag>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessageBox } from 'element-plus';
import {
  Setting, Close, User, Menu, Tools, DataAnalysis,
  Plus, Minus, View, Refresh, Check
} from '@element-plus/icons-vue';
import SelectTree from '@/components/common/SelectTree.vue';
import type { Role } from '@/api/auth/auth.d';

interface TreeNode {
  id: number;
  name: string;
  label?: string;
  children?: TreeNode[];
}

const props = defineProps<{
  visible: boolean,
  currentRole?: Role,
  routersTree: TreeNode[],
  defaultCheckedMenus: number[],
  loading: boolean
}>();

const emits = defineEmits(['update:visible', 'save', 'reset', 'close', 'update:checkedMenus']);

const menuTreeRef = ref();
const activeTab = ref('menu');
const previewVisible = ref(false);

// 操作权限数据
const operationPermissions = reactive([
  {
    id: 1,
    name: '用户管理',
    icon: 'User',
    selected: [],
    permissions: [
      { label: '查看用户', value: 'user:view' },
      { label: '新增用户', value: 'user:create' },
      { label: '编辑用户', value: 'user:edit' },
      { label: '删除用户', value: 'user:delete' },
      { label: '重置密码', value: 'user:reset' }
    ]
  },
  {
    id: 2,
    name: '角色管理',
    icon: 'UserFilled',
    selected: [],
    permissions: [
      { label: '查看角色', value: 'role:view' },
      { label: '新增角色', value: 'role:create' },
      { label: '编辑角色', value: 'role:edit' },
      { label: '删除角色', value: 'role:delete' },
      { label: '分配权限', value: 'role:assign' }
    ]
  },
  {
    id: 3,
    name: '系统设置',
    icon: 'Setting',
    selected: [],
    permissions: [
      { label: '查看设置', value: 'system:view' },
      { label: '修改设置', value: 'system:edit' },
      { label: '系统监控', value: 'system:monitor' },
      { label: '日志管理', value: 'system:log' }
    ]
  }
]);

// 数据权限配置
const dataPermissions = reactive([
  {
    id: 1,
    name: '数据范围',
    description: '配置角色可以访问的数据范围',
    selected: 'all',
    options: [
      { label: '全部数据', value: 'all', description: '可以访问系统中的所有数据' },
      { label: '部门数据', value: 'dept', description: '只能访问本部门及下级部门的数据' },
      { label: '个人数据', value: 'self', description: '只能访问个人创建的数据' }
    ]
  },
  {
    id: 2,
    name: '文件访问',
    description: '配置角色的文件访问权限',
    selected: 'limited',
    options: [
      { label: '完全访问', value: 'full', description: '可以访问所有文件资源' },
      { label: '受限访问', value: 'limited', description: '只能访问指定目录的文件' },
      { label: '只读访问', value: 'readonly', description: '只能查看文件，不能修改' }
    ]
  }
]);

// 展开所有菜单
const expandAllMenus = () => {
  menuTreeRef.value?.expandAll();
};

// 收起所有菜单
const collapseAllMenus = () => {
  menuTreeRef.value?.collapseAll();
};

// 获取当前权限数量
const getCurrentPermissionCount = () => {
  const menuCount = menuTreeRef.value?.getCheckedKeys()?.length || 0;
  const operationCount = operationPermissions.reduce((sum, op) => sum + op.selected.length, 0);
  const dataCount = dataPermissions.filter(d => d.selected).length;
  return menuCount + operationCount + dataCount;
};

// 显示权限预览
const showPermissionPreview = () => {
  previewVisible.value = true;
};

// 获取选中的菜单
const getSelectedMenus = () => {
  // 这里应该根据实际的树数据返回选中的菜单名称
  return ['用户管理', '角色管理', '系统设置'];
};

// 获取选中的操作权限
const getSelectedOperations = () => {
  const selected: string[] = [];
  operationPermissions.forEach(op => {
    op.selected.forEach(s => {
      const perm = op.permissions.find(p => p.value === s);
      if (perm) selected.push(`${op.name} - ${perm.label}`);
    });
  });
  return selected;
};

// 获取选中的数据权限
const getSelectedDataPermissions = () => {
  return dataPermissions.map(d => {
    const option = d.options.find(o => o.value === d.selected);
    return option ? `${d.name}: ${option.label}` : '';
  }).filter(Boolean);
};

const handleCheckedKeysChange = (keys: number[]) => {
  emits('update:checkedMenus', keys);
};

const handleCheckChange = (data: any, checked: boolean) => {
  const checkedKeys = menuTreeRef.value?.getCheckedKeys() || [];
  emits('update:checkedMenus', checkedKeys);
};

const closeDialog = () => {
  emits('update:visible', false);
  emits('close');
};

const handleClosePermDialog = (done?: () => void) => {
  if (checkForUnsavedChanges()) {
    ElMessageBox.confirm('关闭对话框将丢失未保存的修改，确定要关闭吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      closeDialog();
      if (done) done();
    }).catch(() => {});
  } else {
    closeDialog();
    if (done) done();
  }
};

const resetPermissions = () => {
  emits('reset');
};

const savePermissions = () => {
  emits('save');
};

const checkForUnsavedChanges = () => {
  return false;
};
</script>

<style scoped lang="scss">
@use '@/assets/style/variables.scss' as *;

.permissions-dialog {
  :deep(.el-dialog) {
    border-radius: var(--radius-xl);
    @include card-shadow;
    overflow: hidden;
  }

  :deep(.el-dialog__header) {
    padding: 0;
    margin: 0;
  }

  :deep(.el-dialog__body) {
    padding: 0;
  }

  :deep(.el-dialog__footer) {
    padding: 0;
    margin: 0;
    border-top: 1px solid var(--border-light);
  }
}

// 对话框头部
.dialog-header {
  @include flex-between;
  padding: var(--spacing-xl) var(--spacing-xxxl);
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  color: var(--text-inverse);

  .header-content {
    @include flex-start;
    gap: var(--spacing-lg);

    .header-icon {
      @include flex-center;
      width: 48px;
      height: 48px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: var(--radius-lg);
      font-size: var(--font-size-xl);
    }

    .header-text {
      .dialog-title {
        margin: 0 0 var(--spacing-xs) 0;
        font-size: var(--font-size-xxl);
        font-weight: var(--font-weight-bold);
        line-height: var(--line-height-tight);
      }

      .dialog-subtitle {
        margin: 0;
        font-size: var(--font-size-md);
        opacity: 0.9;
        font-weight: var(--font-weight-normal);
      }
    }
  }

  .close-btn {
    width: 36px;
    height: 36px;
    border: 1px solid rgba(255, 255, 255, 0.3);
    background: rgba(255, 255, 255, 0.1);
    color: var(--text-inverse);
    border-radius: var(--radius-md);
    transition: all var(--transition-normal) var(--transition-timing);

    &:hover {
      background: rgba(255, 255, 255, 0.2);
      @include hover-lift;
    }
  }
}

// 对话框内容
.dialog-content {
  padding: var(--spacing-xl) var(--spacing-xxxl) var(--spacing-lg);
  min-height: 500px;
  max-height: 70vh;
  overflow-y: auto;
}

// 角色信息卡片
.role-info-card {
  @include flex-start;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  background: var(--primary-lighter);
  border-radius: var(--radius-lg);
  border: 1px solid var(--primary-color);
  margin-bottom: var(--spacing-xl);

  .role-avatar {
    @include flex-center;
    width: 60px;
    height: 60px;
    background: var(--primary-color);
    color: var(--text-inverse);
    border-radius: var(--radius-xl);
    font-size: var(--font-size-xl);
  }

  .role-details {
    flex: 1;

    .role-name {
      margin: 0 0 var(--spacing-xs) 0;
      font-size: var(--font-size-lg);
      font-weight: var(--font-weight-semibold);
      color: var(--text-primary);
    }

    .role-desc {
      margin: 0;
      font-size: var(--font-size-sm);
      color: var(--text-secondary);
    }
  }

  .role-stats {
    text-align: center;

    .stat-item {
      .stat-number {
        display: block;
        font-size: var(--font-size-xl);
        font-weight: var(--font-weight-bold);
        color: var(--primary-color);
        margin-bottom: var(--spacing-xs);
      }

      .stat-label {
        font-size: var(--font-size-xs);
        color: var(--text-secondary);
      }
    }
  }
}

// 权限配置标签页
.permissions-tabs {
  .custom-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: var(--spacing-lg);
      border-radius: var(--radius-lg);
      overflow: hidden;
      background: var(--background-secondary);
    }

    :deep(.el-tabs__item) {
      padding: var(--spacing-lg) var(--spacing-xl);
      font-size: var(--font-size-md);
      font-weight: var(--font-weight-medium);
      color: var(--text-secondary);
      transition: all var(--transition-normal) var(--transition-timing);
      border: none;

      &:hover {
        color: var(--primary-color);
        background: var(--primary-lighter);
      }

      &.is-active {
        color: var(--primary-color);
        background: var(--background-card);
        font-weight: var(--font-weight-semibold);
      }
    }

    :deep(.el-tabs__content) {
      padding: var(--spacing-lg);
      background: var(--background-card);
      border-radius: var(--radius-lg);
      border: 1px solid var(--border-light);
    }
  }

  .tab-content {
    animation: fadeInUp 0.3s var(--transition-timing);
  }
}

// 区块头部
.section-header {
  @include flex-between;
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-light);

  .section-info {
    @include flex-start;
    gap: var(--spacing-md);

    .section-icon {
      @include flex-center;
      width: 40px;
      height: 40px;
      background: var(--primary-lighter);
      color: var(--primary-color);
      border-radius: var(--radius-lg);
      font-size: var(--font-size-lg);
    }

    .section-title {
      margin: 0 0 var(--spacing-xs) 0;
      font-size: var(--font-size-lg);
      font-weight: var(--font-weight-semibold);
      color: var(--text-primary);
    }

    .section-desc {
      margin: 0;
      font-size: var(--font-size-sm);
      color: var(--text-secondary);
    }
  }

  .section-actions {
    @include flex-start;
    gap: var(--spacing-sm);

    .action-btn {
      @include flex-center;
      gap: var(--spacing-xs);
      border-radius: var(--radius-md);
      transition: all var(--transition-normal) var(--transition-timing);

      &:hover {
        @include hover-lift;
        background: var(--primary-color);
        color: var(--text-inverse);
        border-color: var(--primary-color);
      }
    }
  }
}

// 树容器
.tree-container {
  margin-top: var(--spacing-lg);
}

// 操作权限网格
.operation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--spacing-lg);

  .operation-card {
    background: var(--background-card);
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-light);
    @include card-shadow;
    overflow: hidden;
    transition: all var(--transition-normal) var(--transition-timing);

    &:hover {
      @include hover-lift;
      border-color: var(--primary-color);
    }

    .card-header {
      @include flex-start;
      gap: var(--spacing-md);
      padding: var(--spacing-lg);
      background: var(--background-secondary);
      border-bottom: 1px solid var(--border-light);

      .el-icon {
        @include flex-center;
        width: 32px;
        height: 32px;
        background: var(--primary-color);
        color: var(--text-inverse);
        border-radius: var(--radius-md);
        font-size: var(--font-size-md);
      }

      .card-title {
        font-size: var(--font-size-md);
        font-weight: var(--font-weight-semibold);
        color: var(--text-primary);
      }
    }

    .card-content {
      padding: var(--spacing-lg);

      .permission-checkbox {
        display: block;
        margin-bottom: var(--spacing-md);
        padding: var(--spacing-sm);
        border-radius: var(--radius-md);
        transition: all var(--transition-normal) var(--transition-timing);

        &:hover {
          background: var(--primary-lighter);
        }

        :deep(.el-checkbox__label) {
          font-weight: var(--font-weight-medium);
          color: var(--text-primary);
        }
      }
    }
  }
}

// 数据权限网格
.data-permission-grid {
  display: grid;
  gap: var(--spacing-xl);

  .permission-group {
    background: var(--background-card);
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-light);
    @include card-shadow;
    overflow: hidden;

    .group-header {
      padding: var(--spacing-lg);
      background: var(--background-secondary);
      border-bottom: 1px solid var(--border-light);

      .group-title {
        margin: 0 0 var(--spacing-xs) 0;
        font-size: var(--font-size-lg);
        font-weight: var(--font-weight-semibold);
        color: var(--text-primary);
      }

      .group-desc {
        margin: 0;
        font-size: var(--font-size-sm);
        color: var(--text-secondary);
      }
    }

    .group-content {
      padding: var(--spacing-lg);

      .permission-radios {
        display: grid;
        gap: var(--spacing-md);

        .permission-radio {
          padding: var(--spacing-md);
          background: var(--background-secondary);
          border-radius: var(--radius-md);
          border: 1px solid var(--border-light);
          transition: all var(--transition-normal) var(--transition-timing);

          &:hover {
            border-color: var(--primary-color);
            background: var(--primary-lighter);
          }

          .radio-content {
            .radio-label {
              display: block;
              font-weight: var(--font-weight-medium);
              color: var(--text-primary);
              margin-bottom: var(--spacing-xs);
            }

            .radio-desc {
              font-size: var(--font-size-sm);
              color: var(--text-secondary);
            }
          }
        }
      }
    }
  }
}

// 对话框底部
.dialog-footer {
  @include flex-between;
  padding: var(--spacing-lg) var(--spacing-xxxl);
  background: var(--background-secondary);

  .footer-left {
    .preview-btn {
      @include flex-center;
      gap: var(--spacing-xs);
      color: var(--text-secondary);
      border-color: var(--border-medium);

      &:hover {
        color: var(--primary-color);
        border-color: var(--primary-color);
        background: var(--primary-lighter);
      }
    }
  }

  .footer-right {
    @include flex-start;
    gap: var(--spacing-md);

    .cancel-btn {
      border-color: var(--border-medium);
      color: var(--text-secondary);

      &:hover {
        border-color: var(--error-color);
        color: var(--error-color);
        background: rgba(239, 68, 68, 0.1);
      }
    }

    .reset-btn {
      @include flex-center;
      gap: var(--spacing-xs);
      color: var(--warning-color);
      border-color: var(--warning-color);

      &:hover {
        background: rgba(245, 158, 11, 0.1);
        @include hover-lift;
      }
    }

    .save-btn {
      @include flex-center;
      gap: var(--spacing-xs);
      background: var(--gradient-primary);
      border: none;
      font-weight: var(--font-weight-semibold);
      padding: var(--spacing-sm) var(--spacing-xl);

      &:hover {
        background: var(--primary-dark);
        @include hover-lift;
      }
    }
  }
}

// 权限预览对话框
.preview-dialog {
  .preview-content {
    .preview-section {
      margin-bottom: var(--spacing-lg);

      h6 {
        margin: 0 0 var(--spacing-md) 0;
        font-size: var(--font-size-md);
        font-weight: var(--font-weight-semibold);
        color: var(--text-primary);
      }

      .permission-tag {
        margin: 0 var(--spacing-xs) var(--spacing-xs) 0;
      }
    }
  }
}

// 响应式设计
@include responsive(lg) {
  .dialog-header {
    padding: var(--spacing-lg) var(--spacing-xl);

    .header-content {
      gap: var(--spacing-md);

      .header-icon {
        width: 40px;
        height: 40px;
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

  .dialog-content {
    padding: var(--spacing-lg);
  }

  .operation-grid {
    grid-template-columns: 1fr;
  }
}

@include responsive(md) {
  .permissions-dialog {
    :deep(.el-dialog) {
      width: 95vw !important;
      margin: var(--spacing-lg) auto;
    }
  }

  .role-info-card {
    flex-direction: column;
    text-align: center;
  }

  .section-header {
    flex-direction: column;
    gap: var(--spacing-md);
    align-items: flex-start;
  }

  .dialog-footer {
    flex-direction: column;
    gap: var(--spacing-md);

    .footer-right {
      width: 100%;
      justify-content: space-between;
    }
  }
}

// 动画定义
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