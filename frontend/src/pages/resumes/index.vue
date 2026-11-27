<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import { useRouter } from 'vue-router'
import { RiAddLine, RiRefreshLine } from '@remixicon/vue'
import { createResume, deleteResume, listResumes, renameResume, resumeHistory, type ResumeRecord } from '@/api/resume/resume'
import { useSession } from '@/composables/useSession'
import { mutationResultUnknown, restoreActionFocus } from '@/lib/rbac-mutations'
import { importResumeText, resumeBlank, resumeSkeleton } from '@/lib/resume-import'
import { resumeListReadback, resumeReadMessage, resumeTime, shortCommit, type ResumeListChange } from '@/lib/resumes'

const router = useRouter()
const session = useSession()
const resumes = ref<ResumeRecord[]>([])
const pending = ref(true)
const loadError = ref('')
const notice = ref('')
const query = ref('')
const branch = ref('')
const sort = ref('recent')
const applied = ref({ query: '', branch: '', sort: 'recent' })
const branches = computed(() => [...new Set(resumes.value.map(item => item.currentBranch).filter(Boolean))])
const results = computed(() => resumes.value
  .filter(item => item.title.toLowerCase().includes(applied.value.query.toLowerCase()) &&
    (!applied.value.branch || applied.value.branch === item.currentBranch))
  .sort((a, b) => applied.value.sort === 'title' ? a.title.localeCompare(b.title, 'zh-CN') :
    (b.updatedAt ?? b.createdAt ?? '').localeCompare(a.updatedAt ?? a.createdAt ?? '')))
const dialog = ref<'create' | 'rename' | 'delete' | null>(null)
const selected = ref<ResumeRecord | null>(null)
const title = ref('')
const entry = ref<'blank' | 'skeleton' | 'paste'>('blank')
const importSource = ref('')
const importPreview = ref('')
const committing = ref(false)
const unverified = shallowRef<ResumeListChange | null>(null)
const writeDisabled = computed(() => pending.value || committing.value || Boolean(loadError.value || unverified.value))
const dialogError = ref('')
const dialogTitle = computed(() => dialog.value === 'delete' ? '删除简历' : dialog.value === 'rename' ? '重命名简历' : '新建简历')
const entries = [
  { value: 'blank', label: '空白' },
  { value: 'skeleton', label: '标准骨架' },
  { value: 'paste', label: '粘贴现有简历' },
] as const
watch(importSource, value => {
  const imported = importResumeText(value)
  importPreview.value = imported.content
  if (!title.value.trim() && imported.title) title.value = imported.title
})

function createContent(name: string) {
  if (entry.value === 'paste') return importPreview.value
  return entry.value === 'skeleton' ? resumeSkeleton(name) : resumeBlank(name)
}

function resetCreateDraft() {
  entry.value = 'blank'
  importSource.value = ''
  importPreview.value = ''
  title.value = ''
}
const mainHeading = ref<HTMLElement | null>(null)
let trigger: HTMLElement | null = null
let loadingVersion = 0
let mounted = true

async function load() {
  const version = ++loadingVersion
  const owner = session.capture()
  pending.value = true
  loadError.value = ''
  try {
    const items = await listResumes()
    if (version !== loadingVersion || !session.owns(owner)) return
    resumes.value = items
    return items
  } catch (cause) {
    if (version === loadingVersion) loadError.value = resumeReadMessage(cause)
  } finally {
    if (version === loadingVersion) pending.value = false
  }
}
onMounted(load)
onUnmounted(() => { mounted = false; loadingVersion++ })

const operationLabel = { create: '创建简历', rename: '重命名简历', delete: '删除简历' }
const writeConfirmed = { create: '简历已创建。', rename: '简历已重命名。', delete: '简历已删除。' }

async function refreshList() {
  if (pending.value || committing.value) return
  const items = await load()
  const change = unverified.value
  if (!items || !change) return
  unverified.value = null
  const confirmed = resumeListReadback(change, items)
  if (change.kind === 'create') {
    notice.value = confirmed
      ? (change.id ? writeConfirmed.create : '已刷新列表，发现标题和正文一致的简历。请在列表中核对并打开。')
      : '已刷新列表，暂未发现标题和正文一致的简历。请核对后再创建。'
    if (confirmed) resetCreateDraft()
  } else {
    notice.value = confirmed
      ? writeConfirmed[change.kind]
      : '列表中仍未看到预期结果。请核对最新记录后再操作。'
  }
}

function apply() {
  applied.value = { query: query.value.trim(), branch: branch.value, sort: sort.value }
}

function reset() {
  query.value = branch.value = ''
  sort.value = 'recent'
  apply()
}

function open(kind: NonNullable<typeof dialog.value>, event: MouseEvent, row?: ResumeRecord) {
  if (writeDisabled.value) return
  trigger = event.currentTarget as HTMLElement
  selected.value = row ?? null
  // Keep an unconfirmed create draft: the write may have succeeded even when its reply was lost.
  if (kind !== 'create') title.value = row?.title ?? ''
  dialogError.value = ''
  dialog.value = kind
}

function close(open: boolean) {
  if (!open && !committing.value) dialog.value = null
}

async function confirm() {
  const operation = dialog.value
  const row = selected.value
  const name = title.value.trim()
  if (writeDisabled.value || !operation || (operation !== 'create' && !row)) return
  if (operation !== 'delete' && !name) {
    dialogError.value = '请输入简历标题。'
    return
  }
  if (operation === 'create' && entry.value === 'paste' && !importPreview.value.trim()) {
    dialogError.value = '请先粘贴简历文本，核对预览后再创建。'
    return
  }
  committing.value = true
  dialogError.value = ''
  notice.value = ''
  const owner = session.capture()
  const active = () => mounted && session.owns(owner)
  let change: ResumeListChange
  if (operation === 'create') {
    change = {
      kind: 'create', title: name, content: createContent(name),
      previousIds: new Set(resumes.value.map(item => item.id)),
    }
  } else {
    change = operation === 'rename' ? { kind: 'rename', id: row!.id, title: name } : { kind: 'delete', id: row!.id }
  }
  try {
    try {
      if (change.kind === 'create') change.id = (await createResume(change.title, change.content)).id
      else if (change.kind === 'rename') await renameResume(change.id, name)
      else await deleteResume(change.id)
    } catch (cause) {
      if (!active()) return
      if (mutationResultUnknown(cause)) {
        unverified.value = change
        dialog.value = null
        notice.value = operationLabel[operation] + '结果未知。' +
          (operation === 'create' ? '请先刷新列表，按标题和正文核对是否已生成，再决定是否重试。' : '请先刷新列表核对，再继续操作。')
      } else {
        dialogError.value = operationLabel[operation] + '写入失败，请核对后重试。'
      }
      return
    }
    if (!active()) return
    let versionConfirmed = true
    if (change.kind === 'create' && change.id) {
      try {
        const commits = await resumeHistory(change.id, 2)
        versionConfirmed = commits.length === 1 && commits[0]?.content === change.content
      } catch {
        versionConfirmed = false
      }
      if (!active()) return
    }
    dialog.value = null
    const items = await load()
    if (!active()) return
    if (!items || !versionConfirmed || !resumeListReadback(change, items)) {
      unverified.value = change
      notice.value = operationLabel[operation] + '已受理，但回读未能确认标题、正文或初始版本。请刷新列表核对。'
      return
    }
    notice.value = writeConfirmed[operation]
    if (change.kind === 'create' && change.id) {
      resetCreateDraft()
      await router.push('/resumes/' + change.id + '/edit').catch(() => {
        if (active()) notice.value = '简历已创建。请从列表打开编辑器。'
      })
    }
  } finally {
    committing.value = false
  }
}
</script>

<template>
  <section class="flex min-w-0 flex-col gap-5" aria-labelledby="resumes-title">
    <header class="flex flex-wrap items-start justify-between gap-3">
      <div class="flex flex-col gap-2">
        <h1 id="resumes-title" ref="mainHeading" tabindex="-1" class="text-3xl font-semibold tracking-tight">我的简历</h1>
        <p class="text-sm text-muted-foreground">仅本人可见，每次保存均写入版本快照。</p>
      </div>
      <UiButton :disabled="writeDisabled" @click="open('create', $event)"><RiAddLine data-icon="inline-start" />新建简历</UiButton>
    </header>

    <form class="flex flex-wrap items-end gap-3 border bg-card p-4" @submit.prevent="apply">
      <UiField class="min-w-40 flex-1 sm:max-w-72">
        <UiFieldLabel for="resume-query">简历标题</UiFieldLabel>
        <UiInput id="resume-query" v-model="query" type="search" placeholder="按简历标题搜索" />
      </UiField>
      <UiField class="w-auto min-w-32 flex-1 sm:flex-none">
        <UiFieldLabel for="resume-branch">当前分支</UiFieldLabel>
        <select id="resume-branch" v-model="branch" class="h-8 border bg-background px-2 text-sm">
          <option value="">全部分支</option>
          <option v-for="item in branches" :key="item" :value="item">{{ item }}</option>
        </select>
      </UiField>
      <UiField class="w-auto min-w-32 flex-1 sm:flex-none">
        <UiFieldLabel for="resume-sort">排序</UiFieldLabel>
        <select id="resume-sort" v-model="sort" class="h-8 border bg-background px-2 text-sm">
          <option value="recent">最近更新</option><option value="title">按标题</option>
        </select>
      </UiField>
      <div class="flex flex-wrap gap-2">
        <UiButton type="submit" variant="secondary">查询</UiButton>
        <UiButton type="button" variant="ghost" @click="reset">重置</UiButton>
        <UiButton type="button" variant="outline" :disabled="pending || committing" @click="refreshList"><RiRefreshLine data-icon="inline-start" />刷新</UiButton>
      </div>
    </form>

    <p v-if="notice" role="status" class="text-sm text-muted-foreground">{{ notice }}</p>
    <UiSkeleton v-if="pending" role="status" aria-label="正在读取简历" class="h-44" />
    <UiAlert v-else-if="loadError" role="alert" variant="destructive">
      <UiAlertDescription>{{ loadError }} <UiButton variant="outline" size="sm" :disabled="committing" @click="refreshList">重试</UiButton></UiAlertDescription>
    </UiAlert>
    <div v-else-if="!results.length" class="flex min-h-40 flex-col items-center justify-center gap-3 border bg-card px-4 text-center">
      <p class="font-medium">{{ resumes.length ? '没有匹配的简历' : '还没有简历' }}</p>
      <UiButton v-if="resumes.length" variant="outline" @click="reset">清除筛选</UiButton>
      <UiButton v-else variant="outline" :disabled="writeDisabled" @click="open('create', $event)">创建第一份简历</UiButton>
    </div>
    <template v-else>
      <div class="hidden border bg-card lg:block">
        <UiTable>
          <UiTableCaption class="sr-only">我的简历</UiTableCaption>
          <UiTableHeader><UiTableRow class="bg-muted/50">
            <UiTableHead>简历标题</UiTableHead><UiTableHead>当前分支</UiTableHead><UiTableHead>HEAD</UiTableHead>
            <UiTableHead class="text-right">分支数</UiTableHead><UiTableHead>最近更新时间</UiTableHead><UiTableHead class="text-right">操作</UiTableHead>
          </UiTableRow></UiTableHeader>
          <UiTableBody>
            <UiTableRow v-for="item in results" :key="item.id">
              <UiTableCell class="max-w-64 break-words font-medium">
                <RouterLink :to="'/resumes/' + item.id + '/edit'" class="hover:underline">{{ item.title }}</RouterLink>
              </UiTableCell>
              <UiTableCell><UiBadge variant="secondary">{{ item.currentBranch ?? '—' }}</UiBadge></UiTableCell>
              <UiTableCell class="font-mono text-xs">{{ shortCommit(item.headCommit) }}</UiTableCell>
              <UiTableCell class="text-right tabular-nums">{{ item.branches ? Object.keys(item.branches).length : '—' }}</UiTableCell>
              <UiTableCell class="tabular-nums">{{ resumeTime(item.updatedAt ?? item.createdAt) }}</UiTableCell>
              <UiTableCell>
                <div class="flex justify-end gap-1">
                  <UiButton as-child variant="ghost" size="sm"><RouterLink :to="'/resumes/' + item.id + '/versions'">版本历史</RouterLink></UiButton>
                  <UiButton variant="ghost" size="sm" :disabled="writeDisabled" @click="open('rename', $event, item)">重命名</UiButton>
                  <UiButton variant="ghost" size="sm" :disabled="writeDisabled" @click="open('delete', $event, item)">删除</UiButton>
                </div>
              </UiTableCell>
            </UiTableRow>
          </UiTableBody>
        </UiTable>
      </div>
      <ul class="divide-y border bg-card lg:hidden">
        <li v-for="item in results" :key="item.id" class="flex min-w-0 flex-col gap-3 p-4">
          <RouterLink :to="'/resumes/' + item.id + '/edit'" class="break-words font-medium hover:underline">{{ item.title }}</RouterLink>
          <p class="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
            <UiBadge variant="secondary">{{ item.currentBranch ?? '—' }}</UiBadge>
            {{ resumeTime(item.updatedAt ?? item.createdAt) }}
          </p>
          <div class="flex flex-wrap gap-1">
            <UiButton as-child variant="outline" size="sm"><RouterLink :to="'/resumes/' + item.id + '/versions'">版本历史</RouterLink></UiButton>
            <UiButton variant="ghost" size="sm" :disabled="writeDisabled" @click="open('rename', $event, item)">重命名</UiButton>
            <UiButton variant="ghost" size="sm" :disabled="writeDisabled" @click="open('delete', $event, item)">删除</UiButton>
          </div>
        </li>
      </ul>
      <p class="text-sm tabular-nums text-muted-foreground">共 {{ results.length }} 份简历</p>
    </template>

    <UiDialog :open="dialog !== null" @update:open="close">
      <UiDialogContent @interact-outside.prevent @escape-key-down="committing && $event.preventDefault()" @close-auto-focus="restoreActionFocus($event, trigger, mainHeading)">
        <UiDialogHeader>
          <UiDialogTitle>{{ dialogTitle }}</UiDialogTitle>
          <UiDialogDescription>{{ dialog === 'delete' ? '将永久删除「' + selected?.title + '」及其全部版本，无法恢复。' : dialog === 'create' ? '选择起步方式，创建后进入编辑器；只生成一条初始版本。' : '新标题会与当前内容一起保存为一个版本。' }}</UiDialogDescription>
        </UiDialogHeader>
        <form class="flex flex-col gap-4" @submit.prevent="confirm">
          <UiField v-if="dialog === 'create'">
            <p id="resume-entry-label" class="text-sm font-medium">起步方式</p>
            <UiRadioGroup v-model="entry" aria-labelledby="resume-entry-label" class="flex flex-wrap gap-x-4 gap-y-2">
              <label v-for="option in entries" :key="option.value" class="flex items-center gap-2 text-sm">
                <UiRadioGroupItem :value="option.value" :disabled="committing" />{{ option.label }}
              </label>
            </UiRadioGroup>
          </UiField>
          <UiField v-if="dialog !== 'delete'">
            <UiFieldLabel for="resume-title">简历标题</UiFieldLabel>
            <UiInput id="resume-title" v-model="title" maxlength="120" :disabled="committing" required />
          </UiField>
          <template v-if="dialog === 'create' && entry === 'paste'">
            <UiAlert>
              <UiAlertDescription>表格、图片和多栏排版无法从纯文本还原；转换结果只保留姓名、章节、列表符号和日期。请先核对下方预览，确认创建前不会提交。</UiAlertDescription>
            </UiAlert>
            <UiField>
              <UiFieldLabel for="resume-import-source">粘贴纯文本简历</UiFieldLabel>
              <UiTextarea id="resume-import-source" v-model="importSource" rows="6" :disabled="committing" placeholder="粘贴姓名、联系方式、教育经历、工作经历、项目经历和技能" />
            </UiField>
            <UiField v-if="importSource.trim()">
              <UiFieldLabel for="resume-import-preview">导入预览（Markdown，可修改）</UiFieldLabel>
              <UiTextarea id="resume-import-preview" v-model="importPreview" rows="10" :disabled="committing" />
              <p class="text-xs text-muted-foreground">预览是最终保存的正文；修改上方原文会重新生成预览。</p>
            </UiField>
          </template>
          <UiAlert v-if="dialogError" role="alert" variant="destructive"><UiAlertDescription>{{ dialogError }}</UiAlertDescription></UiAlert>
          <UiDialogFooter>
            <UiButton type="button" variant="outline" :disabled="committing" @click="close(false)">取消</UiButton>
            <UiButton type="submit" :variant="dialog === 'delete' ? 'destructive' : 'default'" :disabled="writeDisabled">
              {{ committing ? '正在处理…' : dialog === 'delete' ? '永久删除' : dialog === 'rename' ? '保存标题' : '创建简历' }}
            </UiButton>
          </UiDialogFooter>
        </form>
      </UiDialogContent>
    </UiDialog>
  </section>
</template>
