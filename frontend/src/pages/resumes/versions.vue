<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { useMediaQuery } from '@vueuse/core'
import { RiAddLine, RiArrowLeftLine } from '@remixicon/vue'
import {
  changeResumeVersion, getResume, inspectCommit, inspectResume, resumeHistory,
  type ResumeCommit, type ResumeRecord, type ResumeValidation,
} from '@/api/resume/resume'
import { useSession } from '@/composables/useSession'
import { restoreActionFocus } from '@/lib/rbac-mutations'
import {
  countDuplicateCommits, resumeFirstParentRelation, resumeLineDiff, resumeMainline, resumeReadMessage, resumeTime, shortCommit,
} from '@/lib/resumes'
import ResumePreview from '@/components/resumes/ResumePreview.vue'

type Action = 'branch' | 'switch' | 'merge' | 'stash' | 'stash/pop' | 'checkout' | 'reset' | 'revert' | 'fix-duplicate-ids'
const actions: Record<Action, { title: string; description: string; destructive?: boolean }> = {
  branch: { title: '新建分支', description: '新分支将从当前 HEAD 建立。' },
  switch: { title: '切换分支', description: '工作区内容将更新为目标分支的最新快照。' },
  merge: { title: '合并分支', description: '使用源分支快照创建双父提交并更新目标指针。系统不会自动合并正文冲突。' },
  stash: { title: '新增储藏', description: '下方已填入当前工作区正文。可在储藏前编辑，储藏不会创建版本提交。' },
  'stash/pop': { title: '弹出最新储藏', description: '将从储藏栈移除最新快照并恢复到工作区。HEAD 会保留当前版本，直至你提交恢复后的正文。' },
  checkout: { title: '检出历史快照', description: '此操作会用所选快照替换当前工作区内容。' },
  reset: { title: '重置分支', description: '将当前分支指针和工作区回退至所选提交。后续历史可能不再出现在当前分支。', destructive: true },
  revert: { title: '创建回滚提交', description: '用所选提交的第一父快照创建一个新提交，保留原有历史。' },
  'fix-duplicate-ids': { title: '去重重建仓库', description: '将永久删除旧历史、清空分支与储藏，并使用当前内容重建 main。', destructive: true },
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
const busy = ref('')
const loadError = ref('')
const detailError = ref('')
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
  if (operation.value === 'fix-duplicate-ids') return duplicates.value > 0 && confirmed.value
  return true
})

watch([source, target], () => { confirmed.value = false }, { flush: 'sync' })
watch([from, to], () => { range.value = comparison.value = null })

async function read(reset = false) {
  const generation = ++revision
  const owner = session.capture()
  const key = id.value
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
      getResume(key), resumeHistory(key), refreshRecent ? resumeHistory(key, true) : undefined,
    ])
    if (generation !== revision || key !== id.value || !session.owns(owner)) return false
    record.value = item
    history.value = commits
    if (latest) recent.value = latest
    writeLocked.value = false
    return true
  } catch (cause) {
    if (generation === revision && session.owns(owner)) {
      loadError.value = resumeReadMessage(cause)
      writeLocked.value = true
    }
    return false
  }
}

async function refresh(reset = false) {
  if (busy.value) return
  busy.value = 'load'
  detailError.value = ''
  try {
    await read(reset)
  } finally {
    busy.value = ''
  }
}

watch(id, () => void refresh(true))
onMounted(() => void refresh(true))
onUnmounted(() => { revision++ })
onBeforeRouteLeave(() => !busy.value.startsWith('write') || session.state.value.invalidated === true)
onBeforeRouteUpdate((to, from) => to.params.id === from.params.id || !busy.value)

async function readRecent() {
  if (busy.value) return
  busy.value = 'recent'
  detailError.value = ''
  const key = id.value
  const owner = session.capture()
  try {
    const commits = await resumeHistory(key, true)
    if (key === id.value && session.owns(owner)) {
      recent.value = commits
      mode.value = 'recent'
    }
  } catch {
    detailError.value = '无法读取最近提交，请重试。'
  } finally {
    busy.value = ''
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
  busy.value = 'range'
  const key = id.value
  const owner = session.capture()
  try {
    const commits = await resumeHistory(key, false, { fromCommit: start.id, toCommit: end.id })
    if (key === id.value && session.owns(owner)) range.value = commits
  } catch {
    detailError.value = '暂时无法读取区间提交。'
  } finally {
    busy.value = ''
  }
}

async function inspect(commit?: ResumeCommit, event?: MouseEvent) {
  if (busy.value) return
  if (event) panelTrigger = event.currentTarget as HTMLElement
  busy.value = 'inspect'
  detailError.value = ''
  const key = id.value
  const owner = session.capture()
  try {
    if (commit) {
      const result = await inspectCommit(key, commit.id)
      if (key !== id.value || !session.owns(owner)) return
      debug.value = result
      selection.value = commit
      panel.value = 'inspect'
    } else {
      if (!(await read()) || key !== id.value || !session.owns(owner)) return
      const result = await inspectResume(key)
      if (key !== id.value || !session.owns(owner)) return
      validation.value = result
      selection.value = null
      panel.value = 'inspect'
    }
  } catch {
    detailError.value = '暂时无法完成版本诊断，请重试。'
  } finally {
    busy.value = ''
  }
}

function showSnapshot(commit: ResumeCommit, event: MouseEvent) {
  panelTrigger = event.currentTarget as HTMLElement
  selection.value = commit
  panel.value = 'snapshot'
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
  let result: string | undefined
  try {
    if (command === 'branch') await changeResumeVersion(key, 'branch', { name: branchName.value.trim() })
    else if (command === 'switch') await changeResumeVersion(key, 'switch', { branch: target.value })
    else if (command === 'merge') result = await changeResumeVersion(key, 'merge', { source: source.value, target: target.value })
    else if (command === 'stash') await changeResumeVersion(key, 'stash', { content: draft.value })
    else if (command === 'stash/pop') result = await changeResumeVersion(key, 'stash/pop')
    else if (command === 'fix-duplicate-ids') await changeResumeVersion(key, 'fix-duplicate-ids')
    else if (commit) result = await changeResumeVersion(key, command, { commitId: commit.id })
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
        case 'fix-duplicate-ids': return !duplicates.value && current.currentBranch === 'main'
        default: return false
      }
    })()
    if (verified) feedback.value = '操作成功，已重新读取版本状态。'
    else detailError.value = '操作已被接收，重读时未能确认预期结果。请核对版本状态。'
  } catch {
    if (key === id.value && session.owns(owner)) {
      range.value = validation.value = null
      await read()
      operation.value = null
      detailError.value = '操作结果未确认，已尝试重读版本。请核对后再操作。'
    }
  } finally {
    busy.value = ''
  }
}
</script>

<template>
  <section class="flex min-w-0 flex-col gap-5" aria-labelledby="resume-versions-title">
    <header class="flex flex-wrap items-start justify-between gap-3">
      <div class="flex min-w-0 flex-col gap-2">
        <UiButton as-child variant="ghost" size="sm" class="self-start"><RouterLink :to="editPath"><RiArrowLeftLine data-icon="inline-start" />返回编辑器</RouterLink></UiButton>
        <h1 id="resume-versions-title" ref="mainHeading" tabindex="-1" class="text-3xl font-semibold tracking-tight">版本历史</h1>
        <p v-if="record" class="break-words text-sm text-muted-foreground">{{ record.title }}</p>
      </div>
      <div v-if="record" class="flex flex-wrap gap-2">
        <UiButton variant="outline" :disabled="Boolean(busy) || writeLocked" @click="open('merge', $event)">合并分支</UiButton>
        <UiButton variant="outline" :disabled="Boolean(busy)" @click="inspect(undefined, $event)">仓库校验</UiButton>
        <UiButton :disabled="Boolean(busy) || writeLocked" @click="open('branch', $event)"><RiAddLine data-icon="inline-start" />新建分支</UiButton>
      </div>
    </header>

    <UiAlert v-if="loadError || detailError" variant="destructive" role="alert">
      <UiAlertDescription class="flex flex-wrap items-center gap-2">
        {{ loadError || detailError }}
        <UiButton v-if="loadError" variant="outline" size="sm" :disabled="Boolean(busy)" @click="refresh()">重新读取</UiButton>
      </UiAlertDescription>
    </UiAlert>
    <p v-if="feedback" role="status" class="text-sm text-muted-foreground">{{ feedback }}</p>
    <UiSkeleton v-if="busy === 'load' && !record" role="status" aria-label="正在读取版本历史" class="h-64" />
    <div v-if="record" class="grid min-w-0 items-start gap-4 xl:grid-cols-[16rem_minmax(0,1fr)_17rem]">
      <aside class="flex min-w-0 flex-col gap-4 border bg-card p-4">
        <div class="flex flex-col gap-2">
          <h2 class="font-medium">当前分支 / HEAD</h2>
          <p class="flex flex-wrap items-center gap-2 text-sm"><UiBadge variant="secondary">{{ record.currentBranch ?? '—' }}</UiBadge><span class="font-mono">{{ shortCommit(record.headCommit) }}</span></p>
          <UiButton variant="outline" :disabled="Boolean(busy) || branches.length < 2 || writeLocked" @click="open('switch', $event)">切换分支</UiButton>
        </div>
        <UiSeparator />
        <div class="flex flex-col gap-2">
          <h2 class="font-medium">储藏栈</h2>
          <p class="text-sm text-muted-foreground">已储藏 {{ stashCount }} 项</p>
          <UiButton variant="outline" :disabled="Boolean(busy) || writeLocked" @click="open('stash', $event)">新增储藏</UiButton>
          <UiButton variant="outline" :disabled="Boolean(busy) || !stashCount || writeLocked" @click="open('stash/pop', $event)">弹出最新储藏</UiButton>
        </div>
      </aside>

      <section aria-labelledby="commit-list-title" class="flex min-w-0 flex-col gap-3 border bg-card">
        <header class="flex flex-wrap items-center justify-between gap-3 p-4 pb-0">
          <h2 id="commit-list-title" class="font-semibold">提交历史 <span class="text-sm tabular-nums text-muted-foreground">{{ items.length }} 条</span></h2>
          <UiButton variant="ghost" size="sm" :disabled="Boolean(busy)" @click="refresh()">刷新</UiButton>
        </header>
        <div class="flex flex-wrap items-center gap-2 px-4 text-sm">
          <UiButton variant="outline" size="sm" :aria-pressed="mode === 'all'" :disabled="Boolean(busy)" @click="mode = 'all'">全部历史</UiButton>
          <UiButton variant="outline" size="sm" :aria-pressed="mode === 'recent'" :disabled="Boolean(busy)" @click="readRecent">最近 10 次</UiButton>
          <select v-model="filter" aria-label="筛选提交分支" class="h-8 border bg-background px-2">
            <option value="">全部分支</option><option v-for="name in branches" :key="name">{{ name }}</option>
          </select>
        </div>
        <p v-if="!items.length" class="border-t p-4 text-sm text-muted-foreground">没有匹配的提交。</p>
        <ol v-else class="divide-y border-t">
          <li v-for="(item, index) in items" :key="item.id + '-' + index" class="flex min-w-0 flex-col gap-2 p-4">
            <div class="flex flex-wrap items-center gap-2">
              <button type="button" class="break-words text-left font-medium hover:underline" @click="showSnapshot(item, $event)">{{ item.message || '无提交备注' }}</button>
              <UiBadge v-if="item.id === record.headCommit">HEAD</UiBadge>
              <UiBadge v-if="item.commitType === 'merge'" variant="secondary">双父提交</UiBadge>
            </div>
            <p class="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
              <span class="font-mono">{{ shortCommit(item.id) }}</span><span>{{ item.branch ?? '—' }}</span>
              <span v-if="item.author">{{ item.author }}</span><span class="tabular-nums">{{ resumeTime(item.commitTime) }}</span>
            </p>
            <div class="flex flex-wrap gap-1">
              <UiButton variant="outline" size="sm" @click="showSnapshot(item, $event)">查看快照</UiButton>
              <UiButton variant="ghost" size="sm" :disabled="Boolean(busy) || writeLocked" @click="open('checkout', $event, item)">检出</UiButton>
              <UiButton variant="ghost" size="sm" :disabled="Boolean(busy) || !item.parentCommits?.length || writeLocked" @click="open('revert', $event, item)">回滚</UiButton>
              <UiButton variant="ghost" size="sm" :disabled="Boolean(busy) || writeLocked || !mainline.has(item.id)" @click="open('reset', $event, item)">重置</UiButton>
              <UiButton variant="ghost" size="sm" :disabled="Boolean(busy)" @click="inspect(item, $event)">诊断</UiButton>
            </div>
          </li>
        </ol>
      </section>

      <form aria-labelledby="commit-range-title" class="flex min-w-0 flex-col gap-3 border bg-card p-4" @submit.prevent="readRange">
        <div class="flex flex-col gap-1">
          <h2 id="commit-range-title" class="font-medium">区间提交记录</h2>
          <p class="text-xs text-muted-foreground">比较所选快照的正文，提交列表从结束提交沿第一父链回溯。</p>
        </div>
        <UiField>
          <UiFieldLabel for="range-from">起始提交</UiFieldLabel>
          <select id="range-from" v-model="from" :disabled="Boolean(busy)" class="min-w-0 border bg-background p-2 text-sm"><option value="">选择提交</option><option v-for="(item, index) in history" :key="index" :value="item.id">{{ shortCommit(item.id) }}</option></select>
        </UiField>
        <UiField>
          <UiFieldLabel for="range-to">结束提交</UiFieldLabel>
          <select id="range-to" v-model="to" :disabled="Boolean(busy)" class="min-w-0 border bg-background p-2 text-sm"><option value="">选择提交</option><option v-for="(item, index) in history" :key="index" :value="item.id">{{ shortCommit(item.id) }}</option></select>
        </UiField>
        <UiButton type="submit" variant="outline" :disabled="Boolean(busy) || !from || !to || from === to">查询区间</UiButton>
        <p v-if="rangeRelation === 'separate'" role="status" class="text-sm">两次提交不在同一条第一父链上。下方仍可比较正文。</p>
        <p v-else-if="rangeRelation === 'reverse'" role="status" class="text-sm">结束提交是起始提交的第一父链祖先。交换起止提交可查看区间记录。</p>
        <div v-if="range" role="status" class="flex flex-col gap-2 text-sm">
          共 {{ range.length }} 条提交
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
          <UiSheetTitle>{{ panel === 'snapshot' ? '查看历史快照' : selection ? '提交诊断' : '仓库完整性校验' }}</UiSheetTitle>
          <UiSheetDescription>{{ selection ? shortCommit(selection.id) + ' · ' + (selection.message ?? '') : '检查 HEAD、当前分支及提交引用。' }}</UiSheetDescription>
        </UiSheetHeader>
        <div class="flex flex-col gap-4 px-4 pb-6">
          <ResumePreview v-if="panel === 'snapshot' && selection" :content="selection.content ?? ''" />
          <dl v-else-if="selection && debug" class="flex flex-col gap-2 text-sm">
            <div>提交存在 · {{ debug.commitExists === true ? '是' : '否' }}</div>
            <div>父提交数 · {{ typeof debug.parentCount === 'number' ? debug.parentCount : '—' }}</div>
            <div>当前分支 · {{ typeof debug.currentBranch === 'string' ? debug.currentBranch : '—' }}</div>
            <div>HEAD · {{ typeof debug.headCommit === 'string' ? shortCommit(debug.headCommit) : '—' }}</div>
          </dl>
          <template v-else-if="validation">
            <UiAlert :variant="validation.isValid ? 'default' : 'destructive'"><UiAlertDescription>{{ validation.isValid ? '未发现完整性错误。' : '检测到完整性异常，请核对分支和引用。' }}</UiAlertDescription></UiAlert>
            <ul class="flex flex-col gap-2 text-sm">
              <li>分支数 · {{ validation.branchCount ?? '—' }}</li><li>孤立提交 · {{ validation.orphanCommits ?? '—' }}</li><li>重复提交 ID · {{ duplicates }}</li>
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
              <UiButton v-if="duplicates" variant="destructive" :disabled="Boolean(busy) || writeLocked" @click="open('fix-duplicate-ids', $event)">去重重建</UiButton>
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
            <UiFieldLabel for="new-resume-branch">分支名称</UiFieldLabel><UiInput id="new-resume-branch" v-model="branchName" maxlength="80" :disabled="Boolean(busy)" required />
          </UiField>
          <UiField v-if="operation === 'merge'">
            <UiFieldLabel for="merge-source">源分支</UiFieldLabel>
            <select id="merge-source" v-model="source" :disabled="Boolean(busy)" class="h-8 border bg-background px-2"><option value="">选择源分支</option><option v-for="name in branches" :key="name">{{ name }}</option></select>
          </UiField>
          <UiField v-if="operation === 'merge' || operation === 'switch'">
            <UiFieldLabel for="version-target">目标分支</UiFieldLabel>
            <select id="version-target" v-model="target" :disabled="Boolean(busy)" class="h-8 border bg-background px-2"><option v-for="name in branches" :key="name">{{ name }}</option></select>
          </UiField>
          <details v-if="operation === 'merge' && sourceSnapshot" class="border p-2 text-sm">
            <summary>核对源快照 · {{ shortCommit(sourceSnapshot.id) }} → {{ shortCommit(branchTips[target]) }}</summary>
            <pre class="mt-2 max-h-44 overflow-auto whitespace-pre-wrap break-words">{{ sourceSnapshot.content }}</pre>
          </details>
          <label v-if="operation === 'merge' || operation === 'fix-duplicate-ids'" class="flex items-start gap-2 text-sm">
            <input v-model="confirmed" type="checkbox" :disabled="Boolean(busy)" class="mt-1">
            {{ operation === 'merge' ? '我已核对源快照，确认合并' : '我已核对重复提交并确认永久重建' }}
          </label>
          <UiField v-if="operation === 'stash'">
            <UiFieldLabel for="version-stash">要储藏的正文</UiFieldLabel><UiTextarea id="version-stash" v-model="draft" :disabled="Boolean(busy)" />
          </UiField>
          <p v-if="selection && operation !== 'fix-duplicate-ids'" class="font-mono text-xs text-muted-foreground">{{ shortCommit(selection.id) }} · {{ selection.message }}</p>
          <p v-if="operation === 'checkout'" class="text-sm wrap-anywhere">
            <template v-if="checkoutBranches.length === 1">HEAD 将切到「{{ checkoutBranches[0] }}」的尖端，下一次保存会推进该分支。</template>
            <template v-else-if="checkoutBranches.length > 1">这是多个分支的尖端（{{ checkoutBranches.join('、') }}）。服务器选定其中一个作为当前分支，下一次保存会推进该分支。</template>
            <template v-else>HEAD 将分离到所选提交，当前分支指针暂时保留。下一次保存会基于所选提交创建新版本并移动当前分支指针，原先的部分历史可能从该分支脱离。</template>
          </p>
          <div v-if="operation === 'revert'" class="flex flex-col gap-2 text-sm">
            <template v-if="revertSnapshot">
              <p class="font-medium">将恢复的第一父快照 · {{ shortCommit(revertSnapshot.id) }}</p>
              <pre tabindex="0" aria-label="第一父快照正文" class="max-h-40 overflow-auto whitespace-pre-wrap wrap-anywhere border p-2">{{ revertSnapshot.content || '（正文为空）' }}</pre>
            </template>
            <p v-else>找不到第一父快照，无法回滚。</p>
          </div>
          <div v-if="operation === 'reset' && selection" class="flex flex-col gap-2 text-sm">
            <p class="font-medium">目标快照摘要</p>
            <pre tabindex="0" aria-label="目标快照摘要" class="max-h-32 overflow-auto whitespace-pre-wrap wrap-anywhere border p-2">{{ selection.content ? selection.content.slice(0, 400) + (selection.content.length > 400 ? '…' : '') : '（正文为空）' }}</pre>
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
