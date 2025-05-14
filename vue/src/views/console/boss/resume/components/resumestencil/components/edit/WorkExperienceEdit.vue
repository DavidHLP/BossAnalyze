<template>
  <div>
    <div v-for="(exp, index) in workExperience" :key="'work-'+index" class="form-item-group">
      <div class="group-header">
        <h4>工作经历 #{{ index + 1 }}</h4>
        <el-button link @click="removeWorkExperience(index)" class="delete-btn">删除</el-button>
      </div>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="公司名称">
            <el-input v-model="exp.company" placeholder="请输入公司名称"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="职位">
            <el-input v-model="exp.position" placeholder="请输入职位"></el-input>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="开始时间">
            <el-date-picker
              v-model="exp.startDate"
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
              v-model="exp.endDate"
              type="month"
              format="yyyy-MM"
              value-format="yyyy-MM"
              placeholder="选择结束时间">
            </el-date-picker>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="工作职责">
        <div v-for="(duty, dutyIndex) in exp.duties" :key="'duty-'+dutyIndex" class="duty-item">
          <el-input v-model="exp.duties[dutyIndex]" placeholder="请输入工作职责"></el-input>
          <el-button link @click="removeDuty(index, dutyIndex)" class="remove-duty">删除</el-button>
        </div>
        <el-button link @click="addDuty(index)" class="add-duty">+ 添加工作职责</el-button>
      </el-form-item>
    </div>

    <el-button type="primary" @click="addWorkExperience" class="add-btn">添加工作经历</el-button>
  </div>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits } from 'vue'

// 类型定义
interface WorkExperience {
  startDate: string;
  endDate: string;
  company: string;
  position: string;
  duties: string[];
}

// Props 和 Emits 定义
const props = defineProps<{
  workExperience: WorkExperience[]
}>()

const emit = defineEmits<{
  'update:workExperience': [value: WorkExperience[]]
}>()

// 工作经验相关方法
const addWorkExperience = () => {
  const newWorkExperience = [...props.workExperience, {
    startDate: '',
    endDate: '',
    company: '',
    position: '',
    duties: ['']
  }]
  emit('update:workExperience', newWorkExperience)
}

const removeWorkExperience = (index: number) => {
  const newWorkExperience = [...props.workExperience]
  newWorkExperience.splice(index, 1)
  emit('update:workExperience', newWorkExperience)
}

const addDuty = (expIndex: number) => {
  const newWorkExperience = [...props.workExperience]
  newWorkExperience[expIndex].duties.push('')
  emit('update:workExperience', newWorkExperience)
}

const removeDuty = (expIndex: number, dutyIndex: number) => {
  const newWorkExperience = [...props.workExperience]
  newWorkExperience[expIndex].duties.splice(dutyIndex, 1)
  emit('update:workExperience', newWorkExperience)
}
</script>

<style lang="scss" scoped>
@use "../style/edittheme.scss";
</style>
