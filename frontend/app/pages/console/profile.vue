<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import {
  RefreshIcon,
  ArrowRight01Icon,
  Shield01Icon,
  UserAccountIcon,
} from '@hugeicons/core-free-icons'
import { getUserBaseInfo, getUserRole, getUserPrivateInformation } from '@/api/auth/auth'
import { getUserRoutes } from '@/api/router/router'
import { accountSnapshot, accessSnapshot, permissionLabels, roleStatus, settingsFailure } from '@/lib/settings'

definePageMeta({ layout: 'console', middleware: 'auth' })
type State = 'loading' | 'ready' | 'error' | 'forbidden'
const profile = shallowRef<ReturnType<typeof accountSnapshot> | null>(null)
const access = shallowRef<ReturnType<typeof accessSnapshot> | null>(null)
const profileState = ref<State>('loading')
const accessState = ref<State>('loading')
const permissionsOpen = ref(false)
const loadingLabels = ref(false)
const namesByPermission = shallowRef(new Map<string, string>())
const currentRole = computed(() => roleStatus(access.value?.status ?? null))
const pending = computed(() =>
  profileState.value === 'loading' || accessState.value === 'loading' || loadingLabels.value,
)
const initial = computed(() =>
  Array.from(profile.value?.name.trim() || profile.value?.email || '')[0]?.toLocaleUpperCase() ?? '?',
)

async function loadProfile() {
  profileState.value = 'loading'
  profile.value = null
  try {
    profile.value = accountSnapshot(await getUserBaseInfo())
    profileState.value = 'ready'
  } catch (error) {
    profileState.value = settingsFailure(error)
  }
}

async function loadAccess() {
  accessState.value = 'loading'
  access.value = null
  namesByPermission.value = new Map()
  permissionsOpen.value = false
  try {
    const [role, permissions] = await Promise.all([getUserRole(), getUserPrivateInformation()])
    access.value = accessSnapshot(role, permissions)
    accessState.value = 'ready'
  } catch (error) {
    accessState.value = settingsFailure(error)
  }

  const current = access.value
  if (!current?.permissions.length) return
  loadingLabels.value = true
  try {
    // The session's navigation projection omits operation nodes and permission codes.
    namesByPermission.value = permissionLabels(await getUserRoutes(), current.permissions)
  } catch {
    // Permission codes stay available even when route labels cannot be loaded.
  } finally {
    loadingLabels.value = false
  }
}

function loadOverview() {
  return Promise.all([loadProfile(), loadAccess()])
}

function refresh() {
  if (pending.value) return
  return loadOverview()
}

onMounted(loadOverview)
</script>

<template>
  <section class="flex w-full min-w-0 max-w-3xl flex-col gap-6" aria-labelledby="profile-title">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="space-y-2">
        <h1 id="profile-title" class="text-2xl font-semibold tracking-tight">个人中心</h1>
        <p class="text-sm text-muted-foreground">修改资料或密码请联系系统管理员。</p>
      </div>
      <UiButton variant="outline" :aria-disabled="pending" @click="refresh">
        <HugeiconsIcon
          :icon="RefreshIcon"
          class="size-4"
          :class="{ 'motion-safe:animate-spin': pending }"
          aria-hidden="true"
        />
        {{ pending ? '正在刷新…' : '刷新资料' }}
      </UiButton>
    </header>

    <UiCard :aria-busy="profileState === 'loading'">
      <UiCardHeader>
        <h2 class="flex items-center gap-2 text-base font-semibold">
          <HugeiconsIcon :icon="UserAccountIcon" class="size-4 text-muted-foreground" aria-hidden="true" />
          账号资料
        </h2>
      </UiCardHeader>
      <UiCardContent>
        <div v-if="profileState === 'loading'" role="status" class="min-h-28 space-y-3">
          <span class="sr-only">正在读取个人资料</span>
          <UiSkeleton class="size-12 rounded-lg" />
          <UiSkeleton class="h-5 w-36" />
          <UiSkeleton class="h-4 w-48 max-w-full" />
        </div>
        <UiAlert v-else-if="profileState !== 'ready'" variant="destructive" role="alert">
          <UiAlertTitle>
            {{ profileState === 'forbidden' ? '当前账号无权读取个人资料' : '个人资料读取失败' }}
          </UiAlertTitle>
          <UiAlertDescription>
            {{ profileState === 'forbidden' ? '请联系管理员确认访问权限。' : '请检查服务连接后重试。' }}
          </UiAlertDescription>
          <UiButton variant="outline" size="sm" class="mt-3" @click="loadProfile">重试个人资料</UiButton>
        </UiAlert>
        <div v-else-if="profile" class="space-y-4">
          <div class="flex min-w-0 items-center gap-4">
            <UiAvatar class="size-12 shrink-0 rounded-lg" aria-hidden="true">
              <UiAvatarFallback class="rounded-lg bg-accent-data-surface text-lg font-semibold text-accent-data-foreground">
                {{ initial }}
              </UiAvatarFallback>
            </UiAvatar>
            <div class="min-w-0">
              <p class="break-words text-lg font-semibold">{{ profile.name.trim() || '未设置显示名称' }}</p>
              <p class="break-all text-sm text-muted-foreground">
                <span class="sr-only">邮箱 </span>{{ profile.email }}
              </p>
            </div>
          </div>
          <dl class="grid grid-cols-2 gap-4 border-t border-border pt-4 text-sm">
            <div>
              <dt class="text-xs text-muted-foreground">账号 ID</dt>
              <dd class="mt-1 font-mono tabular-nums">{{ profile.id }}</dd>
            </div>
            <div>
              <dt class="text-xs text-muted-foreground">关联角色 ID</dt>
              <dd class="mt-1 font-mono tabular-nums">{{ profile.roleId ?? '—' }}</dd>
            </div>
          </dl>
        </div>
      </UiCardContent>
    </UiCard>

    <UiCard :aria-busy="accessState === 'loading'">
      <UiCardHeader>
        <h2 class="flex items-center gap-2 text-base font-semibold">
          <HugeiconsIcon :icon="Shield01Icon" class="size-4 text-muted-foreground" aria-hidden="true" />
          角色与访问权限
        </h2>
      </UiCardHeader>
      <UiCardContent>
        <div v-if="accessState === 'loading'" role="status" class="min-h-28 space-y-3">
          <span class="sr-only">正在读取角色与访问权限</span>
          <UiSkeleton class="h-5 w-36" />
          <UiSkeleton class="h-4 w-3/4" />
          <UiSkeleton class="h-4 w-24" />
        </div>
        <UiAlert v-else-if="accessState !== 'ready'" variant="destructive" role="alert">
          <UiAlertTitle>
            {{ accessState === 'forbidden' ? '当前账号无权读取角色与访问权限' : '角色与访问权限读取失败' }}
          </UiAlertTitle>
          <UiAlertDescription>请重新读取授权，或联系管理员确认访问权限。</UiAlertDescription>
          <UiButton variant="outline" size="sm" class="mt-3" @click="loadAccess">重试角色与权限</UiButton>
        </UiAlert>
        <div v-else-if="access" class="space-y-4">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div class="min-w-0 space-y-1">
              <h3 class="break-all text-sm font-semibold">{{ access.name }}</h3>
              <p class="text-xs text-muted-foreground">
                角色 ID <span class="font-mono tabular-nums">{{ access.id }}</span>
              </p>
            </div>
            <StatusBadge :tone="currentRole.tone">{{ currentRole.label }}</StatusBadge>
          </div>
          <p class="whitespace-pre-wrap break-words text-sm leading-6 text-muted-foreground">
            {{ access.remark || '暂无角色备注' }}
          </p>
          <UiCollapsible
            v-if="access.permissions.length"
            v-model:open="permissionsOpen"
            class="border-t border-border pt-3"
          >
            <div class="flex flex-wrap items-center justify-between gap-3">
              <p class="text-sm">
                已授予 <span class="font-mono font-medium tabular-nums">{{ access.permissions.length }}</span> 项权限
              </p>
              <UiCollapsibleTrigger as-child>
                <UiButton variant="ghost" size="sm">
                  <HugeiconsIcon
                    :icon="ArrowRight01Icon"
                    class="size-4"
                    :class="{ 'rotate-90': permissionsOpen }"
                    aria-hidden="true"
                  />
                  {{ permissionsOpen ? '收起权限明细' : '查看权限明细' }}
                </UiButton>
              </UiCollapsibleTrigger>
            </div>
            <UiCollapsibleContent>
              <p v-if="loadingLabels" role="status" class="mt-2 text-xs text-muted-foreground">正在匹配权限名称…</p>
              <ul
                aria-label="个人权限明细"
                tabindex="0"
                class="mt-2 max-h-56 divide-y divide-border overflow-y-auto rounded-md bg-muted/30 px-3 focus-visible:outline-2 focus-visible:outline-ring"
              >
                <li
                  v-for="permission in access.permissions"
                  :key="permission"
                  class="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 py-2"
                >
                  <span
                    v-if="!loadingLabels"
                    class="min-w-0 break-words text-sm"
                    :class="{ 'text-muted-foreground': !namesByPermission.has(permission) }"
                  >
                    {{ namesByPermission.get(permission) ?? '未匹配到菜单或操作' }}
                  </span>
                  <code class="min-w-0 break-all font-mono text-xs text-muted-foreground">{{ permission }}</code>
                </li>
              </ul>
            </UiCollapsibleContent>
          </UiCollapsible>
          <p v-else role="status" class="rounded-md bg-muted/30 p-4 text-sm text-muted-foreground">
            尚未分配权限（<span class="font-mono tabular-nums">0</span> 项）。
          </p>
        </div>
      </UiCardContent>
    </UiCard>
  </section>
</template>
