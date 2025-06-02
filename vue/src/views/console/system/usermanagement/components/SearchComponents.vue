<template>
  <el-form :model="localSearchForm" class="search-form">
    <div class="form-row">
      <div class="form-fields">
        <el-form-item label="姓名">
          <el-input v-model="localSearchForm.name" placeholder="请输入姓名" clearable class="rounded-input" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="localSearchForm.status" placeholder="请选择状态" clearable class="rounded-input"
            popper-class="status-select-dropdown">
            <el-option label="启用" :value="1">
              <template #default>
                <div class="status-option">
                  <span class="status-dot"></span>
                  <span>启用</span>
                </div>
              </template>
            </el-option>
            <el-option label="禁用" :value="0">
              <template #default>
                <div class="status-option">
                  <span class="status-dot"></span>
                  <span>禁用</span>
                </div>
              </template>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="localSearchForm.roleId" filterable remote reserve-keyword placeholder="请输入角色名称"
            :remote-method="remoteRoleSearch" :loading="roleLoading" clearable class="rounded-input">
            <el-option v-for="role in roleOptions" :key="role.value" :label="role.label" :value="role.value">
              <template #default>
                <div class="status-option">
                  <span class="status-dot"></span>
                  <span>{{ role.label }}</span>
                </div>
              </template>
            </el-option>
          </el-select>
        </el-form-item>
      </div>
      <div class="search-actions">
        <el-button type="primary" @click="emitSearch" round>搜索</el-button>
        <el-button @click="emitReset" round>重置</el-button>
        <el-button type="primary" icon="Plus" class="add-user-btn" @click="$emit('add-user')" round>+ 添加用户</el-button>
      </div>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { defineProps, defineEmits, reactive, watch } from 'vue';

interface RoleOption {
  value: string | number;
  label: string;
}

const props = defineProps({
  searchForm: {
    type: Object,
    required: true
  },
  roleOptions: {
    type: Array as () => RoleOption[],
    required: true
  },
  roleLoading: {
    type: Boolean,
    default: false
  },
  remoteRoleSearch: {
    type: Function,
    required: true
  }
});

const emit = defineEmits(['search', 'reset', 'add-user', 'update:searchForm']);

const localSearchForm = reactive({ ...props.searchForm });

watch(() => props.searchForm, (val) => {
  Object.assign(localSearchForm, val);
}, { deep: true });

// 监听本地表单变化，同步到父组件
watch(localSearchForm, (val) => {
  emit('update:searchForm', { ...val });
}, { deep: true });

const emitSearch = () => {
  emit('search', { ...localSearchForm });
};

const emitReset = () => {
  Object.assign(localSearchForm, {
    name: '',
    status: undefined,
    roleId: undefined
  });
  emit('reset');
};
</script>

<style lang="scss" scoped>
.search-form {
  padding: 24px 32px 16px 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;

  .form-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 0 24px;

    .form-fields {
      display: flex;
      flex-wrap: wrap;
      gap: 0 24px;
      align-items: flex-end;
      flex: 1;
      min-width: 0;
    }

    .search-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 220px;

      .add-user-btn {
        margin-left: 16px;
        background: #67c23a;
        border-color: #67c23a;
        font-weight: bold;
        letter-spacing: 1px;
        transition: background 0.2s;

        &:hover {
          background: #85ce61;
        }
      }
    }
  }
}

.rounded-input {
  border-radius: 22px;
  min-width: 180px;
  height: 42px;

  :deep(.el-input__wrapper) {
    border-radius: 22px;
    min-height: 42px;
  }

  :deep(.el-input__inner) {
    height: 42px;
    font-size: 15px;
  }
}

// 状态选项样式
.status-option {
  display: flex;
  align-items: center;
  padding: 8px 4px;

  .status-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    margin-right: 10px;
  }
}

:deep(.status-select-dropdown) {
  .el-select-dropdown__item {
    height: auto;
    padding: 0 12px;

    &.selected {
      font-weight: bold;
    }

    &:hover {
      background-color: #f5f7fa;
    }
  }
}

@media (max-width: 900px) {
  .search-form .form-row {
    flex-direction: column;
    align-items: stretch;
    gap: 16px 0;

    .form-fields,
    .search-actions {
      width: 100%;
      justify-content: flex-start;
    }

    .search-actions {
      justify-content: flex-end;
      margin-top: 8px;
    }
  }
}
</style>
