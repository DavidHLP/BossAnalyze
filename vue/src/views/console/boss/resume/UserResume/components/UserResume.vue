<template>
  <div class="resume-page">
    <div class="action-buttons mb-4">
      <el-space>
        <el-button type="primary" @click="submitForm" v-if="!isReadOnly">
          <el-icon class="mr-1"><Check /></el-icon>保存简历
        </el-button>
        <el-button @click="resetForm" v-if="!isReadOnly">
          <el-icon class="mr-1"><RefreshRight /></el-icon>重置
        </el-button>
        <el-button type="success" @click="previewResume" v-if="!isReadOnly">
          <el-icon class="mr-1"><View /></el-icon>全屏预览
        </el-button>
        <el-button @click="isEditing = false" v-if="isReadOnly">
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
        <div class="preview-panel" :class="{ 'full-width': isReadOnly }">
          <StencilViewOne :resume="previewData" v-if="isPreviewDataReady" />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, RefreshRight, View, Back, Close } from '@element-plus/icons-vue'
import StencilViewOne from '@/views/console/boss/resume/UserResume/components/resumestencil/StencilViewOne.vue'
import StencilEditOne from '@/views/console/boss/resume/UserResume/components/resumestencil/StencilEditOne.vue'
import type { ResumeData } from '@/api/resume/resume.d'
import {  saveResumeData, getResumeDetail, addResume } from '@/api/resume/resume'

// 定义拖拽部分的接口
interface DraggableSection {
  id: string;
  type: string;
  title: string;
}

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
const isAdding = ref(false);
const resumeData = ref<ResumeData>({} as ResumeData);
const previewData = ref<ResumeData>({} as ResumeData);
// 添加重置标记，用于强制刷新编辑组件
const resetKey = ref(0);
// 添加加载状态变量，防止重复提交
const loading = ref(false);

// 定义可拖拽部分的默认结构
const draggableSections = ref<DraggableSection[]>([
  { id: '1', type: 'education', title: '教育背景' },
  { id: '2', type: 'workExperience', title: '工作经验' },
  { id: '3', type: 'certificates', title: '个人证书' },
  { id: '4', type: 'interestTags', title: '兴趣爱好' },
  { id: '5', type: 'selfEvaluation', title: '自我评价' }
]);

// 计算属性：是否为只读模式（查看模式）
const isReadOnly = computed(() => props.editMode === 'view');

// 监听编辑模式变化
watch(() => props.editMode, (newMode) => {
  isAdding.value = newMode === 'add';
}, { immediate: true });

// 监听传入的简历数据变化
watch(() => props.initialResumeData, (newData) => {
  if (newData && Object.keys(newData).length > 0) {
    resumeData.value = JSON.parse(JSON.stringify(newData));
    previewData.value = JSON.parse(JSON.stringify(newData));
  }
}, { immediate: true, deep: true });

onMounted(async () => {
  // 更新添加状态
  isAdding.value = props.editMode === 'add';

  // 如果没有传入简历数据，则加载默认数据
  if (!props.initialResumeData || Object.keys(props.initialResumeData).length === 0) {
    if (isAdding.value) {
      // 添加模式，初始化空数据结构
      initEmptyResumeData();
    } else {
      // 编辑模式，加载默认数据
      await loadResumeData();
    }
  } else {
    // 复制传入的数据
    resumeData.value = JSON.parse(JSON.stringify(props.initialResumeData));
    previewData.value = JSON.parse(JSON.stringify(props.initialResumeData));

    // 确保数组字段初始化正确
    ensureArrayFields(resumeData.value);
    ensureArrayFields(previewData.value);
  }
});

  const loadResumeData = async () => {
  try {
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

    // 确保数组字段初始化正确
    ensureArrayFields(resumeData.value);
    ensureArrayFields(previewData.value);

    ElMessage.success('简历数据加载成功');
  } catch {
    ElMessage.error('简历数据加载失败');
  }
};

// 初始化空简历数据（用于添加模式）
const initEmptyResumeData = () => {
  // 清除ID，确保创建新记录
  resumeData.value.id = '';
  previewData.value.id = '';

  // 初始化基本信息为空
  resumeData.value.name = '';
  resumeData.value.age = '';
  resumeData.value.gender = '';
  resumeData.value.location = [];
  resumeData.value.experience = '';
  resumeData.value.phone = '';
  resumeData.value.email = '';
  resumeData.value.avatar = '';
  resumeData.value.jobTarget = '';
  resumeData.value.expectedSalary = '';
  resumeData.value.targetCity = [];
  resumeData.value.availableTime = '';
  resumeData.value.selfEvaluation = '';

  // 初始化数组字段
  resumeData.value.education = [];
  resumeData.value.workExperience = [];
  resumeData.value.certificates = [];
  resumeData.value.interestTags = [];
  resumeData.value.customSkills = [];
  resumeData.value.sectionOrder = draggableSections.value.map(section => section.type);

  // 同步到预览数据
  previewData.value = JSON.parse(JSON.stringify(resumeData.value));

  // 确保数组字段初始化正确
  ensureArrayFields(resumeData.value);
  ensureArrayFields(previewData.value);
};

const switchToPreview = () => {
  isEditing.value = true;
};

const handleSave = async (data: ResumeData) => {
  // 使用深拷贝更新数据
  resumeData.value = JSON.parse(JSON.stringify(data));
  previewData.value = JSON.parse(JSON.stringify(data));

  // ID处理根据当前模式不同
  if (isAdding.value) {
    // 添加模式下，确保ID为空
    resumeData.value.id = '';
    previewData.value.id = '';
  } else if (props.initialResumeData?.id) {
    // 编辑模式下，保留原ID
    resumeData.value.id = props.initialResumeData.id;
    previewData.value.id = props.initialResumeData.id;
  }

  // 确保数组字段初始化正确
  ensureArrayFields(resumeData.value);
  ensureArrayFields(previewData.value);

  // 提交表单
  await submitForm();
  switchToPreview();
};

const handlePreview = (data: ResumeData) => {
  // 使用深拷贝更新数据
  resumeData.value = JSON.parse(JSON.stringify(data));
  previewData.value = JSON.parse(JSON.stringify(data));

  // ID处理根据当前模式不同
  if (isAdding.value) {
    // 添加模式下，确保ID为空
    resumeData.value.id = '';
    previewData.value.id = '';
  } else if (resumeData.value.id) {
    // 编辑模式下，保留原ID
    const originalId = resumeData.value.id;
    previewData.value.id = originalId;
  }

  // 确保数组字段初始化正确
  ensureArrayFields(resumeData.value);
  ensureArrayFields(previewData.value);

  switchToPreview();
};

// 处理表单实时更新，用于同步到预览区域
const handleFormUpdate = (data: ResumeData) => {
  // 使用深拷贝确保数据完全替换，避免引用问题
  previewData.value = JSON.parse(JSON.stringify(data));

  // ID处理根据当前模式不同
  if (isAdding.value) {
    // 添加模式下，确保ID为空
    previewData.value.id = '';
    resumeData.value.id = '';
  } else if (resumeData.value.id) {
    // 编辑模式下，保留原ID
    previewData.value.id = resumeData.value.id;
  }

  // 同步更新resumeData，确保所有字段都正确同步
  // 在已处理ID之后再同步其他字段
  const tempData = JSON.parse(JSON.stringify(previewData.value));
  const currentId = resumeData.value.id; // 保存当前ID
  resumeData.value = tempData;

  // 确保ID按照当前模式处理
  if (isAdding.value) {
    resumeData.value.id = '';
  } else {
    resumeData.value.id = currentId;
  }

  // 确保数组字段始终是数组
  ensureArrayFields(resumeData.value);
  ensureArrayFields(previewData.value);
};

// 确保数组字段一定是数组
const ensureArrayFields = (data: ResumeData) => {
  // 确保这些字段始终是数组，避免渲染错误
  data.education = Array.isArray(data.education) ? data.education : [];
  data.workExperience = Array.isArray(data.workExperience) ? data.workExperience : [];
  data.certificates = Array.isArray(data.certificates) ? data.certificates : [];
  data.interestTags = Array.isArray(data.interestTags) ? data.interestTags : [];
  data.customSkills = Array.isArray(data.customSkills) ? data.customSkills : [];

  // 确保字符串字段有默认值
  data.selfEvaluation = data.selfEvaluation || '';

  // 验证和清理数据
  validateAndCleanData(data);
};

// 验证和清理数据的方法
const validateAndCleanData = (data: ResumeData) => {
  // 清理字符串输入，防止XSS攻击
  data.name = sanitizeInput(data.name);
  data.age = sanitizeInput(data.age);
  data.gender = sanitizeInput(data.gender);

  // 为location数组特殊处理
  data.location = sanitizeArrayField(data.location);

  data.experience = sanitizeInput(data.experience);
  data.phone = sanitizeInput(data.phone);
  data.email = sanitizeInput(data.email);
  data.avatar = sanitizeInput(data.avatar);
  data.jobTarget = sanitizeInput(data.jobTarget);
  data.expectedSalary = sanitizeInput(data.expectedSalary);
  data.targetCity = sanitizeArrayField(data.targetCity);
  data.availableTime = sanitizeInput(data.availableTime);
  data.selfEvaluation = sanitizeInput(data.selfEvaluation);

  // 验证并清理兴趣标签
  data.interestTags = sanitizeArrayField(data.interestTags);

  // 对于复杂对象数组，使用类型断言来解决类型问题

  // 安全地清理教育背景数组
  if (Array.isArray(data.education)) {
    data.education = data.education.map(item => {
      // 创建一个新对象来保存清理后的值
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const cleanItem: Record<string, any> = {};

      // 遍历原对象的所有键
      Object.keys(item).forEach(key => {
        // 对字符串类型的属性进行清理
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        if (typeof (item as Record<string, any>)[key] === 'string') {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          cleanItem[key] = sanitizeInput((item as Record<string, any>)[key]);
        } else {
          // 保留非字符串属性不变
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          cleanItem[key] = (item as Record<string, any>)[key];
        }
      });
      return cleanItem as typeof item;
    });
  }

  // 安全地清理工作经验数组
  if (Array.isArray(data.workExperience)) {
    data.workExperience = data.workExperience.map(item => {
      // 创建一个新对象来保存清理后的值
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const cleanItem: Record<string, any> = {};

      // 遍历原对象的所有键
      Object.keys(item).forEach(key => {
        // 对字符串类型的属性进行清理
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        if (typeof (item as Record<string, any>)[key] === 'string') {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          cleanItem[key] = sanitizeInput((item as Record<string, any>)[key]);
        } else {
          // 保留非字符串属性不变
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          cleanItem[key] = (item as Record<string, any>)[key];
        }
      });

      return cleanItem as typeof item;
    });
  }

  // 安全地清理证书数组
  if (Array.isArray(data.certificates)) {
    data.certificates = data.certificates.map(item => {
      // 创建一个新对象来保存清理后的值
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const cleanItem: Record<string, any> = {};

      // 遍历原对象的所有键
      Object.keys(item).forEach(key => {
        // 对字符串类型的属性进行清理
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        if (typeof (item as Record<string, any>)[key] === 'string') {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          cleanItem[key] = sanitizeInput((item as Record<string, any>)[key]);
        } else {
          // 保留非字符串属性不变
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          cleanItem[key] = (item as Record<string, any>)[key];
        }
      });

      return cleanItem as typeof item;
    });
  }

  // 安全地清理自定义技能数组
  if (Array.isArray(data.customSkills)) {
    data.customSkills = data.customSkills.map(item => {
      // 创建一个新对象来保存清理后的值
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const cleanItem: Record<string, any> = {};

      // 遍历原对象的所有键
      Object.keys(item).forEach(key => {
        // 对字符串类型的属性进行清理
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        if (typeof (item as Record<string, any>)[key] === 'string') {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          cleanItem[key] = sanitizeInput((item as Record<string, any>)[key]);
        } else {
          // 保留非字符串属性不变
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          cleanItem[key] = (item as Record<string, any>)[key];
        }
      });

      return cleanItem as typeof item;
    });
  }
};

// 输入清理函数，防止XSS攻击
const sanitizeInput = (input: string | undefined): string => {
  if (!input) return '';

  // 确保input是字符串
  if (typeof input !== 'string') {
    return '';
  }

  // 移除可能的脚本标签和危险属性
  return input
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/on\w+="[^"]*"/g, '')
    .replace(/javascript:/gi, '')
    .trim();
};

// 处理数组类型字段的函数，返回清理后的字符串数组
const sanitizeArrayField = (input: string[] | string | undefined): string[] => {
  // 如果是数组，对每个元素进行清理并过滤空值
  if (Array.isArray(input)) {
    return input.map(item => sanitizeInput(item)).filter(item => item.length > 0);
  }
  // 如果是字符串，转换为单元素数组
  else if (typeof input === 'string') {
    return input ? [sanitizeInput(input)] : [];
  }
  // 其他情况返回空数组
  else {
    return [];
  }
};

// 添加安全检查
const isResumeSafe = (data: ResumeData): boolean => {
  // 基本必填字段验证
  if (!data.name || data.name.length < 2) {
    ElMessage.warning('姓名不能为空且长度至少为2个字符');
    return false;
  }

  if (!data.phone || !/^1[3-9]\d{9}$/.test(data.phone)) {
    ElMessage.warning('请输入有效的手机号码');
    return false;
  }

  // 验证邮箱格式（如果已填写）
  if (data.email && !/^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/.test(data.email)) {
    ElMessage.warning('请输入有效的邮箱地址');
    return false;
  }

  // 验证数据长度限制，防止数据过大
  if (data.selfEvaluation && data.selfEvaluation.length > 2000) {
    ElMessage.warning('自我评价不能超过2000个字符');
    return false;
  }

  // 验证数组数据数量限制
  if (data.education && data.education.length > 10) {
    ElMessage.warning('教育背景条目不能超过10条');
    return false;
  }

  if (data.workExperience && data.workExperience.length > 15) {
    ElMessage.warning('工作经验条目不能超过15条');
    return false;
  }

  if (data.certificates && data.certificates.length > 20) {
    ElMessage.warning('证书条目不能超过20条');
    return false;
  }

  if (data.interestTags && data.interestTags.length > 30) {
    ElMessage.warning('兴趣标签不能超过30个');
    return false;
  }

  return true;
};

// 简历保存
const submitForm = async () => {
  // 如果不在编辑状态或是只读模式，则不执行保存
  if (isEditing.value || isReadOnly.value) return;

  // 防止重复提交
  if (loading.value) return;

  // 设置加载状态
  loading.value = true;

  try {
    // 确保数据结构完整并验证数据
    ensureArrayFields(resumeData.value);

    // 安全检查
    if (!isResumeSafe(resumeData.value)) {
      loading.value = false;
      return;
    }

    // 根据不同模式处理保存
    const result = isAdding.value
      ? await handleAddResume()
      : await handleUpdateResume();

    if (result.success) {
      emit('save', resumeData.value);
      ElMessage.success(result.message);
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    ElMessage.error('保存失败');
  } finally {
    loading.value = false;
  }
};

// 处理添加新简历
const handleAddResume = async () => {
  // 创建副本并确保ID为空
  const resumeToAdd = JSON.parse(JSON.stringify(resumeData.value));
  resumeToAdd.id = '';

  // 调用添加API
  const result = await addResume(resumeToAdd);
  const success = !!result && !!result.id;

  if (success) {
    // 更新ID并切换到编辑模式
    resumeData.value.id = result.id;
    previewData.value.id = result.id;
    isAdding.value = false;
  }

  return {
    success,
    message: success ? '新简历添加成功' : '添加失败'
  };
};

// 处理更新现有简历
const handleUpdateResume = async () => {
  // 确保有ID再更新
  if (!resumeData.value.id) {
    return {
      success: false,
      message: '缺少简历ID，无法更新'
    };
  }

  // 更新现有简历
  const success = await saveResumeData(resumeData.value);

  return {
    success,
    message: success ? '简历更新成功' : '更新失败'
  };
};

const resetForm = async () => {
  try {
    const data = await getResumeDetail(resumeData.value.id || '');

    // 使用深拷贝确保数据完全替换
    resumeData.value = JSON.parse(JSON.stringify(data));
    previewData.value = JSON.parse(JSON.stringify(data));

    // 确保数组字段初始化正确
    ensureArrayFields(resumeData.value);
    ensureArrayFields(previewData.value);

    // 增加重置键值，强制刷新编辑组件
    resetKey.value++;

    ElMessage.success('重置成功');
  } catch {
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

  // 确保数组字段初始化正确
  ensureArrayFields(previewData.value);
};

// 取消按钮处理函数
const handleCancel = () => {
  emit('cancel');
};

// 添加isPreviewDataReady计算属性
const isPreviewDataReady = computed(() => {
  // 确保previewData中的所有数组字段都已初始化
  return previewData.value &&
         Array.isArray(previewData.value.education) &&
         Array.isArray(previewData.value.workExperience) &&
         Array.isArray(previewData.value.certificates) &&
         Array.isArray(previewData.value.interestTags) &&
         Array.isArray(previewData.value.customSkills);
});
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

/* 合并了add-panel到edit-panel中，样式保持一致 */

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
