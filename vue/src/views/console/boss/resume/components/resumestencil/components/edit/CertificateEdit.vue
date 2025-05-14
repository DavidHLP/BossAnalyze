<template>
  <div>
    <!-- 证书列表 -->
    <div class="section-title">证书列表</div>
    <div v-for="(cert, index) in certificates" :key="index" class="form-item-group">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="证书名称">
            <el-input v-model="cert.name" placeholder="如：英语四级证书" @change="updateCertificates"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="获得时间">
            <el-date-picker v-model="cert.date" type="date" placeholder="选择日期" format="YYYY-MM" value-format="YYYY-MM"
              @change="updateCertificates"></el-date-picker>
          </el-form-item>
        </el-col>
        <el-col :span="10">
          <el-form-item label="证书描述">
            <el-input v-model="cert.description" placeholder="请描述该证书详情" @change="updateCertificates"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="2" class="delete-btn-wrapper">
          <el-button type="danger" circle icon="el-icon-delete" size="small" @click="removeCertificate(index)"
            class="delete-btn"></el-button>
        </el-col>
      </el-row>
    </div>

    <el-button type="primary" plain icon="el-icon-plus" @click="addCertificate" class="add-btn">
      添加证书
    </el-button>
  </div>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits } from 'vue'

interface Certificate {
  name: string
  date: string
  description: string
}

// 使用v-model方式接收并更新props
const props = defineProps<{
  certificates: Certificate[]
}>()

// 定义emit来更新父组件的值
const emit = defineEmits<{
  (e: 'update:certificates', value: Certificate[]): void
}>()

// 添加证书
const addCertificate = () => {
  const updatedCertificates = [...props.certificates]

  updatedCertificates.push({
    name: '',
    date: '',
    description: '',
  })

  emit('update:certificates', updatedCertificates)
}

// 移除证书
const removeCertificate = (index: number) => {
  const updatedCertificates = [...props.certificates]
  updatedCertificates.splice(index, 1)
  emit('update:certificates', updatedCertificates)
}

// 更新证书
const updateCertificates = () => {
  emit('update:certificates', [...props.certificates])
}
</script>

<style lang="scss" scoped>
@use '../style/edittheme.scss';

.delete-btn-wrapper {
  display: flex;
  align-items: center;
  height: 100%;
  justify-content: center;
}
</style>
