<template>
  <div class="search-container">
    <el-form :model="localSearchForm" class="search-form">
      <div class="form-content">
        <div class="form-fields">
          <el-form-item label="姓名" class="form-field">
            <el-input
              v-model="localSearchForm.name"
              placeholder="请输入姓名"
              clearable
              class="search-input"
            />
          </el-form-item>

          <el-form-item label="状态" class="form-field">
            <el-select
              v-model="localSearchForm.status"
              placeholder="请选择状态"
              clearable
              class="search-select"
              popper-class="status-select-dropdown"
            >
              <el-option label="启用" :value="1">
                <template #default>
                  <div class="status-option enabled">
                    <span class="status-dot"></span>
                    <span>启用</span>
                  </div>
                </template>
              </el-option>
              <el-option label="禁用" :value="0">
                <template #default>
                  <div class="status-option disabled">
                    <span class="status-dot"></span>
                    <span>禁用</span>
                  </div>
                </template>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="角色" class="form-field">
            <el-select
              v-model="localSearchForm.roleId"
              filterable
              remote
              reserve-keyword
              placeholder="请输入角色名称"
              :remote-method="remoteRoleSearch"
              :loading="roleLoading"
              clearable
              class="search-select"
            >
              <el-option
                v-for="role in roleOptions"
                :key="role.value"
                :label="role.label"
                :value="role.value"
              >
                <template #default>
                  <div class="role-option">
                    <span class="role-dot"></span>
                    <span>{{ role.label }}</span>
                  </div>
                </template>
              </el-option>
            </el-select>
          </el-form-item>
        </div>

        <div class="search-actions">
          <el-button
            type="primary"
            @click="emitSearch"
            class="search-btn primary-btn"
            :icon="Search"
          >
            搜索
          </el-button>
          <el-button
            @click="emitReset"
            class="search-btn reset-btn"
            :icon="Refresh"
          >
            重置
          </el-button>
          <el-button
            type="success"
            class="search-btn add-btn"
            @click="$emit('add-user')"
            :icon="Plus"
          >
            添加用户
          </el-button>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits, reactive, watch } from 'vue';
import { Search, Refresh, Plus } from '@element-plus/icons-vue';

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
// 引用集中管理的样式文件
@use '../usermanagement.scss' as *;
</style>
