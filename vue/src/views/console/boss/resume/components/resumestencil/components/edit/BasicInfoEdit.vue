<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="姓名">
          <el-input :model-value="basicInfo.name" @update:model-value="updateField('name', $event)"
            placeholder="请输入姓名"></el-input>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="年龄">
          <el-input :model-value="basicInfo.age" @update:model-value="updateField('age', $event)"
            placeholder="请输入年龄"></el-input>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="性别">
          <el-select :model-value="basicInfo.gender" @update:model-value="updateField('gender', $event)"
            placeholder="请选择性别" style="width: 100%">
            <el-option label="男" value="男"></el-option>
            <el-option label="女" value="女"></el-option>
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-form-item label="所在地">
          <el-input :model-value="basicInfo.location" @update:model-value="updateField('location', $event)"
            placeholder="请输入所在地"></el-input>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="工作经验">
          <el-input :model-value="basicInfo.experience" @update:model-value="updateField('experience', $event)"
            placeholder="如：3年经验"></el-input>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="手机号码">
          <el-input :model-value="basicInfo.phone" @update:model-value="updateField('phone', $event)"
            placeholder="请输入手机号码"></el-input>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="电子邮箱">
          <el-input :model-value="basicInfo.email" @update:model-value="updateField('email', $event)"
            placeholder="请输入电子邮箱"></el-input>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="头像">
          <el-upload class="avatar-uploader" action="/api/upload" :show-file-list="false" :on-success="(res) => {
            if (res.code === 0) handleAvatarSuccess(res.data.url);
            else ElMessage.error('上传失败');
          }" :before-upload="beforeAvatarUpload">
            <img v-if="basicInfo.avatar" :src="basicInfo.avatar" class="avatar">
            <el-icon v-else class="avatar-uploader-icon">
              <Plus />
            </el-icon>
          </el-upload>
        </el-form-item>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 定义基本信息接口
interface BasicInfo {
  name: string;
  age: string;
  gender: string;
  location: string;
  experience: string;
  phone: string;
  email: string;
  avatar: string;
}

// 使用单个对象作为props
const props = defineProps<{
  basicInfo: BasicInfo
}>()

// 定义emit来更新父组件的值
const emit = defineEmits<{
  (e: 'update:basicInfo', value: BasicInfo): void
}>()

// 通用字段更新方法
const updateField = (field: keyof BasicInfo, value: string) => {
  emit('update:basicInfo', {
    ...props.basicInfo,
    [field]: value
  })
}

// 头像上传成功回调
const handleAvatarSuccess = (url: string) => {
  updateField('avatar', url);
}

// 头像上传前的验证
const beforeAvatarUpload = (file: File) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG) {
    ElMessage.error('上传头像图片只能是 JPG 或 PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传头像图片大小不能超过 2MB!')
  }
  return isJPG && isLt2M
}
</script>

<style lang="scss" scoped>
@use "../style/edittheme.scss";
</style>
