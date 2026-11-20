<script setup lang="ts">
useHead({ title: '登录 · BossAnalyze' })
</script>

<template>
  <div class="grid min-h-svh lg:grid-cols-2">
    <aside
      aria-hidden="true"
      class="hidden bg-brand-surface text-brand-foreground lg:flex lg:flex-col lg:border-r lg:border-border lg:px-12 lg:py-10 xl:px-16"
    >
      <AppBrand class="self-start text-base" />
      <div class="grid flex-1 place-items-center py-10">
        <p class="w-full max-w-lg text-4xl font-semibold tracking-tight text-balance xl:text-5xl">
          招聘数据洞察<br />与权限治理工作台
        </p>
      </div>
    </aside>

    <div class="flex min-w-0 flex-col bg-card px-6 sm:px-10">
      <header class="flex h-16 shrink-0 items-center justify-between gap-4 lg:h-20 lg:justify-end">
        <AppBrand class="text-base lg:hidden" />
        <ThemeToggle />
      </header>
      <main class="grid flex-1 place-items-center py-10">
        <LoginForm class="w-full max-w-sm" />
      </main>
    </div>
  </div>
</template>
