<template>
  <el-container class="version-control-wrapper">
    <el-aside width="50%" class="control-pane">
      <div class="status-bar">
        <el-tag type="info" size="large"> <i class="el-icon-info"></i> {{ statusMessage }} </el-tag>
      </div>
      <Editor v-model="currentContent" class="editor-instance" />
      <el-tabs type="border-card" class="action-tabs">
        <el-tab-pane label="Commit">
          <el-input
            v-model="commitMessage"
            type="textarea"
            :rows="4"
            placeholder="请输入提交信息..."
          ></el-input>
          <el-button type="primary" @click="handleCommit" class="action-button">
            提交到 '{{ currentBranch }}'
          </el-button>
        </el-tab-pane>

        <el-tab-pane label="Branch">
          <el-form label-width="100px" class="action-form">
            <el-form-item label="当前分支">
              <el-select
                v-model="currentBranch"
                @change="handleSwitchBranch"
                placeholder="选择分支"
              >
                <el-option
                  v-for="b in Object.keys(branches)"
                  :key="b"
                  :label="b"
                  :value="b"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="创建新分支">
              <el-input v-model="newBranchName" placeholder="输入新分支名称">
                <template #append>
                  <el-button @click="handleCreateBranch">创建</el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item label="合并/变基">
              <el-select v-model="branchToActOn" placeholder="选择源分支">
                <el-option
                  v-for="b in Object.keys(branches).filter((br) => br !== currentBranch)"
                  :key="b"
                  :label="b"
                  :value="b"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button-group>
                <el-button @click="handleMerge" :disabled="!branchToActOn"
                  >合并到 {{ currentBranch }}</el-button
                >
                <el-button type="warning" @click="handleRebase" :disabled="!branchToActOn"
                  >变基 {{ currentBranch }}</el-button
                >
              </el-button-group>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="Advanced">
          <div class="advanced-actions">
            <el-button @click="handleCherryPick" :disabled="isDetachedHead || !selectedCommit"
              >遴选选中提交</el-button
            >
            <el-button @click="handleReset" :disabled="isDetachedHead || !selectedCommit"
              >重置到选中提交</el-button
            >
            <el-button @click="handleStash">储藏当前修改</el-button>
            <el-button @click="handleStashPop" :disabled="stashStack.length === 0"
              >弹出储藏</el-button
            >
          </div>
          <el-table :data="stashStack" stripe class="stash-table" v-if="stashStack.length > 0">
            <el-table-column prop="message" label="储藏信息"></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-aside>
    <el-main class="graph-pane">
      <VersionHistoryGraph :data="graphData as any" @node-click="handleNodeClick" />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Editor from '@/components/version-control/editor/editor.vue'
import VersionHistoryGraph from '@/components/version-control/VersionHistoryGraph.vue'
import { useGit } from './hook'
import {
  ElContainer,
  ElAside,
  ElMain,
  ElTabs,
  ElTabPane,
  ElInput,
  ElButton,
  ElButtonGroup,
  ElSelect,
  ElOption,
  ElForm,
  ElFormItem,
  ElTag,
  ElTable,
  ElTableColumn,
} from 'element-plus'

const {
  graphData,
  branches,
  currentBranch,
  currentContent,
  isDetachedHead,
  statusMessage,
  stashStack,
  commit,
  createBranch,
  switchBranch,
  merge,
  checkout,
  reset,
  stash,
  stashPop,
  cherryPick,
  rebase,
} = useGit()

// UI State
const commitMessage = ref('')
const newBranchName = ref('')
const branchToActOn = ref('')
const selectedCommit = ref('')

const showAlert = (message: string | null) => {
  if (message) alert(message) // Later can be replaced with ElMessage
}

// Action Handlers
const handleCommit = () => {
  showAlert(commit(commitMessage.value))
  commitMessage.value = ''
}
const handleCreateBranch = () => {
  showAlert(createBranch(newBranchName.value))
  newBranchName.value = ''
}
const handleSwitchBranch = () => {
  switchBranch(currentBranch.value)
}
const handleMerge = () => {
  showAlert(merge(branchToActOn.value))
  branchToActOn.value = ''
}
const handleRebase = () => {
  if (
    confirm(
      `Are you sure you want to rebase '${currentBranch.value}' onto '${branchToActOn.value}'? This will rewrite history.`,
    )
  ) {
    showAlert(rebase(branchToActOn.value))
    branchToActOn.value = ''
  }
}
const handleNodeClick = (nodeId: string) => {
  checkout(nodeId)
  selectedCommit.value = nodeId
}
const handleReset = () => {
  if (!selectedCommit.value) {
    alert('Please select a commit to reset to first.')
    return
  }
  if (
    confirm(
      `Are you sure you want to reset branch '${currentBranch.value}' to commit ${selectedCommit.value.substring(0, 7)}? This cannot be undone.`,
    )
  ) {
    showAlert(reset(selectedCommit.value))
  }
}
const handleStash = () => {
  showAlert(stash())
}
const handleStashPop = () => {
  showAlert(stashPop())
}
const handleCherryPick = () => {
  if (!selectedCommit.value) {
    alert('Please select a commit to cherry-pick first.')
    return
  }
  showAlert(cherryPick(selectedCommit.value))
}
</script>

<style scoped lang="scss">
.version-control-wrapper {
  height: 650px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-family:
    'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.control-pane {
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-right: 1px solid #e0e0e0;
}

.graph-pane {
  padding: 0;
  overflow: hidden;
}

.status-bar {
  margin-bottom: 16px;
}

.editor-instance {
  flex-grow: 1;
  margin-bottom: 16px;
}

.action-tabs {
  .action-button {
    margin-top: 16px;
    width: 100%;
  }
  .action-form {
    margin-top: 16px;
  }
}

.advanced-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.stash-table {
  margin-top: 16px;
}
</style>
