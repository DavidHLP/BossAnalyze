<template>
  <el-container class="resume-recommendation">
    <el-main>
      <el-row>
        <el-col :span="24" class="mb-4">
          <el-card class="box-card shadow" :body-style="{ padding: '0px' }">
            <el-space direction="vertical" fill class="p-4 steps-container">
              <!-- 步骤条 -->
              <el-card class="steps-wrapper mb-4" shadow="hover">
                <el-steps :active="activeStep" finish-status="success" process-status="process" align-center size="small">
                  <el-step title="选择简历" description="选择您要分析的简历">
                    <template #icon><el-icon><SelectIcon /></el-icon></template>
                  </el-step>
                  <el-step title="分析简历" description="AI分析简历技能匹配">
                    <template #icon><el-icon><DataAnalysis /></el-icon></template>
                  </el-step>
                  <el-step title="职位推荐" description="查看匹配的推荐职位">
                    <template #icon><el-icon><Promotion /></el-icon></template>
                  </el-step>
                </el-steps>
              </el-card>

              <!-- 步骤内容区域 -->
              <el-row class="mt-4">
                <el-col :span="24">
                  <el-space class="step-content-wrapper" direction="vertical" fill>
                    <!-- 第一步：选择简历 -->
                    <div v-if="activeStep === 1" style="width: 100%; display: flex;">
                      <SelectResume
                        :resume-list="resumeList"
                        :selected-resume-id="selectedResumeId"
                        :pagination="pagination"
                        @update:selected-resume-id="val => selectedResumeId = val as string"
                        @page-change="handlePageChange"
                        @create-resume="goToCreateResume"
                        @next="nextStep"
                        class="w-100"
                        style="flex: 1;"
                      />
                    </div>

                    <!-- 第二步：分析简历 -->
                    <div v-if="activeStep === 2">
                      <AnalyzeResume
                        :resume-data="currentResume"
                        :analyzing="analyzing"
                        :core-skills="coreSkills"
                        @prev="prevStep"
                        @next="nextStep"
                      />
                    </div>

                    <!-- 第三步：职位推荐 -->
                    <div v-if="activeStep === 3">
                      <JobRecommendation
                        :jobs="similarJobs"
                        :loading="loading"
                        @prev="prevStep"
                        @reset="resetProcess"
                        @open-job-link="openJobLink"
                      />
                    </div>
                  </el-space>
                </el-col>
              </el-row>
            </el-space>
          </el-card>
        </el-col>
      </el-row>
    </el-main>

    <!-- 页面底部 -->
    <el-footer class="footer-info">
      <el-row>
        <el-col :span="24" class="text-center text-muted small py-3">
          <el-text class="mb-0" type="info" size="small">简历推荐系统 © {{ new Date().getFullYear() }} 求职分析助手</el-text>
        </el-col>
      </el-row>
    </el-footer>
  </el-container>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getResumeList, getResumeDetail } from '@/api/resume/resume'
import { getUserSimilarity } from '@/api/ai/ai'
import type { ResumeData } from '@/api/resume/resume.d'
import type { UserSimilarity, SimilarityRequest } from '@/api/ai/ai.d'
import AnalyzeResume from './components/AnalyzeResume.vue'
import JobRecommendation from './components/JobRecommendation.vue'
import SelectResume from './components/SelectResume.vue'
import { Select as SelectIcon, DataAnalysis, Promotion } from '@element-plus/icons-vue'

const router = useRouter()
const activeStep = ref(1)
const resumeList = ref<ResumeData[]>([])
const selectedResumeId = ref<string>('')
const currentResume = ref<ResumeData>({} as ResumeData)
const analyzing = ref(false)
const loading = ref(false)
const coreSkills = ref<string[]>([])
const similarJobs = ref<UserSimilarity[]>([])
const pagination = ref({
  page: 1,
  size: 10,
  total: 0
})

// 加载用户的简历列表
const loadResumeList = async () => {
  try {
    const response = await getResumeList({
      page: pagination.value.page - 1,
      size: pagination.value.size
    })
    resumeList.value = response.content || []
    pagination.value.total = response.totalElements || 0
  } catch (error) {
    console.error('加载简历列表失败:', error)
    resumeList.value = []
  }
}

// 处理分页变化
const handlePageChange = (page: number) => {
  pagination.value.page = page
  loadResumeList()
}

// 下一步
const nextStep = async () => {
  if (activeStep.value === 1) {
    // 从第一步到第二步，加载选中的简历详情
    if (!selectedResumeId.value) return

    try {
      analyzing.value = true
      currentResume.value = await getResumeDetail(selectedResumeId.value)

      analyzing.value = false
    } catch (error) {
      console.error('分析简历失败:', error)
      analyzing.value = false
      return // 如果发生错误，不进行下一步
    }
  } else if (activeStep.value === 2) {
    // 从第二步到第三步，获取职位推荐
    try {
      loading.value = true
      // 使用简历的ID作为resumeId
      const resumeId = currentResume.value.id || ''

      const similarityRequest = {
        city: currentResume.value.targetCity || [],
        position: currentResume.value.jobTarget || '',
        resume: JSON.stringify(currentResume.value),
        resumeId: resumeId
      }

      await pollForResults(similarityRequest as SimilarityRequest )
      loading.value = false
    } catch (error) {
      console.error('获取职位推荐失败:', error)
      loading.value = false
      return // 如果发生错误，不进行下一步
    }
  }

  activeStep.value++
}

// 上一步
const prevStep = () => {
  activeStep.value--
}

// 重置流程
const resetProcess = () => {
  activeStep.value = 1
  selectedResumeId.value = ''
  currentResume.value = {} as ResumeData
  coreSkills.value = []
  similarJobs.value = []
}

// 跳转到创建简历页面
const goToCreateResume = () => {
  router.push('/console/resume/create')
}

// 轮询获取AI处理结果
const pollForResults = async (similarityRequest: SimilarityRequest) => {
  const maxAttempts = 60 // 最多轮询60次（5分钟）
  const interval = 5000 // 每5秒轮询一次

  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    try {
      const response = await getUserSimilarity(similarityRequest)

      if (response.status === 'completed' && response.data) {
        similarJobs.value = response.data
        return
      } else if (response.status === 'error') {
        throw new Error(response.message)
      }

      // 如果状态是loading，等待后继续轮询
      if (response.status === 'loading') {
        await new Promise(resolve => setTimeout(resolve, interval))
        continue
      }
    } catch (error) {
      if (attempt === maxAttempts - 1) {
        throw error
      }
      await new Promise(resolve => setTimeout(resolve, interval))
    }
  }

  throw new Error('AI处理超时，请稍后重试')
}

// 打开职位链接
const openJobLink = (url: string) => {
  window.open(url, '_blank')
}

onMounted(() => {
  loadResumeList()
})
</script>

<style scoped>
.resume-recommendation {
  padding: 20px;
  background-color: #f9fafc;
  min-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}

.steps-container {
  padding: 20px !important;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.steps-wrapper {
  padding: 15px 10px;
  background-color: #f9fafc;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.step-content-wrapper {
  min-height: 600px;
  transition: all 0.3s;
  width: 100%;
}

.w-100 {
  width: 100% !important;
}

:deep(.el-step__title) {
  font-weight: 500;
  font-size: 14px;
}

:deep(.el-step__description) {
  font-size: 12px;
}

:deep(.el-step.is-process .el-step__title) {
  font-weight: 600;
}

:deep(.el-card) {
  border-radius: 10px;
  overflow: hidden;
}

.footer-info {
  margin-top: auto;
  border-top: 1px solid #ebeef5;
  background-color: #fff;
}

.mb-4 {
  margin-bottom: 1.5rem;
}

.mt-4 {
  margin-top: 1.5rem;
}

.text-center {
  text-align: center;
}

.text-muted {
  color: #909399;
}

.small {
  font-size: 0.875rem;
}

.py-3 {
  padding-top: 1rem;
  padding-bottom: 1rem;
}

.mb-0 {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .steps-wrapper {
    padding: 10px 5px;
  }

  :deep(.el-step__title) {
    font-size: 12px;
  }

  :deep(.el-step__description) {
    display: none;
  }
}
</style>
