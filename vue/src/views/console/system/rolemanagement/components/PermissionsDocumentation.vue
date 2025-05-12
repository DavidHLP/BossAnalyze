<template>
  <el-dialog :model-value="dialogVisible" @update:model-value="updateDialog" title="权限说明文档" width="720px" destroy-on-close top="5vh" class="custom-dialog">
    <div class="guide-content">
      <el-divider content-position="left" class="custom-divider">
        <span>系统权限说明</span>
      </el-divider>

      <p>本系统的权限管理以菜单为基础，每个菜单项关联相应的功能权限：</p>
      <div class="permission-type">
        <div class="permission-item">
          <el-tag type="success" effect="plain" class="perm-tag">菜单权限</el-tag>
          <span>控制用户可以访问哪些菜单和页面，同时自动关联相应的操作权限</span>
        </div>
      </div>

      <el-alert type="info" :closable="false" class="custom-alert">
        <p>当您为角色分配菜单权限时，系统会自动处理以下内容：</p>
        <ol>
          <li>授予该菜单的访问权限</li>
          <li>授予该菜单下所有相关功能的操作权限</li>
          <li>自动处理权限的父子级关系</li>
        </ol>
      </el-alert>

      <el-divider content-position="left" class="custom-divider">
        <span>系统内置角色</span>
      </el-divider>

      <el-table :data="builtInRoles" border class="custom-table">
        <el-table-column prop="name" label="角色名称" width="160" />
        <el-table-column prop="desc" label="描述" />
        <el-table-column prop="editable" label="可编辑性" width="120" align="center">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.editable ? 'success' : 'danger'" class="status-tag">
              {{ scope.row.editable ? '可编辑' : '不可编辑' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';

defineProps<{
  dialogVisible: boolean
}>();

const emit = defineEmits(['update:dialogVisible']);

// 更新对话框状态
const updateDialog = (value: boolean) => {
  emit('update:dialogVisible', value);
};

const builtInRoles = ref([
  { name: 'ADMIN', desc: '系统管理员，拥有所有权限', editable: false },
  { name: 'USER', desc: '普通用户，拥有基础浏览权限', editable: false }
]);
</script>

<style scoped lang="scss">
.custom-dialog {
  :deep(.el-dialog__header) {
    border-bottom: 1px solid #f0f0f0;
    padding-bottom: 16px;
    margin-bottom: 8px;
  }

  :deep(.el-dialog__title) {
    font-weight: 600;
    color: var(--primary-dark, #344767);
  }

  :deep(.el-dialog__body) {
    padding: 24px;
  }
}

.custom-divider {
  margin: 24px 0 16px;

  :deep(.el-divider__text) {
    font-weight: 600;
    color: var(--primary-dark, #344767);
  }
}

.permission-type {
  margin: 16px 0;
}

.permission-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.perm-tag {
  font-weight: 500;
}

.guide-content {
  padding: 0 4px;

  p {
    line-height: 1.6;
    color: var(--text-secondary, #67748e);
  }
}

.custom-table {
  margin-top: 16px;
  border-collapse: separate;
  border-spacing: 0;

  :deep(th) {
    background-color: #f7f9fc;
    color: #344767;
    font-weight: 600;
    padding: 12px 0;
  }

  :deep(td) {
    padding: 12px 0;
  }

  :deep(tr) {
    line-height: 2.5rem;
    transition: background-color 0.3s ease;

    &:hover {
      background-color: #E0F2FE !important;
    }
  }
}

.status-tag {
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
}

.custom-alert {
  background-color: #f0f9ff;
  border-color: #bae6fd;

  :deep(.el-alert__content) {
    padding: 8px 0;
  }

  p {
    margin: 6px 0;
    line-height: 1.5;
    color: #0c4a6e;
  }

  ol {
    margin: 8px 0;
    padding-left: 24px;

    li {
      margin-bottom: 4px;
      color: #0c4a6e;
    }
  }
}
</style>
