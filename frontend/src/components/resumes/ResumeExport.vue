<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { FieldDescription, FieldTitle } from '@/components/ui/field'
import { exportDestination, resumeExportResult, resumeHasMath, resumeHtml, resumePreview, type ResumeMathRender } from '@/lib/resumes'
import { loadResumeMath } from '@/lib/resume-math'
import { inspectResumeTexTemplate, resumeLatex, type ResumeTexError } from '@/lib/resume-tex'
import { resumeTexTemplates, type ResumeTexTemplateId } from '@/lib/resume-tex-templates'

const props = defineProps<{ title: string; content: string }>()
const open = defineModel<boolean>('open', { required: true })
type ResumeRange = Pick<ResumeTexError, 'start' | 'end'>
const emit = defineEmits<{ locate: [range: ResumeRange] }>()
const destination = exportDestination(import.meta.env.VITE_EXPORT_URL, import.meta.env.DEV && import.meta.env.VITE_BOSS_MOCK_PREVIEW === '1')
const accepted = ref(false)
const format = ref('PDF')
const pending = ref(false)
const error = ref('')
const htmlError = ref('')
const htmlJob = ref<symbol>()
const sourceFallback = ref<'html' | 'pdf'>()
const printing = ref(false)
const htmlBusy = computed(() => !!htmlJob.value || printing.value)
const texTemplate = ref<ResumeTexTemplateId>('basic')
const texBlocks = computed(() => resumePreview(props.content))
const texInspection = computed(() => texTemplate.value === 'basic'
  ? null
  : inspectResumeTexTemplate(texBlocks.value, texTemplate.value))
const texErrors = ref<ResumeTexError[]>([])
const texWarnings = ref<string[]>([])
const resultUrl = ref<string | null>(null)
const complete = computed(() => resultUrl.value !== null)
const domain = computed(() => destination?.startsWith('/') ? '本机 Mock 导出服务' : destination)
let locateOnClose: ResumeRange | undefined

function resetLocalExport() {
  texErrors.value = []
  texWarnings.value = []
  htmlError.value = ''
  sourceFallback.value = undefined
  cancelHtml()
}

watch(open, () => {
  accepted.value = false
  error.value = ''
  resultUrl.value = null
  texTemplate.value = 'basic'
  resetLocalExport()
}, { flush: 'sync' })

watch([() => props.content, () => props.title], () => {
  locateOnClose = undefined
  resetLocalExport()
}, { flush: 'sync' })

function download(title: string, content: string, extension: 'html' | 'md' | 'tex', type: string) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = (title.trim().replace(/[<>:"/\\|?*\u0000-\u001f]/g, '_') || '简历') + '.' + extension
  document.body.append(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1_000)
}

function downloadLatex() {
  const { title, content } = props
  const result = resumeLatex(title, texBlocks.value, { sourceMarkdown: content, template: texTemplate.value })
  texErrors.value = result.ok ? [] : result.errors
  texWarnings.value = result.ok ? result.warnings.filter(warning => !texInspection.value?.warnings.includes(warning)) : []
  if (result.ok) download(title, result.tex, 'tex', 'text/x-tex;charset=utf-8')
}

async function useBasicTexTemplate() {
  texTemplate.value = 'basic'
  await nextTick()
  document.getElementById('resume-tex-template-basic')?.focus()
}

function locateFormula({ start, end }: ResumeRange) {
  locateOnClose = { start, end }
  open.value = false
}

function returnToEditor(event: Event) {
  if (!locateOnClose) return
  event.preventDefault()
  emit('locate', locateOnClose)
  locateOnClose = undefined
}

async function exportHtml(output: 'html' | 'pdf', withMath = true) {
  if (!open.value || htmlBusy.value) return
  const { title, content } = props
  const job = Symbol()
  htmlJob.value = job
  htmlError.value = ''
  sourceFallback.value = undefined
  try {
    const blocks = resumePreview(content)
    let renderMath: ResumeMathRender | undefined
    let invalidMath = false
    if (withMath && resumeHasMath(blocks)) {
      const engine = await loadResumeMath().catch(() => null)
      if (htmlJob.value !== job) return
      if (!engine) {
        htmlError.value = '公式渲染器加载失败。请重试，或保留公式源码继续导出。'
        sourceFallback.value = output
        return
      }
      renderMath = (tex, display) => {
        const rendered = engine.renderOrNull(tex, display)
        if (rendered === null) invalidMath = true
        return rendered
      }
    }
    const html = resumeHtml(title, blocks, renderMath)
    if (invalidMath) {
      htmlError.value = '至少有一处公式无法渲染。请修正公式后重试，或下载 Markdown 原文。'
      return
    }
    if (output === 'pdf') printResume(html)
    else download(title, html, 'html', 'text/html;charset=utf-8')
  } finally {
    if (htmlJob.value === job) htmlJob.value = undefined
  }
}

let printFrame: HTMLIFrameElement | undefined

function clearPrintFrame() {
  printFrame?.remove()
  printFrame = undefined
  printing.value = false
}

function cancelHtml() {
  htmlJob.value = undefined
  clearPrintFrame()
}

function printResume(html: string) {
  clearPrintFrame()
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
      htmlError.value = '无法打开打印视图，可先下载 HTML 再打印。'
      clearPrintFrame()
    } finally {
      printing.value = false
    }
  }, { once: true })
  frame.srcdoc = html
  document.body.append(frame)
}

onUnmounted(cancelHtml)

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
    <UiDialogContent @interact-outside.prevent @escape-key-down="pending && $event.preventDefault()" @close-auto-focus="returnToEditor">
      <UiDialogHeader>
        <UiDialogTitle>导出简历</UiDialogTitle>
        <UiDialogDescription>导出当前编辑区中的正文。</UiDialogDescription>
      </UiDialogHeader>
      <section aria-labelledby="local-resume-export" class="flex flex-col gap-2">
        <h3 id="local-resume-export" class="font-medium">本地导出</h3>
        <p class="text-sm text-muted-foreground">文件在浏览器中生成。打印和 HTML 用文字标注图片素材。</p>
        <div class="flex flex-wrap gap-2">
          <UiButton type="button" variant="outline" :disabled="htmlBusy" @click="exportHtml('pdf')">打印 / 另存为 PDF</UiButton>
          <UiButton type="button" variant="outline" :disabled="htmlBusy" @click="exportHtml('html')">下载 HTML</UiButton>
          <UiButton type="button" variant="outline" @click="download(title, content, 'md', 'text/markdown;charset=utf-8')">下载 Markdown</UiButton>
        </div>
        <UiField>
          <FieldTitle id="resume-tex-template">LaTeX (.tex) 模板</FieldTitle>
          <UiRadioGroup v-model="texTemplate" aria-labelledby="resume-tex-template" aria-describedby="resume-tex-template-description" class="flex flex-wrap gap-2">
            <label v-for="template in resumeTexTemplates" :key="template.id" class="flex min-h-8 cursor-pointer items-center gap-2 rounded-md border px-2 text-sm">
              <UiRadioGroupItem :id="`resume-tex-template-${template.id}`" :value="template.id" />
              {{ template.label }}
            </label>
          </UiRadioGroup>
          <div aria-live="polite" class="flex flex-col gap-2">
            <FieldDescription id="resume-tex-template-description">{{ resumeTexTemplates[texTemplate].description }}</FieldDescription>
            <UiAlert v-if="texInspection?.warnings.length" role="note">
              <UiAlertDescription>
                <ul class="flex list-disc flex-col gap-1 pl-4">
                  <li v-for="warning in texInspection.warnings" :key="warning">{{ warning }}</li>
                </ul>
              </UiAlertDescription>
            </UiAlert>
          </div>
          <div class="flex flex-wrap gap-2">
            <UiButton v-if="texInspection?.fallbackToBasic" type="button" variant="outline" @click="useBasicTexTemplate">切换为基础模板</UiButton>
            <UiButton type="button" variant="outline" @click="downloadLatex">下载 LaTeX (.tex)</UiButton>
          </div>
        </UiField>
        <p class="text-sm text-muted-foreground">.tex 仅供本地单向导出。请用本机 XeLaTeX 编译并禁用 --shell-escape。不含图片，无法回导版本库。</p>
        <div v-if="texWarnings.length" role="status">
          <ul class="list-disc space-y-1 pl-5 text-sm text-muted-foreground">
            <li v-for="(warning, index) in texWarnings" :key="index" class="wrap-anywhere">{{ warning }}</li>
          </ul>
        </div>
        <template v-if="texErrors.length">
          <p role="alert" class="text-sm text-destructive">LaTeX 下载已取消。请修正以下公式后重试。</p>
          <ul class="space-y-2 text-sm">
            <li v-for="issue in texErrors" :key="issue.start" class="flex flex-wrap items-start justify-between gap-2 border-t pt-2">
              <div class="min-w-0 flex-1 wrap-anywhere">
                <p class="font-medium">第 {{ issue.line }} 行第 {{ issue.column }} 列</p>
                <code>{{ issue.formula.slice(0, 120) }}{{ issue.formula.length > 120 ? '…' : '' }}</code>
                <p>{{ issue.message }}</p>
              </div>
              <UiButton type="button" variant="link" size="sm" :disabled="pending" @click="locateFormula(issue)">定位到公式</UiButton>
            </li>
          </ul>
        </template>
        <p v-if="htmlBusy" role="status" class="text-sm text-muted-foreground">正在准备 HTML 或打印视图…</p>
        <p v-if="htmlError" role="alert" class="text-sm text-destructive">{{ htmlError }}</p>
        <UiButton v-if="sourceFallback" type="button" variant="outline" class="self-start" @click="exportHtml(sourceFallback, false)">保留公式源码并继续</UiButton>
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
