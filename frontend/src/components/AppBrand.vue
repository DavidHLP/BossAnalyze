<script setup lang="ts">
import { Briefcase01Icon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/vue'
</script>

<template>
  <span class="inline-flex min-w-0 items-center gap-2.5">
    <span class="grid size-8 shrink-0 place-items-center rounded-md bg-primary text-primary-foreground">
      <HugeiconsIcon :icon="Briefcase01Icon" class="size-4" aria-hidden="true" />
    </span>
    <span class="truncate font-semibold tracking-tight group-data-[collapsible=icon]:sr-only">
      BossAnalyze
    </span>
  </span>
</template>
