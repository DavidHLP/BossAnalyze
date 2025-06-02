<template>
  <el-dialog
    :model-value="visible"
    title="权限设置"
    width="720px"
    :before-close="handleClosePermDialog"
    destroy-on-close
    top="5vh"
    class="custom-dialog"
  >
    <div v-if="props.currentRole?.roleName" class="role-tag-container">
      <span>当前角色: </span>
      <span class="role-tag">{{ props.currentRole?.roleName }}</span>
    </div>

    <div class="menu-tree-container">
      <SelectTree
        ref="menuTreeRef"
        :tree-data="props.routersTree"
        :default-checked-keys="props.defaultCheckedMenus"
        :check-strictly="true"
        :default-expand-all="false"
        :expand-on-click-node="false"
        @update:checked-keys="handleCheckedKeysChange"
        @check-change="handleCheckChange"
      />
    </div>

    <template #footer>
      <div class="dialog-footer-between">
        <div>
          <el-popconfirm
            title="检测到数据已修改，确定要关闭吗？"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="closeDialog"
            v-if="checkForUnsavedChanges()"
          >
            <template #reference>
              <el-button>取消</el-button>
            </template>
          </el-popconfirm>
          <el-button v-else @click="closeDialog">取消</el-button>
        </div>
        <div>
          <el-button @click="resetPermissions">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
          <el-button type="primary" @click="savePermissions">
            <el-icon><Check /></el-icon>
            保存
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import { Refresh, Check } from '@element-plus/icons-vue';
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

const handleCheckedKeysChange = (keys: number[]) => {
  emits('update:checkedMenus', keys);
};
const handleCheckChange = () => {};

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
  // 父组件传递方法或数据进行判断
  // 这里直接返回false，实际可通过props传递判断方法
  return false;
};
</script>

<style scoped lang="scss">
.custom-dialog {
  border-radius: 10px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.10);
  padding-bottom: 0;
  .el-dialog__body {
    padding: 18px 20px 0 20px !important;
  }
  .el-dialog__footer {
    padding: 10px 20px 16px 20px !important;
  }
}

.role-tag-container {
  margin-bottom: 6px;
  .role-tag {
    background: #f4f6fa;
    color: #409eff;
    border-radius: 8px;
    padding: 2px 10px;
    font-weight: 500;
    margin-left: 4px;
  }
}

.menu-tree-container {
  margin-bottom: 12px;
  .el-tree {
    background: transparent;
    .el-tree-node__content {
      border-radius: 6px;
      transition: background 0.2s;
      padding: 2px 0 2px 4px;
      min-height: 28px;
      &:hover {
        background: #f4f8ff;
      }
    }
    .el-tree-node.is-checked > .el-tree-node__content {
      background: #e6f7ff;
    }
  }
}

.dialog-footer-between {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  padding: 0;
  > div {
    display: flex;
    gap: 8px;
  }
}

.el-button {
  border-radius: 6px;
  font-size: 15px;
  min-width: 68px;
  padding: 6px 16px;
}
</style>
