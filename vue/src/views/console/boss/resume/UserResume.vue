<template>
  <el-button type="primary" @click="submitForm" v-if="!isEditing">保存简历</el-button>
  <el-button @click="resetForm" v-if="!isEditing">重置</el-button>
  <el-button type="success" @click="previewResume" v-if="!isEditing">全屏预览简历</el-button>
  <el-button type="success" @click="isEditing = false" v-if="isEditing">返回</el-button>
  <div class="resume-container">
    <!-- 简历编辑模式 - 左侧编辑，右侧预览 -->
    <div class="resume-split-view">
      <div class="edit-panel" v-if="!isEditing">
        <StencilEditOne :resumeData="resumeData" @save="handleSave" @preview="handlePreview"
          @update:form="handleFormUpdate" />
      </div>
      <div class="preview-panel">
        <StencilViewOne :resume="previewData" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import StencilViewOne from './components/resumestencil/StencilViewOne.vue'
import StencilEditOne from './components/resumestencil/StencilEditOne.vue'
import { useLayoutStore } from '@/stores/layout/layoutStore'
import type { ResumeData } from '@/mock/resumeData'
import { getResumeData, saveResumeData, resetResumeData } from '@/mock/mockjsApi'

const isEditing = ref(false);
const resumeData = ref<ResumeData>({} as ResumeData);
const previewData = ref<ResumeData>({} as ResumeData);

// 获取布局 store
const layoutStore = useLayoutStore();

onMounted(async () => {
  // 加载简历数据
  await loadResumeData();

  // 进入简历页面时自动折叠侧边栏
  layoutStore.setCollapse(true);
});

const loadResumeData = async () => {
  try {
    // 使用mock服务获取简历数据
    const data = await getResumeData();
    resumeData.value = data;
    previewData.value = { ...data };
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
  resumeData.value = { ...data };
  previewData.value = { ...data };
  await saveResumeToStorage();
  switchToPreview();
};

const handlePreview = (data: ResumeData) => {
  resumeData.value = { ...data };
  previewData.value = { ...data };
  switchToPreview();
};

// 处理表单实时更新，用于同步到预览区域
const handleFormUpdate = (data: ResumeData) => {
  previewData.value = { ...data };
};

// 添加按钮对应的功能函数
const submitForm = async () => {
  if (!isEditing.value) {
    // 如果是编辑模式，获取StencilEditOne组件中的数据并保存
    await saveResumeToStorage();
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
    // 使用mock服务重置简历数据
    const data = await resetResumeData();
    resumeData.value = { ...data };
    previewData.value = { ...data };
    ElMessage.success('重置成功');
  } catch (error) {
    console.error('重置简历数据失败', error);
    ElMessage.error('重置失败');
  }
};

const previewResume = () => {
  // 全屏预览简历
  isEditing.value = true;
  previewData.value = { ...resumeData.value };
};
</script>

<style scoped>
.resume-container {
  max-width: 100%;
  margin: 0 auto;
  padding: 20px;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.resume-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 2px solid #3498db;
  padding-bottom: 10px;
}

.resume-header h1 {
  margin: 0;
  color: #2c3e50;
}

.resume-content {
  background-color: #f9f9f9;
  border-radius: 5px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  flex: 1;
  overflow: auto;
}

/* 新增分屏视图样式 */
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
  border-radius: 5px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: auto;
}

.preview-panel {
  flex: 1;
  background-color: #f9f9f9;
  border-radius: 5px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: auto;
}
</style>
