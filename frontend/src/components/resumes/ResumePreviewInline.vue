<script setup lang="ts">
import type { ResumeInline } from '@/lib/resumes'

defineProps<{ tokens: ResumeInline[] }>()
</script>

<template>
  <template v-for="(token, index) in tokens" :key="index">
    <a v-if="token.kind === 'link'" :href="token.href" target="_blank" rel="noopener noreferrer" class="underline">{{ token.text }}</a>
    <code v-else-if="token.kind === 'code'" class="bg-muted px-1 font-mono">{{ token.text }}</code>
    <strong v-else-if="token.kind === 'strong'">{{ token.text }}</strong>
    <em v-else-if="token.kind === 'em'">{{ token.text }}</em>
    <template v-else-if="token.kind === 'text'">{{ token.text }}</template>
    <template v-else-if="token.kind === 'math'">{{ token.raw }}</template>
  </template>
</template>
