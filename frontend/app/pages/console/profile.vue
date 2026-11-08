<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import {
  RefreshIcon,
  ArrowRight01Icon,
  Shield01Icon,
  UserAccountIcon,
} from '@hugeicons/core-free-icons'
import { getUserBaseInfo, getUserRole, getUserPrivateInformation } from '@/api/auth/auth'
import { accountSnapshot, accessSnapshot, settingsFailure } from '@/lib/settings'

definePageMeta({ layout: 'console', middleware: 'auth' })
type State = 'loading' | 'ready' | 'error' | 'forbidden'
const profile = shallowRef<ReturnType<typeof accountSnapshot> | null>(null)
const access = shallowRef<ReturnType<typeof accessSnapshot> | null>(null)
const profileState = ref<State>('loading')
const accessState = ref<State>('loading')
const permissionsOpen = ref(false)
const pending = computed(() => profileState.value === 'loading' || accessState.value === 'loading')
const initial = computed(() =>
  Array.from(profile.value?.name.trim() || profile.value?.email || '')[0]?.toLocaleUpperCase(),
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
  permissionsOpen.value = false
  try {
    const [role, permissions] = await Promise.all([getUserRole(), getUserPrivateInformation()])
    access.value = accessSnapshot(role, permissions)
    accessState.value = 'ready'
  } catch (error) {
    accessState.value = settingsFailure(error)
  }
}
async function refresh() {
  await Promise.all([loadProfile(), loadAccess()])
}
onMounted(refresh)
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="profile-title">
    <header class="flex flex-wrap items-start justify-between gap-4">
      <div class="space-y-2">
        <h1 id="profile-title" class="text-2xl font-semibold tracking-tight">个人中心</h1>
        <p class="text-sm text-muted-foreground">查看你的账号资料与当前访问权限。</p>
      </div>
      <UiButton variant="outline" :disabled="pending" @click="refresh"
        ><HugeiconsIcon
          :icon="RefreshIcon"
          class="size-4 -ml-0.5"
          :class="{ 'motion-safe:animate-spin': pending }"
          aria-hidden="true"
        />{{ pending ? '正在刷新…' : '刷新资料' }}</UiButton
      >
    </header>
    <div class="grid min-w-0 items-start gap-6 xl:grid-cols-[280px_minmax(0,1fr)]">
      <UiCard
        class="gap-0 border-[0.5px] border-border/70 py-0 shadow-none ring-0"
        :aria-busy="profileState === 'loading'"
      >
        <UiCardContent class="p-6">
          <div v-if="profileState === 'loading'" role="status" class="min-h-40 space-y-4">
            <span class="sr-only">正在读取个人资料</span
            ><UiSkeleton class="size-16 rounded-lg" /><UiSkeleton class="h-6 w-36" /><UiSkeleton
              class="h-4 w-52 max-w-full"
            />
          </div>
          <UiAlert v-else-if="profileState !== 'ready'" role="alert"
            ><UiAlertTitle>{{
              profileState === 'forbidden' ? '当前账号无权读取个人资料' : '个人资料读取失败'
            }}</UiAlertTitle
            ><UiAlertDescription>{{
              profileState === 'forbidden' ? '请联系管理员确认访问权限。' : '请检查服务连接后重试。'
            }}</UiAlertDescription
            ><UiButton variant="outline" size="sm" class="mt-3" @click="loadProfile"
              >重试个人资料</UiButton
            ></UiAlert
          >
          <div
            v-else-if="profile"
            class="flex min-w-0 items-center gap-5 xl:flex-col xl:items-start"
          >
            <UiAvatar class="size-16 shrink-0 rounded-lg" aria-hidden="true"
              ><UiAvatarFallback
                class="rounded-lg bg-primary/10 text-2xl font-medium text-primary"
                >{{ initial }}</UiAvatarFallback
              ></UiAvatar
            >
            <div class="min-w-0 space-y-2">
              <p class="text-xs text-muted-foreground">当前账号</p>
              <h2 class="break-words text-lg font-semibold">{{ profile.name || '未设置名称' }}</h2>
              <p class="break-all text-sm text-muted-foreground">{{ profile.email }}</p>
            </div>
          </div>
        </UiCardContent>
      </UiCard>
      <div class="min-w-0 space-y-6">
        <UiCard
          class="gap-0 border-[0.5px] border-border/70 py-0 shadow-none ring-0"
          :aria-busy="profileState === 'loading'"
        >
          <UiCardHeader class="border-b-[0.5px] border-border/70 px-6 py-4"
            ><h2 class="flex items-center gap-2 text-sm font-medium">
              <HugeiconsIcon
                :icon="UserAccountIcon"
                class="size-4 text-muted-foreground"
                aria-hidden="true"
              />基本资料
            </h2></UiCardHeader
          >
          <UiCardContent class="p-6">
            <div v-if="profileState === 'loading'" class="min-h-28 space-y-4" aria-hidden="true">
              <UiSkeleton class="h-4 w-24" /><UiSkeleton class="h-4 w-3/4" /><UiSkeleton
                class="h-4 w-40"
              />
            </div>
            <dl v-else-if="profile" class="grid gap-x-8 gap-y-6 text-sm sm:grid-cols-2">
              <div>
                <dt class="text-xs text-muted-foreground">显示名称</dt>
                <dd class="mt-1.5 break-words">{{ profile.name || '未设置' }}</dd>
              </div>
              <div>
                <dt class="text-xs text-muted-foreground">邮箱</dt>
                <dd class="mt-1.5 break-all">{{ profile.email }}</dd>
              </div>
              <div>
                <dt class="text-xs text-muted-foreground">账号 ID</dt>
                <dd class="mt-1.5 tabular-nums">{{ profile.id }}</dd>
              </div>
              <div>
                <dt class="text-xs text-muted-foreground">关联角色 ID</dt>
                <dd class="mt-1.5 tabular-nums">{{ profile.roleId ?? '—' }}</dd>
              </div>
            </dl>
            <p v-else class="min-h-28 text-sm text-muted-foreground">
              个人资料暂不可用，请在账号卡片中重新读取。
            </p>
          </UiCardContent>
          <UiCardFooter class="border-t-[0.5px] border-border/70 px-6 py-4"
            ><p class="text-xs leading-5 text-muted-foreground">
              如需调整账号资料，请联系管理员。
            </p></UiCardFooter
          >
        </UiCard>
        <UiCard
          class="gap-0 border-[0.5px] border-border/70 py-0 shadow-none ring-0"
          :aria-busy="accessState === 'loading'"
        >
          <UiCardHeader class="border-b-[0.5px] border-border/70 px-6 py-4"
            ><h2 class="flex items-center gap-2 text-sm font-medium">
              <HugeiconsIcon
                :icon="Shield01Icon"
                class="size-4 text-muted-foreground"
                aria-hidden="true"
              />角色与访问
            </h2></UiCardHeader
          >
          <UiCardContent class="p-6">
            <div v-if="accessState === 'loading'" role="status" class="min-h-36 space-y-4">
              <span class="sr-only">正在读取角色与访问权限</span
              ><UiSkeleton class="h-5 w-36" /><UiSkeleton class="h-4 w-3/4" /><UiSkeleton
                class="h-4 w-24"
              />
            </div>
            <UiAlert v-else-if="accessState !== 'ready'" role="alert"
              ><UiAlertTitle>{{
                accessState === 'forbidden'
                  ? '当前账号无权读取角色与访问权限'
                  : '角色与访问权限读取失败'
              }}</UiAlertTitle
              ><UiAlertDescription>请重新读取授权，或联系管理员确认访问权限。</UiAlertDescription
              ><UiButton variant="outline" size="sm" class="mt-3" @click="loadAccess"
                >重试角色与权限</UiButton
              ></UiAlert
            >
            <div v-else-if="access" class="space-y-5">
              <div class="flex flex-wrap items-start justify-between gap-3">
                <div class="min-w-0 space-y-1.5">
                  <h3 class="break-all text-sm font-semibold">{{ access.name }}</h3>
                  <p class="text-xs tabular-nums text-muted-foreground">角色 ID {{ access.id }}</p>
                </div>
                <UiBadge
                  variant="secondary"
                  :class="
                    access.status === 1
                      ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                      : 'bg-muted text-muted-foreground'
                  "
                  >{{
                    access.status === 1 ? '启用' : access.status === 0 ? '停用' : '状态未知'
                  }}</UiBadge
                >
              </div>
              <p
                v-if="access.remark"
                class="whitespace-pre-wrap break-words text-sm leading-6 text-muted-foreground"
              >
                {{ access.remark }}
              </p>
              <UiCollapsible
                v-if="access.permissions.length"
                v-model:open="permissionsOpen"
                class="border-t-[0.5px] border-border/70 pt-4"
                ><div class="flex flex-wrap items-center justify-between gap-3">
                  <p class="text-sm">
                    已授予
                    <span class="font-medium tabular-nums">{{ access.permissions.length }}</span>
                    项权限
                  </p>
                  <UiCollapsibleTrigger as-child
                    ><UiButton variant="ghost" size="sm"
                      ><HugeiconsIcon
                        :icon="ArrowRight01Icon"
                        class="size-4"
                        :class="{ 'rotate-90': permissionsOpen }"
                        aria-hidden="true"
                      />{{ permissionsOpen ? '收起权限明细' : '查看权限明细' }}</UiButton
                    ></UiCollapsibleTrigger
                  >
                </div>
                <UiCollapsibleContent
                  ><ul
                    aria-label="个人权限明细"
                    tabindex="0"
                    class="mt-3 max-h-56 space-y-2 overflow-y-auto rounded-md bg-muted/30 p-3 focus-visible:outline-2 focus-visible:outline-ring"
                  >
                    <li
                      v-for="permission in access.permissions"
                      :key="permission"
                      class="break-all font-mono text-xs leading-5 text-muted-foreground"
                    >
                      {{ permission }}
                    </li>
                  </ul></UiCollapsibleContent
                ></UiCollapsible
              >
              <p
                v-else
                role="status"
                class="rounded-md bg-muted/30 p-4 text-sm leading-6 text-muted-foreground"
              >
                当前角色尚未分配权限。如需访问业务功能，请联系管理员。
              </p>
            </div>
          </UiCardContent>
        </UiCard>
      </div>
    </div>
  </section>
</template>
