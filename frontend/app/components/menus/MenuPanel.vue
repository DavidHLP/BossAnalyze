<script setup lang="ts">
import { useMediaQuery } from '@vueuse/core'
import { HugeiconsIcon } from '@hugeicons/vue'
import { Cancel01Icon } from '@hugeicons/core-free-icons'
import { addRouter, editRouter, getUserRoutes } from '@/api/router/router'
import { getRoleList } from '@/api/auth/auth'
import { managedRoles } from '@/lib/roles'
import { mutationError } from '@/lib/rbac-mutations'
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
  awaitingRead: boolean
}>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: [tree: MenuRecord[]]
  read: [tree: MenuRecord[]]
  unconfirmed: []
}>()
const mobile = useMediaQuery('(width < 640px)')
const draft = reactive(menuDraft())
const initial = ref('')
const pending = ref(false)
const requestFailure = ref('')
const readFailed = ref(false)
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
  { key: 'icon', label: '图标标识（如 UserGroupIcon，未知标识使用默认图标）', required: false },
] as const
const metadataFields = [
  { key: 'component', label: '组件标识', required: false },
  { key: 'redirect', label: '重定向路径', required: false },
] as const
const visibilityFlags = [
  { key: 'hidden', label: '整枝隐藏', detail: '从授权树及导航移除该节点与全部子项。' },
  { key: 'metaHidden', label: '仅藏导航', detail: '已授权的页面仍可通过链接访问。' },
] as const
const metadataFlags = [
  { key: 'alwaysShow', label: '始终显示' },
  { key: 'keepAlive', label: '缓存页面' },
] as const
watch(
  () => props.open,
  (open) => {
    if (!open) return
    Object.assign(draft, menuDraft(props.mode === 'create' ? null : props.node))
    initial.value = JSON.stringify(draft)
    requestFailure.value = ''
    readFailed.value = false
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
  if (target === '/login') await useSession().signOut()
  else if (target) await navigateTo(target)
}
onBeforeRouteLeave((to) => {
  if (useSession().state.value.invalidated) return true
  if (pending.value) return false
  if (!dirty.value) return true
  destination.value = to.fullPath
  discardOpen.value = true
  return false
})
function beforeUnload(event: BeforeUnloadEvent) {
  if (!useSession().state.value.invalidated && (dirty.value || pending.value)) {
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
    readFailed.value = true
  } finally {
    pending.value = false
  }
}
async function save() {
  if (pending.value || props.awaitingRead || props.mode === 'view') return
  if (props.mode === 'edit' && (!dirty.value || !props.node)) return
  submitted.value = true
  const first = Object.keys(errors.value)[0]
  if (first) {
    const radio = first === 'type' || first === 'status'
    document.querySelector<HTMLElement>(`#menu-${first}${radio ? ' button' : ''}`)?.focus()
    return
  }
  pending.value = true
  requestFailure.value = ''
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
    menuWriteSucceeded(await (current ? editRouter(payload) : addRouter(payload)))
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
  } catch (error) {
    if (accepted) {
      initial.value = JSON.stringify(draft)
      emit('unconfirmed')
    } else requestFailure.value = mutationError(error, '保存', '路由名称或权限标识发生冲突，请核对后重试。')
  } finally {
    pending.value = false
    if (usedPermission.value && usedPermission.value === draft.permission.trim()) {
      await nextTick()
      document.querySelector<HTMLElement>('#menu-permission')?.focus()
    }
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
      class="w-full! shadow-panel sm:max-w-[600px]! data-[side=bottom]:h-[80dvh] data-[side=bottom]:rounded-t-xl"
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
        <UiSheetDescription class="break-all">{{
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
              { label: '菜单 ID', value: node.id, mono: true },
              { label: '上级 ID', value: node.pid ?? '顶级', mono: node.pid !== null },
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
              { label: '排序', value: node.menuOrder, mono: true },
              { label: '子项', value: node.children.length, mono: true },
            ]"
            :key="field.label"
          >
            <dt class="text-xs text-muted-foreground">{{ field.label }}</dt>
            <dd v-if="field.label === '状态'" class="mt-1.5">
              <StatusBadge :tone="node.status === 1 ? 'success' : node.status === 0 ? 'warning' : 'neutral'">
                {{ field.value }}
              </StatusBadge>
            </dd>
            <dd v-else class="mt-1.5 break-all" :class="{ 'font-mono tabular-nums': 'mono' in field && field.mono }">
              {{ field.value }}
            </dd>
          </div>
          <div
            v-for="field in [...basicFields, ...routeFields]"
            :key="field.key"
            class="col-span-2"
          >
            <dt class="text-xs text-muted-foreground">{{ field.label }}</dt>
            <dd class="mt-1.5 break-all">{{ draft[field.key] || '—' }}</dd>
          </div>
          <div class="col-span-2">
            <dt class="text-xs text-muted-foreground">备注</dt>
            <dd class="mt-1.5 whitespace-pre-wrap break-words">{{ node.remark || '—' }}</dd>
          </div>
        </dl>
        <section aria-labelledby="menu-view-visibility" class="space-y-3 rounded-lg border border-border p-4">
          <h3 id="menu-view-visibility" class="text-sm font-medium">生效可见性开关</h3>
          <dl class="space-y-3 text-sm">
            <div v-for="field in visibilityFlags" :key="field.key">
              <dt class="text-xs text-muted-foreground">{{ field.label }}</dt>
              <dd class="mt-1"><StatusBadge :tone="draft[field.key] ? 'warning' : 'neutral'">
                {{ draft[field.key] ? '开启' : '关闭' }}
              </StatusBadge></dd>
            </div>
          </dl>
        </section>
        <section aria-labelledby="menu-view-metadata" class="space-y-3 rounded-lg bg-muted/50 p-4">
          <h3 id="menu-view-metadata" class="text-sm font-medium">仅保存元数据</h3>
          <p class="text-xs text-muted-foreground">以下配置在当前控制台不生效。</p>
          <dl class="grid grid-cols-2 gap-4 text-sm">
            <div v-for="field in metadataFields" :key="field.key" class="min-w-0">
              <dt class="text-xs text-muted-foreground">{{ field.label }}</dt>
              <dd class="mt-1 break-all">{{ draft[field.key] || '—' }}</dd>
            </div>
            <div v-for="field in metadataFlags" :key="field.key">
              <dt class="text-xs text-muted-foreground">{{ field.label }}</dt>
              <dd class="mt-1">{{ draft[field.key] ? '是' : '否' }}</dd>
            </div>
            <div class="col-span-2">
              <dt class="text-xs text-muted-foreground">角色元数据</dt>
              <dd class="mt-1 break-all">{{ node.meta.metaRoles || '—' }}</dd>
            </div>
          </dl>
        </section>
      </div>
      <form
        v-else
        id="menu-panel-form"
        class="min-h-0 flex-1 space-y-6 overflow-y-auto p-6"
        :aria-busy="pending"
        novalidate
        @submit.prevent="save"
      >
        <UiAlert v-if="awaitingRead" class="border-status-warning/40 bg-status-warning-surface text-status-warning-foreground">
          <UiAlertTitle>菜单配置已提交，未确认</UiAlertTitle>
          <UiAlertDescription class="text-inherit">{{
            readFailed ? '重新读取失败，请稍后重试。' : '请重新读取菜单，并核对最终配置。'
          }}</UiAlertDescription>
          <UiButton type="button" variant="outline" size="sm" class="mt-2 text-foreground"
            :disabled="pending" @click="reread">重新读取菜单</UiButton>
        </UiAlert>
        <section class="space-y-4" aria-label="菜单资料">
          <h3 class="text-sm font-medium">菜单资料</h3>
          <p v-if="mode === 'edit'" class="text-xs leading-5 text-muted-foreground">
            上级 <span :class="{ 'font-mono tabular-nums': node?.pid != null }">{{ node?.pid ?? '顶级' }}</span>。
            上级和权限标识固定，以保留层级与角色授权关系。
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
              :disabled="pending || awaitingRead"
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
              :disabled="pending || awaitingRead"
            >
              <label
                v-for="option in [
                  { value: 'C', label: '目录' },
                  { value: 'M', label: '菜单' },
                  { value: 'F', label: '操作' },
                ]"
                :key="option.value"
                class="flex items-center gap-2 text-sm"
                ><UiRadioGroupItem :value="option.value"
                  :disabled="option.value === 'F' && Boolean(node?.children.length)" />{{ option.label }}</label
              >
            </UiRadioGroup>
            <p v-if="submitted && errors.type" class="text-xs text-destructive">
              {{ errors.type }}
            </p></UiField
          >
          <UiField
            ><UiFieldLabel as="span">状态</UiFieldLabel
            ><UiRadioGroup
              id="menu-status"
              v-model="draft.status"
              aria-label="菜单状态"
              class="flex gap-6"
              :disabled="pending || awaitingRead"
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
              :disabled="pending || awaitingRead"
              :aria-invalid="Boolean((submitted || touched.order) && errors.order)"
              @blur="touched.order = true"
            />
            <p v-if="(submitted || touched.order) && errors.order" class="text-xs text-destructive">
              {{ errors.order }}
            </p></UiField
          >
        </section>
        <section v-for="group in [
          { title: '路由配置', fields: routeFields },
          { title: '仅保存元数据', fields: metadataFields },
        ]" :key="group.title" class="space-y-4" :aria-label="group.title"
          :class="group.fields === metadataFields ? 'rounded-lg bg-muted/50 p-4' : 'border-t border-border pt-5'">
          <h3 class="text-sm font-medium">{{ group.title }}</h3>
          <p v-if="group.fields === metadataFields" class="text-xs text-muted-foreground">
            以下配置在当前控制台不生效。
          </p>
          <UiField v-for="field in group.fields" :key="field.key">
            <UiFieldLabel :for="`menu-${field.key}`">{{ field.label }}<span v-if="field.required" aria-hidden="true"> *</span></UiFieldLabel>
            <UiInput
              :id="`menu-${field.key}`"
              v-model="draft[field.key]"
              maxlength="255"
              :required="field.required"
              :disabled="pending || awaitingRead"
              :aria-invalid="Boolean((submitted || touched[field.key]) && errors[field.key])"
              @blur="touched[field.key] = true"
            />
            <p v-if="(submitted || touched[field.key]) && errors[field.key]" class="text-xs text-destructive">
              {{ errors[field.key] }}
            </p>
          </UiField>
          <template v-if="group.fields === routeFields">
            <fieldset class="space-y-3 rounded-lg border border-border p-4">
              <legend class="px-1 text-sm font-medium">生效可见性开关</legend>
              <label v-for="field in visibilityFlags" :key="field.key" class="flex items-start gap-3 text-sm">
                <UiCheckbox v-model="draft[field.key]" class="mt-0.5 shrink-0" :disabled="pending || awaitingRead"
                  :aria-label="field.label" :aria-describedby="`menu-${field.key}-hint`" />
                <span>{{ field.label }}<span :id="`menu-${field.key}-hint`" class="mt-1 block text-xs text-muted-foreground">{{ field.detail }}</span></span>
              </label>
            </fieldset>
          </template>
          <template v-else>
            <label v-for="field in metadataFlags" :key="field.key" class="flex items-center gap-3 text-sm">
              <UiCheckbox v-model="draft[field.key]" :disabled="pending || awaitingRead" />{{ field.label }}
            </label>
          </template>
        </section>
        <div class="border-t border-border pt-5">
          <UiField
            ><UiFieldLabel for="menu-remark">备注</UiFieldLabel
            ><UiTextarea
              id="menu-remark"
              v-model="draft.remark"
              maxlength="255"
              :disabled="pending || awaitingRead"
          /></UiField>
        </div>
      </form>
      <p v-if="requestFailure" role="alert" class="shrink-0 px-6 text-sm text-destructive">
        {{ requestFailure }}填写内容已保留。
      </p>
      <UiSheetFooter class="shrink-0 flex-row justify-end border-t-[0.5px] border-border">
        <UiButton variant="outline" :disabled="pending" @click="close">{{
          mode === 'view' ? '关闭' : '取消'
        }}</UiButton>
        <UiButton
          v-if="mode !== 'view'"
          type="submit"
          form="menu-panel-form"
          :disabled="pending || awaitingRead || (mode === 'edit' && !dirty)"
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
