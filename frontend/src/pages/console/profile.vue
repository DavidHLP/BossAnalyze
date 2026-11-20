<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import { HugeiconsIcon } from '@hugeicons/vue'
import {
  RefreshIcon,
  ArrowRight01Icon,
  InformationCircleIcon,
  LockKeyIcon,
} from '@hugeicons/core-free-icons'
import { getUserBaseInfo, getUserRole, getUserPrivateInformation } from '@/api/auth/auth'
import { getUserRoutes } from '@/api/router/router'
import { accountSnapshot, accessSnapshot, permissionLabels, roleStatus, settingsFailure } from '@/lib/settings'

type State = 'loading' | 'ready' | 'error' | 'forbidden'
const profile = shallowRef<ReturnType<typeof accountSnapshot> | null>(null)
const access = shallowRef<ReturnType<typeof accessSnapshot> | null>(null)
const profileState = ref<State>('loading')
const accessState = ref<State>('loading')
const permissionsOpen = ref(true)
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
  <section class="flex min-w-0 flex-col gap-5" aria-labelledby="profile-title">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="space-y-2">
        <h1 id="profile-title" class="text-3xl font-semibold tracking-tight">个人中心</h1>
        <p class="text-sm text-muted-foreground">查看本人账号、关联角色与访问权限。</p>
      </div>
      <UiButton :aria-disabled="pending" @click="refresh">
        <HugeiconsIcon
          :icon="RefreshIcon"
          class="size-4"
          :class="{ 'motion-safe:animate-spin': pending }"
          aria-hidden="true"
        />
        {{ pending ? '正在刷新…' : '刷新' }}
      </UiButton>
    </header>

    <UiCard class="min-w-0 gap-4 py-4" :aria-busy="profileState === 'loading'">
      <UiCardHeader class="flex flex-wrap items-center justify-between gap-3">
        <h2 class="text-lg font-semibold">账号摘要与基本资料</h2>
        <UiBadge variant="secondary">
          <HugeiconsIcon :icon="LockKeyIcon" class="size-3" aria-hidden="true" />
          只读
        </UiBadge>
      </UiCardHeader>
      <UiCardContent>
        <div v-if="profileState === 'loading'" role="status" class="min-h-24 space-y-3 border-t pt-4">
          <span class="sr-only">正在读取个人资料</span>
          <UiSkeleton class="size-14 rounded-full" />
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
        <div v-else-if="profile" class="space-y-4 border-t pt-4">
          <div class="grid min-w-0 gap-4 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1.85fr)]">
            <div class="flex min-w-0 items-center gap-4">
              <UiAvatar class="size-14 shrink-0" aria-hidden="true">
                <UiAvatarFallback class="bg-muted text-xl font-semibold text-foreground">
                  {{ initial }}
                </UiAvatarFallback>
              </UiAvatar>
              <div class="min-w-0 space-y-1">
                <p class="break-words text-lg font-semibold">{{ profile.name.trim() || '未设置显示名称' }}</p>
                <p class="break-all font-mono text-sm text-muted-foreground">
                  <span class="sr-only">邮箱 </span>{{ profile.email }}
                </p>
              </div>
            </div>
            <dl class="grid grid-cols-2 divide-x border-t pt-3 lg:border-l lg:border-t-0 lg:pt-0">
              <div class="min-w-0 pr-3 lg:px-6">
                <dt class="text-xs text-muted-foreground">用户 ID</dt>
                <dd class="mt-1 break-all font-mono text-xl font-semibold tabular-nums">{{ profile.id }}</dd>
              </div>
              <div class="min-w-0 pl-4 lg:pl-6">
                <dt class="text-xs text-muted-foreground">关联角色 ID</dt>
                <dd class="mt-1 break-all font-mono text-xl font-semibold tabular-nums">{{ profile.roleId ?? '—' }}</dd>
              </div>
            </dl>
          </div>
          <p class="flex gap-2 rounded-md border bg-muted/50 px-3 py-2 text-sm text-muted-foreground">
            <HugeiconsIcon :icon="InformationCircleIcon" class="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            当前页面仅支持只读查看，修改资料或密码请联系系统管理员。
          </p>
        </div>
      </UiCardContent>
    </UiCard>

    <UiCard class="min-w-0 gap-4 py-4" :aria-busy="accessState === 'loading'">
      <UiCardHeader>
        <h2 class="text-lg font-semibold">角色与访问权限</h2>
      </UiCardHeader>
      <UiCardContent>
        <div v-if="accessState === 'loading'" role="status" class="min-h-24 space-y-3 border-t pt-4">
          <span class="sr-only">正在读取角色与访问权限</span>
          <UiSkeleton class="h-5 w-36" />
          <UiSkeleton class="h-4 w-3/4" />
          <UiSkeleton class="h-4 w-24" />
        </div>
        <UiAlert v-else-if="accessState !== 'ready'" variant="destructive" role="alert">
          <UiAlertTitle>
            {{ accessState === 'forbidden' ? '当前账号无权读取角色与访问权限' : '角色与访问权限读取失败' }}
          </UiAlertTitle>
          <UiAlertDescription>
            <template v-if="profileState === 'ready'">账号资料仍可查看。</template>
            请重新读取授权，或联系管理员确认访问权限。
          </UiAlertDescription>
          <UiButton variant="outline" size="sm" class="mt-3" @click="loadAccess">重试角色与权限</UiButton>
        </UiAlert>
        <div v-else-if="access" class="space-y-4 border-t pt-4">
          <div class="grid min-w-0 gap-4 sm:grid-cols-[minmax(0,2fr)_minmax(0,1fr)_minmax(0,1fr)]">
            <div class="min-w-0 space-y-2">
              <div class="flex flex-wrap items-center gap-3">
                <h3 class="min-w-0 break-all font-mono text-2xl font-semibold">{{ access.name }}</h3>
                <StatusBadge :tone="currentRole.tone">{{ currentRole.label }}</StatusBadge>
              </div>
              <p class="whitespace-pre-wrap break-words text-sm text-muted-foreground">{{ access.remark || '暂无角色备注' }}</p>
            </div>
            <dl class="min-w-0 sm:border-l sm:pl-5">
              <dt class="text-xs text-muted-foreground">角色 ID</dt>
              <dd class="mt-1 break-all font-mono text-xl font-semibold tabular-nums">{{ access.id }}</dd>
            </dl>
            <p class="text-sm font-medium sm:border-l sm:pl-5">
              <span class="mr-1 font-mono text-2xl font-semibold tabular-nums">{{ access.permissions.length }}</span>
              项权限
            </p>
          </div>
          <UiCollapsible
            v-if="access.permissions.length"
            v-model:open="permissionsOpen"
            class="border-t pt-3"
          >
            <UiCollapsibleTrigger as-child>
              <UiButton variant="ghost" size="sm" class="max-w-full justify-start px-0">
                <HugeiconsIcon
                  :icon="ArrowRight01Icon"
                  class="size-4"
                  :class="{ 'rotate-90': permissionsOpen }"
                  aria-hidden="true"
                />
                权限清单<span class="sr-only">，{{ permissionsOpen ? '收起' : '展开' }}</span>
              </UiButton>
            </UiCollapsibleTrigger>
            <span class="ml-2 text-xs text-muted-foreground">去重并按权限字符串排序</span>
            <UiCollapsibleContent>
              <p v-if="loadingLabels" role="status" class="mt-2 text-xs text-muted-foreground">正在匹配权限名称…</p>
              <ul
                aria-label="个人权限明细"
                tabindex="0"
                class="mt-2 grid max-h-56 grid-cols-1 gap-2 overflow-y-auto rounded-md border bg-muted/50 p-3 focus-visible:outline-2 focus-visible:outline-ring sm:grid-cols-2 lg:grid-cols-3"
              >
                <li
                  v-for="permission in access.permissions"
                  :key="permission"
                  class="min-w-0 space-y-1"
                >
                  <code class="inline-block max-w-full break-all rounded-md bg-muted px-2 py-1 font-mono text-xs text-muted-foreground">{{ permission }}</code>
                  <p v-if="namesByPermission.has(permission)" class="break-words px-2 text-xs text-muted-foreground">
                    {{ namesByPermission.get(permission) }}
                  </p>
                </li>
              </ul>
            </UiCollapsibleContent>
          </UiCollapsible>
          <p v-else role="status" class="rounded-md bg-muted/50 p-4 text-sm text-muted-foreground">
            尚未分配权限。
          </p>
        </div>
      </UiCardContent>
    </UiCard>
  </section>
</template>
