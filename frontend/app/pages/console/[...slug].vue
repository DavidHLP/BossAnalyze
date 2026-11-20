<script setup lang="ts">
import { ArrowLeft01Icon, FolderOpenIcon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/vue'
import { findMenu } from '@/lib/session'

definePageMeta({ layout: 'console', middleware: 'auth' })
const route = useRoute()
const { state: session } = useSession()
const current = computed(() => findMenu(session.value.menus, route.path))
</script>

<template>
  <section class="space-y-5" aria-labelledby="placeholder-title">
    <h1 id="placeholder-title" class="text-2xl font-semibold tracking-tight">{{ current?.title }}</h1>
    <div class="grid min-h-72 place-items-center rounded-lg border bg-card px-6 py-12 text-center">
      <div class="grid justify-items-center gap-4">
        <span class="grid size-12 place-items-center rounded-lg bg-muted text-muted-foreground">
          <HugeiconsIcon :icon="FolderOpenIcon" class="size-6" aria-hidden="true" />
        </span>
        <h2 class="text-lg font-semibold">此功能尚未开放</h2>
        <UiButton as-child class="h-9 gap-2 px-4 text-sm">
          <NuxtLink to="/console">
            <HugeiconsIcon :icon="ArrowLeft01Icon" aria-hidden="true" />
            返回工作台
          </NuxtLink>
        </UiButton>
      </div>
    </div>
  </section>
</template>
