<template>
  <div class="menu-management-container">
    <!-- 顶部操作区 -->
    <div class="action-bar">
      <div class="action-left">
        <el-button type="primary" @click="handleAdd" class="action-button">
          <el-icon>
            <Plus />
          </el-icon>
          <span>添加菜单</span>
        </el-button>
        <el-button type="success" @click="expandAll" class="action-button">
          <el-icon>
            <Expand />
          </el-icon>
          <span>展开全部</span>
        </el-button>
        <el-button type="info" @click="collapseAll" class="action-button">
          <el-icon>
            <Fold />
          </el-icon>
          <span>折叠全部</span>
        </el-button>
      </div>
      <div class="action-right">
        <el-button type="primary" plain @click="toggleSidebar" class="toggle-sidebar-btn">
          <el-icon>
            <component :is="isSidebarCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>
          <span class="hidden-sm-and-down">{{ isSidebarCollapsed ? '展开侧栏' : '折叠侧栏' }}</span>
        </el-button>
      </div>
    </div>

    <!-- 主内容区 - 分割面板 -->
    <el-row :gutter="20" class="content-panel">
      <!-- 左侧树形菜单 -->
      <el-col :xs="24" :sm="isSidebarCollapsed ? 6 : 8" :md="isSidebarCollapsed ? 5 : 8"
        :lg="isSidebarCollapsed ? 4 : 8" class="sidebar-col" :class="{ 'is-collapsed': isSidebarCollapsed }">
        <el-card shadow="hover" class="tree-card">
          <template #header>
            <div class="card-header">
              <h3 class="card-title">菜单结构</h3>
              <div class="search-wrapper">
                <el-input v-model="filterText" placeholder="搜索菜单" prefix-icon="Search" clearable class="search-input" />
              </div>
            </div>
          </template>
          <el-tree ref="menuTreeRef" :data="tableData" :props="{
            label: 'name',
            children: 'children'
          }" node-key="id" :filter-node-method="filterNode as any" :expand-on-click-node="false"
            :default-expanded-keys="expandedKeys" draggable @node-drag-end="handleDragEnd" highlight-current
            @node-click="handleNodeClick" class="menu-tree">
            <template #default="{ node, data }">
              <div class="tree-node">
                <div class="node-label">
                  <el-icon v-if="data.meta?.metaIcon" class="node-icon">
                    <component :is="data.meta.metaIcon" />
                  </el-icon>
                  <span class="node-text" :class="{ 'truncated-text': isSidebarCollapsed }">{{ node.label }}</span>
                  <el-tag v-if="data.meta?.type" size="small" :type="tagType(data.meta.type)" class="node-tag">
                    {{ { M: '目录', C: '菜单', F: '按钮' }[data.meta.type as 'M' | 'C' | 'F'] || '-' }}
                  </el-tag>
                </div>
                <div class="node-actions">
                  <el-tooltip content="添加子菜单" placement="top">
                    <el-button type="primary" link @click.stop="handleAddChild(data)" class="action-icon"
                      v-if="data.meta?.type !== 'F'">
                      <el-icon>
                        <Plus />
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="编辑" placement="top">
                    <el-button type="primary" link @click.stop="handleEdit(data)" class="action-icon">
                      <el-icon>
                        <Edit />
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top">
                    <el-button type="danger" link @click.stop="handleDelete(data)" class="action-icon">
                      <el-icon>
                        <Delete />
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </div>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧详情信息 -->
      <el-col :xs="24" :sm="isSidebarCollapsed ? 18 : 16" :md="isSidebarCollapsed ? 19 : 16"
        :lg="isSidebarCollapsed ? 20 : 16" class="content-col">
        <el-card shadow="hover" class="detail-card">
          <template #header>
            <div class="card-header">
              <h3 class="card-title">菜单详情</h3>
            </div>
          </template>

          <div v-if="currentNode" class="details-container">
            <el-descriptions :column="descriptionColumns" border class="details-info">
              <el-descriptions-item label="菜单名称">{{ currentNode.name }}</el-descriptions-item>
              <el-descriptions-item label="路由路径">{{ currentNode.path }}</el-descriptions-item>
              <el-descriptions-item label="组件路径">{{ currentNode.meta?.component || '-' }}</el-descriptions-item>
              <el-descriptions-item label="权限标识">{{ currentNode.permission || '-' }}</el-descriptions-item>
              <el-descriptions-item label="图标">
                <span class="icon-preview">
                  <el-icon v-if="currentNode.meta?.metaIcon">
                    <component :is="currentNode.meta.metaIcon" />
                  </el-icon>
                  {{ currentNode.meta?.metaIcon || '-' }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="排序">{{ currentNode.menuOrder }}</el-descriptions-item>
              <el-descriptions-item label="类型">
                {{ { M: '目录', C: '菜单', F: '按钮' }[currentNode.meta?.type as 'M' | 'C' | 'F'] || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="currentNode.status === 1 ? 'success' : 'danger'" class="status-tag">
                  {{ currentNode.status === 1 ? '正常' : '停用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="总是显示" :span="1">
                <el-tag size="small" :type="currentNode.meta?.alwaysShow ? 'success' : 'info'" class="feature-tag">
                  {{ currentNode.meta?.alwaysShow ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="隐藏菜单" :span="1">
                <el-tag size="small" :type="currentNode.meta?.metaHidden ? 'warning' : 'info'" class="feature-tag">
                  {{ currentNode.meta?.metaHidden ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="保持活跃">
                <el-tag size="small" :type="currentNode.meta?.metaKeepAlive ? 'success' : 'info'" class="feature-tag">
                  {{ currentNode.meta?.metaKeepAlive ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="访问角色">
                {{ currentNode.meta?.metaRoles || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="descriptionColumns">
                {{ currentNode.remark || '-' }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="action-group">
              <el-button type="primary" @click="handleEdit(currentNode)" class="detail-button">
                <el-icon>
                  <Edit />
                </el-icon>
                <span>编辑菜单</span>
              </el-button>
              <el-button v-if="currentNode.meta?.type !== 'F'" type="success" @click="handleAddChild(currentNode)"
                class="detail-button">
                <el-icon>
                  <Plus />
                </el-icon>
                <span>添加子菜单</span>
              </el-button>
              <el-button type="danger" @click="handleDelete(currentNode)" class="detail-button">
                <el-icon>
                  <Delete />
                </el-icon>
                <span>删除菜单</span>
              </el-button>
            </div>
          </div>

          <div v-else class="empty-state">
            <el-empty description="请选择一个菜单项查看详情" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" :title="dialogTitle" :width="dialogWidth" destroy-on-close
      class="menu-dialog">
      <el-form :model="formData" label-width="100px" class="menu-form">
        <el-tabs>
          <el-tab-pane label="基本信息">
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="菜单名称" required>
                  <el-input v-model="formData.name" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="路由路径" required v-if="formData.meta!.type !== 'F'">
                  <el-input v-model="formData.path" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="权限标识">
                  <el-input v-model="formData.permission" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="图标">
                  <el-input v-model="formData.meta.metaIcon" placeholder="Element Plus 图标名称">
                    <template #append>
                      <el-popover placement="bottom" trigger="click" width="550" :hide-after="0" class="icon-popover">
                        <template #reference>
                          <el-button class="select-icon-btn">选择图标</el-button>
                        </template>
                        <div class="icon-selector">
                          <div class="icon-search">
                            <el-input v-model="iconSearchText" placeholder="搜索图标" clearable prefix-icon="Search" />
                          </div>
                          <div class="icon-grid-container">
                            <el-scrollbar height="350px">
                              <div class="icon-grid">
                                <div v-for="icon in filteredIcons" :key="icon" @click="selectIcon(icon)"
                                  class="icon-item" :class="{ 'icon-selected': icon === formData.meta.metaIcon }">
                                  <el-icon>
                                    <component :is="icon" />
                                  </el-icon>
                                  <span class="icon-name">{{ icon }}</span>
                                </div>
                              </div>
                            </el-scrollbar>
                          </div>
                          <div class="icon-footer">
                            <el-button size="small" @click="clearSelectedIcon" class="clear-icon-btn">清除</el-button>
                            <div v-if="formData.meta.metaIcon" class="selected-icon">
                              <span>已选图标：</span>
                              <el-tag type="success">
                                <el-icon>
                                  <component :is="formData.meta.metaIcon" />
                                </el-icon>
                                {{ formData.meta.metaIcon }}
                              </el-tag>
                            </div>
                          </div>
                        </div>
                      </el-popover>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="排序">
                  <el-input-number v-model="formData.menuOrder" :min="0" class="number-input" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="状态">
                  <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" active-text="启用"
                    inactive-text="停用" class="status-switch" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="备注">
              <el-input v-model="formData.remark" type="textarea" :rows="2" class="textarea" />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="元数据信息">
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="菜单类型" required>
                  <el-select v-model="formData.meta!.type" class="type-select">
                    <el-option label="目录" value="M" />
                    <el-option label="菜单" value="C" />
                    <el-option label="按钮" value="F" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="组件路径" v-if="formData.meta!.type === 'C'">
                  <el-input v-model="formData.meta!.component" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="标题">
                  <el-input v-model="formData.meta!.metaTitle" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="重定向" v-if="formData.meta!.type === 'M'">
                  <el-input v-model="formData.meta!.redirect" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :xs="24" :sm="8">
                <el-form-item label="总是显示" class="switch-item">
                  <el-switch v-model="formData.meta!.alwaysShow" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="隐藏菜单" class="switch-item">
                  <el-switch v-model="formData.meta!.metaHidden" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="保持活跃" class="switch-item">
                  <el-switch v-model="formData.meta!.metaKeepAlive" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="访问角色">
              <el-input v-model="formData.meta!.metaRoles" placeholder="多个角色用逗号分隔" disabled />
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="isAddMode ? submitAdd() : submitEdit()">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加拖拽确认对话框 -->
    <el-dialog v-model="dragConfirmVisible" title="确认菜单位置修改" width="30%" :close-on-click-modal="false"
      :close-on-press-escape="false" :show-close="false" class="confirm-dialog">
      <div class="confirm-content">
        <el-icon class="warning-icon">
          <Warning />
        </el-icon>
        <span>
          确定要将菜单 "<strong>{{ dragInfo?.nodeName || '' }}</strong>"
          {{ dragInfo?.dropType === 'inner' ? '移动到' : '移动到与' }}
          "<strong>{{ dragInfo?.targetName || '' }}</strong>"
          {{ dragInfo?.dropType === 'inner' ? '内部' : '同级' }}吗？
        </span>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelDragChange">取消</el-button>
          <el-button type="primary" @click="confirmDragChange">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="MenuManagementComponents">
import { ref, computed, onMounted, watch, onBeforeMount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Router } from '@/router/index.d'
import { getUserRoutes, editRouter, addRouter, deleteRouter } from '@/api/router/router'
import { useRouterStore } from '@/stores/router/routerStore'
import { setupAsyncRoutes } from '@/router'
import type { TreeInstance, TreeNode } from 'element-plus'
import { useWindowSize } from '@vueuse/core'

// 响应式数据
const tableData = ref<Router[]>([])
const expandedKeys = ref<number[]>([])
const editDialogVisible = ref(false)
const dialogTitle = ref('编辑菜单')
const currentNode = ref<Router | null>(null)
const filterText = ref('')
const menuTreeRef = ref<TreeInstance | null>(null)
const iconSearchText = ref('')
const isAddMode = ref(false) // 新增标识，区分添加和编辑模式
const isSidebarCollapsed = ref(false) // 侧边栏折叠状态

// 响应式窗口尺寸
const { width } = useWindowSize()

// 根据窗口宽度计算对话框宽度
const dialogWidth = computed(() => {
  if (width.value < 768) return '95%'
  if (width.value < 992) return '80%'
  return '60%'
})

// 根据窗口宽度和侧边栏状态计算描述列表的列数
const descriptionColumns = computed(() => {
  if (width.value < 768) return 1
  if (width.value < 1200 || isSidebarCollapsed.value) return 1
  return 2
})

// 图标列表 - Element Plus 常用图标
const iconList = [
  'Add', 'AddLocation', 'Aim', 'Alarm', 'Apple', 'ArrowDown', 'ArrowDownBold', 'ArrowLeft',
  'ArrowLeftBold', 'ArrowRight', 'ArrowRightBold', 'ArrowUp', 'ArrowUpBold', 'Avatar',
  'Back', 'Baseball', 'Basketball', 'Bell', 'BellFilled', 'Bicycle', 'Bottom', 'BottomLeft',
  'BottomRight', 'Bowl', 'Box', 'Briefcase', 'Brush', 'BrushFilled', 'Burger', 'Calendar',
  'Camera', 'CameraFilled', 'CaretBottom', 'CaretLeft', 'CaretRight', 'CaretTop', 'Cellphone',
  'ChatDotRound', 'ChatDotSquare', 'ChatLineRound', 'ChatLineSquare', 'ChatRound', 'ChatSquare',
  'Check', 'Checked', 'Cherry', 'Chicken', 'CircleCheck', 'CircleCheckFilled', 'CircleClose',
  'CircleCloseFilled', 'CirclePlus', 'CirclePlusFilled', 'Clock', 'Close', 'CloseBold',
  'Cloudy', 'Coffee', 'CoffeeCup', 'Coin', 'ColdDrink', 'Collection', 'CollectionTag',
  'Comment', 'Compass', 'Connection', 'Coordinate', 'CopyDocument', 'Cpu', 'CreditCard',
  'Crop', 'DataAnalysis', 'DataBoard', 'DataLine', 'Delete', 'DeleteFilled', 'DeleteLocation',
  'Dessert', 'Discount', 'Dish', 'DishDot', 'Document', 'DocumentAdd', 'DocumentChecked',
  'DocumentCopy', 'DocumentDelete', 'DocumentRemove', 'Download', 'Drizzling', 'Edit', 'EditPen',
  'Eleme', 'ElemeFilled', 'ElementPlus', 'Expand', 'Failed', 'Female', 'Files', 'Film',
  'Filter', 'Finished', 'FirstAidKit', 'Flag', 'Fold', 'Folder', 'FolderAdd', 'FolderChecked',
  'FolderDelete', 'FolderOpened', 'FolderRemove', 'Food', 'Football', 'ForkSpoon', 'Fries',
  'FullScreen', 'Goblet', 'GobletFull', 'GobletSquare', 'GobletSquareFull', 'Goods', 'GoodsFilled',
  'Grape', 'Grid', 'Guide', 'Handbag', 'Headset', 'Help', 'HelpFilled', 'Hide', 'Histogram',
  'History', 'HomeFilled', 'HotWater', 'House', 'IceCream', 'IceCreamRound', 'IceCreamSquare',
  'IceDrink', 'IceTea', 'InfoFilled', 'Iphone', 'Key', 'KnifeFork', 'Lightning', 'Link',
  'List', 'Loading', 'Location', 'LocationFilled', 'LocationInformation', 'Lock', 'Lollipop',
  'Magic', 'Magnet', 'Male', 'Management', 'MapLocation', 'Medal', 'Memo', 'Menu', 'Message',
  'MessageBox', 'Mic', 'Microphone', 'MilkTea', 'Minus', 'Money', 'Monitor', 'Moon', 'MoonNight',
  'More', 'MoreFilled', 'MostlyCloudy', 'Mouse', 'Mug', 'Mute', 'MuteNotification', 'NoSmoking',
  'Notebook', 'Notification', 'Odometer', 'OfficeBuilding', 'Open', 'Operation', 'Opportunity',
  'Orange', 'Paperclip', 'PartlyCloudy', 'Pear', 'Phone', 'PhoneFilled', 'Picture',
  'PictureFilled', 'PictureRounded', 'PieChart', 'Place', 'Platform', 'Plus', 'Pointer',
  'Position', 'Postcard', 'Pouring', 'Present', 'PriceTag', 'Printer', 'Promotion', 'QuartzWatch',
  'Question', 'QuestionFilled', 'Rank', 'Reading', 'ReadingLamp', 'Refresh', 'RefreshLeft',
  'RefreshRight', 'Refrigerator', 'Remove', 'RemoveFilled', 'Right', 'ScaleToOriginal', 'School',
  'Scissor', 'Search', 'Select', 'Sell', 'SemiSelect', 'Service', 'Setting', 'Share', 'Ship',
  'Shop', 'ShoppingBag', 'ShoppingCart', 'ShoppingCartFull', 'ShoppingTrolley', 'Smoking',
  'Soccer', 'SoldOut', 'Sort', 'SortDown', 'SortUp', 'Stamp', 'Star', 'StarFilled', 'Stopwatch',
  'SuccessFilled', 'Sugar', 'Suitcase', 'Sunny', 'Sunrise', 'Sunset', 'Switch', 'SwitchButton',
  'TakeawayBox', 'Ticket', 'Tickets', 'Timer', 'ToiletPaper', 'Tools', 'Top', 'TopLeft',
  'TopRight', 'TrendCharts', 'Trophy', 'TrophyBase', 'Truck', 'Umbrella', 'Unlock', 'Upload',
  'UploadFilled', 'User', 'UserFilled', 'Van', 'VideoCamera', 'VideoCameraFilled', 'VideoPause',
  'VideoPlay', 'View', 'Wallet', 'WalletFilled', 'Warning', 'WarningFilled', 'Watch',
  'Watermelon', 'WindPower', 'ZoomIn', 'ZoomOut'
]

// 过滤图标
const filteredIcons = computed(() => {
  if (!iconSearchText.value) {
    return iconList
  }
  return iconList.filter(icon =>
    icon.toLowerCase().includes(iconSearchText.value.toLowerCase())
  )
})

// 选择图标方法
const selectIcon = (icon: string) => {
  formData.value.meta.metaIcon = icon
}

// 清除选择的图标
const clearSelectedIcon = () => {
  formData.value.meta.metaIcon = null
}

// 切换侧边栏折叠状态
const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

// 添加Router类型的扩展
interface RouterWithMeta {
  id?: number;
  name: string;
  path: string;
  permission: string;
  menuOrder: number;
  status: number;
  remark: string;
  parentId?: number;  // 前端使用parentId
  pid?: number;      // 后端使用pid
  children?: RouterWithMeta[];
  meta: {
    type: string;
    component: string;
    redirect: string | null;
    alwaysShow: boolean;
    metaTitle: string;
    metaIcon: string | null;
    metaHidden: boolean;
    metaRoles: string | null;
    metaKeepAlive: boolean;
    hidden: boolean;
  }
}

// 表单数据
const formData = ref<RouterWithMeta>({
  name: '',
  path: '',
  permission: '',
  menuOrder: 0,
  status: 1,
  remark: '',
  meta: {
    type: 'C',
    component: '',
    redirect: null,
    alwaysShow: false,
    metaTitle: '',
    metaIcon: null,
    metaHidden: false,
    metaRoles: null,
    metaKeepAlive: false,
    hidden: false
  }
})

// 监听搜索框输入变化
watch(filterText, (val) => {
  menuTreeRef.value?.filter(val)
})

// 监听屏幕尺寸变化，自动调整侧边栏状态
watch(width, (newWidth) => {
  if (newWidth < 768 && !isSidebarCollapsed.value) {
    isSidebarCollapsed.value = true
  }
})

// 生命周期
onBeforeMount(() => {
  // 在小屏幕上默认折叠侧边栏
  if (width.value < 768) {
    isSidebarCollapsed.value = true
  }
})

onMounted(() => {
  loadData()
})

// 方法
const loadData = async () => {
  try {
    const result = (await getUserRoutes()) as unknown as Router[]
    tableData.value = result

    // 默认展开第一级
    if (result.length > 0) {
      // 设置默认展开的节点
      expandedKeys.value = result.map(item => item.id as number)
    }
  } catch (error) {
    console.error('数据加载失败', error)
    ElMessage.error('数据加载失败')
  }
}

// 树节点筛选方法
const filterNode = (value: string, data: Router) => {
  if (!value) return true
  return data.name.includes(value) ||
    data.path.includes(value) ||
    String(data.permission).includes(value) ||
    data.meta?.component?.includes(value)
}

// 处理节点点击
const handleNodeClick = (data: Router) => {
  currentNode.value = data

  // 在移动设备上点击节点后自动折叠侧边栏，提供更大的详情显示空间
  if (width.value < 768) {
    isSidebarCollapsed.value = true
  }
}

// 展开所有节点
const expandAll = () => {
  // 递归获取所有节点ID
  const getAllIds = (nodes: Router[]): number[] => {
    let ids: number[] = []
    nodes.forEach(node => {
      if (node.id) ids.push(node.id as number)
      if (node.children && node.children.length > 0) {
        ids = [...ids, ...getAllIds(node.children)]
      }
    })
    return ids
  }

  expandedKeys.value = getAllIds(tableData.value)
}

// 折叠所有节点
const collapseAll = () => {
  if (!menuTreeRef.value) return
  menuTreeRef.value.store._getAllNodes().forEach((node: TreeNode) => {
    node.expanded = false
  })
}

// 添加拖拽确认相关的状态变量
const dragConfirmVisible = ref(false)
const dragPendingChanges = ref<Router | null>(null)
const dragInfo = ref<{
  nodeName: string;
  targetName: string;
  dropType: string;
}>({
  nodeName: '',
  targetName: '',
  dropType: ''
})

interface DragNode {
  data: Router;
  key: number;
  label: string;
}

// 拖拽结束处理
const handleDragEnd = async (draggingNode: DragNode, dropNode: DragNode | null, dropType: string) => {

  // 如果没有目标节点，说明是无效的拖拽
  if (!dropNode) return;

  try {
    // 深拷贝拖拽节点数据，避免直接修改原引用
    const draggingData = JSON.parse(JSON.stringify(draggingNode.data));

    // 获取父节点ID - 确保使用原始ID
    let newParentId: number | null = null;
    let parentNodeName: string = '根节点';

    // 确定新的父节点ID
    if (dropType === 'inner') {
      // 情况1: 拖拽到节点内部，成为其子节点
      newParentId = dropNode.data.id;
      parentNodeName = dropNode.data.name;
    } else if (dropType === 'before' || dropType === 'after') {
      // 情况2: 拖拽到节点前面或后面，成为兄弟节点 - 共享同一个父节点
      newParentId = dropNode.data.pid || null;

      // 如果目标节点有父节点，尝试查找父节点名称
      if (newParentId) {
        const parentNode = findNodeById(tableData.value, newParentId);
        if (parentNode) {
          parentNodeName = parentNode.name;
        }
      }
    }

    // 保存拖拽操作的信息，用于确认对话框
    dragInfo.value = {
      nodeName: draggingData.name,
      targetName: dropType === 'inner' ? dropNode.data.name : parentNodeName,
      dropType: dropType
    };

    // 明确设置新的父节点ID
    draggingData.pid = newParentId;
    draggingData.parentId = newParentId; // 前端结构使用

    // 处理可能的空字符串permission
    if (draggingData.permission === '') {
      draggingData.permission = null;
    }

    // 存储待确认的变更，而不是直接应用
    dragPendingChanges.value = draggingData;

    // 显示确认对话框
    dragConfirmVisible.value = true;

  } catch (error) {
    console.error('处理拖拽数据失败', error);
    ElMessage.error('处理拖拽数据失败');
    // 重新加载数据以恢复原始状态
    await loadData();
  }
}

// 确认拖拽更改
const confirmDragChange = async () => {
  try {
    if (!dragPendingChanges.value) {
      ElMessage.warning('没有待处理的更改');
      dragConfirmVisible.value = false;
      return;
    }

    // 应用变更 - 调用API更新节点数据
    await editRouter(dragPendingChanges.value as unknown as Router);

    // 清除路由缓存并重新加载路由
    localStorage.removeItem('cachedRoutes')
    const routerStore = useRouterStore()
    routerStore.setRoutes([]) // 清空当前路由
    await setupAsyncRoutes() // 重新设置路由

    // 重新加载数据以确保树结构正确
    await loadData();

    ElMessage.success('菜单位置已更新');
    dragConfirmVisible.value = false;
    dragPendingChanges.value = null;
  } catch (error) {
    console.error('更新菜单位置失败', error);
    ElMessage.error('更新菜单位置失败');
    // 重新加载数据以恢复原始状态
    await loadData();
    dragConfirmVisible.value = false;
    dragPendingChanges.value = null;
  }
}

// 取消拖拽更改
const cancelDragChange = async () => {
  dragConfirmVisible.value = false;
  dragPendingChanges.value = null;
  // 重新加载数据以恢复原始状态
  await loadData();
  ElMessage.info('已取消菜单位置修改');
}

// 根据ID查找节点的辅助函数
const findNodeById = (nodes: Router[], id: number): Router | null => {
  for (const node of nodes) {
    if (node.id === id) {
      return node;
    }

    if (node.children && node.children.length > 0) {
      const foundNode = findNodeById(node.children, id);
      if (foundNode) {
        return foundNode;
      }
    }
  }

  return null;
}

// 添加菜单
const handleAdd = () => {
  dialogTitle.value = '添加菜单'
  isAddMode.value = true // 设置为添加模式
  formData.value = {
    name: '',
    path: '',
    permission: '',
    menuOrder: 0,
    status: 1,
    remark: '',
    meta: {
      type: 'C',
      component: '',
      redirect: null,
      alwaysShow: false,
      metaTitle: '',
      metaIcon: null,
      metaHidden: false,
      metaRoles: null,
      metaKeepAlive: false,
      hidden: false
    }
  }
  editDialogVisible.value = true
}

// 添加子菜单
const handleAddChild = (row: Router) => {
  dialogTitle.value = '添加子菜单'
  isAddMode.value = true // 设置为添加模式
  formData.value = {
    name: '',
    path: '',
    permission: '',
    menuOrder: 0,
    status: 1,
    remark: '',
    parentId: row.id,  // 设置父ID
    meta: {
      type: row.meta?.type === 'M' ? 'C' : 'F',  // 如果父节点是目录，子节点默认为菜单
      component: '',
      redirect: null,
      alwaysShow: false,
      metaTitle: '',
      metaIcon: null,
      metaHidden: false,
      metaRoles: null,
      metaKeepAlive: false,
      hidden: false
    }
  }
  editDialogVisible.value = true
}

// 编辑菜单
const handleEdit = (row: Router) => {
  dialogTitle.value = '编辑菜单'
  isAddMode.value = false // 设置为编辑模式
  // 深拷贝防止直接修改原对象
  const rowCopy = JSON.parse(JSON.stringify(row)) as unknown as RouterWithMeta
  // 确保meta对象存在
  if (!rowCopy.meta) {
    rowCopy.meta = {
      type: 'C',
      component: '',
      redirect: null,
      alwaysShow: false,
      metaTitle: '',
      metaIcon: null,
      metaHidden: false,
      metaRoles: null,
      metaKeepAlive: false,
      hidden: false
    }
  }
  formData.value = rowCopy
  editDialogVisible.value = true
}

// 删除菜单
const handleDelete = (row: Router) => {
  ElMessageBox.confirm(
    `确定要删除菜单 "${row.name}" 吗？${row.children?.length ? '此操作将同时删除所有子菜单！' : ''}`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 调用真实API删除
      await deleteRouter(row)
      ElMessage.success('删除成功')

      // 清除路由缓存并重新加载路由
      localStorage.removeItem('cachedRoutes')
      const routerStore = useRouterStore()
      routerStore.setRoutes([]) // 清空当前路由
      await setupAsyncRoutes() // 重新设置路由

      // 刷新数据
      loadData()
      // 如果删除的是当前选中节点，清空选择
      if (currentNode.value && currentNode.value.id === row.id) {
        currentNode.value = null
      }
    } catch (error) {
      console.error('删除失败', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 用户取消删除
  })
}

// 提交添加操作
const submitAdd = async () => {
  try {

    // 处理数据格式，确保与后端兼容
    const submittingData = { ...formData.value }

    // 转换parentId为pid格式
    if (submittingData.parentId) {
      submittingData.pid = submittingData.parentId
      delete submittingData.parentId
    }

    // 添加新菜单
    await addRouter(submittingData as unknown as Router)

    ElMessage.success('添加成功')
    editDialogVisible.value = false

    // 清除路由缓存并重新加载路由
    localStorage.removeItem('cachedRoutes')
    const routerStore = useRouterStore()
    routerStore.setRoutes([]) // 清空当前路由
    await setupAsyncRoutes() // 重新设置路由

    loadData() // 刷新数据
  } catch (error) {
    console.log('添加失败', error)
    ElMessage.error('添加失败')
  }
}

// 提交编辑操作
const submitEdit = async () => {
  try {

    // 处理数据格式，确保与后端兼容
    const submittingData = { ...formData.value }

    // 转换parentId为pid格式
    if (submittingData.parentId) {
      submittingData.pid = submittingData.parentId
      delete submittingData.parentId
    }

    // 编辑现有菜单
    await editRouter(submittingData as unknown as Router)

    ElMessage.success('修改成功')
    editDialogVisible.value = false

    // 清除路由缓存并重新加载路由
    localStorage.removeItem('cachedRoutes')
    const routerStore = useRouterStore()
    routerStore.setRoutes([]) // 清空当前路由
    await setupAsyncRoutes() // 重新设置路由

    loadData() // 刷新数据

    // 如果是编辑当前选中的节点，更新当前节点详情
    if (currentNode.value && currentNode.value.id === formData.value.id) {
      currentNode.value = JSON.parse(JSON.stringify(formData.value))
    }
  } catch (error) {
    console.log('修改失败', error)
    ElMessage.error('修改失败')
  }
}

// 根据菜单类型返回不同标签样式
const tagType = (type: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const typeMap: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    'M': 'info',
    'C': 'success',
    'F': 'warning'
  }
  return typeMap[type] || 'info'
}
</script>

<style scoped lang="scss">
.menu-management-container {
  padding: var(--spacing-xl);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.action-bar {
  margin-bottom: var(--spacing-xl);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--spacing-md);

  .action-left,
  .action-right {
    display: flex;
    gap: var(--spacing-md);
    flex-wrap: wrap;
  }

  .action-button,
  .toggle-sidebar-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    border-radius: var(--border-radius-md);
    transition: transform 0.2s ease;

    &:hover {
      transform: translateY(-2px);
    }
  }
}

.content-panel {
  flex: 1;
  overflow: hidden;
  margin-bottom: var(--spacing-xl);
  min-height: 0; // 修复Flex布局下的溢出问题
}

.sidebar-col {
  transition: all 0.3s ease;
  height: 100%;

  &.is-collapsed {
    .tree-card {
      .card-header {
        .search-wrapper {
          .search-input {
            .el-input__wrapper {
              padding-left: 8px;
              padding-right: 8px;
            }
          }
        }
      }
    }

    .tree-node {
      .node-actions {
        flex-direction: column;
      }
    }
  }
}

.content-col {
  height: 100%;
  transition: all 0.3s ease;
}

.tree-card,
.detail-card {
  height: 100%;
  border-radius: var(--border-radius-lg);
  box-shadow: var(--box-shadow);
  transition: all 0.3s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  &:hover {
    box-shadow: var(--box-shadow-lg);
  }
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) 0;

  .card-title {
    margin: 0;
    font-size: var(--font-size-lg);
    font-weight: 600;
    color: var(--text-primary);
  }

  .search-wrapper {
    margin-top: var(--spacing-sm);

    .search-input {
      border-radius: var(--border-radius-md);
    }
  }
}

.menu-tree {
  flex: 1;
  overflow: auto;
}

.tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;

  .node-label {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0; // 防止子元素溢出

    .node-icon {
      color: var(--primary-color);
      font-size: 16px;
      flex-shrink: 0;
    }

    .node-text {
      font-size: var(--font-size-md);
      color: var(--text-primary);
      flex: 1;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;

      &.truncated-text {
        max-width: 60px;
      }
    }

    .node-tag {
      margin-left: 4px;
      flex-shrink: 0;
    }
  }

  .node-actions {
    display: flex;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.3s ease;
    flex-shrink: 0;

    .action-icon {
      font-size: 14px;
      padding: 2px;
      border-radius: var(--border-radius-sm);
    }
  }

  &:hover .node-actions {
    opacity: 1;
  }
}

.details-container {
  height: 100%;
  display: flex;
  flex-direction: column;

  .details-info {
    margin-bottom: var(--spacing-lg);
  }

  .icon-preview {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .status-tag,
  .feature-tag {
    font-weight: 500;
  }
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  margin-top: auto;
  padding-top: var(--spacing-lg);

  .detail-button {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    border-radius: var(--border-radius-md);
    transition: all 0.2s ease;
    margin-bottom: var(--spacing-sm);

    &:hover {
      transform: translateY(-2px);
    }
  }
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-dialog {
  .menu-form {
    padding: var(--spacing-md) 0;
  }

  .number-input,
  .type-select {
    width: 100%;
  }

  .switch-item {
    display: flex;
    align-items: center;
  }

  .textarea {
    border-radius: var(--border-radius-md);
  }
}

.icon-selector {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);

  .icon-search {
    padding-bottom: var(--spacing-sm);
    border-bottom: 1px solid var(--border-color);
  }

  .icon-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(70px, 1fr));
    gap: var(--spacing-md);
    padding: var(--spacing-sm);

    .icon-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      padding: 8px;
      border-radius: var(--border-radius-md);
      cursor: pointer;
      transition: all 0.2s ease;

      .el-icon {
        font-size: 20px;
      }

      .icon-name {
        font-size: var(--font-size-sm);
        text-align: center;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 100%;
      }

      &:hover {
        background-color: var(--primary-light);
        color: var(--primary-color);
      }

      &.icon-selected {
        background-color: var(--primary-color);
        color: white;
      }
    }
  }

  .icon-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: var(--spacing-sm);
    border-top: 1px solid var(--border-color);

    .selected-icon {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}

.confirm-dialog {
  .confirm-content {
    display: flex;
    align-items: flex-start;
    gap: var(--spacing-md);
    padding: var(--spacing-md) 0;

    .warning-icon {
      font-size: 24px;
      color: var(--warning-color);
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-md);
}

// 响应式样式
@media (max-width: 768px) {
  .menu-management-container {
    padding: var(--spacing-md);
  }

  .action-bar {
    flex-direction: column;
    align-items: stretch;

    .action-left,
    .action-right {
      justify-content: space-between;
    }
  }

  .sidebar-col,
  .content-col {
    margin-bottom: var(--spacing-md);
  }

  .content-panel {
    height: auto;
    min-height: calc(100vh - 150px);
  }

  .hidden-sm-and-down {
    display: none;
  }
}
</style>
