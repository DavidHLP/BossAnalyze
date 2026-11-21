<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { exportDestination } from '@/lib/resumes'

const props = defineProps<{ title: string; content: string }>()
const open = defineModel<boolean>('open', { required: true })
const destination = exportDestination(import.meta.env.VITE_EXPORT_URL, import.meta.env.DEV && import.meta.env.VITE_BOSS_MOCK_PREVIEW === '1')
const accepted = ref(false)
const format = ref('PDF')
const pending = ref(false)
const error = ref('')
const complete = ref(false)
const resultUrl = ref<string | null>(null)
const domain = computed(() => destination?.startsWith('/') ? '本机 Mock 导出服务' : destination)

watch(open, () => {
  accepted.value = complete.value = false
  error.value = ''
  resultUrl.value = null
})

async function submit() {
  if (!accepted.value || pending.value || complete.value || !destination) return
  pending.value = true
  error.value = ''
  try {
    const response = await fetch(destination, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'omit',
      redirect: 'error',
      signal: AbortSignal.timeout(20_000),
      body: JSON.stringify({ content: props.content, style: '', link: '', name: props.title, type: format.value }),
    })
    if (!response.ok) throw new Error('Export failed')
    const body: unknown = await response.json()
    if (!body || typeof body !== 'object' || ('success' in body && body.success === false)) throw new Error('Export failed')
    if ('url' in body && typeof body.url === 'string') resultUrl.value = exportDestination(body.url)
    complete.value = true
  } catch {
    error.value = '未收到有效导出结果。再次导出会重新发送当前简历内容。'
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <UiDialog :open="open" @update:open="!pending && (open = $event)">
    <UiDialogContent @interact-outside.prevent @escape-key-down="pending && $event.preventDefault()">
      <UiDialogHeader>
        <UiDialogTitle>外部导出简历</UiDialogTitle>
        <UiDialogDescription>导出将把当前简历内容发送至所示服务。</UiDialogDescription>
      </UiDialogHeader>
      <form class="flex flex-col gap-4" @submit.prevent="submit">
        <div class="flex flex-col gap-1 text-sm">
          <span class="text-muted-foreground">导出地址</span>
          <span class="break-all font-mono">{{ domain ?? '未配置外部导出地址' }}</span>
        </div>
        <UiField>
          <UiFieldLabel for="resume-export-format">导出格式</UiFieldLabel>
          <select id="resume-export-format" v-model="format" :disabled="pending || complete" class="h-8 border bg-background px-2">
            <option>PDF</option><option>HTML</option>
          </select>
        </UiField>
        <label v-if="destination && !complete" class="flex items-start gap-2 text-sm">
          <input v-model="accepted" :disabled="pending" type="checkbox" class="mt-1">
          我已确认地址并同意发送当前简历内容
        </label>
        <UiAlert v-if="error" role="alert" variant="destructive"><UiAlertDescription>{{ error }}</UiAlertDescription></UiAlert>
        <p v-if="complete" role="status" class="text-sm">
          导出服务已返回结果。
          <a v-if="resultUrl" :href="resultUrl" target="_blank" rel="noopener noreferrer" class="underline">打开导出结果</a>
        </p>
        <UiDialogFooter>
          <UiButton type="button" variant="outline" :disabled="pending" @click="open = false">{{ complete ? '关闭' : '取消' }}</UiButton>
          <UiButton v-if="!complete" type="submit" :disabled="!accepted || pending || !destination">{{ pending ? '正在导出…' : '确认外发并导出' }}</UiButton>
        </UiDialogFooter>
      </form>
    </UiDialogContent>
  </UiDialog>
</template>
