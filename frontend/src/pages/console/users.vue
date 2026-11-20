<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, shallowRef, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { HugeiconsIcon } from '@hugeicons/vue'
import { Add01Icon, ArrowRight01Icon, RefreshIcon, Search01Icon } from '@hugeicons/core-free-icons'
import { getUserManageInfo } from '@/api/user/user'
import { getRoleList } from '@/api/role/role'
import { userPage, userPageNumber, userRoles } from '@/lib/users'
import type { ManagedUser, UserRole } from '@/lib/users'
import { checkReadAccess, listPage, listSize, listStatus, permissionDenied, restoreActionFocus, sessionChangedMessage } from '@/lib/rbac-mutations'
import { withSessionOwner } from '@/lib/session'
import type { UserBaseInfo } from '@/api/auth/auth.d'
import { useSession } from '@/composables/useSession'
import DeleteUserDialog from '@/components/users/DeleteUserDialog.vue'
import RolePicker from '@/components/users/RolePicker.vue'
import UserEditor from '@/components/users/UserEditor.vue'

const session = useSession()
const owner = session.capture()
const sessionChanged = computed(() => !session.owns(owner))
const route = useRoute()
const router = useRouter()
const mobile = useMediaQuery('(width < 640px)')
const filtersExpanded = ref(false)
const queryRole = Number(route.query.roleId)
const filters = reactive({
  name: typeof route.query.name === 'string' ? route.query.name.trim().slice(0, 50) : '',
  roleId: typeof route.query.roleId === 'string' && Number.isSafeInteger(queryRole) &&
    queryRole > 0 ? queryRole : null,
  status: listStatus(route.query.status),
})
function userFilters(): UserBaseInfo {
  return {
    ...(filters.name.trim() ? { name: filters.name.trim() } : {}),
    ...(filters.roleId === null ? {} : { roleId: filters.roleId }),
    ...(filters.status === 'all' ? {} : { status: Number(filters.status) }),
  }
}
const applied = shallowRef(userFilters())
const users = shallowRef<ManagedUser[]>([])
const roles = shallowRef<UserRole[]>([])
const size = ref(listSize(route.query.size))
const page = ref(listPage(route.query.page, size.value))
watch([applied, page, size], () => {
  if (sessionChanged.value) return
  void router.replace({ query: {
    ...route.query,
    name: applied.value.name || undefined,
    roleId: applied.value.roleId?.toString(),
    status: applied.value.status?.toString(),
    page: page.value > 1 ? String(page.value) : undefined,
    size: size.value !== 10 ? String(size.value) : undefined,
  } })
})
const total = ref(0)
const pending = ref(true)
const failed = ref(false)
const restricted = ref(false)
const rolePending = ref(true)
const roleFailed = ref(false)
const roleRestricted = ref(false)
const rolesUnavailable = computed(
  () => rolePending.value || roleFailed.value || !roles.value.length,
)
const editingUnavailable = computed(
  () => pending.value || rolesUnavailable.value || restricted.value || sessionChanged.value,
)
const filtered = computed(() => Object.values(applied.value).some((value) => value !== undefined))
const notice = ref('')
const editorOpen = ref(false)
const editorMode = ref<'view' | 'create' | 'edit'>('view')
const selected = shallowRef<ManagedUser | null>(null)
const deletion = shallowRef<ManagedUser | null>(null)
const number = new Intl.NumberFormat('zh-CN')
const deleting = ref(false)
const listTitle = ref<HTMLElement | null>(null)
let sheetTrigger: HTMLElement | null = null
let deleteTrigger: HTMLElement | null = null
let deleteAfterDetails: ManagedUser | null = null

function restoreSheetFocus(event: Event) {
  if (deleteAfterDetails && canDelete(deleteAfterDetails)) {
    event.preventDefault()
    deleteTrigger = sheetTrigger
    deletion.value = deleteAfterDetails
  } else {
    restoreActionFocus(event, sheetTrigger, listTitle.value)
  }
  deleteAfterDetails = null
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
let loadGeneration = 0

async function loadUsers() {
  if (!owner || !session.owns(owner)) return
  const generation = ++loadGeneration
  const isLatest = () => generation === loadGeneration && session.owns(owner)
  pending.value = true
  failed.value = false
  restricted.value = false
  try {
    const response = await withSessionOwner(owner, () =>
      getUserManageInfo(userPageNumber(page.value, size.value), size.value, applied.value),
    )
    const result = userPage(checkReadAccess(response))
    if (!isLatest()) return
    const last = Math.max(1, Math.ceil(result.total / size.value))
    if (page.value > last) {
      page.value = last
      return await loadUsers()
    }
    users.value = result.users
    total.value = result.total
  } catch (error) {
    if (!isLatest()) return
    users.value = []
    failed.value = true
    restricted.value = permissionDenied(error)
  } finally {
    if (generation === loadGeneration) pending.value = false
  }
}
function refreshUsers() {
  if (!pending.value) void loadUsers()
}
async function loadRoles() {
  if (!owner || !session.owns(owner)) return
  rolePending.value = true
  roleFailed.value = false
  roleRestricted.value = false
  try {
    const result = userRoles(checkReadAccess(await withSessionOwner(owner, getRoleList)))
    if (session.owns(owner)) roles.value = result
  } catch (error) {
    if (!session.owns(owner)) return
    roles.value = []
    roleFailed.value = true
    roleRestricted.value = permissionDenied(error)
  } finally {
    rolePending.value = false
  }
}
function refreshRoles() {
  if (!rolePending.value) void loadRoles()
}
function search() {
  if (pending.value || sessionChanged.value) return
  if (mobile.value && filtersExpanded.value) {
    filtersExpanded.value = false
    void nextTick(() => document.getElementById('users-filter-toggle')?.focus())
  }
  applied.value = userFilters()
  page.value = 1
  notice.value = ''
  void loadUsers()
}
function reset() {
  if (pending.value || sessionChanged.value) return
  Object.assign(filters, { name: '', roleId: null, status: 'all' })
  search()
}
function changePage(value: number) {
  if (!pending.value && !sessionChanged.value) {
    page.value = value
    void loadUsers()
  }
}
function changeSize(value: unknown) {
  if (pending.value || sessionChanged.value) return
  size.value = listSize(value)
  page.value = 1
  void loadUsers()
}
function isCurrentUser(user: ManagedUser) {
  return Boolean(user.email && user.email === session.state.value.user?.email)
}
function openEditor(event: Event, mode: 'view' | 'create' | 'edit', user: ManagedUser | null = null) {
  if (mode !== 'view' && editingUnavailable.value) return
  if (!editorOpen.value) sheetTrigger = event.currentTarget as HTMLElement

  selected.value = user
  editorMode.value = mode
  editorOpen.value = true
}
function canDelete(user: ManagedUser) {
  return !pending.value && !sessionChanged.value && !isCurrentUser(user)
}
function confirmDelete(event: Event, user: ManagedUser) {
  if (!canDelete(user)) return
  if (editorOpen.value) {
    deleteAfterDetails = user
    editorOpen.value = false
    return
  }
  deleteTrigger = event.currentTarget as HTMLElement
  deletion.value = user
}
async function saved(self: boolean) {
  editorOpen.value = false
  if (self) {
    if (owner) await session.signOut(true, null, undefined, owner)
    return
  }
  notice.value = editorMode.value === 'create' ? '用户已创建' : '用户信息已更新'
  if (editorMode.value === 'create') page.value = 1
  await loadUsers()
}
async function deleted() {
  deleteTrigger = null
  deletion.value = null
  notice.value = '用户已删除'
  await loadUsers()
}
onMounted(() => {
  window.addEventListener('beforeunload', beforeUnload)
  void loadUsers()
  void loadRoles()
})
onUnmounted(() => window.removeEventListener('beforeunload', beforeUnload))
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="users-title">
    <header class="flex flex-wrap items-center justify-between gap-4">
      <div class="space-y-1">
        <h1 id="users-title" class="text-2xl font-semibold tracking-tight sm:text-3xl">用户管理</h1>
        <p class="text-sm text-muted-foreground">管理账号、角色与访问状态。</p>
      </div>
      <UiButton :disabled="editingUnavailable" @click="openEditor($event, 'create')"
        ><HugeiconsIcon
          :icon="Add01Icon"
          class="size-4"
          data-icon="inline-start"
          aria-hidden="true"
        />新增用户</UiButton
      >
    </header>
    <UiAlert v-if="notice" role="status"
      ><UiAlertTitle>{{ notice }}</UiAlertTitle></UiAlert
    >
    <UiAlert v-if="sessionChanged" variant="destructive" role="alert">
      <UiAlertDescription>{{ sessionChangedMessage }}</UiAlertDescription>
    </UiAlert>
    <UiAlert v-if="roleFailed || (!rolePending && !roles.length)" variant="destructive" role="alert">
      <UiAlertTitle>{{ roleRestricted ? '当前账号无权读取角色列表' : roleFailed ? '角色列表读取失败' : '暂无可分配角色' }}</UiAlertTitle
      ><UiAlertDescription>{{ roleRestricted
        ? '角色筛选和用户保存暂不可用，请联系管理员确认权限。'
        : '角色筛选和用户保存暂不可用，请重新读取角色列表。' }}</UiAlertDescription
      ><UiButton variant="outline" size="sm" class="mt-2" :aria-disabled="rolePending || sessionChanged" @click="refreshRoles"
        >重新读取角色</UiButton
      >
    </UiAlert>

    <form
      class="flex min-w-0 flex-wrap items-end gap-2 sm:gap-x-6 sm:gap-y-4 sm:rounded-lg sm:border sm:border-border sm:bg-card sm:p-4"
      aria-label="用户筛选"
      @submit.prevent="search"
    >
      <UiField class="min-w-0 flex-1 sm:w-56 sm:flex-none"
        ><UiFieldLabel for="users-search" class="sr-only sm:not-sr-only">姓名</UiFieldLabel
        ><UiInput
          id="users-search"
          v-model="filters.name"
          placeholder="按姓名搜索"
          :disabled="pending"
          maxlength="50"
      /></UiField>
      <UiButton
        v-if="mobile"
        id="users-filter-toggle"
        type="button"
        variant="outline"
        :aria-expanded="filtersExpanded"
        aria-controls="users-extra-filters"
        @click="filtersExpanded = !filtersExpanded"
      >筛选</UiButton>
      <div
        v-show="!mobile || filtersExpanded"
        id="users-extra-filters"
        class="flex w-full flex-wrap items-end gap-4 rounded-lg border border-border bg-card p-3 sm:contents"
      >
        <UiField class="min-w-0 w-full sm:max-w-md sm:min-w-48 sm:w-auto"
          ><UiFieldLabel as="span">角色</UiFieldLabel
          ><RolePicker
            v-model="filters.roleId"
            :roles="roles"
            label="筛选角色"
            allow-all
            :disabled="pending || rolesUnavailable"
        /></UiField>
        <UiField class="w-full sm:w-auto"
          ><UiFieldLabel as="span">状态</UiFieldLabel
          ><UiRadioGroup
            v-model="filters.status"
            aria-label="筛选状态"
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
          ><UiButton variant="outline" type="button" :disabled="pending" @click="reset"
            >重置</UiButton
          >
        </div>
      </div>
    </form>

    <div class="min-w-0 rounded-lg border border-border bg-card" :aria-busy="pending">
      <div class="flex items-center justify-between gap-3 border-b-[0.5px] border-border/70 p-4">
        <h2 ref="listTitle" tabindex="-1" class="text-sm font-medium">
          用户列表
          <span class="ml-2 font-normal text-muted-foreground">
            <template v-if="failed">—</template>
            <template v-else-if="pending && !users.length">读取中</template>
            <template v-else>共 <span class="font-mono tabular-nums">{{ number.format(total) }}</span> 位用户</template>
          </span>
        </h2>
        <UiButton variant="ghost" size="sm" :aria-disabled="pending || sessionChanged" @click="refreshUsers"
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
          {{ restricted ? '当前账号无权读取用户列表' : '用户列表读取失败' }}
        </h3>
        <p class="text-sm text-muted-foreground">
          {{
            restricted
              ? '请联系管理员确认访问权限。'
              : '暂时无法取得用户数据，请检查服务连接后重试。'
          }}
        </p>
        <UiButton variant="outline" :aria-disabled="sessionChanged" @click="refreshUsers">重试</UiButton>
      </div>
      <div
        v-else-if="pending && !users.length"
        class="divide-y divide-border px-4"
        role="status"
      >
        <span class="sr-only">正在读取用户列表…</span>
        <div v-for="row in 5" :key="row" aria-hidden="true" class="grid grid-cols-3 items-center gap-4 py-5">
          <UiSkeleton class="h-4 w-3/4" />
          <UiSkeleton class="h-4 w-1/2" />
          <UiSkeleton class="ml-auto h-4 w-3/4 max-w-28" />
        </div>
      </div>
      <div
        v-else-if="!users.length"
        class="flex min-h-72 flex-col items-center justify-center gap-3 px-6 py-12 text-center"
        role="status"
      >
        <h3 class="font-medium">{{ filtered ? '没有匹配的用户' : '暂无用户' }}</h3>
        <p class="text-sm text-muted-foreground">
          {{
            filtered
              ? '试试其他姓名、角色或状态，或清除当前筛选。'
              : '创建第一个用户后，账户将显示在此处。'
          }}
        </p>
        <UiButton v-if="filtered" variant="outline" @click="reset">清除筛选</UiButton
        ><UiButton v-else variant="outline" :disabled="rolesUnavailable || sessionChanged" @click="openEditor($event, 'create')"
          >新增用户</UiButton
        >
      </div>
      <ul v-else-if="mobile" aria-label="用户账户" class="divide-y divide-border">
        <li v-for="user in users" :key="user.id">
          <button
            type="button"
            aria-haspopup="dialog"
            class="flex w-full items-center gap-3 px-3 py-3 text-left hover:bg-muted focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-ring"
            @click="openEditor($event, 'view', user)"
          >
            <UiAvatar class="size-8 shrink-0">
              <UiAvatarFallback>{{ user.name.slice(0, 1) || '?' }}</UiAvatarFallback>
            </UiAvatar>
            <span class="min-w-0 flex-1">
              <span class="block truncate text-sm font-medium">{{ user.name || '未命名用户' }}</span>
              <span class="block truncate text-xs text-muted-foreground">{{ user.email || '未填写邮箱' }}</span>
            </span>
            <StatusBadge
              class="shrink-0"
              :tone="user.status === 1 ? 'success' : user.status === 0 ? 'warning' : 'neutral'"
            >{{ user.status === 1 ? '启用' : user.status === 0 ? '停用' : '未知' }}</StatusBadge>
            <HugeiconsIcon :icon="ArrowRight01Icon" class="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          </button>
        </li>
      </ul>
      <UiTable v-else>
        <UiTableCaption class="sr-only">用户账户、角色、状态与操作</UiTableCaption>
        <UiTableHeader
          ><UiTableRow class="border-b-[0.5px]"
            ><UiTableHead class="px-4">用户</UiTableHead
            ><UiTableHead class="hidden md:table-cell">角色</UiTableHead
            ><UiTableHead class="text-center">状态</UiTableHead
            ><UiTableHead class="hidden text-right xl:table-cell">创建日期</UiTableHead
            ><UiTableHead
              class="sticky right-0 z-10 w-32 min-w-32 bg-card text-center shadow-[-4px_0_6px_-6px_var(--border)]"
              >操作</UiTableHead
            ></UiTableRow
          ></UiTableHeader
        >
        <UiTableBody
          ><UiTableRow v-for="user in users" :key="user.id" class="border-b-[0.5px] hover:bg-muted">
            <UiTableCell class="min-w-40 whitespace-normal px-4"
              ><div class="flex items-center gap-3">
                <UiAvatar class="hidden size-8 shrink-0 sm:flex"
                  ><UiAvatarFallback>{{ user.name.slice(0, 1) || '?' }}</UiAvatarFallback></UiAvatar
                >
                <div class="min-w-0">
                  <UiButton
                    variant="ghost"
                    class="h-auto max-w-48 justify-start whitespace-normal break-words p-0 text-left font-medium hover:bg-transparent hover:text-accent-data-foreground hover:underline underline-offset-4"
                    @click="openEditor($event, 'view', user)"
                    >{{ user.name || '未命名用户' }}</UiButton
                  >
                  <p class="mt-1 max-w-56 break-all text-xs text-muted-foreground">
                    {{ user.email || '未填写邮箱' }}
                  </p>
                  <p class="mt-1 text-xs text-muted-foreground md:hidden">
                    {{ user.roleName || '未分配角色' }}
                  </p>
                  <StatusBadge v-if="isCurrentUser(user)" :id="`self-user-${user.id}`" class="mt-1">
                    当前账号 · 无法自删
                  </StatusBadge>
                </div>
              </div></UiTableCell
            >
            <UiTableCell class="hidden max-w-48 whitespace-normal break-words md:table-cell">{{
              user.roleName || (user.roleId ? `角色 #${user.roleId}` : '未分配')
            }}</UiTableCell>
            <UiTableCell class="text-center"
              ><StatusBadge
                :tone="user.status === 1 ? 'success' : user.status === 0 ? 'warning' : 'neutral'"
                >{{ user.status === 1 ? '启用' : user.status === 0 ? '停用' : '未知' }}</StatusBadge
              ></UiTableCell
            >
            <UiTableCell
              class="hidden text-right font-mono tabular-nums text-muted-foreground xl:table-cell"
              >{{ user.createTime || '—' }}</UiTableCell
            >
            <UiTableCell
              class="sticky right-0 z-10 w-32 min-w-32 bg-card group-hover/row:bg-muted text-center shadow-[-4px_0_6px_-6px_var(--border)]"
              ><div class="flex justify-center gap-1">
                <UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`查看${user.name || user.id}`"
                  @click="openEditor($event, 'view', user)"
                  >查看</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`编辑${user.name || user.id}`"
                  :aria-disabled="editingUnavailable"
                  @click="openEditor($event, 'edit', user)"
                  >编辑</UiButton
                ><span :title="isCurrentUser(user) ? '无法删除自己的账号' : undefined">
                  <UiButton
                    variant="ghost"
                    size="sm"
                    class="text-destructive hover:text-destructive aria-disabled:opacity-50 aria-disabled:cursor-not-allowed"
                    :aria-label="`删除${user.name || user.id}`"
                    :aria-describedby="isCurrentUser(user) ? `self-user-${user.id}` : undefined"
                    :aria-disabled="!canDelete(user)"
                    @click="confirmDelete($event, user)"
                    >删除</UiButton
                  >
                </span>
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
        <div class="flex min-w-0 flex-wrap items-center gap-3">
          <span class="whitespace-nowrap font-mono text-xs tabular-nums text-muted-foreground"
            >第 {{ page }} / {{ Math.max(1, Math.ceil(total / size)) }} 页</span
          ><UiPagination
            :page="page"
            :total="total"
            :items-per-page="size"
            :disabled="pending"
            :sibling-count="mobile ? 0 : 1"
            aria-label="用户列表分页"
            class="w-auto"
            @update:page="changePage"
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
    <UserEditor
      v-model:open="editorOpen"
      :mode="editorMode"
      :user="selected"
      :roles="roles"
      :roles-unavailable="rolesUnavailable"
      :role-pending="rolePending"
      :role-restricted="roleRestricted"
      @saved="saved"
      @retry-roles="refreshRoles"
      @close-auto-focus="restoreSheetFocus"
    >
      <template v-if="mobile && selected" #detail-actions>
        <UiButton :disabled="editingUnavailable" @click="openEditor($event, 'edit', selected)">编辑</UiButton>
        <UiButton
          variant="outline"
          class="text-destructive hover:text-destructive"
          :aria-disabled="!canDelete(selected)"
          :aria-describedby="isCurrentUser(selected) ? 'mobile-self-user' : undefined"
          @click="confirmDelete($event, selected)"
        >删除</UiButton>
        <span v-if="isCurrentUser(selected)" id="mobile-self-user" class="sr-only">无法删除自己的账号</span>
      </template>
    </UserEditor>
    <DeleteUserDialog
      :user="deletion"
      :session-changed="sessionChanged"
      @pending="deleting = $event"
      @close="deletion = null"
      @deleted="deleted"
      @close-auto-focus="restoreDeleteFocus"
    />
  </section>
</template>
