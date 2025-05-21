<template>
  <div class="self-evaluation-edit">
    <el-form-item>
      <el-input type="textarea" :model-value="modelValue" @input="updateValue" :rows="4" placeholder="请输入自我评价"></el-input>
    </el-form-item>
  </div>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits } from 'vue';

defineProps<{
  modelValue: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const updateValue = (value: string) => {
  emit('update:modelValue', value);
};
</script>

<style lang="scss" scoped>
@use "../style/edittheme.scss";

.self-evaluation-edit {
  width: 100%;
}
</style>
