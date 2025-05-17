<template>
  <div class="resume-edit-container">
    <el-form :model="resumeForm" label-position="top" class="resume-form">
      <!-- 基本信息（固定位置） -->
      <el-card class="form-section mb-4">
        <template #header>
          <div class="section-header-wrapper">
            <h3 class="section-title">基本信息</h3>
          </div>
        </template>
        <BasicInfoEdit
          v-model:basicInfo="basicInfoComputed"
        />
      </el-card>

      <!-- 求职意向（固定位置） -->
      <el-card class="form-section mb-4">
        <template #header>
          <div class="section-header-wrapper">
            <h3 class="section-title">求职意向</h3>
          </div>
        </template>
        <JobIntentionEdit
          v-model:jobIntention="jobIntentionComputed"
        />
      </el-card>

      <!-- 可拖动部分 -->
      <draggable
        v-model="draggableSections"
        item-key="id"
        ghost-class="ghost-section"
        handle=".drag-handle"
        class="draggable-container"
        @end="onDragEnd"
      >
        <template #item="{element}">
          <el-card class="form-section draggable-section mb-4">
            <template #header>
              <div class="section-header-wrapper">
                <h3 class="section-title">{{ element.title }}</h3>
                <el-tooltip content="拖动调整顺序" placement="top" :effect="'light'">
                  <el-icon class="drag-handle"><Rank /></el-icon>
                </el-tooltip>
              </div>
            </template>

            <!-- 教育背景 -->
            <EducationalBackgroundEdit v-if="element.type === 'education'" v-model:education="resumeForm.education" />

            <!-- 工作经验 -->
            <WorkExperienceEdit v-if="element.type === 'workExperience'" v-model:workExperience="resumeForm.workExperience" />

            <!-- 个人证书 -->
            <CertificateEdit v-if="element.type === 'certificates'" v-model:certificates="resumeForm.certificates" />

            <!-- 兴趣爱好 -->
            <InterestTagsEdit v-if="element.type === 'interestTags'" v-model:interestTags="resumeForm.interestTags" />

            <!-- 自我评价 -->
            <SelfEvaluationEdit v-if="element.type === 'selfEvaluation'" v-model:modelValue="resumeForm.selfEvaluation" />
          </el-card>
        </template>
      </draggable>
    </el-form>
  </div>
</template>

<script lang="ts" setup>
import { reactive, onMounted, defineProps, defineEmits, computed, watch, ref } from 'vue'
import { Rank } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import BasicInfoEdit from './components/edit/BasicInfoEdit.vue'
import JobIntentionEdit from './components/edit/JobIntentionEdit.vue'
import CertificateEdit from './components/edit/CertificateEdit.vue'
import InterestTagsEdit from './components/edit/InterestTagsEdit.vue'
import EducationalBackgroundEdit from './components/edit/EducationalBackgroundEdit.vue'
import SelfEvaluationEdit from './components/edit/Self-EvaluationEdit.vue'
import WorkExperienceEdit from './components/edit/WorkExperienceEdit.vue'
import type { ResumeData } from '@/api/resume/resume.d'

// 定义拖拽部分的接口
interface DraggableSection {
  id: string;
  type: string;
  title: string;
}

// Props 定义
const props = defineProps<{
  resumeData?: ResumeData
}>()

// Emits 定义
const emit = defineEmits<{
  (e: 'save', data: ResumeData): void
  (e: 'preview', data: ResumeData): void
  (e: 'update:form', data: ResumeData): void
}>()

// 响应式状态
const resumeForm = reactive<ResumeData>({
  id: '',
  name: '',
  age: '',
  gender: '',
  location: '',
  experience: '',
  phone: '',
  email: '',
  avatar: '',
  jobTarget: '',
  expectedSalary: '',
  targetCity: '',
  availableTime: '',
  education: [],
  workExperience: [],
  languageSkills: '',
  professionalSkills: '',
  computerSkills: '',
  certificates: [],
  interestTags: [],
  selfEvaluation: '',
  customSkills: [],
  // 添加新字段用于存储拖拽顺序
  sectionOrder: []
})

// 可拖拽部分列表
const draggableSections = ref<DraggableSection[]>([
  { id: '1', type: 'education', title: '教育背景' },
  { id: '2', type: 'workExperience', title: '工作经验' },
  { id: '3', type: 'certificates', title: '个人证书' },
  { id: '4', type: 'interestTags', title: '兴趣爱好' },
  { id: '5', type: 'selfEvaluation', title: '自我评价' }
])

// 拖拽结束后更新表单数据和顺序
const onDragEnd = () => {
  // 获取当前拖动后的顺序，保存到resumeForm中
  resumeForm.sectionOrder = draggableSections.value.map(section => section.type);

  // 确保id字段不会被覆盖（保留原值）
  const formData = JSON.parse(JSON.stringify(resumeForm));

  // 发送更新事件
  emit('update:form', formData);
}

// 创建计算属性分离表单数据
const basicInfoComputed = computed({
  get: () => ({
    name: resumeForm.name,
    age: resumeForm.age,
    gender: resumeForm.gender,
    location: resumeForm.location,
    experience: resumeForm.experience,
    phone: resumeForm.phone,
    email: resumeForm.email,
    avatar: resumeForm.avatar
  }),
  set: (val) => {
    Object.assign(resumeForm, val);
    // 触发表单更新事件
    emit('update:form', JSON.parse(JSON.stringify(resumeForm)));
  }
});

const jobIntentionComputed = computed({
  get: () => ({
    jobTarget: resumeForm.jobTarget,
    expectedSalary: resumeForm.expectedSalary,
    targetCity: resumeForm.targetCity,
    availableTime: resumeForm.availableTime
  }),
  set: (val) => {
    Object.assign(resumeForm, val);
    // 触发表单更新事件
    emit('update:form', JSON.parse(JSON.stringify(resumeForm)));
  }
});

// 初始化表单数据
onMounted(() => {
  if (props.resumeData && Object.keys(props.resumeData).length > 0) {
    initFormData()
  }

  // 如果有保存的顺序，则恢复顺序
  if (resumeForm.sectionOrder && resumeForm.sectionOrder.length > 0) {
    restoreSectionOrder();
  }
})

// 恢复已保存的组件顺序
const restoreSectionOrder = () => {
  if (!resumeForm.sectionOrder || resumeForm.sectionOrder.length === 0) return;

  // 临时存储当前顺序
  const tempSections = [...draggableSections.value];
  // 清空当前列表
  draggableSections.value = [];

  // 按保存的顺序重建列表
  resumeForm.sectionOrder.forEach(type => {
    const section = tempSections.find(s => s.type === type);
    if (section) {
      draggableSections.value.push(section);
    }
  });

  // 添加可能的新组件（防止数据结构变化导致的问题）
  tempSections.forEach(section => {
    if (!draggableSections.value.some(s => s.type === section.type)) {
      draggableSections.value.push(section);
    }
  });
}

// 方法
const initFormData = () => {
  if (!props.resumeData) return;

  const data = props.resumeData;

  // 确保ID被正确同步
  resumeForm.id = data.id || '';

  // 基本信息
  resumeForm.name = data.name || '';
  resumeForm.age = data.age || '';
  resumeForm.gender = data.gender || '';
  resumeForm.location = data.location || '';
  resumeForm.experience = data.experience || '';
  resumeForm.phone = data.phone || '';
  resumeForm.email = data.email || '';
  resumeForm.avatar = data.avatar || '';

  // 求职意向
  resumeForm.jobTarget = data.jobTarget || '';
  resumeForm.expectedSalary = data.expectedSalary || '';
  resumeForm.targetCity = data.targetCity || '';
  resumeForm.availableTime = data.availableTime || '';

  // 技能部分
  resumeForm.languageSkills = data.languageSkills || '';
  resumeForm.professionalSkills = data.professionalSkills || '';
  resumeForm.computerSkills = data.computerSkills || '';
  resumeForm.selfEvaluation = data.selfEvaluation || '';

  // 处理证书列表
  resumeForm.certificates = Array.isArray(data.certificates) ? [...data.certificates] : [];

  // 数组类型的字段需要确保是数组
  resumeForm.interestTags = Array.isArray(data.interestTags) ? [...data.interestTags] : [];
  resumeForm.education = Array.isArray(data.education) ? [...data.education] : [];
  resumeForm.workExperience = Array.isArray(data.workExperience) ? [...data.workExperience] : [];

  // 自定义技能
  resumeForm.customSkills = Array.isArray(data.customSkills) ? [...data.customSkills] : [];

  // 拖拽顺序
  resumeForm.sectionOrder = Array.isArray(data.sectionOrder) ? [...data.sectionOrder] : [];

  // 初始化后触发一次更新
  emit('update:form', JSON.parse(JSON.stringify(resumeForm)));
}

// 监听器
watch(() => resumeForm.education, () => {
  emit('update:form', JSON.parse(JSON.stringify(resumeForm)));
}, { deep: true });

watch(() => resumeForm.workExperience, () => {
  emit('update:form', JSON.parse(JSON.stringify(resumeForm)));
}, { deep: true });

watch(() => resumeForm.certificates, () => {
  emit('update:form', JSON.parse(JSON.stringify(resumeForm)));
}, { deep: true });

watch(() => resumeForm.interestTags, () => {
  emit('update:form', JSON.parse(JSON.stringify(resumeForm)));
}, { deep: true });

watch(() => resumeForm.selfEvaluation, () => {
  emit('update:form', JSON.parse(JSON.stringify(resumeForm)));
}, { deep: true });

// 监听拖拽顺序变化
watch(() => draggableSections.value, () => {
  resumeForm.sectionOrder = draggableSections.value.map(section => section.type);
  emit('update:form', JSON.parse(JSON.stringify(resumeForm)));
}, { deep: true });
</script>

<style scoped>
.resume-edit-container {
  padding: 20px;
}

.resume-form {
  width: 100%;
}

.form-section {
  transition: all 0.3s ease;
  margin-bottom: 24px;
  border-radius: 12px;
}

.section-header-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

:deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 2px solid #3B82F6;
  background-color: #f8fafc;
}

:deep(.el-card__body) {
  padding: 20px;
}

.mb-4 {
  margin-bottom: 1rem;
}

/* 拖拽样式 */
.draggable-container {
  width: 100%;
}

.draggable-section {
  position: relative;
  transition: transform 0.3s ease;
}

.draggable-section:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08);
}

.drag-handle {
  cursor: move;
  font-size: 20px;
  color: #3B82F6;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.drag-handle:hover {
  background-color: rgba(59, 130, 246, 0.1);
}

.ghost-section {
  opacity: 0.6;
  background: #e0f2fe;
  border: 2px dashed #3B82F6;
  border-radius: 12px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

@media (max-width: 768px) {
  .resume-edit-container {
    padding: 12px;
  }

  :deep(.el-card__header) {
    padding: 12px 16px;
  }

  :deep(.el-card__body) {
    padding: 16px;
  }
}
</style>
