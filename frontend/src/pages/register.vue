<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { register, sendRegisterEmail } from '@/api/auth/auth'
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
const errors = reactive<Partial<Record<keyof typeof form, string>>>({})
const action = ref<'send' | 'register' | null>(null)
const notice = ref('')
const error = ref('')
const remaining = ref(0)
const sentTo = ref('')
let active = true
let expiresAt = 0
let timer: ReturnType<typeof setInterval> | undefined

function tick() {
  remaining.value = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000))
  if (!remaining.value) clearInterval(timer)
}
onUnmounted(() => {
  active = false
  clearInterval(timer)
})

function validEmail() {
  errors.email = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())
    ? '' : '请输入有效的邮箱地址。'
  return !errors.email
}

function failure(cause: unknown, sending: boolean) {
  const status = cause && typeof cause === 'object' && 'code' in cause ? cause.code : null
  const code = mutationBusinessCode(cause) ?? status
  if (code === 1001 || code === 409) errors.email = '该邮箱已注册，可以直接登录。'
  else if (code === 1003) errors.code = '验证码不正确，请检查后重试。'
  else if (code === 1004) errors.code = '验证码已过期，请重新获取。'
  else if (code === 1011 || code === 429) error.value = '请求过于频繁，请稍后重试。'
  else if (mutationResultUnknown(cause)) error.value = sending
    ? '发送结果未确认，请先检查邮箱。'
    : '注册结果未确认，可以先尝试登录。'
  else error.value = sending ? '暂时无法发送验证码，请稍后重试。' : '暂时无法注册，请核对信息后重试。'
}

async function send() {
  if (action.value || remaining.value || !validEmail()) return
  action.value = 'send'
  error.value = notice.value = ''
  errors.code = ''
  const email = form.email.trim()
  try {
    requireMutationSuccess(await sendRegisterEmail({ email }))
    if (!active) return
    sentTo.value = email
    expiresAt = Date.now() + 60_000
    tick()
    timer = setInterval(tick, 250)
    notice.value = '验证码已发送，1 分钟内有效。'
  } catch (cause) {
    failure(cause, true)
  } finally {
    action.value = null
  }
}

async function submit() {
  if (action.value) return
  error.value = notice.value = ''
  const emailValid = validEmail()
  errors.name = form.name.trim() ? '' : '请输入显示姓名。'
  errors.password = form.password.length >= 8 && form.password.length <= 64 ? '' : '请输入 8–64 个字符。'
  errors.confirmation = form.password === form.confirmation ? '' : '两次输入的密码不一致。'
  errors.code = /^\d{6}$/.test(form.code.trim()) ? '' : '请输入 6 位数字验证码。'
  if (!emailValid || Object.values(errors).some(Boolean)) return
  action.value = 'register'
  try {
    requireMutationSuccess(await register({
      name: form.name.trim(), email: form.email.trim(), password: form.password, code: form.code.trim(),
    }))
    if (!active) return
    await router.replace({ ...login.value, query: { ...login.value.query, registered: '1' } })
  } catch (cause) {
    failure(cause, false)
  } finally {
    action.value = null
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
      <form novalidate :aria-busy="action !== null" class="flex flex-col gap-5" @submit.prevent="submit">
        <UiFieldGroup class="gap-4">
          <UiField :data-invalid="Boolean(errors.name)">
            <UiFieldLabel for="register-name">显示姓名</UiFieldLabel>
            <UiInput id="register-name" v-model="form.name" maxlength="64" autocomplete="name" placeholder="请输入显示姓名" :disabled="action !== null" :aria-invalid="Boolean(errors.name)" :aria-describedby="errors.name ? 'register-name-error' : undefined" @input="errors.name = ''" />
            <UiFieldError v-if="errors.name" id="register-name-error">{{ errors.name }}</UiFieldError>
          </UiField>
          <UiField :data-invalid="Boolean(errors.email)">
            <UiFieldLabel for="register-email">邮箱地址</UiFieldLabel>
            <UiInput id="register-email" v-model="form.email" type="email" maxlength="254" autocomplete="email" placeholder="name@example.com" :disabled="action !== null" :aria-invalid="Boolean(errors.email)" :aria-describedby="errors.email ? 'register-email-error' : undefined" @input="errors.email = ''" />
            <UiFieldError v-if="errors.email" id="register-email-error">{{ errors.email }}</UiFieldError>
          </UiField>
          <UiField :data-invalid="Boolean(errors.code)">
            <UiFieldLabel for="register-code">邮箱验证码</UiFieldLabel>
            <div class="flex gap-2">
              <UiInput id="register-code" v-model="form.code" class="min-w-0" maxlength="6" inputmode="numeric" autocomplete="one-time-code" placeholder="6 位数字" :disabled="action !== null" :aria-invalid="Boolean(errors.code)" :aria-describedby="errors.code ? 'register-code-error' : 'register-code-help'" @input="errors.code = ''" />
              <UiButton type="button" variant="outline" class="tabular-nums" :disabled="action !== null || remaining > 0" @click="send">
                {{ action === 'send' ? '正在发送…' : remaining ? `${remaining}s 后重发` : '获取验证码' }}
              </UiButton>
            </div>
            <UiFieldError v-if="errors.code" id="register-code-error">{{ errors.code }}</UiFieldError>
            <p v-else id="register-code-help" class="text-xs text-muted-foreground">验证码发送后 1 分钟内有效。</p>
          </UiField>
          <UiField :data-invalid="Boolean(errors.password)">
            <UiFieldLabel for="register-password">登录密码</UiFieldLabel>
            <UiInput id="register-password" v-model="form.password" type="password" maxlength="64" autocomplete="new-password" placeholder="8–64 个字符" :disabled="action !== null" :aria-invalid="Boolean(errors.password)" :aria-describedby="errors.password ? 'register-password-error' : undefined" @input="errors.password = ''" />
            <UiFieldError v-if="errors.password" id="register-password-error">{{ errors.password }}</UiFieldError>
          </UiField>
          <UiField :data-invalid="Boolean(errors.confirmation)">
            <UiFieldLabel for="register-confirmation">确认密码</UiFieldLabel>
            <UiInput id="register-confirmation" v-model="form.confirmation" type="password" maxlength="64" autocomplete="new-password" placeholder="再次输入登录密码" :disabled="action !== null" :aria-invalid="Boolean(errors.confirmation)" :aria-describedby="errors.confirmation ? 'register-confirmation-error' : undefined" @input="errors.confirmation = ''" />
            <UiFieldError v-if="errors.confirmation" id="register-confirmation-error">{{ errors.confirmation }}</UiFieldError>
          </UiField>
        </UiFieldGroup>
        <UiAlert v-if="error" role="alert" variant="destructive"><UiAlertDescription>{{ error }}</UiAlertDescription></UiAlert>
        <p v-if="notice && form.email.trim() === sentTo" role="status" class="text-sm text-muted-foreground">{{ notice }}</p>
        <UiButton type="submit" :disabled="action !== null">{{ action === 'register' ? '正在注册…' : '完成注册' }}</UiButton>
      </form>
      <p class="text-center text-sm text-muted-foreground">
        已有账号？
        <RouterLink :to="login" class="font-medium text-foreground underline underline-offset-4">返回登录</RouterLink>
      </p>
    </section>
  </AuthLayout>
</template>
