<script setup lang="ts">
import { useMediaQuery } from '@vueuse/core'
import { HugeiconsIcon } from '@hugeicons/vue'
import { Cancel01Icon } from '@hugeicons/core-free-icons'
import { addRole, editRole, getRoleList, updateRoleRouters } from '@/api/auth/auth'
import {
  canonicalSelection,
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
  actorRoleId: number | null
  locked: boolean
}>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: [self: boolean, rows?: ManagedRole[]]
  rolesRead: [rows: ManagedRole[]]
  retryTree: []
  unconfirmed: []
}>()
const mobile = useMediaQuery('(max-width: 639px)')
const current = shallowRef<ManagedRole | null>(null)
const draft = reactive(roleDraft())
const selected = ref<number[]>([])
const initial = ref('')
const pending = ref(false)
const failed = ref<'' | 'request' | 'unconfirmed' | 'read'>('')
const submitted = ref(false)
const nameTouched = ref(false)
const discardOpen = ref(false)
const clearOpen = ref(false)
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
const self = computed(() => current.value?.id === props.actorRoleId)
const grantBlocked = computed(
  () =>
    props.treeUnavailable ||
    missing.value.length > 0 ||
    failed.value === 'unconfirmed' ||
    failed.value === 'read',
)

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
  failed.value = ''
  submitted.value = false
  nameTouched.value = false
}
watch(
  () => props.open,
  (open) => {
    if (open) initialize(props.role)
  },
)
function close() {
  if (pending.value) return
  if (dirty.value) discardOpen.value = true
  else emit('update:open', false)
}
function escape(event: Event) {
  event.preventDefault()
  close()
}
function cancelDiscard() {
  discardOpen.value = false
  destination.value = null
}
async function discard() {
  const target = destination.value
  cancelDiscard()
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
function toggle(id: number, checked: boolean) {
  if (!pending.value && !grantBlocked.value)
    selected.value = togglePermission(props.tree, selected.value, id, checked)
}

async function reloadRole() {
  if (!current.value || pending.value) return
  pending.value = true
  try {
    const rows = managedRoles(await getRoleList())
    const role = rows.find((row) => row.id === current.value?.id)
    if (!role) throw new Error('Role missing')
    initialize(role)
    emit('rolesRead', rows)
  } catch {
    failed.value = 'read'
  } finally {
    pending.value = false
  }
}
async function save(confirmedClear = false) {
  if (pending.value || props.locked || props.mode === 'view') return
  submitted.value = true
  if (props.mode !== 'grant' && Object.keys(errors.value).length) {
    const first = Object.keys(errors.value)[0]
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
      current.value.permissions.length &&
      !selectionPermissions(props.tree, selected.value).length
    ) {
      clearOpen.value = true
      return
    }
  }
  pending.value = true
  failed.value = ''
  let grantAccepted = false
  try {
    if (props.mode === 'grant' && current.value) {
      const expected = selectionPermissions(props.tree, selected.value)
      roleWriteSucceeded(
        await updateRoleRouters({
          roleId: current.value.id,
          routerIds: canonicalSelection(props.tree, selected.value),
        }),
      )
      grantAccepted = true
      const rows = managedRoles(await getRoleList())
      const updated = rows.find((role) => role.id === current.value?.id)
      if (!updated || JSON.stringify([...updated.permissions].sort()) !== JSON.stringify(expected))
        throw new Error('Grant read-back mismatch')
      initial.value = serialize()
      emit('saved', self.value, rows)
    } else {
      const payload = rolePayload(draft, props.mode === 'create' ? null : current.value)
      roleWriteSucceeded(await (props.mode === 'create' ? addRole(payload) : editRole(payload)))
      initial.value = serialize()
      emit('saved', props.mode === 'edit' && self.value)
    }
  } catch {
    failed.value = grantAccepted ? 'unconfirmed' : 'request'
    if (grantAccepted) {
      initial.value = serialize()
      emit('unconfirmed')
      if (self.value) {
        pending.value = false
        await useSession().signOut()
      }
    }
  } finally {
    pending.value = false
  }
}
async function confirmClear() {
  clearOpen.value = false
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
      class="w-full! sm:max-w-[600px]! data-[side=bottom]:h-[80dvh] data-[side=bottom]:rounded-t-xl"
      @pointer-down-outside.prevent
      @interact-outside.prevent
      @escape-key-down="escape"
    >
      <UiSheetHeader class="shrink-0 border-b-[0.5px] border-border pr-14"
        ><UiSheetTitle>{{ title }}</UiSheetTitle
        ><UiSheetDescription>{{
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
      <div v-if="mode === 'view' && current" class="min-h-0 flex-1 space-y-6 overflow-y-auto p-6">
        <dl class="grid grid-cols-2 gap-5 text-sm">
          <div>
            <dt class="text-xs text-muted-foreground">角色 ID</dt>
            <dd class="mt-1.5 tabular-nums">{{ current.id }}</dd>
          </div>
          <div>
            <dt class="text-xs text-muted-foreground">状态</dt>
            <dd class="mt-1.5">
              {{ current.status === 1 ? '启用' : current.status === 0 ? '停用' : '未知' }}
            </dd>
          </div>
          <div class="col-span-2">
            <dt class="text-xs text-muted-foreground">备注</dt>
            <dd class="mt-1.5 whitespace-pre-wrap break-words">{{ current.remark || '—' }}</dd>
          </div>
        </dl>
        <div>
          <h3 class="mb-3 text-sm font-medium">权限标识（{{ current.permissions.length }}）</h3>
          <ul v-if="current.permissions.length" class="space-y-2">
            <li
              v-for="permission in current.permissions"
              :key="permission"
              class="break-all rounded-md bg-muted/50 px-3 py-2 font-mono text-xs"
            >
              {{ permission }}
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
          <UiAlert v-if="treeUnavailable" role="alert"
            ><UiAlertTitle>菜单列表暂不可用</UiAlertTitle
            ><UiAlertDescription>请重新读取菜单后再配置授权。</UiAlertDescription
            ><UiButton
              type="button"
              variant="outline"
              size="sm"
              class="mt-2"
              @click="emit('retryTree')"
              >重新读取菜单</UiButton
            ></UiAlert
          >
          <UiAlert v-else-if="missing.length" role="alert"
            ><UiAlertTitle>部分现有权限无法在菜单中匹配</UiAlertTitle
            ><UiAlertDescription
              >为保留现有授权，暂不允许覆盖保存。请先核对以下权限与菜单的关联。</UiAlertDescription
            >
            <ul class="mt-2 space-y-1 break-all text-xs">
              <li v-for="item in missing" :key="item">{{ item }}</li>
            </ul></UiAlert
          >
          <div class="flex items-center justify-between gap-3 text-sm">
            <h3 class="font-medium">菜单与操作</h3>
            <span class="text-xs text-muted-foreground"
              >已选 {{ selected.length }} 项（含父级）</span
            >
          </div>
          <ul v-if="tree.length" class="space-y-1">
            <RolesPermissionBranch
              v-for="node in tree"
              :key="node.id"
              :node="node"
              :selected="selected"
              :disabled="pending || grantBlocked || locked"
              @toggle="toggle"
            />
          </ul>
          <p v-else-if="!treeUnavailable" class="py-8 text-center text-sm text-muted-foreground">
            暂无可分配的菜单或操作。
          </p>
        </template>
        <template v-else>
          <UiField
            ><UiFieldLabel for="role-name">角色名称 <span aria-hidden="true">*</span></UiFieldLabel
            ><UiInput
              id="role-name"
              v-model="draft.roleName"
              required
              maxlength="255"
              :readonly="current?.roleName === 'USER'"
              :disabled="pending"
              :aria-invalid="Boolean((submitted || nameTouched) && errors.roleName)"
              :aria-describedby="
                (submitted || nameTouched) && errors.roleName ? 'role-name-error' : undefined
              "
              @blur="nameTouched = true"
            />
            <p v-if="current?.roleName === 'USER'" class="text-xs text-muted-foreground">
              系统默认角色名称固定。
            </p>
            <p
              v-if="(submitted || nameTouched) && errors.roleName"
              id="role-name-error"
              class="text-xs text-destructive"
            >
              {{ errors.roleName }}
            </p></UiField
          >
          <UiField
            ><UiFieldLabel as="span">状态</UiFieldLabel
            ><UiRadioGroup
              id="role-status"
              v-model="draft.status"
              aria-label="角色状态"
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
            ><UiFieldLabel for="role-remark">备注</UiFieldLabel
            ><UiTextarea
              id="role-remark"
              v-model="draft.remark"
              maxlength="255"
              class="min-h-24"
              :disabled="pending"
            />
            <p v-if="submitted && errors.remark" class="text-xs text-destructive">
              {{ errors.remark }}
            </p></UiField
          >
        </template>
        <p v-if="failed === 'request'" role="alert" class="text-sm text-destructive">
          保存失败，请检查输入或服务连接后重试。填写内容已保留。
        </p>
        <UiAlert v-else-if="failed" role="alert"
          ><UiAlertTitle>{{
            failed === 'unconfirmed' ? '授权已提交，但无法确认最终结果' : '无法重新读取角色'
          }}</UiAlertTitle
          ><UiAlertDescription>请重新读取角色并核对授权后再操作。</UiAlertDescription
          ><UiButton
            type="button"
            variant="outline"
            size="sm"
            class="mt-2"
            :disabled="pending"
            @click="reloadRole"
            >重新读取角色</UiButton
          ></UiAlert
        >
      </form>
      <UiSheetFooter class="shrink-0 flex-row justify-end border-t-[0.5px] border-border"
        ><UiButton variant="outline" :disabled="pending" @click="close">{{
          mode === 'view' ? '关闭' : '取消'
        }}</UiButton
        ><UiButton
          v-if="mode !== 'view'"
          form="role-panel-form"
          type="submit"
          :disabled="
            pending || locked || (mode === 'grant' && grantBlocked) || (mode !== 'create' && !dirty)
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
          >关闭后，本次填写与勾选的内容不会保存。</UiDialogDescription
        ></UiDialogHeader
      ><UiDialogFooter
        ><UiButton variant="outline" @click="cancelDiscard">继续编辑</UiButton
        ><UiButton variant="outline" @click="discard">放弃修改</UiButton></UiDialogFooter
      ></UiDialogContent
    ></UiDialog
  >
  <UiDialog
    :open="clearOpen"
    @update:open="
      (value) => {
        if (!pending) clearOpen = value
      }
    "
    ><UiDialogContent
      :show-close-button="false"
      class="top-[38.2%]! sm:max-w-[400px]!"
      @pointer-down-outside.prevent
      @interact-outside.prevent
      ><UiDialogHeader
        ><UiDialogTitle>清空「{{ current?.roleName }}」的授权？</UiDialogTitle
        ><UiDialogDescription
          >该角色将失去全部菜单与操作授权。保存后无法在此撤销，请确认影响范围。</UiDialogDescription
        ></UiDialogHeader
      ><UiDialogFooter
        ><UiButton variant="outline" @click="clearOpen = false">取消</UiButton
        ><UiButton variant="destructive" @click="confirmClear">清空授权</UiButton></UiDialogFooter
      ></UiDialogContent
    ></UiDialog
  >
</template>
