<script setup lang="ts">
import { useMediaQuery } from '@vueuse/core'
import { safeSourceUrl } from '@/lib/recruitment'
import type { JobSample } from '@/lib/recruitment'
const props = defineProps<{ job: JobSample | null }>()
const emit = defineEmits<{ close: [] }>()
const mobile = useMediaQuery('(max-width: 639px)')
const fields = computed(() =>
  props.job
    ? [
        { label: '职位 ID', value: props.job.positionId },
        { label: '城市', value: props.job.cityName },
        { label: '原始薪资', value: props.job.salary },
        { label: '统计薪资值（K）', value: props.job.salaryValue },
        { label: '学历要求', value: props.job.degree },
        { label: '经验要求', value: props.job.experience },
        { label: '公司规模', value: props.job.companySize },
        { label: '融资阶段', value: props.job.financingStage },
      ]
    : [],
)
</script>
<template>
  <UiSheet
    :open="Boolean(job)"
    @update:open="
      (value) => {
        if (!value) emit('close')
      }
    "
    ><UiSheetContent
      :side="mobile ? 'bottom' : 'right'"
      class="w-full! sm:max-w-[600px]! data-[side=bottom]:h-[80dvh] data-[side=bottom]:rounded-t-xl"
    >
      <UiSheetHeader class="shrink-0 border-b-[0.5px] border-border pr-12"
        ><UiSheetTitle class="break-words">{{ job?.positionName || '职位详情' }}</UiSheetTitle
        ><UiSheetDescription class="break-words">{{
          job?.companyName || '未提供公司名称'
        }}</UiSheetDescription></UiSheetHeader
      >
      <div v-if="job" class="min-h-0 flex-1 space-y-6 overflow-y-auto p-6">
        <dl class="grid grid-cols-2 gap-5 text-sm">
          <div v-for="field in fields" :key="field.label">
            <dt class="text-xs text-muted-foreground">{{ field.label }}</dt>
            <dd class="mt-1.5 break-words">{{ field.value ?? '—' }}</dd>
          </div>
          <div class="col-span-2">
            <dt class="text-xs text-muted-foreground">地址</dt>
            <dd class="mt-1.5 whitespace-pre-wrap break-words">{{ job.address || '—' }}</dd>
          </div>
        </dl>
        <section class="space-y-3">
          <h3 class="text-sm font-medium">职位福利</h3>
          <div v-if="job.employeeBenefits.length" class="flex flex-wrap gap-2">
            <UiBadge
              v-for="(item, index) in job.employeeBenefits"
              :key="index"
              variant="secondary"
              class="max-w-full whitespace-normal break-words bg-muted text-muted-foreground"
              >{{ item }}</UiBadge
            >
          </div>
          <p v-else class="text-sm text-muted-foreground">暂无可用福利信息。</p>
        </section>
        <section class="space-y-3">
          <h3 class="text-sm font-medium">工作要求</h3>
          <ul v-if="job.jobRequirements.length" class="list-disc space-y-2 pl-4 text-sm leading-6">
            <li
              v-for="(item, index) in job.jobRequirements"
              :key="index"
              class="whitespace-pre-wrap break-words"
            >
              {{ item }}
            </li>
          </ul>
          <p v-else class="text-sm text-muted-foreground">暂无可用工作要求。</p>
        </section>
      </div>
      <UiSheetFooter class="shrink-0 flex-row flex-wrap justify-end border-t-[0.5px] border-border"
        ><UiButton variant="outline" @click="emit('close')">关闭</UiButton
        ><UiButton v-if="safeSourceUrl(job?.companyUrl ?? null)" variant="outline" as-child
          ><a
            :href="safeSourceUrl(job?.companyUrl ?? null)!"
            target="_blank"
            rel="noopener noreferrer"
            >公司来源</a
          ></UiButton
        ><UiButton v-if="safeSourceUrl(job?.jobUrl ?? null)" as-child
          ><a :href="safeSourceUrl(job?.jobUrl ?? null)!" target="_blank" rel="noopener noreferrer"
            >职位来源</a
          ></UiButton
        ></UiSheetFooter
      >
    </UiSheetContent></UiSheet
  >
</template>
