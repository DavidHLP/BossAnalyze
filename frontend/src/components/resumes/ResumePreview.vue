<script setup lang="ts">
import { computed, shallowRef, useTemplateRef, watch } from 'vue'
import { useEventListener, useResizeObserver, useStyleTag } from '@vueuse/core'
import { loadResumeMath, type ResumeMathEngine } from '@/lib/resume-math'
import { measureResumePagination, type ResumePagination } from '@/lib/resume-pagination'
import { resumeCompactClass, resumeLayoutCss, resumePageHeight, resumePageMargin, resumePageWidth } from '@/lib/resume-render'
import { resumeHasMath, resumePreview } from '@/lib/resumes'
import MathNode from './MathNode.vue'
import ResumePreviewInline from './ResumePreviewInline.vue'

const props = defineProps<{ content: string; images?: Record<string, string>; compact?: boolean; estimatePages?: boolean }>()
const emit = defineEmits<{ pagination: [result: ResumePagination | null] }>()
const blocks = computed(() => resumePreview(props.content))
const textOnly = computed(() => blocks.value.every(block => {
  if (block.kind === 'heading' || block.kind === 'text') return block.inline.every(token => token.kind === 'text')
  return block.kind === 'list' && block.items.every(item => item.every(token => token.kind === 'text'))
}))
const article = useTemplateRef('article')
useStyleTag(resumeLayoutCss, { id: 'resume-layout' })
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

function updatePagination() {
  if (!props.estimatePages || !article.value) return
  emit('pagination', measureResumePagination(article.value, textOnly.value))
}

watch([article, blocks, () => props.compact, () => props.images, math], updatePagination, { flush: 'post' })
useResizeObserver(() => props.estimatePages ? article.value : null, updatePagination)
useEventListener(() => props.estimatePages ? globalThis.document?.fonts : undefined,
  ['loading', 'loadingdone', 'loadingerror'], updatePagination)
</script>

<template>
  <div class="resume-viewport">
    <div class="resume-sheet bg-background shadow-sm ring-1 ring-border">
      <article ref="article" aria-label="简历预览" class="resume-body" :class="{ [resumeCompactClass]: compact }"
        @load.capture="updatePagination" @error.capture="updatePagination">
        <p v-if="!blocks.length" class="text-muted-foreground">此版本没有正文。</p>
        <template v-for="(block, index) in blocks" :key="index">
          <component :is="'h' + block.level" v-if="block.kind === 'heading'"><ResumePreviewInline v-bind="math" :tokens="block.inline" /></component>
          <hr v-else-if="block.kind === 'rule'" />
          <figure v-else-if="block.kind === 'asset'">
            <img v-if="images && Object.hasOwn(images, block.fileName)" :src="images[block.fileName]" :alt="block.text" />
            <figcaption v-else>{{ block.text || '图片素材' }} · 图片无法加载</figcaption>
          </figure>
          <component :is="block.start === null ? 'ul' : 'ol'" v-else-if="block.kind === 'list'" :start="block.start ?? undefined">
            <li v-for="(item, i) in block.items" :key="i"><ResumePreviewInline v-bind="math" :tokens="item" /></li>
          </component>
          <pre v-else-if="block.kind === 'code'"><code>{{ block.text }}</code></pre>
          <table v-else-if="block.kind === 'table'">
            <thead><tr><th v-for="(cell, i) in block.headers" :key="i" scope="col"><ResumePreviewInline v-bind="math" :tokens="cell" /></th></tr></thead>
            <tbody><tr v-for="(row, i) in block.rows" :key="i"><td v-for="(cell, j) in row" :key="j"><ResumePreviewInline v-bind="math" :tokens="cell" /></td></tr></tbody>
          </table>
          <p v-else-if="block.kind === 'text'"><ResumePreviewInline v-bind="math" :tokens="block.inline" /></p>
          <MathNode v-else-if="block.kind === 'math'" display :tex="block.tex" :raw="block.raw"
            :engine="math.mathEngine" :loading="math.mathLoading" :load-failed="math.mathLoadFailed" />
        </template>
      </article>
    </div>
  </div>
</template>

<style scoped>
.resume-viewport {
  container-type: inline-size;
  overflow: auto;
}

.resume-sheet {
  box-sizing: border-box;
  width: v-bind(resumePageWidth);
  min-height: v-bind(resumePageHeight);
  margin-inline: auto;
  padding: v-bind(resumePageMargin);
  zoom: min(1, calc(100cqw / v-bind(resumePageWidth)));
}
</style>
