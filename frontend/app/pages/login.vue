<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { Briefcase01Icon } from '@hugeicons/core-free-icons'

useHead({ title: '登录 · BossAnalyze' })
</script>

<template>
  <div class="grid min-h-svh lg:grid-cols-2">
    <div class="flex flex-col gap-4 p-6 md:p-10">
      <NuxtLink to="/login" class="flex w-fit items-center gap-2 font-semibold">
        <span
          class="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground"
        >
          <HugeiconsIcon :icon="Briefcase01Icon" class="size-5" aria-hidden="true" />
        </span>
        BossAnalyze
      </NuxtLink>
      <main class="flex flex-1 items-center justify-center py-12">
        <div class="w-full max-w-xs"><LoginForm /></div>
      </main>
      <p class="text-xs text-muted-foreground">招聘数据分析工作台</p>
    </div>
    <div class="relative hidden overflow-hidden bg-muted lg:block">
      <img
        src="/images/login-cover.svg"
        alt=""
        class="absolute inset-0 h-full w-full object-cover"
      />
      <div class="absolute inset-x-0 bottom-0 p-10 text-white xl:p-16">
        <p class="mb-3 text-sm tracking-widest text-white/70">BOSSANALYZE</p>
        <h2 class="max-w-sm text-3xl font-medium leading-relaxed">让职业选择<br />更清晰一点。</h2>
        <p class="mt-5 max-w-sm text-sm leading-7 text-white/75">
          从招聘信息出发，理解机会，发现方向。
        </p>
      </div>
    </div>
  </div>
</template>
