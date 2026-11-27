<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, shallowRef, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { HugeiconsIcon } from '@hugeicons/vue'
import { Add01Icon, RefreshIcon, Search01Icon } from '@hugeicons/core-free-icons'
import { deleteRole, getRoleList, getUserRole } from '@/api/auth/auth'
import { getUserRoutes } from '@/api/router/router'
import { getUserManageInfo } from '@/api/user/user'
import { checkReadAccess, listPage, listSize, listStatus, mutationError, mutationResultUnknown, permissionDenied, refreshNavigation, restoreActionFocus, sessionChangedMessage, unknownWriteMessage } from '@/lib/rbac-mutations'
import { currentRoleId, isDefaultRole, managedRoles, permissionTree, roleWriteSucceeded } from '@/lib/roles'
import { findMenu, withSessionOwner } from '@/lib/session'
import { userPage } from '@/lib/users'
import type { ManagedRole, PermissionNode } from '@/lib/roles'
import { useSession } from '@/composables/useSession'
import RolePanel from '@/components/roles/RolePanel.vue'

const session = useSession()
const owner = session.capture()
const sessionChanged = computed(() => !session.owns(owner))
const route = useRoute()
const router = useRouter()
const roles = shallowRef<ManagedRole[]>([])
const tree = shallowRef<PermissionNode[]>([])
const actorId = ref<number | null>(null)
const pending = ref(true)
const failed = ref(false)
const restricted = ref(false)
const treePending = ref(true)
const treeFailed = ref(false)
const treeRestricted = ref(false)
const actorPending = ref(true)
const actorFailed = ref(false)
const actorRestricted = ref(false)
const filters = reactive({
  name: typeof route.query.name === 'string' ? route.query.name.trim().slice(0, 255) : '',
  status: listStatus(route.query.status),
})
const applied = reactive({ ...filters })
const size = ref(listSize(route.query.size))
const page = ref(listPage(route.query.page, size.value))
const notice = ref('')
const createdName = ref('')
const createdRole = computed(() => roles.value.find((role) => role.roleName === createdName.value))
watch([applied, page, size], () => {
  if (sessionChanged.value) return
  void router.replace({ query: {
    ...route.query,
    name: applied.name || undefined,
    status: applied.status === 'all' ? undefined : applied.status,
    page: page.value > 1 ? String(page.value) : undefined,
    size: size.value !== 10 ? String(size.value) : undefined,
  } })
})
const filtered = computed(() =>
  roles.value.filter(
    (role) =>
      (!applied.name ||
        role.roleName.toLocaleLowerCase().includes(applied.name.toLocaleLowerCase())) &&
      (applied.status === 'all' || role.status === Number(applied.status)),
  ),
)
const visible = computed(() =>
  filtered.value.slice((page.value - 1) * size.value, page.value * size.value),
)
const unconfirmedWrite = ref<'self' | 'other' | null>(null)
const locked = computed(() => pending.value || actorPending.value || actorId.value === null ||
  restricted.value || sessionChanged.value || unconfirmedWrite.value !== null)
const treeUnavailable = computed(() => treePending.value || treeFailed.value)
const open = ref(false)
const mode = ref<'view' | 'create' | 'edit' | 'grant'>('view')
const selected = shallowRef<ManagedRole | null>(null)
const awaitingRead = reactive(new Set<number>())
const deletion = shallowRef<ManagedRole | null>(null)
const deleting = ref(false)
const deleteFailure = ref('')
const roleInUse = ref(false)
const listTitle = ref<HTMLElement | null>(null)
let sheetTrigger: HTMLElement | null = null
let deleteTrigger: HTMLElement | null = null

function restoreSheetFocus(event: Event) {
  restoreActionFocus(event, sheetTrigger, listTitle.value)
  sheetTrigger = null
}
function restoreDeleteFocus(event: Event) {
  restoreActionFocus(event, deleteTrigger, listTitle.value)
  deleteTrigger = null
}
onBeforeRouteLeave(() => session.state.value.invalidated || !deleting.value)
function beforeUnload(event: BeforeUnloadEvent) {
  if (session.state.value.invalidated || !deleting.value) return
  event.preventDefault()
  event.returnValue = ''
}

function clampPage() {
  page.value = Math.min(page.value, Math.max(1, Math.ceil(filtered.value.length / size.value)))
}
async function refreshRoles() {
  if (!owner || pending.value || deleting.value || !session.owns(owner)) return
  if (!(await loadRoles()) || !session.owns(owner) || !unconfirmedWrite.value) return
  const self = unconfirmedWrite.value === 'self'
  open.value = false
  deletion.value = null
  unconfirmedWrite.value = null
  if (self) {
    await session.signOut(true, null, undefined, owner)
    return
  }
  notice.value = '已刷新角色列表，请核对实际状态。'
}
function lockWrite(self = false) {
  unconfirmedWrite.value = self ? 'self' : 'other'
  notice.value = ''
  createdName.value = ''
}
async function loadRoles(): Promise<boolean> {
  if (!owner || !session.owns(owner)) return false
  pending.value = true
  failed.value = false
  restricted.value = false
  try {
    const result = managedRoles(checkReadAccess(await withSessionOwner(owner, getRoleList)))
    if (!session.owns(owner)) return false
    roles.value = result
    clampPage()
    return true
  } catch (error) {
    if (!session.owns(owner)) return false
    roles.value = []
    failed.value = true
    restricted.value = permissionDenied(error)
    return false
  } finally {
    pending.value = false
  }
}
function refreshTree() {
  if (!treePending.value) void loadTree()
}
async function loadTree() {
  if (!owner || !session.owns(owner)) return
  treePending.value = true
  treeFailed.value = false
  treeRestricted.value = false
  try {
    const result = permissionTree(checkReadAccess(await withSessionOwner(owner, getUserRoutes)))
    if (!session.owns(owner)) return
    tree.value = result
  } catch (error) {
    if (!session.owns(owner)) return
    tree.value = []
    treeFailed.value = true
    treeRestricted.value = permissionDenied(error)
  } finally {
    treePending.value = false
  }
}
function refreshActor() {
  if (!actorPending.value) void loadActor()
}
async function loadActor() {
  if (!owner || !session.owns(owner)) return
  actorPending.value = true
  actorFailed.value = false
  actorRestricted.value = false
  try {
    const result = currentRoleId(checkReadAccess(await withSessionOwner(owner, getUserRole)))
    if (!session.owns(owner)) return
    actorId.value = result
  } catch (error) {
    if (!session.owns(owner)) return
    actorId.value = null
    actorFailed.value = true
    actorRestricted.value = permissionDenied(error)
  } finally {
    actorPending.value = false
  }
}
function search() {
  if (pending.value || sessionChanged.value) return
  Object.assign(applied, { name: filters.name.trim(), status: filters.status })
  page.value = 1
  notice.value = ''
}
function reset() {
  if (pending.value || sessionChanged.value) return
  Object.assign(filters, { name: '', status: 'all' })
  search()
}
function changeSize(value: unknown) {
  if (pending.value || sessionChanged.value) return
  size.value = listSize(value)
  page.value = 1
}
function show(event: Event, nextMode: typeof mode.value, role: ManagedRole | null = null) {
  if (nextMode !== 'view' && (locked.value || !session.owns(owner))) return
  if (nextMode === 'grant' && treeUnavailable.value && !(role && awaitingRead.has(role.id))) return
  if (nextMode === 'edit' && role && awaitingRead.has(role.id)) return
  sheetTrigger = event.currentTarget as HTMLElement
  mode.value = nextMode
  selected.value = role
  open.value = true
}
async function saved(self: boolean, rows?: ManagedRole[], name?: string) {
  open.value = false
  if (self) {
    if (owner) await session.signOut(true, null, undefined, owner)
    return
  }
  createdName.value = name ?? ''
  if (!rows) {
    notice.value = '角色已创建'
    await loadRoles()
    return
  }
  roles.value = rows
  clampPage()
  await refreshNavigation(session.refresh, notice, mode.value === 'grant' ? '菜单授权已更新并回读确认。' : '角色信息已更新。')
}
function readRoles(rows: ManagedRole[], id: number) {
  roles.value = rows
  awaitingRead.delete(id)
  notice.value = '已重新读取角色，请核对当前授权。'
  clampPage()
}
function lockGrant() {
  if (!selected.value) return
  awaitingRead.add(selected.value.id)
  notice.value = '授权已提交，未确认。请在该角色的菜单授权面板重新读取。'
}
function confirmDelete(event: Event, role: ManagedRole) {
  if (locked.value || !session.owns(owner) || awaitingRead.has(role.id) || role.id === actorId.value || isDefaultRole(role)) return
  deleteTrigger = event.currentTarget as HTMLElement
  deletion.value = role
  deleteFailure.value = ''
  roleInUse.value = false
}

async function hasLinkedUsers(roleId: number) {
  if (!owner || !session.owns(owner) || !findMenu(session.state.value.menus, '/console/users')) return false
  try {
    const response = await withSessionOwner(owner, () => getUserManageInfo(1, 1, { roleId }))
    return session.owns(owner) && userPage(response).users.some(user => user.roleId === roleId)
  } catch {
    return false
  }
}

async function remove() {
  if (
    !owner || !session.owns(owner) || !deletion.value ||
    awaitingRead.has(deletion.value.id) ||
    deleting.value ||
    locked.value ||
    deletion.value.id === actorId.value ||
    isDefaultRole(deletion.value)
  )
    return
  const id = deletion.value.id
  deleting.value = true
  deleteFailure.value = ''
  roleInUse.value = false
  let accepted = false
  try {
    if (await hasLinkedUsers(id)) {
      roleInUse.value = true
      return
    }
    if (!session.owns(owner)) return
    roleWriteSucceeded(await withSessionOwner(owner, () => deleteRole(id)))
    accepted = true
    const read = await loadRoles()
    if (!session.owns(owner)) return
    if (!read || roles.value.some((role) => role.id === id)) throw new Error('Delete not confirmed')
    deleteTrigger = null
    deletion.value = null
    await refreshNavigation(session.refresh, notice, '角色已删除。')
  } catch (error) {
    if (!session.owns(owner)) return
    if (accepted || mutationResultUnknown(error)) {
      lockWrite()
      return
    }
    const linked = !permissionDenied(error) && await hasLinkedUsers(id)
    if (!session.owns(owner)) return
    if (linked) roleInUse.value = true
    else deleteFailure.value = mutationError(error, '删除')
  } finally {
    deleting.value = false
  }
}
onMounted(() => {
  window.addEventListener('beforeunload', beforeUnload)
  void loadRoles()
  void loadTree()
  void loadActor()
})
onUnmounted(() => window.removeEventListener('beforeunload', beforeUnload))
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="roles-title">
    <header class="flex flex-wrap items-center justify-between gap-4">
      <div class="space-y-1">
        <h1 id="roles-title" class="text-2xl font-semibold tracking-tight sm:text-3xl">角色管理</h1>
        <p class="text-sm text-muted-foreground">维护角色信息、菜单访问与操作权限。</p>
      </div>
      <UiButton :disabled="locked" @click="show($event, 'create')"
        ><HugeiconsIcon
          :icon="Add01Icon"
          class="size-4"
          data-icon="inline-start"
          aria-hidden="true"
        />新增角色</UiButton
      >
    </header>
    <UiAlert v-if="sessionChanged" variant="destructive" role="alert">
      <UiAlertDescription>{{ sessionChangedMessage }}</UiAlertDescription>
    </UiAlert>
    <UiAlert v-if="unconfirmedWrite && !open && !deletion" role="alert">
      <UiAlertTitle>结果未确认</UiAlertTitle>
      <UiAlertDescription>{{ unknownWriteMessage }}{{ failed ? '。列表读取失败，请重试。' : '' }}</UiAlertDescription>
      <UiButton type="button" variant="outline" size="sm" class="mt-2"
        :disabled="pending || sessionChanged" @click="refreshRoles">刷新列表核对</UiButton>
    </UiAlert>
    <UiAlert v-if="notice || createdRole" role="status">
      <UiAlertTitle>{{ notice || '角色已创建' }}</UiAlertTitle>
      <UiButton v-if="createdRole" variant="secondary" size="sm" class="mt-2"
        :aria-disabled="locked || treeUnavailable" @click="show($event, 'grant', createdRole)">
        立即配置菜单授权
      </UiButton>
    </UiAlert>
    <UiAlert v-if="actorFailed" variant="destructive" role="alert"
      ><UiAlertTitle>{{ actorRestricted ? '当前账号无权读取自己的角色' : '无法确认当前角色' }}</UiAlertTitle
      ><UiAlertDescription>{{ actorRestricted
        ? '写入操作暂不可用，请联系管理员确认权限。'
        : '写入操作暂不可用，请重新读取当前账号的角色信息。' }}</UiAlertDescription
      ><UiButton
        variant="outline"
        size="sm"
        class="mt-2"
        :aria-disabled="actorPending || sessionChanged"
        @click="refreshActor"
        >重新读取当前角色</UiButton
      ></UiAlert
    >
    <UiAlert v-if="treeFailed" variant="destructive" role="alert"
      ><UiAlertTitle>{{ treeRestricted ? '当前账号无权读取菜单列表' : '菜单列表读取失败' }}</UiAlertTitle
      ><UiAlertDescription>{{ treeRestricted
        ? '角色列表仍可查看，请联系管理员开通菜单读取权限。'
        : '角色列表仍可查看，菜单授权暂不可用。' }}</UiAlertDescription
      ><UiButton variant="outline" size="sm" class="mt-2" :aria-disabled="treePending || sessionChanged" @click="refreshTree"
        >重新读取菜单</UiButton
      ></UiAlert
    >

    <form
      class="flex min-w-0 flex-wrap items-end gap-x-6 gap-y-4 rounded-lg border border-border bg-card p-4"
      aria-label="角色筛选"
      @submit.prevent="search"
    >
      <UiField class="w-full sm:w-64"
        ><UiFieldLabel for="roles-search">角色名称</UiFieldLabel
        ><UiInput
          id="roles-search"
          v-model="filters.name"
          placeholder="按角色名称搜索"
          :disabled="pending"
          maxlength="255"
      /></UiField>
      <UiField class="w-full sm:w-auto"
        ><UiFieldLabel as="span">状态</UiFieldLabel
        ><UiRadioGroup
          v-model="filters.status"
          aria-label="筛选角色状态"
          class="flex min-h-8 flex-wrap gap-4"
          :disabled="pending"
          ><label
            v-for="option in [
              { value: 'all', label: '全部' },
              { value: '1', label: '启用' },
              { value: '0', label: '停用' },
            ]"
            :key="option.value"
            class="flex cursor-pointer items-center gap-2 text-sm"
            ><UiRadioGroupItem :value="option.value" />{{ option.label }}</label
          ></UiRadioGroup
        ></UiField
      >
      <div class="flex gap-2">
        <UiButton type="submit" variant="secondary" :aria-disabled="pending || sessionChanged"
          ><HugeiconsIcon
            :icon="Search01Icon"
            class="size-4"
            data-icon="inline-start"
            aria-hidden="true"
          />查询</UiButton
        ><UiButton type="button" variant="outline" :disabled="pending" @click="reset"
          >重置</UiButton
        >
      </div>
    </form>

    <div class="w-full min-w-0 max-w-3xl rounded-lg border border-border bg-card" :aria-busy="pending">
      <div class="flex items-center justify-between gap-3 border-b-[0.5px] border-border/70 p-4">
        <h2 ref="listTitle" tabindex="-1" class="text-sm font-medium">
          角色列表
          <span class="ml-2 font-normal text-muted-foreground">
            <template v-if="failed">—</template>
            <template v-else-if="pending && !roles.length">读取中</template>
            <template v-else>共 <span class="font-mono tabular-nums">{{ filtered.length }}</span> 个角色</template>
          </span>
        </h2>
        <UiButton variant="ghost" size="sm" :aria-disabled="pending || sessionChanged" @click="refreshRoles"
          ><HugeiconsIcon
            :icon="RefreshIcon"
            class="size-4"
            :class="{ 'motion-safe:animate-spin': pending }"
            aria-hidden="true"
          />{{ pending ? '读取中…' : '刷新' }}</UiButton
        >
      </div>
      <div
        v-if="failed"
        class="flex min-h-72 flex-col items-center justify-center gap-3 px-6 py-12 text-center"
        role="alert"
      >
        <h3 class="font-medium">
          {{ restricted ? '当前账号无权读取角色列表' : '角色列表读取失败' }}
        </h3>
        <p class="text-sm text-muted-foreground">
          {{
            restricted
              ? '请联系管理员确认访问权限。'
              : '暂时无法取得角色数据，请检查服务连接后重试。'
          }}
        </p>
        <UiButton variant="outline" :aria-disabled="sessionChanged" @click="refreshRoles">重试</UiButton>
      </div>
      <div
        v-else-if="pending && !roles.length"
        class="divide-y divide-border px-4"
        role="status"
      >
        <span class="sr-only">正在读取角色列表…</span>
        <div v-for="row in 5" :key="row" aria-hidden="true" class="grid grid-cols-3 items-center gap-4 py-5">
          <UiSkeleton class="h-4 w-3/4" />
          <UiSkeleton class="h-4 w-1/2" />
          <UiSkeleton class="ml-auto h-4 w-3/4 max-w-28" />
        </div>
      </div>
      <div
        v-else-if="!filtered.length"
        class="flex min-h-72 flex-col items-center justify-center gap-3 px-6 py-12 text-center"
        role="status"
      >
        <h3 class="font-medium">
          {{ applied.name || applied.status !== 'all' ? '没有匹配的角色' : '暂无角色' }}
        </h3>
        <p class="text-sm text-muted-foreground">
          {{
            applied.name || applied.status !== 'all'
              ? '请调整名称或状态，或清除当前筛选。'
              : '创建角色后，可为其配置菜单授权。'
          }}
        </p>
        <UiButton v-if="applied.name || applied.status !== 'all'" variant="outline" @click="reset"
          >清除筛选</UiButton
        ><UiButton v-else variant="outline" :disabled="locked" @click="show($event, 'create')">新增角色</UiButton>
      </div>
      <UiTable v-else>
        <UiTableCaption class="sr-only">角色名称、状态、权限数量及操作</UiTableCaption>
        <UiTableHeader
          ><UiTableRow class="border-b-[0.5px]"
            ><UiTableHead class="px-4">角色</UiTableHead
            ><UiTableHead class="hidden text-center sm:table-cell">状态</UiTableHead
            ><UiTableHead class="hidden text-right md:table-cell">权限数</UiTableHead
            ><UiTableHead class="hidden lg:table-cell">备注</UiTableHead
            ><UiTableHead
              class="sticky right-0 z-10 w-44 min-w-44 bg-card text-center shadow-[-4px_0_6px_-6px_var(--border)]"
              >操作</UiTableHead
            ></UiTableRow
          ></UiTableHeader
        >
        <UiTableBody
          ><UiTableRow v-for="role in visible" :key="role.id" class="border-b-[0.5px] hover:bg-muted">
            <UiTableCell class="min-w-36 whitespace-normal px-4"
              ><UiButton
                variant="ghost"
                class="h-auto max-w-56 justify-start whitespace-normal break-all p-0 text-left font-medium hover:bg-transparent hover:text-accent-data-foreground hover:underline underline-offset-4"
                :aria-label="`查看${role.roleName}`"
                @click="show($event, 'view', role)"
                >{{ role.roleName }}</UiButton
              >
              <div class="mt-1 flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                <span class="font-mono tabular-nums">ID {{ role.id }}</span>
                <StatusBadge v-if="isDefaultRole(role)">系统默认</StatusBadge>
                <StatusBadge v-if="role.id === actorId">当前账号</StatusBadge>
                <StatusBadge v-if="awaitingRead.has(role.id)" tone="warning">授权未确认</StatusBadge>
                <StatusBadge class="sm:hidden"
                  :tone="role.status === 1 ? 'success' : role.status === 0 ? 'warning' : 'neutral'">
                  {{ role.status === 1 ? '启用' : role.status === 0 ? '停用' : '未知' }}
                </StatusBadge>
                <span class="md:hidden"><span class="font-mono tabular-nums">{{ role.permissions.length }}</span> 项权限</span>
              </div></UiTableCell
            >
            <UiTableCell class="hidden text-center sm:table-cell"
              ><StatusBadge :tone="role.status === 1 ? 'success' : role.status === 0 ? 'warning' : 'neutral'"
                >{{ role.status === 1 ? '启用' : role.status === 0 ? '停用' : '未知' }}</StatusBadge
              ></UiTableCell
            >
            <UiTableCell class="hidden text-right font-mono tabular-nums md:table-cell">{{
              role.permissions.length
            }}</UiTableCell
            ><UiTableCell
              class="hidden max-w-56 whitespace-normal break-words text-muted-foreground lg:table-cell"
              >{{ role.remark || '—' }}</UiTableCell
            >
            <UiTableCell
              class="sticky right-0 z-10 w-44 min-w-44 bg-card group-hover/row:bg-muted text-center shadow-[-4px_0_6px_-6px_var(--border)]"
              ><div class="flex justify-center gap-1">
                <UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`菜单授权 ${role.roleName}`"
                  :aria-disabled="locked || (treeUnavailable && !awaitingRead.has(role.id))"
                  @click="show($event, 'grant', role)"
                  >菜单授权</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`编辑${role.roleName}`"
                  :aria-disabled="locked || awaitingRead.has(role.id)"
                  @click="show($event, 'edit', role)"
                  >编辑</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  class="text-destructive hover:text-destructive"
                  :aria-label="`删除${role.roleName}`"
                  :aria-disabled="locked || awaitingRead.has(role.id) || role.id === actorId || isDefaultRole(role)"
                  @click="confirmDelete($event, role)"
                  >删除</UiButton
                >
              </div></UiTableCell
            >
          </UiTableRow></UiTableBody
        >
      </UiTable>
      <div
        v-if="!failed"
        class="flex flex-wrap items-center justify-between gap-4 border-t-[0.5px] border-border/70 p-4"
      >
        <UiRadioGroup
          :model-value="String(size)"
          aria-label="每页条数"
          class="flex w-auto gap-3"
          :disabled="pending"
          @update:model-value="changeSize"
          ><label
            v-for="count in [10, 20, 50]"
            :key="count"
            class="flex cursor-pointer items-center gap-1.5 text-xs text-muted-foreground"
            ><UiRadioGroupItem :value="String(count)" />{{ count }} 条/页</label
          ></UiRadioGroup
        >
        <div class="flex items-center gap-3">
          <span class="whitespace-nowrap font-mono text-xs tabular-nums text-muted-foreground"
            >第 {{ page }} / {{ Math.max(1, Math.ceil(filtered.length / size)) }} 页</span
          ><UiPagination
            v-model:page="page"
            :total="filtered.length"
            :items-per-page="size"
            :disabled="pending"
            :sibling-count="1"
            aria-label="角色列表分页"
            class="w-auto"
            ><UiPaginationContent v-slot="{ items }"
              ><UiPaginationPrevious aria-label="上一页" /><template
                v-for="(item, index) in items"
                :key="index"
                ><UiPaginationItem v-if="item.type === 'page'" :value="item.value" as-child
                  ><UiButton
                    :variant="item.value === page ? 'outline' : 'ghost'"
                    size="icon-sm"
                    :aria-label="`第 ${item.value} 页`"
                    :aria-current="item.value === page ? 'page' : undefined"
                    >{{ item.value }}</UiButton
                  ></UiPaginationItem
                ><UiPaginationEllipsis v-else /></template
              ><UiPaginationNext aria-label="下一页" /></UiPaginationContent
          ></UiPagination>
        </div>
      </div>
    </div>
    <RolePanel
      v-model:open="open"
      :mode="mode"
      :role="selected"
      :tree="tree"
      :tree-unavailable="treeUnavailable"
      :tree-pending="treePending"
      :tree-restricted="treeRestricted"
      :actor-role-id="actorId"
      :locked="locked"
      :awaiting-read="Boolean(selected && awaitingRead.has(selected.id))"
      :write-unconfirmed="unconfirmedWrite !== null"
      :list-pending="pending"
      :list-failed="failed"
      @saved="saved"
      @roles-read="readRoles"
      @retry-tree="refreshTree"
      @close-auto-focus="restoreSheetFocus"
      @unconfirmed="lockGrant"
      @write-unconfirmed="lockWrite"
      @refresh-list="refreshRoles"
    />
    <UiDialog
      :open="Boolean(deletion)"
      @update:open="
        (value) => {
          if (!value && !deleting) deletion = null
        }
      "
      ><UiDialogContent
        :show-close-button="false"
        class="sm:max-w-[400px]"
        @pointer-down-outside.prevent
        @interact-outside.prevent
        @close-auto-focus="restoreDeleteFocus"
        @escape-key-down="
          (event) => {
            if (deleting) event.preventDefault()
          }
        "
        ><UiDialogHeader
          ><UiDialogTitle class="break-words">删除角色「{{ deletion?.roleName }}」？</UiDialogTitle
          ><UiDialogDescription
            >将永久删除此角色及其授权关系，无法在此恢复。请先确保相关用户已重新分配角色。</UiDialogDescription
          ></UiDialogHeader
        >
        <p class="text-xs text-muted-foreground">角色 ID <span class="font-mono tabular-nums">{{ deletion?.id }}</span></p>
        <UiAlert v-if="sessionChanged || roleInUse || deleteFailure" variant="destructive" role="alert">
          <UiAlertDescription>{{ sessionChanged ? sessionChangedMessage : roleInUse
            ? '该角色仍有用户使用。请先重新分配关联用户的角色，再删除此角色。' : deleteFailure }}</UiAlertDescription>
          <UiButton v-if="roleInUse && !sessionChanged" as-child variant="outline" size="sm" class="mt-2 justify-self-start">
            <RouterLink :to="{ path: '/console/users', query: { roleId: deletion?.id } }">查看关联用户</RouterLink>
          </UiButton>
        </UiAlert>
        <UiAlert v-if="unconfirmedWrite" role="alert">
          <UiAlertTitle>结果未确认</UiAlertTitle>
          <UiAlertDescription>{{ unknownWriteMessage }}{{ failed ? '。列表读取失败，请重试。' : '' }}</UiAlertDescription>
          <UiButton type="button" variant="outline" size="sm" class="mt-2"
            :disabled="pending || deleting || sessionChanged" @click="refreshRoles">刷新列表核对</UiButton>
        </UiAlert>
        <UiDialogFooter
          ><UiButton variant="outline" :disabled="deleting" @click="deletion = null">取消</UiButton
          ><UiButton variant="destructive" :aria-disabled="deleting || sessionChanged || Boolean(unconfirmedWrite)" @click="remove">{{
            deleting ? '正在删除…' : '删除角色'
          }}</UiButton></UiDialogFooter
        ></UiDialogContent
      ></UiDialog
    >
  </section>
</template>
