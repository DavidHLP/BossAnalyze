<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { RiArrowLeftLine, RiHistoryLine, RiSaveLine } from '@remixicon/vue'
import {
  changeResumeVersion, createResume, getResume, listResumes, resumeHistory, updateResumeAppearance,
  type ResumeCommit, type ResumeRecord,
} from '@/api/resume/resume'
import { ownsResumeAsset } from '@/api/resume/assets'
import { useResumeImages } from '@/composables/useResumeImages'
import { useSession } from '@/composables/useSession'
import { registerDirtyDraft } from '@/composables/session-events'
import { mutationResultUnknown } from '@/lib/rbac-mutations'
import { resumeCopyContent, resumeCopySourceLabel, resumeCopyTitle, resumeCopyVerified } from '@/lib/resume-copy'
import { resumePaginationSuggestions, type ResumePagination, type ResumePaginationSuggestion } from '@/lib/resume-pagination'
import { resolveResumeThemeId, resumeThemes, type ResumeThemeId } from '@/lib/resume-theme'
import {
  assetMarkdown, resumeAssetNames, resumeConflictCopyTitle, resumeDraftStore, resumePreview, resumeReadMessage, resumeTime,
  resumeWriteConflict, shortCommit, type ResumeConflictState, type ResumeDraft,
} from '@/lib/resumes'
import { analyzeResumeQuality } from '@/lib/resume-quality'
import { DialogClose, DialogTrigger } from '@/components/ui/dialog'
import ResumeAssets from '@/components/resumes/ResumeAssets.vue'
import ResumeExport from '@/components/resumes/ResumeExport.vue'
import ResumeForm from '@/components/resumes/ResumeForm.vue'
import ResumePreview from '@/components/resumes/ResumePreview.vue'

const route = useRoute()
const router = useRouter()
const session = useSession()
const { crossTabChanged } = session
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
const submissionLocked = computed(() => writeLocked.value || crossTabChanged.value !== null)
const checkOffered = ref(false)
const checkOpen = ref(false)
const checkPending = ref(false)
const checkError = ref('')
const checkedVersion = shallowRef<{ branch?: string; head: ResumeCommit | null } | null>(null)
const leaveOpen = ref(false)
const markdown = ref('')
const message = ref('')
const localDraft = shallowRef<ResumeDraft | null>(null)
const draftSavedAt = ref('')
const draftUnavailable = ref(false)
const savedAt = ref('')
const savedCommit = ref('')
const restoreOpen = ref(false)
const restorePending = ref(false)
const restoreError = ref('')
const conflictOpen = ref(false)
const conflictState = shallowRef<ResumeConflictState | null>(null)
const conflictPending = ref<'server' | 'copy' | null>(null)
const conflictError = ref('')
const copyOpen = ref(false)
const copyPending = ref(false)
const copyError = ref('')
const copyNote = ref('')
const copyTitle = ref('')
const copyUnverified = ref(false)
const copyCheck = shallowRef<{ title: string; content: string; id: string | null } | null>(null)
const copyPreview = computed(() => resumeCopyContent(record.value?.content ?? ''))
const copySource = computed(() => resumeCopySourceLabel(dirty.value))
const account = computed(() => {
  const user = session.state.value.user
  if (user?.id !== undefined) return 'id:' + user.id
  return user?.email ? 'email:' + user.email : null
})
let draftStore: ReturnType<typeof resumeDraftStore> | undefined
let draftOwner: ReturnType<typeof session.capture> | undefined
const mode = ref<'edit' | 'preview'>('edit')
const editorMode = ref<'form' | 'markdown'>('form')
const assetsOpen = ref(false)
const exportOpen = ref(false)
const compact = ref(false)
const themeId = ref<ResumeThemeId>('basic')
const themeWrite = shallowRef<Promise<void>>()
const themeError = ref('')
const pagination = shallowRef<ResumePagination | null>(null)
const paginationSuggestions = computed(() => (pagination.value?.pages ?? 0) > 1
  ? resumePaginationSuggestions(markdown.value) : [])
const paginationSuggestionLabels: Record<ResumePaginationSuggestion['kind'], string> = {
  'oldest-experience': '考虑删减最早一段工作经历',
  'merge-points': '考虑合并相近要点',
  'shorten-summary': '考虑缩短个人摘要',
}
const staged = ref<string[]>([])
const hidden = ref<string[]>([])
const dirty = computed(() => !!record.value && markdown.value !== (record.value.content ?? ''))
const headUnavailable = computed(() => Boolean(record.value?.headCommit && !head.value))
const restored = computed(() => !!head.value && (head.value.content ?? '') !== (record.value?.content ?? ''))
const hasDraft = computed(() => dirty.value || !!message.value.trim())
const canSave = computed(() => !localDraft.value && (hasDraft.value || restored.value))
const workspaceStatus = computed(() => {
  if (hasDraft.value) return '有未保存的更改'
  if (restored.value) return '已恢复到编辑区，尚未保存'
  return head.value ? '编辑区正文对应当前版本' : '内容已读取，尚未核对当前版本'
})
const assets = computed(() => {
  const keys = resumeAssetNames(resumePreview(markdown.value))
  return [...new Set([...keys, ...staged.value])].filter(key =>
    userId.value !== undefined && ownsResumeAsset(userId.value, key) && !hidden.value.includes(key))
})
const qualityOpen = ref(false)
const ignoredQuality = ref<string[]>([])
const quality = computed(() => analyzeResumeQuality(markdown.value))
const qualityItems = computed(() => quality.value.items.filter(item => !ignoredQuality.value.includes(item.id)))

function ignoreQuality(id: string) {
  if (!ignoredQuality.value.includes(id)) ignoredQuality.value = [...ignoredQuality.value, id]
}

function locateQuality(item: { start: number; end: number }) {
  void locateResumeRange(item)
}
const { enabled: canUseImages, images, loadImage, forgetImage } = useResumeImages(assets)
let loadRevision = 0
let themeRevision = 0
let checkRevision = 0
let leaveDecision: ((discard: boolean) => void) | null = null
let draftInput: HTMLElement | null = null
let returnToDraft = false
let draftTimer: ReturnType<typeof setTimeout> | undefined

function clearLocalDraft() {
  if (draftTimer !== undefined) {
    clearTimeout(draftTimer)
    draftTimer = undefined
  }
  try {
    draftStore?.clear()
  } catch {
    draftUnavailable.value = true
  }
  localDraft.value = null
  draftSavedAt.value = ''
}

function offerLocalDraft(owner: ReturnType<typeof session.capture>, key: string) {
  draftStore = undefined
  draftOwner = owner
  localDraft.value = null
  draftUnavailable.value = false
  if (!account.value || !owner.timestamp) return
  try {
    draftStore = resumeDraftStore(window.sessionStorage, account.value, owner.timestamp, key)
    const saved = draftStore.read()
    if (saved && (saved.content !== markdown.value || saved.message.trim())) localDraft.value = saved
    else draftStore.clear()
  } catch {
    // Blocked or full storage leaves editing and server saves usable.
    draftUnavailable.value = true
  }
}

function persistDraft() {
  if (!draftStore || !draftOwner || !session.owns(draftOwner) ||
    !record.value || record.value.id !== id.value || localDraft.value) return
  try {
    if (hasDraft.value) {
      draftStore.write({
        content: markdown.value,
        message: message.value,
        baseHeadCommit: record.value.headCommit ?? null,
      })
      draftSavedAt.value = new Date().toTimeString().slice(0, 5)
      draftUnavailable.value = false
    } else {
      draftStore.clear()
      draftSavedAt.value = ''
    }
  } catch {
    draftSavedAt.value = ''
    draftUnavailable.value = true
  }
}

function flushDraft() {
  if (draftTimer === undefined) return
  clearTimeout(draftTimer)
  draftTimer = undefined
  persistDraft()
}

function pageHidden() {
  if (document.visibilityState === 'hidden') flushDraft()
}

async function resolveLocalDraft(restore: boolean) {
  const saved = localDraft.value
  if (!saved) return
  if (restore) {
    localDraft.value = null
    markdown.value = saved.content
    message.value = saved.message
  } else {
    clearLocalDraft()
  }
  mode.value = 'edit'
  await nextTick()
  selection()?.focus({ preventScroll: true })
}

watch([markdown, message], () => {
  savedAt.value = ''
  if (!record.value || record.value.id !== id.value || localDraft.value || !draftOwner || !session.owns(draftOwner)) return
  if (draftTimer !== undefined) clearTimeout(draftTimer)
  draftTimer = setTimeout(() => {
    draftTimer = undefined
    persistDraft()
  }, 300)
}, { flush: 'sync' })

watch(account, () => {
  clearLocalDraft()
  draftStore = undefined
  draftOwner = undefined
  loadRevision++
  checkRevision++
  record.value = head.value = null
  markdown.value = message.value = ''
  compact.value = false
  pagination.value = null
  savedAt.value = ''
  restoreOpen.value = false
  restoreError.value = ''
  restorePending.value = false
  conflictOpen.value = false
  conflictState.value = null
  conflictError.value = ''
  conflictPending.value = null
  assetsOpen.value = exportOpen.value = checkOpen.value = false
  ignoredQuality.value = []
  if (account.value) void load(true)
}, { flush: 'sync' })

async function readWorkspace(key: string) {
  const [item, commits] = await Promise.all([getResume(key), resumeHistory(key, 1).catch(() => [])])
  return { item, head: commits.find(commit => commit.id === item.headCommit) ?? null }
}

async function load(force = false) {
  const revision = ++loadRevision
  const owner = session.capture()
  const key = id.value
  const appearanceRevision = themeRevision
  const themeReadStartedDuringWrite = Boolean(themeWrite.value)
  pending.value = true
  loadError.value = ''
  try {
    if (force && themeWrite.value) await themeWrite.value
    const workspace = await readWorkspace(key)
    if (revision !== loadRevision || !session.owns(owner)) return
    const { item } = workspace
    if (force) {
      record.value = null
      markdown.value = item.content ?? ''
      message.value = ''
      staged.value = hidden.value = []
      feedback.value = writeError.value = ''
      themeError.value = ''
      checkOffered.value = checkOpen.value = false
      mode.value = 'edit'
      offerLocalDraft(owner, key)
    }
    head.value = workspace.head
    record.value = item
    if (force || (!themeReadStartedDuringWrite && appearanceRevision === themeRevision)) {
      themeId.value = resolveResumeThemeId(item.themeId)
    }
    writeLocked.value = false
  } catch (cause) {
    if (revision === loadRevision && session.owns(owner)) loadError.value = resumeReadMessage(cause)
  } finally {
    if (revision === loadRevision) pending.value = false
  }
}

async function changeTheme(next: ResumeThemeId) {
  const owner = session.capture()
  const key = id.value
  if (record.value?.id !== key || !session.owns(owner) || themeWrite.value || themeId.value === next) return
  const previous = themeId.value
  themeRevision++
  themeId.value = next
  themeError.value = ''
  const write = updateResumeAppearance(key, next).catch(() => {
    if (key === id.value && session.owns(owner)) {
      themeId.value = previous
      themeError.value = '主题保存失败，已恢复上一套主题。'
    }
  })
  themeWrite.value = write
  await write
  themeWrite.value = undefined
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
    if (workspace.item.headCommit && !workspace.head) throw new Error('Current version snapshot is unavailable')
    if (isCurrent()) checkedVersion.value = { branch: workspace.item.currentBranch, head: workspace.head }
  } catch (cause) {
    if (isCurrent()) checkError.value = resumeReadMessage(cause)
  } finally {
    if (isCurrent()) checkPending.value = false
  }
}

watch(checkOpen, open => { if (open) void checkLatestVersion() })

function uploaded(name: string) {
  hidden.value = hidden.value.filter(key => key !== name)
  forgetImage(name)
  if (!staged.value.includes(name)) staged.value = [...staged.value, name]
  void loadImage(name)
}

function removed(name: string) {
  hidden.value = [...hidden.value, name]
  staged.value = staged.value.filter(key => key !== name)
  forgetImage(name)
}

function selection() {
  return document.getElementById('resume-markdown') as HTMLTextAreaElement | null
}

function scrollToResumeSelection(input: HTMLTextAreaElement, start: number) {
  // An autosizing textarea can extend past the viewport, so measure the selected line with wrapping.
  const styles = getComputedStyle(input)
  const mirror = document.createElement('div')
  Object.assign(mirror.style, {
    position: 'fixed', visibility: 'hidden', boxSizing: 'border-box', width: `${input.clientWidth}px`,
    padding: styles.padding, font: styles.font, letterSpacing: styles.letterSpacing, tabSize: styles.tabSize,
    whiteSpace: styles.whiteSpace, overflowWrap: styles.overflowWrap, wordBreak: styles.wordBreak,
  })
  const line = document.createElement('span')
  line.textContent = input.value.slice(start).split('\n', 1)[0] || '\u200b'
  mirror.append(input.value.slice(0, start), line)
  document.body.append(mirror)
  const offset = line.getBoundingClientRect().top - mirror.getBoundingClientRect().top
  mirror.remove()

  input.scrollTop = offset - input.clientHeight / 2
  const top = input.getBoundingClientRect().top + input.clientTop + offset - input.scrollTop
  window.scrollBy({ top: top - window.innerHeight / 2, behavior: 'instant' })
}

async function locateResumeRange({ start, end }: { start: number; end: number }) {
  exportOpen.value = false
  mode.value = 'edit'
  editorMode.value = 'markdown'
  await nextTick()
  const input = selection()
  if (!input) return
  // The textarea normalizes line endings while stored Markdown may retain CRLF.
  const offset = (index: number) => Math.min(input.value.length,
    markdown.value.slice(0, Math.max(0, index)).replace(/\r\n?/g, '\n').length)
  const first = offset(start)
  input.focus({ preventScroll: true })
  input.setSelectionRange(first, Math.max(first, offset(end)))
  scrollToResumeSelection(input, first)
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
  if (pendingAction.value || localDraft.value) return
  const input = selection()
  if (!input) return
  const { selectionStart: start, selectionEnd: end } = input
  const content = input.value.slice(start, end) || defaultText
  const left = start + before.length
  replaceSelection(input, start, end, before + content + after, left, left + content.length)
}

async function insertAsset(name: string) {
  if (pendingAction.value || localDraft.value) return
  mode.value = 'edit'
  editorMode.value = 'markdown'
  await nextTick()
  const input = selection()
  if (!input) return
  const start = input.selectionStart
  const line = '\n' + assetMarkdown(name) + '\n'
  replaceSelection(input, start, start, line, start + line.length)
}

function stashReadback(before: ResumeRecord | null, after: ResumeRecord | null, content: string) {
  if (!Array.isArray(before?.stashStack) || !Array.isArray(after?.stashStack)) return false
  function matchingTimestamps(stack: unknown[]) {
    return stack.flatMap(entry => {
      if (!entry || typeof entry !== 'object' || !('content' in entry) || entry.content !== content) return []
      if (before?.currentBranch !== undefined && 'branch' in entry && entry.branch !== before.currentBranch) return []
      if (before?.headCommit !== undefined && 'head' in entry && entry.head !== before.headCommit) return []
      const timestamp = 'timestamp' in entry ? entry.timestamp : undefined
      return [typeof timestamp === 'string' || typeof timestamp === 'number' ? timestamp : null]
    })
  }
  const previous = matchingTimestamps(before.stashStack)
  const current = matchingTimestamps(after.stashStack)
  return current.length > previous.length || (previous.every(time => time !== null) &&
    current.some(time => time !== null && !previous.includes(time)))
}

async function write(action: 'save' | 'stash') {
  const owner = draftOwner
  if (!owner || !session.owns(owner)) return false
  if (!(action === 'save' ? canSave.value : dirty.value) || localDraft.value || pendingAction.value || writeLocked.value) return false
  const key = id.value
  const content = markdown.value
  const beforeWrite = record.value
  const revision = record.value?.workspaceRevision
  let confirmed = false
  pendingAction.value = action
  feedback.value = writeError.value = savedAt.value = ''
  checkOffered.value = false
  try {
    let commitId: string | undefined
    if (action === 'save') {
      commitId = await changeResumeVersion(key, 'commit', {
        message: message.value.trim() || '更新简历', content,
      }, revision)
    } else {
      await changeResumeVersion(key, 'stash', { content }, revision)
    }
    if (!session.owns(owner) || key !== id.value) return
    await load()
    if (!session.owns(owner) || key !== id.value) return
    if (loadError.value) {
      writeLocked.value = true
      writeError.value = '操作已被接收，但暂时无法确认最新状态。'
    } else if (action === 'stash'
      ? stashReadback(beforeWrite, record.value, content)
      : record.value?.headCommit === commitId && record.value?.content === content) {
      if (action === 'stash') markdown.value = record.value?.content ?? ''
      else message.value = ''
      if (action === 'stash' && message.value.trim() && draftOwner && session.owns(draftOwner)) {
        persistDraft()
      } else {
        clearLocalDraft()
      }
      if (action === 'save') {
        savedAt.value = new Date().toTimeString().slice(0, 5)
        savedCommit.value = shortCommit(commitId ?? record.value?.headCommit)
      }
      feedback.value = action === 'stash' ? '草稿已暂存，可在版本历史中取回。' : '简历已保存。'
      confirmed = true
    } else {
      writeLocked.value = action === 'stash'
      writeError.value = action === 'stash'
        ? '暂存结果未确认，请先核对暂存记录。'
        : '当前版本与本次保存不同，草稿仍保留。请核对后再操作。'
    }
  } catch (cause) {
    if (session.owns(owner) && key === id.value) {
      const conflict = resumeWriteConflict(cause)
      if (conflict) {
        // Keep the editor and draft untouched; the user decides how to reconcile.
        conflictState.value = conflict
        conflictError.value = ''
        conflictOpen.value = true
        persistDraft()
      } else {
        await load()
        if (!session.owns(owner) || key !== id.value) return
        writeLocked.value = action === 'stash' || Boolean(loadError.value)
        writeError.value = action === 'save'
          ? '保存结果未确认，当前正文和说明仍保留。请先核对当前版本。'
          : '暂存结果未确认，当前正文仍保留。请先核对暂存记录。'
      }
    }
  } finally {
    checkOffered.value = action === 'save' && Boolean(writeError.value) && session.owns(owner) && key === id.value
    pendingAction.value = null
  }
  return confirmed
}

function openRestore() {
  if (pendingAction.value || localDraft.value) return
  restoreError.value = ''
  restoreOpen.value = true
}

async function restoreServerContent(stashFirst: boolean) {
  if (restorePending.value || pendingAction.value || localDraft.value) return
  const owner = session.capture()
  const key = id.value
  restorePending.value = true
  restoreError.value = ''
  try {
    if (stashFirst) {
      const stashed = await write('stash')
      if (!session.owns(owner) || key !== id.value) return
      if (!stashed) {
        restoreError.value = '当前修改未能暂存，编辑区保持不变。'
        return
      }
    }
    // Reload metadata with the body so the next write uses the revision that was just read.
    await load()
    if (!session.owns(owner) || key !== id.value) return
    if (loadError.value) {
      restoreError.value = loadError.value
      return
    }
    markdown.value = record.value?.content ?? ''
    savedAt.value = ''
    clearLocalDraft()
    if (hasDraft.value) persistDraft()
    restoreOpen.value = false
  } catch (cause) {
    if (session.owns(owner) && key === id.value) restoreError.value = resumeReadMessage(cause)
  } finally {
    restorePending.value = false
  }
}

async function resolveConflictWithServer(stashFirst: boolean) {
  if (conflictPending.value || pendingAction.value) return
  const owner = session.capture()
  const key = id.value
  const revision = conflictState.value?.workspaceRevision
  conflictPending.value = 'server'
  conflictError.value = ''
  try {
    if (stashFirst && revision != null) {
      // Rebasing the baseline first lets the readback confirm the new stash entry.
      const item = await getResume(key)
      if (!session.owns(owner) || key !== id.value) return
      record.value = item
    }
    await restoreServerContent(stashFirst && revision != null)
    if (!session.owns(owner) || key !== id.value) return
    if (restoreError.value) {
      conflictError.value = restoreError.value
      return
    }
    conflictOpen.value = false
    conflictState.value = null
  } finally {
    conflictPending.value = null
  }
}

async function saveConflictCopy() {
  if (conflictPending.value || pendingAction.value) return
  const owner = session.capture()
  const key = id.value
  const title = resumeConflictCopyTitle(record.value?.title ?? '')
  conflictPending.value = 'copy'
  conflictError.value = ''
  try {
    const copy = await createResume(title, markdown.value)
    if (!session.owns(owner) || key !== id.value) return
    conflictOpen.value = false
    conflictState.value = null
    clearLocalDraft()
    message.value = ''
    // The copy holds this text now; do not ask the leave guard about it.
    if (record.value) record.value = { ...record.value, content: markdown.value }
    await router.push('/resumes/' + copy.id + '/edit')
  } catch (cause) {
    if (session.owns(owner) && key === id.value) {
      conflictError.value = cause && typeof cause === 'object' && 'resultUnknown' in cause && cause.resultUnknown
        ? '副本创建结果未确认，请先刷新简历列表核对，正文仍保留在编辑区。'
        : '另存副本失败，请重试或选择使用服务器版本。'
    }
  } finally {
    conflictPending.value = null
  }
}

function openCopy() {
  if (!record.value || pendingAction.value) return
  // Keep an unconfirmed attempt so reopening the dialog cannot create a duplicate.
  if (!copyUnverified.value) {
    copyTitle.value = resumeCopyTitle(record.value.title)
    copyCheck.value = null
  }
  copyError.value = copyNote.value = ''
  copyOpen.value = true
}

async function verifyCopy(check: NonNullable<typeof copyCheck.value>, owner: ReturnType<typeof session.capture>) {
  try {
    let target = check.id
    if (target) {
      const [item, commits] = await Promise.all([getResume(target), resumeHistory(target, 2)])
      if (!session.owns(owner) || !resumeCopyVerified(item, check.title, check.content, commits)) return false
    } else {
      // Without an ID the create reply was lost, so confirm through the server list.
      const items = await listResumes()
      const match = items.find(item => item.id !== id.value &&
        item.title === check.title && (item.content ?? '') === check.content)
      if (!session.owns(owner) || !match) return false
      target = match.id
    }
    copyOpen.value = false
    copyUnverified.value = false
    copyNote.value = '简历副本已创建。'
    await router.push('/resumes/' + target + '/edit')
    return true
  } catch {
    return false
  }
}

async function createCopy() {
  const current = record.value
  if (copyPending.value || !current || writeLocked.value) return
  const name = copyTitle.value.trim()
  if (!name) {
    copyError.value = '请输入副本标题。'
    return
  }
  const check = { title: name, content: resumeCopyContent(current.content ?? '').content, id: null as string | null }
  copyCheck.value = check
  copyPending.value = true
  copyError.value = ''
  const owner = session.capture()
  try {
    try {
      check.id = (await createResume(check.title, check.content)).id
    } catch (cause) {
      if (!session.owns(owner)) return
      if (mutationResultUnknown(cause)) {
        copyUnverified.value = true
        copyError.value = '创建结果未知，标题已保留。正在刷新核对，确认前请勿重复创建。'
        if (!(await verifyCopy(check, owner)) && session.owns(owner)) {
          copyError.value = '暂未发现副本。请刷新核对，确认未生成后再重试，避免重复创建。'
        }
      } else {
        copyError.value = '复制失败，请核对后重试。'
      }
      return
    }
    if (!session.owns(owner)) return
    if (!(await verifyCopy(check, owner))) {
      copyUnverified.value = true
      copyError.value = '副本已受理，但未能回读确认标题、正文或初始版本。请刷新核对。'
    }
  } finally {
    if (session.owns(owner)) copyPending.value = false
  }
}

async function retryCopy() {
  const check = copyCheck.value
  if (!check || copyPending.value) return
  copyPending.value = true
  copyError.value = ''
  const owner = session.capture()
  try {
    if (await verifyCopy(check, owner)) return
    if (session.owns(owner)) copyError.value = '仍未回读确认副本。请到我的简历核对后再操作，避免重复创建。'
  } finally {
    if (session.owns(owner)) copyPending.value = false
  }
}

async function saveThenCopy() {
  if (copyPending.value || !dirty.value || localDraft.value) return
  copyPending.value = true
  copyError.value = ''
  try {
    await write('save')
    if (!dirty.value && !writeError.value && !writeLocked.value) {
      copyPending.value = false
      await createCopy()
      return
    }
    copyError.value = '保存未回读确认，尚未创建副本。请核对当前版本后重试。'
  } finally {
    copyPending.value = false
  }
}

async function blockNavigation() {
  if (session.state.value.invalidated) return true
  if (themeWrite.value) await themeWrite.value
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
  if (discard) clearLocalDraft()
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
watch(id, () => {
  record.value = null
  compact.value = false
  pagination.value = null
  restoreOpen.value = false
  restoreError.value = ''
  restorePending.value = false
  conflictOpen.value = false
  conflictState.value = null
  conflictError.value = ''
  conflictPending.value = null
  savedAt.value = ''
  ignoredQuality.value = []
  void load(true)
})
let unregisterDraft: (() => void) | undefined
onMounted(() => {
  unregisterDraft = registerDirtyDraft('resume-editor', () => hasDraft.value || localDraft.value !== null)
  void load(true)
  window.addEventListener('beforeunload', beforeUnload)
  window.addEventListener('pagehide', flushDraft)
  document.addEventListener('visibilitychange', pageHidden)
})
onUnmounted(() => {
  unregisterDraft?.()
  leaveDecision?.(false)
  flushDraft()
  loadRevision++
  checkRevision++
  window.removeEventListener('beforeunload', beforeUnload)
  window.removeEventListener('pagehide', flushDraft)
  document.removeEventListener('visibilitychange', pageHidden)
})
</script>

<template>
  <section class="flex min-w-0 flex-col gap-5 pb-24 md:pb-0" aria-labelledby="resume-edit-title">
    <header class="flex flex-wrap items-start justify-between gap-3">
      <div class="flex min-w-0 flex-col gap-2">
        <UiButton as-child variant="ghost" size="sm" class="self-start"><RouterLink to="/resumes"><RiArrowLeftLine data-icon="inline-start" />我的简历</RouterLink></UiButton>
        <h1 id="resume-edit-title" class="break-words text-2xl font-semibold tracking-tight">{{ record?.title ?? '编辑简历' }}</h1>
        <div v-if="record" class="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
          <span role="status">{{ workspaceStatus }}</span>
          <span v-if="draftUnavailable" role="status">本地草稿不可用</span>
          <span v-else-if="draftSavedAt" role="status">已自动暂存 {{ draftSavedAt }}</span>
        </div>
      </div>
      <div v-if="record" class="hidden flex-wrap items-center gap-2 md:flex">
        <UiButton as-child variant="ghost"><RouterLink :to="'/resumes/' + id + '/versions'"><RiHistoryLine data-icon="inline-start" />版本历史</RouterLink></UiButton>
        <UiButton variant="outline" @click="exportOpen = true">导出</UiButton>
        <UiButton :disabled="!canSave || Boolean(pendingAction) || submissionLocked" @click="write('save')"><RiSaveLine data-icon="inline-start" />{{ pendingAction === 'save' ? '正在保存…' : '保存' }}</UiButton>
        <UiDropdownMenu>
          <UiDropdownMenuTrigger as-child>
            <UiButton variant="outline" aria-label="更多操作">更多</UiButton>
          </UiDropdownMenuTrigger>
          <UiDropdownMenuContent align="end" class="w-48">
            <UiDropdownMenuItem :disabled="!dirty || Boolean(pendingAction) || submissionLocked" @select="write('stash')">暂存草稿</UiDropdownMenuItem>
            <UiDropdownMenuItem :disabled="Boolean(pendingAction) || Boolean(localDraft)" @select="openRestore">恢复服务器正文</UiDropdownMenuItem>
            <UiDropdownMenuItem :disabled="Boolean(pendingAction)" @select="openCopy">复制简历</UiDropdownMenuItem>
            <UiDropdownMenuItem :disabled="Boolean(pendingAction)" @select="assetsOpen = true">图片素材</UiDropdownMenuItem>
          </UiDropdownMenuContent>
        </UiDropdownMenu>
      </div>
    </header>

    <UiDialog v-model:open="checkOpen">
      <UiAlert v-if="loadError || writeError || headUnavailable" variant="destructive" role="alert">
        <UiAlertDescription class="flex flex-wrap items-center gap-2">
          {{ writeError || loadError || '暂时无法核对当前版本。重新读取会保留编辑中的正文和说明。' }}
          <DialogTrigger v-if="checkOffered" as-child>
            <UiButton variant="outline" size="sm">核对最新版本</UiButton>
          </DialogTrigger>
          <UiButton v-if="loadError || writeLocked || headUnavailable" variant="outline" size="sm" :disabled="pending || pendingAction !== null" @click="load(!record)">重新读取</UiButton>
        </UiAlertDescription>
      </UiAlert>
      <UiDialogContent class="sm:max-w-xl">
        <UiDialogHeader>
          <UiDialogTitle>核对最新版本</UiDialogTitle>
          <UiDialogDescription>查看当前版本的已保存内容。编辑区的正文和说明会保留。</UiDialogDescription>
        </UiDialogHeader>
        <p v-if="checkPending" role="status">正在读取最新版本…</p>
        <p v-else-if="checkError" role="alert">{{ checkError }}</p>
        <template v-else-if="checkedVersion">
          <template v-if="checkedVersion.head">
            <div class="flex flex-col gap-1 break-words">
              <p class="font-medium">{{ checkedVersion.head.message || '暂无修改说明' }}</p>
              <p class="text-xs text-muted-foreground">
                <span class="font-mono">当前版本 {{ shortCommit(checkedVersion.head.id) }}</span> ·
                {{ resumeTime(checkedVersion.head.commitTime) }}
              </p>
            </div>
            <pre class="max-h-64 overflow-auto whitespace-pre-wrap break-words border p-3">{{ checkedVersion.head.content || '（正文为空）' }}</pre>
          </template>
          <p v-else>当前没有已保存版本。</p>
        </template>
        <UiDialogFooter>
          <UiButton v-if="checkError" variant="outline" @click="checkLatestVersion">重新核对</UiButton>
          <DialogClose as-child><UiButton variant="outline">返回编辑</UiButton></DialogClose>
        </UiDialogFooter>
      </UiDialogContent>
    </UiDialog>
    <UiDialog v-model:open="conflictOpen">
      <UiDialogContent>
        <UiDialogHeader>
          <UiDialogTitle>保存冲突</UiDialogTitle>
          <UiDialogDescription>
            另一个标签页或设备已保存新版本。你的正文和修改说明仍保留在编辑区和本地草稿中，请选择如何处理。
          </UiDialogDescription>
        </UiDialogHeader>
        <p v-if="conflictState?.headCommit" class="text-xs text-muted-foreground">
          服务器最新版本 <span class="font-mono">{{ shortCommit(conflictState.headCommit) }}</span>
        </p>
        <p v-if="conflictError" role="alert">{{ conflictError }}</p>
        <UiDialogFooter class="flex flex-wrap justify-end gap-2">
          <UiButton variant="outline" :disabled="Boolean(conflictPending)" @click="conflictOpen = false">继续编辑</UiButton>
          <UiButton variant="outline" :disabled="Boolean(conflictPending)" @click="saveConflictCopy">
            {{ conflictPending === 'copy' ? '正在另存…' : '保留我的修改并另存为副本' }}
          </UiButton>
          <UiButton
            v-if="dirty && !submissionLocked && conflictState?.workspaceRevision != null"
            variant="outline"
            :disabled="Boolean(conflictPending)"
            @click="resolveConflictWithServer(true)"
          >暂存我的修改并使用服务器版本</UiButton>
          <UiButton :disabled="Boolean(conflictPending)" @click="resolveConflictWithServer(false)">
            {{ conflictPending === 'server' ? '正在读取…' : '查看并使用服务器版本' }}
          </UiButton>
        </UiDialogFooter>
      </UiDialogContent>
    </UiDialog>
    <UiDialog :open="leaveOpen" @update:open="!$event && finishNavigation(false)">
      <UiDialogContent :show-close-button="false" @interact-outside.prevent @close-auto-focus="restoreDraftFocus">
        <UiDialogHeader>
          <UiDialogTitle>丢弃未保存的更改？</UiDialogTitle>
          <UiDialogDescription>
            丢弃后，重新打开时会读取服务器上的正文，本次改动和说明将丢失。
            <template v-if="dirty && !submissionLocked">你也可以先暂存正文，修改说明会留在编辑页。</template>
          </UiDialogDescription>
        </UiDialogHeader>
        <UiDialogFooter>
          <UiButton variant="outline" @click="finishNavigation(false)">继续编辑</UiButton>
          <UiButton v-if="dirty && !submissionLocked" variant="outline" @click="stashFromLeave">暂存正文</UiButton>
          <UiButton variant="destructive" @click="finishNavigation(true)">丢弃更改</UiButton>
        </UiDialogFooter>
      </UiDialogContent>
    </UiDialog>
    <UiDialog v-model:open="restoreOpen">
      <UiDialogContent>
        <UiDialogHeader>
          <UiDialogTitle>恢复服务器正文？</UiDialogTitle>
          <UiDialogDescription>
            编辑区将替换为服务器当前保存的正文。这不会创建、重置或检出版本。
          </UiDialogDescription>
        </UiDialogHeader>
        <p v-if="restoreError" role="alert">{{ restoreError }}</p>
        <UiDialogFooter>
          <UiButton variant="outline" :disabled="restorePending" @click="restoreOpen = false">取消</UiButton>
          <UiButton v-if="dirty && !submissionLocked" variant="outline" :disabled="restorePending" @click="restoreServerContent(true)">暂存当前修改并恢复</UiButton>
          <UiButton variant="destructive" :disabled="restorePending" @click="restoreServerContent(false)">
            {{ restorePending ? '正在读取…' : '放弃修改并恢复' }}
          </UiButton>
        </UiDialogFooter>
      </UiDialogContent>
    </UiDialog>
    <UiDialog v-model:open="copyOpen">
      <UiDialogContent @interact-outside.prevent @escape-key-down="copyPending && $event.preventDefault()">
        <UiDialogHeader>
          <UiDialogTitle>复制简历</UiDialogTitle>
          <UiDialogDescription>以服务器已保存正文创建一份独立简历和一条初始版本，原简历不变。副本可使用新标题，创建后继续编辑。</UiDialogDescription>
        </UiDialogHeader>
        <div class="flex flex-col gap-4">
          <UiAlert>
            <UiAlertDescription>
              复制来源：{{ copySource }}。
              <template v-if="copyPreview.imageCount">{{ copyPreview.imageCount }} 张图片不会随副本复制，正文中保留文字占位。</template>
              <template v-else>图片素材为用户级素材，不会随副本复制。</template>
            </UiAlertDescription>
          </UiAlert>
          <UiAlert v-if="dirty" role="status">
            <UiAlertDescription>当前有未保存修改，副本不会包含这些修改。可先保存再复制，或明确复制服务器版本。</UiAlertDescription>
          </UiAlert>
          <UiField>
            <UiFieldLabel for="resume-copy-title">副本标题</UiFieldLabel>
            <UiInput id="resume-copy-title" v-model="copyTitle" maxlength="120" :disabled="copyPending || copyUnverified" required />
          </UiField>
          <p v-if="copyError" role="alert" class="text-sm text-destructive">{{ copyError }}</p>
        </div>
        <UiDialogFooter>
          <UiButton variant="outline" :disabled="copyPending" @click="copyOpen = false">取消</UiButton>
          <UiButton v-if="copyUnverified" variant="outline" :disabled="copyPending" @click="retryCopy">重新核对</UiButton>
          <template v-else>
            <UiButton v-if="dirty && !localDraft" variant="outline" :disabled="copyPending || Boolean(pendingAction)" @click="saveThenCopy">先保存再复制</UiButton>
            <UiButton :disabled="copyPending || Boolean(pendingAction)" @click="createCopy">{{ copyPending ? '正在创建…' : '复制服务器版本' }}</UiButton>
          </template>
        </UiDialogFooter>
      </UiDialogContent>
    </UiDialog>
    <p v-if="copyNote" role="status" class="text-sm text-muted-foreground">{{ copyNote }}</p>
    <p v-if="feedback" role="status" class="text-sm text-muted-foreground">{{ feedback }}</p>
    <p v-if="savedAt" role="status" class="text-sm text-muted-foreground">已保存 {{ savedAt }} · 版本 {{ savedCommit }}</p>
    <UiSkeleton v-if="pending && !record" role="status" aria-label="正在读取简历" class="h-96" />
    <template v-if="record">
      <UiAlert v-if="localDraft" role="status">
        <UiAlertTitle>检测到本地草稿</UiAlertTitle>
        <UiAlertDescription class="flex flex-col items-start gap-2">
          恢复会将暂存的正文和修改说明填入编辑区。你可以在编辑后保存或暂存。
          <div class="flex flex-wrap gap-2">
            <UiButton type="button" size="sm" @click="resolveLocalDraft(true)">恢复本地草稿</UiButton>
            <UiButton type="button" size="sm" variant="outline" @click="resolveLocalDraft(false)">忽略并清除</UiButton>
          </div>
        </UiAlertDescription>
      </UiAlert>
      <div class="flex gap-2 md:hidden" role="group" aria-label="简历工作区">
        <UiButton size="sm" variant="outline" :aria-pressed="mode === 'edit'" @click="mode = 'edit'">编辑</UiButton>
        <UiButton size="sm" variant="outline" :aria-pressed="mode === 'preview'" @click="mode = 'preview'">预览</UiButton>
      </div>
      <div class="grid min-w-0 gap-4 md:grid-cols-2">
        <div class="min-w-0 border bg-card" :class="{ 'max-md:hidden': mode === 'preview' }">
          <div class="flex flex-wrap items-center justify-between gap-2 border-b px-4 py-3">
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="font-medium">简历编辑</h2>
              <div class="flex gap-1" role="group" aria-label="编辑方式">
                <UiButton size="sm" variant="outline" :aria-pressed="editorMode === 'form'" @click="editorMode = 'form'">表单</UiButton>
                <UiButton size="sm" variant="outline" :aria-pressed="editorMode === 'markdown'" @click="editorMode = 'markdown'">高级 Markdown</UiButton>
              </div>
            </div>
            <span class="text-xs text-muted-foreground">{{ editorMode === 'form' ? '基本信息 · 教育 · 工作' : '纯文本 · UTF-8' }}</span>
          </div>
          <ResumeForm v-if="editorMode === 'form'" v-model="markdown" :disabled="Boolean(pendingAction || localDraft)" />
          <template v-else>
            <fieldset :disabled="localDraft !== null" class="flex flex-wrap gap-1 border-b px-3 py-2" aria-label="Markdown 工具栏">
              <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @mousedown.prevent @click="insert('## ', '', '小节标题')">标题</UiButton>
              <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @mousedown.prevent @click="insert('**', '**', '加粗文字')">加粗</UiButton>
              <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @mousedown.prevent @click="insert('- ', '', '列表项')">列表</UiButton>
              <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @mousedown.prevent @click="insert('\n---\n')">分割线</UiButton>
              <UiButton size="sm" variant="ghost" @click="assetsOpen = true">插入图片</UiButton>
            </fieldset>
            <UiField class="p-3">
              <UiFieldLabel for="resume-markdown" class="sr-only">简历正文</UiFieldLabel>
              <UiTextarea id="resume-markdown" v-model="markdown" class="min-h-[24rem] font-mono leading-relaxed" :readonly="Boolean(pendingAction || localDraft)" spellcheck="false" @focus="rememberDraftFocus" />
            </UiField>
          </template>
          <div class="flex flex-col gap-3 border-t p-3">
            <UiField>
              <UiFieldLabel for="resume-message">修改说明（可选）</UiFieldLabel>
              <UiInput id="resume-message" v-model="message" maxlength="160" placeholder="简单描述本次修改" :disabled="Boolean(pendingAction || localDraft)" @focus="rememberDraftFocus" />
            </UiField>
            <p class="flex flex-wrap gap-3 text-xs tabular-nums text-muted-foreground">
              <span>{{ markdown.length }} 字符</span>
              <span class="font-mono">当前版本 {{ shortCommit(record.headCommit) }}</span>
            </p>
          </div>
          <UiCollapsible v-model:open="qualityOpen" class="border-t">
            <UiCollapsibleTrigger as-child>
              <UiButton variant="ghost" class="w-full justify-between rounded-none px-4 py-3">
                <span class="font-medium">可补充项</span>
                <span class="text-xs font-normal text-muted-foreground">
                  {{ !quality.hasContent ? '等待填写' : qualityItems.length ? qualityItems.length + ' 条' : '暂无' }}
                </span>
              </UiButton>
            </UiCollapsibleTrigger>
            <UiCollapsibleContent>
              <div class="flex flex-col gap-3 px-4 pb-4">
                <p v-if="!quality.hasContent" class="text-sm text-muted-foreground">
                  正文还是空的。填入内容后，这里会按章节给出可补充项；它不会阻止保存或导出。
                </p>
                <template v-else>
                  <p v-if="!qualityItems.length" class="text-sm text-muted-foreground">暂时没有可补充项。</p>
                  <ul v-else class="flex flex-col gap-3">
                    <li v-for="item in qualityItems" :key="item.id" class="flex flex-col gap-1">
                      <p class="text-sm">{{ item.message }}</p>
                      <div class="flex flex-wrap gap-2">
                        <UiButton type="button" size="sm" variant="outline" @click="locateQuality(item)">定位</UiButton>
                        <UiButton type="button" size="sm" variant="ghost" @click="ignoreQuality(item.id)">忽略</UiButton>
                      </div>
                    </li>
                  </ul>
                  <p class="text-xs text-muted-foreground">
                    字数口径：汉字 {{ quality.wordCount.han }} + 英文词 {{ quality.wordCount.words }} =
                    {{ quality.wordCount.total }}，不计标点、Markdown 符号、代码与链接；标点 {{ quality.wordCount.punctuation }}。
                  </p>
                </template>
              </div>
            </UiCollapsibleContent>
          </UiCollapsible>
        </div>
        <div class="min-w-0 border bg-muted/40" :class="{ 'max-md:hidden': mode === 'edit' }">
          <div class="flex flex-wrap items-center justify-between gap-2 border-b bg-card px-4 py-3">
            <h2 class="font-medium">实时预览</h2>
            <div class="flex flex-wrap items-center gap-2">
              <div class="inline-flex gap-1" role="group" aria-label="简历主题">
                <UiButton v-for="(theme, option) in resumeThemes" :key="option" type="button" size="sm"
                  :variant="themeId === option ? 'secondary' : 'ghost'" :aria-pressed="themeId === option"
                  :disabled="Boolean(themeWrite) || crossTabChanged !== null" @click="changeTheme(option)">
                  {{ theme.label }}
                </UiButton>
              </div>
              <span role="status" class="text-xs tabular-nums text-muted-foreground">
                {{ pagination ? `约 ${pagination.pages} 页` : '页数待确认' }}
                <template v-if="pagination?.overflowLines"> · 超出约 {{ pagination.overflowLines }} 行</template>
              </span>
              <UiButton type="button" size="sm" variant="outline" :aria-pressed="compact" @click="compact = !compact">紧凑排版</UiButton>
            </div>
            <p v-if="themeError" role="alert" class="w-full text-sm text-destructive">{{ themeError }}</p>
          </div>
          <ul v-if="paginationSuggestions.length" aria-label="缩短简历的建议" class="flex flex-wrap gap-x-2 border-b bg-card px-3 py-1">
            <li v-for="suggestion in paginationSuggestions" :key="suggestion.kind">
              <UiButton type="button" size="sm" variant="link" @click="locateResumeRange(suggestion)">
                {{ paginationSuggestionLabels[suggestion.kind] }}
              </UiButton>
            </li>
          </ul>
          <div class="p-3 sm:p-5">
            <ResumePreview :content="markdown" :images="images" :compact="compact" :theme-id="themeId" estimate-pages @pagination="pagination = $event" />
          </div>
        </div>
      </div>
      <ResumeAssets v-model:open="assetsOpen" :enabled="canUseImages" :user-id="userId" :assets="assets" :images="images" @uploaded="uploaded" @removed="removed" @insert="insertAsset" />
      <ResumeExport v-model:open="exportOpen" :title="record.title" :content="markdown" :saved-content="record.content" :dirty="dirty" :compact="compact" :theme-id="themeId" :load-image="loadImage" @locate="locateResumeRange" />
    </template>
    <div v-if="record" class="fixed inset-x-0 bottom-0 z-40 flex items-center gap-2 border-t bg-background/95 p-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))] backdrop-blur md:hidden" role="group" aria-label="简历操作">
      <UiButton class="flex-1" :disabled="!canSave || Boolean(pendingAction) || submissionLocked" @click="write('save')"><RiSaveLine data-icon="inline-start" />{{ pendingAction === 'save' ? '正在保存…' : '保存' }}</UiButton>
      <UiButton variant="outline" @click="exportOpen = true">导出</UiButton>
      <UiDropdownMenu>
        <UiDropdownMenuTrigger as-child>
          <UiButton variant="outline" aria-label="更多操作">更多</UiButton>
        </UiDropdownMenuTrigger>
        <UiDropdownMenuContent align="end" :side="'top'" class="w-48">
          <UiDropdownMenuItem :disabled="!dirty || Boolean(pendingAction) || submissionLocked" @select="write('stash')">暂存草稿</UiDropdownMenuItem>
          <UiDropdownMenuItem :disabled="Boolean(pendingAction)" @select="openCopy">复制简历</UiDropdownMenuItem>
          <UiDropdownMenuItem :disabled="Boolean(pendingAction)" @select="assetsOpen = true">图片素材</UiDropdownMenuItem>
          <UiDropdownMenuItem as-child>
            <RouterLink :to="'/resumes/' + id + '/versions'">版本历史</RouterLink>
          </UiDropdownMenuItem>
        </UiDropdownMenuContent>
      </UiDropdownMenu>
    </div>
  </section>
</template>
