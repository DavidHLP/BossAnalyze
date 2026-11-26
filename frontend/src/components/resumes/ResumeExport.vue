<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { exportDestination, resumeExportResult, resumeHtml } from '@/lib/resumes'

const props = defineProps<{ title: string; content: string }>()
const open = defineModel<boolean>('open', { required: true })
const destination = exportDestination(import.meta.env.VITE_EXPORT_URL, import.meta.env.DEV && import.meta.env.VITE_BOSS_MOCK_PREVIEW === '1')
const accepted = ref(false)
const format = ref('PDF')
const pending = ref(false)
const error = ref('')
const printError = ref('')
const printing = ref(false)
const resultUrl = ref<string | null>(null)
const complete = computed(() => resultUrl.value !== null)
const domain = computed(() => destination?.startsWith('/') ? '本机 Mock 导出服务' : destination)

watch(open, () => {
  accepted.value = false
  error.value = ''
  resultUrl.value = null
  printError.value = ''
  if (!open.value) clearPrintFrame()
})

function download(format: 'html' | 'md') {
  const html = format === 'html'
  const blob = new Blob([html ? resumeHtml(props.title, props.content) : props.content], {
    type: html ? 'text/html;charset=utf-8' : 'text/markdown;charset=utf-8',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = (props.title.trim().replace(/[<>:"/\\|?*\u0000-\u001f]/g, '_') || '简历') + '.' + format
  document.body.append(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1_000)
}

let printFrame: HTMLIFrameElement | undefined

function clearPrintFrame() {
  printFrame?.remove()
  printFrame = undefined
  printing.value = false
}

function printResume() {
  clearPrintFrame()
  printError.value = ''
  printing.value = true
  const frame = document.createElement('iframe')
  printFrame = frame
  frame.title = '简历打印视图'
  frame.className = 'fixed size-0 border-0'
  frame.tabIndex = -1
  frame.setAttribute('aria-hidden', 'true')
  frame.setAttribute('sandbox', 'allow-same-origin allow-modals')
  frame.addEventListener('load', () => {
    if (printFrame !== frame) return
    try {
      const view = frame.contentWindow
      if (!view) throw new Error('Print view unavailable')
      view.addEventListener('afterprint', () => {
        if (printFrame === frame) clearPrintFrame()
      }, { once: true })
      view.print()
    } catch {
      printError.value = '无法打开打印视图，可先下载 HTML 再打印。'
      clearPrintFrame()
    } finally {
      printing.value = false
    }
  }, { once: true })
  frame.srcdoc = resumeHtml(props.title, props.content)
  document.body.append(frame)
}

onUnmounted(clearPrintFrame)

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
    resultUrl.value = resumeExportResult(await response.json())
    if (!resultUrl.value) throw new Error('Missing export result')
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
        <UiDialogTitle>导出简历</UiDialogTitle>
        <UiDialogDescription>导出当前编辑区中的正文。</UiDialogDescription>
      </UiDialogHeader>
      <section aria-labelledby="local-resume-export" class="flex flex-col gap-2">
        <h3 id="local-resume-export" class="font-medium">本地导出</h3>
        <p class="text-sm text-muted-foreground">文件在浏览器中生成。打印和 HTML 用文字标注图片素材。</p>
        <div class="flex flex-wrap gap-2">
          <UiButton type="button" variant="outline" :disabled="printing" @click="printResume">打印 / 另存为 PDF</UiButton>
          <UiButton type="button" variant="outline" @click="download('html')">下载 HTML</UiButton>
          <UiButton type="button" variant="outline" @click="download('md')">下载 Markdown</UiButton>
        </div>
        <p v-if="printError" role="alert" class="text-sm text-destructive">{{ printError }}</p>
      </section>
      <form aria-labelledby="external-resume-export" class="flex flex-col gap-4 border-t pt-4" @submit.prevent="submit">
        <div>
          <h3 id="external-resume-export" class="font-medium">外部导出</h3>
          <p class="text-sm text-muted-foreground">确认后，当前简历正文和标题将发送至所示服务。</p>
        </div>
        <div class="flex flex-col gap-1 text-sm">
          <span class="text-muted-foreground">导出地址</span>
          <span class="break-all font-mono">{{ domain ?? '未配置外部导出地址' }}</span>
        </div>
        <UiField v-if="destination">
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
          <UiButton v-if="destination && !complete" type="submit" :disabled="!accepted || pending">{{ pending ? '正在导出…' : '确认外发并导出' }}</UiButton>
        </UiDialogFooter>
      </form>
    </UiDialogContent>
  </UiDialog>
</template>
