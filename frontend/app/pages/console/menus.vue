<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { Add01Icon, ArrowRight01Icon, RefreshIcon, Search01Icon } from '@hugeicons/core-free-icons'
import { getUserRoutes, deleteRouter } from '@/api/router/router'
import { getRoleList } from '@/api/auth/auth'
import { checkReadAccess, listStatus, mutationError, mutationResultUnknown, permissionDenied, restoreActionFocus, sessionChangedMessage } from '@/lib/rbac-mutations'
import { managedRoles } from '@/lib/roles'
import {
  managedMenus,
  menuRows,
  visibleMenus,
  menuDeleteReasons,
  menuWriteSucceeded,
} from '@/lib/menus'
import { withSessionOwner } from '@/lib/session'
import type { MenuRecord } from '@/lib/menus'

definePageMeta({ layout: 'console', middleware: 'auth' })
const types = { C: '目录', M: '菜单', F: '操作' } as const
const menuType = (type: string) =>
  type === 'C' || type === 'M' || type === 'F' ? types[type] : undefined
const session = useSession()
const owner = session.capture()
const sessionChanged = computed(() => !session.owns(owner))
const route = useRoute()
const router = useRouter()
const tree = shallowRef<MenuRecord[]>([])
const pending = ref(true)
const failed = ref(false)
const restricted = ref(false)
const filters = reactive({
  query: typeof route.query.q === 'string' ? route.query.q.trim().slice(0, 255) : '',
  status: listStatus(route.query.status),
})
const applied = reactive({ ...filters })
watch(applied, () => {
  if (sessionChanged.value) return
  void router.replace({ query: {
    ...route.query,
    q: applied.query || undefined,
    status: applied.status === 'all' ? undefined : applied.status,
  } })
})
const collapsed = ref<number[]>([])
const rows = computed(() =>
  visibleMenus(tree.value, applied.query, applied.status, collapsed.value),
)
const filtering = computed(() => Boolean(applied.query || applied.status !== 'all'))
const total = computed(() => menuRows(tree.value).length)
const matches = computed(() => rows.value.filter((row) => row.match).length)
const notice = ref('')
const navigationNotice = '重新登录后将更新左侧导航栏。'
const awaitingRead = ref<'save' | 'delete' | null>(null)
const writeLocked = computed(() => pending.value || failed.value || awaitingRead.value !== null || sessionChanged.value)
const open = ref(false)
const mode = ref<'view' | 'create' | 'edit'>('view')
const selected = shallowRef<MenuRecord | null>(null)
const parent = shallowRef<MenuRecord | null>(null)
const deletion = shallowRef<MenuRecord | null>(null)
const deleting = ref(false)
const checking = ref(false)
const deleteReasons = ref<string[]>([])
const deleteFailure = ref('')
const preflightFailed = ref(false)
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
onBeforeRouteLeave(() => session.state.value.invalidated || !(deleting.value || checking.value))
function beforeUnload(event: BeforeUnloadEvent) {
  if (session.state.value.invalidated || !(deleting.value || checking.value)) return
  event.preventDefault()
  event.returnValue = ''
}
function refresh() {
  if (!pending.value && !deleting.value && !checking.value) void load()
}
async function load() {
  if (!owner || !session.owns(owner)) return
  pending.value = true
  failed.value = false
  restricted.value = false
  try {
    const result = managedMenus(checkReadAccess(await withSessionOwner(owner, getUserRoutes)))
    if (!session.owns(owner)) return
    tree.value = result
    if (awaitingRead.value) read(tree.value)
  } catch (error) {
    if (!session.owns(owner)) return
    tree.value = []
    failed.value = true
    restricted.value = permissionDenied(error)
  } finally {
    pending.value = false
  }
}
function search() {
  if (pending.value || sessionChanged.value) return
  Object.assign(applied, { query: filters.query.trim(), status: filters.status })
  notice.value = ''
}
function reset() {
  if (pending.value || sessionChanged.value) return
  Object.assign(filters, { query: '', status: 'all' })
  search()
}
function toggle(id: number) {
  collapsed.value = collapsed.value.includes(id)
    ? collapsed.value.filter((value) => value !== id)
    : [...collapsed.value, id]
}
function childBlocker(node: MenuRecord) {
  if (node.meta.type === 'F') return '操作节点不能添加子项。'
  if (node.pid !== null && node.pid >= node.id) return '上级 ID 大于或等于此节点 ID，暂不能添加子项。'
}
const missingPermission = '缺权限标识，暂不可编辑/删除'
function deleteBlocker(node: MenuRecord) {
  if (!node.permission?.trim()) return missingPermission
  if (node.children.length) return '请先处理此菜单的子项。'
}
function show(
  event: Event,
  next: typeof mode.value,
  node: MenuRecord | null = null,
  ancestor: MenuRecord | null = null,
) {
  if (next !== 'view' && (writeLocked.value || !session.owns(owner))) return
  if (next === 'edit' && !node?.permission?.trim()) return
  if (ancestor && childBlocker(ancestor)) return
  sheetTrigger = event.currentTarget as HTMLElement
  mode.value = next
  selected.value = node
  parent.value = ancestor
  open.value = true
}
function saved(value: MenuRecord[]) {
  tree.value = value
  open.value = false
  notice.value = `菜单已保存并回读确认。${navigationNotice}`
}
function read(value: MenuRecord[]) {
  tree.value = value
  awaitingRead.value = null
  notice.value = `已重新读取菜单，请核对最终配置。${navigationNotice}`
}
function lockSave() {
  awaitingRead.value = 'save'
  notice.value = ''
}
async function preflight(id: number) {
  if (!owner || !session.owns(owner)) return
  checking.value = true
  deleteReasons.value = []
  try {
    const [menus, roles] = await withSessionOwner(owner, () => Promise.all([getUserRoutes(), getRoleList()]))
    if (!session.owns(owner) || deletion.value?.id !== id) return
    const fresh = managedMenus(checkReadAccess(menus))
    preflightFailed.value = false
    tree.value = fresh
    const current = menuRows(fresh).find((row) => row.node.id === id)?.node
    if (!current) {
      deleteReasons.value = ['该菜单已不存在，请关闭确认窗口。']
      return
    }
    deletion.value = current
    deleteReasons.value = menuDeleteReasons(
      current,
      fresh,
      managedRoles(checkReadAccess(roles)).flatMap((role) => role.permissions),
    )
    if (!deleteReasons.value.length) return current
  } catch (error) {
    if (!session.owns(owner)) return
    preflightFailed.value = true
    deleteReasons.value = [permissionDenied(error)
      ? '当前账号无权读取菜单或角色引用，请联系管理员确认权限。'
      : '无法核对菜单及角色引用，请重新检查。']
  } finally {
    checking.value = false
  }
}
async function prepareDelete(event: Event, node: MenuRecord) {
  if (writeLocked.value || !session.owns(owner) || checking.value || deleting.value || deleteBlocker(node)) return
  deleteTrigger = event.currentTarget as HTMLElement
  preflightFailed.value = false
  deletion.value = node
  deleteFailure.value = ''
  await preflight(node.id)
}
async function recheck(event: Event) {
  if (!deletion.value || checking.value || deleting.value || sessionChanged.value) return
  const trigger = event.currentTarget
  deleteFailure.value = ''
  await preflight(deletion.value.id)
  if (!preflightFailed.value && document.activeElement === trigger) {
    document.getElementById('menu-delete-confirm')?.focus()
  }
}
async function remove() {
  if (
    !owner || !session.owns(owner) || !deletion.value || deleting.value ||
    checking.value || writeLocked.value || deleteReasons.value.length
  ) return
  const id = deletion.value.id
  deleteFailure.value = ''
  deleting.value = true
  let accepted = false
  try {
    const target = await preflight(id)
    if (!target || !session.owns(owner) || deletion.value?.id !== id) return
    // Send no descendants: the backend deletes supplied children and their permission records.
    menuWriteSucceeded(
      await withSessionOwner(owner, () => deleteRouter({
        id: target.id,
        name: target.name,
        permission: target.permission,
        children: [],
      })),
    )
    accepted = true
    if (!session.owns(owner)) return
    const result = managedMenus(await withSessionOwner(owner, getUserRoutes))
    if (!session.owns(owner)) return
    if (menuRows(result).some((row) => row.node.id === target.id))
      throw new Error('Delete not confirmed')
    tree.value = result
    deleteTrigger = null
    deletion.value = null
    notice.value = `菜单已删除并回读确认。${navigationNotice}`
  } catch (error) {
    if (!session.owns(owner)) return
    if (accepted || mutationResultUnknown(error)) {
      deleteTrigger = null
      deletion.value = null
      awaitingRead.value = 'delete'
      notice.value = ''
    } else deleteFailure.value = mutationError(error, '删除', '菜单依赖可能已变化，请重新核对后重试。')
  } finally {
    deleting.value = false
  }
}
onMounted(() => {
  window.addEventListener('beforeunload', beforeUnload)
  void load()
})
onUnmounted(() => window.removeEventListener('beforeunload', beforeUnload))
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="menus-title">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="space-y-2">
        <h1 id="menus-title" class="text-2xl font-semibold tracking-tight">菜单管理</h1>
        <p class="text-sm text-muted-foreground">管理导航层级、路由配置与操作权限。</p>
      </div>
      <UiButton :disabled="writeLocked" @click="show($event, 'create')"
        ><HugeiconsIcon
          :icon="Add01Icon"
          class="size-4 -ml-0.5"
          aria-hidden="true"
        />新增菜单</UiButton
      >
    </header>
    <UiAlert v-if="sessionChanged" variant="destructive" role="alert">
      <UiAlertDescription>{{ sessionChangedMessage }}</UiAlertDescription>
    </UiAlert>
    <UiAlert v-if="notice" role="status"
      ><UiAlertTitle>{{ notice }}</UiAlertTitle></UiAlert
    >
    <UiAlert v-if="awaitingRead" class="border-status-warning/40 bg-status-warning-surface text-status-warning-foreground">
      <UiAlertTitle>{{ awaitingRead === 'delete' ? '菜单删除已提交，未确认' : '菜单配置已提交，未确认' }}</UiAlertTitle>
      <UiAlertDescription class="text-inherit">请重新读取菜单并核对最终配置。</UiAlertDescription>
      <UiButton variant="outline" size="sm" class="mt-2 text-foreground" :aria-disabled="pending || sessionChanged" @click="refresh">
        重新读取菜单
      </UiButton>
    </UiAlert>
    <form
      class="flex min-w-0 flex-wrap items-end gap-x-6 gap-y-4 rounded-lg border border-border bg-card p-4"
      aria-label="菜单筛选"
      @submit.prevent="search"
    >
      <UiField class="w-full sm:w-72"
        ><UiFieldLabel for="menus-search">名称、路径或权限</UiFieldLabel
        ><UiInput
          id="menus-search"
          v-model="filters.query"
          maxlength="255"
          placeholder="搜索菜单"
          :disabled="pending"
      /></UiField>
      <UiField class="w-full sm:w-auto"
        ><UiFieldLabel as="span">状态</UiFieldLabel
        ><UiRadioGroup
          v-model="filters.status"
          aria-label="筛选菜单状态"
          class="flex min-h-8 gap-4"
          :disabled="pending"
          ><label
            v-for="option in [
              { value: 'all', label: '全部' },
              { value: '1', label: '启用' },
              { value: '0', label: '停用' },
            ]"
            :key="option.value"
            class="flex items-center gap-2 text-sm"
            ><UiRadioGroupItem :value="option.value" />{{ option.label }}</label
          ></UiRadioGroup
        ></UiField
      >
      <div class="flex gap-2">
        <UiButton type="submit" variant="secondary" :aria-disabled="pending || sessionChanged"
          ><HugeiconsIcon
            :icon="Search01Icon"
            class="size-4 -ml-0.5"
            aria-hidden="true"
          />查询</UiButton
        ><UiButton type="button" variant="outline" :disabled="pending" @click="reset"
          >重置</UiButton
        >
      </div>
    </form>
    <div class="min-w-0 rounded-lg border border-border bg-card" :aria-busy="pending">
      <div
        class="flex flex-wrap items-center justify-between gap-3 border-b-[0.5px] border-border/70 p-4"
      >
        <h2 ref="listTitle" tabindex="-1" class="text-sm font-medium">
          菜单树
          <span class="ml-2 font-normal text-muted-foreground">
            <template v-if="failed">—</template>
            <template v-else-if="pending">读取中</template>
            <template v-else>
              <template v-if="filtering">命中 <span class="font-mono tabular-nums">{{ matches }}</span> / </template>
              共 <span class="font-mono tabular-nums">{{ total }}</span> 项
            </template>
          </span>
        </h2>
        <div class="flex gap-1">
          <UiButton
            variant="ghost"
            size="sm"
            :aria-controls="rows.length ? 'menu-tree-rows' : undefined"
            :disabled="pending || failed || filtering || !rows.length"
            @click="collapsed = []"
            >展开全部</UiButton
          ><UiButton
            variant="ghost"
            size="sm"
            :aria-controls="rows.length ? 'menu-tree-rows' : undefined"
            :disabled="pending || failed || filtering || !rows.length"
            @click="
              collapsed = menuRows(tree)
                .filter((row) => row.node.children.length)
                .map((row) => row.node.id)
            "
            >收起全部</UiButton
          ><UiButton variant="ghost" size="sm" :aria-disabled="pending || sessionChanged" @click="refresh"
            ><HugeiconsIcon
              :icon="RefreshIcon"
              class="size-4"
              :class="{ 'motion-safe:animate-spin': pending }"
              aria-hidden="true"
            />刷新</UiButton
          >
        </div>
      </div>
      <div
        v-if="failed"
        role="alert"
        class="flex min-h-72 flex-col items-center justify-center gap-3 p-6 text-center"
      >
        <h3 class="font-medium">{{ restricted ? '当前账号无权读取菜单' : '菜单读取失败' }}</h3>
        <p class="text-sm text-muted-foreground">
          {{
            restricted
              ? '请联系管理员确认访问权限。'
              : '暂时无法取得菜单数据，请检查服务连接后重试。'
          }}
        </p>
        <UiButton variant="outline" :aria-disabled="sessionChanged" @click="refresh">重试</UiButton>
      </div>
      <div
        v-else-if="pending && !tree.length"
        class="divide-y divide-border px-4"
        role="status"
      >
        <span class="sr-only">正在读取菜单…</span>
        <div v-for="row in 5" :key="row" aria-hidden="true" class="grid grid-cols-3 items-center gap-4 py-5">
          <UiSkeleton class="h-4 w-3/4" />
          <UiSkeleton class="h-4 w-1/2" />
          <UiSkeleton class="ml-auto h-4 w-3/4 max-w-28" />
        </div>
      </div>
      <div
        v-else-if="!rows.length"
        role="status"
        class="flex min-h-72 flex-col items-center justify-center gap-3 p-6 text-center"
      >
        <h3 class="font-medium">{{ filtering ? '没有匹配的菜单' : '暂无菜单' }}</h3>
        <p class="text-sm text-muted-foreground">
          {{
            filtering ? '请调整名称、路径、权限或状态筛选。' : '创建顶级菜单后，可继续添加子项。'
          }}
        </p>
        <UiButton v-if="filtering" variant="outline" @click="reset">清除筛选</UiButton
        ><UiButton v-else variant="outline" :disabled="writeLocked" @click="show($event, 'create')">新增菜单</UiButton>
      </div>
      <UiTable v-else
        ><UiTableCaption class="sr-only">菜单层级、类型、状态、路径、权限和操作</UiTableCaption
        ><UiTableHeader
          ><UiTableRow class="border-b-[0.5px]"
            ><UiTableHead class="px-4">菜单</UiTableHead
            ><UiTableHead class="hidden sm:table-cell">类型</UiTableHead
            ><UiTableHead class="hidden text-center md:table-cell">状态</UiTableHead
            ><UiTableHead class="hidden lg:table-cell">路由 / 权限</UiTableHead
            ><UiTableHead class="hidden text-right xl:table-cell">排序</UiTableHead
            ><UiTableHead
              class="sticky right-0 z-10 w-28 min-w-28 bg-card text-center shadow-[-4px_0_6px_-6px_var(--border)] sm:w-44 sm:min-w-44"
              >操作</UiTableHead
            ></UiTableRow
          ></UiTableHeader
        >
        <UiTableBody id="menu-tree-rows"
          ><UiTableRow
            v-for="row in rows"
            :key="row.node.id"
            class="border-b-[0.5px] bg-(--menu-row-color)! hover:bg-muted!"
            :style="{ '--menu-row-color': filtering && !row.match
              ? 'color-mix(in oklab, var(--muted) 25%, var(--card))'
              : 'var(--card)' }"
          >
            <UiTableCell class="min-w-40 max-w-80 whitespace-normal px-2 sm:min-w-48 sm:px-4"
              ><div
                class="flex items-start gap-1 pl-[calc(var(--depth)*0.5rem)] sm:pl-[calc(var(--depth)*1rem)]"
                :style="{ '--depth': Math.min(row.depth, 5) }"
              >
                <UiButton
                  v-if="row.node.children.length"
                  variant="ghost"
                  size="icon-sm"
                  class="shrink-0"
                  :disabled="filtering"
                  :aria-label="`展开或收起${row.node.meta.metaTitle}`"
                  :aria-expanded="filtering || !collapsed.includes(row.node.id)"
                  @click="toggle(row.node.id)"
                  ><HugeiconsIcon
                    :icon="ArrowRight01Icon"
                    class="size-4"
                    :class="{ 'rotate-90': filtering || !collapsed.includes(row.node.id) }"
                    aria-hidden="true" /></UiButton
                ><span v-else class="w-8 shrink-0" aria-hidden="true" />
                <div class="min-w-0">
                  <UiButton
                    variant="ghost"
                    class="h-auto min-h-8 justify-start whitespace-normal break-all p-0 text-left font-medium hover:bg-transparent hover:text-accent-data-foreground hover:underline underline-offset-4"
                    :aria-label="`查看${row.node.meta.metaTitle}`"
                    @click="show($event, 'view', row.node)"
                    >{{ row.node.meta.metaTitle }}</UiButton
                  >
                  <div class="mt-1 flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                    <span class="font-mono tabular-nums">ID {{ row.node.id }}</span>
                    <StatusBadge v-if="filtering && !row.match">祖先上下文</StatusBadge>
                    <StatusBadge v-if="row.node.meta.hidden" tone="warning">整枝隐藏</StatusBadge>
                    <StatusBadge v-if="row.node.meta.metaHidden">仅藏导航</StatusBadge>
                    <UiBadge class="sm:hidden" variant="outline">{{ menuType(row.node.meta.type) ?? '未知类型' }}</UiBadge>
                    <StatusBadge class="md:hidden"
                      :tone="row.node.status === 1 ? 'success' : row.node.status === 0 ? 'warning' : 'neutral'">
                      {{ row.node.status === 1 ? '启用' : row.node.status === 0 ? '停用' : '未知' }}
                    </StatusBadge>
                  </div>
                  <p v-if="!row.node.permission?.trim()" class="mt-1 text-xs text-muted-foreground">{{ missingPermission }}</p>
                </div>
              </div></UiTableCell
            >
            <UiTableCell class="hidden sm:table-cell"
              ><UiBadge variant="outline">{{ menuType(row.node.meta.type) ?? '未知类型' }}</UiBadge></UiTableCell
            >
            <UiTableCell class="hidden text-center md:table-cell"
              ><StatusBadge
                :tone="row.node.status === 1 ? 'success' : row.node.status === 0 ? 'warning' : 'neutral'"
                >{{
                  row.node.status === 1 ? '启用' : row.node.status === 0 ? '停用' : '未知'
                }}</StatusBadge
              ></UiTableCell
            >
            <UiTableCell class="hidden max-w-56 whitespace-normal break-all font-mono text-xs lg:table-cell"
              ><p>{{ row.node.path }}</p>
              <p class="mt-1 text-muted-foreground">
                {{ row.node.permission || '无权限标识' }}
              </p></UiTableCell
            ><UiTableCell class="hidden text-right font-mono tabular-nums xl:table-cell">{{
              row.node.menuOrder
            }}</UiTableCell>
            <UiTableCell
              class="sticky right-0 z-10 w-28 min-w-28 bg-(--menu-row-color) group-hover/row:bg-muted px-1 text-center shadow-[-4px_0_6px_-6px_var(--border)] sm:w-44 sm:min-w-44"
              ><div class="grid grid-cols-2 justify-center gap-1 sm:flex">
                <UiButton
                  variant="ghost"
                  size="sm"
                  class="col-span-2 px-1"
                  :aria-label="`添加子项 ${row.node.meta.metaTitle}`"
                  :aria-disabled="writeLocked || Boolean(childBlocker(row.node))"
                  :title="childBlocker(row.node)"
                  :aria-description="childBlocker(row.node)"
                  @click="show($event, 'create', null, row.node)"
                  >添加子项</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`编辑${row.node.meta.metaTitle}`"
                  :aria-disabled="writeLocked || !row.node.permission?.trim()"
                  :title="!row.node.permission?.trim() ? missingPermission : undefined"
                  @click="show($event, 'edit', row.node)"
                  >编辑</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  class="text-destructive hover:text-destructive"
                  :aria-label="`删除${row.node.meta.metaTitle}`"
                  :aria-disabled="writeLocked || Boolean(deleteBlocker(row.node))"
                  :title="deleteBlocker(row.node)"
                  :aria-description="deleteBlocker(row.node)"
                  @click="prepareDelete($event, row.node)"
                  >删除</UiButton
                >
              </div></UiTableCell
            >
          </UiTableRow></UiTableBody
        >
      </UiTable>
      <p
        v-if="!failed && tree.length"
        class="border-t-[0.5px] border-border/70 px-4 py-3 text-xs text-muted-foreground"
      >
        {{
          filtering
            ? '筛选结果包含祖先节点以保留层级；重置后可手动折叠。'
            : '按层级与排序值展示；目录需先处理子项，再删除。'
        }}
      </p>
    </div>
    <MenusMenuPanel
      v-model:open="open"
      :mode="mode"
      :node="selected"
      :parent="parent"
      :tree="tree"
      :awaiting-read="awaitingRead !== null"
      @saved="saved"
      @read="read"
      @unconfirmed="lockSave"
      @close-auto-focus="restoreSheetFocus"
    />
    <UiDialog
      :open="Boolean(deletion)"
      @update:open="
        (value) => {
          if (!value && !deleting && !checking) deletion = null
        }
      "
      ><UiDialogContent
        :show-close-button="false"
        class="top-[38.2%]! sm:max-w-[400px]!"
        @pointer-down-outside.prevent
        @interact-outside.prevent
        @close-auto-focus="restoreDeleteFocus"
        @escape-key-down="
          (event) => {
            if (deleting || checking) event.preventDefault()
          }
        "
        ><UiDialogHeader
          ><UiDialogTitle class="break-words"
            >删除菜单「{{ deletion?.meta.metaTitle }}」？</UiDialogTitle
          ><UiDialogDescription
            >将永久删除此菜单及其独立权限记录，无法在此恢复。子菜单、共享权限和角色引用会阻止删除。</UiDialogDescription
          ></UiDialogHeader
        >
        <p class="text-xs text-muted-foreground">菜单 ID <span class="font-mono tabular-nums">{{ deletion?.id }}</span></p>
        <p v-if="checking" role="status" class="text-sm text-muted-foreground">
          依赖预检中，正在核对菜单和角色引用…
        </p>
        <UiAlert v-else-if="deleteReasons.length" :variant="preflightFailed ? 'destructive' : 'default'"
          :role="preflightFailed ? 'alert' : 'status'"
          :class="!preflightFailed && 'border-status-warning/40 bg-status-warning-surface text-status-warning-foreground'">
          <UiAlertTitle>暂不能删除</UiAlertTitle>
          <ul class="list-disc space-y-1 pl-5">
            <li v-for="reason in deleteReasons" :key="reason">{{ reason }}</li>
          </ul>
        </UiAlert>
        <p v-else-if="!deleting" role="status" class="text-sm text-muted-foreground">
          依赖预检通过，确认删除时将再次核对。
        </p>
        <UiAlert v-if="sessionChanged || deleteFailure" variant="destructive" role="alert">
          <UiAlertDescription>{{ sessionChanged ? sessionChangedMessage : deleteFailure }}</UiAlertDescription>
        </UiAlert>
        <UiDialogFooter
          ><UiButton v-if="preflightFailed" variant="outline" type="button"
            :aria-disabled="checking || deleting || sessionChanged" @click="recheck">重新检查</UiButton>
          <UiButton variant="outline" :disabled="deleting || checking" @click="deletion = null"
            >取消</UiButton
          ><UiButton
            id="menu-delete-confirm"
            variant="destructive"
            :aria-disabled="deleting || checking || writeLocked || deleteReasons.length > 0"
            @click="remove"
            >{{ deleting ? '正在删除…' : '删除菜单' }}</UiButton
          ></UiDialogFooter
        ></UiDialogContent
      ></UiDialog
    >
  </section>
</template>
