<template>
  <div class="resume-version-control">
    <!-- 版本控制状态栏 -->
    <div class="version-status-bar">
      <el-card class="status-card">
        <div class="status-info">
          <el-tag v-if="!resumeVersionData?.hasVersionControl" type="info" size="large">
            <i class="el-icon-info"></i> 未启用版本控制
          </el-tag>
          <div v-else class="version-info">
            <el-tag type="success" size="large">
              <i class="el-icon-check"></i> 分支: {{ resumeVersionData.currentBranch }}
            </el-tag>
            <el-tag type="primary" size="large" class="ml-2">
              <i class="el-icon-time"></i> HEAD: {{ resumeVersionData.headCommit?.substring(0, 8) }}
            </el-tag>
          </div>
        </div>
        <div class="action-buttons">
          <el-button
            v-if="!resumeVersionData?.hasVersionControl"
            type="primary"
            @click="initVersionControl"
            :loading="loading"
          >
            启用版本控制
          </el-button>
          <el-button v-else type="primary" @click="showVersionPanel = !showVersionPanel">
            {{ showVersionPanel ? '隐藏' : '显示' }}版本控制面板
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 版本控制面板 -->
    <el-drawer
      v-model="showVersionPanel"
      title="简历版本控制"
      direction="rtl"
      size="60%"
      :with-header="true"
    >
      <div class="version-panel-content">
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
                <el-table-column label="操作" width="120">
                  <template #default="{ row }">
                    <el-button size="small" @click="handleCheckout(row.id)" :loading="loading">
                      检出
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-main>
        </el-container>
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

// 响应式数据
const loading = ref(false)
const showVersionPanel = ref(false)
const resumeVersionData = ref<Resume | null>(null)
const commitHistory = ref<ResumeCommit[]>([])
const commitMessage = ref('')
const newBranchName = ref('')
const branchToMerge = ref('')
const currentBranch = ref('main')

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

// 初始化版本控制
const initVersionControl = async () => {
  try {
    loading.value = true
    const content = editorStore.MDContent
    const result = await versionApi.initVersionControl(resumeId.value, content)
    resumeVersionData.value = result
    await loadCommitHistory()
    ElMessage.success('版本控制初始化成功！')
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '未知错误'
    ElMessage.error(`初始化失败: ${message}`)
  } finally {
    loading.value = false
  }
}

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
    ElMessage.success(`提交成功！提交ID: ${commitId.substring(0, 8)}`)
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
      `确认检出到提交 ${commitId.substring(0, 8)} 吗？这将改变当前的简历内容。`,
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
      // 首先加载简历版本数据以检查是否启用版本控制
      await loadResumeVersionData()

      // 如果启用了版本控制，则加载提交历史
      if (resumeVersionData.value?.hasVersionControl) {
        await loadCommitHistory()
      }
    } catch (error) {
      console.error('组件初始化失败:', error)
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
}
</style>
