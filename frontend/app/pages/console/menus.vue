<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { Add01Icon, ArrowRight01Icon, RefreshIcon, Search01Icon } from '@hugeicons/core-free-icons'
import { getUserRoutes, deleteRouter } from '@/api/router/router'
import { getRoleList } from '@/api/auth/auth'
import { mutationError } from '@/lib/rbac-mutations'
import { managedRoles } from '@/lib/roles'
import {
  managedMenus,
  menuRows,
  visibleMenus,
  menuDeleteReasons,
  menuWriteSucceeded,
} from '@/lib/menus'
import type { MenuRecord } from '@/lib/menus'

definePageMeta({ layout: 'console', middleware: 'auth' })
const types = {
  C: { label: '目录', tone: 'neutral' },
  M: { label: '菜单', tone: 'success' },
  F: { label: '操作', tone: 'warning' },
} as const
const menuType = (type: string) =>
  type === 'C' || type === 'M' || type === 'F' ? types[type] : undefined
const tree = shallowRef<MenuRecord[]>([])
const pending = ref(true)
const failed = ref(false)
const restricted = ref(false)
const filters = reactive({ query: '', status: 'all' })
const applied = reactive({ query: '', status: 'all' })
const collapsed = ref<number[]>([])
const rows = computed(() =>
  visibleMenus(tree.value, applied.query, applied.status, collapsed.value),
)
const filtering = computed(() => Boolean(applied.query || applied.status !== 'all'))
const total = computed(() => menuRows(tree.value).length)
const notice = ref('')
const navigationNotice = '重新登录后将更新左侧导航栏。'
const awaitingRead = ref(false)
const open = ref(false)
const mode = ref<'view' | 'create' | 'edit'>('view')
const selected = shallowRef<MenuRecord | null>(null)
const parent = shallowRef<MenuRecord | null>(null)
const deletion = shallowRef<MenuRecord | null>(null)
const deleting = ref(false)
const checking = ref(false)
const deleteReasons = ref<string[]>([])
const deleteFailure = ref('')
async function load() {
  pending.value = true
  failed.value = false
  restricted.value = false
  try {
    tree.value = managedMenus(await getUserRoutes())
    if (awaitingRead.value) read(tree.value)
  } catch (error) {
    tree.value = []
    failed.value = true
    restricted.value = Boolean(
      error && typeof error === 'object' && 'code' in error && error.code === 403,
    )
  } finally {
    pending.value = false
  }
}
function search() {
  Object.assign(applied, { query: filters.query.trim(), status: filters.status })
  notice.value = ''
}
function reset() {
  Object.assign(filters, { query: '', status: 'all' })
  search()
}
function toggle(id: number) {
  collapsed.value = collapsed.value.includes(id)
    ? collapsed.value.filter((value) => value !== id)
    : [...collapsed.value, id]
}
function show(
  next: typeof mode.value,
  node: MenuRecord | null = null,
  ancestor: MenuRecord | null = null,
) {
  if (awaitingRead.value && next !== 'view') return
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
  awaitingRead.value = false
  notice.value = `已重新读取菜单，请核对最终配置。${navigationNotice}`
}
function lockSave() {
  awaitingRead.value = true
  notice.value = ''
}
async function preflight(id: number) {
  checking.value = true
  deleteReasons.value = []
  try {
    const [menus, roles] = await Promise.all([getUserRoutes(), getRoleList()])
    const fresh = managedMenus(menus)
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
      managedRoles(roles).flatMap((role) => role.permissions),
    )
    if (!deleteReasons.value.length) return current
  } catch {
    deleteReasons.value = ['无法核对菜单及角色引用，请关闭后重试。']
  } finally {
    checking.value = false
  }
}
async function prepareDelete(node: MenuRecord) {
  if (awaitingRead.value || checking.value || deleting.value) return
  deletion.value = node
  deleteFailure.value = ''
  await preflight(node.id)
}
async function remove() {
  if (
    !deletion.value || deleting.value || checking.value || awaitingRead.value ||
    deleteReasons.value.length
  ) return
  deleteFailure.value = ''
  const target = await preflight(deletion.value.id)
  if (!target) return
  deleting.value = true
  let accepted = false
  try {
    // Send no descendants: the backend deletes supplied children and their permission records.
    menuWriteSucceeded(
      await deleteRouter({
        id: target.id,
        name: target.name,
        permission: target.permission,
        children: [],
      }),
    )
    accepted = true
    const result = managedMenus(await getUserRoutes())
    if (menuRows(result).some((row) => row.node.id === target.id))
      throw new Error('Delete not confirmed')
    tree.value = result
    deletion.value = null
    notice.value = `菜单已删除并回读确认。${navigationNotice}`
  } catch (error) {
    if (accepted) {
      deletion.value = null
      notice.value = `删除请求已提交，未确认。请刷新菜单核对。${navigationNotice}`
      await load()
    } else deleteFailure.value = mutationError(error, '删除', '菜单依赖可能已变化，请重新核对后重试。')
  } finally {
    deleting.value = false
  }
}
onMounted(load)
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="menus-title">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="space-y-2">
        <h1 id="menus-title" class="text-2xl font-semibold tracking-tight">菜单管理</h1>
        <p class="text-sm text-muted-foreground">管理导航层级、路由配置与操作权限。</p>
      </div>
      <UiButton :disabled="pending || failed || awaitingRead" @click="show('create')"
        ><HugeiconsIcon
          :icon="Add01Icon"
          class="size-4 -ml-0.5"
          aria-hidden="true"
        />新增菜单</UiButton
      >
    </header>
    <UiAlert v-if="notice" role="status"
      ><UiAlertTitle>{{ notice }}</UiAlertTitle></UiAlert
    >
    <UiAlert v-if="awaitingRead" class="border-status-warning/40 bg-status-warning-surface text-status-warning-foreground">
      <UiAlertTitle>菜单配置已提交，未确认</UiAlertTitle>
      <UiAlertDescription class="text-inherit">请重新读取菜单并核对最终配置。</UiAlertDescription>
      <UiButton variant="outline" size="sm" class="mt-2 text-foreground" :disabled="pending" @click="load">
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
        <UiButton type="submit" :disabled="pending"
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
    <div class="min-w-0 rounded-lg border border-border bg-card shadow-panel" :aria-busy="pending">
      <div
        class="flex flex-wrap items-center justify-between gap-3 border-b-[0.5px] border-border/70 p-4"
      >
        <h2 class="text-sm font-medium">
          菜单树
          <span class="ml-2 font-normal text-muted-foreground">
            <template v-if="failed">—</template>
            <template v-else-if="pending">读取中</template>
            <template v-else>共 <span class="font-mono tabular-nums">{{ total }}</span> 项</template>
          </span>
        </h2>
        <div class="flex gap-1">
          <UiButton
            variant="ghost"
            size="sm"
            :disabled="pending || failed || filtering"
            @click="collapsed = []"
            >展开全部</UiButton
          ><UiButton
            variant="ghost"
            size="sm"
            :disabled="pending || failed || filtering"
            @click="
              collapsed = menuRows(tree)
                .filter((row) => row.node.children.length)
                .map((row) => row.node.id)
            "
            >收起全部</UiButton
          ><UiButton variant="ghost" size="sm" :disabled="pending" @click="load"
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
        <UiButton variant="outline" @click="load">重试</UiButton>
      </div>
      <div
        v-else-if="pending && !tree.length"
        role="status"
        class="flex min-h-72 items-center justify-center text-sm text-muted-foreground"
      >
        正在读取菜单…
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
        ><UiButton v-else :disabled="pending || awaitingRead" @click="show('create')">新增菜单</UiButton>
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
              class="sticky right-0 z-10 w-40 min-w-40 bg-card text-center shadow-[-4px_0_6px_-6px_var(--border)] sm:w-44 sm:min-w-44"
              >操作</UiTableHead
            ></UiTableRow
          ></UiTableHeader
        >
        <UiTableBody
          ><UiTableRow
            v-for="row in rows"
            :key="row.node.id"
            class="border-b-[0.5px]"
            :class="{ 'bg-muted/25': filtering && !row.match }"
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
                    class="h-auto min-h-8 justify-start whitespace-normal break-all p-0 text-left font-medium hover:bg-transparent"
                    :aria-label="`查看${row.node.meta.metaTitle}`"
                    @click="show('view', row.node)"
                    >{{ row.node.meta.metaTitle }}</UiButton
                  >
                  <div class="mt-1 flex flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                    <span class="font-mono tabular-nums">ID {{ row.node.id }}</span>
                    <StatusBadge v-if="filtering && !row.match">祖先上下文</StatusBadge>
                    <StatusBadge v-if="row.node.meta.hidden" tone="warning">整枝隐藏</StatusBadge>
                    <StatusBadge v-if="row.node.meta.metaHidden">仅藏导航</StatusBadge>
                    <StatusBadge class="sm:hidden" :tone="menuType(row.node.meta.type)?.tone">
                      {{ menuType(row.node.meta.type)?.label ?? '未知类型' }}
                    </StatusBadge>
                    <StatusBadge class="md:hidden"
                      :tone="row.node.status === 1 ? 'success' : row.node.status === 0 ? 'warning' : 'neutral'">
                      {{ row.node.status === 1 ? '启用' : row.node.status === 0 ? '停用' : '未知' }}
                    </StatusBadge>
                  </div>
                </div>
              </div></UiTableCell
            >
            <UiTableCell class="hidden sm:table-cell"
              ><StatusBadge :tone="menuType(row.node.meta.type)?.tone">{{
                menuType(row.node.meta.type)?.label ?? '未知类型'
              }}</StatusBadge></UiTableCell
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
              class="sticky right-0 z-10 w-40 min-w-40 bg-card text-center shadow-[-4px_0_6px_-6px_var(--border)] sm:w-44 sm:min-w-44"
              ><div class="flex justify-center">
                <UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`添加${row.node.meta.metaTitle}的子项`"
                  :disabled="
                    pending || awaitingRead ||
                    row.node.meta.type === 'F' ||
                    (row.node.pid !== null && row.node.pid >= row.node.id)
                  "
                  @click="show('create', null, row.node)"
                  >添加子项</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`编辑${row.node.meta.metaTitle}`"
                  :disabled="pending || awaitingRead || row.node.permission === null"
                  @click="show('edit', row.node)"
                  >编辑</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  class="text-destructive hover:text-destructive"
                  :aria-label="`删除${row.node.meta.metaTitle}`"
                  :disabled="pending || awaitingRead"
                  @click="prepareDelete(row.node)"
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
      :awaiting-read="awaitingRead"
      @saved="saved"
      @read="read"
      @unconfirmed="lockSave"
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
        <UiAlert v-else-if="deleteReasons.length" class="border-status-warning/40 bg-status-warning-surface text-status-warning-foreground">
          <UiAlertTitle>暂不能删除</UiAlertTitle>
          <ul class="list-disc space-y-1 pl-5">
            <li v-for="reason in deleteReasons" :key="reason">{{ reason }}</li>
          </ul>
        </UiAlert>
        <p v-else-if="!deleting" role="status" class="text-sm text-muted-foreground">
          依赖预检通过，确认删除时将再次核对。
        </p>
        <p v-if="deleteFailure" role="alert" class="text-sm text-destructive">
          {{ deleteFailure }}
        </p>
        <UiDialogFooter
          ><UiButton variant="outline" :disabled="deleting || checking" @click="deletion = null"
            >取消</UiButton
          ><UiButton
            variant="destructive"
            :disabled="deleting || checking || deleteReasons.length > 0"
            @click="remove"
            >{{ deleting ? '正在删除…' : '删除菜单' }}</UiButton
          ></UiDialogFooter
        ></UiDialogContent
      ></UiDialog
    >
  </section>
</template>
