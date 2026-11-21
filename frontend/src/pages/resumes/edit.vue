<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute } from 'vue-router'
import { RiArrowLeftLine, RiHistoryLine, RiImageLine, RiSaveLine } from '@remixicon/vue'
import { changeResumeVersion, getResume, type ResumeRecord } from '@/api/resume/resume'
import { ownsResumeAsset, readResumeAsset } from '@/api/resume/assets'
import { useSession } from '@/composables/useSession'
import { assetMarkdown, resumePreview, resumeReadMessage, shortCommit } from '@/lib/resumes'
import ResumeAssets from '@/components/resumes/ResumeAssets.vue'
import ResumeExport from '@/components/resumes/ResumeExport.vue'
import ResumePreview from '@/components/resumes/ResumePreview.vue'

const route = useRoute()
const session = useSession()
const userId = computed(() => session.state.value.user?.id)
const id = computed(() => String(route.params.id))
const record = shallowRef<ResumeRecord | null>(null)
const pending = ref(true)
const pendingAction = ref<'save' | 'stash' | null>(null)
const loadError = ref('')
const feedback = ref('')
const writeError = ref('')
const writeLocked = ref(false)
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
const canSave = computed(() => dirty.value || !!message.value.trim())
const assets = computed(() => {
  const keys = resumePreview(markdown.value).flatMap(block => block.kind === 'asset' ? [block.fileName] : [])
  return [...new Set([...keys, ...staged.value])].filter(key =>
    userId.value !== undefined && ownsResumeAsset(userId.value, key) && !hidden.value.includes(key))
})
let loadRevision = 0

async function load(force = false) {
  const revision = ++loadRevision
  const owner = session.capture()
  const key = id.value
  pending.value = true
  loadError.value = ''
  try {
    const item = await getResume(key)
    if (revision !== loadRevision || !session.owns(owner)) return
    if (force) {
      record.value = null
      markdown.value = item.content ?? ''
      message.value = ''
      staged.value = hidden.value = []
      feedback.value = writeError.value = ''
      mode.value = 'edit'
    }
    record.value = item
    writeLocked.value = false
  } catch (cause) {
    if (revision === loadRevision && session.owns(owner)) loadError.value = resumeReadMessage(cause)
  } finally {
    if (revision === loadRevision) pending.value = false
  }
}

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

function insert(before: string, after = '', defaultText = '') {
  const input = selection()
  const start = input?.selectionStart ?? markdown.value.length
  const end = input?.selectionEnd ?? start
  const content = markdown.value.slice(start, end) || defaultText
  markdown.value = markdown.value.slice(0, start) + before + content + after + markdown.value.slice(end)
  void nextTick(() => {
    input?.focus()
    input?.setSelectionRange(start + before.length, start + before.length + content.length)
  })
}

function insertAsset(name: string) {
  const input = selection()
  const start = input?.selectionStart ?? markdown.value.length
  const line = '\n' + assetMarkdown(name) + '\n'
  markdown.value = markdown.value.slice(0, start) + line + markdown.value.slice(start)
  assetsOpen.value = false
  void nextTick(() => {
    input?.focus()
    input?.setSelectionRange(start + line.length, start + line.length)
  })
}

async function write(action: 'save' | 'stash') {
  if (!(action === 'save' ? canSave.value : dirty.value) || pendingAction.value || writeLocked.value) return
  const key = id.value
  const content = markdown.value
  const owner = session.capture()
  pendingAction.value = action
  feedback.value = writeError.value = ''
  try {
    if (action === 'save') await changeResumeVersion(key, 'commit', { message: message.value.trim() || '更新简历', content })
    else await changeResumeVersion(key, 'stash', { content })
    if (!session.owns(owner) || key !== id.value) return
    await load()
    if (loadError.value) {
      writeLocked.value = true
      writeError.value = '提交已被接收，但暂时无法确认最新状态。请先重新读取。'
    } else if (action === 'stash' || record.value?.content === content) {
      if (action === 'stash') markdown.value = record.value?.content ?? ''
      message.value = ''
      feedback.value = action === 'stash' ? '草稿已储藏，可在版本历史中取回。' : '简历已保存并提交。'
    } else {
      writeError.value = '最新版本与本次提交不同，草稿仍保留。请核对后再操作。'
    }
  } catch {
    if (session.owns(owner) && key === id.value) {
      await load()
      writeLocked.value = Boolean(loadError.value)
      writeError.value = '操作结果未确认，当前正文仍保留。请核对版本或储藏记录后再操作。'
    }
  } finally {
    pendingAction.value = null
  }
}

function blockNavigation() {
  if (session.state.value.invalidated) return true
  if (pendingAction.value) return false
  return !dirty.value || window.confirm('当前简历有未保存的内容。确定离开？')
}

function beforeUnload(event: BeforeUnloadEvent) {
  if (dirty.value || pendingAction.value) {
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
  loadRevision++
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
          <span role="status">{{ dirty ? '未保存 · 离开将确认草稿' : '工作区已同步' }}</span>
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

    <UiAlert v-if="loadError || writeError" variant="destructive" role="alert">
      <UiAlertDescription class="flex flex-wrap items-center gap-2">
        {{ writeError || loadError }}
        <UiButton v-if="loadError || writeLocked" variant="outline" size="sm" :disabled="pendingAction !== null" @click="load(!record)">重新读取</UiButton>
      </UiAlertDescription>
    </UiAlert>
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
            <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @click="insert('## ', '', '小节标题')">标题</UiButton>
            <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @click="insert('**', '**', '加粗文字')">加粗</UiButton>
            <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @click="insert('- ', '', '列表项')">列表</UiButton>
            <UiButton size="sm" variant="ghost" :disabled="Boolean(pendingAction)" @click="insert('\n---\n')">分割线</UiButton>
            <UiButton size="sm" variant="ghost" @click="assetsOpen = true">插入图片</UiButton>
          </div>
          <UiField class="p-3">
            <UiFieldLabel for="resume-markdown" class="sr-only">简历正文</UiFieldLabel>
            <UiTextarea id="resume-markdown" v-model="markdown" class="min-h-[24rem] font-mono leading-relaxed" :disabled="Boolean(pendingAction)" spellcheck="false" />
          </UiField>
          <div class="flex flex-col gap-3 border-t p-3">
            <UiField>
              <UiFieldLabel for="resume-message">提交备注</UiFieldLabel>
              <UiInput id="resume-message" v-model="message" maxlength="160" placeholder="简单描述本次修改" :disabled="Boolean(pendingAction)" />
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
