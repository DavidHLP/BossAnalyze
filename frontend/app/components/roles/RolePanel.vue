<script setup lang="ts">
import { useMediaQuery } from '@vueuse/core'
import { HugeiconsIcon } from '@hugeicons/vue'
import { Cancel01Icon } from '@hugeicons/core-free-icons'
import { withSessionOwner } from '@/lib/session'
import { addRole, editRole, getRoleList, updateRoleRouters } from '@/api/auth/auth'
import { mutationError, mutationResultUnknown, sessionChangedMessage } from '@/lib/rbac-mutations'
import {
  canonicalSelection,
  filterPermissionTree,
  isDefaultRole,
  managedRoles,
  permissionRows,
  roleDraft,
  roleErrors,
  rolePayload,
  roleWriteSucceeded,
  selectionPermissions,
  togglePermission,
  unrepresentedGrants,
} from '@/lib/roles'
import type { ManagedRole, PermissionNode } from '@/lib/roles'

const props = defineProps<{
  open: boolean
  mode: 'view' | 'create' | 'edit' | 'grant'
  role: ManagedRole | null
  tree: PermissionNode[]
  treeUnavailable: boolean
  treePending: boolean
  treeRestricted: boolean
  actorRoleId: number | null
  locked: boolean
  awaitingRead: boolean
}>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  closeAutoFocus: [event: Event]
  saved: [self: boolean, rows?: ManagedRole[], createdName?: string]
  rolesRead: [rows: ManagedRole[], id: number]
  retryTree: []
  unconfirmed: []
}>()
const mobile = useMediaQuery('(width < 640px)')
const session = useSession()
const owner = session.capture()
const sessionChanged = computed(() => !session.owns(owner))
const current = shallowRef<ManagedRole | null>(null)
const draft = reactive(roleDraft())
const selected = ref<number[]>([])
const initial = ref('')
const hadPermissions = ref(false)
const pending = ref(false)
const requestFailure = ref('')
const readFailed = ref(false)
const submitted = ref(false)
const nameTouched = ref(false)
const discardOpen = ref(false)
const discardPrompt = ref<HTMLElement | null>(null)
let discardTrigger: HTMLElement | null = null
watch(discardOpen, (open) => {
  if (open) discardPrompt.value?.focus()
}, { flush: 'post' })
function requestDiscard() {
  if (discardOpen.value) return
  if (clearOpen.value) cancelClear()
  discardTrigger = typeof document === 'undefined' ? null : document.activeElement as HTMLElement | null
  discardOpen.value = true
}
const clearOpen = ref(false)
const clearPrompt = ref<HTMLElement | null>(null)
let clearTrigger: HTMLElement | null = null
watch(clearOpen, (open) => {
  if (open) clearPrompt.value?.focus()
}, { flush: 'post' })
function cancelClear() {
  clearOpen.value = false
  clearTrigger?.focus()
  clearTrigger = null
}
const destination = ref<string | null>(null)
const title = computed(
  () => ({ view: '角色详情', create: '新增角色', edit: '编辑角色', grant: '菜单授权' })[props.mode],
)
const serialize = () => JSON.stringify(props.mode === 'grant' ? selected.value : draft)
const dirty = computed(() => props.open && props.mode !== 'view' && serialize() !== initial.value)
const errors = computed(() => roleErrors(draft))
const missing = computed(() =>
  current.value ? unrepresentedGrants(current.value, props.tree) : [],
)
const self = computed(() => !!current.value && current.value.id === props.actorRoleId)
const grantBlocked = computed(
  () =>
    props.treeUnavailable ||
    missing.value.length > 0 ||
    props.awaitingRead,
)

const permissionQuery = ref('')
const onlySelected = ref(false)
const collapsed = ref<number[]>([])
const displayedTree = computed(() =>
  filterPermissionTree(props.tree, permissionQuery.value, onlySelected.value ? selected.value : null),
)
const permissionCount = computed(() => new Set(permissionRows(props.tree)
  .filter(({ node }) => node.permission && selected.value.includes(node.id))
  .map(({ node }) => node.permission)).size)
const permissionTitles = computed(() => {
  const titles = new Map<string, string[]>()
  for (const { node } of permissionRows(props.tree)) {
    if (!node.permission) continue
    const names = titles.get(node.permission) ?? []
    if (!names.includes(node.title)) names.push(node.title)
    titles.set(node.permission, names)
  }
  return titles
})
watch([permissionQuery, onlySelected], () => { collapsed.value = [] })
function expand(id: number, open: boolean) {
  collapsed.value = open
    ? collapsed.value.filter((value) => value !== id)
    : [...collapsed.value, id]
}
function collapseAll() {
  collapsed.value = permissionRows(displayedTree.value)
    .filter(({ node }) => node.children.length)
    .map(({ node }) => node.id)
}

function initialize(role: ManagedRole | null) {
  current.value = role
  Object.assign(draft, roleDraft(role))
  selected.value = canonicalSelection(
    props.tree,
    permissionRows(props.tree)
      .filter(
        ({ node }) =>
          role?.routerIds.includes(node.id) ||
          (node.permission && role?.permissions.includes(node.permission)),
      )
      .map(({ node }) => node.id),
  )
  initial.value = serialize()
  hadPermissions.value = selectionPermissions(props.tree, selected.value).length > 0
  requestFailure.value = ''
  readFailed.value = false
  submitted.value = false
  nameTouched.value = false
}
watch(
  () => props.open,
  (open) => {
    if (open) {
      permissionQuery.value = ''
      onlySelected.value = false
      collapsed.value = []
      initialize(props.role)
    }
    else {
      cancelDiscard(false)
      clearOpen.value = false
    }
  },
)
function close() {
  if (pending.value) return
  if (dirty.value) requestDiscard()
  else emit('update:open', false)
}
function escape(event: Event) {
  event.preventDefault()
  if (discardOpen.value) cancelDiscard()
  else if (clearOpen.value) cancelClear()
  else close()
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
  emit('update:open', false)
  await nextTick()
  if (target === '/login' && owner && session.owns(owner)) {
    await session.signOut(false, null, undefined, owner)
  } else if (target) await navigateTo(target)
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
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onUnmounted(() => window.removeEventListener('beforeunload', beforeUnload))
function toggle(id: number, checked: boolean) {
  if (session.owns(owner) && !pending.value && !grantBlocked.value && !discardOpen.value && !clearOpen.value)
    selected.value = togglePermission(props.tree, selected.value, id, checked)
}

async function reloadRole() {
  if (!owner || !session.owns(owner) || !current.value || pending.value) return
  pending.value = true
  try {
    const rows = managedRoles(await withSessionOwner(owner, getRoleList))
    if (!session.owns(owner)) return
    const role = rows.find((row) => row.id === current.value?.id)
    const id = current.value.id
    initialize(role ?? null)
    emit('rolesRead', rows, id)
    if (!role) emit('update:open', false)
  } catch {
    readFailed.value = true
  } finally {
    pending.value = false
  }
}
async function save(confirmedClear = false) {
  if (
    !owner || !session.owns(owner) || discardOpen.value || clearOpen.value ||
    pending.value || props.locked || props.mode === 'view' ||
    (props.mode !== 'create' && !dirty.value)
  ) return
  submitted.value = true
  if (props.mode !== 'grant' && Object.keys(errors.value).length) {
    const first = Object.keys(errors.value)[0]
    await nextTick()
    document
      .querySelector<HTMLElement>(
        first === 'roleName'
          ? '#role-name'
          : first === 'status'
            ? '#role-status button'
            : '#role-remark',
      )
      ?.focus()
    return
  }
  if (props.mode === 'grant') {
    if (!current.value || grantBlocked.value) return
    if (
      !confirmedClear &&
      hadPermissions.value &&
      !selectionPermissions(props.tree, selected.value).length
    ) {
      clearTrigger = document.activeElement as HTMLElement | null
      clearOpen.value = true
      return
    }
  }
  pending.value = true
  requestFailure.value = ''
  let grantAccepted = false
  try {
    if (props.mode === 'grant' && current.value) {
      const roleId = current.value.id
      const expected = selectionPermissions(props.tree, selected.value)
      roleWriteSucceeded(
        await withSessionOwner(owner, () => updateRoleRouters({
          roleId,
          routerIds: canonicalSelection(props.tree, selected.value),
        })),
      )
      grantAccepted = true
      if (!session.owns(owner)) return
      const rows = managedRoles(await withSessionOwner(owner, getRoleList))
      if (!session.owns(owner)) return
      const updated = rows.find((role) => role.id === roleId)
      if (!updated || JSON.stringify([...updated.permissions].sort()) !== JSON.stringify(expected))
        throw new Error('Grant read-back mismatch')
      initial.value = serialize()
      emit('saved', self.value, rows)
    } else {
      const payload = rolePayload(draft, props.mode === 'create' ? null : current.value)
      roleWriteSucceeded(await withSessionOwner(owner, () =>
        props.mode === 'create' ? addRole(payload) : editRole(payload),
      ))
      if (!session.owns(owner)) return
      initial.value = serialize()
      emit('saved', props.mode === 'edit' && self.value, undefined, props.mode === 'create' ? payload.roleName : undefined)
    }
  } catch (error) {
    if (!session.owns(owner)) return
    if (props.mode === 'grant' && (grantAccepted || mutationResultUnknown(error))) {
      initial.value = serialize()
      emit('unconfirmed')
      if (self.value) {
        pending.value = false
        await session.signOut(true, null, undefined, owner)
      }
    } else requestFailure.value = mutationError(error, '保存')
  } finally {
    pending.value = false
  }
}
async function confirmClear() {
  if (!session.owns(owner) || !clearOpen.value || discardOpen.value) return
  cancelClear()
  await save(true)
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
      @escape-key-down="escape"
    >
      <UiSheetHeader class="shrink-0 border-b-[0.5px] border-border pr-14"
        ><UiSheetTitle>{{ title }}</UiSheetTitle
        ><UiSheetDescription class="break-all">{{
          mode === 'create' ? '先创建角色，再按需配置菜单授权' : current?.roleName
        }}</UiSheetDescription></UiSheetHeader
      >
      <UiButton
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label="关闭角色面板"
        class="absolute right-4 top-4"
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
      <div
        v-if="clearOpen"
        ref="clearPrompt"
        tabindex="-1"
        role="group"
        aria-label="确认清空授权"
        class="mx-4 mt-3 shrink-0 space-y-2 rounded-md border border-border bg-muted p-3"
        @keydown.esc.stop.prevent="cancelClear"
      >
        <p class="text-sm font-medium">清空「{{ current?.roleName }}」的授权？</p>
        <p class="text-sm">该角色将失去全部菜单与操作授权。</p>
        <div class="flex flex-wrap justify-end gap-2">
          <UiButton type="button" variant="secondary" @click="cancelClear">取消</UiButton>
          <UiButton type="button" variant="destructive" :aria-disabled="sessionChanged" @click="confirmClear">清空授权</UiButton>
        </div>
      </div>
      <UiAlert v-if="sessionChanged && mode !== 'view'" variant="destructive" role="alert" class="mx-4 mt-3 w-auto shrink-0">
        <UiAlertDescription>{{ sessionChangedMessage }}</UiAlertDescription>
      </UiAlert>
      <div v-if="mode === 'view' && current" class="min-h-0 flex-1 space-y-6 overflow-y-auto p-6">
        <dl class="grid grid-cols-2 gap-5 text-sm">
          <div>
            <dt class="text-xs text-muted-foreground">角色 ID</dt>
            <dd class="mt-1.5 font-mono tabular-nums">{{ current.id }}</dd>
          </div>
          <div>
            <dt class="text-xs text-muted-foreground">状态</dt>
            <dd class="mt-1.5">
              <StatusBadge :tone="current.status === 1 ? 'success' : current.status === 0 ? 'warning' : 'neutral'">
                {{ current.status === 1 ? '启用' : current.status === 0 ? '停用' : '未知' }}
              </StatusBadge>
            </dd>
          </div>
          <div class="col-span-2">
            <dt class="text-xs text-muted-foreground">备注</dt>
            <dd class="mt-1.5 whitespace-pre-wrap break-words">{{ current.remark || '—' }}</dd>
          </div>
        </dl>
        <div>
          <h3 class="mb-3 text-sm font-medium">
            菜单权限 <span class="font-mono tabular-nums">{{ current.permissions.length }}</span>
          </h3>
          <ul v-if="current.permissions.length" class="space-y-2">
            <li
              v-for="permission in current.permissions"
              :key="permission"
              class="break-all rounded-md bg-muted/50 px-3 py-2"
            >
              <p class="text-sm font-medium">{{ permissionTitles.get(permission)?.join('、') ?? '未匹配到菜单' }}</p>
              <p class="font-mono text-xs text-muted-foreground">{{ permission }}</p>
            </li>
          </ul>
          <p v-else class="text-sm text-muted-foreground">此角色尚未分配权限。</p>
        </div>
      </div>
      <form
        v-else
        id="role-panel-form"
        class="min-h-0 flex-1 space-y-5 overflow-y-auto p-6"
        novalidate
        :aria-busy="pending"
        @submit.prevent="save()"
      >
        <p v-if="self" class="text-sm text-muted-foreground">
          正在修改当前账号的角色，保存成功后需重新登录。
        </p>
        <template v-if="mode === 'grant'">
          <p class="text-sm leading-6 text-muted-foreground">
            勾选子项会包含必要父级；共享同一权限标识的条目联动选择。取消父级会移除其下授权。
          </p>
          <UiAlert v-if="awaitingRead" :variant="readFailed ? 'destructive' : 'default'"
            :role="readFailed ? 'alert' : 'status'"
            :class="!readFailed && 'border-status-warning/40 bg-status-warning-surface text-status-warning-foreground'"
            ><UiAlertTitle>授权已提交，未确认</UiAlertTitle
            ><UiAlertDescription class="text-inherit">{{
              readFailed ? '重新读取失败，请稍后重试。' : '请重新读取角色，并核对当前授权。'
            }}</UiAlertDescription
            ><UiButton
              type="button"
              variant="outline"
              size="sm"
              class="mt-2 text-foreground"
              :aria-disabled="pending || sessionChanged"
              @click="reloadRole"
              >重新读取角色</UiButton
            ></UiAlert
          >
          <UiAlert v-if="treeUnavailable" variant="destructive" role="alert"
            ><UiAlertTitle>{{ treeRestricted ? '当前账号无权读取菜单列表' : '菜单列表暂不可用' }}</UiAlertTitle
            ><UiAlertDescription>{{ treeRestricted ? '请联系管理员确认权限。' : '请重新读取菜单后再配置授权。' }}</UiAlertDescription
            ><UiButton
              type="button"
              variant="outline"
              size="sm"
              class="mt-2"
              :aria-disabled="treePending || sessionChanged"
              @click="!treePending && !sessionChanged && emit('retryTree')"
              >重新读取菜单</UiButton
            ></UiAlert
          >
          <UiAlert v-else-if="missing.length" variant="destructive" role="alert"
            ><UiAlertTitle>部分现有权限无法在菜单中匹配</UiAlertTitle
            ><UiAlertDescription class="text-inherit"
              >为保留现有授权，暂不允许覆盖保存。请先核对以下权限与菜单的关联。</UiAlertDescription
            >
            <ul class="mt-2 space-y-1 break-all font-mono text-xs tabular-nums">
              <li v-for="item in missing" :key="item">{{ item }}</li>
            </ul></UiAlert
          >
          <div class="space-y-3">
            <div class="flex flex-wrap items-center justify-between gap-2 text-sm">
              <h3 class="font-medium">菜单与操作</h3>
              <span class="text-xs text-muted-foreground" role="status">
                已选 <span class="font-mono tabular-nums">{{ permissionCount }}</span> 项权限（不含仅作路径的父级）
              </span>
            </div>
            <UiField>
              <UiFieldLabel for="permission-search">搜索菜单或权限标识</UiFieldLabel>
              <UiInput id="permission-search" v-model="permissionQuery" placeholder="输入菜单标题或权限码" />
            </UiField>
            <div class="flex flex-wrap items-center gap-2">
              <UiButton type="button" variant="outline" size="sm" aria-controls="role-permission-tree"
                @click="collapsed = []">展开全部</UiButton>
              <UiButton type="button" variant="outline" size="sm" aria-controls="role-permission-tree"
                @click="collapseAll">收起全部</UiButton>
              <label class="ml-1 flex items-center gap-2 text-sm">
                <UiCheckbox v-model="onlySelected" />仅看已选
              </label>
            </div>
          </div>
          <div id="role-permission-tree">
            <ul v-if="displayedTree.length" class="space-y-1 rounded-lg border border-border bg-card p-2">
              <RolesPermissionBranch
                v-for="node in displayedTree"
                :key="node.id"
                :node="node"
                :selected="selected"
                :collapsed="collapsed"
                :disabled="pending || grantBlocked || locked || sessionChanged || discardOpen || clearOpen"
                @toggle="toggle"
                @expand="expand"
              />
            </ul>
            <p v-else-if="!treeUnavailable" class="py-8 text-center text-sm text-muted-foreground">
              {{ tree.length ? '没有符合筛选条件的权限。' : '暂无可分配的菜单或操作。' }}
            </p>
          </div>
        </template>
        <template v-else>
          <UiField
            ><UiFieldLabel for="role-name">角色名称 <span aria-hidden="true">*</span></UiFieldLabel
            ><UiInput
              id="role-name"
              v-model="draft.roleName"
              required
              maxlength="255"
              :readonly="isDefaultRole(current)"
              :disabled="pending"
              :aria-invalid="Boolean((submitted || nameTouched) && errors.roleName)"
              :aria-describedby="
                (submitted || nameTouched) && errors.roleName ? 'role-name-error' : undefined
              "
              @blur="nameTouched = true"
            />
            <p v-if="isDefaultRole(current)" class="text-xs text-muted-foreground">
              系统默认角色名称固定。
            </p>
            <UiFieldError
              v-if="(submitted || nameTouched) && errors.roleName"
              id="role-name-error"
              class="text-xs text-destructive"
            >
              {{ errors.roleName }}
            </UiFieldError></UiField
          >
          <UiField
            ><UiFieldLabel as="span">状态</UiFieldLabel
            ><UiRadioGroup
              id="role-status"
              v-model="draft.status"
              aria-label="角色状态"
              class="flex gap-6"
              :disabled="pending"
              :aria-invalid="Boolean(submitted && errors.status)"
              :aria-describedby="submitted && errors.status ? 'role-status-error' : undefined"
              ><label class="flex items-center gap-2 text-sm"
                ><UiRadioGroupItem value="1" />启用</label
              ><label class="flex items-center gap-2 text-sm"
                ><UiRadioGroupItem value="0" />停用</label
              ></UiRadioGroup
            >
            <UiFieldError v-if="submitted && errors.status" id="role-status-error">
              {{ errors.status }}
            </UiFieldError></UiField
          >
          <UiField
            ><UiFieldLabel for="role-remark">备注</UiFieldLabel
            ><UiTextarea
              id="role-remark"
              v-model="draft.remark"
              maxlength="255"
              class="min-h-24"
              :disabled="pending"
              :aria-invalid="Boolean(submitted && errors.remark)"
              :aria-describedby="submitted && errors.remark ? 'role-remark-error' : undefined"
            />
            <UiFieldError v-if="submitted && errors.remark" id="role-remark-error">
              {{ errors.remark }}
            </UiFieldError></UiField
          >
        </template>
      </form>
      <UiAlert v-if="requestFailure" variant="destructive" role="alert" class="mx-4 mb-2 w-auto shrink-0">
        <UiAlertDescription><span class="block">{{ requestFailure }}</span><span class="block">填写内容已保留。</span></UiAlertDescription>
      </UiAlert>
      <UiSheetFooter class="shrink-0 flex-row justify-end border-t-[0.5px] border-border"
        ><UiButton variant="outline" :disabled="pending" @click="close">{{
          mode === 'view' ? '关闭' : '取消'
        }}</UiButton
        ><UiButton
          v-if="mode !== 'view'"
          form="role-panel-form"
          type="submit"
          :aria-disabled="
            pending || sessionChanged || discardOpen || clearOpen || locked || (mode === 'grant' && grantBlocked) || (mode !== 'create' && !dirty)
          "
          >{{
            pending
              ? '正在保存…'
              : mode === 'grant'
                ? '保存授权'
                : mode === 'create'
                  ? '创建角色'
                  : '保存更改'
          }}</UiButton
        ></UiSheetFooter
      >
    </UiSheetContent>
  </UiSheet>
</template>
