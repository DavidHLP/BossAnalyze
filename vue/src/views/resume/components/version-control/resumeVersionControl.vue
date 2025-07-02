<template>
  <div class="resume-version-control">
    <!-- 版本控制状态栏 -->
    <div class="version-status-bar">
      <el-card class="status-card">
        <div class="status-info">
          <div v-if="resumeVersionData" class="version-info">
            <el-tag type="success" size="large">
              <i class="el-icon-check"></i> 分支: {{ resumeVersionData.currentBranch }}
            </el-tag>
            <el-tag type="primary" size="large" class="ml-2">
              <i class="el-icon-time"></i> HEAD: {{ resumeVersionData.headCommit?.substring(0, 8) }}
            </el-tag>
          </div>
          <el-tag v-else type="info" size="large">
            <i class="el-icon-loading"></i> 正在加载版本信息...
          </el-tag>
        </div>
        <div class="action-buttons">
          <el-button
            type="primary"
            @click="showVersionPanel = !showVersionPanel"
            :disabled="!resumeVersionData"
          >
            {{ showVersionPanel ? '隐藏' : '显示' }}版本控制面板
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 版本控制面板 -->
    <el-drawer
      :model-value="showVersionPanel"
      title="简历版本控制"
      direction="rtl"
      size="85%"
      @update:model-value="emit('update:showVersionPanel', $event)"
    >
      <div v-if="resumeVersionData" class="version-panel-content">
        <el-container class="full-height">
          <el-aside width="50%" class="control-pane">
            <!-- 当前状态 -->
            <div class="current-status">
              <h3>当前状态</h3>
              <el-descriptions :column="1" size="small" border>
                <el-descriptions-item label="当前分支">
                  {{ resumeVersionData?.currentBranch }}
                </el-descriptions-item>
                <el-descriptions-item label="HEAD提交">
                  {{ resumeVersionData?.headCommit?.substring(0, 12) }}
                </el-descriptions-item>
                <el-descriptions-item label="储藏数量">
                  {{ resumeVersionData?.stashStack?.length || 0 }}
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 提交操作 -->
            <el-card class="mt-4">
              <template #header>
                <span>提交更改</span>
              </template>
              <el-form label-width="80px">
                <el-form-item label="提交信息">
                  <el-input
                    v-model="commitMessage"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入提交信息..."
                  />
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    @click="handleCommit"
                    :loading="loading"
                    :disabled="!commitMessage.trim()"
                  >
                    提交到 '{{ resumeVersionData?.currentBranch }}'
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <!-- 分支操作 -->
            <el-card class="mt-4">
              <template #header>
                <span>分支管理</span>
              </template>
              <el-form label-width="80px">
                <el-form-item label="当前分支">
                  <el-select
                    v-model="currentBranch"
                    @change="handleSwitchBranch"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="(commitId, branchName) in resumeVersionData?.branches"
                      :key="branchName"
                      :label="branchName"
                      :value="branchName"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="新分支名">
                  <el-input v-model="newBranchName" placeholder="输入新分支名称">
                    <template #append>
                      <el-button
                        @click="handleCreateBranch"
                        :loading="loading"
                        :disabled="!newBranchName.trim()"
                      >
                        创建
                      </el-button>
                    </template>
                  </el-input>
                </el-form-item>
                <el-form-item label="合并分支">
                  <el-select
                    v-model="branchToMerge"
                    placeholder="选择要合并的分支"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="(commitId, branchName) in resumeVersionData?.branches"
                      :key="branchName"
                      :label="branchName"
                      :value="branchName"
                      :disabled="branchName === resumeVersionData?.currentBranch"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-button @click="handleMerge" :loading="loading" :disabled="!branchToMerge">
                    合并到 {{ resumeVersionData?.currentBranch }}
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <!-- 储藏操作 -->
            <el-card class="mt-4">
              <template #header>
                <span>储藏管理</span>
              </template>
              <div class="stash-actions">
                <el-button @click="handleStash" :loading="loading"> 储藏当前更改 </el-button>
                <el-button
                  @click="handleStashPop"
                  :loading="loading"
                  :disabled="!resumeVersionData?.stashStack?.length"
                >
                  弹出储藏
                </el-button>
              </div>
              <el-table
                v-if="resumeVersionData?.stashStack?.length"
                :data="resumeVersionData.stashStack"
                stripe
                class="mt-3"
              >
                <el-table-column prop="message" label="储藏信息" />
                <el-table-column prop="timestamp" label="时间" width="180">
                  <template #default="{ row }">
                    {{ formatDate(row.timestamp) }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>

            <!-- 高级回溯功能 -->
            <el-card class="mt-4">
              <template #header>
                <span>🔄 高级回溯功能</span>
              </template>

              <!-- 快速回溯 -->
              <div class="rollback-section">
                <h4>⚡ 快速回溯</h4>
                <el-button
                  @click="loadRecentCommits"
                  :loading="loading"
                  type="primary"
                  size="small"
                  class="mb-2"
                >
                  加载最近提交
                </el-button>
                <el-table
                  v-if="recentCommits.length"
                  :data="recentCommits"
                  stripe
                  size="small"
                  class="mt-2"
                  max-height="200"
                >
                  <el-table-column prop="id" label="提交ID" width="100">
                    <template #default="{ row }">
                      {{ row.id.substring(0, 8) }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="message" label="提交信息" show-overflow-tooltip />
                  <el-table-column prop="commitTime" label="时间" width="120">
                    <template #default="{ row }">
                      {{ formatShortDate(row.commitTime) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="180">
                    <template #default="{ row }">
                      <el-button-group size="small">
                        <el-button @click="handleQuickCheckout(row.id)" :loading="loading">
                          检出
                        </el-button>
                        <el-button @click="handleReset(row.id)" :loading="loading" type="warning">
                          重置
                        </el-button>
                        <el-button @click="handleRevert(row.id)" :loading="loading" type="danger">
                          回滚
                        </el-button>
                      </el-button-group>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <!-- 回溯工具 -->
              <el-divider />
              <div class="rollback-tools">
                <h4>🛠️ 回溯工具</h4>
                <el-form label-width="80px" size="small">
                  <el-form-item label="提交ID">
                    <el-input v-model="targetCommitId" placeholder="输入要回溯的提交ID" clearable>
                      <template #append>
                        <el-button-group>
                          <el-button
                            @click="handleReset(targetCommitId)"
                            :loading="loading"
                            type="warning"
                          >
                            重置
                          </el-button>
                          <el-button
                            @click="handleRevert(targetCommitId)"
                            :loading="loading"
                            type="danger"
                          >
                            回滚
                          </el-button>
                        </el-button-group>
                      </template>
                    </el-input>
                  </el-form-item>
                </el-form>
              </div>

              <!-- 操作说明 -->
              <el-alert type="info" :closable="false" class="mt-3">
                <template #title>
                  <div class="rollback-help">
                    <p><strong>检出</strong>：切换到指定提交，不改变历史</p>
                    <p><strong>重置</strong>：将当前分支重置到指定提交，会改变历史 ⚠️</p>
                    <p><strong>回滚</strong>：创建新提交来撤销指定提交的更改，安全操作 ✅</p>
                  </div>
                </template>
              </el-alert>

              <!-- 调试功能 -->
              <el-divider />
              <div class="debug-section">
                <h4>🔧 调试功能</h4>
                <el-space direction="vertical" style="width: 100%">
                  <el-button
                    @click="performValidateVersionControl"
                    :loading="loading"
                    type="info"
                    size="small"
                  >
                    验证版本控制完整性
                  </el-button>

                  <el-button
                    @click="fixDuplicateIds"
                    :loading="loading"
                    type="warning"
                    size="small"
                  >
                    🔧 修复重复提交ID
                  </el-button>

                  <el-input v-model="debugCommitId" placeholder="输入提交ID进行诊断" size="small">
                    <template #append>
                      <el-button @click="debugCommit" :loading="loading" size="small">
                        诊断
                      </el-button>
                    </template>
                  </el-input>
                </el-space>

                <!-- 调试结果显示 -->
                <el-dialog
                  v-model="showDebugDialog"
                  title="调试信息"
                  width="80%"
                  :close-on-click-modal="false"
                >
                  <el-tabs v-model="debugActiveTab" type="border-card">
                    <!-- 验证结果 -->
                    <el-tab-pane label="完整性验证" name="validation" v-if="validationResult">
                      <div class="debug-content">
                        <el-alert
                          :type="validationResult.isValid ? 'success' : 'error'"
                          :title="
                            validationResult.isValid ? '✅ 版本控制数据完整' : '❌ 发现数据问题'
                          "
                          :closable="false"
                          class="mb-3"
                        />

                        <el-descriptions :column="2" border>
                          <el-descriptions-item label="总提交数">{{
                            validationResult.totalCommits
                          }}</el-descriptions-item>
                          <el-descriptions-item label="分支数">{{
                            validationResult.branchCount
                          }}</el-descriptions-item>
                          <el-descriptions-item label="储藏数">{{
                            validationResult.stashCount
                          }}</el-descriptions-item>
                          <el-descriptions-item label="孤立提交">{{
                            validationResult.orphanCommits
                          }}</el-descriptions-item>
                          <el-descriptions-item label="损坏提交">{{
                            validationResult.corruptedCommits
                          }}</el-descriptions-item>
                        </el-descriptions>

                        <div v-if="validationResult.errors?.length" class="mt-3">
                          <h5>❌ 错误:</h5>
                          <ul>
                            <li
                              v-for="error in validationResult.errors"
                              :key="error"
                              class="error-item"
                            >
                              {{ error }}
                            </li>
                          </ul>
                        </div>

                        <div v-if="validationResult.warnings?.length" class="mt-3">
                          <h5>⚠️ 警告:</h5>
                          <ul>
                            <li
                              v-for="warning in validationResult.warnings"
                              :key="warning"
                              class="warning-item"
                            >
                              {{ warning }}
                            </li>
                          </ul>
                        </div>
                      </div>
                    </el-tab-pane>

                    <!-- 提交诊断 -->
                    <el-tab-pane label="提交诊断" name="commit" v-if="commitDebugInfo">
                      <div class="debug-content">
                        <el-alert
                          :type="commitDebugInfo.success ? 'success' : 'error'"
                          :title="
                            commitDebugInfo.success ? '✅ 提交信息获取成功' : '❌ 提交诊断失败'
                          "
                          :closable="false"
                          class="mb-3"
                        />

                        <el-descriptions :column="2" border v-if="commitDebugInfo.success">
                          <el-descriptions-item label="简历存在">
                            <el-tag :type="commitDebugInfo.resumeExists ? 'success' : 'danger'">
                              {{ commitDebugInfo.resumeExists ? '是' : '否' }}
                            </el-tag>
                          </el-descriptions-item>
                          <el-descriptions-item label="版本控制启用">
                            <el-tag
                              :type="commitDebugInfo.hasVersionControl ? 'success' : 'danger'"
                            >
                              {{ commitDebugInfo.hasVersionControl ? '是' : '否' }}
                            </el-tag>
                          </el-descriptions-item>
                          <el-descriptions-item label="当前分支">{{
                            commitDebugInfo.currentBranch
                          }}</el-descriptions-item>
                          <el-descriptions-item label="HEAD提交">{{
                            commitDebugInfo.headCommit?.substring(0, 8)
                          }}</el-descriptions-item>
                          <el-descriptions-item label="提交存在">
                            <el-tag :type="commitDebugInfo.commitExists ? 'success' : 'danger'">
                              {{ commitDebugInfo.commitExists ? '是' : '否' }}
                            </el-tag>
                          </el-descriptions-item>
                          <el-descriptions-item label="是否初始提交">
                            <el-tag :type="commitDebugInfo.isInitialCommit ? 'warning' : 'success'">
                              {{ commitDebugInfo.isInitialCommit ? '是' : '否' }}
                            </el-tag>
                          </el-descriptions-item>
                        </el-descriptions>

                        <div v-if="commitDebugInfo.commitExists" class="mt-3">
                          <h5>📝 提交详情:</h5>
                          <el-descriptions :column="1" border>
                            <el-descriptions-item label="提交消息">{{
                              commitDebugInfo.commitMessage
                            }}</el-descriptions-item>
                            <el-descriptions-item label="提交类型">{{
                              commitDebugInfo.commitType
                            }}</el-descriptions-item>
                            <el-descriptions-item label="所属分支">{{
                              commitDebugInfo.commitBranch
                            }}</el-descriptions-item>
                            <el-descriptions-item label="提交时间">{{
                              formatDate(commitDebugInfo.commitTime)
                            }}</el-descriptions-item>
                            <el-descriptions-item label="父提交数量">{{
                              commitDebugInfo.parentCount
                            }}</el-descriptions-item>
                          </el-descriptions>

                          <div v-if="commitDebugInfo.parentCommitsInfo?.length" class="mt-3">
                            <h5>👨‍👩‍👧‍👦 父提交信息:</h5>
                            <el-table :data="commitDebugInfo.parentCommitsInfo" border>
                              <el-table-column prop="parentId" label="父提交ID" width="150">
                                <template #default="{ row }">
                                  {{ row.parentId.substring(0, 8) }}
                                </template>
                              </el-table-column>
                              <el-table-column label="存在状态" width="100">
                                <template #default="{ row }">
                                  <el-tag :type="row.parentExists ? 'success' : 'danger'">
                                    {{ row.parentExists ? '存在' : '缺失' }}
                                  </el-tag>
                                </template>
                              </el-table-column>
                              <el-table-column
                                prop="parentMessage"
                                label="父提交消息"
                                show-overflow-tooltip
                              />
                              <el-table-column prop="parentTime" label="父提交时间" width="180">
                                <template #default="{ row }">
                                  {{ row.parentTime ? formatDate(row.parentTime) : '-' }}
                                </template>
                              </el-table-column>
                            </el-table>
                          </div>
                        </div>

                        <div v-if="!commitDebugInfo.success" class="mt-3">
                          <el-alert type="error" :title="commitDebugInfo.error" :closable="false" />
                        </div>
                      </div>
                    </el-tab-pane>
                  </el-tabs>
                </el-dialog>
              </div>
            </el-card>
          </el-aside>

          <el-main class="history-pane">
            <!-- 提交历史图 -->
            <h3>提交历史</h3>
            <div class="commit-history">
              <VersionHistoryGraph :data="graphData" @node-click="handleNodeClick" />
            </div>

            <!-- 提交列表 -->
            <div class="commit-list mt-4">
              <h4>提交记录</h4>
              <el-table :data="commitHistory" stripe>
                <el-table-column prop="id" label="提交ID" width="120">
                  <template #default="{ row }">
                    {{ row.id.substring(0, 8) }}
                  </template>
                </el-table-column>
                <el-table-column prop="message" label="提交信息" />
                <el-table-column prop="branch" label="分支" width="100" />
                <el-table-column prop="commitTime" label="时间" width="180">
                  <template #default="{ row }">
                    {{ formatDate(row.commitTime) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200">
                  <template #default="{ row }">
                    <el-button-group size="small">
                      <el-button @click="handleCheckout(row.id)" :loading="loading">
                        检出
                      </el-button>
                      <el-button @click="handleReset(row.id)" :loading="loading" type="warning">
                        重置
                      </el-button>
                      <el-button @click="handleRevert(row.id)" :loading="loading" type="danger">
                        回滚
                      </el-button>
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-main>
        </el-container>
      </div>
      <div v-else class="loading-panel">
        <el-skeleton :rows="10" animated />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import VersionHistoryGraph from '@/components/version-control/VersionHistoryGraph.vue'
import useEditorStore from '@/store/modules/editor'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { NodeData, EdgeData } from '@antv/g6'
import * as versionApi from '@/api/resume/version'
import { getResumeById } from '@/api/resume/resume'
import type { Resume } from '@/api/resume/types'
import type { ResumeCommit } from '@/api/resume/version'

interface MyGraphData {
  nodes: NodeData[]
  edges: EdgeData[]
}

const route = useRoute()
const editorStore = useEditorStore()

const props = defineProps<{
  resumeId: string
  showVersionPanel: boolean
}>()

const emit = defineEmits(['update:showVersionPanel'])

// 响应式数据
const loading = ref(false)
const showVersionPanel = ref(false)
const resumeVersionData = ref<Resume | null>(null)
const commitHistory = ref<ResumeCommit[]>([])
const commitMessage = ref('')
const newBranchName = ref('')
const branchToMerge = ref('')
const currentBranch = ref('main')

// 新增：高级回溯功能相关数据
const recentCommits = ref<ResumeCommit[]>([])
const targetCommitId = ref('')

// 新增：调试功能相关数据
const debugCommitId = ref('')
const showDebugDialog = ref(false)
const debugActiveTab = ref('validation')
const validationResult = ref<{
  isValid: boolean
  totalCommits: number
  branchCount: number
  stashCount: number
  orphanCommits: number
  corruptedCommits: number
  errors?: string[]
  warnings?: string[]
} | null>(null)
const commitDebugInfo = ref<{
  success: boolean
  resumeExists: boolean
  hasVersionControl: boolean
  currentBranch: string
  headCommit?: string
  commitExists: boolean
  isInitialCommit: boolean
  commitMessage: string
  commitType: string
  commitBranch: string
  commitTime: Date
  parentCount: number
  parentCommitsInfo?: {
    parentId: string
    parentExists: boolean
    parentMessage: string
    parentTime: Date
  }[]
  error?: string
} | null>(null)

// 计算属性
const resumeId = computed(() => route.query.id as string)

const graphData = computed<MyGraphData>(() => {
  if (!commitHistory.value.length) {
    return { nodes: [], edges: [] }
  }

  const nodes: NodeData[] = commitHistory.value.map((commit) => ({
    id: commit.id,
    data: {
      label: commit.message,
      branch: commit.branch,
      type: commit.commitType,
    },
  }))

  const edges: EdgeData[] = []
  commitHistory.value.forEach((commit) => {
    commit.parentCommits.forEach((parentId) => {
      edges.push({
        source: parentId,
        target: commit.id,
        data: { branch: commit.branch },
      })
    })
  })

  return { nodes, edges }
})

// 提交更改
const handleCommit = async () => {
  try {
    loading.value = true
    const content = editorStore.MDContent

    if (!content || content.trim() === '') {
      ElMessage.warning('编辑器内容为空，无法提交')
      return
    }

    const commitId = await versionApi.commitChanges(resumeId.value, commitMessage.value, content)

    // 更新版本控制状态和历史
    await loadResumeVersionData()
    await loadCommitHistory()

    commitMessage.value = ''
    ElMessage.success(
      `提交成功！提交ID: ${commitId ? commitId.toString().substring(0, 8) : '未知'}`,
    )
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`提交失败: ${message}`)
  } finally {
    loading.value = false
  }
}

// 创建分支
const handleCreateBranch = async () => {
  try {
    loading.value = true
    await versionApi.createBranch(resumeId.value, newBranchName.value)
    await loadResumeVersionData()
    newBranchName.value = ''
    ElMessage.success('分支创建成功！')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`分支创建失败: ${message}`)
  } finally {
    loading.value = false
  }
}

// 切换分支
const handleSwitchBranch = async () => {
  try {
    loading.value = true
    await versionApi.switchBranch(resumeId.value, currentBranch.value)

    // 重新加载数据以同步内容
    await loadResumeVersionData()
    await loadCommitHistory()

    ElMessage.success(`已切换到分支: ${currentBranch.value}，编辑器内容已更新`)
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`分支切换失败: ${message}`)

    // 切换失败时恢复原分支显示
    if (resumeVersionData.value?.currentBranch) {
      currentBranch.value = resumeVersionData.value.currentBranch
    }
  } finally {
    loading.value = false
  }
}

// 合并分支
const handleMerge = async () => {
  try {
    await ElMessageBox.confirm(
      `确认将分支 '${branchToMerge.value}' 合并到 '${resumeVersionData.value?.currentBranch}' 吗？合并后将采用源分支的内容。`,
      '确认合并',
      { type: 'warning' },
    )

    loading.value = true
    await versionApi.mergeBranch(
      resumeId.value,
      branchToMerge.value,
      resumeVersionData.value!.currentBranch,
    )

    // 重新加载数据以同步合并后的内容
    await loadResumeVersionData()
    await loadCommitHistory()

    const mergedBranch = branchToMerge.value
    branchToMerge.value = ''
    ElMessage.success(`分支 '${mergedBranch}' 合并成功！编辑器内容已更新`)
  } catch (error: unknown) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : '未知错误'
      ElMessage.error(`分支合并失败: ${message}`)
    }
  } finally {
    loading.value = false
  }
}

// 储藏更改
const handleStash = async () => {
  try {
    loading.value = true
    const content = editorStore.MDContent
    await versionApi.stashChanges(resumeId.value, content)
    await loadResumeVersionData()
    ElMessage.success('储藏成功！')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`储藏失败: ${message}`)
  } finally {
    loading.value = false
  }
}

// 弹出储藏
const handleStashPop = async () => {
  try {
    loading.value = true
    const content = await versionApi.stashPop(resumeId.value)
    editorStore.setMDContent(content, (route.query.type as string) || '10front_end')
    await loadResumeVersionData()
    ElMessage.success('储藏弹出成功！')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`储藏弹出失败: ${message}`)
  } finally {
    loading.value = false
  }
}

// 检出到指定提交
const handleCheckout = async (commitId: string) => {
  try {
    await ElMessageBox.confirm(
      `确认检出到提交 ${commitId ? commitId.toString().substring(0, 8) : '未知'} 吗？这将改变当前的简历内容。`,
      '确认检出',
      { type: 'warning' },
    )

    loading.value = true
    await versionApi.checkoutCommit(resumeId.value, commitId)

    // 重新加载简历数据和版本控制状态
    await loadResumeVersionData()
    await loadCommitHistory()

    ElMessage.success('检出成功！编辑器内容已更新')
  } catch (error: unknown) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : '未知错误'
      ElMessage.error(`检出失败: ${message}`)
    }
  } finally {
    loading.value = false
  }
}

// 节点点击事件
const handleNodeClick = (nodeId: string) => {
  handleCheckout(nodeId)
}

// 加载简历版本数据
const loadResumeVersionData = async () => {
  if (!resumeId.value) return
  try {
    loading.value = true
    const resume = await getResumeById(resumeId.value)
    resumeVersionData.value = resume

    if (resume.content) {
      try {
        editorStore.setMDContent(resume.content, (route.query.type as string) || '10front_end')
      } catch (storeError) {
        console.warn('更新编辑器内容失败:', storeError)
      }
    }
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`加载简历数据失败: ${message}`)
    console.error('加载简历版本数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载提交历史
const loadCommitHistory = async () => {
  try {
    commitHistory.value = await versionApi.getCommitHistory(resumeId.value)
  } catch (error: unknown) {
    console.error('加载提交历史失败:', error)
  }
}

// 格式化日期
const formatDate = (date: Date | string) => {
  return new Date(date).toLocaleString()
}

// 格式化短日期
const formatShortDate = (date: Date | string) => {
  return new Date(date).toLocaleDateString()
}

// 加载最近提交记录
const loadRecentCommits = async () => {
  try {
    loading.value = true
    recentCommits.value = await versionApi.getRecentCommits(resumeId.value, 10)
    ElMessage.success('最近提交记录加载成功！')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`加载最近提交失败: ${message}`)
  } finally {
    loading.value = false
  }
}

// 快速检出（不显示确认对话框）
const handleQuickCheckout = async (commitId: string) => {
  try {
    loading.value = true
    await versionApi.checkoutCommit(resumeId.value, commitId)

    // 重新加载简历数据和版本控制状态
    await loadResumeVersionData()
    await loadCommitHistory()

    ElMessage.success(
      `快速检出到 ${commitId ? commitId.toString().substring(0, 8) : '未知'} 成功！`,
    )
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`快速检出失败: ${message}`)
  } finally {
    loading.value = false
  }
}

// 重置到指定提交
const handleReset = async (commitId: string) => {
  if (!commitId || !commitId.trim()) {
    ElMessage.warning('请输入有效的提交ID')
    return
  }

  try {
    await ElMessageBox.confirm(
      `⚠️ 确认重置当前分支到提交 ${commitId ? commitId.toString().substring(0, 8) : '未知'} 吗？\n\n这将会：\n• 将分支指针移动到指定提交\n• 丢失该提交之后的所有更改\n• 此操作不可逆转！`,
      '危险操作确认',
      {
        type: 'warning',
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
      },
    )

    loading.value = true
    await versionApi.resetToCommit(resumeId.value, commitId)

    // 重新加载数据
    await loadResumeVersionData()
    await loadCommitHistory()
    if (recentCommits.value.length) {
      await loadRecentCommits()
    }

    ElMessage.success(
      `重置到 ${commitId ? commitId.toString().substring(0, 8) : '未知'} 成功！编辑器内容已更新`,
    )
  } catch (error: unknown) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : '未知错误'
      ElMessage.error(`重置失败: ${message}`)
    }
  } finally {
    loading.value = false
  }
}

// 回滚指定提交
const handleRevert = async (commitId: string) => {
  if (!commitId || !commitId.trim()) {
    ElMessage.warning('请输入有效的提交ID')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认回滚提交 ${commitId ? commitId.toString().substring(0, 8) : '未知'} 吗？\n\n这将会：\n• 创建一个新提交来撤销指定提交的更改\n• 保留完整的提交历史\n• 这是安全的操作，可以再次回滚`,
      '确认回滚',
      {
        type: 'info',
        confirmButtonText: '确认回滚',
        cancelButtonText: '取消',
      },
    )

    loading.value = true
    const revertCommitId = await versionApi.revertCommit(resumeId.value, commitId)

    // 重新加载数据
    await loadResumeVersionData()
    await loadCommitHistory()
    if (recentCommits.value.length) {
      await loadRecentCommits()
    }

    ElMessage.success(
      `回滚成功！创建新提交: ${revertCommitId ? revertCommitId.toString().substring(0, 8) : '未知'}`,
    )
  } catch (error: unknown) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : '未知错误'
      ElMessage.error(`回滚失败: ${message}`)
    }
  } finally {
    loading.value = false
  }
}

// 验证版本控制完整性
const validateVersionControl = async () => {
  try {
    loading.value = true
    validationResult.value = await versionApi.validateVersionControl(resumeId.value)
    ElMessage.success('版本控制完整性验证成功！')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`验证失败: ${message}`)
  } finally {
    loading.value = false
  }
}

// 提交诊断
const debugCommit = async () => {
  if (!debugCommitId.value || !debugCommitId.value.trim()) {
    ElMessage.warning('请输入有效的提交ID')
    return
  }

  try {
    loading.value = true
    commitDebugInfo.value = await versionApi.getCommitDebugInfo(resumeId.value, debugCommitId.value)
    debugActiveTab.value = 'commit'
    showDebugDialog.value = true
    ElMessage.success('提交诊断成功！')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`诊断失败: ${message}`)
  } finally {
    loading.value = false
  }
}

// 执行验证并显示结果
const performValidateVersionControl = async () => {
  await validateVersionControl()
  if (validationResult.value) {
    debugActiveTab.value = 'validation'
    showDebugDialog.value = true
  }
}

// 修复重复提交ID
const fixDuplicateIds = async () => {
  try {
    await ElMessageBox.confirm(
      '⚠️ 确认修复重复提交ID吗？\n\n这将会：\n• 删除所有现有提交\n• 重新创建初始提交\n• 清理版本控制状态\n\n此操作不可逆转！',
      '修复重复ID',
      {
        type: 'warning',
        dangerouslyUseHTMLString: true,
        confirmButtonText: '确认修复',
        cancelButtonText: '取消',
      },
    )

    loading.value = true
    const result = await versionApi.fixDuplicateCommitIds(resumeId.value)

    if (result.success) {
      await loadResumeVersionData()
      await loadCommitHistory()
      const commitIdPreview = result.newInitialCommitId
        ? result.newInitialCommitId.toString().substring(0, 8)
        : '未知'
      ElMessage.success('重复提交ID修复成功！新的初始提交ID: ' + commitIdPreview)
    } else {
      ElMessage.error('修复失败: ' + result.error)
    }
  } catch (error: unknown) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : '未知错误'
      ElMessage.error(`修复失败: ${message}`)
    }
  } finally {
    loading.value = false
  }
}

// 监听分支变化
watch(
  () => resumeVersionData.value?.currentBranch,
  (newBranch) => {
    if (newBranch) {
      currentBranch.value = newBranch
    }
  },
)

// 监听resumeId变化，确保数据及时加载
watch(
  () => resumeId.value,
  async (newResumeId) => {
    if (newResumeId) {
      try {
        await loadResumeVersionData()
        if (resumeVersionData.value?.hasVersionControl) {
          await loadCommitHistory()
        }
      } catch (error) {
        console.error('加载简历数据失败:', error)
      }
    }
  },
  { immediate: false },
)

// 组件挂载时加载数据
onMounted(async () => {
  if (resumeId.value) {
    try {
      await loadResumeVersionData()
      // Since version control is always on, we can directly load history
      await loadCommitHistory()
    } catch (error) {
      console.error('组件初始化失败:', error)
      ElMessage.error('加载版本数据失败，请刷新重试')
    }
  }
})
</script>

<style lang="scss" scoped>
@use '@/assets/style/variables.scss' as *;

.resume-version-control {
  .version-status-bar {
    margin-bottom: 16px;

    .status-card {
      .el-card__body {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px 20px;
      }
    }

    .version-info {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .version-panel-content {
    height: 100%;

    .full-height {
      height: calc(100vh - 100px);
    }

    .control-pane {
      padding: 16px;
      border-right: 1px solid var(--el-border-color);
      overflow-y: auto;
    }

    .history-pane {
      padding: 16px;
      overflow-y: auto;
    }
  }

  .current-status {
    margin-bottom: 16px;
  }

  .stash-actions {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
  }

  .commit-history {
    height: 300px;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
  }

  .ml-2 {
    margin-left: 8px;
  }

  .mt-3 {
    margin-top: 12px;
  }

  .mt-4 {
    margin-top: 16px;
  }

  // 高级回溯功能样式
  .rollback-section {
    margin-bottom: 16px;

    h4 {
      margin: 0 0 12px 0;
      color: var(--el-text-color-primary);
      font-size: 14px;
      font-weight: 600;
    }

    .mb-2 {
      margin-bottom: 8px;
    }
  }

  .rollback-tools {
    h4 {
      margin: 0 0 12px 0;
      color: var(--el-text-color-primary);
      font-size: 14px;
      font-weight: 600;
    }
  }

  .rollback-help {
    font-size: 12px;
    line-height: 1.5;

    p {
      margin: 4px 0;

      strong {
        color: var(--el-text-color-primary);
      }
    }
  }

  // 调试功能样式
  .debug-section {
    margin-top: 16px;
  }

  .debug-content {
    padding: 16px;
  }

  .debug-content .el-alert {
    margin-bottom: 16px;
  }

  .debug-content .el-descriptions {
    margin-bottom: 16px;
  }

  .debug-content .el-table {
    margin-bottom: 16px;
  }

  .debug-content .el-tag {
    margin-left: 8px;
  }

  .debug-content .error-item {
    color: var(--el-color-error);
  }

  .debug-content .warning-item {
    color: var(--el-color-warning);
  }

  .mb-3 {
    margin-bottom: 16px;
  }

  // 响应式设计
  @media (max-width: 768px) {
    .version-panel-content {
      .full-height {
        height: calc(100vh - 60px);
      }

      .control-pane {
        width: 100%;
        border-right: none;
        border-bottom: 1px solid var(--el-border-color);
      }

      .history-pane {
        width: 100%;
      }
    }

    .rollback-section {
      .el-table {
        font-size: 12px;
      }
    }
  }
}
</style>
