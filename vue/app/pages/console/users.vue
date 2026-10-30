<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { Add01Icon, RefreshIcon, Search01Icon } from '@hugeicons/core-free-icons'
import { getUserManageInfo } from '@/api/user/user'
import { getRoleList } from '@/api/role/role'
import { userPage, userPageNumber, userRoles } from '@/lib/users'
import type { ManagedUser, UserRole } from '@/lib/users'
import type { UserBaseInfo } from '@/api/auth/auth.d'

definePageMeta({ layout: 'console', middleware: 'auth' })
const session = useSession()
const filters = reactive({ name: '', roleId: null as number | null, status: 'all' })
const applied = shallowRef<UserBaseInfo>({})
const users = shallowRef<ManagedUser[]>([])
const roles = shallowRef<UserRole[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const pending = ref(true)
const failed = ref(false)
const restricted = ref(false)
const rolePending = ref(true)
const roleFailed = ref(false)
const rolesUnavailable = computed(
  () => rolePending.value || roleFailed.value || !roles.value.length,
)
const filtered = computed(() => Object.values(applied.value).some((value) => value !== undefined))
const notice = ref('')
const editorOpen = ref(false)
const editorMode = ref<'view' | 'create' | 'edit'>('view')
const selected = shallowRef<ManagedUser | null>(null)
const deletion = shallowRef<ManagedUser | null>(null)
const number = new Intl.NumberFormat('zh-CN')

async function loadUsers() {
  pending.value = true
  failed.value = false
  restricted.value = false
  try {
    const result = userPage(
      await getUserManageInfo(userPageNumber(page.value, size.value), size.value, applied.value),
    )
    const last = Math.max(1, Math.ceil(result.total / size.value))
    if (page.value > last) {
      page.value = last
      return await loadUsers()
    }
    users.value = result.users
    total.value = result.total
  } catch (error) {
    users.value = []
    failed.value = true
    restricted.value = Boolean(
      error && typeof error === 'object' && 'code' in error && error.code === 403,
    )
  } finally {
    pending.value = false
  }
}
async function loadRoles() {
  rolePending.value = true
  roleFailed.value = false
  try {
    roles.value = userRoles(await getRoleList())
  } catch {
    roles.value = []
    roleFailed.value = true
  } finally {
    rolePending.value = false
  }
}
function search() {
  applied.value = {
    ...(filters.name.trim() ? { name: filters.name.trim() } : {}),
    ...(filters.roleId === null ? {} : { roleId: filters.roleId }),
    ...(filters.status === 'all' ? {} : { status: Number(filters.status) }),
  }
  page.value = 1
  notice.value = ''
  void loadUsers()
}
function reset() {
  Object.assign(filters, { name: '', roleId: null, status: 'all' })
  search()
}
function changePage(value: number) {
  if (!pending.value) {
    page.value = value
    void loadUsers()
  }
}
function changeSize(value: unknown) {
  size.value = Number(value)
  page.value = 1
  void loadUsers()
}
function openEditor(mode: 'view' | 'create' | 'edit', user: ManagedUser | null = null) {
  selected.value = user
  editorMode.value = mode
  editorOpen.value = true
}
async function saved(self: boolean) {
  editorOpen.value = false
  if (self) {
    await session.signOut()
    return
  }
  notice.value = editorMode.value === 'create' ? '用户已创建' : '用户信息已更新'
  if (editorMode.value === 'create') page.value = 1
  await loadUsers()
}
async function deleted() {
  deletion.value = null
  notice.value = '用户已删除'
  await loadUsers()
}
onMounted(() => {
  void loadUsers()
  void loadRoles()
})
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="users-title">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="space-y-2">
        <h1 id="users-title" class="text-2xl font-semibold tracking-tight">用户管理</h1>
        <p class="text-sm text-muted-foreground">管理账户资料、状态与角色分配。</p>
      </div>
      <UiButton :disabled="rolesUnavailable || restricted" @click="openEditor('create')"
        ><HugeiconsIcon
          :icon="Add01Icon"
          class="size-4 -ml-0.5"
          aria-hidden="true"
        />新增用户</UiButton
      >
    </header>
    <UiAlert v-if="notice" role="status"
      ><UiAlertTitle>{{ notice }}</UiAlertTitle></UiAlert
    >
    <UiAlert v-if="roleFailed || (!rolePending && !roles.length)" role="alert">
      <UiAlertTitle>{{ roleFailed ? '角色列表读取失败' : '暂无可分配角色' }}</UiAlertTitle
      ><UiAlertDescription>角色筛选和用户保存暂不可用，请重新读取角色列表。</UiAlertDescription
      ><UiButton variant="outline" size="sm" class="mt-2" :disabled="rolePending" @click="loadRoles"
        >重新读取角色</UiButton
      >
    </UiAlert>

    <form
      class="flex flex-wrap items-end gap-x-6 gap-y-4"
      aria-label="用户筛选"
      @submit.prevent="search"
    >
      <UiField class="w-full sm:w-56"
        ><UiFieldLabel for="users-search">姓名</UiFieldLabel
        ><UiInput
          id="users-search"
          v-model="filters.name"
          placeholder="按姓名搜索"
          :disabled="pending"
          maxlength="50"
      /></UiField>
      <UiField class="w-full sm:w-auto sm:min-w-48"
        ><UiFieldLabel as="span">角色</UiFieldLabel
        ><UsersRolePicker
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
        <UiButton type="submit" :disabled="pending"
          ><HugeiconsIcon
            :icon="Search01Icon"
            class="size-4 -ml-0.5"
            aria-hidden="true"
          />查询</UiButton
        ><UiButton variant="outline" type="button" :disabled="pending" @click="reset"
          >重置</UiButton
        >
      </div>
    </form>

    <div class="min-w-0 rounded-lg border-[0.5px] border-border/70 bg-card" :aria-busy="pending">
      <div class="flex items-center justify-between gap-3 border-b-[0.5px] border-border/70 p-4">
        <h2 class="text-sm font-medium">
          用户列表
          <span class="ml-2 font-normal text-muted-foreground">{{
            failed ? '—' : pending && !users.length ? '读取中' : `共 ${number.format(total)} 位用户`
          }}</span>
        </h2>
        <UiButton variant="ghost" size="sm" :disabled="pending" @click="loadUsers"
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
        <UiButton variant="outline" @click="loadUsers">重试</UiButton>
      </div>
      <div
        v-else-if="pending && !users.length"
        class="flex min-h-72 items-center justify-center text-sm text-muted-foreground"
        role="status"
      >
        正在读取用户列表…
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
        ><UiButton v-else :disabled="rolesUnavailable" @click="openEditor('create')"
          >新增用户</UiButton
        >
      </div>
      <UiTable v-else>
        <UiTableCaption class="sr-only">用户账户、角色、状态与操作</UiTableCaption>
        <UiTableHeader
          ><UiTableRow class="border-b-[0.5px]"
            ><UiTableHead class="px-4">用户</UiTableHead
            ><UiTableHead class="hidden md:table-cell">角色</UiTableHead
            ><UiTableHead class="text-center">状态</UiTableHead
            ><UiTableHead class="hidden text-right xl:table-cell">创建日期</UiTableHead
            ><UiTableHead
              class="sticky right-0 w-32 min-w-32 bg-card text-center shadow-[-4px_0_6px_-6px_var(--border)]"
              >操作</UiTableHead
            ></UiTableRow
          ></UiTableHeader
        >
        <UiTableBody
          ><UiTableRow v-for="user in users" :key="user.id" class="border-b-[0.5px]">
            <UiTableCell class="min-w-40 whitespace-normal px-4"
              ><div class="flex items-center gap-3">
                <UiAvatar class="hidden size-8 shrink-0 sm:flex"
                  ><UiAvatarFallback>{{ user.name.slice(0, 1) || '?' }}</UiAvatarFallback></UiAvatar
                >
                <div class="min-w-0">
                  <UiButton
                    variant="ghost"
                    class="h-auto max-w-48 justify-start whitespace-normal break-words p-0 text-left font-medium hover:bg-transparent"
                    @click="openEditor('view', user)"
                    >{{ user.name || '未命名用户' }}</UiButton
                  >
                  <p class="mt-1 max-w-56 break-all text-xs text-muted-foreground">
                    {{ user.email || '未填写邮箱' }}
                  </p>
                  <p class="mt-1 text-xs text-muted-foreground md:hidden">
                    {{ user.roleName || '未分配角色' }}
                  </p>
                </div>
              </div></UiTableCell
            >
            <UiTableCell class="hidden max-w-48 whitespace-normal break-words md:table-cell">{{
              user.roleName || (user.roleId ? `角色 #${user.roleId}` : '未分配')
            }}</UiTableCell>
            <UiTableCell class="text-center"
              ><UiBadge
                variant="secondary"
                :class="
                  user.status === 1
                    ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                    : 'bg-muted text-muted-foreground'
                "
                >{{ user.status === 1 ? '启用' : user.status === 0 ? '停用' : '未知' }}</UiBadge
              ></UiTableCell
            >
            <UiTableCell
              class="hidden text-right tabular-nums text-muted-foreground xl:table-cell"
              >{{ user.createTime || '—' }}</UiTableCell
            >
            <UiTableCell
              class="sticky right-0 w-32 min-w-32 bg-card text-center shadow-[-4px_0_6px_-6px_var(--border)]"
              ><div class="flex justify-center">
                <UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`查看${user.name || user.id}`"
                  @click="openEditor('view', user)"
                  >查看</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`编辑${user.name || user.id}`"
                  :disabled="rolesUnavailable"
                  @click="openEditor('edit', user)"
                  >编辑</UiButton
                ><UiButton
                  variant="ghost"
                  size="sm"
                  class="text-destructive hover:text-destructive"
                  :aria-label="`删除${user.name || user.id}`"
                  :disabled="user.email === session.state.value.user?.email"
                  @click="deletion = user"
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
            >第 {{ page }} / {{ Math.max(1, Math.ceil(total / size)) }} 页</span
          ><UiPagination
            :page="page"
            :total="total"
            :items-per-page="size"
            :disabled="pending"
            :sibling-count="1"
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
    <UsersUserEditor
      v-model:open="editorOpen"
      :mode="editorMode"
      :user="selected"
      :roles="roles"
      :roles-unavailable="rolesUnavailable"
      @saved="saved"
      @retry-roles="loadRoles"
    />
    <UsersDeleteUserDialog :user="deletion" @close="deletion = null" @deleted="deleted" />
  </section>
</template>
