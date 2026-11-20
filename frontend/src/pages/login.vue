<script setup lang="ts">
import { RiBarChartGroupedLine, RiShieldCheckLine } from '@remixicon/vue'
import AppBrand from '@/components/AppBrand.vue'
import LoginForm from '@/components/LoginForm.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const salaryBuckets = [
  { label: '10K', height: 35 }, { label: '20K', height: 62 }, { label: '30K', height: 91 },
  { label: '40K', height: 69 }, { label: '50K', height: 49 }, { label: '60K', height: 28 },
]
</script>

<template>
  <div class="grid min-h-svh lg:grid-cols-[49%_51%]">
    <aside class="login-hero relative isolate hidden flex-col px-10 py-12 lg:flex xl:px-16 xl:py-15">
      <AppBrand class="self-start" />
      <div class="flex flex-1 flex-col justify-center gap-5 py-10">
        <p class="mb-8 text-4xl font-semibold leading-tight tracking-tight sm:text-5xl">
          招聘数据洞察<br />与权限治理工作台
        </p>
        <section class="login-hero-panel flex flex-col gap-4 rounded-lg border p-6" aria-label="招聘薪资示意数据">
          <div class="flex items-center gap-3">
            <RiBarChartGroupedLine class="size-5 text-chart-2" aria-hidden="true" />
            <h2 class="font-semibold">招聘薪资</h2>
            <span class="ml-auto text-xs opacity-75">示意数据</span>
          </div>
          <div class="grid grid-cols-2 items-center gap-4">
            <div class="flex flex-col gap-2">
              <p class="text-4xl font-semibold tabular-nums">24.8K</p>
              <p class="text-sm opacity-75">职位样本平均薪资</p>
            </div>
            <div class="border-l border-current/15 pl-4" aria-hidden="true">
              <div class="login-salary-plot flex h-26 items-end gap-2 border-b border-current/15 px-1">
                <span
                  v-for="bucket in salaryBuckets"
                  :key="bucket.label"
                  class="min-w-0 flex-1 bg-chart-2/70"
                  :style="{ height: bucket.height + '%' }"
                />
              </div>
              <div class="flex justify-between gap-1 py-2 text-xs tabular-nums opacity-75">
                <span v-for="bucket in salaryBuckets" :key="bucket.label">{{ bucket.label }}</span>
              </div>
            </div>
          </div>
        </section>
        <section class="login-hero-panel flex flex-wrap items-center justify-between gap-4 rounded-lg border p-6">
          <div class="flex items-start gap-3">
            <RiShieldCheckLine class="size-5 text-chart-2" aria-hidden="true" />
            <div class="flex flex-col gap-1">
              <h2 class="font-semibold">权限治理</h2>
              <p class="text-sm opacity-75">用户、角色与菜单授权</p>
            </div>
          </div>
          <div class="flex gap-2 text-xs">
            <span v-for="item in ['用户', '角色', '菜单']" :key="item" class="rounded-md border border-current/15 px-3 py-1">{{ item }}</span>
          </div>
        </section>
      </div>
    </aside>

    <div class="relative flex min-w-0 flex-col bg-card px-6 sm:px-10">
      <header class="flex h-16 shrink-0 items-center justify-between lg:absolute lg:top-0 lg:right-8">
        <AppBrand class="lg:hidden" />
        <ThemeToggle />
      </header>
      <main class="grid flex-1 place-items-center py-8">
        <LoginForm class="w-full max-w-[24.5rem]" />
      </main>
    </div>
  </div>
</template>

<style scoped>
.login-hero {
  background-color: var(--sidebar-foreground);
  color: var(--sidebar);
}
.login-hero::before {
  content: '';
  position: absolute;
  z-index: -1;
  inset: 8rem 0 3rem;
  background-image: linear-gradient(to right, currentColor 1px, transparent 1px),
    linear-gradient(to bottom, currentColor 1px, transparent 1px);
  background-size: 3rem 3rem;
  opacity: .09;
  pointer-events: none;
}
.login-hero-panel {
  background-color: color-mix(in oklch, var(--sidebar-foreground) 92%, var(--sidebar));
  border-color: color-mix(in oklch, currentColor 20%, transparent);
}
.login-salary-plot {
  background: repeating-linear-gradient(to top, transparent 0 calc(32% - 1px), color-mix(in oklch, currentColor 15%, transparent) calc(32% - 1px) 32%);
}
:global(.dark) .login-hero {
  background-color: var(--sidebar);
  color: var(--sidebar-foreground);
}
:global(.dark) .login-hero-panel {
  background-color: var(--background);
}
</style>
