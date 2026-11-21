<script setup lang="ts">
import { computed, nextTick, onUnmounted, reactive, ref, useTemplateRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { register, sendRegisterEmail } from '@/api/auth/auth'
import AuthPasswordInput from '@/components/AuthPasswordInput.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
import { mutationBusinessCode, mutationResultUnknown, requireMutationSuccess } from '@/lib/rbac-mutations'
import { safeReturnPath } from '@/lib/session'

const route = useRoute()
const router = useRouter()
const login = computed(() => ({
  path: '/login',
  query: { redirect: safeReturnPath(route.query.redirect) ?? undefined },
}))
const form = reactive({ name: '', email: '', code: '', password: '', confirmation: '' })
const registrationForm = useTemplateRef<HTMLFormElement>('registrationForm')
const email = computed(() => form.email.trim())
const errors = reactive<Partial<Record<keyof typeof form, string>>>({})
const fields = [
  ['name', '显示姓名'], ['email', '邮箱地址'], ['code', '邮箱验证码'],
  ['password', '登录密码'], ['confirmation', '确认密码'],
] as const
const invalidFields = computed(() => fields.filter(([field]) => errors[field]))
const showErrorSummary = ref(false)
const registering = ref(false)
const error = ref('')
type Feedback = { message: string, field?: 'email' | 'code' }
type Delivery = { state: 'sending' } | { state: 'sent', expiresAt: number } | { state: 'failed', feedback: Feedback }
const deliveries = reactive(new Map<string, Delivery>())
const delivery = computed(() => deliveries.get(email.value))
const sending = computed(() => delivery.value?.state === 'sending')
const sendIssue = computed(() => delivery.value?.state === 'failed' ? delivery.value.feedback : undefined)
const emailError = computed(() => errors.email || (sendIssue.value?.field === 'email' ? sendIssue.value.message : ''))
const codeError = computed(() => errors.code || (sendIssue.value?.field === 'code' ? sendIssue.value.message : ''))
const now = ref(Date.now())
const remaining = computed(() => delivery.value?.state === 'sent'
  ? Math.max(0, Math.ceil((delivery.value.expiresAt - now.value) / 1000)) : 0)
let active = true
let timer: ReturnType<typeof setInterval> | undefined

function tick() {
  now.value = Date.now()
  let coolingDown = false
  for (const [target, result] of deliveries) {
    if (result.state !== 'sent') continue
    if (result.expiresAt <= now.value) deliveries.delete(target)
    else coolingDown = true
  }
  if (!coolingDown) {
    clearInterval(timer)
    timer = undefined
  }
}
onUnmounted(() => {
  active = false
  clearInterval(timer)
})
watch(email, () => {
  form.code = ''
  errors.code = ''
})

function validEmail() {
  errors.email = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)
    ? '' : '请输入有效的邮箱地址。'
  return !errors.email
}

function focusField(field: keyof typeof form) {
  registrationForm.value?.querySelector<HTMLInputElement>(`#register-${field}`)?.focus()
}

async function focusFirstError() {
  showErrorSummary.value = true
  await nextTick()
  const first = invalidFields.value[0]
  if (first) focusField(first[0])
}

function failure(cause: unknown, sending: boolean): Feedback {
  const status = cause && typeof cause === 'object' && 'code' in cause ? cause.code : null
  const code = mutationBusinessCode(cause) ?? status
  if (code === 1001 || code === 409) return { field: 'email', message: '该邮箱已注册，可以直接登录。' }
  if (code === 1003) return { field: 'code', message: '验证码不正确，请检查后重试。' }
  if (code === 1004) return { field: 'code', message: '验证码已过期，请重新获取。' }
  if (code === 1011 || code === 429) return { message: '请求过于频繁，请稍后重试。' }
  if (mutationResultUnknown(cause)) return { message: sending
    ? '发送结果未确认，请先检查邮箱。'
    : '注册结果未确认，可以先尝试登录。' }
  return { message: sending ? '暂时无法发送验证码，请稍后重试。' : '暂时无法注册，请核对信息后重试。' }
}

async function send() {
  if (registering.value || sending.value || remaining.value || !validEmail()) return
  const target = email.value
  deliveries.set(target, { state: 'sending' })
  error.value = ''
  errors.code = ''
  try {
    requireMutationSuccess(await sendRegisterEmail({ email: target }))
    if (!active) return
    tick()
    deliveries.set(target, { state: 'sent', expiresAt: now.value + 60_000 })
    timer ??= setInterval(tick, 250)
  } catch (cause) {
    if (active) deliveries.set(target, { state: 'failed', feedback: failure(cause, true) })
  }
}

async function submit() {
  if (registering.value || sending.value) return
  error.value = ''
  showErrorSummary.value = false
  if (sendIssue.value) deliveries.delete(email.value)
  validEmail()
  errors.name = form.name.trim() ? '' : '请输入显示姓名。'
  errors.password = form.password.length >= 8 && form.password.length <= 64 ? '' : '请输入 8–64 个字符。'
  errors.confirmation = form.password === form.confirmation ? '' : '两次输入的密码不一致。'
  errors.code = /^\d{6}$/.test(form.code.trim()) ? '' : '请输入 6 位数字验证码。'
  if (invalidFields.value.length) return focusFirstError()
  registering.value = true
  try {
    requireMutationSuccess(await register({
      name: form.name.trim(), email: email.value, password: form.password, code: form.code.trim(),
    }))
    if (!active) return
    await router.replace({ ...login.value, query: { ...login.value.query, registered: '1' } })
  } catch (cause) {
    if (active) {
      const feedback = failure(cause, false)
      if (feedback.field) {
        errors[feedback.field] = feedback.message
        void focusFirstError()
      } else error.value = feedback.message
    }
  } finally {
    registering.value = false
  }
}
</script>

<template>
  <AuthLayout registration>
    <section class="flex w-full max-w-md flex-col gap-5" aria-labelledby="register-title">
      <header class="flex flex-col gap-2">
        <h1 id="register-title" class="text-3xl font-semibold tracking-tight">创建你的账号</h1>
        <p class="text-sm text-muted-foreground">验证邮箱后即可创建 BossAnalyze 账号。</p>
      </header>
      <form ref="registrationForm" novalidate :aria-busy="registering" class="flex flex-col gap-5" @submit.prevent="submit">
        <UiFieldGroup class="gap-4">
          <UiField :data-invalid="Boolean(errors.name)">
            <UiFieldLabel for="register-name">显示姓名</UiFieldLabel>
            <UiInput id="register-name" v-model="form.name" maxlength="64" autocomplete="name" placeholder="请输入显示姓名" :disabled="registering" :aria-invalid="Boolean(errors.name)" :aria-describedby="errors.name ? 'register-name-error' : undefined" @input="errors.name = ''" />
            <UiFieldError v-if="errors.name" id="register-name-error">{{ errors.name }}</UiFieldError>
          </UiField>
          <UiField :data-invalid="Boolean(emailError)">
            <UiFieldLabel for="register-email">邮箱地址</UiFieldLabel>
            <UiInput id="register-email" v-model="form.email" type="email" maxlength="254" autocomplete="email" placeholder="name@example.com" :disabled="registering" :aria-invalid="Boolean(emailError)" :aria-describedby="emailError ? 'register-email-error' : undefined" @input="errors.email = ''" />
            <UiFieldError v-if="emailError" id="register-email-error">{{ emailError }}</UiFieldError>
          </UiField>
          <UiField :data-invalid="Boolean(codeError)">
            <UiFieldLabel for="register-code">邮箱验证码</UiFieldLabel>
            <div class="flex gap-2">
              <UiInput id="register-code" v-model="form.code" class="min-w-0" maxlength="6" inputmode="numeric" autocomplete="one-time-code" placeholder="6 位数字" :disabled="registering" :aria-invalid="Boolean(codeError)" :aria-describedby="codeError ? 'register-code-error' : 'register-code-help'" @input="errors.code = ''" />
              <UiButton type="button" variant="outline" class="tabular-nums" :disabled="registering || sending || remaining > 0" @click="send">
                {{ sending ? '正在发送…' : remaining ? `${remaining}s 后重发` : '获取验证码' }}
              </UiButton>
            </div>
            <UiFieldError v-if="codeError" id="register-code-error">{{ codeError }}</UiFieldError>
            <p v-else id="register-code-help" class="text-xs text-muted-foreground">验证码发送后 1 分钟内有效。</p>
            <p v-if="remaining" role="status" class="text-sm text-muted-foreground [overflow-wrap:anywhere]">验证码已发送至 {{ email }}。</p>
            <UiAlert v-if="sendIssue && !sendIssue.field" role="alert" variant="destructive"><UiAlertDescription>{{ sendIssue.message }}</UiAlertDescription></UiAlert>
          </UiField>
          <UiField :data-invalid="Boolean(errors.password)">
            <UiFieldLabel for="register-password">登录密码</UiFieldLabel>
            <AuthPasswordInput id="register-password" v-model="form.password" label="登录密码" maxlength="64" autocomplete="new-password" placeholder="8–64 个字符" :disabled="registering" :aria-invalid="Boolean(errors.password)" :aria-describedby="errors.password ? 'register-password-error' : undefined" @input="errors.password = ''" />
            <UiFieldError v-if="errors.password" id="register-password-error">{{ errors.password }}</UiFieldError>
          </UiField>
          <UiField :data-invalid="Boolean(errors.confirmation)">
            <UiFieldLabel for="register-confirmation">确认密码</UiFieldLabel>
            <AuthPasswordInput id="register-confirmation" v-model="form.confirmation" label="确认密码" maxlength="64" autocomplete="new-password" placeholder="再次输入登录密码" :disabled="registering" :aria-invalid="Boolean(errors.confirmation)" :aria-describedby="errors.confirmation ? 'register-confirmation-error' : undefined" @input="errors.confirmation = ''" />
            <UiFieldError v-if="errors.confirmation" id="register-confirmation-error">{{ errors.confirmation }}</UiFieldError>
          </UiField>
        </UiFieldGroup>
        <UiAlert v-if="error" role="alert" variant="destructive"><UiAlertDescription>{{ error }}</UiAlertDescription></UiAlert>
        <UiAlert v-if="showErrorSummary && invalidFields.length" variant="destructive">
          <UiAlertTitle>请检查以下字段</UiAlertTitle>
          <UiAlertDescription>
            <ul class="flex flex-wrap gap-x-3 gap-y-1">
              <li v-for="[field, label] in invalidFields" :key="field">
                <a :href="`#register-${field}`" class="underline underline-offset-4" @click.prevent="focusField(field)">{{ label }}</a>
              </li>
            </ul>
          </UiAlertDescription>
        </UiAlert>
        <UiButton type="submit" :disabled="registering || sending">{{ registering ? '正在注册…' : '完成注册' }}</UiButton>
      </form>
      <p class="text-center text-sm text-muted-foreground">
        已有账号？
        <RouterLink :to="login" class="font-medium text-foreground underline underline-offset-4">返回登录</RouterLink>
      </p>
    </section>
  </AuthLayout>
</template>
