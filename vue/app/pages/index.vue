<template>
  <main class="flex min-h-svh items-center justify-center bg-muted/30 px-6 py-12">
    <section class="w-full max-w-lg" aria-labelledby="welcome-title">
      <p class="mb-4 text-xs font-medium tracking-widest text-muted-foreground">BOSSANALYZE</p>
      <UiCard>
        <UiCardHeader class="gap-3">
          <UiCardTitle>
            <h1 id="welcome-title" class="text-2xl font-semibold tracking-tight">
              招聘数据分析的新起点
            </h1>
          </UiCardTitle>
          <UiCardDescription class="text-sm leading-6">
            全新的前端基础已准备就绪，业务界面即将重新构建。
          </UiCardDescription>
        </UiCardHeader>
        <UiCardFooter class="flex flex-wrap gap-2">
          <UiButton as-child size="lg">
            <a href="https://shadcn-vue.com/docs/components" target="_blank" rel="noopener noreferrer">
              查看组件文档<span class="sr-only">（在新窗口打开）</span>
            </a>
          </UiButton>
          <UiButton as-child variant="outline" size="lg">
            <a href="https://github.com/DavidHLP/BossAnalyze/blob/main/docs/frontend-api/README.md"
               target="_blank" rel="noopener noreferrer">
              查看接口清单<span class="sr-only">（在新窗口打开）</span>
            </a>
          </UiButton>
        </UiCardFooter>
      </UiCard>
    </section>
  </main>
</template>
