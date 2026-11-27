<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { RiAddLine, RiArrowLeftLine } from '@remixicon/vue'
import {
  changeResumeVersion, getResume, inspectCommit, inspectResume, resumeHistory,
  type ResumeCommit, type ResumeRecord, type ResumeValidation,
} from '@/api/resume/resume'
import { useSession } from '@/composables/useSession'
import { useResumeImages } from '@/composables/useResumeImages'
import { restoreActionFocus } from '@/lib/rbac-mutations'
import {
  countDuplicateCommits, resumeAssetNames, resumeFirstParentRelation, resumeLineDiff, resumeMainline, resumePreview, resumeReadMessage, resumeTime,
  resumeWriteConflict, shortCommit,
} from '@/lib/resumes'
import ResumePreview from '@/components/resumes/ResumePreview.vue'

type Action = 'branch' | 'switch' | 'merge' | 'stash' | 'stash/pop' | 'checkout' | 'reset' | 'revert'
const actions: Record<Action, { title: string; description: string; destructive?: boolean }> = {
  branch: { title: '新建版本线', description: '新版本线将从当前版本建立。' },
  switch: { title: '切换版本线', description: '编辑区内容将更新为目标版本线的最新内容。' },
  merge: { title: '合并版本线', description: '使用源版本线内容创建合并版本并更新目标版本线。系统不会自动合并正文冲突。' },
  stash: { title: '暂存草稿', description: '下方已填入当前编辑区正文。可在暂存前编辑，暂存不会创建新版本。' },
  'stash/pop': { title: '取回最近暂存', description: '将取回最近暂存的正文到编辑区。当前版本保持不变，直至你保存取回后的正文。' },
  checkout: { title: '恢复到此版本', description: '此操作会用所选版本的内容替换当前编辑区内容。' },
  reset: { title: '回退到此版本', description: '将当前版本线和编辑区回退至所选版本。后续版本可能不再出现在当前版本线上，且会改变版本记录。', destructive: true },
  revert: { title: '以该版本内容新建保存', description: '用所选版本的正文创建一个新版本，保留原有版本记录。' },
}
const route = useRoute()
const router = useRouter()
const session = useSession()
const id = computed(() => String(route.params.id))
const editPath = computed(() => '/resumes/' + id.value + '/edit')
const mobile = useMediaQuery('(max-width: 767px)')
const record = shallowRef<ResumeRecord | null>(null)
const history = shallowRef<ResumeCommit[]>([])
const recent = shallowRef<ResumeCommit[]>([])
const range = shallowRef<ResumeCommit[] | null>(null)
const comparison = shallowRef<{ from: ResumeCommit; to: ResumeCommit } | null>(null)
const validation = shallowRef<ResumeValidation | null>(null)
const debug = shallowRef<Record<string, unknown> | null>(null)
const selection = shallowRef<ResumeCommit | null>(null)
const panel = ref<'snapshot' | 'inspect' | null>(null)
const snapshotImageNames = computed(() => panel.value === 'snapshot'
  ? resumeAssetNames(resumePreview(selection.value?.content ?? '')) : [])
const { images } = useResumeImages(snapshotImageNames)
const operation = ref<Action | null>(null)
const action = computed(() => operation.value ? actions[operation.value] : null)
const from = ref('')
const to = ref('')
const source = ref('')
const target = ref('')
const draft = ref('')
const branchName = ref('')
const filter = ref('')
const mode = ref<'all' | 'recent'>('all')
const advancedOpen = ref(false)
const rangeForm = ref<HTMLElement | null>(null)
const busy = ref('')
const loadError = ref('')
const detailError = ref('')
const recentReadError = '无法读取最近版本，请重试。'
const feedback = ref('')
const confirmed = ref(false)
const mainHeading = ref<HTMLElement | null>(null)
const writeLocked = ref(false)
let trigger: HTMLElement | null = null
let panelTrigger: HTMLElement | null = null
let revision = 0
const branchTips = computed(() => record.value?.branches ?? {})
const branches = computed(() => Object.keys(branchTips.value))
const duplicates = computed(() => countDuplicateCommits(history.value))
const items = computed(() => (mode.value === 'all' ? history.value : recent.value)
  .filter(commit => !filter.value || filter.value === commit.branch))
const sourceSnapshot = computed(() => history.value.find(commit => commit.id === branchTips.value[source.value]))
const stashCount = computed(() => record.value?.stashStack?.length ?? 0)
const mainline = computed(() => resumeMainline(branchTips.value[record.value?.currentBranch ?? ''], history.value))
const canReset = computed(() => !!selection.value && mainline.value.has(selection.value.id))
const checkoutBranches = computed(() => Object.entries(branchTips.value)
  .filter(([, tip]) => tip === selection.value?.id).map(([name]) => name))
const revertSnapshot = computed(() => history.value.find(commit => commit.id === selection.value?.parentCommits?.[0]))
const rangeRelation = computed(() => comparison.value
  ? resumeFirstParentRelation(comparison.value.from.id, comparison.value.to.id, history.value)
  : null)
const lineDiff = computed(() => comparison.value
  ? resumeLineDiff(comparison.value.from.content ?? '', comparison.value.to.content ?? '')
  : null)
const lineMarkers = { added: '+', removed: '−', unchanged: ' ' }
const lineLabels = { added: '新增', removed: '删除', unchanged: '未变' }
const lineColors = { added: 'bg-primary/10', removed: 'bg-destructive/10', unchanged: '' }
const canConfirm = computed(() => {
  if (!operation.value || busy.value || writeLocked.value) return false
  if (operation.value === 'branch') return !!branchName.value.trim() && !branches.value.includes(branchName.value.trim())
  if (operation.value === 'switch') return !!target.value && target.value !== record.value?.currentBranch
  if (operation.value === 'merge') return !!sourceSnapshot.value && !!target.value && target.value !== source.value && confirmed.value
  if (operation.value === 'reset') return canReset.value
  if (operation.value === 'revert') return !!revertSnapshot.value && !!record.value?.headCommit
  return true
})

watch([source, target], () => { confirmed.value = false }, { flush: 'sync' })
watch([from, to], () => { range.value = comparison.value = null })

async function read(reset = false) {
  const generation = revision
  const owner = session.capture()
  const key = id.value
  const isCurrent = () => generation === revision && key === id.value && session.owns(owner)
  loadError.value = ''
  range.value = comparison.value = null
  if (reset) {
    record.value = null
    history.value = recent.value = []
    range.value = validation.value = null
    panel.value = operation.value = null
    mode.value = 'all'
    filter.value = from.value = to.value = feedback.value = ''
  }
  try {
    const refreshRecent = mode.value === 'recent'
    if (refreshRecent) recent.value = []
    const [item, commits, latest] = await Promise.all([
      getResume(key), resumeHistory(key), refreshRecent ? resumeHistory(key, true).catch(() => null) : undefined,
    ])
    if (!isCurrent()) return false
    record.value = item
    history.value = commits
    if (latest === null) detailError.value = recentReadError
    else if (latest) recent.value = latest
    writeLocked.value = false
    return true
  } catch (cause) {
    if (isCurrent()) {
      loadError.value = resumeReadMessage(cause)
      writeLocked.value = true
    }
    return false
  }
}

async function refresh(reset = false) {
  if (busy.value && !reset) return
  const generation = reset ? ++revision : revision
  busy.value = 'load'
  detailError.value = ''
  try {
    await read(reset)
  } finally {
    if (generation === revision) busy.value = ''
  }
}

watch(id, () => void refresh(true))
onMounted(() => void refresh(true))
onUnmounted(() => { revision++ })
onBeforeRouteLeave(() => !busy.value.startsWith('write') || session.state.value.invalidated === true)
onBeforeRouteUpdate((to, from) => to.params.id === from.params.id || !busy.value.startsWith('write'))

async function readRecent() {
  if (busy.value) return
  const generation = revision
  busy.value = 'recent'
  detailError.value = ''
  const key = id.value
  const owner = session.capture()
  try {
    const commits = await resumeHistory(key, true)
    if (generation === revision && key === id.value && session.owns(owner)) {
      recent.value = commits
      mode.value = 'recent'
    }
  } catch {
    if (generation === revision) detailError.value = recentReadError
  } finally {
    if (generation === revision) busy.value = ''
  }
}

async function readRange() {
  if (from.value === to.value || busy.value) return
  const start = history.value.find(item => item.id === from.value)
  const end = history.value.find(item => item.id === to.value)
  if (!start || !end) return
  detailError.value = ''
  range.value = null
  comparison.value = { from: start, to: end }
  if (rangeRelation.value !== 'forward') return
  const generation = revision
  busy.value = 'range'
  const key = id.value
  const owner = session.capture()
  try {
    const commits = await resumeHistory(key, false, { fromCommit: start.id, toCommit: end.id })
    if (generation === revision && key === id.value && session.owns(owner)) range.value = commits
  } catch {
    if (generation === revision) detailError.value = '暂时无法读取版本区间。'
  } finally {
    if (generation === revision) busy.value = ''
  }
}

async function inspect(commit?: ResumeCommit, event?: MouseEvent) {
  if (busy.value) return
  const generation = revision
  if (event) panelTrigger = event.currentTarget as HTMLElement
  busy.value = 'inspect'
  detailError.value = ''
  const key = id.value
  const owner = session.capture()
  const isCurrent = () => generation === revision && key === id.value && session.owns(owner)
  try {
    if (commit) {
      const result = await inspectCommit(key, commit.id)
      if (!isCurrent()) return
      debug.value = result
      selection.value = commit
      panel.value = 'inspect'
    } else {
      if (!(await read()) || !isCurrent()) return
      const result = await inspectResume(key)
      if (!isCurrent()) return
      validation.value = result
      selection.value = null
      panel.value = 'inspect'
    }
  } catch {
    if (generation === revision) detailError.value = '暂时无法完成版本诊断，请重试。'
  } finally {
    if (generation === revision) busy.value = ''
  }
}

function showSnapshot(commit: ResumeCommit, event: MouseEvent) {
  panelTrigger = event.currentTarget as HTMLElement
  selection.value = commit
  panel.value = 'snapshot'
}

function pickCompare(commit: ResumeCommit) {
  if (from.value === commit.id) return
  if (!from.value || to.value) {
    from.value = commit.id
    to.value = ''
  } else {
    to.value = commit.id
  }
  void nextTick(() => rangeForm.value?.scrollIntoView?.({ block: 'start', behavior: 'smooth' }))
}

function open(kind: Action, event: MouseEvent, commit?: ResumeCommit) {
  if (busy.value || writeLocked.value) return
  trigger = event.currentTarget as HTMLElement
  selection.value = commit ?? null
  branchName.value = source.value = ''
  target.value = record.value?.currentBranch ?? ''
  draft.value = record.value?.content ?? ''
  confirmed.value = false
  operation.value = kind
}

async function submit() {
  const command = operation.value
  const commit = selection.value
  if (!command || !canConfirm.value) return
  busy.value = 'write'
  detailError.value = feedback.value = ''
  const key = id.value
  const owner = session.capture()
  const previous = record.value
  const previousStash = stashCount.value
  const revision = record.value?.workspaceRevision
  let result: string | undefined
  try {
    if (command === 'branch') await changeResumeVersion(key, 'branch', { name: branchName.value.trim() }, revision)
    else if (command === 'switch') await changeResumeVersion(key, 'switch', { branch: target.value }, revision)
    else if (command === 'merge') result = await changeResumeVersion(key, 'merge', { source: source.value, target: target.value }, revision)
    else if (command === 'stash') await changeResumeVersion(key, 'stash', { content: draft.value }, revision)
    else if (command === 'stash/pop') result = await changeResumeVersion(key, 'stash/pop', undefined, revision)
    else if (commit) result = await changeResumeVersion(key, command, { commitId: commit.id }, revision)
    if (key !== id.value || !session.owns(owner)) return
    operation.value = null
    mode.value = 'all'
    range.value = validation.value = null
    panel.value = null
    if (command === 'stash/pop' && typeof result === 'string') {
      busy.value = ''
      await router.push(editPath.value)
      return
    }
    const updated = await read()
    const current = record.value
    const verified = updated && current && (() => {
      switch (command) {
        case 'branch': return current.branches?.[branchName.value.trim()] === previous?.headCommit
        case 'switch': return current.currentBranch === target.value
        case 'merge': return current.branches?.[target.value] === result
        case 'stash': return stashCount.value > previousStash
        case 'checkout': return current.headCommit === commit?.id
        case 'reset': return current.headCommit === commit?.id && current.branches?.[current.currentBranch ?? ''] === commit?.id
        case 'revert': return current.headCommit === result
        default: return false
      }
    })()
    if (verified) feedback.value = '操作成功，已重新读取版本状态。'
    else detailError.value = '操作已被接收，重读时未能确认预期结果。请核对版本状态。'
  } catch (cause) {
    if (key === id.value && session.owns(owner)) {
      const conflict = resumeWriteConflict(cause)
      range.value = validation.value = null
      await read()
      operation.value = null
      detailError.value = conflict
        ? '另一个标签页或设备已保存新版本，本次操作未执行。请核对最新版本后重试。'
        : '操作结果未确认，已尝试重读版本。请核对后再操作。'
    }
  } finally {
    busy.value = ''
  }
}
</script>

<template>
  <section class="flex min-w-0 flex-col gap-5" aria-labelledby="resume-versions-title">
    <header class="flex min-w-0 flex-col gap-2">
      <UiButton as-child variant="ghost" size="sm" class="self-start"><RouterLink :to="editPath"><RiArrowLeftLine data-icon="inline-start" />返回编辑器</RouterLink></UiButton>
      <h1 id="resume-versions-title" ref="mainHeading" tabindex="-1" class="text-3xl font-semibold tracking-tight">版本历史</h1>
      <p v-if="record" class="break-words text-sm text-muted-foreground">{{ record.title }}</p>
      <p v-if="record" class="text-xs text-muted-foreground">当前版本 {{ shortCommit(record.headCommit) }}</p>
    </header>

    <UiCollapsible v-if="record" v-model:open="advancedOpen" class="border bg-card">
      <UiCollapsibleTrigger as-child>
        <UiButton variant="ghost" class="w-full justify-between"><span>高级版本操作</span><span class="text-xs text-muted-foreground">{{ advancedOpen ? '收起' : '展开' }}</span></UiButton>
      </UiCollapsibleTrigger>
      <UiCollapsibleContent>
        <div class="flex flex-col gap-4 border-t p-4">
          <div class="flex flex-col gap-2">
            <h2 class="font-medium">版本线</h2>
            <p class="text-sm text-muted-foreground">当前版本线：<UiBadge variant="secondary">{{ record.currentBranch ?? '—' }}</UiBadge></p>
            <div class="flex flex-wrap gap-2">
              <UiButton variant="outline" :disabled="Boolean(busy) || branches.length < 2 || writeLocked" @click="open('switch', $event)">切换版本线</UiButton>
              <UiButton variant="outline" :disabled="Boolean(busy) || writeLocked" @click="open('merge', $event)">合并版本线</UiButton>
              <UiButton :disabled="Boolean(busy) || writeLocked" @click="open('branch', $event)"><RiAddLine data-icon="inline-start" />新建版本线</UiButton>
              <UiButton variant="outline" :disabled="Boolean(busy)" @click="inspect(undefined, $event)">检查版本完整性</UiButton>
            </div>
          </div>
          <UiSeparator />
          <div class="flex flex-col gap-2">
            <h2 class="font-medium">暂存草稿</h2>
            <p class="text-sm text-muted-foreground">已暂存 {{ stashCount }} 项，暂存不会创建新版本。</p>
            <div class="flex flex-wrap gap-2">
              <UiButton variant="outline" :disabled="Boolean(busy) || writeLocked" @click="open('stash', $event)">暂存当前正文</UiButton>
              <UiButton variant="outline" :disabled="Boolean(busy) || !stashCount || writeLocked" @click="open('stash/pop', $event)">取回最近暂存</UiButton>
            </div>
          </div>
        </div>
      </UiCollapsibleContent>
    </UiCollapsible>

    <UiAlert v-if="loadError || detailError" variant="destructive" role="alert">
      <UiAlertDescription class="flex flex-wrap items-center gap-2">
        {{ loadError || detailError }}
        <UiButton v-if="loadError" variant="outline" size="sm" :disabled="Boolean(busy)" @click="refresh()">重新读取</UiButton>
      </UiAlertDescription>
    </UiAlert>
    <p v-if="feedback" role="status" class="text-sm text-muted-foreground">{{ feedback }}</p>
    <UiSkeleton v-if="busy === 'load' && !record" role="status" aria-label="正在读取版本历史" class="h-64" />
    <div v-if="record" class="grid min-w-0 items-start gap-4 xl:grid-cols-[minmax(0,1fr)_20rem]">
      <section aria-labelledby="commit-list-title" class="flex min-w-0 flex-col gap-3 border bg-card">
        <header class="flex flex-wrap items-center justify-between gap-3 p-4 pb-0">
          <h2 id="commit-list-title" class="font-semibold">版本记录 <span class="text-sm tabular-nums text-muted-foreground">{{ items.length }} 个</span></h2>
          <UiButton variant="ghost" size="sm" :disabled="Boolean(busy)" @click="refresh()">刷新</UiButton>
        </header>
        <div class="flex flex-wrap items-center gap-2 px-4 text-sm">
          <UiButton variant="outline" size="sm" :aria-pressed="mode === 'all'" :disabled="Boolean(busy)" @click="mode = 'all'">全部版本</UiButton>
          <UiButton variant="outline" size="sm" :aria-pressed="mode === 'recent'" :disabled="Boolean(busy)" @click="readRecent">最近 10 个版本</UiButton>
          <select v-if="advancedOpen" v-model="filter" aria-label="筛选版本线" class="h-8 border bg-background px-2">
            <option value="">全部版本线</option><option v-for="name in branches" :key="name">{{ name }}</option>
          </select>
        </div>
        <p v-if="!items.length" class="border-t p-4 text-sm text-muted-foreground">没有匹配的版本。</p>
        <ol v-else class="divide-y border-t">
          <li v-for="(item, index) in items" :key="item.id + '-' + index" class="flex min-w-0 flex-col gap-2 p-4">
            <div class="flex flex-wrap items-center gap-2">
              <button type="button" class="break-words text-left font-medium hover:underline" @click="showSnapshot(item, $event)">{{ item.message || '无修改说明' }}</button>
              <UiBadge v-if="item.id === record.headCommit">当前版本</UiBadge>
              <UiBadge v-if="item.commitType === 'merge'" variant="secondary">合并版本</UiBadge>
            </div>
            <p class="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
              <span class="font-mono">版本 {{ shortCommit(item.id) }}</span>
              <span v-if="advancedOpen">{{ item.branch ?? '—' }}</span>
              <span v-if="item.author">{{ item.author }}</span><span class="tabular-nums">{{ resumeTime(item.commitTime) }}</span>
            </p>
            <div class="flex flex-wrap gap-1">
              <UiButton variant="outline" size="sm" :disabled="Boolean(busy) || writeLocked" @click="open('checkout', $event, item)">恢复到此版本</UiButton>
              <UiButton variant="outline" size="sm" :disabled="Boolean(busy)" :aria-pressed="from === item.id || to === item.id" @click="pickCompare(item)">比较</UiButton>
              <template v-if="advancedOpen">
                <UiButton variant="ghost" size="sm" @click="showSnapshot(item, $event)">查看内容</UiButton>
                <UiButton variant="ghost" size="sm" :disabled="Boolean(busy) || !item.parentCommits?.length || writeLocked" @click="open('revert', $event, item)">以该版本内容新建保存</UiButton>
                <UiButton variant="ghost" size="sm" :disabled="Boolean(busy) || writeLocked || !mainline.has(item.id)" @click="open('reset', $event, item)">回退到此版本</UiButton>
                <UiButton variant="ghost" size="sm" :disabled="Boolean(busy)" @click="inspect(item, $event)">版本诊断</UiButton>
              </template>
            </div>
          </li>
        </ol>
      </section>

      <form ref="rangeForm" aria-labelledby="commit-range-title" class="flex min-w-0 flex-col gap-3 border bg-card p-4" @submit.prevent="readRange">
        <div class="flex flex-col gap-1">
          <h2 id="commit-range-title" class="font-medium">比较版本</h2>
          <p class="text-xs text-muted-foreground">选择两个版本查看正文差异。也可以先点版本旁的“比较”，再点另一个版本。</p>
        </div>
        <UiField>
          <UiFieldLabel for="range-from">较早版本</UiFieldLabel>
          <select id="range-from" v-model="from" :disabled="Boolean(busy)" class="min-w-0 border bg-background p-2 text-sm"><option value="">选择版本</option><option v-for="(item, index) in history" :key="index" :value="item.id">{{ shortCommit(item.id) }} · {{ item.message }}</option></select>
        </UiField>
        <UiField>
          <UiFieldLabel for="range-to">较晚版本</UiFieldLabel>
          <select id="range-to" v-model="to" :disabled="Boolean(busy)" class="min-w-0 border bg-background p-2 text-sm"><option value="">选择版本</option><option v-for="(item, index) in history" :key="index" :value="item.id">{{ shortCommit(item.id) }} · {{ item.message }}</option></select>
        </UiField>
        <UiButton type="submit" variant="outline" :disabled="Boolean(busy) || !from || !to || from === to">比较</UiButton>
        <p v-if="rangeRelation === 'separate'" role="status" class="text-sm">两个版本不在同一条版本线上。下方仍可比较正文。</p>
        <p v-else-if="rangeRelation === 'reverse'" role="status" class="text-sm">较晚版本是较早版本的前序版本。交换两个版本可查看区间记录。</p>
        <div v-if="range" role="status" class="flex flex-col gap-2 text-sm">
          共 {{ range.length }} 个版本
          <ul class="flex flex-col gap-1 text-xs text-muted-foreground">
            <li v-for="(item, index) in range" :key="index" class="break-words">{{ shortCommit(item.id) }} · {{ item.message }}</li>
          </ul>
        </div>
        <section v-if="lineDiff" aria-labelledby="line-diff-title" class="flex min-w-0 flex-col gap-2 text-sm">
          <h3 id="line-diff-title" class="font-medium">正文逐行对比</h3>
          <p v-if="lineDiff.limited" class="text-xs text-muted-foreground">改动区间较大，中间部分按整段删除和新增显示。</p>
          <p v-if="!lineDiff.lines.length" class="text-muted-foreground">两份正文均为空。</p>
          <ul v-else tabindex="0" aria-label="逐行正文差异" class="max-h-80 overflow-auto border font-mono text-xs">
            <li v-for="(line, index) in lineDiff.lines" :key="index" class="flex gap-2 px-2 py-0.5" :class="lineColors[line.kind]">
              <span aria-hidden="true" class="w-3 shrink-0 whitespace-pre">{{ lineMarkers[line.kind] }}</span>
              <span class="sr-only">{{ lineLabels[line.kind] }}</span>
              <span class="min-w-0 whitespace-pre-wrap wrap-anywhere">{{ line.text }}</span>
            </li>
          </ul>
        </section>
      </form>
    </div>

    <UiSheet :open="panel !== null" @update:open="!$event && (panel = null)">
      <UiSheetContent :side="mobile ? 'bottom' : 'right'" class="max-h-[85dvh] w-full overflow-auto sm:max-w-xl" @close-auto-focus="restoreActionFocus($event, panelTrigger, mainHeading)">
        <UiSheetHeader>
          <UiSheetTitle>{{ panel === 'snapshot' ? '查看版本内容' : selection ? '版本诊断' : '完整性检查' }}</UiSheetTitle>
          <UiSheetDescription>{{ selection ? shortCommit(selection.id) + ' · ' + (selection.message ?? '') : '检查当前版本、版本线及版本引用。' }}</UiSheetDescription>
        </UiSheetHeader>
        <div class="flex flex-col gap-4 px-4 pb-6">
          <ResumePreview v-if="panel === 'snapshot' && selection" :content="selection.content ?? ''" :images="images" :theme-id="record?.themeId" />
          <dl v-else-if="selection && debug" class="flex flex-col gap-2 text-sm">
            <div>版本存在 · {{ debug.commitExists === true ? '是' : '否' }}</div>
            <div>前序版本数 · {{ typeof debug.parentCount === 'number' ? debug.parentCount : '—' }}</div>
            <div>当前版本线 · {{ typeof debug.currentBranch === 'string' ? debug.currentBranch : '—' }}</div>
            <div>当前版本 · {{ typeof debug.headCommit === 'string' ? shortCommit(debug.headCommit) : '—' }}</div>
          </dl>
          <template v-else-if="validation">
            <UiAlert :variant="validation.isValid ? 'default' : 'destructive'"><UiAlertDescription>{{ validation.isValid ? '未发现完整性错误。' : '检测到完整性异常，请核对版本线和版本引用。' }}</UiAlertDescription></UiAlert>
            <ul class="flex flex-col gap-2 text-sm">
              <li>版本线数 · {{ validation.branchCount ?? '—' }}</li><li>孤立版本 · {{ validation.orphanCommits ?? '—' }}</li><li>重复版本 ID · {{ duplicates }}</li>
            </ul>
            <div v-if="validation.errors?.length" class="text-sm">
              <h3 class="font-medium text-destructive">错误</h3>
              <ul class="list-disc space-y-1 pl-5 wrap-anywhere"><li v-for="(error, index) in validation.errors" :key="index">{{ error }}</li></ul>
            </div>
            <div v-if="validation.warnings?.length" class="text-sm">
              <h3 class="font-medium">警告</h3>
              <ul class="list-disc space-y-1 pl-5 wrap-anywhere"><li v-for="(warning, index) in validation.warnings" :key="index">{{ warning }}</li></ul>
            </div>
            <div class="flex flex-wrap gap-2">
              <UiButton variant="outline" :disabled="Boolean(busy)" @click="inspect()">重新校验</UiButton>
            </div>
          </template>
        </div>
      </UiSheetContent>
    </UiSheet>

    <UiDialog :open="operation !== null" @update:open="!$event && !busy && (operation = null)">
      <UiDialogContent @interact-outside.prevent @escape-key-down="busy && $event.preventDefault()" @close-auto-focus="restoreActionFocus($event, trigger, mainHeading)">
        <UiDialogHeader>
          <UiDialogTitle>{{ action?.title }}</UiDialogTitle><UiDialogDescription>{{ action?.description }}</UiDialogDescription>
        </UiDialogHeader>
        <form class="flex flex-col gap-3" @submit.prevent="submit">
          <UiField v-if="operation === 'branch'">
            <UiFieldLabel for="new-resume-branch">版本线名称</UiFieldLabel><UiInput id="new-resume-branch" v-model="branchName" maxlength="80" :disabled="Boolean(busy)" required />
          </UiField>
          <UiField v-if="operation === 'merge'">
            <UiFieldLabel for="merge-source">源版本线</UiFieldLabel>
            <select id="merge-source" v-model="source" :disabled="Boolean(busy)" class="h-8 border bg-background px-2"><option value="">选择源版本线</option><option v-for="name in branches" :key="name">{{ name }}</option></select>
          </UiField>
          <UiField v-if="operation === 'merge' || operation === 'switch'">
            <UiFieldLabel for="version-target">目标版本线</UiFieldLabel>
            <select id="version-target" v-model="target" :disabled="Boolean(busy)" class="h-8 border bg-background px-2"><option v-for="name in branches" :key="name">{{ name }}</option></select>
          </UiField>
          <details v-if="operation === 'merge' && sourceSnapshot" class="border p-2 text-sm">
            <summary>核对源内容 · {{ shortCommit(sourceSnapshot.id) }} → {{ shortCommit(branchTips[target]) }}</summary>
            <pre class="mt-2 max-h-44 overflow-auto whitespace-pre-wrap break-words">{{ sourceSnapshot.content }}</pre>
          </details>
          <label v-if="operation === 'merge'" class="flex items-start gap-2 text-sm">
            <input v-model="confirmed" type="checkbox" :disabled="Boolean(busy)" class="mt-1">
            我已核对源内容，确认合并
          </label>
          <UiField v-if="operation === 'stash'">
            <UiFieldLabel for="version-stash">要暂存的正文</UiFieldLabel><UiTextarea id="version-stash" v-model="draft" :disabled="Boolean(busy)" />
          </UiField>
          <p v-if="selection" class="font-mono text-xs text-muted-foreground">{{ shortCommit(selection.id) }} · {{ selection.message }}</p>
          <p v-if="operation === 'checkout'" class="text-sm wrap-anywhere">
            <template v-if="checkoutBranches.length === 1">当前版本线将指向「{{ checkoutBranches[0] }}」的最新版本，下一次保存会推进该版本线。</template>
            <template v-else-if="checkoutBranches.length > 1">这是多个版本线的最新版本（{{ checkoutBranches.join('、') }}）。服务器会选定其中一个作为当前版本线，下一次保存会推进该版本线。</template>
            <template v-else>编辑区将基于所选版本更新，当前版本线暂时保留。下一次保存会基于所选版本创建新版本并移动当前版本线，原先的部分版本可能从该版本线脱离。</template>
          </p>
          <div v-if="operation === 'revert'" class="flex flex-col gap-2 text-sm">
            <template v-if="revertSnapshot">
              <p class="font-medium">将恢复的前序版本 · {{ shortCommit(revertSnapshot.id) }}</p>
              <pre tabindex="0" aria-label="前序版本正文" class="max-h-40 overflow-auto whitespace-pre-wrap wrap-anywhere border p-2">{{ revertSnapshot.content || '（正文为空）' }}</pre>
            </template>
            <p v-else>找不到前序版本，无法新建保存。</p>
          </div>
          <div v-if="operation === 'reset' && selection" class="flex flex-col gap-2 text-sm">
            <p class="font-medium">目标版本摘要</p>
            <pre tabindex="0" aria-label="目标版本摘要" class="max-h-32 overflow-auto whitespace-pre-wrap wrap-anywhere border p-2">{{ selection.content ? selection.content.slice(0, 400) + (selection.content.length > 400 ? '…' : '') : '（正文为空）' }}</pre>
          </div>
          <UiDialogFooter>
            <UiButton type="button" variant="outline" :disabled="Boolean(busy)" @click="operation = null">取消</UiButton>
            <UiButton type="submit" :variant="action?.destructive ? 'destructive' : 'default'" :disabled="!canConfirm">{{ busy ? '正在处理…' : action?.title }}</UiButton>
          </UiDialogFooter>
        </form>
      </UiDialogContent>
    </UiDialog>
  </section>
</template>
