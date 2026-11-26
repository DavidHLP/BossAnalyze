<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { HugeiconsIcon } from '@hugeicons/vue'
import { Cancel01Icon } from '@hugeicons/core-free-icons'
import { withSessionOwner } from '@/lib/session'
import { addRouter, editRouter, getUserRoutes } from '@/api/router/router'
import { getRoleList } from '@/api/auth/auth'
import { managedRoles } from '@/lib/roles'
import { checkReadAccess, mutationError, mutationResultUnknown, sessionChangedMessage } from '@/lib/rbac-mutations'
import {
  managedMenus,
  menuDraft,
  menuErrors,
  menuPayload,
  menuRows,
  menuWriteSucceeded,
} from '@/lib/menus'
import type { MenuRecord } from '@/lib/menus'
import { useSession } from '@/composables/useSession'
import { registerDirtyDraft } from '@/composables/session-events'

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
  closeAutoFocus: [event: Event]
  saved: [tree: MenuRecord[]]
  read: [tree: MenuRecord[]]
  unconfirmed: []
}>()
const mobile = useMediaQuery('(width < 640px)')
const session = useSession()
const router = useRouter()
const owner = session.capture()
const sessionChanged = computed(() => !session.owns(owner))
const draft = reactive(menuDraft())
const initial = ref('')
const pending = ref(false)
const requestFailure = ref('')
const readFailed = ref(false)
const submitted = ref(false)
const usedPermission = ref('')
const touched = reactive<Record<string, boolean>>({})
const discardOpen = ref(false)
const discardPrompt = ref<HTMLElement | null>(null)
let discardTrigger: HTMLElement | null = null
watch(discardOpen, (open) => {
  if (open) discardPrompt.value?.focus()
}, { flush: 'post' })
function requestDiscard() {
  if (discardOpen.value) return
  discardTrigger = typeof document === 'undefined' ? null : document.activeElement as HTMLElement | null
  discardOpen.value = true
}
const destination = ref<string | null>(null)
const dirty = computed(
  () => props.open && props.mode !== 'view' && JSON.stringify(draft) !== initial.value,
)
const errors = computed<Record<string, string>>(() => ({
  ...menuErrors(draft, props.tree, props.mode === 'edit' ? props.node : null, props.parent),
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
  { key: 'title', label: '显示名称', required: true, placeholder: undefined },
  { key: 'name', label: '路由名称', required: true, placeholder: '例如 user-management' },
  { key: 'permission', label: '权限标识', required: true, placeholder: '例如 system:users:read' },
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
    if (!open) {
      cancelDiscard(false)
      return
    }
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
  if (dirty.value) requestDiscard()
  else emit('update:open', false)
}
function cancelDiscard(restoreFocus = true) {
  discardOpen.value = false
  destination.value = null
  if (restoreFocus) discardTrigger?.focus()
  discardTrigger = null
}
async function discard() {
  const target = destination.value
  cancelDiscard(false)
  initial.value = JSON.stringify(draft)
  emit('update:open', false)
  await nextTick()
  if (target === '/login' && owner && session.owns(owner)) {
    await session.signOut(false, null, undefined, owner)
  } else if (target) await router.push(target)
}
onBeforeRouteLeave((to) => {
  if (session.state.value.invalidated) return true
  if (pending.value) return false
  if (!dirty.value) return true
  destination.value = to.fullPath
  requestDiscard()
  return false
})
function beforeUnload(event: BeforeUnloadEvent) {
  if (!session.state.value.invalidated && (dirty.value || pending.value)) {
    event.preventDefault()
    event.returnValue = ''
  }
}
let unregisterDraft: (() => void) | undefined
onMounted(() => {
  unregisterDraft = registerDirtyDraft('menu-panel', () => dirty.value)
  window.addEventListener('beforeunload', beforeUnload)
})
onUnmounted(() => {
  unregisterDraft?.()
  window.removeEventListener('beforeunload', beforeUnload)
})
async function reread() {
  if (!owner || !session.owns(owner) || pending.value) return
  pending.value = true
  try {
    const tree = managedMenus(await withSessionOwner(owner, getUserRoutes))
    if (!session.owns(owner)) return
    emit('read', tree)
    emit('update:open', false)
  } catch {
    readFailed.value = true
  } finally {
    pending.value = false
  }
}
async function save() {
  if (!owner || !session.owns(owner) || discardOpen.value || pending.value || props.awaitingRead || props.mode === 'view') return
  if (props.mode === 'edit' && (!dirty.value || !props.node)) return
  submitted.value = true
  const first = Object.keys(errors.value)[0]
  if (first) {
    await nextTick()
    const radio = first === 'type' || first === 'status'
    const field = document.querySelector<HTMLElement>(
      `#menu-${first}${radio ? ' button:not(:disabled)' : ''}`,
    )
    const details = field?.closest('details')
    if (details) details.open = true
    field?.focus()
    return
  }
  pending.value = true
  requestFailure.value = ''
  let writeStarted = false
  let accepted = false
  try {
    const current = props.mode === 'edit' ? props.node : null
    const payload = menuPayload(draft, props.tree, current, props.parent)
    if (
      !current &&
      managedRoles(checkReadAccess(await withSessionOwner(owner, getRoleList))).some((role) =>
        role.permissions.includes(payload.permission),
      )
    ) {
      usedPermission.value = payload.permission
      return
    }
    if (!session.owns(owner)) return
    writeStarted = true
    menuWriteSucceeded(await withSessionOwner(owner, () => current ? editRouter(payload) : addRouter(payload)))
    accepted = true
    if (!session.owns(owner)) return
    const tree = managedMenus(await withSessionOwner(owner, getUserRoutes))
    if (!session.owns(owner)) return
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
    if (!session.owns(owner)) return
    if (accepted || (writeStarted && mutationResultUnknown(error))) {
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
      @close-auto-focus="emit('closeAutoFocus', $event)"
      @escape-key-down="
        (event) => {
          event.preventDefault()
          if (discardOpen) cancelDiscard()
          else close()
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
      <div
        v-if="discardOpen"
        ref="discardPrompt"
        tabindex="-1"
        role="group"
        aria-label="放弃未保存的修改？"
        class="mx-4 mt-3 shrink-0 space-y-2 rounded-md border border-border bg-muted p-3"
        @keydown.esc.stop.prevent="cancelDiscard()"
      >
        <p class="text-sm font-medium">放弃未保存的修改？</p>
        <div class="flex flex-wrap justify-end gap-2">
          <UiButton type="button" variant="ghost" class="text-destructive hover:text-destructive"
            @click="discard">放弃修改</UiButton>
          <UiButton type="button" variant="secondary" @click="cancelDiscard()">继续编辑</UiButton>
        </div>
      </div>
      <UiAlert v-if="sessionChanged && mode !== 'view'" variant="destructive" role="alert" class="mx-4 mt-3 w-auto shrink-0">
        <UiAlertDescription>{{ sessionChangedMessage }}</UiAlertDescription>
      </UiAlert>
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
            <dd v-else-if="field.label === '类型'" class="mt-1.5">
              <UiBadge variant="outline">{{ field.value }}</UiBadge>
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
        <details class="rounded-lg bg-muted/50 p-4">
          <summary class="cursor-pointer text-sm font-medium">仅保存元数据（当前不生效）</summary>
          <dl class="mt-3 grid grid-cols-2 gap-4 text-sm">
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
        </details>
      </div>
      <form
        v-else
        id="menu-panel-form"
        class="min-h-0 flex-1 space-y-6 overflow-y-auto p-6"
        :aria-busy="pending"
        novalidate
        @submit.prevent="save"
      >
        <UiAlert v-if="awaitingRead" :variant="readFailed ? 'destructive' : 'default'"
          :role="readFailed ? 'alert' : 'status'"
          :class="!readFailed && 'border-status-warning-foreground/25 bg-status-warning-surface text-status-warning-foreground'">
          <UiAlertTitle>菜单配置已提交，未确认</UiAlertTitle>
          <UiAlertDescription class="text-inherit">{{
            readFailed ? '重新读取失败，请稍后重试。' : '请重新读取菜单，并核对最终配置。'
          }}</UiAlertDescription>
          <UiButton type="button" variant="outline" size="sm" class="mt-2 text-foreground"
            :aria-disabled="pending || sessionChanged" @click="reread">重新读取菜单</UiButton>
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
              :placeholder="field.placeholder"
              :readonly="mode === 'edit' && field.key === 'permission'"
              :aria-invalid="Boolean((submitted || touched[field.key]) && errors[field.key])"
              :aria-describedby="
                (submitted || touched[field.key]) && errors[field.key]
                  ? `menu-${field.key}-error`
                  : undefined
              "
              @blur="touched[field.key] = true"
            />
            <UiFieldError
              v-if="(submitted || touched[field.key]) && errors[field.key]"
              :id="`menu-${field.key}-error`"
              class="text-xs text-destructive"
            >
              {{ errors[field.key] }}
            </UiFieldError>
          </UiField>
          <UiField
            ><UiFieldLabel as="span">类型</UiFieldLabel
            ><UiRadioGroup
              id="menu-type"
              v-model="draft.type"
              aria-label="菜单类型"
              class="flex gap-6"
              :disabled="pending || awaitingRead"
              :aria-invalid="Boolean(submitted && errors.type)"
              :aria-describedby="submitted && errors.type ? 'menu-type-error' : undefined"
            >
              <label
                v-for="option in [
                  { value: 'C', label: '目录' },
                  { value: 'M', label: '菜单' },
                  { value: 'F', label: '操作' },
                ]"
                :key="option.value"
                class="flex items-center gap-2 text-sm"
                :title="option.value === 'F' && node?.children.length ? '请先处理子菜单，再改为操作类型' : undefined"
                ><UiRadioGroupItem :value="option.value"
                  :disabled="option.value === 'F' && Boolean(node?.children.length)" />{{ option.label }}</label
              >
            </UiRadioGroup>
            <p v-if="mode === 'edit' && node?.children.length" class="text-xs text-muted-foreground">
              请先处理子菜单，再改为操作类型。
            </p>
            <UiFieldError v-if="submitted && errors.type" id="menu-type-error">
              {{ errors.type }}
            </UiFieldError></UiField
          >
          <UiField
            ><UiFieldLabel as="span">状态</UiFieldLabel
            ><UiRadioGroup
              id="menu-status"
              v-model="draft.status"
              aria-label="菜单状态"
              class="flex gap-6"
              :disabled="pending || awaitingRead"
              :aria-invalid="Boolean(submitted && errors.status)"
              :aria-describedby="submitted && errors.status ? 'menu-status-error' : undefined"
              ><label class="flex items-center gap-2 text-sm"
                ><UiRadioGroupItem value="1" />启用</label
              ><label class="flex items-center gap-2 text-sm"
                ><UiRadioGroupItem value="0" />停用</label
              ></UiRadioGroup
            >
            <UiFieldError v-if="submitted && errors.status" id="menu-status-error">
              {{ errors.status }}
            </UiFieldError></UiField
          >
          <UiField
            ><UiFieldLabel for="menu-order">排序</UiFieldLabel
            ><UiInput
              id="menu-order"
              v-model="draft.order"
              inputmode="numeric"
              :disabled="pending || awaitingRead"
              :aria-invalid="Boolean((submitted || touched.order) && errors.order)"
              :aria-describedby="(submitted || touched.order) && errors.order ? 'menu-order-error' : undefined"
              @blur="touched.order = true"
            />
            <UiFieldError v-if="(submitted || touched.order) && errors.order" id="menu-order-error">
              {{ errors.order }}
            </UiFieldError></UiField
          >
        </section>
        <component
          v-for="group in [
            { title: '路由配置', fields: routeFields, element: 'section' },
            { title: '仅保存元数据（当前不生效）', fields: metadataFields, element: 'details' },
          ]"
          :is="group.element"
          :key="group.title"
          :class="group.element === 'details' ? 'rounded-lg bg-muted/50 p-4' : 'border-t border-border pt-5'"
        >
          <summary v-if="group.element === 'details'" class="cursor-pointer text-sm font-medium">{{ group.title }}</summary>
          <h3 v-else class="text-sm font-medium">{{ group.title }}</h3>
          <div class="mt-4 space-y-4">
            <UiField v-for="field in group.fields" :key="field.key">
              <UiFieldLabel :for="`menu-${field.key}`">{{ field.label }}<span v-if="field.required" aria-hidden="true"> *</span></UiFieldLabel>
              <UiInput
                :id="`menu-${field.key}`"
                v-model="draft[field.key]"
                maxlength="255"
                :required="field.required"
                :disabled="pending || awaitingRead"
                :aria-invalid="Boolean((submitted || touched[field.key]) && errors[field.key])"
                :aria-describedby="(submitted || touched[field.key]) && errors[field.key] ? `menu-${field.key}-error` : undefined"
                @blur="touched[field.key] = true"
              />
              <UiFieldError v-if="(submitted || touched[field.key]) && errors[field.key]" :id="`menu-${field.key}-error`">
                {{ errors[field.key] }}
              </UiFieldError>
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
          </div>
        </component>
        <div class="border-t border-border pt-5">
          <UiField
            ><UiFieldLabel for="menu-remark">备注</UiFieldLabel
            ><UiTextarea
              id="menu-remark"
              v-model="draft.remark"
              maxlength="255"
              :disabled="pending || awaitingRead"
              :aria-invalid="Boolean((submitted || touched.remark) && errors.remark)"
              :aria-describedby="(submitted || touched.remark) && errors.remark ? 'menu-remark-error' : undefined"
              @blur="touched.remark = true"
            />
            <UiFieldError v-if="(submitted || touched.remark) && errors.remark" id="menu-remark-error">{{ errors.remark }}</UiFieldError>
          </UiField>
        </div>
      </form>
      <UiAlert v-if="requestFailure" variant="destructive" role="alert" class="mx-4 mb-2 w-auto shrink-0">
        <UiAlertDescription><span class="block">{{ requestFailure }}</span><span class="block">填写内容已保留。</span></UiAlertDescription>
      </UiAlert>
      <UiSheetFooter class="shrink-0 flex-row justify-end border-t-[0.5px] border-border">
        <UiButton variant="outline" :disabled="pending" @click="close">{{
          mode === 'view' ? '关闭' : '取消'
        }}</UiButton>
        <UiButton
          v-if="mode !== 'view'"
          type="submit"
          form="menu-panel-form"
          :aria-disabled="pending || sessionChanged || discardOpen || awaitingRead || (mode === 'edit' && !dirty)"
          >{{ pending ? '正在保存…' : mode === 'create' ? '创建菜单' : '保存更改' }}</UiButton
        >
      </UiSheetFooter>
    </UiSheetContent>
  </UiSheet>
</template>
