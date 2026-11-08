<script setup lang="ts">
import { useMediaQuery } from '@vueuse/core'
import { HugeiconsIcon } from '@hugeicons/vue'
import { Cancel01Icon } from '@hugeicons/core-free-icons'
import { addRouter, editRouter, getUserRoutes } from '@/api/router/router'
import { getRoleList } from '@/api/auth/auth'
import { managedRoles } from '@/lib/roles'
import type { Router } from '@/router/index.d'
import {
  managedMenus,
  menuDraft,
  menuErrors,
  menuPayload,
  menuRows,
  menuWriteSucceeded,
} from '@/lib/menus'
import type { MenuRecord } from '@/lib/menus'

const props = defineProps<{
  open: boolean
  mode: 'view' | 'create' | 'edit'
  node: MenuRecord | null
  parent: MenuRecord | null
  tree: MenuRecord[]
}>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: [tree: MenuRecord[]]
  read: [tree: MenuRecord[]]
  unconfirmed: []
}>()
const mobile = useMediaQuery('(max-width: 639px)')
const draft = reactive(menuDraft())
const initial = ref('')
const pending = ref(false)
const failed = ref<'' | 'request' | 'unconfirmed'>('')
const submitted = ref(false)
const usedPermission = ref('')
const touched = reactive<Record<string, boolean>>({})
const discardOpen = ref(false)
const destination = ref<string | null>(null)
const dirty = computed(
  () => props.open && props.mode !== 'view' && JSON.stringify(draft) !== initial.value,
)
const errors = computed<Record<string, string>>(() => ({
  ...menuErrors(draft, props.tree, props.mode === 'edit' ? props.node : null),
  ...(props.mode === 'create' &&
  usedPermission.value &&
  usedPermission.value === draft.permission.trim()
    ? { permission: '权限标识已被角色使用，请使用独立标识' }
    : {}),
}))
const title = computed(
  () => ({ view: '菜单详情', create: '新增菜单', edit: '编辑菜单' })[props.mode],
)
const basicFields = [
  { key: 'title', label: '显示名称', required: true },
  { key: 'name', label: '路由名称', required: true },
  { key: 'permission', label: '权限标识', required: true },
] as const
const routeFields = [
  { key: 'path', label: '路由路径', required: true },
  { key: 'component', label: '组件标识', required: false },
  { key: 'redirect', label: '重定向路径', required: false },
  { key: 'icon', label: '图标标识', required: false },
] as const
const flags = [
  { key: 'hidden', label: '隐藏整个节点' },
  { key: 'metaHidden', label: '隐藏菜单入口' },
  { key: 'alwaysShow', label: '始终显示' },
  { key: 'keepAlive', label: '缓存页面' },
] as const
watch(
  () => props.open,
  (open) => {
    if (!open) return
    Object.assign(draft, menuDraft(props.mode === 'create' ? null : props.node))
    initial.value = JSON.stringify(draft)
    failed.value = ''
    submitted.value = false
    usedPermission.value = ''
    for (const key of Object.keys(touched)) delete touched[key]
  },
)
function close() {
  if (pending.value) return
  if (dirty.value) discardOpen.value = true
  else emit('update:open', false)
}
function cancelDiscard() {
  discardOpen.value = false
  destination.value = null
}
async function discard() {
  const target = destination.value
  cancelDiscard()
  initial.value = JSON.stringify(draft)
  emit('update:open', false)
  await nextTick()
  if (target) await navigateTo(target)
}
onBeforeRouteLeave((to) => {
  if (pending.value) return false
  if (!dirty.value) return true
  destination.value = to.fullPath
  discardOpen.value = true
  return false
})
function beforeUnload(event: BeforeUnloadEvent) {
  if (dirty.value || pending.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onUnmounted(() => window.removeEventListener('beforeunload', beforeUnload))
async function reread() {
  if (pending.value) return
  pending.value = true
  try {
    emit('read', managedMenus(await getUserRoutes()))
    emit('update:open', false)
  } catch {
    failed.value = 'unconfirmed'
  } finally {
    pending.value = false
  }
}
async function save() {
  if (pending.value || failed.value === 'unconfirmed' || props.mode === 'view') return
  submitted.value = true
  if (Object.keys(errors.value).length) {
    document.querySelector<HTMLElement>(`#menu-${Object.keys(errors.value)[0]}`)?.focus()
    return
  }
  pending.value = true
  failed.value = ''
  let accepted = false
  try {
    const current = props.mode === 'edit' ? props.node : null
    const payload = menuPayload(draft, props.tree, current, props.parent)
    if (
      !current &&
      managedRoles(await getRoleList()).some((role) =>
        role.permissions.includes(payload.permission),
      )
    ) {
      usedPermission.value = payload.permission
      return
    }
    // The preserved Router declaration disagrees with the backend string permission and nullable pid.
    const request = payload as unknown as Router
    menuWriteSucceeded(await (current ? editRouter(request) : addRouter(request)))
    accepted = true
    const tree = managedMenus(await getUserRoutes())
    const saved = menuRows(tree).find(({ node }) =>
      current
        ? node.id === current.id
        : node.name === payload.name && node.permission === payload.permission,
    )?.node
    if (
      !saved ||
      JSON.stringify(menuPayload(menuDraft(saved), tree, saved, null)) !==
        JSON.stringify({ id: saved.id, ...payload })
    )
      throw new Error('Menu read-back mismatch')
    initial.value = JSON.stringify(draft)
    emit('saved', tree)
  } catch {
    failed.value = accepted ? 'unconfirmed' : 'request'
    if (accepted) {
      initial.value = JSON.stringify(draft)
      emit('unconfirmed')
    }
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <UiSheet
    :open="open"
    @update:open="
      (value) => {
        if (!value) close()
      }
    "
  >
    <UiSheetContent
      :side="mobile ? 'bottom' : 'right'"
      :show-close-button="false"
      class="w-full! sm:max-w-[600px]! data-[side=bottom]:h-[80dvh] data-[side=bottom]:rounded-t-xl"
      @pointer-down-outside.prevent
      @interact-outside.prevent
      @escape-key-down="
        (event) => {
          event.preventDefault()
          close()
        }
      "
    >
      <UiSheetHeader class="shrink-0 border-b-[0.5px] border-border pr-14">
        <UiSheetTitle>{{ title }}</UiSheetTitle>
        <UiSheetDescription>{{
          mode === 'create' ? `上级：${parent?.meta.metaTitle || '顶级菜单'}` : node?.meta.metaTitle
        }}</UiSheetDescription>
      </UiSheetHeader>
      <UiButton
        variant="ghost"
        size="icon-sm"
        class="absolute right-4 top-4"
        aria-label="关闭菜单面板"
        :disabled="pending"
        @click="close"
        ><HugeiconsIcon :icon="Cancel01Icon" aria-hidden="true"
      /></UiButton>
      <div v-if="mode === 'view' && node" class="min-h-0 flex-1 space-y-6 overflow-y-auto p-6">
        <dl class="grid grid-cols-2 gap-5 text-sm">
          <div
            v-for="field in [
              { label: '菜单 ID', value: node.id },
              { label: '上级 ID', value: node.pid ?? '顶级' },
              {
                label: '类型',
                value:
                  ({ C: '目录', M: '菜单', F: '操作' } as Record<string, string>)[node.meta.type] ||
                  node.meta.type,
              },
              {
                label: '状态',
                value: node.status === 1 ? '启用' : node.status === 0 ? '停用' : '未知',
              },
              { label: '排序', value: node.menuOrder },
              { label: '子项', value: node.children.length },
            ]"
            :key="field.label"
          >
            <dt class="text-xs text-muted-foreground">{{ field.label }}</dt>
            <dd class="mt-1.5 break-all">{{ field.value }}</dd>
          </div>
          <div
            v-for="field in [...basicFields, ...routeFields]"
            :key="field.key"
            class="col-span-2"
          >
            <dt class="text-xs text-muted-foreground">{{ field.label }}</dt>
            <dd class="mt-1.5 break-all">{{ draft[field.key] || '—' }}</dd>
          </div>
          <div v-for="field in flags" :key="field.key">
            <dt class="text-xs text-muted-foreground">{{ field.label }}</dt>
            <dd class="mt-1.5">{{ draft[field.key] ? '是' : '否' }}</dd>
          </div>
          <div class="col-span-2">
            <dt class="text-xs text-muted-foreground">角色元数据</dt>
            <dd class="mt-1.5 break-all">{{ node.meta.metaRoles || '—' }}</dd>
          </div>
          <div class="col-span-2">
            <dt class="text-xs text-muted-foreground">备注</dt>
            <dd class="mt-1.5 whitespace-pre-wrap break-words">{{ node.remark || '—' }}</dd>
          </div>
        </dl>
      </div>
      <form
        v-else
        id="menu-panel-form"
        class="min-h-0 flex-1 space-y-6 overflow-y-auto p-6"
        :aria-busy="pending"
        novalidate
        @submit.prevent="save"
      >
        <section class="space-y-4" aria-label="菜单资料">
          <h3 class="text-sm font-medium">菜单资料</h3>
          <p v-if="mode === 'edit'" class="text-xs leading-5 text-muted-foreground">
            上级和权限标识保持固定，以保留已有层级与角色授权关系。
          </p>
          <UiField v-for="field in basicFields" :key="field.key">
            <UiFieldLabel :for="`menu-${field.key}`"
              >{{ field.label
              }}<span v-if="field.required" aria-hidden="true"> *</span></UiFieldLabel
            >
            <UiInput
              :id="`menu-${field.key}`"
              v-model="draft[field.key]"
              maxlength="255"
              :required="field.required"
              :disabled="pending"
              :readonly="mode === 'edit' && field.key === 'permission'"
              :aria-invalid="Boolean((submitted || touched[field.key]) && errors[field.key])"
              :aria-describedby="
                (submitted || touched[field.key]) && errors[field.key]
                  ? `menu-${field.key}-error`
                  : undefined
              "
              @blur="touched[field.key] = true"
            />
            <p
              v-if="(submitted || touched[field.key]) && errors[field.key]"
              :id="`menu-${field.key}-error`"
              class="text-xs text-destructive"
            >
              {{ errors[field.key] }}
            </p>
          </UiField>
          <UiField
            ><UiFieldLabel as="span">类型</UiFieldLabel
            ><UiRadioGroup
              id="menu-type"
              v-model="draft.type"
              aria-label="菜单类型"
              class="flex gap-6"
              :disabled="pending"
            >
              <label
                v-for="option in [
                  { value: 'C', label: '目录' },
                  { value: 'M', label: '菜单' },
                  { value: 'F', label: '操作' },
                ]"
                :key="option.value"
                class="flex items-center gap-2 text-sm"
                ><UiRadioGroupItem :value="option.value" />{{ option.label }}</label
              >
            </UiRadioGroup>
            <p v-if="submitted && errors.type" class="text-xs text-destructive">
              {{ errors.type }}
            </p></UiField
          >
          <UiField
            ><UiFieldLabel as="span">状态</UiFieldLabel
            ><UiRadioGroup
              v-model="draft.status"
              aria-label="菜单状态"
              class="flex gap-6"
              :disabled="pending"
              ><label class="flex items-center gap-2 text-sm"
                ><UiRadioGroupItem value="1" />启用</label
              ><label class="flex items-center gap-2 text-sm"
                ><UiRadioGroupItem value="0" />停用</label
              ></UiRadioGroup
            >
            <p v-if="submitted && errors.status" class="text-xs text-destructive">
              {{ errors.status }}
            </p></UiField
          >
          <UiField
            ><UiFieldLabel for="menu-order">排序</UiFieldLabel
            ><UiInput
              id="menu-order"
              v-model="draft.order"
              inputmode="numeric"
              :disabled="pending"
              :aria-invalid="Boolean((submitted || touched.order) && errors.order)"
              @blur="touched.order = true"
            />
            <p v-if="(submitted || touched.order) && errors.order" class="text-xs text-destructive">
              {{ errors.order }}
            </p></UiField
          >
        </section>
        <section class="space-y-4 border-t-[0.5px] border-border pt-5" aria-label="路由配置">
          <h3 class="text-sm font-medium">路由配置</h3>
          <p class="text-xs leading-5 text-muted-foreground">
            组件与路径需对应已有前端页面；配置菜单不会自动创建业务页面。
          </p>
          <UiField v-for="field in routeFields" :key="field.key"
            ><UiFieldLabel :for="`menu-${field.key}`"
              >{{ field.label
              }}<span v-if="field.required" aria-hidden="true"> *</span></UiFieldLabel
            ><UiInput
              :id="`menu-${field.key}`"
              v-model="draft[field.key]"
              maxlength="255"
              :required="field.required"
              :disabled="pending"
              :aria-invalid="Boolean((submitted || touched[field.key]) && errors[field.key])"
              @blur="touched[field.key] = true"
            />
            <p
              v-if="(submitted || touched[field.key]) && errors[field.key]"
              class="text-xs text-destructive"
            >
              {{ errors[field.key] }}
            </p></UiField
          >
        </section>
        <section class="space-y-4 border-t-[0.5px] border-border pt-5" aria-label="显示选项">
          <h3 class="text-sm font-medium">显示选项</h3>
          <label v-for="field in flags" :key="field.key" class="flex items-center gap-3 text-sm"
            ><UiCheckbox v-model="draft[field.key]" :disabled="pending" />{{ field.label }}</label
          >
          <UiField
            ><UiFieldLabel for="menu-remark">备注</UiFieldLabel
            ><UiTextarea
              id="menu-remark"
              v-model="draft.remark"
              maxlength="255"
              :disabled="pending"
          /></UiField>
        </section>
        <p v-if="failed === 'request'" role="alert" class="text-sm text-destructive">
          保存失败，请检查输入或服务连接后重试。填写内容已保留。
        </p>
        <UiAlert v-if="failed === 'unconfirmed'" role="alert"
          ><UiAlertTitle>已提交，但无法确认最终结果</UiAlertTitle
          ><UiAlertDescription>请重新读取菜单核对，避免重复提交。</UiAlertDescription
          ><UiButton
            type="button"
            variant="outline"
            size="sm"
            class="mt-2"
            :disabled="pending"
            @click="reread"
            >重新读取菜单</UiButton
          ></UiAlert
        >
      </form>
      <UiSheetFooter class="shrink-0 flex-row justify-end border-t-[0.5px] border-border">
        <UiButton variant="outline" :disabled="pending" @click="close">{{
          mode === 'view' ? '关闭' : '取消'
        }}</UiButton>
        <UiButton
          v-if="mode !== 'view'"
          type="submit"
          form="menu-panel-form"
          :disabled="pending || failed === 'unconfirmed' || (mode === 'edit' && !dirty)"
          >{{ pending ? '正在保存…' : mode === 'create' ? '创建菜单' : '保存更改' }}</UiButton
        >
      </UiSheetFooter>
    </UiSheetContent>
  </UiSheet>
  <UiDialog
    :open="discardOpen"
    @update:open="
      (value) => {
        if (!value) cancelDiscard()
      }
    "
    ><UiDialogContent
      :show-close-button="false"
      class="top-[38.2%]! sm:max-w-[400px]!"
      @pointer-down-outside.prevent
      @interact-outside.prevent
      ><UiDialogHeader
        ><UiDialogTitle>放弃未保存的修改？</UiDialogTitle
        ><UiDialogDescription
          >关闭后，本次填写的菜单配置不会保存。</UiDialogDescription
        ></UiDialogHeader
      ><UiDialogFooter
        ><UiButton variant="outline" @click="cancelDiscard">继续编辑</UiButton
        ><UiButton variant="outline" @click="discard">放弃修改</UiButton></UiDialogFooter
      ></UiDialogContent
    ></UiDialog
  >
</template>
