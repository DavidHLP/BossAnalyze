<template>
  <el-col :xs="24" :sm="isSidebarCollapsed ? 18 : 16" :md="isSidebarCollapsed ? 19 : 16"
    :lg="isSidebarCollapsed ? 20 : 16" class="content-col">
    <el-card shadow="hover" class="detail-card">
      <template #header>
        <div class="card-header">
          <h3 class="card-title">菜单详情</h3>
        </div>
      </template>

      <div v-if="currentNode" class="details-container">
        <el-descriptions :column="descriptionColumns" border class="details-info">
          <el-descriptions-item label="菜单名称">{{ currentNode.name }}</el-descriptions-item>
          <el-descriptions-item label="路由路径">{{ currentNode.path }}</el-descriptions-item>
          <el-descriptions-item label="组件路径">{{ currentNode.meta?.component || '-' }}</el-descriptions-item>
          <el-descriptions-item label="权限标识">{{ currentNode.permission || '-' }}</el-descriptions-item>
          <el-descriptions-item label="图标">
            <span class="icon-preview">
              <el-icon v-if="currentNode.meta?.metaIcon">
                <component :is="currentNode.meta.metaIcon" />
              </el-icon>
              {{ currentNode.meta?.metaIcon || '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="排序">{{ currentNode.menuOrder }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            {{ { M: '目录', C: '菜单', F: '按钮' }[currentNode.meta?.type as 'M' | 'C' | 'F'] || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentNode.status === 1 ? 'success' : 'danger'" class="status-tag">
              {{ currentNode.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总是显示" :span="1">
            <el-tag size="small" :type="currentNode.meta?.alwaysShow ? 'success' : 'info'" class="feature-tag">
              {{ currentNode.meta?.alwaysShow ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="隐藏菜单" :span="1">
            <el-tag size="small" :type="currentNode.meta?.metaHidden ? 'warning' : 'info'" class="feature-tag">
              {{ currentNode.meta?.metaHidden ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="保持活跃">
            <el-tag size="small" :type="currentNode.meta?.metaKeepAlive ? 'success' : 'info'" class="feature-tag">
              {{ currentNode.meta?.metaKeepAlive ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="访问角色">
            {{ currentNode.meta?.metaRoles || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="descriptionColumns">
            {{ currentNode.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="action-group">
          <el-button type="primary" @click="handleEdit(currentNode)" class="detail-button">
            <el-icon>
              <Edit />
            </el-icon>
            <span>编辑菜单</span>
          </el-button>
          <el-button v-if="currentNode.meta?.type !== 'F'" type="success" @click="handleAddChild(currentNode)"
            class="detail-button">
            <el-icon>
              <Plus />
            </el-icon>
            <span>添加子菜单</span>
          </el-button>
          <el-button type="danger" @click="handleDelete(currentNode)" class="detail-button">
            <el-icon>
              <Delete />
            </el-icon>
            <span>删除菜单</span>
          </el-button>
        </div>
      </div>

      <div v-else class="empty-state">
        <el-empty description="请选择一个菜单项查看详情" />
      </div>
    </el-card>
  </el-col>
</template>

<script setup lang="ts" name="RightView">
import type { Router } from '@/router/index.d'

// 定义组件接收的属性
defineProps({
  currentNode: {
    type: Object as () => Router | null,
    default: null
  },
  isSidebarCollapsed: {
    type: Boolean,
    default: false
  },
  descriptionColumns: {
    type: Number,
    default: 2
  }
})

// 定义组件发出的事件
const emit = defineEmits(['edit', 'addChild', 'delete'])

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
.content-col {
  transition: all 0.3s ease;
}

.detail-card {
  height: 100%;

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .card-title {
      margin: 0;
      font-size: 16px;
      color: #303133;
    }
  }

  .details-container {
    height: 100%;
    overflow: auto;
    padding: 8px 0;

    .details-info {
      margin-bottom: 24px;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);

      :deep(.el-descriptions__cell) {
        padding: 12px 16px;
      }

      :deep(.el-descriptions__label) {
        color: #606266;
        font-weight: 500;
        background-color: #fafafa;
      }

      .status-tag {
        font-weight: normal;
        padding: 2px 10px;
        border-radius: 12px;
      }

      .feature-tag {
        padding: 0 8px;
        border-radius: 10px;
      }

      .icon-preview {
        display: flex;
        align-items: center;

        .el-icon {
          margin-right: 8px;
          font-size: 16px;
        }
      }
    }

    .action-group {
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      margin-top: 16px;

      .detail-button {
        min-width: 120px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 6px;
        transition: all 0.3s ease;

        .el-icon {
          margin-right: 4px;
        }

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }
      }
    }
  }

  .empty-state {
    height: 80%;
    display: flex;
    align-items: center;
    justify-content: center;

    .el-empty {
      padding: 40px 0;
    }
  }
}

@media (max-width: 768px) {
  .content-col {
    margin-top: 16px;
  }

  .action-group {
    flex-direction: column;

    .detail-button {
      width: 100% !important;
    }
  }
}
</style>
