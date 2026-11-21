<script setup lang="ts">
import { computed } from 'vue'
import { resumePreview } from '@/lib/resumes'

const props = defineProps<{ content: string; images?: Record<string, string> }>()
const blocks = computed(() => resumePreview(props.content))
const headings = ['h2', 'h3', 'h4']
</script>

<template>
  <article aria-label="简历预览" class="mx-auto flex aspect-[210/297] min-h-80 w-full max-w-2xl flex-col gap-2 break-words border bg-background p-5 text-sm leading-relaxed shadow-sm sm:min-h-[32rem] sm:p-9">
    <p v-if="!blocks.length" class="text-muted-foreground">此版本没有正文。</p>
    <template v-for="(block, index) in blocks" :key="index">
      <component :is="headings[block.level - 1]" v-if="block.kind === 'heading'" :class="block.level === 1 ? 'text-2xl font-semibold tracking-tight' : 'mt-3 border-b pb-1 font-semibold'">{{ block.text }}</component>
      <UiSeparator v-else-if="block.kind === 'rule'" class="my-1" />
      <figure v-else-if="block.kind === 'asset'">
        <img v-if="images?.[block.fileName]" :src="images[block.fileName]" :alt="block.text" class="max-h-52 max-w-full object-contain" />
        <figcaption v-else class="text-muted-foreground">{{ block.text || '图片素材' }} · 预览不可用</figcaption>
      </figure>
      <p v-else :class="block.kind === 'bullet' ? 'pl-3' : undefined">
        <span v-if="block.kind === 'bullet'" aria-hidden="true" class="mr-1">•</span>
        <template v-for="(part, i) in block.text.split(/\*\*([^*]+)\*\*/g)" :key="i">
          <strong v-if="i % 2">{{ part }}</strong><template v-else>{{ part }}</template>
        </template>
      </p>
    </template>
  </article>
</template>
