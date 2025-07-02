<template>
  <div class="git-version-history">
    <!-- 头部工具栏 -->
    <div class="toolbar">
      <div class="current-info">
        <el-tag type="success" size="small">
          <i class="el-icon-branch"></i>
          当前分支: {{ currentBranch }}
        </el-tag>
        <el-tag type="info" size="small" style="margin-left: 8px">
          HEAD: {{ headCommitId?.substring(0, 8) }}
        </el-tag>
      </div>

      <div class="actions">
        <el-button type="primary" size="small" @click="showCommitDialog">
          <i class="el-icon-plus"></i>
          手动提交
        </el-button>
        <el-button type="success" size="small" @click="showBranchDialog">
          <i class="el-icon-branch"></i>
          新建分支
        </el-button>
        <el-button type="warning" size="small" @click="showTagDialog">
          <i class="el-icon-price-tag"></i>
          创建标签
        </el-button>
      </div>
    </div>

    <!-- 分支切换器 -->
    <div class="branch-switcher">
      <el-select
        v-model="selectedBranch"
        @change="switchBranch"
        placeholder="选择分支"
        size="small"
      >
        <el-option
          v-for="branch in branches"
          :key="branch.name"
          :label="branch.name"
          :value="branch.name"
        >
          <span>{{ branch.name }}</span>
          <span v-if="branch.isDefault" style="color: #409eff; margin-left: 8px">(默认)</span>
        </el-option>
      </el-select>

      <el-button-group style="margin-left: 12px">
        <el-button size="small" @click="activeTab = 'commits'">
          <i class="el-icon-time"></i>
          提交历史
        </el-button>
        <el-button size="small" @click="activeTab = 'branches'">
          <i class="el-icon-branch"></i>
          分支管理
        </el-button>
        <el-button size="small" @click="activeTab = 'tags'">
          <i class="el-icon-price-tag"></i>
          标签管理
        </el-button>
        <el-button size="small" @click="activeTab = 'graph'">
          <i class="el-icon-picture-outline"></i>
          图形化
        </el-button>
      </el-button-group>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">
      <!-- 提交历史 -->
      <div v-if="activeTab === 'commits'" class="commits-view">
        <div v-if="commits.length === 0" class="empty-state">
          <p>暂无提交记录</p>
        </div>
        <div v-else class="commit-timeline">
          <div
            v-for="commit in commits"
            :key="commit.commitId"
            class="commit-item"
            :class="{ 'merge-commit': commit.isMergeCommit }"
          >
            <div class="commit-dot"></div>
            <div class="commit-content">
              <div class="commit-header">
                <div class="commit-info">
                  <span class="commit-id">{{ commit.commitId.substring(0, 8) }}</span>
                  <span class="commit-message">{{ commit.commitMessage }}</span>
                  <el-tag v-if="commit.isMergeCommit" type="info" size="small"> 合并提交 </el-tag>
                </div>
                <div class="commit-actions">
                  <el-button size="small" @click="previewCommitFn(commit)">预览</el-button>
                  <el-button size="small" type="warning" @click="resetToCommitFn(commit)">
                    重置到此
                  </el-button>
                </div>
              </div>
              <div class="commit-meta">
                <span class="author">{{ commit.author }}</span>
                <span class="time">{{ formatTime(commit.commitTime) }}</span>
                <span class="changes">{{ commit.changesSummary }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分支管理 -->
      <div v-if="activeTab === 'branches'" class="branches-view">
        <div class="branch-list">
          <div
            v-for="branch in branches"
            :key="branch.name"
            class="branch-item"
            :class="{ active: branch.name === currentBranch }"
          >
            <div class="branch-info">
              <i class="el-icon-branch"></i>
              <span class="branch-name">{{ branch.name }}</span>
              <el-tag v-if="branch.isDefault" type="success" size="small">默认</el-tag>
              <span class="branch-desc">{{ branch.description }}</span>
            </div>
            <div class="branch-actions">
              <el-button
                v-if="branch.name !== currentBranch"
                size="small"
                @click="switchToBranch(branch.name)"
              >
                切换
              </el-button>
              <el-button size="small" @click="showMergeDialog(branch)">合并</el-button>
              <el-button
                v-if="branch.name !== currentBranch && !branch.isDefault"
                size="small"
                type="danger"
                plain
                @click="handleDeleteBranch(branch)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 标签管理 -->
      <div v-if="activeTab === 'tags'" class="tags-view">
        <div class="tag-list">
          <div v-for="tag in tags" :key="tag.name" class="tag-item">
            <div class="tag-info">
              <i class="el-icon-price-tag"></i>
              <span class="tag-name">{{ tag.name }}</span>
              <span class="tag-commit">{{ tag.commitId.substring(0, 8) }}</span>
              <span class="tag-message">{{ tag.message }}</span>
            </div>
            <div class="tag-actions">
              <el-button size="small" @click="jumpToTag(tag)"> 跳转到此 </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 图形化展示 -->
      <div v-if="activeTab === 'graph'" class="graph-view">
        <GitGraph :commits="commits" :branches="branches" :tags="tags" />
      </div>
    </div>

    <!-- 提交对话框 -->
    <el-dialog v-model="commitDialogVisible" title="手动提交" width="500px">
      <el-form :model="commitForm" label-width="80px">
        <el-form-item label="提交信息">
          <el-input v-model="commitForm.message" placeholder="描述这次提交的内容" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="commitForm.author" placeholder="提交者" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="commitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCommit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 分支对话框 -->
    <el-dialog v-model="branchDialogVisible" title="创建新分支" width="500px">
      <el-form :model="branchForm" label-width="80px">
        <el-form-item label="分支名称">
          <el-input v-model="branchForm.name" placeholder="分支名称" />
        </el-form-item>
        <el-form-item label="分支描述">
          <el-input v-model="branchForm.description" placeholder="分支用途说明" />
        </el-form-item>
        <el-form-item label="基于提交">
          <el-input v-model="branchForm.fromCommitId" placeholder="留空表示基于当前HEAD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="branchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateBranch">创建</el-button>
      </template>
    </el-dialog>

    <!-- 标签对话框 -->
    <el-dialog v-model="tagDialogVisible" title="创建标签" width="500px">
      <el-form :model="tagForm" label-width="80px">
        <el-form-item label="标签名称">
          <el-input v-model="tagForm.name" placeholder="标签名称，如：v1.0" />
        </el-form-item>
        <el-form-item label="标签说明">
          <el-input v-model="tagForm.message" placeholder="标签用途说明" />
        </el-form-item>
        <el-form-item label="提交ID">
          <el-input v-model="tagForm.commitId" placeholder="留空表示标记当前HEAD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTag">创建</el-button>
      </template>
    </el-dialog>

    <!-- 合并对话框 -->
    <el-dialog v-model="mergeDialogVisible" title="合并分支" width="500px">
      <el-form :model="mergeForm" label-width="100px">
        <el-form-item label="源分支">
          <el-input v-model="mergeForm.sourceBranch" readonly />
        </el-form-item>
        <el-form-item label="目标分支">
          <el-select v-model="mergeForm.targetBranch" placeholder="选择目标分支">
            <el-option
              v-for="branch in branches"
              :key="branch.name"
              :label="branch.name"
              :value="branch.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="合并信息">
          <el-input v-model="mergeForm.message" placeholder="合并信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mergeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMergeBranch">合并</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      :title="`预览提交: ${previewCommit?.commitId?.substring(0, 8)}`"
      width="80%"
    >
      <div v-if="previewCommit" class="commit-preview">
        <div class="commit-details">
          <h4>提交信息: {{ previewCommit.commitMessage }}</h4>
          <p>作者: {{ previewCommit.author }}</p>
          <p>时间: {{ formatTime(previewCommit.commitTime) }}</p>
          <p>变更: {{ previewCommit.changesSummary }}</p>
        </div>
        <div class="commit-content-preview">
          <h4>简历内容:</h4>
          <div class="content-box">
            <h5>{{ previewCommit.title }}</h5>
            <div v-html="previewCommit.content" class="content-html"></div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCommitHistory,
  getBranches,
  getTags,
  commitChanges,
  createBranch,
  createTag,
  checkoutBranch,
  mergeBranch,
  resetToCommit,
  deleteBranch,
} from '@/api/resume/version'
import type {
  Commit,
  Branch,
  Tag,
  CommitRequest,
  BranchRequest,
  MergeRequest,
  TagRequest,
} from '@/api/resume/types'
import GitGraph from './graph/GitGraph.vue'

// Props
interface Props {
  resumeId: string
  visible: boolean
  currentTitle?: string
  currentContent?: string
}

const props = withDefaults(defineProps<Props>(), {
  currentTitle: '',
  currentContent: '',
})

// Emits
const emit = defineEmits<{
  'update:visible': [value: boolean]
  refresh: []
}>()

// Reactive data
const loading = ref(false)
const activeTab = ref('commits')
const commits = ref<Commit[]>([])
const branches = ref<Branch[]>([])
const tags = ref<Tag[]>([])
const selectedBranch = ref('')

// Current state
const currentBranch = computed(() => selectedBranch.value || 'main')
const headCommitId = computed(() => {
  const currentBranchObj = branches.value.find((b) => b.name === currentBranch.value)
  return currentBranchObj?.headCommitId || ''
})

// Dialog states
const commitDialogVisible = ref(false)
const branchDialogVisible = ref(false)
const tagDialogVisible = ref(false)
const mergeDialogVisible = ref(false)
const previewDialogVisible = ref(false)

// Forms
const commitForm = reactive<CommitRequest>({
  title: '',
  content: '',
  message: '',
  author: 'user',
})

const branchForm = reactive<BranchRequest>({
  name: '',
  description: '',
  fromCommitId: '',
})

const tagForm = reactive<TagRequest>({
  name: '',
  commitId: '',
  message: '',
})

const mergeForm = reactive<MergeRequest>({
  sourceBranch: '',
  targetBranch: '',
  message: '',
})

const previewCommit = ref<Commit | null>(null)

// Methods
const loadCommitHistory = async () => {
  try {
    loading.value = true
    commits.value = await getCommitHistory(props.resumeId, '')
  } catch (error) {
    console.error('获取提交历史失败:', error)
    ElMessage.error('获取提交历史失败')
  } finally {
    loading.value = false
  }
}

const loadBranches = async () => {
  try {
    branches.value = await getBranches(props.resumeId)
    if (branches.value.length > 0 && !selectedBranch.value) {
      const defaultBranch = branches.value.find((b) => b.isDefault) || branches.value[0]
      selectedBranch.value = defaultBranch.name
    }
  } catch (error) {
    console.error('获取分支列表失败:', error)
    ElMessage.error('获取分支列表失败')
  }
}

const loadTags = async () => {
  try {
    tags.value = await getTags(props.resumeId)
  } catch (error) {
    console.error('获取标签列表失败:', error)
    ElMessage.error('获取标签列表失败')
  }
}

const loadAllData = async () => {
  await Promise.all([loadBranches(), loadTags()])
  await loadCommitHistory()
}

const switchBranch = async (branchName: string) => {
  try {
    await checkoutBranch(props.resumeId, branchName)
    selectedBranch.value = branchName
    await loadCommitHistory()
    emit('refresh')
    ElMessage.success(`已切换到分支: ${branchName}`)
  } catch (error) {
    console.error('切换分支失败:', error)
    ElMessage.error('切换分支失败')
  }
}

const switchToBranch = (branchName: string) => {
  switchBranch(branchName)
}

const showCommitDialog = () => {
  commitForm.title = props.currentTitle
  commitForm.content = props.currentContent
  commitForm.message = ''
  commitForm.author = 'user'
  commitDialogVisible.value = true
}

const handleCommit = async () => {
  try {
    await commitChanges(props.resumeId, { ...commitForm })
    commitDialogVisible.value = false
    await loadCommitHistory()
    emit('refresh')
    ElMessage.success('提交成功')
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
  }
}

const showBranchDialog = () => {
  branchForm.name = ''
  branchForm.description = ''
  branchForm.fromCommitId = ''
  branchDialogVisible.value = true
}

const handleCreateBranch = async () => {
  try {
    await createBranch(props.resumeId, { ...branchForm })
    branchDialogVisible.value = false
    await loadBranches()
    ElMessage.success('分支创建成功')
  } catch (error) {
    console.error('创建分支失败:', error)
    ElMessage.error('创建分支失败')
  }
}

const showTagDialog = () => {
  tagForm.name = ''
  tagForm.commitId = ''
  tagForm.message = ''
  tagDialogVisible.value = true
}

const handleCreateTag = async () => {
  try {
    await createTag(props.resumeId, { ...tagForm })
    tagDialogVisible.value = false
    await loadTags()
    ElMessage.success('标签创建成功')
  } catch (error) {
    console.error('创建标签失败:', error)
    ElMessage.error('创建标签失败')
  }
}

const showMergeDialog = (branch: Branch) => {
  mergeForm.sourceBranch = branch.name
  mergeForm.targetBranch = currentBranch.value
  mergeForm.message = `合并分支 '${branch.name}' 到 '${currentBranch.value}'`
  mergeDialogVisible.value = true
}

const handleMergeBranch = async () => {
  try {
    const sourceBranchName = mergeForm.sourceBranch // 保存源分支名称
    await mergeBranch(props.resumeId, { ...mergeForm })
    mergeDialogVisible.value = false
    await loadAllData()
    emit('refresh')
    ElMessage.success('分支合并成功')

    // 弹出确认删除对话框
    await ElMessageBox.confirm(
      `分支 '${sourceBranchName}' 已成功合并。是否要删除这个分支？`,
      '清理分支',
      {
        confirmButtonText: '删除分支',
        cancelButtonText: '保留分支',
        type: 'success',
        center: true,
      },
    )

    // 如果用户确认，则调用删除接口
    await deleteBranch(props.resumeId, sourceBranchName)
    await loadBranches() // 重新加载分支列表
    ElMessage.info(`分支 '${sourceBranchName}' 已被删除`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('合并或删除分支失败:', error)
      ElMessage.error('操作失败')
    } else {
      ElMessage.info('已保留分支')
    }
  }
}

const previewCommitFn = (commit: Commit) => {
  previewCommit.value = commit
  previewDialogVisible.value = true
}

const resetToCommitFn = async (commit: Commit) => {
  try {
    await ElMessageBox.confirm(
      `确定要重置到提交 ${commit.commitId.substring(0, 8)} 吗？这将丢失当前的所有未提交更改。`,
      '确认重置',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await resetToCommit(props.resumeId, commit.commitId)
    await loadAllData()
    emit('refresh')
    ElMessage.success('重置成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置失败:', error)
      ElMessage.error('重置失败')
    }
  }
}

const formatTime = (timeStr: string) => {
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN')
}

const jumpToTag = async (tag: Tag) => {
  try {
    await resetToCommit(props.resumeId, tag.commitId)
    await loadAllData()
    emit('refresh')
    ElMessage.success('跳转成功')
  } catch (error) {
    console.error('跳转失败:', error)
    ElMessage.error('跳转失败')
  }
}

const handleDeleteBranch = async (branch: Branch) => {
  try {
    await ElMessageBox.confirm(`确定要删除分支 '${branch.name}' 吗？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteBranch(props.resumeId, branch.name)
    await loadBranches()
    ElMessage.success(`分支 '${branch.name}' 已成功删除`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除分支失败:', error)
      ElMessage.error('删除分支失败')
    }
  }
}

// Watch for visibility changes
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      loadAllData()
    }
  },
)

onMounted(() => {
  if (props.visible) {
    loadAllData()
  }
})
</script>

<style scoped lang="scss">
.git-version-history {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e4e7ed;

    .current-info {
      display: flex;
      align-items: center;
    }

    .actions {
      display: flex;
      gap: 8px;
    }
  }

  .branch-switcher {
    display: flex;
    align-items: center;
    margin-bottom: 20px;
    gap: 12px;
  }

  .content-area {
    flex: 1;
    overflow: hidden;
  }

  .commits-view {
    height: 100%;
    overflow-y: auto;

    .empty-state {
      text-align: center;
      padding: 40px;
      color: #909399;
    }

    .commit-timeline {
      position: relative;
      padding-left: 20px;

      &::before {
        content: '';
        position: absolute;
        left: 8px;
        top: 0;
        bottom: 0;
        width: 2px;
        background: #e4e7ed;
      }

      .commit-item {
        position: relative;
        margin-bottom: 24px;

        &.merge-commit .commit-dot {
          background: #f56c6c;
        }

        .commit-dot {
          position: absolute;
          left: -12px;
          top: 8px;
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #409eff;
          border: 2px solid #fff;
          box-shadow: 0 0 0 1px #e4e7ed;
        }

        .commit-content {
          background: #fff;
          border: 1px solid #e4e7ed;
          border-radius: 6px;
          padding: 16px;
          margin-left: 12px;

          .commit-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;

            .commit-info {
              display: flex;
              align-items: center;
              gap: 12px;

              .commit-id {
                font-family: monospace;
                background: #f5f7fa;
                padding: 2px 6px;
                border-radius: 3px;
                font-size: 12px;
              }

              .commit-message {
                font-weight: 500;
                color: #303133;
              }
            }

            .commit-actions {
              display: flex;
              gap: 8px;
            }
          }

          .commit-meta {
            display: flex;
            gap: 16px;
            font-size: 12px;
            color: #909399;

            .author {
              color: #606266;
            }
          }
        }
      }
    }
  }

  .branches-view,
  .tags-view {
    .branch-list,
    .tag-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .branch-item,
    .tag-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      border: 1px solid #e4e7ed;
      border-radius: 6px;
      background: #fff;

      &.active {
        border-color: #409eff;
        background: #ecf5ff;
      }

      .branch-info,
      .tag-info {
        display: flex;
        align-items: center;
        gap: 12px;
        flex: 1;

        .branch-name,
        .tag-name {
          font-weight: 500;
          color: #303133;
        }

        .branch-desc,
        .tag-message {
          color: #909399;
          font-size: 12px;
        }

        .tag-commit {
          font-family: monospace;
          background: #f5f7fa;
          padding: 2px 6px;
          border-radius: 3px;
          font-size: 12px;
        }
      }

      .branch-actions,
      .tag-actions {
        display: flex;
        gap: 8px;
      }
    }
  }

  .graph-view {
    width: 100%;
    height: 100%;
  }

  .commit-preview {
    .commit-details {
      margin-bottom: 20px;
      padding: 16px;
      background: #f5f7fa;
      border-radius: 6px;

      h4 {
        margin: 0 0 8px 0;
        color: #303133;
      }

      p {
        margin: 4px 0;
        color: #606266;
      }
    }

    .commit-content-preview {
      .content-box {
        border: 1px solid #e4e7ed;
        border-radius: 6px;
        padding: 16px;
        background: #fff;

        h5 {
          margin: 0 0 12px 0;
          color: #303133;
        }

        .content-html {
          color: #606266;
          line-height: 1.6;
        }
      }
    }
  }
}
</style>
