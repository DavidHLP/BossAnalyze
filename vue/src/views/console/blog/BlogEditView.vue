<template>
  <div class="blog-edit-container">
    <el-card class="blog-edit-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>{{ isEdit ? '编辑博客' : '新建博客' }}</h3>
            <el-tag v-if="isEdit" type="info" class="edit-tag">ID: {{ blogId }}</el-tag>
          </div>
          <el-button @click="goBack" class="back-btn">
            <el-icon><ArrowLeft /></el-icon>
            返回列表
          </el-button>
        </div>
      </template>

      <div class="form-container">
        <!-- 表单状态提示 -->
        <div class="form-status" v-if="isEdit">
          <el-alert
            type="info"
            :closable="false"
            class="edit-alert"
          >
            <template #title>
              <div class="alert-content">
                <el-icon><InfoFilled /></el-icon>
                <span>您正在编辑 <strong>{{ editedItem.title || '未命名博客' }}</strong></span>
                <el-tag :type="editedItem.status === 1 ? 'success' : 'warning'" class="status-tag">
                  {{ editedItem.status === 1 ? '已发布' : '草稿' }}
                </el-tag>
              </div>
            </template>
          </el-alert>
        </div>

        <el-form
          ref="form"
          :model="editedItem"
          :rules="rules"
          label-width="100px"
          class="blog-form"
          :status-icon="true"
        >
          <el-form-item label="博客标题" prop="title" class="title-item">
            <el-input
              v-model="editedItem.title"
              placeholder="请输入博客标题"
              maxlength="100"
              show-word-limit
              class="title-input"
            />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="博客分类" prop="categoryId">
                <el-select
                  v-model="editedItem.categoryId"
                  placeholder="选择分类"
                  style="width: 100%"
                  class="category-select"
                  filterable
                >
                  <!-- 顶级分类 -->
                  <template v-for="category in processedCategories" :key="category.id">
                    <!-- 父分类 -->
                    <el-option-group v-if="category.children && category.children.length > 0" :label="category.name">
                      <!-- 父分类本身作为选项 -->
                      <el-option
                        :key="`parent-${category.id}`"
                        :label="`${category.name} (父分类)`"
                        :value="category.id"
                      >
                        <div class="category-option">
                          <span>{{ category.name }} <el-tag size="small" type="success">父分类</el-tag></span>
                          <el-tag size="small" type="info" class="count-tag">
                            {{ category.count || 0 }} 篇
                          </el-tag>
                        </div>
                      </el-option>
                      <!-- 子分类 -->
                      <el-option
                        v-for="child in category.children"
                        :key="child.id"
                        :label="getCategoryDisplayName(child)"
                        :value="child.id"
                      >
                        <div class="category-option">
                          <span>{{ getCategoryDisplayName(child) }}</span>
                          <el-tag size="small" type="info" class="count-tag">
                            {{ child.count || 0 }} 篇
                          </el-tag>
                        </div>
                      </el-option>
                    </el-option-group>

                    <!-- 没有子分类的父分类 -->
                    <el-option
                      v-else
                      :key="category.id"
                      :label="category.name"
                      :value="category.id"
                    >
                      <div class="category-option">
                        <span>{{ category.name }}</span>
                        <el-tag size="small" type="info" class="count-tag">
                          {{ category.count || 0 }} 篇
                        </el-tag>
                      </div>
                    </el-option>
                  </template>
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="12">
              <el-form-item label="博客标签">
                <el-select
                  v-model="editedItem.tags"
                  multiple
                  placeholder="选择标签"
                  style="width: 100%"
                  collapse-tags
                  collapse-tags-tooltip
                  class="tag-select"
                  filterable
                >
                  <el-option
                    v-for="tag in tags"
                    :key="tag.id"
                    :label="tag.name"
                    :value="tag.id"
                  >
                    <div class="tag-option">
                      <el-tag
                        :style="{ backgroundColor: tag.color || '#409EFF' }"
                        effect="dark"
                        class="tag-color"
                      >
                        {{ tag.name }}
                      </el-tag>
                      <span class="tag-count">{{ tag.count || 0 }} 篇</span>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="博客摘要" prop="summary">
            <el-input
              v-model="editedItem.summary"
              type="textarea"
              :rows="3"
              placeholder="请输入博客摘要，不超过200字"
              maxlength="200"
              show-word-limit
              class="summary-input"
            />
          </el-form-item>

          <el-form-item label="封面图片">
            <el-upload
              class="cover-uploader"
              action="#"
              :http-request="uploadCover"
              :show-file-list="false"
              :before-upload="beforeCoverUpload"
            >
              <img v-if="editedItem.coverUrl" :src="editedItem.coverUrl" class="cover-image" />
              <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">
              <el-text type="info">建议上传16:9比例图片，最佳尺寸1200x675px</el-text>
            </div>
          </el-form-item>

          <el-divider content-position="left">
            <el-icon><Edit /></el-icon>
            <span class="divider-text">博客内容</span>
          </el-divider>

          <el-form-item class="editor-container">
            <MarkdownEditor
              v-model="editedItem.content"
              :height="600"
              class="markdown-editor"
            />
          </el-form-item>

          <el-divider content-position="left">
            <el-icon><Setting /></el-icon>
            <span class="divider-text">发布设置</span>
          </el-divider>

          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="发布状态" class="status-item">
                <el-radio-group v-model="editedItem.status" class="status-radio">
                  <el-radio :label="1">
                    <el-icon><View /></el-icon>
                    <span>发布</span>
                  </el-radio>
                  <el-radio :label="0">
                    <el-icon><Hide /></el-icon>
                    <span>草稿</span>
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="允许评论">
                <el-switch
                  v-model="editedItem.allowComment"
                  :active-value="true"
                  :inactive-value="false"
                  active-text="允许"
                  inactive-text="禁止"
                  class="comment-switch"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item class="action-buttons">
            <el-button
              type="primary"
              @click="saveBlog"
              :loading="loading"
              class="save-btn"
            >
              <el-icon><Check /></el-icon>
              保存博客
            </el-button>
            <el-button
              type="success"
              @click="saveAndPublish"
              :loading="loading"
              v-if="editedItem.status === 0"
              class="publish-btn"
            >
              <el-icon><Upload /></el-icon>
              保存并发布
            </el-button>
            <el-button
              @click="goBack"
              class="cancel-btn"
            >
              <el-icon><Close /></el-icon>
              取消
            </el-button>
            <el-button
              type="danger"
              @click="confirmReset"
              class="reset-btn"
              v-if="!isEdit"
            >
              <el-icon><Delete /></el-icon>
              重置表单
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 重置确认对话框 -->
    <el-dialog
      v-model="resetDialog"
      title="确认重置"
      width="400px"
      class="reset-dialog"
    >
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        title="警告"
        description="重置表单将清空所有已输入的内容，此操作不可撤销"
        class="reset-warning"
      />
      <div class="reset-confirm-content">
        <p>确定要重置表单吗？</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resetDialog = false">取消</el-button>
          <el-button type="danger" @click="resetForm">确认重置</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormRules, FormInstance, UploadProps } from 'element-plus'
import {
  ArrowLeft,
  InfoFilled,
  Edit,
  Setting,
  View,
  Hide,
  Check,
  Close,
  Delete,
  Plus,
  Upload
} from '@element-plus/icons-vue'
import { useSnackbar } from '@/composables/useSnackbar'
import MarkdownEditor from '@/components/markdown/MdEditorElement.vue'
import {
  getBlogDetail,
  createBlog,
  updateBlog,
  getBlogCategories,
  getBlogTags,
  type Blog,
  type Category,
  type Tag
} from '@/api/blog/blog'

// 扩展类型定义
interface ExtendedBlog extends Blog {
  coverUrl?: string;
  allowComment?: boolean;
}

interface ExtendedCategory extends Category {
  count?: number;
  parentId: string | null;
  children?: ExtendedCategory[];
}

interface ExtendedTag extends Tag {
  color?: string;
  count?: number;
}

const route = useRoute()
const router = useRouter()
const { showSnackbar } = useSnackbar()

// 页面状态
const loading = ref(false)
const isEdit = ref(false)
const blogId = ref<string | null>(null)
const resetDialog = ref(false)

// 表单引用
const form = ref<FormInstance>()

// 分类和标签
const categories = ref<ExtendedCategory[]>([])
const tags = ref<ExtendedTag[]>([])

// 编辑项接口
interface EditedItem {
  id: string;
  title: string;
  summary: string;
  content: string;
  categoryId: string;
  tags: string[];
  status: number;
  coverUrl?: string;
  allowComment: boolean;
}

// 编辑项
const defaultItem: EditedItem = {
  id: '',
  title: '',
  summary: '',
  content: '',
  categoryId: '',
  tags: [],
  status: 0,
  coverUrl: '',
  allowComment: true
}
const editedItem = reactive<EditedItem>({ ...defaultItem })

// 表单验证规则
const rules: FormRules = {
  title: [
    { required: true, message: '标题不能为空', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度应在2-100个字符之间', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '分类不能为空', trigger: 'change' }
  ],
  summary: [
    { required: true, message: '摘要不能为空', trigger: 'blur' },
    { max: 200, message: '摘要不能超过200字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '博客内容不能为空', trigger: 'blur' }
  ]
}

// 计算属性
const formTitle = computed(() => {
  return isEdit.value ? `编辑博客: ${editedItem.title}` : '创建新博客'
})

// 处理分类数据，构建层级结构
const processedCategories = computed(() => {
  const categoryMap = new Map<string, ExtendedCategory>();
  const result: ExtendedCategory[] = [];

  // 首先将所有分类添加到Map中
  categories.value.forEach(category => {
    categoryMap.set(category.id, { ...category, children: [] });
  });

  // 然后构建层级结构
  categories.value.forEach(category => {
    const current = categoryMap.get(category.id);
    if (current && current.parentId) {
      // 如果有父分类，添加到父分类的children中
      const parent = categoryMap.get(current.parentId);
      if (parent) {
        parent.children = parent.children || [];
        parent.children.push(current);
      } else {
        // 如果找不到父分类，作为顶级分类处理
        result.push(current);
      }
    } else {
      // 没有父分类的作为顶级分类
      if (current) {
        result.push(current);
      }
    }
  });

  // 如果没有层级结构，直接返回原始分类列表
  return result.length > 0 ? result : categories.value;
});

// 获取分类的显示名称（包含父类路径）
const getCategoryDisplayName = (category: ExtendedCategory): string => {
  if (!category.parentId) {
    return category.name;
  }

  // 查找父分类
  const parent = categories.value.find(c => c.id === category.parentId);
  if (parent) {
    return `${parent.name} / ${category.name}`;
  }

  return category.name;
};

// 加载分类
const loadCategories = async () => {
  try {
    const response = await getBlogCategories()

    // 处理分类数据，确保parentId字段存在
    const categoriesData = response.data.map((category: Category) => {
      return {
        ...category,
        parentId: (category as any).parentId || null,
        count: (category as any).count || 0
      } as ExtendedCategory;
    });

    categories.value = categoriesData;
  } catch (error) {
    console.error('加载分类失败', error)
    showSnackbar('加载分类失败', 'error')
  }
}

// 加载标签
const loadTags = async () => {
  try {
    const response = await getBlogTags()
    tags.value = response.data
  } catch (error) {
    console.error('加载标签失败', error)
    showSnackbar('加载标签失败', 'error')
  }
}

// 加载博客详情
const loadBlogDetail = async (id: string) => {
  try {
    loading.value = true
    const response = await getBlogDetail(id)
    const blogDetail = response.data as ExtendedBlog

    Object.assign(editedItem, {
      id: blogDetail.id,
      title: blogDetail.title,
      summary: blogDetail.summary,
      content: blogDetail.content,
      categoryId: blogDetail.categoryId,
      tags: blogDetail.tags?.map((tag: Tag) => tag.id) || [],
      status: blogDetail.status,
      coverUrl: blogDetail.coverUrl || '',
      allowComment: blogDetail.allowComment !== false
    })
  } catch (error) {
    console.error('获取博客详情失败', error)
    showSnackbar('获取博客详情失败', 'error')
    goBack()
  } finally {
    loading.value = false
  }
}

// 上传封面图片前的验证
const beforeCoverUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    showSnackbar('封面图片只能是图片格式!', 'error')
    return false
  }
  if (!isLt2M) {
    showSnackbar('封面图片大小不能超过 2MB!', 'error')
    return false
  }
  return true
}

// 上传封面图片
const uploadCover = (options: any) => {
  const file = options.file
  // 这里应该调用实际的上传API，这里仅做演示
  // 模拟上传成功后获取URL
  const reader = new FileReader()
  reader.readAsDataURL(file)
  reader.onload = () => {
    editedItem.coverUrl = reader.result as string
    showSnackbar('封面图片上传成功', 'success')
  }
}

// 保存博客
const saveBlog = async () => {
  if (!form.value) return

  try {
    await form.value.validate()

    loading.value = true
    const blogData = { ...editedItem }

    if (isEdit.value) {
      await updateBlog(blogData.id, blogData)
      showSnackbar('博客更新成功', 'success')
    } else {
      await createBlog(blogData)
      showSnackbar('博客创建成功', 'success')
    }

    goBack()
  } catch (error) {
    console.error('保存博客失败', error)
    showSnackbar('保存博客失败', 'error')
  } finally {
    loading.value = false
  }
}

// 保存并发布
const saveAndPublish = async () => {
  editedItem.status = 1
  await saveBlog()
}

// 确认重置表单
const confirmReset = () => {
  resetDialog.value = true
}

// 重置表单
const resetForm = () => {
  Object.assign(editedItem, defaultItem)
  resetDialog.value = false
  showSnackbar('表单已重置', 'info')
}

// 返回列表页
const goBack = () => {
  if (form.value && editedItem.title) {
    ElMessageBox.confirm(
      '您有未保存的更改，确定要离开吗？',
      '确认离开',
      {
        confirmButtonText: '离开',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
      .then(() => {
        router.push('/console/blog/blogmanagement')
      })
      .catch(() => {
        // 用户取消离开
      })
  } else {
    router.push('/console/blog/blogmanagement')
  }
}

// 初始化
onMounted(() => {
  // 加载分类和标签
  loadCategories()
  loadTags()

  // 判断是新建还是编辑
  const id = route.params.id as string | undefined
  if (id && id !== 'new') {
    isEdit.value = true
    blogId.value = id
    loadBlogDetail(id)
  } else {
    isEdit.value = false
    blogId.value = null
  }
})
</script>

<style scoped>
.blog-edit-container {
  padding: 16px;
  max-width: 1200px;
  margin: 0 auto;
}

.blog-edit-card {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.blog-edit-card:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h3 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.edit-tag {
  font-size: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
}

.form-container {
  padding: 8px 0;
}

.form-status {
  margin-bottom: 24px;
}

.edit-alert {
  border-radius: 8px;
}

.alert-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-tag {
  margin-left: 8px;
}

.blog-form {
  margin-top: 16px;
}

.title-item {
  margin-bottom: 24px;
}

.title-input {
  font-size: 16px;
}

/* 分类选择器样式 */
.category-select :deep(.el-select-group__title) {
  font-weight: bold;
  color: var(--el-color-primary);
  padding-left: 12px;
  position: relative;
}

.category-select :deep(.el-select-group__title)::before {
  content: '📁';
  margin-right: 6px;
}

.category-select :deep(.el-select-group__wrap) {
  margin-left: 12px;
}

.category-select :deep(.el-select-group__list) {
  padding-left: 12px;
  border-left: 1px dashed var(--el-border-color-lighter);
}

.category-option, .tag-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.count-tag {
  font-size: 11px;
}

.tag-color {
  padding: 0 8px;
  margin-right: 8px;
}

.tag-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.summary-input {
  font-size: 14px;
}

.cover-uploader {
  width: 300px;
  height: 169px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.cover-uploader:hover {
  border-color: var(--el-color-primary);
}

.cover-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 300px;
  height: 169px;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
}

.cover-image {
  width: 300px;
  height: 169px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
}

.divider-text {
  margin-left: 8px;
  font-weight: 600;
}

.editor-container {
  margin-top: 16px;
}

.markdown-editor {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.status-radio {
  display: flex;
  gap: 24px;
}

.status-radio :deep(.el-radio) {
  display: flex;
  align-items: center;
  height: 32px;
}

.status-radio :deep(.el-radio__label) {
  display: flex;
  align-items: center;
  gap: 4px;
}

.comment-switch {
  margin-left: 8px;
}

.action-buttons {
  margin-top: 32px;
  display: flex;
  justify-content: center;
  gap: 16px;
}

.save-btn {
  background: linear-gradient(45deg, var(--vue-color-primary-dark-1), var(--vue-color-primary));
  border: none;
  box-shadow: 0 2px 6px rgba(66, 184, 131, 0.2);
  transition: all 0.3s;
  min-width: 120px;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(66, 184, 131, 0.3);
}

.publish-btn {
  min-width: 120px;
}

.cancel-btn, .reset-btn {
  min-width: 100px;
}

.reset-dialog :deep(.el-dialog__header) {
  background-color: #fef0f0;
  border-bottom: 1px solid #fde2e2;
}

.reset-dialog :deep(.el-dialog__title) {
  color: #f56c6c;
}

.reset-warning {
  margin-bottom: 16px;
}

.reset-confirm-content {
  padding: 8px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .action-buttons {
    flex-direction: column;
    align-items: stretch;
  }

  .save-btn, .publish-btn, .cancel-btn, .reset-btn {
    margin-bottom: 8px;
  }

  .cover-uploader, .cover-uploader-icon, .cover-image {
    width: 100%;
    height: auto;
    aspect-ratio: 16/9;
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .blog-edit-card {
    background-color: var(--el-bg-color-overlay);
  }

  .cover-uploader {
    border-color: var(--el-border-color-darker);
    background-color: var(--el-bg-color);
  }

  .cover-uploader-icon {
    color: var(--el-text-color-secondary);
  }

  .category-select :deep(.el-select-group__list) {
    border-left-color: var(--el-border-color-dark);
  }
}
</style>
