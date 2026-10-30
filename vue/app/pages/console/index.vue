<script setup lang="ts">
definePageMeta({ layout: 'console', middleware: 'auth' })
const { state: session } = useSession()
</script>

<template>
  <section class="flex flex-col gap-6">
    <div>
      <p class="mb-2 text-sm text-muted-foreground">工作台</p>
      <h1 class="break-words text-2xl font-semibold tracking-tight">
        欢迎回来，{{ session.user?.name }}
      </h1>
      <p class="mt-3 text-sm leading-6 text-muted-foreground">从左侧导航开始，继续你的工作。</p>
    </div>
    <UiCard>
      <UiCardHeader>
        <UiCardTitle>新的工作空间</UiCardTitle>
        <UiCardDescription>控制台框架已就绪，业务页面正在重新构建。</UiCardDescription>
      </UiCardHeader>
      <UiCardContent>
        <p v-if="!session.menus.length" role="status" class="text-sm text-muted-foreground">
          当前账号暂无可用菜单。如需更多功能，请联系管理员。
        </p>
        <p v-else class="text-sm text-muted-foreground">左侧仅显示当前账号有权限访问的菜单。</p>
      </UiCardContent>
    </UiCard>
  </section>
</template>
