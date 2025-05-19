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
              format="YYYY-MM"
              value-format="YYYY-MM"
              :disabled-date="disableFutureDates"
              @change="validateDates(index)"
              placeholder="选择开始时间">
            </el-date-picker>
            <div v-if="edu.dateError" class="error-message">{{ edu.dateError }}</div>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结束时间">
            <el-date-picker
              v-model="edu.endDate"
              type="month"
              format="YYYY-MM"
              value-format="YYYY-MM"
              :disabled-date="(time: Date) => disableEndDate(time, edu.startDate)"
              @change="validateDates(index)"
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
  dateError?: string;
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

// 禁用未来日期
const disableFutureDates = (time: Date) => {
  return time.getTime() > Date.now()
}

// 禁用早于开始日期的结束日期
const disableEndDate = (time: Date, startDate: string) => {
  // 禁用未来日期
  if (time.getTime() > Date.now()) {
    return true
  }

  // 如果有开始日期，则禁用早于开始日期的时间
  if (startDate) {
    const start = new Date(startDate)
    return time.getTime() < start.getTime()
  }

  return false
}

// 验证日期
const validateDates = (index: number) => {
  const edu = educationList.value[index]

  // 清除之前的错误
  edu.dateError = ''

  if (edu.startDate && edu.endDate) {
    const startDate = new Date(edu.startDate)
    const endDate = new Date(edu.endDate)

    if (startDate > endDate) {
      edu.dateError = '结束时间不能早于开始时间'
      return false
    }
  }

  updateEducation()
  return true
}

// 添加教育经历
const addEducation = () => {
  educationList.value.push({
    startDate: '',
    endDate: '',
    school: '',
    major: '',
    gpa: '',
    courses: '',
    dateError: ''
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
  // 过滤掉 dateError 字段后再发送
  const cleanedEducationList = educationList.value.map((edu) => {
    // 创建新对象不包含dateError
    return {
      startDate: edu.startDate,
      endDate: edu.endDate,
      school: edu.school,
      major: edu.major,
      gpa: edu.gpa,
      courses: edu.courses
    }
  })
  emit('update:education', cleanedEducationList)
}
</script>

<style lang="scss" scoped>
@use "../style/edittheme.scss";

.education-section {
  width: 100%;
}

.error-message {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
}
</style>
