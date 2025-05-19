<template>
  <div>
    <div class="avatar-container">
      <el-image :src="avatarUrl || defaultAvatar" alt="头像" class="avatar"
        :preview-src-list="avatarUrl ? [avatarUrl] : []" fit="cover" preview-teleported />
    </div>
    <div class="personal-info">
      <div class="info-item"><el-icon><Calendar /></el-icon> {{ basicInfo.age || '27岁' }}</div>
      <div class="info-item"><el-icon><Male /></el-icon> {{ basicInfo.gender || '男' }}</div>
      <div class="info-item"><el-icon><Location /></el-icon> {{ basicInfo.location || '上海' }}</div>
      <div class="info-item"><el-icon><Timer /></el-icon> {{ basicInfo.experience || '4年经验' }}</div>
      <div class="info-item"><el-icon><Phone /></el-icon> {{ basicInfo.phone || '15688888888' }}</div>
      <div class="info-item"><el-icon><Message /></el-icon> {{ basicInfo.email || 'example@qq.com' }}</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch, onMounted } from 'vue'
import { Calendar, Male, Location, Timer, Phone, Message } from '@element-plus/icons-vue'
import { getImageUrl } from '@/api/minio/minio'

interface BasicInfo {
  age?: string;
  gender?: string;
  location?: string;
  experience?: string;
  phone?: string;
  email?: string;
  avatar?: string;
}

const props = defineProps<{
  basicInfo: BasicInfo
}>()

const defaultAvatar = ref('/avatar-default.jpg')
const avatarUrl = ref('')

// 加载头像URL
const loadAvatarUrl = async () => {
  if (props.basicInfo.avatar) {
    try {
      const res = await getImageUrl(props.basicInfo.avatar)
      avatarUrl.value = res.url
    } catch (error) {
      console.error('加载头像失败:', error)
      avatarUrl.value = ''
    }
  } else {
    avatarUrl.value = ''
  }
}

// 监听头像变化
watch(() => props.basicInfo.avatar, () => {
  loadAvatarUrl()
})

// 组件挂载时加载头像
onMounted(() => {
  loadAvatarUrl()
})
</script>

<style scoped>
.avatar-container {
  text-align: center;
  margin-bottom: 25px;
  padding-top: 10px;
}

.avatar {
  width: 140px;
  height: 140px;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  border: 3px solid rgba(255, 255, 255, 0.3);
  transition: transform 0.3s ease;
}

.avatar:hover {
  transform: scale(1.03);
}

.personal-info {
  margin-bottom: 30px;
}

.info-item {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  padding: 8px 2px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 14px;
  transition: background-color 0.2s ease;
}

.info-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.info-item .el-icon {
  margin-right: 12px;
  font-size: 16px;
  width: 20px;
  text-align: center;
  color: #3498db;
}
</style>
