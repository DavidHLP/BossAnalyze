<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="求职意向">
          <el-input :model-value="jobIntention.jobTarget" @update:model-value="updateField('jobTarget', $event)" placeholder="如：行政专员"></el-input>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="期望薪资">
          <el-input :model-value="jobIntention.expectedSalary" @update:model-value="updateField('expectedSalary', $event)" placeholder="如：8000/月"></el-input>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="意向城市">
          <el-input :model-value="jobIntention.targetCity" @update:model-value="updateField('targetCity', $event)" placeholder="如：上海"></el-input>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="入职时间">
          <el-input :model-value="jobIntention.availableTime" @update:model-value="updateField('availableTime', $event)" placeholder="如：一个月内到岗"></el-input>
        </el-form-item>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits } from 'vue'

// 定义求职意向接口
interface JobIntention {
  jobTarget: string;
  expectedSalary: string;
  targetCity: string;
  availableTime: string;
}

// 使用单个对象作为props
const props = defineProps<{
  jobIntention: JobIntention
}>()

// 定义emit来更新父组件的值
const emit = defineEmits<{
  (e: 'update:jobIntention', value: JobIntention): void
}>()

// 通用字段更新方法
const updateField = (field: keyof JobIntention, value: string) => {
  emit('update:jobIntention', {
    ...props.jobIntention,
    [field]: value
  })
}
</script>

<style lang="scss" scoped>
@use "../style/edittheme.scss";
</style>
