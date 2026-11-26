<script setup lang="ts">
import { computed, shallowRef, watch } from 'vue'
import { loadResumeMath, type ResumeMathEngine } from '@/lib/resume-math'
import { resumeHasMath, resumePreview } from '@/lib/resumes'
import MathNode from './MathNode.vue'
import ResumePreviewInline from './ResumePreviewInline.vue'

const props = defineProps<{ content: string; images?: Record<string, string> }>()
const blocks = computed(() => resumePreview(props.content))
const headings = ['h2', 'h3', 'h4']
const math = shallowRef({ mathEngine: null as ResumeMathEngine | null, mathLoading: false, mathLoadFailed: false })

watch(() => resumeHasMath(blocks.value), async (hasMath, _, onCleanup) => {
  if (!hasMath || math.value.mathEngine) return
  let active = true
  onCleanup(() => { active = false })
  math.value = { mathEngine: null, mathLoading: true, mathLoadFailed: false }
  try {
    const mathEngine = await loadResumeMath()
    if (active) math.value = { mathEngine, mathLoading: false, mathLoadFailed: false }
  } catch {
    if (active) math.value = { mathEngine: null, mathLoading: false, mathLoadFailed: true }
  }
}, { immediate: true })
</script>

<template>
  <article aria-label="简历预览" class="mx-auto flex aspect-[210/297] min-h-80 w-full max-w-2xl flex-col gap-2 break-words border bg-background p-5 text-sm leading-relaxed shadow-sm sm:min-h-[32rem] sm:p-9">
    <p v-if="!blocks.length" class="text-muted-foreground">此版本没有正文。</p>
    <template v-for="(block, index) in blocks" :key="index">
      <component :is="headings[block.level - 1]" v-if="block.kind === 'heading'" :class="block.level === 1 ? 'text-2xl font-semibold tracking-tight' : 'mt-3 border-b pb-1 font-semibold'"><ResumePreviewInline v-bind="math" :tokens="block.inline" /></component>
      <UiSeparator v-else-if="block.kind === 'rule'" class="my-1" />
      <figure v-else-if="block.kind === 'asset'">
        <img v-if="images && Object.hasOwn(images, block.fileName)" :src="images[block.fileName]" :alt="block.text" class="max-h-52 max-w-full object-contain" />
        <figcaption v-else class="text-muted-foreground">{{ block.text || '图片素材' }} · 预览不可用</figcaption>
      </figure>
      <component :is="block.start === null ? 'ul' : 'ol'" v-else-if="block.kind === 'list'" :start="block.start ?? undefined"
        class="pl-6" :class="block.start === null ? 'list-disc' : 'list-decimal'">
        <li v-for="(item, i) in block.items" :key="i"><ResumePreviewInline v-bind="math" :tokens="item" /></li>
      </component>
      <pre v-else-if="block.kind === 'code'" class="overflow-x-auto whitespace-pre-wrap wrap-anywhere bg-muted p-2"><code>{{ block.text }}</code></pre>
      <div v-else-if="block.kind === 'table'" class="overflow-x-auto">
        <table class="w-full border-collapse text-left">
          <thead><tr><th v-for="(cell, i) in block.headers" :key="i" scope="col" class="border bg-muted px-2 py-1"><ResumePreviewInline v-bind="math" :tokens="cell" /></th></tr></thead>
          <tbody><tr v-for="(row, i) in block.rows" :key="i"><td v-for="(cell, j) in row" :key="j" class="border px-2 py-1"><ResumePreviewInline v-bind="math" :tokens="cell" /></td></tr></tbody>
        </table>
      </div>
      <p v-else-if="block.kind === 'text'"><ResumePreviewInline v-bind="math" :tokens="block.inline" /></p>
      <MathNode v-else-if="block.kind === 'math'" display :tex="block.tex" :raw="block.raw"
        :engine="math.mathEngine" :loading="math.mathLoading" :load-failed="math.mathLoadFailed" />
    </template>
  </article>
</template>
