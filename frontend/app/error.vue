<script setup lang="ts">
import type { NuxtError } from '#app'

const props = defineProps<{ error: NuxtError }>()
const title = computed(() =>
  props.error.statusCode === 403
    ? '无权访问此页面'
    : props.error.statusCode === 404
      ? '页面不存在'
      : '页面暂时无法加载',
)
useHead({ title: () => `${title.value} · BossAnalyze` })
</script>

<template>
  <main class="flex min-h-svh items-center justify-center bg-background p-6 text-foreground">
    <section class="w-full max-w-md space-y-6" aria-labelledby="error-title">
      <p class="text-sm font-medium">BossAnalyze · {{ error.statusCode }}</p>
      <h1 id="error-title" class="text-2xl font-semibold tracking-tight">{{ title }}</h1>
      <p class="text-sm text-muted-foreground">
        {{
          error.statusCode === 403
            ? '当前账号没有此页面的访问权限，请返回工作台或使用其他账号登录。'
            : '请返回工作台继续操作，或稍后重试。'
        }}
      </p>
      <div class="flex flex-wrap gap-3">
        <UiButton @click="clearError({ redirect: '/console' })">返回工作台</UiButton>
        <UiButton variant="outline" @click="clearError({ redirect: '/login' })">前往登录</UiButton>
      </div>
    </section>
  </main>
</template>
