<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute } from 'vue-router'
import { RiArrowLeftLine, RiHistoryLine, RiImageLine, RiSaveLine } from '@remixicon/vue'
import { changeResumeVersion, getResume, resumeHistory, type ResumeCommit, type ResumeRecord } from '@/api/resume/resume'
import { ownsResumeAsset, readResumeAsset } from '@/api/resume/assets'
import { useSession } from '@/composables/useSession'
import { assetMarkdown, resumePreview, resumeReadMessage, resumeTime, shortCommit } from '@/lib/resumes'
import { DialogClose, DialogTrigger } from '@/components/ui/dialog'
import ResumeAssets from '@/components/resumes/ResumeAssets.vue'
import ResumeExport from '@/components/resumes/ResumeExport.vue'
import ResumePreview from '@/components/resumes/ResumePreview.vue'

const route = useRoute()
const session = useSession()
const userId = computed(() => session.state.value.user?.id)
const id = computed(() => String(route.params.id))
const record = shallowRef<ResumeRecord | null>(null)
const head = shallowRef<ResumeCommit | null>(null)
const pending = ref(true)
const pendingAction = ref<'save' | 'stash' | null>(null)
const loadError = ref('')
const feedback = ref('')
const writeError = ref('')
const writeLocked = ref(false)
const checkOffered = ref(false)
const checkOpen = ref(false)
const checkPending = ref(false)
const checkError = ref('')
const checkedVersion = shallowRef<{ branch?: string; head: ResumeCommit | null } | null>(null)
const leaveOpen = ref(false)
const markdown = ref('')
const message = ref('')
const mode = ref<'edit' | 'preview'>('edit')
const assetsOpen = ref(false)
const exportOpen = ref(false)
const staged = ref<string[]>([])
const hidden = ref<string[]>([])
const images = shallowRef<Record<string, string>>({})
const imageRequests = new Map<string, symbol>()
const canUseImages = import.meta.env.VITE_RESUME_PRIVATE_IMAGES === 'true' ||
  (import.meta.env.DEV && import.meta.env.VITE_BOSS_MOCK_PREVIEW === '1')
const dirty = computed(() => !!record.value && markdown.value !== (record.value.content ?? ''))
const headUnavailable = computed(() => Boolean(record.value?.headCommit && !head.value))
const restored = computed(() => !!head.value && (head.value.content ?? '') !== (record.value?.content ?? ''))
const hasDraft = computed(() => dirty.value || !!message.value.trim())
const canSave = computed(() => hasDraft.value || restored.value)
const workspaceStatus = computed(() => {
  if (hasDraft.value) return '有未保存的更改'
  if (restored.value) return '已恢复到工作区，尚未提交'
  return head.value ? '工作区正文对应当前 HEAD' : '工作区已读取，尚未核对 HEAD'
})
const assets = computed(() => {
  const keys = resumePreview(markdown.value).flatMap(block => block.kind === 'asset' ? [block.fileName] : [])
  return [...new Set([...keys, ...staged.value])].filter(key =>
    userId.value !== undefined && ownsResumeAsset(userId.value, key) && !hidden.value.includes(key))
})
let loadRevision = 0
let checkRevision = 0
let leaveDecision: ((discard: boolean) => void) | null = null
let draftInput: HTMLElement | null = null
let returnToDraft = false

async function readWorkspace(key: string) {
  const [item, commits] = await Promise.all([getResume(key), resumeHistory(key, 1).catch(() => [])])
  return { item, head: commits.find(commit => commit.id === item.headCommit) ?? null }
}

async function load(force = false) {
  const revision = ++loadRevision
  const owner = session.capture()
  const key = id.value
  pending.value = true
  loadError.value = ''
  try {
    const workspace = await readWorkspace(key)
    if (revision !== loadRevision || !session.owns(owner)) return
    const { item } = workspace
    if (force) {
      record.value = null
      markdown.value = item.content ?? ''
      message.value = ''
      staged.value = hidden.value = []
      feedback.value = writeError.value = ''
      checkOffered.value = checkOpen.value = false
      mode.value = 'edit'
    }
    head.value = workspace.head
    record.value = item
    writeLocked.value = false
  } catch (cause) {
    if (revision === loadRevision && session.owns(owner)) loadError.value = resumeReadMessage(cause)
  } finally {
    if (revision === loadRevision) pending.value = false
  }
}

async function checkLatestVersion() {
  const revision = ++checkRevision
  const key = id.value
  const owner = session.capture()
  const isCurrent = () => revision === checkRevision && checkOpen.value && key === id.value && session.owns(owner)
  checkPending.value = true
  checkError.value = ''
  checkedVersion.value = null
  try {
    const workspace = await readWorkspace(key)
    if (workspace.item.headCommit && !workspace.head) throw new Error('HEAD snapshot is unavailable')
    if (isCurrent()) checkedVersion.value = { branch: workspace.item.currentBranch, head: workspace.head }
  } catch (cause) {
    if (isCurrent()) checkError.value = resumeReadMessage(cause)
  } finally {
    if (isCurrent()) checkPending.value = false
  }
}

watch(checkOpen, open => { if (open) void checkLatestVersion() })

function revokeImage(key: string) {
  imageRequests.delete(key)
  const url = images.value[key]
  if (!url) return
  URL.revokeObjectURL(url)
  const rest = { ...images.value }
  delete rest[key]
  images.value = rest
}

async function fetchImage(key: string) {
  const actor = userId.value
  if (!canUseImages || actor === undefined || images.value[key] || imageRequests.has(key)) return
  const request = Symbol()
  imageRequests.set(key, request)
  const owner = session.capture()
  try {
    const blob = await readResumeAsset(actor, key)
    if (blob && imageRequests.get(key) === request && session.owns(owner)) {
      images.value = { ...images.value, [key]: URL.createObjectURL(blob) }
    }
  } catch {
    // A missing asset does not prevent the document from being edited.
  } finally {
    if (imageRequests.get(key) === request) imageRequests.delete(key)
  }
}

watch(() => JSON.stringify(assets.value), () => {
  const keys = new Set(assets.value)
  for (const key of new Set([...Object.keys(images.value), ...imageRequests.keys()])) {
    if (!keys.has(key)) revokeImage(key)
  }
  for (const key of keys) void fetchImage(key)
})

function uploaded(name: string) {
  hidden.value = hidden.value.filter(key => key !== name)
  revokeImage(name)
  if (!staged.value.includes(name)) staged.value = [...staged.value, name]
  void fetchImage(name)
}

function removed(name: string) {
  hidden.value = [...hidden.value, name]
  staged.value = staged.value.filter(key => key !== name)
  revokeImage(name)
}

function selection() {
  return document.getElementById('resume-markdown') as HTMLTextAreaElement | null
}

function replaceSelection(input: HTMLTextAreaElement, start: number, end: number, text: string, left: number, right = left) {
  input.focus({ preventScroll: true })
  input.setSelectionRange(start, end)
  // insertText keeps programmatic edits in the textarea's native undo history.
  if (!document.execCommand('insertText', false, text)) {
    // shortcut: engines without insertText can edit here without native undo.
    input.setRangeText(text, start, end, 'end')
    input.dispatchEvent(new Event('input', { bubbles: true }))
  }
  input.setSelectionRange(left, right)
}

function insert(before: string, after = '', defaultText = '') {
  if (pendingAction.value) return
  const input = selection()
  if (!input) return
  const { selectionStart: start, selectionEnd: end } = input
  const content = input.value.slice(start, end) || defaultText
  const left = start + before.length
  replaceSelection(input, start, end, before + content + after, left, left + content.length)
}

async function insertAsset(name: string) {
  if (pendingAction.value) return
  mode.value = 'edit'
  await nextTick()
  const input = selection()
  if (!input) return
  const start = input.selectionStart
  const line = '\n' + assetMarkdown(name) + '\n'
  replaceSelection(input, start, start, line, start + line.length)
}

async function write(action: 'save' | 'stash') {
  if (!(action === 'save' ? canSave.value : dirty.value) || pendingAction.value || writeLocked.value) return
  const key = id.value
  const content = markdown.value
  const owner = session.capture()
  pendingAction.value = action
  feedback.value = writeError.value = ''
  checkOffered.value = false
  try {
    let commitId: string | undefined
    if (action === 'save') commitId = await changeResumeVersion(key, 'commit', { message: message.value.trim() || '更新简历', content })
    else await changeResumeVersion(key, 'stash', { content })
    if (!session.owns(owner) || key !== id.value) return
    await load()
    if (!session.owns(owner) || key !== id.value) return
    if (loadError.value) {
      writeLocked.value = true
      writeError.value = '操作已被接收，但暂时无法确认最新状态。'
    } else if (action === 'stash' || (record.value?.headCommit === commitId && record.value?.content === content)) {
      if (action === 'stash') markdown.value = record.value?.content ?? ''
      else message.value = ''
      feedback.value = action === 'stash' ? '草稿已储藏，可在版本历史中取回。' : '简历已保存并提交。'
    } else {
      writeError.value = '最新版本与本次提交不同，草稿仍保留。请核对后再操作。'
    }
  } catch {
    if (session.owns(owner) && key === id.value) {
      await load()
      if (!session.owns(owner) || key !== id.value) return
      writeLocked.value = Boolean(loadError.value)
      writeError.value = action === 'save'
        ? '提交结果未确认，当前正文和备注仍保留。请先核对最新版本。'
        : '储藏结果未确认，当前正文仍保留。请先核对储藏记录。'
    }
  } finally {
    checkOffered.value = action === 'save' && Boolean(writeError.value) && session.owns(owner) && key === id.value
    pendingAction.value = null
  }
}

function blockNavigation() {
  if (session.state.value.invalidated) return true
  if (pendingAction.value || leaveDecision) return false
  if (!hasDraft.value) return true
  return new Promise<boolean>(resolve => {
    leaveDecision = resolve
    leaveOpen.value = true
  })
}

function rememberDraftFocus(event: FocusEvent) {
  draftInput = event.target as HTMLElement
}

function finishNavigation(discard: boolean) {
  const resolve = leaveDecision
  leaveDecision = null
  returnToDraft = !discard
  leaveOpen.value = false
  resolve?.(discard)
}

function stashFromLeave() {
  finishNavigation(false)
  void write('stash')
}

function restoreDraftFocus(event: Event) {
  event.preventDefault()
  if (!returnToDraft) return
  returnToDraft = false
  const target = draftInput?.isConnected ? draftInput : selection()
  target?.focus({ preventScroll: true })
}

function beforeUnload(event: BeforeUnloadEvent) {
  if (hasDraft.value || pendingAction.value) {
    event.preventDefault()
    event.returnValue = ''
  }
}

onBeforeRouteLeave(blockNavigation)
onBeforeRouteUpdate((to, from) => to.params.id === from.params.id || blockNavigation())
watch(id, () => { record.value = null; void load(true) })
onMounted(() => {
  void load(true)
  window.addEventListener('beforeunload', beforeUnload)
})
onUnmounted(() => {
  leaveDecision?.(false)
  loadRevision++
  checkRevision++
  window.removeEventListener('beforeunload', beforeUnload)
  imageRequests.clear()
  for (const key of Object.keys(images.value)) revokeImage(key)
})
</script>

<template>
  <section class="flex min-w-0 flex-col gap-5" aria-labelledby="resume-edit-title">
    <header class="flex flex-wrap items-start justify-between gap-3">
      <div class="flex min-w-0 flex-col gap-2">
        <UiButton as-child variant="ghost" size="sm" class="self-start"><RouterLink to="/resumes"><RiArrowLeftLine data-icon="inline-start" />我的简历</RouterLink></UiButton>
        <h1 id="resume-edit-title" class="break-words text-2xl font-semibold tracking-tight">{{ record?.title ?? '编辑简历' }}</h1>
        <div v-if="record" class="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          <UiBadge variant="secondary">{{ record.currentBranch ?? '未知分支' }}</UiBadge>
          <span role="status">{{ workspaceStatus }}</span>
        </div>
      </div>
      <div v-if="record" class="flex flex-wrap items-center gap-2">
        <UiButton variant="outline" :disabled="!dirty || Boolean(pendingAction) || writeLocked" @click="write('stash')">储藏</UiButton>
        <UiButton as-child variant="outline"><RouterLink :to="'/resumes/' + id + '/versions'"><RiHistoryLine data-icon="inline-start" />版本历史</RouterLink></UiButton>
        <UiButton variant="outline" :disabled="Boolean(pendingAction)" @click="assetsOpen = true"><RiImageLine data-icon="inline-start" />图片素材</UiButton>
        <UiButton variant="outline" @click="exportOpen = true">外部导出</UiButton>
        <UiButton :disabled="!canSave || Boolean(pendingAction) || writeLocked" @click="write('save')"><RiSaveLine data-icon="inline-start" />{{ pendingAction === 'save' ? '正在提交…' : '保存并提交' }}</UiButton>
      </div>
    </header>

    <UiDialog v-model:open="checkOpen">
      <UiAlert v-if="loadError || writeError || headUnavailable" variant="destructive" role="alert">
        <UiAlertDescription class="flex flex-wrap items-center gap-2">
          {{ writeError || loadError || '暂时无法核对当前版本。重新读取会保留编辑中的正文和备注。' }}
          <DialogTrigger v-if="checkOffered" as-child>
            <UiButton variant="outline" size="sm">核对最新版本</UiButton>
          </DialogTrigger>
          <UiButton v-if="loadError || writeLocked || headUnavailable" variant="outline" size="sm" :disabled="pending || pendingAction !== null" @click="load(!record)">重新读取</UiButton>
        </UiAlertDescription>
      </UiAlert>
      <UiDialogContent class="sm:max-w-xl">
        <UiDialogHeader>
          <UiDialogTitle>核对最新版本</UiDialogTitle>
          <UiDialogDescription>查看当前 HEAD 的已提交快照。编辑区的正文和备注会保留。</UiDialogDescription>
        </UiDialogHeader>
        <p v-if="checkPending" role="status">正在读取最新版本…</p>
        <p v-else-if="checkError" role="alert">{{ checkError }}</p>
        <template v-else-if="checkedVersion">
          <template v-if="checkedVersion.head">
            <div class="flex flex-col gap-1 break-words">
              <p class="font-medium">{{ checkedVersion.head.message || '无提交备注' }}</p>
              <p class="text-xs text-muted-foreground">
                {{ checkedVersion.branch ?? '未知分支' }} ·
                <span class="font-mono">HEAD {{ shortCommit(checkedVersion.head.id) }}</span> ·
                {{ resumeTime(checkedVersion.head.commitTime) }}
              </p>
            </div>
            <pre class="max-h-64 overflow-auto whitespace-pre-wrap break-words border p-3">{{ checkedVersion.head.content || '（正文为空）' }}</pre>
          </template>
          <p v-else>当前没有已提交版本。</p>
        </template>
        <UiDialogFooter>
          <UiButton v-if="checkError" variant="outline" @click="checkLatestVersion">重新核对</UiButton>
          <DialogClose as-child><UiButton variant="outline">返回编辑</UiButton></DialogClose>
        </UiDialogFooter>
      </UiDialogContent>
    </UiDialog>
    <UiDialog :open="leaveOpen" @update:open="!$event && finishNavigation(false)">
      <UiDialogContent :show-close-button="false" @interact-outside.prevent @close-auto-focus="restoreDraftFocus">
        <UiDialogHeader>
          <UiDialogTitle>丢弃未保存的更改？</UiDialogTitle>
          <UiDialogDescription>
            丢弃后，重新打开时会读取服务器上的工作区正文，本次改动和备注将丢失。
            <template v-if="dirty && !writeLocked">你也可以先储藏正文，备注会留在编辑页。</template>
          </UiDialogDescription>
        </UiDialogHeader>
        <UiDialogFooter>
          <UiButton variant="outline" @click="finishNavigation(false)">继续编辑</UiButton>
          <UiButton v-if="dirty && !writeLocked" variant="outline" @click="stashFromLeave">储藏正文</UiButton>
          <UiButton variant="destructive" @click="finishNavigation(true)">丢弃更改</UiButton>
        </UiDialogFooter>
      </UiDialogContent>
    </UiDialog>
    <p v-if="feedback" role="status" class="text-sm text-muted-foreground">{{ feedback }}</p>
    <UiSkeleton v-if="pending && !record" role="status" aria-label="正在读取简历" class="h-96" />
    <template v-if="record">
      <div class="flex gap-2 md:hidden" role="group" aria-label="简历工作区">
        <UiButton size="sm" variant="outline" :aria-pressed="mode === 'edit'" @click="mode = 'edit'">编辑</UiButton>
        <UiButton size="sm" variant="outline" :aria-pressed="mode === 'preview'" @click="mode = 'preview'">预览</UiButton>
      </div>
      <div class="grid min-w-0 gap-4 md:grid-cols-2">
        <div class="min-w-0 border bg-card" :class="{ 'max-md:hidden': mode === 'preview' }">
          <div class="flex flex-wrap items-center justify-between gap-2 border-b px-4 py-3">
            <h2 class="font-medium">Markdown 编辑</h2><span class="text-xs text-muted-foreground">纯文本 · UTF-8</span>
          </div>
          <div class="flex flex-wrap gap-1 border-b px-3 py-2" role="group" aria-label="Markdown 工具栏">
            <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @mousedown.prevent @click="insert('## ', '', '小节标题')">标题</UiButton>
            <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @mousedown.prevent @click="insert('**', '**', '加粗文字')">加粗</UiButton>
            <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @mousedown.prevent @click="insert('- ', '', '列表项')">列表</UiButton>
            <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @mousedown.prevent @click="insert('\n---\n')">分割线</UiButton>
            <UiButton size="sm" variant="ghost" @click="assetsOpen = true">插入图片</UiButton>
          </div>
          <UiField class="p-3">
            <UiFieldLabel for="resume-markdown" class="sr-only">简历正文</UiFieldLabel>
            <UiTextarea id="resume-markdown" v-model="markdown" class="min-h-[24rem] font-mono leading-relaxed" :disabled="Boolean(pendingAction)" spellcheck="false" @focus="rememberDraftFocus" />
          </UiField>
          <div class="flex flex-col gap-3 border-t p-3">
            <UiField>
              <UiFieldLabel for="resume-message">提交备注</UiFieldLabel>
              <UiInput id="resume-message" v-model="message" maxlength="160" placeholder="简单描述本次修改" :disabled="Boolean(pendingAction)" @focus="rememberDraftFocus" />
            </UiField>
            <p class="flex flex-wrap gap-3 text-xs tabular-nums text-muted-foreground">
              <span>{{ markdown.length }} 字符</span>
              <span>分支 {{ record.currentBranch ?? '—' }}</span>
              <span class="font-mono">HEAD {{ shortCommit(record.headCommit) }}</span>
            </p>
          </div>
        </div>
        <div class="min-w-0 border bg-muted/40" :class="{ 'max-md:hidden': mode === 'edit' }">
          <div class="flex justify-between gap-2 border-b bg-card px-4 py-3">
            <h2 class="font-medium">实时预览</h2><span class="text-xs text-muted-foreground">A4 · 纵向</span>
          </div>
          <div class="p-3 sm:p-5"><ResumePreview :content="markdown" :images="images" /></div>
        </div>
      </div>
      <ResumeAssets v-model:open="assetsOpen" :enabled="canUseImages" :user-id="userId" :assets="assets" :images="images" @uploaded="uploaded" @removed="removed" @insert="insertAsset" />
      <ResumeExport v-model:open="exportOpen" :title="record.title" :content="markdown" />
    </template>
  </section>
</template>
