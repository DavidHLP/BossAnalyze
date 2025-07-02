<template>
  <div class="version-history">
    <div class="version-header">
      <h3>
        <el-icon><Clock /></el-icon>
        版本历史
      </h3>
      <div class="header-actions">
        <el-button
          type="primary"
          size="small"
          @click="createSnapshotDialog = true"
          :loading="loading"
        >
          创建快照
        </el-button>
        <el-button size="small" @click="refreshVersions" :loading="loading"> 刷新 </el-button>
      </div>
    </div>

    <div class="version-list" v-loading="loading">
      <div
        v-for="version in versionList"
        :key="version.id"
        class="version-item"
        :class="{ 'auto-save': version.isAutoSave }"
      >
        <div class="version-info">
          <div class="version-header-info">
            <span class="version-number">V{{ version.versionNumber }}</span>
            <span class="version-title">{{ version.title }}</span>
            <el-tag v-if="version.isAutoSave" type="info" size="small"> 自动保存 </el-tag>
          </div>

          <div class="version-description">
            {{ version.changeDescription }}
          </div>

          <div class="version-meta">
            <span class="version-time">
              {{ formatDateTime(version.createdAt) }}
            </span>
            <span class="version-author"> by {{ version.createdBy }} </span>
          </div>
        </div>

        <div class="version-actions">
          <el-button type="text" size="small" @click="showPreviewVersion(version)">
            预览
          </el-button>
          <el-button
            type="text"
            size="small"
            @click="restoreVersion(version)"
            :disabled="version.versionNumber === getCurrentVersion"
          >
            恢复
          </el-button>
        </div>
      </div>

      <div v-if="versionList.length === 0" class="empty-state">
        <el-empty description="暂无版本历史" />
      </div>
    </div>

    <!-- 创建快照对话框 -->
    <el-dialog v-model="createSnapshotDialog" title="创建版本快照" width="400px">
      <el-form @submit.prevent="createSnapshot">
        <el-form-item label="描述信息">
          <el-input
            v-model="snapshotDescription"
            placeholder="请输入此次快照的描述信息..."
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createSnapshotDialog = false">取消</el-button>
        <el-button type="primary" @click="createSnapshot" :loading="creating"> 创建快照 </el-button>
      </template>
    </el-dialog>

    <!-- 版本预览对话框 -->
    <el-dialog v-model="previewDialog" title="版本预览" width="80%" top="5vh">
      <div v-if="previewVersionData">
        <div class="preview-header">
          <h4>{{ previewVersionData.title }} (V{{ previewVersionData.versionNumber }})</h4>
          <p>{{ previewVersionData.changeDescription }}</p>
          <p class="preview-time">{{ formatDateTime(previewVersionData.createdAt) }}</p>
        </div>

        <div class="preview-content">
          <!-- 这里可以根据简历内容格式进行渲染 -->
          <pre>{{ previewVersionData.content }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock } from '@element-plus/icons-vue'
import {
  getVersionHistory,
  createVersionSnapshot,
  restoreToVersion,
  type ResumeVersion,
} from '@/api/resume/version'

interface Props {
  resumeId: string
  currentVersion?: number
}

const props = defineProps<Props>()
const emit = defineEmits<{
  versionRestored: [version: ResumeVersion]
  versionCreated: [version: ResumeVersion]
}>()

const versionList = ref<ResumeVersion[]>([])
const loading = ref(false)
const creating = ref(false)
const createSnapshotDialog = ref(false)
const snapshotDescription = ref('')
const previewDialog = ref(false)
const previewVersionData = ref<ResumeVersion | null>(null)

// 计算属性
const getCurrentVersion = computed(() => {
  if (versionList.value.length === 0) return 0
  return props.currentVersion || Math.max(...versionList.value.map((v) => v.versionNumber))
})

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 获取版本历史
const fetchVersionHistory = async () => {
  loading.value = true
  try {
    versionList.value = await getVersionHistory(props.resumeId)
  } catch (error) {
    ElMessage.error('获取版本历史失败')
  } finally {
    loading.value = false
  }
}

// 刷新版本列表
const refreshVersions = () => {
  fetchVersionHistory()
}

// 预览版本
const showPreviewVersion = (version: ResumeVersion) => {
  previewVersionData.value = version
  previewDialog.value = true
}

// 恢复版本
const restoreVersion = async (version: ResumeVersion) => {
  const result = await ElMessageBox.confirm(
    `确定要恢复到版本 V${version.versionNumber} 吗？当前内容将被覆盖！`,
    '恢复版本',
    {
      confirmButtonText: '确定恢复',
      cancelButtonText: '取消',
      type: 'warning',
    },
  ).catch(() => false)

  if (!result) return

  loading.value = true
  try {
    await restoreToVersion(props.resumeId, version.versionNumber)
    ElMessage.success('版本恢复成功！')
    emit('versionRestored', version)
    fetchVersionHistory()
  } catch (error) {
    ElMessage.error('版本恢复失败')
  } finally {
    loading.value = false
  }
}

// 创建快照
const createSnapshot = async () => {
  creating.value = true
  try {
    const version = await createVersionSnapshot(props.resumeId, snapshotDescription.value)
    ElMessage.success('快照创建成功！')
    createSnapshotDialog.value = false
    snapshotDescription.value = ''
    emit('versionCreated', version)
    fetchVersionHistory()
  } catch (error) {
    ElMessage.error('快照创建失败')
  } finally {
    creating.value = false
  }
}

// 初始化
onMounted(() => {
  fetchVersionHistory()
})
</script>

<style scoped lang="scss">
.version-history {
  .version-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e4e7ed;

    h3 {
      margin: 0;
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      color: #303133;
    }
  }

  .version-list {
    max-height: 500px;
    overflow-y: auto;
  }

  .version-item {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 12px;
    margin-bottom: 8px;
    border: 1px solid #e4e7ed;
    border-radius: 6px;
    transition: all 0.2s;

    &:hover {
      border-color: #409eff;
      box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
    }

    &.auto-save {
      background-color: #f8f9fa;
    }
  }

  .version-info {
    flex: 1;

    .version-header-info {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;

      .version-number {
        font-weight: 600;
        color: #409eff;
      }

      .version-title {
        font-weight: 500;
        color: #303133;
      }
    }

    .version-description {
      font-size: 14px;
      color: #606266;
      margin-bottom: 8px;
    }

    .version-meta {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: #909399;

      .version-time {
        font-weight: 500;
      }
    }
  }

  .version-actions {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  .preview-header {
    padding-bottom: 16px;
    border-bottom: 1px solid #e4e7ed;
    margin-bottom: 16px;

    h4 {
      margin: 0 0 8px 0;
      color: #303133;
    }

    p {
      margin: 4px 0;
      color: #606266;
    }

    .preview-time {
      font-size: 12px;
      color: #909399;
    }
  }

  .preview-content {
    max-height: 400px;
    overflow-y: auto;

    pre {
      white-space: pre-wrap;
      word-break: break-word;
      background-color: #f8f9fa;
      padding: 16px;
      border-radius: 6px;
      font-family: inherit;
      font-size: 14px;
      line-height: 1.6;
    }
  }
}
</style>
