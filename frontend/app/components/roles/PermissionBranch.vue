<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { ArrowRight01Icon } from '@hugeicons/core-free-icons'
import type { PermissionNode } from '@/lib/roles'

defineProps<{ node: PermissionNode; selected: number[]; disabled?: boolean }>()
const emit = defineEmits<{ toggle: [id: number, checked: boolean] }>()
</script>

<template>
  <UiCollapsible :default-open="true" as-child>
    <li>
      <div class="flex min-h-10 items-center gap-2 rounded-md px-1 py-1 hover:bg-muted/50">
        <UiCollapsibleTrigger v-if="node.children.length" as-child>
          <UiButton
            variant="ghost"
            size="icon-sm"
            :aria-label="`展开或收起${node.title}`"
            class="group shrink-0"
          >
            <HugeiconsIcon :icon="ArrowRight01Icon" class="size-4 group-data-open:rotate-90"
              aria-hidden="true" />
          </UiButton>
        </UiCollapsibleTrigger>
        <span v-else class="w-8 shrink-0" aria-hidden="true" />
        <UiCheckbox
          :model-value="selected.includes(node.id)"
          :disabled="disabled || !node.permission"
          :aria-label="`授权${node.title}`"
          class="shrink-0"
          @update:model-value="(value) => emit('toggle', node.id, value === true)"
        />
        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-1.5">
            <span class="min-w-0 break-all text-sm font-medium">{{ node.title }}</span>
            <StatusBadge v-if="node.type === 'F'">操作</StatusBadge>
            <StatusBadge v-if="node.status === 0" tone="warning">停用</StatusBadge>
          </div>
          <p v-if="node.permission" class="mt-0.5 break-all font-mono text-xs text-muted-foreground">
            {{ node.permission }}
          </p>
          <p v-else class="mt-0.5 text-xs text-muted-foreground">无独立权限标识，随子项选中</p>
        </div>
      </div>
      <UiCollapsibleContent v-if="node.children.length">
        <ul class="ml-1 space-y-1 border-l border-border pl-1 sm:ml-3 sm:pl-2">
          <RolesPermissionBranch
            v-for="child in node.children"
            :key="child.id"
            :node="child"
            :selected="selected"
            :disabled="disabled"
            @toggle="(id, checked) => emit('toggle', id, checked)"
          />
        </ul>
      </UiCollapsibleContent>
    </li>
  </UiCollapsible>
</template>
