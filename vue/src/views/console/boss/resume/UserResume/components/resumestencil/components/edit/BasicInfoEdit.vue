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
          <el-select
            v-model="locationValue"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请输入所在地"
            @change="handleLocationChange"
            style="width: 100%"
          >
            <el-option
              v-for="item in locationValue"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
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
          <div class="avatar-upload-section">
            <el-upload :http-request="handleUploadRequest" :on-success="handleAvatarSuccess"
              list-type="picture-card" class="avatar-uploader" :auto-upload="true">
              <div class="avatar-inner">
                <template v-if="basicInfo.avatar">
                  <el-image :src="avatarUrl" class="uploaded-avatar" fit="cover" />
                </template>
                <div v-else class="upload-placeholder">
                  <el-icon>
                    <Plus />
                  </el-icon>
                  <span>上传头像</span>
                </div>
              </div>
            </el-upload>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits, ref, onMounted, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { uploadImage, getImageUrl } from '@/api/minio/minio'
import type { ImageResponse } from '@/api/minio/minio.d'

// 定义基本信息接口
interface BasicInfo {
  name: string;
  age: string;
  gender: string;
  location: string[] | string;
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

const avatarUrl = ref('')
const locationValue = ref<string[]>([])

// 处理所在地变化
const handleLocationChange = (value: string[]) => {
  updateField('location', value)
}

// 通用字段更新方法
const updateField = (field: keyof BasicInfo, value: string | string[]) => {
  emit('update:basicInfo', {
    ...props.basicInfo,
    [field]: value
  })
}

// 更新头像URL
const updateAvatarUrl = async () => {
  if (props.basicInfo.avatar) {
    try {
      const response = await getImageUrl(props.basicInfo.avatar)
      avatarUrl.value = response.url
    } catch (error) {
      console.error('获取头像URL失败:', error)
      avatarUrl.value = ''
    }
  } else {
    avatarUrl.value = ''
  }
}

// 组件挂载时初始化locationValue
onMounted(() => {
  updateAvatarUrl()

  // 初始化location值
  if (props.basicInfo.location) {
    // 如果location是字符串，转换为数组
    if (typeof props.basicInfo.location === 'string') {
      locationValue.value = props.basicInfo.location ? [props.basicInfo.location] : []
    } else {
      // 如果已经是数组则直接赋值
      locationValue.value = props.basicInfo.location
    }
  }
})

// 为avatar单独添加监听
watch(() => props.basicInfo.avatar, () => {
  updateAvatarUrl()
})

// 监听location属性变化
watch(() => props.basicInfo.location, (newValue) => {
  if (newValue) {
    if (typeof newValue === 'string') {
      locationValue.value = newValue ? [newValue] : []
    } else {
      locationValue.value = newValue
    }
  } else {
    locationValue.value = []
  }
})

// 头像上传成功回调
const handleAvatarSuccess = (response: ImageResponse) => {

  if (response.fileName) {
    updateField('avatar', response.fileName)
    ElMessage.success('头像上传成功')
    updateAvatarUrl()
  } else {
    console.error('头像上传响应无效:', response)
    ElMessage.error('头像上传失败，响应数据不完整')
  }
}

// 自定义上传请求处理
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const handleUploadRequest = async (options: any) => {
  try {
    const response = await uploadImage(options.file)
    handleAvatarSuccess(response)
    if (options.onSuccess && response) {
      options.onSuccess(response)
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败，请稍后重试')
    if (options.onError) {
      options.onError(new Error('上传失败'))
    }
  }
}
</script>

<style lang="scss" scoped>
@use "../style/edittheme.scss";

.avatar-upload-section {
  display: flex;
  justify-content: center;
}

.avatar-uploader {
  :deep(.el-upload) {
    border: 1px dashed #d9d9d9;
    border-radius: 50%;
    overflow: hidden;
    cursor: pointer;
    position: relative;
    transition: all 0.3s ease;

    &:hover {
      border-color: #3B82F6;
    }
  }

  .avatar-inner {
    width: 110px;
    height: 110px;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
  }

  .uploaded-avatar {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .upload-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: #8c8c8c;

    .el-icon {
      font-size: 22px;
    }

    span {
      font-size: 14px;
    }
  }
}
</style>
