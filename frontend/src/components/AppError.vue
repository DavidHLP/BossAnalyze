<template>
  <main class="grid min-h-svh place-items-center bg-background px-6">
    <section class="flex w-full max-w-lg flex-col gap-5 rounded-lg border bg-card p-6 text-card-foreground sm:p-8">
      <p class="font-mono text-5xl font-semibold text-accent-data-foreground">500</p>
      <h1 class="text-2xl font-semibold tracking-tight">暂时无法加载页面</h1>
      <p class="text-sm text-muted-foreground">请刷新页面，或返回工作台继续操作。</p>
      <div class="flex flex-wrap gap-2">
        <a class="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground" href="/console">返回工作台</a>
        <a class="rounded-md border px-4 py-2 text-sm font-medium" href="/login">前往登录</a>
      </div>
    </section>
  </main>
</template>
