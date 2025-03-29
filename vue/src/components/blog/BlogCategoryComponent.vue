<template>
  <div class="blog-category">
    <el-row :gutter="20" class="control-row">
      <el-col :span="24">
        <el-alert
          v-if="useStaticData"
          type="info"
          show-icon
          :closable="false"
          title="当前使用静态演示数据"
          description="修改这些数据不会持久化到后端"
        />
        <el-switch
          v-model="useStaticData"
          active-text="使用静态演示数据"
          inactive-text="使用API数据"
          class="data-switch"
          @change="handleDataSourceChange"
        />
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 左侧分类树 -->
      <el-col :span="8">
        <el-card class="category-tree-card">
          <template #header>
            <div class="card-header">
              <h3>分类列表</h3>
              <el-button type="primary" size="small" @click="openCreateDialog(null)">
                <el-icon><Plus /></el-icon>添加根分类
              </el-button>
            </div>
          </template>
          <div class="search-container">
            <el-input
              v-model="searchQuery"
              placeholder="搜索分类"
              clearable
              @input="filterTree"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          <div class="tree-container" v-loading="loading">
            <el-tree
              ref="categoryTree"
              :data="treeCategories"
              node-key="id"
              highlight-current
              :props="{ label: 'name', children: 'children' }"
              :expand-on-click-node="false"
              :filter-node-method="filterNode"
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <div class="custom-tree-node">
                  <span>{{ node.label }}</span>
                  <span class="node-actions">
                    <el-tooltip content="添加子分类" placement="top">
                      <el-button
                        type="success"
                        circle
                        size="small"
                        @click.stop="openCreateDialog(data)"
                      >
                        <el-icon><Plus /></el-icon>
                      </el-button>
                    </el-tooltip>
                    <el-tooltip content="编辑" placement="top">
                      <el-button
                        type="primary"
                        circle
                        size="small"
                        @click.stop="openEditDialog(data)"
                      >
                        <el-icon><Edit /></el-icon>
                      </el-button>
                    </el-tooltip>
                    <el-tooltip content="删除" placement="top">
                      <el-button
                        type="danger"
                        circle
                        size="small"
                        @click.stop="confirmDelete(data)"
                      >
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </span>
                </div>
              </template>
            </el-tree>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧详情面板 -->
      <el-col :span="16">
        <el-card class="category-detail-card">
          <template #header>
            <div class="card-header">
              <h3>{{ selectedCategory ? '分类详情: ' + selectedCategory.name : '分类详情' }}</h3>
              <div v-if="selectedCategory">
                <el-button type="primary" size="small" @click="openEditDialog(selectedCategory)">
                  <el-icon><Edit /></el-icon>编辑
                </el-button>
                <el-button type="danger" size="small" @click="confirmDelete(selectedCategory)">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </div>
            </div>
          </template>

          <div v-if="selectedCategory" class="category-detail">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="分类名称">{{ selectedCategory.name }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(selectedCategory.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="排序值">{{ selectedCategory.sortOrder }}</el-descriptions-item>
              <el-descriptions-item label="父级分类">
                {{ getParentCategoryName(selectedCategory.parentId) }}
              </el-descriptions-item>
              <el-descriptions-item label="描述">
                <div class="description-content">{{ selectedCategory.description || '暂无描述' }}</div>
              </el-descriptions-item>
            </el-descriptions>

            <div v-if="hasChildren(selectedCategory)" class="subcategories-container">
              <h4>子分类列表</h4>
              <el-table :data="getChildCategories(selectedCategory.id)" style="width: 100%">
                <el-table-column prop="name" label="名称"></el-table-column>
                <el-table-column prop="description" label="描述" show-overflow-tooltip></el-table-column>
                <el-table-column label="创建时间" width="180">
                  <template #default="scope">
                    {{ formatDateTime(scope.row.createTime) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="150">
                  <template #default="scope">
                    <el-button-group>
                      <el-button type="primary" size="small" @click="openEditDialog(scope.row)">
                        <el-icon><Edit /></el-icon>
                      </el-button>
                      <el-button type="danger" size="small" @click="confirmDelete(scope.row)">
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div v-if="!hasChildren(selectedCategory)" class="empty-subcategories">
              <el-empty description="暂无子分类">
                <el-button type="primary" @click="openCreateDialog(selectedCategory)">添加子分类</el-button>
              </el-empty>
            </div>
          </div>

          <div v-else class="empty-detail">
            <el-empty description="请从左侧选择一个分类查看详情"></el-empty>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新建/编辑分类对话框 -->
    <el-dialog
      v-model="dialog"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
    >
      <el-form ref="form" :model="editedItem" :rules="rules" status-icon label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="editedItem.name" placeholder="请输入分类名称"></el-input>
        </el-form-item>
        <el-form-item label="父级分类">
          <el-cascader
            v-model="editedItem.parentId"
            :options="categoryOptions"
            :props="{ checkStrictly: true, emitPath: false, value: 'id', label: 'name' }"
            clearable
            placeholder="请选择父分类（可选）"
            :disabled="isSubCategory || isRootCategory"
          />
          <div class="form-help-text">留空表示顶级分类</div>
        </el-form-item>
        <el-form-item label="排序顺序" prop="sortOrder">
          <el-input-number
            v-model="editedItem.sortOrder"
            :min="0"
            :step="1"
            controls-position="right"
            placeholder="数值越小越靠前显示"
          ></el-input-number>
          <div class="form-help-text">决定同级分类的显示顺序，数值越小越靠前</div>
        </el-form-item>
        <el-form-item label="分类描述" prop="description">
          <el-input
            v-model="editedItem.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分类描述（可选）"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeDialog">取消</el-button>
          <el-button type="primary" @click="saveCategory" :loading="saveLoading">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 确认删除对话框 -->
    <el-dialog
      v-model="deleteDialog"
      title="确认删除"
      width="400px"
    >
      <el-alert
        v-if="hasChildren(deleteItem as CategoryExtended)"
        type="warning"
        show-icon
        :closable="false"
        title="警告：该分类包含子分类"
        description="删除此分类将同时删除所有子分类！此操作不可恢复。"
      />
      <div class="delete-confirm-content">
        <p>确定要删除分类 "{{ (deleteItem as CategoryExtended).name }}" 吗？</p>
        <p>此操作不可撤销，且可能影响关联的博客文章。</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialog = false">取消</el-button>
          <el-button type="danger" @click="deleteCategoryItem" :loading="deleteLoading">确认删除</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup name="BlogCategoryComponent">
import { ref, reactive, onMounted, watch, computed, nextTick } from 'vue'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElTree } from 'element-plus'
import type { FormInstance } from 'element-plus'
import type { FilterNodeMethodFunction } from 'element-plus/es/components/tree/src/tree.type.mjs'
import {
  getBlogCategories,
  createBlogCategory,
  updateBlogCategory,
  deleteBlogCategory,
  type Category
} from '@/api/blog/blog'

// 扩展Category接口，添加我们需要的字段
interface CategoryExtended extends Omit<Category, 'description' | 'createTime' | 'parentId'> {
  description?: string;
  createTime?: string;
  sortOrder: number;
  parentId: string | null;
  children?: CategoryExtended[];
  hasChildren?: boolean;
}

// 示例静态数据
const staticCategoryData: CategoryExtended[] = [
  {
    id: '1',
    name: '技术',
    description: '技术相关的文章和教程',
    createTime: '2023-01-15T08:30:00Z',
    sortOrder: 1,
    parentId: null
  },
  {
    id: '2',
    name: '前端开发',
    description: '前端开发相关技术和框架',
    createTime: '2023-01-16T09:15:00Z',
    sortOrder: 1,
    parentId: '1'
  },
  {
    id: '3',
    name: 'Vue.js',
    description: 'Vue.js框架学习和实践',
    createTime: '2023-01-17T10:20:00Z',
    sortOrder: 1,
    parentId: '2'
  },
  {
    id: '4',
    name: 'React',
    description: 'React框架学习和实践',
    createTime: '2023-01-18T11:25:00Z',
    sortOrder: 2,
    parentId: '2'
  },
  {
    id: '5',
    name: '后端开发',
    description: '后端开发相关技术和框架',
    createTime: '2023-01-19T12:30:00Z',
    sortOrder: 2,
    parentId: '1'
  },
  {
    id: '6',
    name: 'Spring Boot',
    description: 'Spring Boot框架学习和实践',
    createTime: '2023-01-20T13:35:00Z',
    sortOrder: 1,
    parentId: '5'
  },
  {
    id: '7',
    name: 'Node.js',
    description: 'Node.js技术学习和实践',
    createTime: '2023-01-21T14:40:00Z',
    sortOrder: 2,
    parentId: '5'
  },
  {
    id: '8',
    name: '生活',
    description: '生活方式和日常分享',
    createTime: '2023-01-22T15:45:00Z',
    sortOrder: 2,
    parentId: null
  },
  {
    id: '9',
    name: '美食',
    description: '美食制作和分享',
    createTime: '2023-01-23T16:50:00Z',
    sortOrder: 1,
    parentId: '8'
  },
  {
    id: '10',
    name: '旅行',
    description: '旅行经验和攻略',
    createTime: '2023-01-24T17:55:00Z',
    sortOrder: 2,
    parentId: '8'
  },
  {
    id: '11',
    name: '国内旅行',
    description: '国内旅行目的地推荐',
    createTime: '2023-01-25T18:00:00Z',
    sortOrder: 1,
    parentId: '10'
  },
  {
    id: '12',
    name: '国际旅行',
    description: '国际旅行目的地推荐',
    createTime: '2023-01-26T19:05:00Z',
    sortOrder: 2,
    parentId: '10'
  },
  {
    id: '13',
    name: '读书',
    description: '读书笔记和书评',
    createTime: '2023-01-27T20:10:00Z',
    sortOrder: 3,
    parentId: '8'
  },
  {
    id: '14',
    name: '分享',
    description: '资源分享和链接',
    createTime: '2023-01-28T21:15:00Z',
    sortOrder: 3,
    parentId: null
  }
];

// 是否使用静态数据
const useStaticData = ref(true);

// 表格相关
const flatCategories = ref<CategoryExtended[]>([])
const treeCategories = ref<CategoryExtended[]>([])
const loading = ref(false)
const searchQuery = ref('')
const categoryTree = ref<InstanceType<typeof ElTree> | null>(null)
const selectedCategory = ref<CategoryExtended | null>(null)

// 对话框状态
const dialog = ref(false)
const isEdit = ref(false)
const isSubCategory = ref(false) // 是否是添加子分类
const isRootCategory = ref(false) // 是否是添加根分类
const form = ref<FormInstance>()
const deleteDialog = ref(false)
const deleteItem = ref<CategoryExtended | {}>({})
const saveLoading = ref(false)
const deleteLoading = ref(false)

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '分类名称不能为空', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  sortOrder: [
    { type: 'number', min: 0, message: '排序值不能为负数', trigger: 'blur' }
  ]
}

// 对话框标题
const dialogTitle = computed(() => {
  if (isEdit.value) return '编辑分类';
  if (isSubCategory.value) return '添加子分类';
  if (isRootCategory.value) return '添加根分类';
  return '添加分类';
});

// 分类选项（用于级联选择器）
const categoryOptions = computed(() => {
  // 过滤掉当前编辑项及其子分类，防止选择自己或其子分类作为父分类
  const filteredCategories = flatCategories.value.filter(c => {
    if (!isEdit.value) return true;
    if (c.id === editedItem.id) return false;
    // 避免循环引用 - 不能选择自己的子分类作为父分类
    const isChildOfCurrent = isChildCategory(c, editedItem.id);
    return !isChildOfCurrent;
  });

  return buildCategoryTree(filteredCategories);
});

// 编辑项
const defaultItem: CategoryExtended = {
  id: '',
  name: '',
  description: '',
  sortOrder: 0,
  parentId: null
}
const editedItem = reactive<CategoryExtended>({ ...defaultItem })

// 格式化日期时间
const formatDateTime = (dateString: string | undefined): string => {
  if (!dateString) return '未知时间';
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}

// 检查是否是某个分类的子分类
const isChildCategory = (category: CategoryExtended, parentId: string): boolean => {
  if (!category.parentId) return false;
  if (category.parentId === parentId) return true;

  const parent = flatCategories.value.find(c => c.id === category.parentId);
  if (!parent) return false;

  return isChildCategory(parent, parentId);
}

// 树节点过滤方法
const filterNode: FilterNodeMethodFunction = (value: string, data: any) => {
  if (!value) return true;
  return data.name.toLowerCase().includes(value.toLowerCase());
};

// 将扁平分类列表转换为树形结构
const buildCategoryTree = (categories: CategoryExtended[]): CategoryExtended[] => {
  const categoryMap = new Map<string, CategoryExtended>();
  const result: CategoryExtended[] = [];

  // 第一步：创建以ID为键的map
  categories.forEach(category => {
    categoryMap.set(category.id, { ...category, children: [] });
  });

  // 第二步：构建树形结构
  categories.forEach(category => {
    const currentCategory = categoryMap.get(category.id);
    if (!currentCategory) return;

    if (category.parentId && categoryMap.has(category.parentId)) {
      // 如果有父级，添加到父级的children
      const parentCategory = categoryMap.get(category.parentId);
      if (parentCategory && parentCategory.children) {
        parentCategory.children.push(currentCategory);
      }
    } else {
      // 如果没有父级或父级不存在，添加到结果数组
      result.push(currentCategory);
    }
  });

  // 按排序字段排序
  result.sort((a, b) => a.sortOrder - b.sortOrder);
  result.forEach(category => {
    if (category.children && category.children.length > 0) {
      category.children.sort((a, b) => a.sortOrder - b.sortOrder);
    }
  });

  return result;
};

// 获取父分类名称
const getParentCategoryName = (parentId: string | null | undefined): string => {
  if (!parentId) return '顶级分类';
  const parent = flatCategories.value.find(c => c.id === parentId);
  return parent ? parent.name : '未知分类';
}

// 获取指定分类的子分类
const getChildCategories = (parentId: string): CategoryExtended[] => {
  return flatCategories.value.filter(c => c.parentId === parentId);
}

// 检查分类是否有子分类
const hasChildren = (category: CategoryExtended): boolean => {
  if (!category) return false;
  return flatCategories.value.some(c => c.parentId === category.id);
};

// 加载分类列表
const loadCategories = async () => {
  try {
    loading.value = true;
    let allCategories: CategoryExtended[] = [];

    if (useStaticData.value) {
      // 使用静态数据
      allCategories = [...staticCategoryData];
    } else {
      // 使用API数据
      const response = await getBlogCategories();

      // 将结果转换为扩展类型，添加额外字段
      allCategories = response.data.map(cat => {
        // 使用类型转换，因为API不支持扩展字段
        return {
          ...cat,
          description: '',  // 添加默认值
          createTime: new Date().toISOString(),  // 添加默认时间
          sortOrder: 0,     // 添加默认排序
          parentId: null    // 初始设置为顶级分类
        } as CategoryExtended;
      });
    }

    // 如果有搜索关键字，进行过滤
    if (searchQuery.value) {
      allCategories = allCategories.filter(cat =>
        cat.name.toLowerCase().includes(searchQuery.value.toLowerCase())
      );
    }

    // 保存扁平结构
    flatCategories.value = allCategories;

    // 转换为树形结构并存储
    treeCategories.value = buildCategoryTree(allCategories);

    // 重置搜索过滤
    if (categoryTree.value && searchQuery.value) {
      categoryTree.value.filter('');
    }

    // 重置选中的分类
    selectedCategory.value = null;
  } catch (error) {
    console.error('加载分类列表失败', error);
    ElMessage.error('加载分类列表失败');
  } finally {
    loading.value = false;
  }
};

// 节点点击处理
const handleNodeClick = (data: CategoryExtended) => {
  selectedCategory.value = data;
};

// 过滤树
const filterTree = () => {
  categoryTree.value?.filter(searchQuery.value);
};

// 打开新建对话框
const openCreateDialog = (parentCategory: CategoryExtended | null) => {
  isEdit.value = false;
  isSubCategory.value = !!parentCategory;
  isRootCategory.value = !parentCategory;

  const newItem = { ...defaultItem };
  if (parentCategory) {
    newItem.parentId = parentCategory.id;
  } else {
    newItem.parentId = null;
  }

  Object.assign(editedItem, newItem);
  dialog.value = true;
};

// 打开编辑对话框
const openEditDialog = (item: CategoryExtended) => {
  isEdit.value = true;
  isSubCategory.value = false;
  isRootCategory.value = false;
  Object.assign(editedItem, item);
  dialog.value = true;
};

// 关闭对话框
const closeDialog = () => {
  dialog.value = false;
  nextTick(() => {
    form.value?.resetFields();
    Object.assign(editedItem, defaultItem);
  });
};

// 保存分类
const saveCategory = async () => {
  if (!form.value) return;

  await form.value.validate(async (valid) => {
    if (valid) {
      try {
        saveLoading.value = true;

        // 构建要保存的数据
        const categoryData = {
          id: editedItem.id,
          name: editedItem.name,
          description: editedItem.description || '',
          sortOrder: editedItem.sortOrder,
          parentId: editedItem.parentId,
          createTime: editedItem.createTime || new Date().toISOString()
        };

        if (useStaticData.value) {
          // 静态数据模式，直接修改内存数据
          if (isEdit.value) {
            // 更新现有分类
            const index = staticCategoryData.findIndex(c => c.id === categoryData.id);
            if (index !== -1) {
              staticCategoryData[index] = {...staticCategoryData[index], ...categoryData};
            }
            ElMessage.success('分类更新成功');
          } else {
            // 创建新分类
            const newId = (Math.max(...staticCategoryData.map(c => parseInt(c.id))) + 1).toString();
            const newCategory = {
              ...categoryData,
              id: newId,
              createTime: new Date().toISOString()
            };
            staticCategoryData.push(newCategory);
            ElMessage.success('分类创建成功');
          }
        } else {
          // API模式，调用API
          if (isEdit.value) {
            await updateBlogCategory(categoryData.id, categoryData);
            ElMessage.success('分类更新成功');
          } else {
            await createBlogCategory(categoryData);
            ElMessage.success('分类创建成功');
          }
        }

        closeDialog();
        await loadCategories();
      } catch (error) {
        console.error('保存分类失败', error);
        ElMessage.error('保存分类失败');
      } finally {
        saveLoading.value = false;
      }
    }
  });
};

// 确认删除
const confirmDelete = (item: CategoryExtended) => {
  deleteItem.value = item;
  deleteDialog.value = true;
};

// 删除分类
const deleteCategoryItem = async () => {
  try {
    deleteLoading.value = true;
    const categoryToDelete = deleteItem.value as CategoryExtended;

    if (useStaticData.value) {
      // 静态数据模式，直接修改内存数据
      // 递归删除子分类
      const deleteWithChildren = (categoryId: string) => {
        // 先找出所有子分类
        const childCategories = staticCategoryData.filter(c => c.parentId === categoryId);

        // 递归删除子分类
        for (const child of childCategories) {
          deleteWithChildren(child.id);
        }

        // 从静态数据中移除当前分类
        const index = staticCategoryData.findIndex(c => c.id === categoryId);
        if (index !== -1) {
          staticCategoryData.splice(index, 1);
        }
      };

      deleteWithChildren(categoryToDelete.id);
    } else {
      // API模式，调用API
      // 递归删除子分类
      const deleteWithChildren = async (categoryId: string) => {
        // 先找出所有子分类
        const childCategories = flatCategories.value.filter(c => c.parentId === categoryId);

        // 递归删除子分类
        for (const child of childCategories) {
          await deleteWithChildren(child.id);
        }

        // 删除当前分类
        await deleteBlogCategory(categoryId);
      };

      await deleteWithChildren(categoryToDelete.id);
    }

    // 如果删除的是当前选中的分类，清空选择
    if (selectedCategory.value && selectedCategory.value.id === categoryToDelete.id) {
      selectedCategory.value = null;
    }

    ElMessage.success('分类删除成功');
    deleteDialog.value = false;
    await loadCategories();
  } catch (error) {
    console.error('删除分类失败', error);
    ElMessage.error('删除分类失败');
  } finally {
    deleteLoading.value = false;
  }
};

// 初始化
onMounted(() => {
  loadCategories();
});

// 监听搜索查询变化
watch(searchQuery, (val) => {
  filterTree();
});

// 处理数据源变更的方法
const handleDataSourceChange = (value: boolean): void => {
  loadCategories();
};
</script>

<style scoped>
.blog-category {
  padding: 16px;
}

.control-row {
  margin-bottom: 16px;
}

.data-switch {
  margin-top: 8px;
}

.category-tree-card,
.category-detail-card {
  height: 100%;
  min-height: 600px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-container {
  margin-bottom: 16px;
}

.tree-container {
  height: calc(100% - 50px);
  overflow: auto;
  padding: 8px 0;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}

.node-actions {
  display: none;
}

.custom-tree-node:hover .node-actions {
  display: flex;
  gap: 4px;
}

.category-detail {
  padding: 16px 0;
}

.description-content {
  white-space: pre-wrap;
  min-height: 40px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.subcategories-container {
  margin-top: 24px;
}

.subcategories-container h4 {
  margin-bottom: 16px;
  font-weight: 500;
  color: #303133;
}

.empty-detail,
.empty-subcategories {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
}

.delete-confirm-content {
  margin: 16px 0;
  line-height: 1.5;
}

.form-help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>

