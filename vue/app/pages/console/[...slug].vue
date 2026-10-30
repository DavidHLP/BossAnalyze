<script setup lang="ts">
import { findMenu } from '@/lib/session'
definePageMeta({ layout: 'console', middleware: 'auth' })
const route = useRoute()
const { state: session } = useSession()
const current = computed(() => findMenu(session.value.menus, route.path))
</script>

<template>
  <UiCard>
    <UiCardHeader>
      <UiCardTitle
        ><h1>{{ current?.title }}</h1></UiCardTitle
      >
      <UiCardDescription>此业务页面正在重新构建，现有数据保持不变。</UiCardDescription>
    </UiCardHeader>
    <UiCardFooter
      ><UiButton as-child variant="outline"
        ><NuxtLink to="/console">返回工作台</NuxtLink></UiButton
      ></UiCardFooter
    >
  </UiCard>
</template>
