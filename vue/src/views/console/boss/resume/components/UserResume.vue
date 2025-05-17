<template>
  <div class="resume-page">
    <div class="action-buttons mb-4">
      <el-space>
        <el-button type="primary" @click="submitForm" v-if="!isReadOnly && !isEditing">
          <el-icon class="mr-1"><Check /></el-icon>保存简历
        </el-button>
        <el-button @click="resetForm" v-if="!isReadOnly && !isEditing">
          <el-icon class="mr-1"><RefreshRight /></el-icon>重置
        </el-button>
        <el-button type="success" @click="previewResume" v-if="!isReadOnly && !isEditing">
          <el-icon class="mr-1"><View /></el-icon>全屏预览
        </el-button>
        <el-button @click="isEditing = false" v-if="isEditing">
          <el-icon class="mr-1"><Back /></el-icon>返回编辑
        </el-button>
        <el-button @click="handleCancel">
          <el-icon class="mr-1"><Close /></el-icon>关闭
        </el-button>
      </el-space>
    </div>

    <div class="resume-container">
      <!-- 简历编辑模式 - 左侧编辑，右侧预览 -->
      <div class="resume-split-view">
        <div class="edit-panel" v-if="!isEditing && !isReadOnly">
          <StencilEditOne
            :key="resetKey"
            :resumeData="resumeData"
            @save="handleSave"
            @preview="handlePreview"
            @update:form="handleFormUpdate"
          />
        </div>
        <div class="preview-panel" :class="{ 'full-width': isReadOnly || isEditing }">
          <StencilViewOne :resume="previewData" />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, RefreshRight, View, Back, Close } from '@element-plus/icons-vue'
import StencilViewOne from '@/views/console/boss/resume/components/resumestencil/StencilViewOne.vue'
import StencilEditOne from '@/views/console/boss/resume/components/resumestencil/StencilEditOne.vue'
import type { ResumeData } from '@/api/resume/resume.d'
import { getResumeData, saveResumeData, getResumeDetail } from '@/api/resume/resume'

const props = defineProps({
  initialResumeData: {
    type: Object as () => ResumeData,
    required: false,
    default: () => ({} as ResumeData)
  },
  editMode: {
    type: String as () => 'add' | 'edit' | 'view',
    default: 'edit'
  }
})

const emit = defineEmits(['save', 'cancel'])

const isEditing = ref(false);
const resumeData = ref<ResumeData>({} as ResumeData);
const previewData = ref<ResumeData>({} as ResumeData);
// 添加重置标记，用于强制刷新编辑组件
const resetKey = ref(0);

// 计算属性：是否为只读模式（查看模式）
const isReadOnly = computed(() => props.editMode === 'view');

// 监听传入的简历数据变化
watch(() => props.initialResumeData, (newData) => {
  if (newData && Object.keys(newData).length > 0) {
    resumeData.value = JSON.parse(JSON.stringify(newData)); // 使用深拷贝
    previewData.value = JSON.parse(JSON.stringify(newData)); // 使用深拷贝
  }
}, { immediate: true, deep: true });

onMounted(async () => {
  // 如果没有传入简历数据，则加载默认数据
  if (!props.initialResumeData || Object.keys(props.initialResumeData).length === 0) {
    await loadResumeData();
  }
});

const loadResumeData = async () => {
  try {
    // 使用mock服务获取简历数据
    const data = await getResumeData();

    // 保存原ID(如果有)
    const originalId = resumeData.value?.id;

    // 使用深拷贝更新数据
    resumeData.value = JSON.parse(JSON.stringify(data));
    previewData.value = JSON.parse(JSON.stringify(data));

    // 如果有原ID，则保留
    if (originalId) {
      resumeData.value.id = originalId;
      previewData.value.id = originalId;
    }

    ElMessage.success('简历数据加载成功');
  } catch (error) {
    console.error('简历数据加载失败', error);
    ElMessage.error('简历数据加载失败');
  }
};

const switchToPreview = () => {
  isEditing.value = true;
};

const handleSave = async (data: ResumeData) => {
  // 保存原ID(如果有)
  const originalId = resumeData.value?.id;

  // 使用深拷贝更新数据
  resumeData.value = JSON.parse(JSON.stringify(data));
  previewData.value = JSON.parse(JSON.stringify(data));

  // 如果有原ID，则保留
  if (originalId) {
    resumeData.value.id = originalId;
    previewData.value.id = originalId;
  }

  await saveResumeToStorage();
  // 向父组件发送保存事件
  emit('save', resumeData.value);
  switchToPreview();
};

const handlePreview = (data: ResumeData) => {
  // 保存原ID(如果有)
  const originalId = resumeData.value?.id;

  // 使用深拷贝更新数据
  resumeData.value = JSON.parse(JSON.stringify(data));
  previewData.value = JSON.parse(JSON.stringify(data));

  // 如果有原ID，则保留
  if (originalId) {
    resumeData.value.id = originalId;
    previewData.value.id = originalId;
  }

  switchToPreview();
};

// 处理表单实时更新，用于同步到预览区域
const handleFormUpdate = (data: ResumeData) => {
  // 使用深拷贝确保数据完全替换，避免引用问题
  previewData.value = JSON.parse(JSON.stringify(data));

  // 确保ID一致性 - 双向同步ID
  if (resumeData.value.id) {
    // 如果resumeData有ID但data没有，则保留resumeData的ID
    previewData.value.id = resumeData.value.id;
  } else if (data.id) {
    // 如果data有ID，则更新resumeData的ID
    resumeData.value.id = data.id;
  }

  // 同步更新resumeData，确保保存时能获取到最新数据
  resumeData.value = JSON.parse(JSON.stringify(previewData.value));
};

// 添加按钮对应的功能函数
const submitForm = async () => {
  if (!isEditing.value && !isReadOnly.value) {
    // 如果是编辑模式，获取StencilEditOne组件中的数据并保存
    await saveResumeToStorage();
    // 向父组件发送保存事件
    emit('save', resumeData.value);
    ElMessage.success('保存成功');
  }
};

const saveResumeToStorage = async () => {
  try {
    // 使用mock服务保存简历数据
    const success = await saveResumeData(resumeData.value);
    if (!success) {
      ElMessage.error('保存失败');
    }
  } catch (error) {
    console.error('保存简历数据失败', error);
    ElMessage.error('保存失败');
  }
};

const resetForm = async () => {
  try {
    const data = await getResumeDetail(resumeData.value.id || '');

    // 使用深拷贝确保数据完全替换
    resumeData.value = JSON.parse(JSON.stringify(data));
    previewData.value = JSON.parse(JSON.stringify(data));

    // 增加重置键值，强制刷新编辑组件
    resetKey.value++;

    ElMessage.success('重置成功');
  } catch (error) {
    console.error('重置简历数据失败', error);
    ElMessage.error('重置失败');
  }
};

const previewResume = () => {
  // 全屏预览简历
  isEditing.value = true;

  // 确保ID同步
  const originalId = resumeData.value?.id;
  previewData.value = JSON.parse(JSON.stringify(resumeData.value));

  // 如果有原ID，则保留
  if (originalId) {
    previewData.value.id = originalId;
  }
};

// 取消按钮处理函数
const handleCancel = () => {
  emit('cancel');
};
</script>

<style scoped>
.resume-page {
  max-width: 100%;
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
  padding: 0 20px;
}

.action-buttons {
  padding: 16px 0;
}

.mr-1 {
  margin-right: 4px;
}

.mb-4 {
  margin-bottom: 16px;
}

.resume-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: calc(100% - 60px);
}

.resume-split-view {
  display: flex;
  gap: 20px;
  height: 100%;
  flex: 1;
  overflow: hidden;
}

.edit-panel {
  flex: 1;
  background-color: #f9f9f9;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  overflow: auto;
}

.preview-panel {
  flex: 1;
  background-color: #f9f9f9;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  overflow: auto;
}

.full-width {
  flex: 2;
  width: 100%;
}

@media (max-width: 1200px) {
  .resume-split-view {
    flex-direction: column;
  }

  .edit-panel, .preview-panel {
    max-height: 50%;
  }
}

@media (max-width: 768px) {
  .resume-page {
    padding: 0 12px;
  }

  .action-buttons {
    padding: 12px 0;
  }
}
</style>
