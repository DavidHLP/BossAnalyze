<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { Add01Icon, ArrowRight01Icon, RefreshIcon, Search01Icon } from '@hugeicons/core-free-icons'
import { getUserRoutes, deleteRouter } from '@/api/router/router'
import { getRoleList } from '@/api/auth/auth'
import type { Router } from '@/router/index.d'
import { managedRoles } from '@/lib/roles'
import {
  managedMenus,
  menuRows,
  visibleMenus,
  menuDeleteReason,
  menuWriteSucceeded,
} from '@/lib/menus'
import type { MenuRecord } from '@/lib/menus'

definePageMeta({ layout: 'console', middleware: 'auth' })
const types: Record<string, string> = { C: '目录', M: '菜单', F: '操作' }
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
const open = ref(false)
const mode = ref<'view' | 'create' | 'edit'>('view')
const selected = shallowRef<MenuRecord | null>(null)
const parent = shallowRef<MenuRecord | null>(null)
const deletion = shallowRef<MenuRecord | null>(null)
const deleting = ref(false)
const checking = ref(false)
const deleteReason = ref('')
const deleteFailed = ref(false)
async function load() {
  pending.value = true
  failed.value = false
  restricted.value = false
  try {
    tree.value = managedMenus(await getUserRoutes())
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
  mode.value = next
  selected.value = node
  parent.value = ancestor
  open.value = true
}
function saved(value: MenuRecord[]) {
  tree.value = value
  open.value = false
  notice.value = '菜单已保存并回读确认。重新登录后导航会应用最新配置。'
}
function read(value: MenuRecord[]) {
  tree.value = value
  notice.value = '已重新读取菜单，请核对最终配置。'
}
async function prepareDelete(node: MenuRecord) {
  deletion.value = node
  checking.value = true
  deleteFailed.value = false
  deleteReason.value = ''
  try {
    const [menus, roles] = await Promise.all([getUserRoutes(), getRoleList()])
    const fresh = managedMenus(menus)
    const current = menuRows(fresh).find((row) => row.node.id === node.id)?.node
    if (!current) throw new Error('Menu missing')
    tree.value = fresh
    deletion.value = current
    deleteReason.value = menuDeleteReason(
      current,
      fresh,
      managedRoles(roles).flatMap((role) => role.permissions),
    )
  } catch {
    deleteReason.value = '无法核对菜单及角色引用，请关闭后重试。'
  } finally {
    checking.value = false
  }
}
async function remove() {
  if (!deletion.value || deleting.value || checking.value || deleteReason.value) return
  deleting.value = true
  deleteFailed.value = false
  let accepted = false
  try {
    const [menus, roles] = await Promise.all([getUserRoutes(), getRoleList()])
    const fresh = managedMenus(menus)
    const target = menuRows(fresh).find((row) => row.node.id === deletion.value?.id)?.node
    if (!target) throw new Error('Menu missing')
    const reason = menuDeleteReason(
      target,
      fresh,
      managedRoles(roles).flatMap((role) => role.permissions),
    )
    if (reason) {
      deleteReason.value = reason
      tree.value = fresh
      return
    }
    // Send no descendants: the backend deletes supplied children and their permission records.
    menuWriteSucceeded(
      await deleteRouter({
        id: target.id,
        name: target.name,
        permission: target.permission,
        children: [],
      } as unknown as Router),
    )
    accepted = true
    const result = managedMenus(await getUserRoutes())
    if (menuRows(result).some((row) => row.node.id === target.id))
      throw new Error('Delete not confirmed')
    tree.value = result
    deletion.value = null
    notice.value = '菜单已删除并回读确认。重新登录后导航会应用最新配置。'
  } catch {
    if (accepted) {
      deletion.value = null
      notice.value = '删除请求已提交，尚未确认结果，请刷新菜单核对。'
      await load()
    } else deleteFailed.value = true
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
      <UiButton :disabled="pending || failed" @click="show('create')"
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
    <form
      class="flex flex-wrap items-end gap-x-6 gap-y-4"
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
    <div class="min-w-0 rounded-lg border-[0.5px] border-border/70 bg-card" :aria-busy="pending">
      <div
        class="flex flex-wrap items-center justify-between gap-3 border-b-[0.5px] border-border/70 p-4"
      >
        <h2 class="text-sm font-medium">
          菜单树
          <span class="ml-2 font-normal text-muted-foreground">{{
            failed ? '—' : pending ? '读取中' : `共 ${total} 项`
          }}</span>
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
        ><UiButton v-else :disabled="pending" @click="show('create')">新增菜单</UiButton>
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
              class="sticky right-0 w-44 min-w-44 bg-card text-center shadow-[-4px_0_6px_-6px_var(--border)]"
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
            <UiTableCell class="min-w-48 max-w-80 whitespace-normal px-4"
              ><div
                class="flex items-start gap-1"
                :style="{ paddingLeft: `${Math.min(row.depth, 5) * 16}px` }"
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
                  <p class="mt-1 text-xs text-muted-foreground">
                    ID {{ row.node.id
                    }}<span v-if="row.node.meta.hidden || row.node.meta.metaHidden" class="ml-2"
                      >隐藏</span
                    >
                  </p>
                  <p class="mt-1 text-xs text-muted-foreground md:hidden">
                    {{ row.node.status === 1 ? '启用' : row.node.status === 0 ? '停用' : '未知' }}
                  </p>
                </div>
              </div></UiTableCell
            >
            <UiTableCell class="hidden sm:table-cell"
              ><UiBadge variant="secondary" class="bg-muted text-muted-foreground">{{
                types[row.node.meta.type] || '未知'
              }}</UiBadge></UiTableCell
            >
            <UiTableCell class="hidden text-center md:table-cell"
              ><UiBadge
                variant="secondary"
                :class="
                  row.node.status === 1
                    ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                    : 'bg-muted text-muted-foreground'
                "
                >{{
                  row.node.status === 1 ? '启用' : row.node.status === 0 ? '停用' : '未知'
                }}</UiBadge
              ></UiTableCell
            >
            <UiTableCell class="hidden max-w-56 whitespace-normal break-all text-xs lg:table-cell"
              ><p>{{ row.node.path }}</p>
              <p class="mt-1 text-muted-foreground">
                {{ row.node.permission || '无权限标识' }}
              </p></UiTableCell
            ><UiTableCell class="hidden text-right tabular-nums xl:table-cell">{{
              row.node.menuOrder
            }}</UiTableCell>
            <UiTableCell
              class="sticky right-0 w-44 min-w-44 bg-card text-center shadow-[-4px_0_6px_-6px_var(--border)]"
              ><div class="flex justify-center">
                <UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`添加${row.node.meta.metaTitle}的子项`"
                  :disabled="
                    pending ||
                    row.node.meta.type === 'F' ||
                    (row.node.pid !== null && row.node.pid >= row.node.id)
                  "
                  @click="show('create', null, row.node)"
                  >添加子项</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`编辑${row.node.meta.metaTitle}`"
                  :disabled="pending || row.node.permission === null"
                  @click="show('edit', row.node)"
                  >编辑</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  class="text-destructive hover:text-destructive"
                  :aria-label="`删除${row.node.meta.metaTitle}`"
                  :disabled="pending || row.node.children.length > 0"
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
      @saved="saved"
      @read="read"
      @unconfirmed="notice = '菜单配置已提交，尚未确认结果，请重新读取后核对。'"
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
        <p class="text-xs text-muted-foreground">菜单 ID：{{ deletion?.id }}</p>
        <p v-if="checking" role="status" class="text-sm text-muted-foreground">
          正在核对菜单和角色引用…
        </p>
        <p v-else-if="deleteReason" role="alert" class="text-sm text-muted-foreground">
          {{ deleteReason }}
        </p>
        <p v-if="deleteFailed" role="alert" class="text-sm text-destructive">
          删除失败，请检查服务连接后重试。
        </p>
        <UiDialogFooter
          ><UiButton variant="outline" :disabled="deleting || checking" @click="deletion = null"
            >取消</UiButton
          ><UiButton
            variant="destructive"
            :disabled="deleting || checking || Boolean(deleteReason)"
            @click="remove"
            >{{ deleting ? '正在删除…' : '删除菜单' }}</UiButton
          ></UiDialogFooter
        ></UiDialogContent
      ></UiDialog
    >
  </section>
</template>
