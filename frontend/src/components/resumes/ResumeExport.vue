<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, shallowRef, watch } from 'vue'
import { FieldDescription, FieldTitle } from '@/components/ui/field'
import {
  exportDestination, resumeAssetNames, resumeExportContent, resumeExportFileBase, resumeExportLossNotes, resumeExportResult,
  resumeExportSummary, resumeExportUnavailableReason, resumeHasMath, resumeHtml,
  resumeImageCount, resumePreview, type ResumeExportSource, type ResumeMathRender,
} from '@/lib/resumes'
import { loadResumeMath } from '@/lib/resume-math'
import type { ResumeThemeId } from '@/lib/resume-theme'
import { inspectResumeTexTemplate, resumeLatex, type ResumeTexError } from '@/lib/resume-tex'
import { resumeTexTemplates, type ResumeTexTemplateId } from '@/lib/resume-tex-templates'

const props = defineProps<{
  title: string; content: string; savedContent: string | null; dirty: boolean; compact?: boolean; themeId?: ResumeThemeId
  loadImage: (name: string) => Promise<string | undefined>
}>()
const open = defineModel<boolean>('open', { required: true })
type ResumeRange = Pick<ResumeTexError, 'start' | 'end'>
const emit = defineEmits<{ locate: [range: ResumeRange] }>()
const destination = exportDestination(import.meta.env.VITE_EXPORT_URL, import.meta.env.DEV && import.meta.env.VITE_BOSS_MOCK_PREVIEW === '1')
const docxEnabled = import.meta.env.VITE_RESUME_DOCX_EXPORT === 'true'
const accepted = ref(false)
const format = ref('PDF')
const pending = ref(false)
const error = ref('')
const htmlError = ref('')
const htmlJob = ref<symbol>()
const sourceFallback = ref<'html' | 'pdf'>()
const printing = ref(false)
const htmlBusy = computed(() => !!htmlJob.value || printing.value)
const docxWorker = shallowRef<Worker>()
const docxError = ref('')
const source = ref<ResumeExportSource>('current')
const sourceOptions: { value: ResumeExportSource; label: string }[] = [
  { value: 'current', label: '导出当前内容' },
  { value: 'saved', label: '导出服务器正文' },
  { value: 'save-first', label: '先保存再导出' },
]
const exportContent = computed(() => resumeExportContent(source.value, props.content, props.savedContent))
const omitImages = ref(false)
const embeddedImageCount = ref<number>()
const imageCount = computed(() => resumeImageCount(exportContent.value))
const imageOptions = computed(() => ({
  imageCount: imageCount.value, includeImages: !omitImages.value, embeddedImageCount: embeddedImageCount.value,
}))
const summary = computed(() => resumeExportSummary({
  source: source.value, dirty: props.dirty, savedContent: props.savedContent,
  title: props.title, docxEnabled, ...imageOptions.value,
}))
const sourceReason = (value: ResumeExportSource) =>
  resumeExportUnavailableReason(value, { dirty: props.dirty, savedContent: props.savedContent })
const markdownLoss = computed(() => resumeExportLossNotes('markdown', imageOptions.value))
const docxLoss = computed(() => resumeExportLossNotes('docx', imageOptions.value))
const externalLoss = computed(() => resumeExportLossNotes('external', imageOptions.value))
const texTemplate = ref<ResumeTexTemplateId>('basic')
const texBlocks = computed(() => resumePreview(exportContent.value))
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
  docxError.value = ''
  embeddedImageCount.value = undefined
  sourceFallback.value = undefined
  cancelHtml()
  cancelDocx()
}

watch(open, () => {
  source.value = 'current'
  omitImages.value = false
  accepted.value = false
  error.value = ''
  resultUrl.value = null
  texTemplate.value = 'basic'
  cancelExternal()
  resetLocalExport()
}, { flush: 'sync' })

watch(omitImages, () => { embeddedImageCount.value = undefined })

watch(source, () => {
  accepted.value = false
  error.value = ''
  resultUrl.value = null
  cancelExternal()
  resetLocalExport()
}, { flush: 'sync' })

watch([() => props.content, () => props.title, () => props.savedContent, () => props.compact], () => {
  locateOnClose = undefined
  resetLocalExport()
}, { flush: 'sync' })

watch(() => props.themeId, () => {
  htmlError.value = docxError.value = ''
  sourceFallback.value = undefined
  cancelHtml()
  cancelDocx()
}, { flush: 'sync' })

watch([() => props.dirty, () => props.savedContent], () => {
  if (sourceReason(source.value)) source.value = 'current'
}, { flush: 'sync' })

function download(title: string, content: string, extension: 'html' | 'md' | 'tex', type: string) {
  downloadBlob(title, new Blob([content], { type }), extension)
}

function downloadBlob(title: string, blob: Blob, extension: 'html' | 'md' | 'tex' | 'docx') {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = resumeExportFileBase(title) + '.' + extension
  document.body.append(link)
  try {
    link.click()
  } finally {
    link.remove()
    window.setTimeout(() => URL.revokeObjectURL(url), 1_000)
  }
}

function cancelDocx() {
  docxWorker.value?.terminate()
  docxWorker.value = undefined
}

async function cancelDocxFromDialog() {
  cancelDocx()
  await nextTick()
  document.getElementById('resume-word-export')?.focus()
}

function exportDocx() {
  if (!open.value || !docxEnabled || docxWorker.value) return
  const { title, compact, themeId } = props
  docxError.value = ''
  const fail = () => {
    cancelDocx()
    docxError.value = 'Word 导出失败。请再次选择导出 Word 重试。'
  }
  try {
    const worker = new Worker(new URL('../../lib/resume-docx-worker.ts', import.meta.url), { type: 'module' })
    docxWorker.value = worker
    const failCurrent = () => { if (docxWorker.value === worker) fail() }
    worker.onerror = failCurrent
    worker.onmessageerror = failCurrent
    worker.onmessage = ({ data }: MessageEvent<Blob | null>) => {
      if (docxWorker.value !== worker) return
      if (!data) return fail()
      try {
        downloadBlob(title, data, 'docx')
        cancelDocx()
      } catch {
        fail()
      }
    }
    worker.postMessage({ content: exportContent.value, compact, themeId })
  } catch {
    fail()
  }
}

function downloadLatex() {
  const { title } = props
  const result = resumeLatex(title, texBlocks.value, { sourceMarkdown: exportContent.value, template: texTemplate.value })
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
  const { title, compact, themeId } = props
  const content = exportContent.value
  const job = Symbol()
  htmlJob.value = job
  htmlError.value = ''
  embeddedImageCount.value = undefined
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
    const withImages = !omitImages.value
    const names = withImages ? resumeAssetNames(blocks) : []
    let images: Record<string, string> | undefined
    if (names.length) {
      const loaded = await Promise.all(names.map(async name => [name, await props.loadImage(name)] as const))
      if (htmlJob.value !== job) return
      images = Object.fromEntries(loaded.filter((entry): entry is readonly [string, string] => entry[1] !== undefined))
    }
    const html = resumeHtml(title, blocks, renderMath, { images, includeImages: withImages, compact, themeId })
    if (invalidMath) {
      htmlError.value = '至少有一处公式无法渲染。请修正公式后重试，或下载 Markdown 原文。'
      return
    }
    if (images) embeddedImageCount.value = Object.keys(images).length
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

let exportJob: symbol | undefined
let exportAbort: AbortController | undefined

function cancelExternal() {
  exportJob = undefined
  exportAbort?.abort()
  exportAbort = undefined
  pending.value = false
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

onUnmounted(() => {
  cancelHtml()
  cancelDocx()
  cancelExternal()
})

async function submit() {
  if (!accepted.value || pending.value || complete.value || !destination) return
  const job = Symbol()
  const abort = new AbortController()
  exportJob = job
  exportAbort = abort
  pending.value = true
  error.value = ''
  try {
    const response = await fetch(destination, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'omit',
      redirect: 'error',
      signal: AbortSignal.any([abort.signal, AbortSignal.timeout(20_000)]),
      body: JSON.stringify({ content: exportContent.value, style: '', link: '', name: props.title, type: format.value }),
    })
    if (exportJob !== job) return
    if (!response.ok) throw new Error('Export failed')
    resultUrl.value = resumeExportResult(await response.json())
    if (!resultUrl.value) throw new Error('Missing export result')
  } catch {
    if (exportJob !== job) return
    error.value = '未收到有效导出结果。再次导出会重新发送所选的简历正文。'
  } finally {
    if (exportJob === job) {
      exportJob = undefined
      exportAbort = undefined
      pending.value = false
    }
  }
}
</script>

<template>
  <UiDialog :open="open" @update:open="!pending && (open = $event)">
    <UiDialogContent @interact-outside.prevent @escape-key-down="pending && $event.preventDefault()" @close-auto-focus="returnToEditor">
      <UiDialogHeader>
        <UiDialogTitle>导出简历</UiDialogTitle>
        <UiDialogDescription>选择导出来源与格式。导出文件不包含下方摘要以外的推断。</UiDialogDescription>
      </UiDialogHeader>
      <dl aria-label="导出来源摘要" class="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 border-y py-2 text-sm">
        <dt class="text-muted-foreground">来源</dt><dd>{{ summary.source }}</dd>
        <dt class="text-muted-foreground">未保存</dt><dd>{{ summary.unsaved }}</dd>
        <dt class="text-muted-foreground">打印 / HTML 图片</dt><dd aria-live="polite">{{ summary.images }}</dd>
        <dt class="text-muted-foreground">文件名</dt><dd class="wrap-anywhere">{{ summary.fileName }}</dd>
      </dl>
      <UiField>
        <FieldTitle id="resume-export-source">导出来源</FieldTitle>
        <UiRadioGroup v-model="source" aria-labelledby="resume-export-source" class="flex flex-col gap-2">
          <div v-for="option in sourceOptions" :key="option.value" class="flex flex-col gap-1">
            <label class="flex min-h-8 items-center gap-2 text-sm" :class="sourceReason(option.value) ? 'opacity-60' : 'cursor-pointer'">
              <UiRadioGroupItem :id="`resume-export-source-${option.value}`" :value="option.value" :disabled="Boolean(sourceReason(option.value))" />
              {{ option.label }}
            </label>
            <p v-if="sourceReason(option.value)" class="text-xs text-muted-foreground">{{ sourceReason(option.value) }}</p>
          </div>
        </UiRadioGroup>
      </UiField>
      <section aria-labelledby="local-resume-export" class="flex flex-col gap-2">
        <h3 id="local-resume-export" class="font-medium">本地导出</h3>
        <p class="text-sm text-muted-foreground">文件在浏览器中生成。</p>
        <ul class="list-disc space-y-1 pl-5 text-sm text-muted-foreground">
          <li v-for="note in markdownLoss" :key="note">{{ note }}</li>
        </ul>
        <div id="resume-word-export-hint" class="text-sm text-muted-foreground">
          <p v-for="note in docxLoss" :key="note">{{ note }}</p>
          <p v-if="!docxEnabled">待 Word / WPS 人工验收。</p>
        </div>
        <label v-if="imageCount" class="flex items-center gap-2 text-sm">
          <input v-model="omitImages" :disabled="htmlBusy" type="checkbox">
          打印 / HTML 不含图片（ATS）
        </label>
        <div class="flex flex-wrap gap-2">
          <UiButton type="button" variant="outline" :disabled="htmlBusy" @click="exportHtml('pdf')">打印 / 另存为 PDF</UiButton>
          <UiButton type="button" variant="outline" :disabled="htmlBusy" @click="exportHtml('html')">下载 HTML</UiButton>
          <UiButton type="button" variant="outline" @click="download(title, exportContent, 'md', 'text/markdown;charset=utf-8')">下载 Markdown</UiButton>
          <UiButton id="resume-word-export" type="button" variant="outline" :disabled="!docxEnabled" :aria-disabled="Boolean(docxWorker)" aria-describedby="resume-word-export-hint" @click="exportDocx">导出 Word (.docx)</UiButton>
          <UiButton v-if="docxWorker" type="button" variant="outline" @click="cancelDocxFromDialog">取消 Word 导出</UiButton>
        </div>
        <p v-if="docxWorker" role="status" class="text-sm text-muted-foreground">正在生成 Word 文件…</p>
        <p v-if="docxError" role="alert" class="text-sm text-destructive">{{ docxError }}</p>
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
          <p class="text-sm text-muted-foreground">确认后，{{ summary.source }}和标题将发送至所示服务。</p>
          <ul class="list-disc space-y-1 pl-5 text-sm text-muted-foreground">
            <li v-for="note in externalLoss" :key="note">{{ note }}</li>
          </ul>
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
          我已确认地址并同意发送{{ summary.source }}
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
