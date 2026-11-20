<script setup lang="ts">
import AppBrand from '@/components/AppBrand.vue'
import LoginForm from '@/components/LoginForm.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const exampleDistribution = [18, 31, 58, 83, 70, 43, 23]
</script>

<template>
  <div class="grid min-h-svh lg:grid-cols-[49%_51%]">
    <aside class="hidden flex-col border-r bg-brand-surface p-12 text-brand-foreground lg:flex xl:p-16">
      <AppBrand class="self-start" />
      <div class="mx-auto flex w-full max-w-lg flex-1 flex-col justify-center gap-6 py-10">
        <p class="text-4xl font-semibold leading-tight tracking-tight text-balance xl:text-5xl">
          招聘数据洞察<br />与权限治理工作台
        </p>
        <UiCard aria-hidden="true" class="gap-4">
          <UiCardHeader>
            <UiCardTitle>招聘薪资</UiCardTitle>
            <UiCardDescription>示意数据 · 职位样本平均薪资</UiCardDescription>
          </UiCardHeader>
          <UiCardContent class="flex flex-col gap-2">
            <p class="text-3xl font-semibold tabular-nums">24.8K</p>
            <div class="flex h-20 items-end gap-2 border-b">
              <span
                v-for="(height, index) in exampleDistribution"
                :key="index"
                class="min-w-0 flex-1 bg-chart-3/70"
                :style="{ height: height + '%' }"
              />
            </div>
            <div class="flex justify-between text-xs text-muted-foreground tabular-nums">
              <span v-for="value in [10, 20, 30, 40, 50, 60]" :key="value">{{ value }}K</span>
            </div>
          </UiCardContent>
        </UiCard>
        <UiCard class="gap-3">
          <UiCardHeader>
            <UiCardTitle>权限治理</UiCardTitle>
            <UiCardDescription>用户、角色与菜单授权</UiCardDescription>
          </UiCardHeader>
          <UiCardContent class="flex flex-wrap gap-2">
            <UiBadge variant="secondary">用户</UiBadge>
            <UiBadge variant="secondary">角色</UiBadge>
            <UiBadge variant="secondary">菜单</UiBadge>
          </UiCardContent>
        </UiCard>
      </div>
    </aside>

    <div class="flex min-w-0 flex-col bg-card px-6 sm:px-10">
      <header class="flex h-16 shrink-0 items-center justify-between lg:h-20 lg:justify-end">
        <AppBrand class="lg:hidden" />
        <ThemeToggle />
      </header>
      <main class="grid flex-1 place-items-center py-8">
        <LoginForm class="w-full max-w-[24.5rem]" />
      </main>
    </div>
  </div>
</template>
