<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? '编辑角色' : '添加角色'"
    width="480px"
    destroy-on-close
    top="10vh"
    class="custom-dialog"
    @close="handleClose"
  >
    <el-form :model="localRoleData" label-width="80px" ref="roleFormRef" class="custom-form">
      <el-form-item label="角色名称" prop="roleName" :rules="[{ required: true, message: '请输入角色名称', trigger: 'blur' }]">
        <el-input
          v-model="localRoleData.roleName"
          placeholder="请输入角色名称"
          :disabled="isEdit && (localRoleData.roleName === 'ADMIN' || localRoleData.roleName === 'USER')"
        />
      </el-form-item>
      <el-form-item label="描述" prop="remark">
        <el-input
          v-model="localRoleData.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入角色描述"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch
          v-model="localRoleData.status"
          :active-value="1"
          :inactive-value="0"
          class="custom-switch"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="default" size="large" round @click="handleCancel">
          <el-icon><Close /></el-icon>
          取消
        </el-button>
        <el-button type="primary" size="large" round @click="handleConfirm">
          <el-icon><Check /></el-icon>
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { FormInstance } from 'element-plus';
import type { Role } from '@/api/auth/auth.d';
import { Check, Close } from '@element-plus/icons-vue';

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
</style>
