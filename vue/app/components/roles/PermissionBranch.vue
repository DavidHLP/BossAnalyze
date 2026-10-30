<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { ArrowRight01Icon } from '@hugeicons/core-free-icons'
import type { PermissionNode } from '@/lib/roles'
defineProps<{ node: PermissionNode; selected: number[]; disabled?: boolean; readonly?: boolean }>()
const emit = defineEmits<{ toggle: [id: number, checked: boolean] }>()
</script>

<template>
  <UiCollapsible v-if="node.children.length" :default-open="true" as-child>
    <li>
      <div class="flex min-h-10 items-center gap-2 py-1">
        <UiCollapsibleTrigger as-child
          ><UiButton
            variant="ghost"
            size="icon-sm"
            :aria-label="`展开或收起${node.title}`"
            class="group shrink-0"
            ><HugeiconsIcon
              :icon="ArrowRight01Icon"
              class="size-4 group-data-open:rotate-90"
              aria-hidden="true" /></UiButton
        ></UiCollapsibleTrigger>
        <UiCheckbox
          :model-value="selected.includes(node.id)"
          :disabled="disabled || readonly || !node.permission"
          :aria-label="`授权${node.title}`"
          @update:model-value="(value) => emit('toggle', node.id, value === true)"
        />
        <span class="min-w-0 flex-1 break-words text-sm font-medium"
          >{{ node.title
          }}<span v-if="node.status === 0" class="ml-2 text-xs text-muted-foreground">停用</span
          ><span class="mt-0.5 block break-all text-xs font-normal text-muted-foreground">{{
            node.permission || '无独立权限标识'
          }}</span></span
        >
      </div>
      <UiCollapsibleContent
        ><ul class="ml-4 space-y-1 border-l-[0.5px] border-border/70 pl-3">
          <RolesPermissionBranch
            v-for="child in node.children"
            :key="child.id"
            :node="child"
            :selected="selected"
            :disabled="disabled"
            :readonly="readonly"
            @toggle="(id, checked) => emit('toggle', id, checked)"
          /></ul
      ></UiCollapsibleContent>
    </li>
  </UiCollapsible>
  <li v-else class="flex min-h-10 items-center gap-2 py-1 pl-8">
    <UiCheckbox
      :model-value="selected.includes(node.id)"
      :disabled="disabled || readonly || !node.permission"
      :aria-label="`授权${node.title}`"
      @update:model-value="(value) => emit('toggle', node.id, value === true)"
    />
    <span class="min-w-0 flex-1 break-words text-sm"
      >{{ node.title
      }}<span v-if="node.status === 0" class="ml-2 text-xs text-muted-foreground">停用</span
      ><span class="mt-0.5 block break-all text-xs text-muted-foreground">{{
        node.permission || '无独立权限标识'
      }}</span></span
    >
  </li>
</template>
