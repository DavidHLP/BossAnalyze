<template>
  <div class="education-section">
    <div v-for="(edu, index) in educationList" :key="'edu-'+index" class="form-item-group">
      <div class="group-header">
        <h4>教育经历 #{{ index + 1 }}</h4>
        <el-button link @click="removeEducation(index)" class="delete-btn">删除</el-button>
      </div>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="学校名称">
            <el-input v-model="edu.school" placeholder="请输入学校名称"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="专业">
            <el-input v-model="edu.major" placeholder="请输入专业名称"></el-input>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="开始时间">
            <el-date-picker
              v-model="edu.startDate"
              type="month"
              format="yyyy-MM"
              value-format="yyyy-MM"
              placeholder="选择开始时间">
            </el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结束时间">
            <el-date-picker
              v-model="edu.endDate"
              type="month"
              format="yyyy-MM"
              value-format="yyyy-MM"
              placeholder="选择结束时间">
            </el-date-picker>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="专业成绩">
            <el-input v-model="edu.gpa" placeholder="如：GPA 3.86/4（专业前5%）"></el-input>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="专业课程">
        <el-input type="textarea" v-model="edu.courses" :rows="3" placeholder="请输入相关专业课程，用逗号分隔"></el-input>
      </el-form-item>
    </div>

    <el-button type="primary" @click="addEducation" class="add-btn">添加教育经历</el-button>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch, defineProps, defineEmits } from 'vue'

interface Education {
  startDate: string;
  endDate: string;
  school: string;
  major: string;
  gpa: string;
  courses: string;
}

const props = defineProps<{
  education: Education[]
}>()

const emit = defineEmits<{
  (e: 'update:education', value: Education[]): void
}>()

const educationList = ref<Education[]>([...props.education])

// 监听props变化更新本地值
watch(() => props.education, (newValue) => {
  educationList.value = [...newValue]
}, { deep: true })

// 添加教育经历
const addEducation = () => {
  educationList.value.push({
    startDate: '',
    endDate: '',
    school: '',
    major: '',
    gpa: '',
    courses: ''
  })
  updateEducation()
}

// 删除教育经历
const removeEducation = (index: number) => {
  educationList.value.splice(index, 1)
  updateEducation()
}

// 更新教育信息
const updateEducation = () => {
  emit('update:education', educationList.value)
}
</script>

<style lang="scss" scoped>
@use "../style/edittheme.scss";

.education-section {
  width: 100%;
}
</style>
