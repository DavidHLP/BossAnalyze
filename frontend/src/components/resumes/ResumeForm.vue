<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  parseResumeForm, renderResumeForm, resumeFormValues,
  type ResumeFormDocument, type ResumeFormValues, type ResumeFormSection,
} from '@/lib/resume-form'

defineProps<{ disabled?: boolean }>()
const model = defineModel<string>({ required: true })

const doc = ref<ResumeFormDocument>(parseResumeForm(model.value))
const values = ref<ResumeFormValues>(resumeFormValues(doc.value))
let lastRendered = model.value

watch(model, next => {
  if (next === lastRendered) return
  doc.value = parseResumeForm(next)
  values.value = resumeFormValues(doc.value)
  lastRendered = next
})

function commit() {
  const next = renderResumeForm(doc.value, values.value)
  lastRendered = next
  if (next !== model.value) model.value = next
}

function addEducation() {
  values.value.education.push({ period: '', school: '', major: '', degree: '', details: '' })
  commit()
}

function addWork() {
  values.value.work.push({ period: '', company: '', role: '', details: '' })
  commit()
}

function removeEducation(index: number) {
  values.value.education.splice(index, 1)
  commit()
}

function removeWork(index: number) {
  values.value.work.splice(index, 1)
  commit()
}

const SECTION_NAMES: Record<ResumeFormSection, string> = {
  basics: '基本信息',
  education: '教育经历',
  work: '工作经历',
}
</script>

<template>
  <div class="flex flex-col gap-4 p-3">
    <p class="text-xs text-muted-foreground">
      表单只编辑基本信息、教育经历与工作经历。其他章节、重复标题和复杂格式保持原文，切换到高级 Markdown 不会改写正文。
    </p>
    <UiAlert v-if="doc.duplicateSections.length" role="status">
      <UiAlertDescription>
        {{ doc.duplicateSections.map(kind => SECTION_NAMES[kind]).join('、') }}存在重复标题，已保留原文，请切换到高级 Markdown 编辑。
      </UiAlertDescription>
    </UiAlert>
    <fieldset :disabled="disabled" class="flex min-w-0 flex-col gap-6">
      <section v-if="!doc.duplicateSections.includes('basics')" class="flex min-w-0 flex-col gap-3" aria-labelledby="resume-form-basics">
        <h3 id="resume-form-basics" class="font-medium">基本信息</h3>
        <UiFieldGroup class="grid gap-3 sm:grid-cols-2">
          <UiField>
            <UiFieldLabel for="resume-form-name">姓名</UiFieldLabel>
            <UiInput id="resume-form-name" v-model="values.basics.name" maxlength="60" placeholder="请输入姓名" @update:model-value="commit" />
          </UiField>
          <UiField>
            <UiFieldLabel for="resume-form-phone">电话</UiFieldLabel>
            <UiInput id="resume-form-phone" v-model="values.basics.phone" type="tel" maxlength="40" placeholder="138-0000-0000" @update:model-value="commit" />
          </UiField>
          <UiField>
            <UiFieldLabel for="resume-form-email">邮箱</UiFieldLabel>
            <UiInput id="resume-form-email" v-model="values.basics.email" type="email" maxlength="120" placeholder="name@example.com" @update:model-value="commit" />
          </UiField>
          <UiField>
            <UiFieldLabel for="resume-form-city">城市</UiFieldLabel>
            <UiInput id="resume-form-city" v-model="values.basics.city" maxlength="60" placeholder="北京" @update:model-value="commit" />
          </UiField>
          <UiField class="sm:col-span-2">
            <UiFieldLabel for="resume-form-objective">求职意向</UiFieldLabel>
            <UiInput id="resume-form-objective" v-model="values.basics.objective" maxlength="120" placeholder="例如：前端开发工程师" @update:model-value="commit" />
          </UiField>
        </UiFieldGroup>
      </section>

      <section v-if="!doc.duplicateSections.includes('education')" class="flex min-w-0 flex-col gap-3" aria-labelledby="resume-form-education">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <h3 id="resume-form-education" class="font-medium">教育经历</h3>
          <UiButton type="button" variant="outline" size="sm" @click="addEducation">添加教育经历</UiButton>
        </div>
        <p v-if="!values.education.length" class="text-sm text-muted-foreground">还没有教育经历。</p>
        <div v-for="(entry, index) in values.education" :key="'education-' + index" class="flex min-w-0 flex-col gap-3 border p-3">
          <div class="flex items-center justify-between gap-2">
            <p class="text-sm font-medium">教育经历 {{ index + 1 }}</p>
            <UiButton type="button" variant="ghost" size="sm" @click="removeEducation(index)">删除</UiButton>
          </div>
          <UiFieldGroup class="grid gap-3 sm:grid-cols-2">
            <UiField>
              <UiFieldLabel :for="'resume-form-edu-period-' + index">起止时间</UiFieldLabel>
              <UiInput :id="'resume-form-edu-period-' + index" v-model="entry.period" maxlength="60" placeholder="2019 年 09 月 - 2023 年 06 月" @update:model-value="commit" />
            </UiField>
            <UiField>
              <UiFieldLabel :for="'resume-form-edu-school-' + index">学校</UiFieldLabel>
              <UiInput :id="'resume-form-edu-school-' + index" v-model="entry.school" maxlength="80" placeholder="某某大学" @update:model-value="commit" />
            </UiField>
            <UiField>
              <UiFieldLabel :for="'resume-form-edu-major-' + index">专业</UiFieldLabel>
              <UiInput :id="'resume-form-edu-major-' + index" v-model="entry.major" maxlength="80" placeholder="计算机科学与技术" @update:model-value="commit" />
            </UiField>
            <UiField>
              <UiFieldLabel :for="'resume-form-edu-degree-' + index">学历</UiFieldLabel>
              <UiInput :id="'resume-form-edu-degree-' + index" v-model="entry.degree" maxlength="40" placeholder="本科" @update:model-value="commit" />
            </UiField>
            <UiField class="sm:col-span-2">
              <UiFieldLabel :for="'resume-form-edu-details-' + index">详情（每行一条）</UiFieldLabel>
              <UiTextarea :id="'resume-form-edu-details-' + index" v-model="entry.details" class="min-h-16" placeholder="主修课程、奖学金等" @update:model-value="commit" />
            </UiField>
          </UiFieldGroup>
        </div>
      </section>

      <section v-if="!doc.duplicateSections.includes('work')" class="flex min-w-0 flex-col gap-3" aria-labelledby="resume-form-work">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <h3 id="resume-form-work" class="font-medium">工作经历</h3>
          <UiButton type="button" variant="outline" size="sm" @click="addWork">添加工作经历</UiButton>
        </div>
        <p v-if="!values.work.length" class="text-sm text-muted-foreground">还没有工作经历。</p>
        <div v-for="(entry, index) in values.work" :key="'work-' + index" class="flex min-w-0 flex-col gap-3 border p-3">
          <div class="flex items-center justify-between gap-2">
            <p class="text-sm font-medium">工作经历 {{ index + 1 }}</p>
            <UiButton type="button" variant="ghost" size="sm" @click="removeWork(index)">删除</UiButton>
          </div>
          <UiFieldGroup class="grid gap-3 sm:grid-cols-2">
            <UiField>
              <UiFieldLabel :for="'resume-form-work-period-' + index">起止时间</UiFieldLabel>
              <UiInput :id="'resume-form-work-period-' + index" v-model="entry.period" maxlength="60" placeholder="2023 年 07 月 - 至今" @update:model-value="commit" />
            </UiField>
            <UiField>
              <UiFieldLabel :for="'resume-form-work-company-' + index">公司</UiFieldLabel>
              <UiInput :id="'resume-form-work-company-' + index" v-model="entry.company" maxlength="80" placeholder="某某公司" @update:model-value="commit" />
            </UiField>
            <UiField class="sm:col-span-2">
              <UiFieldLabel :for="'resume-form-work-role-' + index">职位</UiFieldLabel>
              <UiInput :id="'resume-form-work-role-' + index" v-model="entry.role" maxlength="80" placeholder="前端开发工程师" @update:model-value="commit" />
            </UiField>
            <UiField class="sm:col-span-2">
              <UiFieldLabel :for="'resume-form-work-details-' + index">详情（每行一条）</UiFieldLabel>
              <UiTextarea :id="'resume-form-work-details-' + index" v-model="entry.details" class="min-h-16" placeholder="职责与可量化结果" @update:model-value="commit" />
            </UiField>
          </UiFieldGroup>
        </div>
      </section>
    </fieldset>
  </div>
</template>
