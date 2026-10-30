<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { Add01Icon, RefreshIcon, Search01Icon } from '@hugeicons/core-free-icons'
import { deleteRole, getRoleList, getUserRole } from '@/api/auth/auth'
import { getUserRoutes } from '@/api/router/router'
import { currentRoleId, managedRoles, permissionTree, roleWriteSucceeded } from '@/lib/roles'
import type { ManagedRole, PermissionNode } from '@/lib/roles'

definePageMeta({ layout: 'console', middleware: 'auth' })
const session = useSession()
const roles = shallowRef<ManagedRole[]>([])
const tree = shallowRef<PermissionNode[]>([])
const actorId = ref<number | null>(null)
const pending = ref(true)
const failed = ref(false)
const restricted = ref(false)
const treePending = ref(true)
const treeFailed = ref(false)
const actorPending = ref(true)
const actorFailed = ref(false)
const filters = reactive({ name: '', status: 'all' })
const applied = reactive({ name: '', status: 'all' })
const page = ref(1)
const size = ref(10)
const notice = ref('')
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
const locked = computed(() => pending.value || actorId.value === null || restricted.value)
const treeUnavailable = computed(() => treePending.value || treeFailed.value)
const open = ref(false)
const mode = ref<'view' | 'create' | 'edit' | 'grant'>('view')
const selected = shallowRef<ManagedRole | null>(null)
const deletion = shallowRef<ManagedRole | null>(null)
const deleting = ref(false)
const deleteFailed = ref(false)

function clampPage() {
  page.value = Math.min(page.value, Math.max(1, Math.ceil(filtered.value.length / size.value)))
}
async function loadRoles() {
  pending.value = true
  failed.value = false
  restricted.value = false
  try {
    roles.value = managedRoles(await getRoleList())
    clampPage()
  } catch (error) {
    roles.value = []
    failed.value = true
    restricted.value = Boolean(
      error && typeof error === 'object' && 'code' in error && error.code === 403,
    )
  } finally {
    pending.value = false
  }
}
async function loadTree() {
  treePending.value = true
  treeFailed.value = false
  try {
    tree.value = permissionTree(await getUserRoutes())
  } catch {
    tree.value = []
    treeFailed.value = true
  } finally {
    treePending.value = false
  }
}
async function loadActor() {
  actorPending.value = true
  actorFailed.value = false
  try {
    actorId.value = currentRoleId(await getUserRole())
  } catch {
    actorId.value = null
    actorFailed.value = true
  } finally {
    actorPending.value = false
  }
}
function search() {
  Object.assign(applied, { name: filters.name.trim(), status: filters.status })
  page.value = 1
  notice.value = ''
}
function reset() {
  Object.assign(filters, { name: '', status: 'all' })
  search()
}
function changeSize(value: unknown) {
  size.value = Number(value)
  page.value = 1
}
function show(nextMode: typeof mode.value, role: ManagedRole | null = null) {
  mode.value = nextMode
  selected.value = role
  open.value = true
}
async function saved(self: boolean, rows?: ManagedRole[]) {
  open.value = false
  if (self) {
    await nextTick()
    await session.signOut()
    return
  }
  notice.value =
    mode.value === 'grant'
      ? '菜单授权已更新并回读确认'
      : mode.value === 'create'
        ? '角色已创建，可继续配置菜单授权'
        : '角色信息已更新'
  if (rows) {
    roles.value = rows
    clampPage()
  } else await loadRoles()
}
function readRoles(rows: ManagedRole[]) {
  roles.value = rows
  notice.value = ''
  clampPage()
}
function confirmDelete(role: ManagedRole) {
  if (locked.value || role.id === actorId.value || role.roleName === 'USER') return
  deletion.value = role
  deleteFailed.value = false
}
async function remove() {
  if (
    !deletion.value ||
    deleting.value ||
    deletion.value.id === actorId.value ||
    deletion.value.roleName === 'USER'
  )
    return
  deleting.value = true
  deleteFailed.value = false
  try {
    roleWriteSucceeded(await deleteRole(deletion.value.id))
    deletion.value = null
    notice.value = '角色已删除'
    await loadRoles()
  } catch {
    deleteFailed.value = true
  } finally {
    deleting.value = false
  }
}
onMounted(() => {
  void loadRoles()
  void loadTree()
  void loadActor()
})
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="roles-title">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="space-y-2">
        <h1 id="roles-title" class="text-2xl font-semibold tracking-tight">角色管理</h1>
        <p class="text-sm text-muted-foreground">管理角色资料与菜单、操作授权。</p>
      </div>
      <UiButton :disabled="locked" @click="show('create')"
        ><HugeiconsIcon
          :icon="Add01Icon"
          class="size-4 -ml-0.5"
          aria-hidden="true"
        />新增角色</UiButton
      >
    </header>
    <UiAlert v-if="notice" role="status"
      ><UiAlertTitle>{{ notice }}</UiAlertTitle></UiAlert
    >
    <UiAlert v-if="actorFailed" role="alert"
      ><UiAlertTitle>无法确认当前角色</UiAlertTitle
      ><UiAlertDescription>写入操作暂不可用，请重新读取当前账号的角色信息。</UiAlertDescription
      ><UiButton
        variant="outline"
        size="sm"
        class="mt-2"
        :disabled="actorPending"
        @click="loadActor"
        >重新读取当前角色</UiButton
      ></UiAlert
    >
    <UiAlert v-if="treeFailed" role="alert"
      ><UiAlertTitle>菜单列表读取失败</UiAlertTitle
      ><UiAlertDescription>角色列表仍可查看，菜单授权暂不可用。</UiAlertDescription
      ><UiButton variant="outline" size="sm" class="mt-2" :disabled="treePending" @click="loadTree"
        >重新读取菜单</UiButton
      ></UiAlert
    >

    <form
      class="flex flex-wrap items-end gap-x-6 gap-y-4"
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
      <div class="flex items-center justify-between gap-3 border-b-[0.5px] border-border/70 p-4">
        <h2 class="text-sm font-medium">
          角色列表
          <span class="ml-2 font-normal text-muted-foreground">{{
            failed ? '—' : pending && !roles.length ? '读取中' : `共 ${filtered.length} 个角色`
          }}</span>
        </h2>
        <UiButton variant="ghost" size="sm" :disabled="pending" @click="loadRoles"
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
        <UiButton variant="outline" @click="loadRoles">重试</UiButton>
      </div>
      <div
        v-else-if="pending && !roles.length"
        class="flex min-h-72 items-center justify-center text-sm text-muted-foreground"
        role="status"
      >
        正在读取角色列表…
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
        ><UiButton v-else :disabled="locked" @click="show('create')">新增角色</UiButton>
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
              class="sticky right-0 w-44 min-w-44 bg-card text-center shadow-[-4px_0_6px_-6px_var(--border)]"
              >操作</UiTableHead
            ></UiTableRow
          ></UiTableHeader
        >
        <UiTableBody
          ><UiTableRow v-for="role in visible" :key="role.id" class="border-b-[0.5px]">
            <UiTableCell class="min-w-36 whitespace-normal px-4"
              ><UiButton
                variant="ghost"
                class="h-auto max-w-56 justify-start whitespace-normal break-all p-0 text-left font-medium hover:bg-transparent"
                :aria-label="`查看${role.roleName}`"
                @click="show('view', role)"
                >{{ role.roleName }}</UiButton
              >
              <p class="mt-1 text-xs text-muted-foreground">
                ID {{ role.id }}<span v-if="role.roleName === 'USER'" class="ml-2">系统默认</span
                ><span v-if="role.id === actorId" class="ml-2">当前账号</span>
              </p>
              <p class="mt-1 text-xs text-muted-foreground sm:hidden">
                {{ role.status === 1 ? '启用' : role.status === 0 ? '停用' : '未知' }} ·
                {{ role.permissions.length }} 项权限
              </p></UiTableCell
            >
            <UiTableCell class="hidden text-center sm:table-cell"
              ><UiBadge
                variant="secondary"
                :class="
                  role.status === 1
                    ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                    : 'bg-muted text-muted-foreground'
                "
                >{{ role.status === 1 ? '启用' : role.status === 0 ? '停用' : '未知' }}</UiBadge
              ></UiTableCell
            >
            <UiTableCell class="hidden text-right tabular-nums md:table-cell">{{
              role.permissions.length
            }}</UiTableCell
            ><UiTableCell
              class="hidden max-w-56 whitespace-normal break-words text-muted-foreground lg:table-cell"
              >{{ role.remark || '—' }}</UiTableCell
            >
            <UiTableCell
              class="sticky right-0 w-44 min-w-44 bg-card text-center shadow-[-4px_0_6px_-6px_var(--border)]"
              ><div class="flex justify-center">
                <UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`授权${role.roleName}`"
                  :disabled="locked || treeUnavailable"
                  @click="show('grant', role)"
                  >菜单授权</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`编辑${role.roleName}`"
                  :disabled="locked"
                  @click="show('edit', role)"
                  >编辑</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  class="text-destructive hover:text-destructive"
                  :aria-label="`删除${role.roleName}`"
                  :disabled="locked || role.id === actorId || role.roleName === 'USER'"
                  @click="confirmDelete(role)"
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
          <span class="whitespace-nowrap text-xs tabular-nums text-muted-foreground"
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
    <RolesRolePanel
      v-model:open="open"
      :mode="mode"
      :role="selected"
      :tree="tree"
      :tree-unavailable="treeUnavailable"
      :actor-role-id="actorId"
      :locked="locked"
      @saved="saved"
      @roles-read="readRoles"
      @retry-tree="loadTree"
      @unconfirmed="notice = '授权已提交，尚未确认最终结果，请重新读取角色核对'"
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
        class="top-[38.2%]! sm:max-w-[400px]!"
        @pointer-down-outside.prevent
        @interact-outside.prevent
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
        <p class="text-xs text-muted-foreground">角色 ID：{{ deletion?.id }}</p>
        <p v-if="deleteFailed" role="alert" class="text-sm text-destructive">
          删除失败，请检查服务连接以及是否仍有关联用户后重试。
        </p>
        <UiDialogFooter
          ><UiButton variant="outline" :disabled="deleting" @click="deletion = null">取消</UiButton
          ><UiButton variant="destructive" :disabled="deleting" @click="remove">{{
            deleting ? '正在删除…' : '删除角色'
          }}</UiButton></UiDialogFooter
        ></UiDialogContent
      ></UiDialog
    >
  </section>
</template>
