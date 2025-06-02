<template>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" :width="dialogWidth" destroy-on-close
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
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, defineProps, defineEmits } from 'vue'
import { useWindowSize } from '@vueuse/core'

// 定义组件接收的属性
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '编辑菜单'
  },
  isAddMode: {
    type: Boolean,
    default: false
  },
  initialFormData: {
    type: Object,
    required: true
  }
})

// 定义组件事件
const emit = defineEmits(['update:modelValue', 'submit', 'cancel'])

// 响应式窗口尺寸
const { width } = useWindowSize()

// 根据窗口宽度计算对话框宽度
const dialogWidth = computed(() => {
  if (width.value < 768) return '95%'
  if (width.value < 992) return '80%'
  return '60%'
})

// 组件内部数据
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const dialogTitle = computed(() => props.title)

// 深拷贝表单数据
const formData = ref(JSON.parse(JSON.stringify(props.initialFormData)))

// 图标选择相关
const iconSearchText = ref('')

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

// 关闭对话框
const close = () => {
  dialogVisible.value = false
  emit('cancel')
}

// 提交表单
const submit = () => {
  emit('submit', JSON.parse(JSON.stringify(formData.value)), props.isAddMode)
}
</script>

<style scoped lang="scss">
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-md);
}
</style>
