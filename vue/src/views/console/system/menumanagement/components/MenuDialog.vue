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
import { ref, computed, defineProps, defineEmits, watch } from 'vue'
import { useWindowSize } from '@vueuse/core'
import { iconList } from '../types/icons'

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

// 确保formData中的meta对象结构完整
const ensureFormDataComplete = () => {
  if (!formData.value.meta) {
    formData.value.meta = {
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
  } else {
    // 确保meta的所有属性都存在
    formData.value.meta = {
      type: formData.value.meta.type || 'C',
      component: formData.value.meta.component || '',
      redirect: formData.value.meta.redirect || null,
      alwaysShow: typeof formData.value.meta.alwaysShow === 'boolean' ? formData.value.meta.alwaysShow : false,
      metaTitle: formData.value.meta.metaTitle || '',
      metaIcon: formData.value.meta.metaIcon || null,
      metaHidden: typeof formData.value.meta.metaHidden === 'boolean' ? formData.value.meta.metaHidden : false,
      metaRoles: formData.value.meta.metaRoles || null,
      metaKeepAlive: typeof formData.value.meta.metaKeepAlive === 'boolean' ? formData.value.meta.metaKeepAlive : false,
      hidden: typeof formData.value.meta.hidden === 'boolean' ? formData.value.meta.hidden : false
    }
  }
}

// 初始化时确保数据完整
ensureFormDataComplete()

// 监听initialFormData变化，确保数据始终完整
watch(() => props.initialFormData, (newVal) => {
  formData.value = JSON.parse(JSON.stringify(newVal))
  ensureFormDataComplete()
}, { deep: true })

// 图标选择相关
const iconSearchText = ref('')

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
</style>
