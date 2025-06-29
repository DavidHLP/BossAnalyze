<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, MoreFilled, Edit, Delete, Clock } from '@element-plus/icons-vue';
import { getResumes, createResume, deleteResume as apiDeleteResume } from '@/api/resume/resume';
import type { Resume } from '@/api/resume/resume.d';
import { default as template } from '@/templates/modules/10front_end/index';

const router = useRouter();

const resumeList = ref<Resume[]>([]);
const loading = ref(false);

const getResumeList = async () => {
  loading.value = true;
  try {
    resumeList.value = await getResumes();
  } catch (error) {
    ElMessage.error('获取简历列表失败');
  } finally {
    loading.value = false;
  }
};

const handleCreateResume = async () => {
  try {
    const newResume = await createResume({ title: '我的新简历', content: template.content });
    if (newResume && newResume.id) {
        ElMessage.success('创建成功！');
        router.push(`/editor?type=create&id=${newResume.id}`);
    }
  } catch (error) {
      ElMessage.error('创建失败');
  }
};

const editResume = (id: string) => {
  router.push(`/editor?type=create&id=${id}`);
};

const handleDeleteResume = (id: string) => {
  ElMessageBox.confirm('确定要删除这份简历吗？删除后无法恢复哦！', '温馨提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
        await apiDeleteResume(id);
        ElMessage.success('删除成功！');
        getResumeList(); // Refresh the list
    } catch (error) {
        ElMessage.error('删除失败');
    }
  }).catch(() => {
    // Cancelled
  });
};

onMounted(() => {
  getResumeList();
});

</script>

<template>
  <div class="resume-list-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-info">
          <h1 class="page-title">我的简历</h1>
          <p class="page-subtitle">管理您的简历，让每一份作品都精彩绝伦</p>
        </div>
        <el-button
          type="primary"
          size="large"
          class="create-btn"
          @click="handleCreateResume"
        >
          <el-icon class="create-icon">
            <Plus />
          </el-icon>
          新建简历
        </el-button>
      </div>
    </div>

    <!-- 简历列表 -->
    <div class="resume-content" v-loading="loading">
      <!-- 空状态 -->
      <div v-if="!loading && resumeList.length === 0" class="empty-state">
        <div class="empty-icon">📄</div>
        <h3 class="empty-title">还没有简历哦</h3>
        <p class="empty-description">快来创建您的第一份简历吧，展示您的才华与经验！</p>
        <el-button type="primary" size="large" @click="handleCreateResume">
          立即创建
        </el-button>
      </div>

      <!-- 简历卡片列表 -->
      <div v-else class="resume-grid">
        <div
          v-for="resume in resumeList"
          :key="resume.id"
          class="resume-card"
          @click="editResume(resume.id)"
        >
          <div class="card-header">
            <div class="resume-icon">📋</div>
            <div class="card-actions">
              <el-dropdown trigger="click" @click.stop>
                <el-button
                  class="action-btn"
                  size="small"
                  text
                  @click.stop
                >
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="editResume(resume.id)">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-dropdown-item>
                    <el-dropdown-item
                      class="danger-item"
                      @click="handleDeleteResume(resume.id)"
                    >
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>

          <div class="card-body">
            <h3 class="resume-title">{{ resume.title }}</h3>
            <div class="resume-meta">
              <span class="meta-item">
                <el-icon><Clock /></el-icon>
                {{ new Date(resume.updatedAt).toLocaleDateString() }}
              </span>
            </div>
          </div>

          <div class="card-footer">
            <el-button
              type="primary"
              size="small"
              class="edit-btn"
              @click.stop="editResume(resume.id)"
            >
              <el-icon><Edit /></el-icon>
              编辑简历
            </el-button>
          </div>

          <!-- 悬浮效果装饰 -->
          <div class="card-decoration"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/assets/style/variables.scss' as *;

.resume-list-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  padding: var(--spacing-xl);
}

.page-header {
  margin-bottom: var(--spacing-xxxl);

  .header-content {
    @include flex-between;
    max-width: 1200px;
    margin: 0 auto;
  }

  .header-info {
    .page-title {
      font-size: var(--font-size-xxxl);
      font-weight: var(--font-weight-bold);
      color: var(--text-primary);
      margin: 0 0 var(--spacing-sm) 0;
      background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .page-subtitle {
      font-size: var(--font-size-lg);
      color: var(--text-secondary);
      margin: 0;
    }
  }

  .create-btn {
    height: 48px;
    padding: 0 var(--spacing-xl);
    border-radius: var(--radius-xl);
    font-weight: var(--font-weight-semibold);
    box-shadow: var(--shadow-lg);
    background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
    border: none;
    transition: all var(--transition-normal) var(--transition-timing);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(59, 130, 246, 0.3);
    }

    .create-icon {
      margin-right: var(--spacing-sm);
      font-size: var(--font-size-lg);
    }
  }
}

.resume-content {
  max-width: 1200px;
  margin: 0 auto;
}

.empty-state {
  @include flex-center;
  @include flex-column;
  padding: var(--spacing-xxxl) var(--spacing-xl);
  text-align: center;
  background: var(--background-card);
  border-radius: var(--radius-xl);
  @include card-shadow;

  .empty-icon {
    font-size: 64px;
    margin-bottom: var(--spacing-lg);
    opacity: 0.6;
  }

  .empty-title {
    font-size: var(--font-size-xxl);
    color: var(--text-primary);
    margin: 0 0 var(--spacing-md) 0;
    font-weight: var(--font-weight-semibold);
  }

  .empty-description {
    font-size: var(--font-size-lg);
    color: var(--text-secondary);
    margin: 0 0 var(--spacing-xl) 0;
    max-width: 400px;
  }
}

.resume-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: var(--spacing-xl);

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: var(--spacing-lg);
  }
}

.resume-card {
  background: var(--background-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  cursor: pointer;
  transition: all var(--transition-normal) var(--transition-timing);
  border: 1px solid transparent;
  position: relative;
  overflow: hidden;
  @include card-shadow;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
    border-color: var(--primary-light);

    .card-decoration {
      opacity: 1;
      transform: scale(1);
    }

    .resume-title {
      color: var(--primary-color);
    }
  }

  .card-header {
    @include flex-between;
    margin-bottom: var(--spacing-lg);

    .resume-icon {
      width: 48px;
      height: 48px;
      @include flex-center;
      background: linear-gradient(135deg, var(--primary-light), var(--primary-lighter));
      border-radius: var(--radius-lg);
      font-size: var(--font-size-xl);
    }

    .card-actions {
      .action-btn {
        width: 32px;
        height: 32px;
        @include flex-center;
        border-radius: var(--radius-md);
        color: var(--text-secondary);
        transition: all var(--transition-normal) var(--transition-timing);

        &:hover {
          background-color: var(--background-secondary);
          color: var(--text-primary);
        }
      }
    }
  }

  .card-body {
    margin-bottom: var(--spacing-lg);

    .resume-title {
      font-size: var(--font-size-xl);
      font-weight: var(--font-weight-semibold);
      color: var(--text-primary);
      margin: 0 0 var(--spacing-md) 0;
      transition: color var(--transition-normal) var(--transition-timing);
    }

    .resume-meta {
      .meta-item {
        @include flex-start;
        font-size: var(--font-size-sm);
        color: var(--text-secondary);
        gap: var(--spacing-xs);

        .el-icon {
          font-size: var(--font-size-md);
        }
      }
    }
  }

  .card-footer {
    .edit-btn {
      width: 100%;
      height: 40px;
      border-radius: var(--radius-lg);
      font-weight: var(--font-weight-medium);
      background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
      border: none;
      transition: all var(--transition-normal) var(--transition-timing);

      &:hover {
        background: var(--primary-dark);
        transform: translateY(-1px);
      }

      .el-icon {
        margin-right: var(--spacing-xs);
      }
    }
  }

  .card-decoration {
    position: absolute;
    top: -50%;
    right: -50%;
    width: 100%;
    height: 100%;
    background: linear-gradient(45deg, transparent, var(--primary-lighter), transparent);
    border-radius: var(--radius-full);
    opacity: 0;
    transform: scale(0.8);
    transition: all var(--transition-slow) var(--transition-timing);
  }
}

// Element Plus 下拉菜单样式覆盖
:deep(.el-dropdown-menu) {
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  @include card-shadow;

  .el-dropdown-menu__item {
    padding: var(--spacing-md) var(--spacing-lg);
    @include flex-start;
    gap: var(--spacing-sm);

    &:hover {
      background-color: var(--primary-lighter);
      color: var(--primary-color);
    }

    &.danger-item:hover {
      background-color: rgba(239, 68, 68, 0.1);
      color: var(--error-color);
    }
  }
}

// 加载状态优化
:deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
}

// 响应式优化
@media (max-width: 768px) {
  .resume-list-container {
    padding: var(--spacing-lg);
  }

  .page-header {
    margin-bottom: var(--spacing-xl);

    .header-content {
      flex-direction: column;
      gap: var(--spacing-lg);
      text-align: center;
    }
  }

  .resume-card {
    padding: var(--spacing-lg);
  }
}
</style>
