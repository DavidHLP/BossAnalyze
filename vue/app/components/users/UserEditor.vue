<script setup lang="ts">
import { useMediaQuery } from '@vueuse/core'
import { HugeiconsIcon } from '@hugeicons/vue'
import { Cancel01Icon } from '@hugeicons/core-free-icons'
import { addUser, updateUser } from '@/api/user/user'
import { userDraft, userErrors, userPayload, userWriteSucceeded } from '@/lib/users'
import type { ManagedUser, UserRole } from '@/lib/users'

const props = defineProps<{
  open: boolean
  mode: 'view' | 'create' | 'edit'
  user: ManagedUser | null
  roles: UserRole[]
  rolesUnavailable: boolean
}>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: [self: boolean]
  retryRoles: []
}>()
const mobile = useMediaQuery('(max-width: 639px)')
const session = useSession()
const draft = reactive(userDraft())
const initial = ref('')
const saving = ref(false)
const failed = ref(false)
const submitted = ref(false)
const touched = reactive<Record<string, boolean>>({})
const discardOpen = ref(false)
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
      failed.value = false
      Object.keys(touched).forEach((key) => delete touched[key])
    } else draft.password = ''
  },
)

function close() {
  if (saving.value) return
  if (dirty.value) discardOpen.value = true
  else emit('update:open', false)
}
function escape(event: Event) {
  event.preventDefault()
  close()
}
function cancelDiscard() {
  discardOpen.value = false
  destination.value = null
}
async function discard() {
  const target = destination.value
  destination.value = null
  discardOpen.value = false
  emit('update:open', false)
  await nextTick()
  if (target) await navigateTo(target)
}
onBeforeRouteLeave((to) => {
  if (saving.value) return false
  if (!dirty.value) return true
  destination.value = to.fullPath
  discardOpen.value = true
  return false
})
const beforeUnload = (event: BeforeUnloadEvent) => {
  if (dirty.value || saving.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onUnmounted(() => window.removeEventListener('beforeunload', beforeUnload))

async function save() {
  if (saving.value || props.mode === 'view' || props.rolesUnavailable) return
  submitted.value = true
  const firstError = Object.keys(errors.value)[0]
  if (firstError) {
    const selector =
      firstError === 'roleId'
        ? '#user-role input, #user-role button'
        : firstError === 'status'
          ? '#user-status button'
          : `#user-${firstError}`
    document.querySelector<HTMLElement>(selector)?.focus()
    return
  }
  saving.value = true
  failed.value = false
  try {
    const payload = userPayload(draft, creating.value ? undefined : props.user?.id)
    userWriteSucceeded(await (creating.value ? addUser(payload) : updateUser(payload)))
    draft.password = ''
    initial.value = JSON.stringify(draft)
    emit('saved', !creating.value && props.user?.email === session.state.value.user?.email)
  } catch {
    failed.value = true
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
      class="w-full! sm:max-w-[600px]! data-[side=bottom]:h-[80dvh] data-[side=bottom]:rounded-t-xl"
      @pointer-down-outside.prevent
      @interact-outside.prevent
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

      <div v-if="mode === 'view' && user" class="min-h-0 flex-1 overflow-y-auto p-6">
        <div class="mb-6">
          <p class="text-lg font-semibold">{{ user.name || '未命名用户' }}</p>
          <p class="mt-1 break-all text-sm text-muted-foreground">
            {{ user.email || '未填写邮箱' }}
          </p>
        </div>
        <dl class="grid grid-cols-2 gap-x-6 gap-y-5 text-sm">
          <div
            v-for="item in [
              ['用户 ID', String(user.id)],
              ['角色', user.roleName || (user.roleId ? `角色 #${user.roleId}` : '未分配')],
              ['状态', user.status === 1 ? '启用' : user.status === 0 ? '停用' : '未知'],
              ['创建日期', user.createTime],
              ['最近登录', user.lastLogin],
              ['最近登录 IP', user.lastLoginIp],
              ['地址', user.address],
              ['个人简介', user.introduction],
            ]"
            :key="item[0]"
            class="min-w-0 space-y-1.5"
          >
            <dt class="text-xs text-muted-foreground">{{ item[0] }}</dt>
            <dd class="break-words whitespace-pre-wrap font-medium">{{ item[1] || '—' }}</dd>
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
        <UiAlert v-if="rolesUnavailable" role="alert"
          ><UiAlertTitle>角色列表暂不可用</UiAlertTitle
          ><UiAlertDescription>请重新读取角色后保存。</UiAlertDescription
          ><UiButton
            type="button"
            size="sm"
            variant="outline"
            class="mt-2"
            @click="emit('retryRoles')"
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
          <p v-if="error('name')" id="user-name-error" class="text-xs text-destructive">
            {{ error('name') }}
          </p></UiField
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
          <p v-if="error('email')" id="user-email-error" class="text-xs text-destructive">
            {{ error('email') }}
          </p></UiField
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
          <p v-if="error('password')" id="user-password-error" class="text-xs text-destructive">
            {{ error('password') }}
          </p></UiField
        >
        <UiField
          ><UiFieldLabel as="span">角色 <span aria-hidden="true">*</span></UiFieldLabel
          ><UsersRolePicker
            id="user-role"
            v-model="draft.roleId"
            :roles="roles"
            label="用户角色"
            :disabled="saving || rolesUnavailable"
            aria-required="true"
            :aria-invalid="Boolean(error('roleId'))"
            :aria-describedby="error('roleId') ? 'user-role-error' : undefined"
          />
          <p v-if="error('roleId')" id="user-role-error" class="text-xs text-destructive">
            {{ error('roleId') }}
          </p></UiField
        >
        <UiField
          ><UiFieldLabel as="span">状态</UiFieldLabel
          ><UiRadioGroup
            id="user-status"
            v-model="draft.status"
            aria-label="用户状态"
            class="flex gap-6"
            :disabled="saving"
            ><label class="flex items-center gap-2 text-sm"
              ><UiRadioGroupItem value="1" />启用</label
            ><label class="flex items-center gap-2 text-sm"
              ><UiRadioGroupItem value="0" />停用</label
            ></UiRadioGroup
          >
          <p v-if="error('status')" class="text-xs text-destructive">
            {{ error('status') }}
          </p></UiField
        >
        <UiSeparator />
        <UiField
          ><UiFieldLabel for="user-address">地址</UiFieldLabel
          ><UiInput id="user-address" v-model="draft.address" maxlength="255" :disabled="saving" />
          <p v-if="error('address')" class="text-xs text-destructive">
            {{ error('address') }}
          </p></UiField
        >
        <UiField
          ><UiFieldLabel for="user-introduction">个人简介</UiFieldLabel
          ><UiTextarea
            id="user-introduction"
            v-model="draft.introduction"
            maxlength="500"
            class="min-h-24"
            :disabled="saving"
          />
          <p v-if="error('introduction')" class="text-xs text-destructive">
            {{ error('introduction') }}
          </p></UiField
        >
        <p v-if="failed" role="alert" class="text-sm text-destructive">
          保存失败，请检查输入或服务连接后重试。填写内容已保留。
        </p>
      </form>
      <UiSheetFooter class="shrink-0 flex-row justify-end border-t-[0.5px] border-border">
        <UiButton variant="outline" :disabled="saving" @click="close">{{
          mode === 'view' ? '关闭' : '取消'
        }}</UiButton>
        <UiButton
          v-if="mode !== 'view'"
          type="submit"
          form="user-editor-form"
          :disabled="saving || rolesUnavailable"
          >{{ saving ? '正在保存…' : creating ? '创建用户' : '保存更改' }}</UiButton
        >
      </UiSheetFooter>
    </UiSheetContent>
  </UiSheet>
  <UiDialog
    :open="discardOpen"
    @update:open="
      (value) => {
        discardOpen = value
        if (!value) destination = null
      }
    "
  >
    <UiDialogContent
      :show-close-button="false"
      class="top-[38.2%]! sm:max-w-[400px]!"
      @pointer-down-outside.prevent
      @interact-outside.prevent
    >
      <UiDialogHeader
        ><UiDialogTitle>放弃未保存的修改？</UiDialogTitle
        ><UiDialogDescription>关闭后，本次填写的内容不会保存。</UiDialogDescription></UiDialogHeader
      >
      <UiDialogFooter
        ><UiButton variant="outline" @click="cancelDiscard">继续编辑</UiButton
        ><UiButton variant="outline" @click="discard">放弃修改</UiButton></UiDialogFooter
      >
    </UiDialogContent>
  </UiDialog>
</template>
