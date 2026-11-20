<script setup lang="ts">
useHead({ title: '登录 · BossAnalyze' })
</script>

<template>
  <div class="grid min-h-svh lg:grid-cols-[1fr_1fr]">
    <aside class="login-cover relative hidden overflow-hidden bg-[#0f172a] text-slate-50 lg:flex lg:flex-col lg:px-12 lg:py-10 xl:px-16">
      <AppBrand class="relative self-start text-base" />
      <div class="relative mx-auto my-auto w-full max-w-lg py-16">
        <h2 class="text-4xl leading-[1.25] font-semibold tracking-tight xl:text-5xl">
          招聘数据洞察<br />与权限治理工作台
        </h2>
        <div aria-hidden="true" class="mt-12 grid gap-3">
          <div class="rounded-lg border border-slate-700 bg-slate-900/90 px-5 pt-4 shadow-panel">
            <p class="text-sm font-medium text-slate-300">招聘薪资趋势</p>
            <svg viewBox="0 0 440 124" class="mt-3 w-full" fill="none">
              <path d="M0 26H440M0 58H440M0 90H440" stroke="#334155" stroke-dasharray="4 4" />
              <path
                d="M0 99L45 81L89 86L133 67L177 73L221 46L265 55L309 39L353 48L397 25L440 12V124H0Z"
                fill="#0284c7"
                fill-opacity=".15"
              />
              <path
                d="M0 99L45 81L89 86L133 67L177 73L221 46L265 55L309 39L353 48L397 25L440 12"
                stroke="#38bdf8"
                stroke-width="2"
                vector-effect="non-scaling-stroke"
              />
              <circle cx="309" cy="39" r="4" fill="#0f172a" stroke="#7dd3fc" stroke-width="2" />
            </svg>
          </div>
          <div class="flex items-center justify-between gap-4 rounded-lg border border-slate-700 bg-slate-900/90 px-5 py-3 shadow-panel">
            <p class="text-sm font-medium text-slate-300">权限治理</p>
            <svg viewBox="0 0 180 28" class="h-7 w-1/2" fill="none">
              <path d="M14 14H166" stroke="#475569" stroke-dasharray="4 4" />
              <circle cx="14" cy="14" r="5" fill="#0c4a6e" stroke="#38bdf8" />
              <circle cx="90" cy="14" r="5" fill="#0c4a6e" stroke="#38bdf8" />
              <circle cx="166" cy="14" r="5" fill="#0c4a6e" stroke="#38bdf8" />
            </svg>
          </div>
        </div>
      </div>
    </aside>

    <div class="flex min-w-0 flex-col bg-background px-6 sm:px-10">
      <header class="flex h-16 items-center justify-between gap-4 lg:h-20 lg:justify-end">
        <AppBrand class="text-base lg:hidden" />
        <ThemeToggle />
      </header>
      <main class="grid flex-1 place-items-center pt-8 pb-20">
        <LoginForm class="w-full max-w-sm" />
      </main>
    </div>
  </div>
</template>

<style scoped>
.login-cover {
  background-image:
    radial-gradient(ellipse at 22% 62%, rgb(14 165 233 / 0.12), transparent 58%),
    linear-gradient(rgb(148 163 184 / 0.09) 1px, transparent 1px),
    linear-gradient(90deg, rgb(148 163 184 / 0.09) 1px, transparent 1px);
  background-size: auto, 32px 32px, 32px 32px;
}
</style>
