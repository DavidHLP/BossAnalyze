<template>
  <el-dialog :model-value="dialogVisible" @update:model-value="updateDialog" width="720px" destroy-on-close top="5vh" class="custom-dialog">
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
.guide-content {
  padding: 32px 36px 24px 36px;
  font-size: 16px;
  color: #222;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 4px 24px 0 rgba(60, 80, 120, 0.08);
  transition: box-shadow 0.2s;
  max-width: 680px;
  margin: 0 auto;
}

.custom-divider {
  margin: 32px 0 18px 0;
  .el-divider__text {
    font-weight: 700;
    font-size: 20px;
    color: #409eff;
    letter-spacing: 1px;
  }
  border-color: #e3e8ee;
}

.permission-type {
  margin-bottom: 22px;
  .permission-item {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
    .perm-tag {
      margin-right: 12px;
      border-radius: 12px;
      font-size: 14px;
      padding: 3px 16px;
      background: linear-gradient(90deg, #e0f7fa 0%, #e8f5e9 100%);
      color: #26a69a;
      border: none;
      font-weight: 600;
      box-shadow: 0 1px 4px #0001;
    }
    span {
      color: #555;
      font-size: 15px;
    }
  }
}

.custom-alert {
  margin: 22px 0 28px 0;
  border-radius: 12px;
  box-shadow: 0 2px 12px #0001;
  background: linear-gradient(90deg, #f4f8ff 60%, #f8fafc 100%);
  border: none;
  p {
    font-weight: 600;
    color: #409eff;
    margin-bottom: 10px;
    font-size: 15px;
  }
  ol {
    margin-left: 22px;
    color: #666;
    font-size: 14px;
    li {
      margin-bottom: 5px;
      line-height: 1.7;
    }
  }
}

.custom-table {
  margin-top: 12px;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 12px #0001;
  .el-table__header th {
    background: #f6f8fa;
    font-weight: 700;
    color: #222;
    font-size: 16px;
    letter-spacing: 0.5px;
    border-bottom: 1.5px solid #e3e8ee;
  }
  .el-table__body td {
    font-size: 15px;
    color: #444;
    background: #fff;
    transition: background 0.2s;
  }
  .el-table__body tr:hover > td {
    background: #f0f7ff;
  }
  .status-tag {
    border-radius: 10px;
    font-size: 14px;
    padding: 3px 14px;
    font-weight: 600;
    letter-spacing: 1px;
    background: #ffeaea;
    color: #f56c6c;
    border: none;
    &.el-tag--success {
      background: #e8f5e9;
      color: #67c23a;
    }
  }
}

/* 响应式适配 */
@media (max-width: 800px) {
  .guide-content {
    padding: 16px 6vw 12px 6vw;
    font-size: 15px;
    max-width: 98vw;
  }
  .custom-divider .el-divider__text {
    font-size: 17px;
  }
  .custom-table .el-table__header th,
  .custom-table .el-table__body td {
    font-size: 14px;
  }
}
</style>
