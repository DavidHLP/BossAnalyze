<script setup lang="ts">
import { deleteUser } from '@/api/user/user'
import { userDeleteError, userWriteSucceeded } from '@/lib/users'
import type { ManagedUser } from '@/lib/users'
const props = defineProps<{ user: ManagedUser | null }>()
const emit = defineEmits<{ close: []; deleted: [] }>()
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
  if (pending.value || !props.user || !password.value) return
  pending.value = true
  failure.value = ''
  try {
    userWriteSucceeded(await deleteUser(props.user.id, password.value))
    password.value = ''
    emit('deleted')
  } catch (error) {
    failure.value = userDeleteError(error)
  } finally {
    pending.value = false
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
      class="top-[38.2%]! sm:max-w-[400px]!"
      @pointer-down-outside.prevent
      @interact-outside.prevent
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
      <form id="delete-user-form" :aria-busy="pending" @submit.prevent="remove">
        <UiField
          ><UiFieldLabel for="delete-user-password">当前账号密码</UiFieldLabel
          ><UiInput
            id="delete-user-password"
            v-model="password"
            type="password"
            autocomplete="current-password"
            :disabled="pending"
            required
            :aria-describedby="failure ? 'delete-user-error' : undefined"
        /></UiField>
        <p v-if="failure" id="delete-user-error" role="alert" class="mt-3 text-sm text-destructive">
          {{ failure }}
        </p>
      </form>
      <UiDialogFooter
        ><UiButton variant="outline" :disabled="pending" @click="close">取消</UiButton
        ><UiButton
          variant="destructive"
          form="delete-user-form"
          type="submit"
          :disabled="pending || !password"
          >{{ pending ? '正在删除…' : '删除用户' }}</UiButton
        ></UiDialogFooter
      >
    </UiDialogContent>
  </UiDialog>
</template>
