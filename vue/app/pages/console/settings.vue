<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import {
  RefreshIcon,
  UserAccountIcon,
  Shield01Icon,
  ServerStack01Icon,
} from '@hugeicons/core-free-icons'
import { getUserBaseInfo, getUserRole, getUserPrivateInformation } from '@/api/auth/auth'
import { checkServiceHealth } from '@/api/log/accesslog'
import { accountSnapshot, accessSnapshot, healthSnapshot, settingsFailure } from '@/lib/settings'

definePageMeta({ layout: 'console', middleware: 'auth' })
const route = useRoute()
type State = 'loading' | 'ready' | 'error' | 'forbidden'
const account = shallowRef<ReturnType<typeof accountSnapshot> | null>(null)
const access = shallowRef<ReturnType<typeof accessSnapshot> | null>(null)
const health = shallowRef<ReturnType<typeof healthSnapshot> | null>(null)
const accountState = ref<State>('loading')
const accessState = ref<State>('loading')
const healthState = ref<State>('loading')
const checkedAt = computed(() =>
  health.value
    ? new Intl.DateTimeFormat('zh-CN', {
        dateStyle: 'medium',
        timeStyle: 'medium',
      }).format(health.value.checkedAt)
    : '—',
)
async function loadAccount() {
  accountState.value = 'loading'
  account.value = null
  try {
    account.value = accountSnapshot(await getUserBaseInfo())
    accountState.value = 'ready'
  } catch (error) {
    accountState.value = settingsFailure(error)
  }
}
async function loadAccess() {
  accessState.value = 'loading'
  access.value = null
  try {
    const [role, permissions] = await Promise.all([getUserRole(), getUserPrivateInformation()])
    access.value = accessSnapshot(role, permissions)
    accessState.value = 'ready'
  } catch (error) {
    accessState.value = settingsFailure(error)
  }
}
async function loadHealth() {
  healthState.value = 'loading'
  health.value = null
  try {
    health.value = healthSnapshot(await checkServiceHealth())
    healthState.value = 'ready'
  } catch (error) {
    healthState.value = settingsFailure(error)
  }
}
onMounted(() => {
  void loadAccount()
  void loadAccess()
  void loadHealth()
})
</script>

<template>
  <section class="flex min-w-0 flex-col gap-8" aria-labelledby="settings-title">
    <header class="space-y-2">
      <h1 id="settings-title" class="text-2xl font-semibold tracking-tight">设置</h1>
      <p class="text-sm text-muted-foreground">查看当前账号的访问范围与服务连接状态。</p>
    </header>
    <div class="grid min-w-0 gap-6 lg:grid-cols-[180px_minmax(0,1fr)] lg:gap-10">
      <nav aria-label="设置分类" class="flex gap-1 self-start lg:sticky lg:top-6 lg:flex-col">
        <UiButton variant="ghost" class="justify-start" as-child
          ><a
            href="#account-access"
            :aria-current="route.hash !== '#service-status' ? 'location' : undefined"
            class="aria-[current=location]:bg-muted"
            ><HugeiconsIcon
              :icon="UserAccountIcon"
              class="size-4"
              aria-hidden="true"
            />账号与访问</a
          ></UiButton
        >
        <UiButton variant="ghost" class="justify-start" as-child
          ><a
            href="#service-status"
            :aria-current="route.hash === '#service-status' ? 'location' : undefined"
            class="aria-[current=location]:bg-muted"
            ><HugeiconsIcon
              :icon="ServerStack01Icon"
              class="size-4"
              aria-hidden="true"
            />服务状态</a
          ></UiButton
        >
      </nav>
      <div class="min-w-0 space-y-8">
        <section
          id="account-access"
          class="scroll-mt-6 space-y-5"
          aria-labelledby="account-access-title"
        >
          <div class="space-y-1.5">
            <h2 id="account-access-title" class="text-base font-semibold">账号与访问</h2>
            <p class="text-xs text-muted-foreground">账号资料与角色授权由管理员维护。</p>
          </div>
          <UiCard
            class="gap-0 border-[0.5px] border-border/70 py-0 shadow-none ring-0"
            :aria-busy="accountState === 'loading'"
          >
            <UiCardHeader
              class="flex flex-row items-center justify-between gap-3 border-b-[0.5px] border-border/70 px-5 py-4"
              ><UiCardTitle class="flex items-center gap-2 text-sm"
                ><HugeiconsIcon
                  :icon="UserAccountIcon"
                  class="size-4 text-muted-foreground"
                  aria-hidden="true"
                />当前账号</UiCardTitle
              ><UiButton
                variant="ghost"
                size="sm"
                aria-label="刷新账号资料"
                :disabled="accountState === 'loading'"
                @click="loadAccount"
                ><HugeiconsIcon
                  :icon="RefreshIcon"
                  class="size-4"
                  :class="{ 'motion-safe:animate-spin': accountState === 'loading' }"
                  aria-hidden="true"
                />刷新</UiButton
              ></UiCardHeader
            >
            <UiCardContent class="p-5">
              <div v-if="accountState === 'loading'" role="status" class="min-h-28 space-y-4">
                <span class="sr-only">正在读取账号资料</span
                ><UiSkeleton class="h-4 w-24" /><UiSkeleton
                  class="h-4 w-56 max-w-full"
                /><UiSkeleton class="h-4 w-16" />
              </div>
              <UiAlert v-else-if="accountState !== 'ready'" role="alert"
                ><UiAlertTitle>{{
                  accountState === 'forbidden' ? '当前账号无权读取账号资料' : '账号资料读取失败'
                }}</UiAlertTitle
                ><UiAlertDescription>{{
                  accountState === 'forbidden'
                    ? '请联系管理员确认访问权限。'
                    : '请检查服务连接后重试。'
                }}</UiAlertDescription
                ><UiButton variant="outline" size="sm" class="mt-3" @click="loadAccount"
                  >重试账号资料</UiButton
                ></UiAlert
              >
              <dl v-else-if="account" class="grid gap-x-8 gap-y-5 text-sm sm:grid-cols-2">
                <div>
                  <dt class="text-xs text-muted-foreground">显示名称</dt>
                  <dd class="mt-1.5 break-words font-medium">{{ account.name || '未设置' }}</dd>
                </div>
                <div>
                  <dt class="text-xs text-muted-foreground">邮箱</dt>
                  <dd class="mt-1.5 break-all">{{ account.email }}</dd>
                </div>
                <div>
                  <dt class="text-xs text-muted-foreground">账号 ID</dt>
                  <dd class="mt-1.5 tabular-nums">{{ account.id }}</dd>
                </div>
                <div>
                  <dt class="text-xs text-muted-foreground">关联角色 ID</dt>
                  <dd class="mt-1.5 tabular-nums">{{ account.roleId ?? '—' }}</dd>
                </div>
              </dl>
            </UiCardContent>
          </UiCard>
          <UiCard
            class="gap-0 border-[0.5px] border-border/70 py-0 shadow-none ring-0"
            :aria-busy="accessState === 'loading'"
          >
            <UiCardHeader
              class="flex flex-row items-center justify-between gap-3 border-b-[0.5px] border-border/70 px-5 py-4"
              ><UiCardTitle class="flex items-center gap-2 text-sm"
                ><HugeiconsIcon
                  :icon="Shield01Icon"
                  class="size-4 text-muted-foreground"
                  aria-hidden="true"
                />角色与权限</UiCardTitle
              ><UiButton
                variant="ghost"
                size="sm"
                aria-label="刷新角色与权限"
                :disabled="accessState === 'loading'"
                @click="loadAccess"
                ><HugeiconsIcon
                  :icon="RefreshIcon"
                  class="size-4"
                  :class="{ 'motion-safe:animate-spin': accessState === 'loading' }"
                  aria-hidden="true"
                />刷新</UiButton
              ></UiCardHeader
            >
            <UiCardContent class="space-y-5 p-5">
              <div v-if="accessState === 'loading'" role="status" class="min-h-36 space-y-4">
                <span class="sr-only">正在读取角色与权限</span
                ><UiSkeleton class="h-4 w-40" /><UiSkeleton class="h-4 w-full" /><UiSkeleton
                  class="h-4 w-3/4"
                />
              </div>
              <UiAlert v-else-if="accessState !== 'ready'" role="alert"
                ><UiAlertTitle>{{
                  accessState === 'forbidden' ? '当前账号无权读取角色与权限' : '角色与权限读取失败'
                }}</UiAlertTitle
                ><UiAlertDescription>{{
                  accessState === 'forbidden'
                    ? '请联系管理员确认访问权限。'
                    : '请重试读取角色与权限。'
                }}</UiAlertDescription
                ><UiButton variant="outline" size="sm" class="mt-3" @click="loadAccess"
                  >重试角色与权限</UiButton
                ></UiAlert
              >
              <template v-else-if="access">
                <dl class="grid gap-5 text-sm sm:grid-cols-2">
                  <div>
                    <dt class="text-xs text-muted-foreground">当前角色</dt>
                    <dd class="mt-1.5 break-all font-medium">{{ access.name }}</dd>
                  </div>
                  <div>
                    <dt class="text-xs text-muted-foreground">角色状态</dt>
                    <dd class="mt-1.5">
                      <UiBadge
                        variant="secondary"
                        :class="
                          access.status === 1
                            ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                            : 'bg-muted text-muted-foreground'
                        "
                        >{{
                          access.status === 1 ? '启用' : access.status === 0 ? '停用' : '未知'
                        }}</UiBadge
                      >
                    </dd>
                  </div>
                  <div v-if="access.remark" class="sm:col-span-2">
                    <dt class="text-xs text-muted-foreground">角色说明</dt>
                    <dd class="mt-1.5 whitespace-pre-wrap break-words text-muted-foreground">
                      {{ access.remark }}
                    </dd>
                  </div>
                </dl>
                <div class="border-t-[0.5px] border-border/70 pt-4">
                  <h3 class="text-xs font-medium">
                    已授予权限
                    <span class="ml-1 text-muted-foreground"
                      >{{ access.permissions.length }} 项</span
                    >
                  </h3>
                  <ul
                    v-if="access.permissions.length"
                    aria-label="当前账号权限"
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
                  </ul>
                  <p
                    v-else
                    role="status"
                    class="mt-3 rounded-md bg-muted/30 p-4 text-sm text-muted-foreground"
                  >
                    当前角色尚未分配权限。如需访问业务功能，请联系管理员。
                  </p>
                </div>
              </template>
            </UiCardContent>
          </UiCard>
        </section>
        <section
          id="service-status"
          class="scroll-mt-6 space-y-5"
          aria-labelledby="service-status-title"
        >
          <div class="space-y-1.5">
            <h2 id="service-status-title" class="text-base font-semibold">服务状态</h2>
            <p class="text-xs text-muted-foreground">手动检查日志分析服务的连接情况。</p>
          </div>
          <UiCard
            class="gap-0 border-[0.5px] border-border/70 py-0 shadow-none ring-0"
            :aria-busy="healthState === 'loading'"
          >
            <UiCardHeader
              class="flex flex-row items-center justify-between gap-3 border-b-[0.5px] border-border/70 px-5 py-4"
              ><UiCardTitle class="flex items-center gap-2 text-sm"
                ><HugeiconsIcon
                  :icon="ServerStack01Icon"
                  class="size-4 text-muted-foreground"
                  aria-hidden="true"
                />日志服务连通性</UiCardTitle
              ><UiButton
                variant="outline"
                size="sm"
                :disabled="healthState === 'loading'"
                @click="loadHealth"
                ><HugeiconsIcon
                  :icon="RefreshIcon"
                  class="size-4"
                  :class="{ 'motion-safe:animate-spin': healthState === 'loading' }"
                  aria-hidden="true"
                />{{ healthState === 'loading' ? '正在检查…' : '重新检查' }}</UiButton
              ></UiCardHeader
            >
            <UiCardContent class="space-y-5 p-5">
              <div v-if="healthState === 'loading'" role="status" class="min-h-28 space-y-4">
                <span class="sr-only">正在检查日志服务连接</span
                ><UiSkeleton class="h-5 w-24" /><UiSkeleton class="h-4 w-48 max-w-full" />
              </div>
              <UiAlert v-else-if="healthState !== 'ready'" role="alert"
                ><UiAlertTitle>{{
                  healthState === 'forbidden' ? '当前账号无权检查服务状态' : '无法读取服务状态'
                }}</UiAlertTitle
                ><UiAlertDescription>{{
                  healthState === 'forbidden'
                    ? '请联系管理员确认访问权限。'
                    : '本次请求未取得有效结果，请重新检查。'
                }}</UiAlertDescription></UiAlert
              >
              <template v-else-if="health"
                ><div class="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p class="text-sm font-medium">网关 → Spark 日志服务</p>
                    <p class="mt-1 text-xs text-muted-foreground">
                      {{
                        health.connected
                          ? '网关已收到日志服务响应。'
                          : '网关可响应，但未能连接日志服务。'
                      }}
                    </p>
                  </div>
                  <UiBadge
                    variant="secondary"
                    :class="
                      health.connected
                        ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300'
                        : 'bg-amber-50 text-amber-800 dark:bg-amber-950/40 dark:text-amber-300'
                    "
                    >{{ health.connected ? '连接正常' : '连接降级' }}</UiBadge
                  >
                </div>
                <dl class="text-sm">
                  <dt class="text-xs text-muted-foreground">服务端检查时间（浏览器本地时间）</dt>
                  <dd class="mt-1.5 tabular-nums">{{ checkedAt }}</dd>
                </dl></template
              >
              <p
                class="border-t-[0.5px] border-border/70 pt-4 text-xs leading-5 text-muted-foreground"
              >
                此检查仅反映日志服务连通性，不代表数据库、采集任务或分析结果可用。检查不会启动分析任务。
              </p>
            </UiCardContent>
          </UiCard>
        </section>
      </div>
    </div>
  </section>
</template>
