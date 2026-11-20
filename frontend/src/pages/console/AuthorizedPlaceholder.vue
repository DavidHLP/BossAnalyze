<script setup lang="ts">
import { computed, inject } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSession } from '@/composables/useSession'
import { ArrowLeft01Icon, FolderOpenIcon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/vue'
import type { ComputedRef } from 'vue'
import { findMenu, safeReturnPath, type MenuItem } from '@/lib/session'

const route = useRoute()
const router = useRouter()
const { state: session } = useSession()
const current = computed(() => findMenu(session.value.menus, route.path))
const trail = inject<ComputedRef<MenuItem[]>>('consoleMenuTrail')
const parent = computed(() => trail?.value.at(-2))
const title = computed(() => current.value?.title.trim() || '未命名功能')

function goBack() {
  const previous = safeReturnPath(router.options.history.state.back)
  if (previous && previous !== route.fullPath) {
    router.back()
    return
  }
  return router.push(parent.value?.path ?? '/console')
}
</script>

<template>
  <section class="space-y-5" aria-labelledby="placeholder-title">
    <h1 id="placeholder-title" class="text-2xl font-semibold tracking-tight">{{ title }}</h1>
    <div class="grid min-h-72 place-items-center rounded-lg border bg-card px-6 py-12 text-center">
      <div class="grid justify-items-center gap-4">
        <span class="grid size-12 place-items-center rounded-lg bg-muted text-muted-foreground">
          <HugeiconsIcon :icon="FolderOpenIcon" class="size-6" aria-hidden="true" />
        </span>
        <h2 class="text-lg font-semibold">此功能尚未开放</h2>
        <p class="text-sm text-muted-foreground">所属模块：{{ parent?.title.trim() || '控制台' }}</p>
        <div class="flex flex-wrap justify-center gap-2">
          <UiButton type="button" @click="goBack">
            <HugeiconsIcon :icon="ArrowLeft01Icon" aria-hidden="true" />
            返回上一页
          </UiButton>
          <UiButton as-child variant="outline">
            <RouterLink to="/console">返回工作台</RouterLink>
          </UiButton>
        </div>
      </div>
    </div>
  </section>
</template>
