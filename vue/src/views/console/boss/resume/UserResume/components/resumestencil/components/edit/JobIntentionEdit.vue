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
          <el-select
            v-model="selectedCities"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请输入或选择城市"
            style="width: 100%"
            @change="updateCities"
          >
            <el-option v-for="city in commonCities" :key="city" :label="city" :value="city" />
          </el-select>
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
import { defineProps, defineEmits, ref, watch, onMounted } from 'vue'

// 定义求职意向接口
interface JobIntention {
  jobTarget: string;
  expectedSalary: string;
  targetCity: string[];
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

// 常用城市列表
const commonCities = [
  '北京', '上海', '广州', '深圳', '杭州',
  '南京', '武汉', '成都', '重庆', '西安',
  '苏州', '天津', '长沙', '郑州', '青岛',
  '大连', '宁波', '厦门', '福州', '济南'
]

// 本地维护城市选择状态
const selectedCities = ref<string[]>([])

// 初始化和同步城市选择
onMounted(() => {
  if (Array.isArray(props.jobIntention.targetCity)) {
    selectedCities.value = [...props.jobIntention.targetCity]
  }
})

// 监听prop变化同步状态
watch(() => props.jobIntention.targetCity, (newValue) => {
  if (Array.isArray(newValue) && JSON.stringify(newValue) !== JSON.stringify(selectedCities.value)) {
    selectedCities.value = [...newValue]
  }
}, { deep: true })

// 通用字段更新方法
const updateField = (field: keyof JobIntention, value: string) => {
  emit('update:jobIntention', {
    ...props.jobIntention,
    [field]: value
  })
}

// 更新城市选择
const updateCities = (cities: string[]) => {
  emit('update:jobIntention', {
    ...props.jobIntention,
    targetCity: cities
  })
}
</script>

<style lang="scss" scoped>
@use "../style/edittheme.scss";
</style>
