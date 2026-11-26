<script setup lang="ts">
import type { ResumeInline } from '@/lib/resumes'
import type { ResumeMathEngine } from '@/lib/resume-math'
import MathNode from './MathNode.vue'

defineProps<{
  tokens: ResumeInline[]; mathEngine: ResumeMathEngine | null; mathLoading: boolean; mathLoadFailed: boolean
}>()
</script>

<template>
  <template v-for="(token, index) in tokens" :key="index">
    <a v-if="token.kind === 'link'" :href="token.href" target="_blank" rel="noopener noreferrer" class="underline">{{ token.text }}</a>
    <code v-else-if="token.kind === 'code'" class="bg-muted px-1 font-mono">{{ token.text }}</code>
    <strong v-else-if="token.kind === 'strong'">{{ token.text }}</strong>
    <em v-else-if="token.kind === 'em'">{{ token.text }}</em>
    <template v-else-if="token.kind === 'text'">{{ token.text }}</template>
    <MathNode v-else-if="token.kind === 'math'" :tex="token.tex" :raw="token.raw"
      :engine="mathEngine" :loading="mathLoading" :load-failed="mathLoadFailed" />
  </template>
</template>
