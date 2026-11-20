<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { HugeiconsIcon } from '@hugeicons/vue'
import { Cancel01Icon } from '@hugeicons/core-free-icons'
import { withSessionOwner } from '@/lib/session'
import { sessionChangedMessage } from '@/lib/rbac-mutations'
import { addUser, updateUser } from '@/api/user/user'
import { userDraft, userErrors, userPayload, userSaveError, userWriteSucceeded } from '@/lib/users'
import type { ManagedUser, UserRole } from '@/lib/users'
import { useSession } from '@/composables/useSession'
import RolePicker from './RolePicker.vue'

const props = defineProps<{
  open: boolean
  mode: 'view' | 'create' | 'edit'
  user: ManagedUser | null
  roles: UserRole[]
  rolesUnavailable: boolean
  rolePending: boolean
  roleRestricted: boolean
}>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  closeAutoFocus: [event: Event]
  saved: [self: boolean]
  retryRoles: []
}>()
const mobile = useMediaQuery('(width < 640px)')
const session = useSession()
const router = useRouter()
const owner = session.capture()
const sessionChanged = computed(() => !session.owns(owner))
const draft = reactive(userDraft())
const initial = ref('')
const saving = ref(false)
const failure = ref('')
const submitted = ref(false)
const touched = reactive<Record<string, boolean>>({})
const discardOpen = ref(false)
const discardPrompt = ref<HTMLElement | null>(null)
let discardTrigger: HTMLElement | null = null
watch(discardOpen, (open) => {
  if (open) discardPrompt.value?.focus()
}, { flush: 'post' })
function requestDiscard() {
  if (discardOpen.value) return
  discardTrigger = typeof document === 'undefined' ? null : document.activeElement as HTMLElement | null
  discardOpen.value = true
}
const destination = ref<string | null>(null)
const creating = computed(() => props.mode === 'create')
const title = computed(() =>
  props.mode === 'view' ? '用户详情' : creating.value ? '新增用户' : '编辑用户',
)
const errors = computed(() => userErrors(draft, creating.value))
const dirty = computed(
  () => props.open && props.mode !== 'view' && JSON.stringify(draft) !== initial.value,
)
function error(field: string) {
  return submitted.value || touched[field] ? errors.value[field] : undefined
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      Object.assign(draft, userDraft(props.user))
      initial.value = JSON.stringify(draft)
      submitted.value = false
      failure.value = ''
      Object.keys(touched).forEach((key) => delete touched[key])
    } else {
      cancelDiscard(false)
      draft.password = ''
    }
  },
)

function close() {
  if (saving.value) return
  if (dirty.value) requestDiscard()
  else emit('update:open', false)
}
function escape(event: Event) {
  event.preventDefault()
  if (discardOpen.value) cancelDiscard()
  else close()
}
function cancelDiscard(restoreFocus = true) {
  discardOpen.value = false
  destination.value = null
  if (restoreFocus) discardTrigger?.focus()
  discardTrigger = null
}
async function discard() {
  const target = destination.value
  cancelDiscard(false)
  emit('update:open', false)
  await nextTick()
  if (target === '/login' && owner && session.owns(owner)) {
    await session.signOut(false, null, undefined, owner)
  } else if (target) await router.push(target)
}
onBeforeRouteLeave((to) => {
  if (session.state.value.invalidated) return true
  if (saving.value) return false
  if (!dirty.value) return true
  destination.value = to.fullPath
  requestDiscard()
  return false
})
const beforeUnload = (event: BeforeUnloadEvent) => {
  if (!session.state.value.invalidated && (dirty.value || saving.value)) {
    event.preventDefault()
    event.returnValue = ''
  }
}
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onUnmounted(() => window.removeEventListener('beforeunload', beforeUnload))

async function save() {
  if (!owner || !session.owns(owner) || discardOpen.value || saving.value || props.mode === 'view' || props.rolesUnavailable) return
  submitted.value = true
  const firstError = Object.keys(errors.value)[0]
  if (firstError) {
    await nextTick()
    const selector =
      firstError === 'roleId'
        ? '#user-role input, #user-role button'
        : firstError === 'status'
          ? '#user-status button'
          : `#user-${firstError}`
    document.querySelector<HTMLElement>(selector)?.focus()
    return
  }
  const self = !creating.value && props.user?.email === session.state.value.user?.email
  saving.value = true
  failure.value = ''
  try {
    const payload = userPayload(draft, creating.value ? undefined : props.user?.id)
    userWriteSucceeded(await withSessionOwner(owner, () =>
      creating.value ? addUser(payload) : updateUser(payload),
    ))
    if (!session.owns(owner)) return
    draft.password = ''
    initial.value = JSON.stringify(draft)
    emit('saved', self)
  } catch (error) {
    failure.value = userSaveError(error)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <UiSheet
    :open="open"
    @update:open="
      (value) => {
        if (!value) close()
      }
    "
  >
    <UiSheetContent
      :side="mobile ? 'bottom' : 'right'"
      :show-close-button="false"
      class="w-full! shadow-panel sm:max-w-[600px]! data-[side=bottom]:h-[80dvh] data-[side=bottom]:rounded-t-xl"
      @pointer-down-outside.prevent
      @interact-outside.prevent
      @close-auto-focus="emit('closeAutoFocus', $event)"
      @escape-key-down="escape"
    >
      <UiSheetHeader class="shrink-0 border-b-[0.5px] border-border pr-14">
        <UiSheetTitle>{{ title }}</UiSheetTitle
        ><UiSheetDescription>{{
          mode === 'view' ? '账户信息与登录记录' : '填写账户信息后保存生效'
        }}</UiSheetDescription>
      </UiSheetHeader>
      <UiButton
        type="button"
        variant="ghost"
        size="icon-sm"
        aria-label="关闭用户面板"
        class="absolute right-4 top-4"
        :disabled="saving"
        @click="close"
        ><HugeiconsIcon :icon="Cancel01Icon" aria-hidden="true"
      /></UiButton>

      <div
        v-if="discardOpen"
        ref="discardPrompt"
        tabindex="-1"
        role="group"
        aria-label="放弃未保存的修改？"
        class="mx-4 mt-3 shrink-0 space-y-2 rounded-md border border-border bg-muted p-3"
        @keydown.esc.stop.prevent="cancelDiscard()"
      >
        <p class="text-sm font-medium">放弃未保存的修改？</p>
        <div class="flex flex-wrap justify-end gap-2">
          <UiButton type="button" variant="ghost" class="text-destructive hover:text-destructive"
            @click="discard">放弃修改</UiButton>
          <UiButton type="button" variant="secondary" @click="cancelDiscard()">继续编辑</UiButton>
        </div>
      </div>
      <UiAlert v-if="sessionChanged && mode !== 'view'" variant="destructive" role="alert" class="mx-4 mt-3 w-auto shrink-0">
        <UiAlertDescription>{{ sessionChangedMessage }}</UiAlertDescription>
      </UiAlert>
      <div v-if="mode === 'view' && user" class="min-h-0 flex-1 overflow-y-auto p-6">
        <div class="mb-6">
          <p class="break-all text-lg font-semibold">{{ user.name || '未命名用户' }}</p>
          <p class="mt-1 break-all text-sm text-muted-foreground">
            {{ user.email || '未填写邮箱' }}
          </p>
        </div>
        <dl class="grid grid-cols-2 gap-x-6 gap-y-5 text-sm">
          <div
            v-for="item in [
              { label: '用户 ID', value: user.id, mono: true },
              { label: '角色', value: user.roleName || (user.roleId ? `角色 #${user.roleId}` : '未分配') },
              { label: '状态' },
              { label: '创建日期', value: user.createTime, mono: true },
              { label: '最近登录', value: user.lastLogin, mono: true },
              { label: '最近登录 IP', value: user.lastLoginIp, mono: true },
              { label: '地址', value: user.address },
              { label: '个人简介', value: user.introduction },
            ]"
            :key="item.label"
            class="min-w-0 space-y-1.5"
          >
            <dt class="text-xs text-muted-foreground">{{ item.label }}</dt>
            <dd v-if="item.label === '状态'">
              <StatusBadge :tone="user.status === 1 ? 'success' : user.status === 0 ? 'warning' : 'neutral'">
                {{ user.status === 1 ? '启用' : user.status === 0 ? '停用' : '未知' }}
              </StatusBadge>
            </dd>
            <dd v-else class="break-words whitespace-pre-wrap font-medium"
              :class="{ 'font-mono tabular-nums': 'mono' in item && item.mono }">{{ item.value || '—' }}</dd>
          </div>
        </dl>
      </div>
      <form
        v-else
        id="user-editor-form"
        class="min-h-0 flex-1 space-y-5 overflow-y-auto p-6"
        novalidate
        :aria-busy="saving"
        @submit.prevent="save"
      >
        <UiAlert v-if="mode === 'edit' && user?.email === session.state.value.user?.email"
          class="border-status-warning-foreground/25 bg-status-warning-surface text-status-warning-foreground">
          <UiAlertDescription class="text-inherit">修改当前登录账号保存后需重新登录。</UiAlertDescription>
        </UiAlert>
        <UiAlert v-if="rolesUnavailable" variant="destructive" role="alert"
          ><UiAlertTitle>{{ roleRestricted ? '当前账号无权读取角色列表' : '角色列表暂不可用' }}</UiAlertTitle
          ><UiAlertDescription>{{ roleRestricted ? '请联系管理员确认权限。' : '请重新读取角色后保存。' }}</UiAlertDescription
          ><UiButton
            type="button"
            size="sm"
            variant="outline"
            class="mt-2"
            :aria-disabled="rolePending || sessionChanged"
            @click="!rolePending && !sessionChanged && emit('retryRoles')"
            >重新读取角色</UiButton
          ></UiAlert
        >
        <UiField
          ><UiFieldLabel for="user-name">姓名 <span aria-hidden="true">*</span></UiFieldLabel
          ><UiInput
            id="user-name"
            required
            v-model="draft.name"
            maxlength="50"
            autocomplete="name"
            :disabled="saving"
            :aria-invalid="Boolean(error('name'))"
            :aria-describedby="error('name') ? 'user-name-error' : undefined"
            @blur="touched.name = true"
          />
          <UiFieldError v-if="error('name')" id="user-name-error" class="text-xs text-destructive">
            {{ error('name') }}
          </UiFieldError></UiField
        >
        <UiField
          ><UiFieldLabel for="user-email">邮箱 <span aria-hidden="true">*</span></UiFieldLabel
          ><UiInput
            id="user-email"
            required
            v-model="draft.email"
            type="email"
            maxlength="254"
            autocomplete="email"
            :disabled="saving"
            :aria-invalid="Boolean(error('email'))"
            :aria-describedby="error('email') ? 'user-email-error' : undefined"
            @blur="touched.email = true"
          />
          <UiFieldError v-if="error('email')" id="user-email-error" class="text-xs text-destructive">
            {{ error('email') }}
          </UiFieldError></UiField
        >
        <UiField v-if="creating"
          ><UiFieldLabel for="user-password"
            >初始密码 <span aria-hidden="true">*</span></UiFieldLabel
          ><UiInput
            id="user-password"
            required
            v-model="draft.password"
            type="password"
            autocomplete="new-password"
            maxlength="64"
            :disabled="saving"
            :aria-invalid="Boolean(error('password'))"
            :aria-describedby="error('password') ? 'user-password-error' : 'user-password-hint'"
            @blur="touched.password = true"
          />
          <p id="user-password-hint" class="text-xs text-muted-foreground">8–64 个字符</p>
          <UiFieldError v-if="error('password')" id="user-password-error" class="text-xs text-destructive">
            {{ error('password') }}
          </UiFieldError></UiField
        >
        <UiField
          @focusout="touched.roleId = true"
          ><UiFieldLabel as="span">角色 <span aria-hidden="true">*</span></UiFieldLabel
          ><RolePicker
            id="user-role"
            v-model="draft.roleId"
            :roles="roles"
            label="用户角色"
            :disabled="saving || rolesUnavailable"
            required
            :invalid="Boolean(error('roleId'))"
            :described-by="error('roleId') ? 'user-role-error' : undefined"
          />
          <UiFieldError v-if="error('roleId')" id="user-role-error" class="text-xs text-destructive">
            {{ error('roleId') }}
          </UiFieldError></UiField
        >
        <UiField @focusout="touched.status = true"
          ><UiFieldLabel as="span">状态</UiFieldLabel
          ><UiRadioGroup
            id="user-status"
            v-model="draft.status"
            aria-label="用户状态"
            class="flex gap-6"
            :disabled="saving"
            :aria-invalid="Boolean(error('status'))"
            :aria-describedby="error('status') ? 'user-status-error' : undefined"
            ><label class="flex items-center gap-2 text-sm"
              ><UiRadioGroupItem value="1" />启用</label
            ><label class="flex items-center gap-2 text-sm"
              ><UiRadioGroupItem value="0" />停用</label
            ></UiRadioGroup
          >
          <UiFieldError v-if="error('status')" id="user-status-error" class="text-xs text-destructive">
            {{ error('status') }}
          </UiFieldError></UiField
        >
        <UiSeparator />
        <UiField
          ><UiFieldLabel for="user-address">地址</UiFieldLabel
          ><UiInput id="user-address" v-model="draft.address" maxlength="255" :disabled="saving"
            :aria-invalid="Boolean(error('address'))"
            :aria-describedby="error('address') ? 'user-address-error' : undefined"
            @blur="touched.address = true" />
          <UiFieldError v-if="error('address')" id="user-address-error" class="text-xs text-destructive">
            {{ error('address') }}
          </UiFieldError></UiField
        >
        <UiField
          ><UiFieldLabel for="user-introduction">个人简介</UiFieldLabel
          ><UiTextarea
            id="user-introduction"
            v-model="draft.introduction"
            maxlength="255"
            class="min-h-24"
            :disabled="saving"
            :aria-invalid="Boolean(error('introduction'))"
            :aria-describedby="error('introduction') ? 'user-introduction-error' : undefined"
            @blur="touched.introduction = true"
          />
          <UiFieldError v-if="error('introduction')" id="user-introduction-error" class="text-xs text-destructive">
            {{ error('introduction') }}
          </UiFieldError></UiField
        >
      </form>
      <UiAlert v-if="failure" variant="destructive" role="alert" class="mx-4 mb-2 w-auto shrink-0">
        <UiAlertDescription><span class="block">{{ failure }}</span><span class="block">填写内容已保留。</span></UiAlertDescription>
      </UiAlert>
      <UiSheetFooter class="shrink-0 flex-row justify-end border-t-[0.5px] border-border">
        <UiButton variant="outline" :disabled="saving" @click="close">{{
          mode === 'view' ? '关闭' : '取消'
        }}</UiButton>
        <UiButton
          v-if="mode !== 'view'"
          type="submit"
          form="user-editor-form"
          :aria-disabled="saving || rolesUnavailable || discardOpen || sessionChanged"
          >{{ saving ? '正在保存…' : creating ? '创建用户' : '保存更改' }}</UiButton
        >
      </UiSheetFooter>
    </UiSheetContent>
  </UiSheet>
</template>
