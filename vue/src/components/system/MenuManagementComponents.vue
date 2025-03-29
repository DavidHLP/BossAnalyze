<template>
  <div class="menu-management-container">
    <!-- 顶部操作区 -->
    <div class="top-actions">
      <el-button type="primary" @click="handleAdd" icon="Plus">添加菜单</el-button>
      <el-button type="success" @click="expandAll" icon="Expand">展开全部</el-button>
      <el-button type="info" @click="collapseAll" icon="Fold">折叠全部</el-button>
    </div>

    <!-- 主内容区 - 分割面板 -->
    <el-row :gutter="20" class="main-content">
      <!-- 左侧树形菜单 -->
      <el-col :span="8" class="tree-panel">
        <el-card shadow="hover" class="tree-card">
          <template #header>
            <div class="card-header">
              <span>菜单结构</span>
              <div class="search-box">
                <el-input
                  v-model="filterText"
                  placeholder="搜索菜单"
                  prefix-icon="Search"
                  clearable
                />
              </div>
            </div>
          </template>
          <el-tree
            ref="menuTreeRef"
            :data="tableData"
            :props="{
              label: 'name',
              children: 'children'
            }"
            node-key="id"
            :filter-node-method="filterNode"
            :expand-on-click-node="false"
            :default-expanded-keys="expandedKeys"
            draggable
            @node-drag-end="handleDragEnd"
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <div class="custom-tree-node">
                <div class="tree-node-label">
                  <el-icon v-if="data.icon" class="menu-icon">
                    <component :is="data.icon" />
                  </el-icon>
                  <span :class="{ 'disabled-menu': data.status !== 1 }">{{ node.label }}</span>
                  <el-tag v-if="data.meta?.type" size="small" :type="tagType(data.meta.type)" class="menu-type-tag">
                    {{ { M: '目录', C: '菜单', F: '按钮' }[data.meta.type as 'M' | 'C' | 'F'] || '-' }}
                  </el-tag>
                </div>
                <div class="node-actions">
                  <el-tooltip content="添加子菜单" placement="top">
                    <el-button
                      type="primary"
                      link
                      @click.stop="handleAddChild(data)"
                      icon="Plus"
                      v-if="data.meta?.type !== 'F'"
                    />
                  </el-tooltip>
                  <el-tooltip content="编辑" placement="top">
                    <el-button
                      type="primary"
                      link
                      @click.stop="handleEdit(data)"
                      icon="Edit"
                    />
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top">
                    <el-button
                      type="danger"
                      link
                      @click.stop="handleDelete(data)"
                      icon="Delete"
                    />
                  </el-tooltip>
                </div>
              </div>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧详情信息 -->
      <el-col :span="16" class="detail-panel">
        <el-card shadow="hover" class="detail-card">
          <template #header>
            <div class="card-header">
              <span>菜单详情</span>
            </div>
          </template>

          <div v-if="currentNode" class="menu-detail">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="菜单名称">{{ currentNode.name }}</el-descriptions-item>
              <el-descriptions-item label="路由路径">{{ currentNode.path }}</el-descriptions-item>
              <el-descriptions-item label="组件路径">{{ currentNode.meta?.component || '-' }}</el-descriptions-item>
              <el-descriptions-item label="权限标识">{{ currentNode.permission || '-' }}</el-descriptions-item>
              <el-descriptions-item label="图标">{{ currentNode.icon || '-' }}</el-descriptions-item>
              <el-descriptions-item label="排序">{{ currentNode.menuOrder }}</el-descriptions-item>
              <el-descriptions-item label="类型">
                {{ { M: '目录', C: '菜单', F: '按钮' }[currentNode.meta?.type as 'M' | 'C' | 'F'] || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="currentNode.status === 1 ? 'success' : 'danger'">
                  {{ currentNode.status === 1 ? '正常' : '停用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="总是显示" :span="1">
                <el-tag size="small" :type="currentNode.meta?.alwaysShow ? 'success' : 'info'">
                  {{ currentNode.meta?.alwaysShow ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="隐藏菜单" :span="1">
                <el-tag size="small" :type="currentNode.meta?.metaHidden ? 'warning' : 'info'">
                  {{ currentNode.meta?.metaHidden ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="保持活跃">
                <el-tag size="small" :type="currentNode.meta?.metaKeepAlive ? 'success' : 'info'">
                  {{ currentNode.meta?.metaKeepAlive ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="访问角色">
                {{ currentNode.meta?.metaRoles || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">
                {{ currentNode.remark || '-' }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="detail-actions">
              <el-button type="primary" @click="handleEdit(currentNode)" icon="Edit">编辑菜单</el-button>
              <el-button v-if="currentNode.meta?.type !== 'F'" type="success" @click="handleAddChild(currentNode)" icon="Plus">添加子菜单</el-button>
              <el-button type="danger" @click="handleDelete(currentNode)" icon="Delete">删除菜单</el-button>
            </div>
          </div>

          <div v-else class="no-selection">
            <el-empty description="请选择一个菜单项查看详情" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" :title="dialogTitle" width="60%" destroy-on-close>
      <el-form :model="formData" label-width="100px">
        <el-tabs>
          <el-tab-pane label="基本信息">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="菜单名称" required>
                  <el-input v-model="formData.name" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="路由路径" required v-if="formData.meta!.type !== 'F'">
                  <el-input v-model="formData.path" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="权限标识">
                  <el-input v-model="formData.permission" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="图标">
                  <el-input v-model="formData.icon" placeholder="Element Plus 图标名称">
                    <template #append>
                      <el-popover placement="bottom" trigger="click" width="550" :hide-after="0">
                        <template #reference>
                          <el-button>选择图标</el-button>
                        </template>
                        <div class="icon-selector">
                          <div class="icon-selector-header">
                            <el-input
                              v-model="iconSearchText"
                              placeholder="搜索图标"
                              clearable
                              prefix-icon="Search"
                            />
                          </div>
                          <div class="icon-selector-body">
                            <el-scrollbar height="350px">
                              <div class="icon-grid">
                                <div
                                  v-for="icon in filteredIcons"
                                  :key="icon"
                                  class="icon-item"
                                  :class="{ 'icon-item-active': formData.icon === icon }"
                                  @click="selectIcon(icon)"
                                >
                                  <el-icon>
                                    <component :is="icon" />
                                  </el-icon>
                                  <span class="icon-name">{{ icon }}</span>
                                </div>
                              </div>
                            </el-scrollbar>
                          </div>
                          <div class="icon-selector-footer">
                            <el-button size="small" @click="clearSelectedIcon">清除</el-button>
                            <div class="selected-icon" v-if="formData.icon">
                              <span>已选图标：</span>
                              <el-tag type="success">
                                <el-icon class="selected-preview">
                                  <component :is="formData.icon" />
                                </el-icon>
                                {{ formData.icon }}
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
              <el-col :span="12">
                <el-form-item label="排序">
                  <el-input-number v-model="formData.menuOrder" :min="0" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="状态">
                  <el-switch
                    v-model="formData.status"
                    :active-value="1"
                    :inactive-value="0"
                    active-text="启用"
                    inactive-text="停用"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="备注">
              <el-input v-model="formData.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="元数据信息">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="菜单类型" required>
                  <el-select v-model="formData.meta!.type">
                    <el-option label="目录" value="M" />
                    <el-option label="菜单" value="C" />
                    <el-option label="按钮" value="F" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="组件路径" v-if="formData.meta!.type === 'C'">
                  <el-input v-model="formData.meta!.component" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="标题">
                  <el-input v-model="formData.meta!.metaTitle" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="重定向" v-if="formData.meta!.type === 'M'">
                  <el-input v-model="formData.meta!.redirect" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="总是显示">
                  <el-switch v-model="formData.meta!.alwaysShow" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="隐藏菜单">
                  <el-switch v-model="formData.meta!.metaHidden" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="保持活跃">
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
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="isAddMode ? submitAdd() : submitEdit()">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加拖拽确认对话框 -->
    <el-dialog
      v-model="dragConfirmVisible"
      title="确认菜单位置修改"
      width="30%"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <span>
        确定要将菜单 "{{ dragInfo?.nodeName || '' }}"
        {{ dragInfo?.dropType === 'inner' ? '移动到' : '移动到与' }}
        "{{ dragInfo?.targetName || '' }}"
        {{ dragInfo?.dropType === 'inner' ? '内部' : '同级' }}吗？
      </span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelDragChange">取消</el-button>
          <el-button type="primary" @click="confirmDragChange">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="MenuManagementComponents">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Router } from '@/router/index.d'
import { getUserRoutes, editRouter, addRouter, deleteRouter } from '@/api/router/router'
import { useRouterStore } from '@/stores/router/routerStore'
import { setupAsyncRoutes } from '@/router'

// 响应式数据
const tableData = ref<Router[]>([])
const expandedKeys = ref<number[]>([])
const editDialogVisible = ref(false)
const dialogTitle = ref('编辑菜单')
const currentNode = ref<Router | null>(null)
const filterText = ref('')
const menuTreeRef = ref<any>(null)
const iconSearchText = ref('')
const isAddMode = ref(false) // 新增标识，区分添加和编辑模式

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
  formData.value.icon = icon
}

// 清除选择的图标
const clearSelectedIcon = () => {
  formData.value.icon = ''
}

// 添加Router类型的扩展
interface RouterWithMeta {
  id?: number;
  name: string;
  path: string;
  icon: string;
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
    redirect: any;
    alwaysShow: boolean;
    metaTitle: string;
    metaIcon: any;
    metaHidden: boolean;
    metaRoles: any;
    metaKeepAlive: boolean;
    hidden: boolean;
  }
}

// 表单数据
const formData = ref<RouterWithMeta>({
  name: '',
  path: '',
  icon: '',
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

// 生命周期
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
         data.permission?.includes(value) ||
         data.meta?.component?.includes(value)
}

// 处理节点点击
const handleNodeClick = (data: Router) => {
  currentNode.value = data
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
  menuTreeRef.value.store._getAllNodes().forEach((node: any) => {
    node.expanded = false;
  });
}

// 添加拖拽确认相关的状态变量
const dragConfirmVisible = ref(false)
const dragPendingChanges = ref<any>(null)
const dragInfo = ref<{
  nodeName: string;
  targetName: string;
  dropType: string;
}>({
  nodeName: '',
  targetName: '',
  dropType: ''
})

// 拖拽结束处理
const handleDragEnd = async (draggingNode: any, dropNode: any, dropType: string) => {
  console.log('拖拽结束', draggingNode.data, dropNode?.data, dropType);

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
    icon: '',
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
    icon: '',
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
    console.log('提交添加', formData.value)

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
    console.log('提交编辑', formData.value)

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
const tagType = (type: string) => {
  const typeMap: Record<string, string> = {
    'M': 'info',
    'C': 'success',
    'F': 'warning'
  }
  return typeMap[type] || 'info'
}
</script>

<style scoped>
.menu-management-container {
  padding: 20px;
}

.top-actions {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.main-content {
  margin-bottom: 20px;
}

.tree-card, .detail-card {
  height: calc(100vh - 220px);
  overflow: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-box {
  width: 60%;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}

.tree-node-label {
  display: flex;
  align-items: center;
  gap: 5px;
}

.menu-icon {
  margin-right: 5px;
}

.menu-type-tag {
  margin-left: 5px;
}

.disabled-menu {
  color: var(--el-text-color-disabled);
  text-decoration: line-through;
}

.node-actions {
  margin-left: 8px;
  display: none;
}

.custom-tree-node:hover .node-actions {
  display: flex;
  gap: 5px;
}

.menu-detail {
  padding: 10px;
}

.detail-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  justify-content: center;
}

.no-selection {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.icon-selector-header {
  padding: 0 0 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.icon-selector-body {
  flex: 1;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 12px;
  padding: 10px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 10px 5px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.icon-item:hover {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
}

.icon-item-active {
  background-color: var(--el-color-primary-light-8);
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.icon-item .el-icon {
  font-size: 24px;
  margin-bottom: 5px;
}

.icon-name {
  font-size: 12px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 100%;
}

.icon-selector-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0 0 0;
  border-top: 1px solid var(--el-border-color-lighter);
}

.selected-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}

.selected-preview {
  margin-right: 5px;
}

/* 暗黑模式适配 */
@media (prefers-color-scheme: dark) {
  .tree-card, .detail-card {
    background-color: var(--el-bg-color);
  }
}
</style>
