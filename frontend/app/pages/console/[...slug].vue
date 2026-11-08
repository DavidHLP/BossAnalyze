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
      <UiCardDescription>此功能尚未开放，当前仅提供已授权的导航入口。请返回工作台使用现有功能。</UiCardDescription>
    </UiCardHeader>
    <UiCardFooter
      ><UiButton as-child variant="outline"
        ><NuxtLink to="/console">返回工作台</NuxtLink></UiButton
      ></UiCardFooter
    >
  </UiCard>
</template>
