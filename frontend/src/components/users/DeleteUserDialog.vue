<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { withSessionOwner } from '@/lib/session'
import { mutationResultUnknown, sessionChangedMessage, unknownWriteMessage } from '@/lib/rbac-mutations'
import { deleteUser } from '@/api/user/user'
import { userDeleteError, userWriteSucceeded } from '@/lib/users'
import type { ManagedUser } from '@/lib/users'
import { useSession } from '@/composables/useSession'
const props = defineProps<{
  user: ManagedUser | null
  sessionChanged: boolean
  writeUnconfirmed: boolean
  listPending: boolean
  listFailed: boolean
}>()
const emit = defineEmits<{
  close: []
  deleted: []
  pending: [boolean]
  closeAutoFocus: [Event]
  writeUnconfirmed: []
  refreshList: []
}>()
const session = useSession()
const owner = session.capture()
const password = ref('')
const pending = ref(false)
const failure = ref('')
watch(
  () => props.user,
  () => {
    password.value = ''
    failure.value = ''
  },
)
function close() {
  if (!pending.value) emit('close')
}
async function remove() {
  const user = props.user
  if (!owner || !session.owns(owner) || props.sessionChanged || pending.value || props.writeUnconfirmed || !user ||
      !password.value || user.email === session.state.value.user?.email) return
  pending.value = true
  emit('pending', true)
  failure.value = ''
  try {
    userWriteSucceeded(await withSessionOwner(owner, () => deleteUser(user.id, password.value)))
    if (!session.owns(owner)) return
    password.value = ''
    emit('deleted')
  } catch (error) {
    if (!session.owns(owner)) return
    if (mutationResultUnknown(error)) {
      password.value = ''
      emit('writeUnconfirmed')
    } else failure.value = userDeleteError(error)
  } finally {
    pending.value = false
    emit('pending', false)
  }
  if (failure.value) {
    await nextTick()
    document.querySelector<HTMLElement>('#delete-user-password')?.focus()
  }
}
</script>

<template>
  <UiDialog
    :open="Boolean(user)"
    @update:open="
      (value) => {
        if (!value) close()
      }
    "
  >
    <UiDialogContent
      :show-close-button="false"
      class="sm:max-w-[400px]"
      @pointer-down-outside.prevent
      @interact-outside.prevent
      @close-auto-focus="emit('closeAutoFocus', $event)"
      @escape-key-down="
        (event) => {
          if (pending) event.preventDefault()
        }
      "
    >
      <UiDialogHeader>
        <UiDialogTitle class="break-words"
          >删除用户「{{ user?.name || '未命名用户' }}」？</UiDialogTitle
        >
        <UiDialogDescription
          >将永久移除该用户账户，无法在此恢复。请输入你当前账号的密码确认。</UiDialogDescription
        >
      </UiDialogHeader>
      <p class="break-all text-sm text-muted-foreground">
        {{ user?.email || '未填写邮箱' }} · <span class="font-mono tabular-nums">ID {{ user?.id }}</span>
      </p>
      <UiAlert v-if="sessionChanged" variant="destructive" role="alert">
        <UiAlertDescription>{{ sessionChangedMessage }}</UiAlertDescription>
      </UiAlert>
      <form id="delete-user-form" :aria-busy="pending" @submit.prevent="remove">
        <UiField
          ><UiFieldLabel for="delete-user-password">当前账号密码</UiFieldLabel
          ><UiInput
            id="delete-user-password"
            v-model="password"
            type="password"
            autocomplete="current-password"
            :disabled="pending || writeUnconfirmed"
            required
            :aria-describedby="failure ? 'delete-user-error' : undefined"
        /></UiField>
        <UiAlert v-if="failure" id="delete-user-error" variant="destructive" role="alert" class="mt-3">
          <UiAlertDescription>{{ failure }}</UiAlertDescription>
        </UiAlert>
      </form>
      <UiAlert v-if="writeUnconfirmed" role="alert">
        <UiAlertTitle>结果未确认</UiAlertTitle>
        <UiAlertDescription>{{ unknownWriteMessage }}{{ listFailed ? '。列表读取失败，请重试。' : '' }}</UiAlertDescription>
        <UiButton type="button" variant="outline" size="sm" class="mt-2"
          :disabled="pending || listPending || sessionChanged" @click="emit('refreshList')">刷新列表核对</UiButton>
      </UiAlert>
      <UiDialogFooter
        ><UiButton variant="outline" :disabled="pending" @click="close">取消</UiButton
        ><UiButton
          variant="destructive"
          form="delete-user-form"
          type="submit"
          :aria-disabled="pending || sessionChanged || writeUnconfirmed || !password"
          >{{ pending ? '正在删除…' : '删除用户' }}</UiButton
        ></UiDialogFooter
      >
    </UiDialogContent>
  </UiDialog>
</template>
