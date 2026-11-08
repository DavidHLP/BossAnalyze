<script setup lang="ts">
import { HugeiconsIcon } from '@hugeicons/vue'
import { Search01Icon, RefreshIcon } from '@hugeicons/core-free-icons'
import {
  getCityNameList,
  getPositionNameList,
  getSalaryHotJob,
  getTwoDimensionalAnalysisChart,
} from '@/api/boss/user/user'
import {
  analysisAxes,
  analysisQuery,
  jobSamples,
  jobSummary,
  optionNames,
  safeCity,
  salaryRanking,
} from '@/lib/recruitment'
import type { JobSample, AnalysisFilter } from '@/lib/recruitment'
import { settingsFailure } from '@/lib/settings'

definePageMeta({ layout: 'console', middleware: 'signed-in' })
type State = 'idle' | 'loading' | 'ready' | 'error' | 'forbidden'
const draft = reactive<AnalysisFilter>({
  city: 'all',
  position: 'all',
  x: 'salary_value',
  y: 'degree_value',
})
const applied = shallowRef<AnalysisFilter | null>(null)
const cities = shallowRef<string[]>([])
const positions = shallowRef<string[]>([])
const cityState = ref<State>('loading')
const positionState = ref<State>('loading')
const jobs = shallowRef<JobSample[]>([])
const jobsState = ref<State>('idle')
const ranking = shallowRef<ReturnType<typeof salaryRanking>>([])
const rankingState = ref<State>('loading')
const rankingLimit = ref('10')
const selected = shallowRef<JobSample | null>(null)
const formError = ref('')
const page = ref(1)
const size = ref('20')
let positionRequest = 0
const cityOptions = computed(() => [
  { value: 'all', label: '全部城市' },
  ...cities.value.map((value) => ({ value, label: value, disabled: !safeCity(value) })),
])
const positionOptions = computed(() => [
  { value: 'all', label: '全部职位' },
  ...positions.value.map((value) => ({ value, label: value })),
])
const visible = computed(() =>
  jobs.value.slice((page.value - 1) * Number(size.value), page.value * Number(size.value)),
)
const summary = computed(() => (jobsState.value === 'ready' ? jobSummary(jobs.value) : null))
const changed = computed(
  () => applied.value && JSON.stringify(draft) !== JSON.stringify(applied.value),
)
const number = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 })
const scope = computed(() =>
  applied.value
    ? `${applied.value.city === 'all' ? '全部城市' : applied.value.city} / ${applied.value.position === 'all' ? '全部职位' : applied.value.position}`
    : '尚未查询',
)
async function loadCities() {
  cityState.value = 'loading'
  cities.value = []
  try {
    cities.value = optionNames(await getCityNameList())
    cityState.value = 'ready'
  } catch (error) {
    cityState.value = settingsFailure(error)
  }
}
async function loadPositions() {
  const request = ++positionRequest,
    city = draft.city
  positionState.value = 'loading'
  positions.value = []
  try {
    if (!safeCity(city)) throw new Error('Unsupported city')
    const result = optionNames(await getPositionNameList(city))
    if (request === positionRequest) {
      positions.value = result
      positionState.value = 'ready'
    }
  } catch (error) {
    if (request === positionRequest) positionState.value = settingsFailure(error)
  }
}
watch(
  () => draft.city,
  () => {
    draft.position = 'all'
    void loadPositions()
  },
)
watch(size, () => {
  page.value = 1
})
watch(rankingLimit, () => {
  void loadRanking()
})
async function loadRanking() {
  rankingState.value = 'loading'
  ranking.value = []
  try {
    if (!['5', '10', '20'].includes(rankingLimit.value)) throw new Error('Invalid limit')
    ranking.value = salaryRanking(await getSalaryHotJob(Number(rankingLimit.value)))
    rankingState.value = 'ready'
  } catch (error) {
    rankingState.value = settingsFailure(error)
  }
}
async function analyze() {
  if (jobsState.value === 'loading' || positionState.value === 'loading') return
  formError.value = ''
  let args: ReturnType<typeof analysisQuery>
  try {
    args = analysisQuery(draft, cities.value, positions.value)
  } catch {
    formError.value = '请选择有效的城市、职位与不同的分析维度。'
    return
  }
  jobsState.value = 'loading'
  jobs.value = []
  page.value = 1
  selected.value = null
  applied.value = { ...draft }
  try {
    jobs.value = jobSamples(await getTwoDimensionalAnalysisChart(...args))
    jobsState.value = 'ready'
  } catch (error) {
    jobsState.value = settingsFailure(error)
  }
}
function reset() {
  Object.assign(draft, { city: 'all', position: 'all', x: 'salary_value', y: 'degree_value' })
  formError.value = ''
}
onMounted(() => {
  void loadCities()
  void loadPositions()
  void loadRanking()
})
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="recruitment-title">
    <header class="space-y-2">
      <h1 id="recruitment-title" class="text-2xl font-semibold tracking-tight">招聘数据分析</h1>
      <p class="text-sm text-muted-foreground">按城市与职位比较招聘样本，查看薪资和任职要求。</p>
    </header>
    <form
      class="space-y-5 rounded-lg border-[0.5px] border-border/70 bg-card p-5"
      aria-label="招聘分析筛选"
      @submit.prevent="analyze"
    >
      <div class="grid gap-5 sm:grid-cols-2">
        <UiField
          ><UiFieldLabel for="recruit-city">城市</UiFieldLabel
          ><RecruitmentOptionPicker
            id="recruit-city"
            v-model="draft.city"
            :options="cityOptions"
            :disabled="cityState === 'loading' || jobsState === 'loading'"
          />
          <div
            v-if="cityState === 'error' || cityState === 'forbidden'"
            class="text-xs text-muted-foreground"
            role="alert"
          >
            {{ cityState === 'forbidden' ? '无权读取城市选项。' : '城市选项读取失败。'
            }}<UiButton type="button" variant="ghost" size="sm" @click="loadCities"
              >重试城市选项</UiButton
            >
          </div></UiField
        ><UiField
          ><UiFieldLabel for="recruit-position">职位</UiFieldLabel
          ><RecruitmentOptionPicker
            id="recruit-position"
            v-model="draft.position"
            :options="positionOptions"
            :disabled="positionState === 'loading' || jobsState === 'loading'"
          />
          <div
            v-if="positionState === 'error' || positionState === 'forbidden'"
            class="text-xs text-muted-foreground"
            role="alert"
          >
            {{
              positionState === 'forbidden'
                ? '无权读取职位选项。'
                : '职位选项读取失败，请重试或切换城市。'
            }}<UiButton type="button" variant="ghost" size="sm" @click="loadPositions"
              >重试职位选项</UiButton
            >
          </div></UiField
        >
      </div>
      <div class="grid gap-5 lg:grid-cols-2">
        <UiField
          v-for="axis in [
            { key: 'x', label: '横轴' },
            { key: 'y', label: '纵轴' },
          ] as const"
          :key="axis.key"
          ><UiFieldLabel as="span">{{ axis.label }}</UiFieldLabel
          ><UiRadioGroup
            v-model="draft[axis.key]"
            :aria-label="axis.label"
            class="flex flex-wrap gap-x-5 gap-y-3"
            :disabled="jobsState === 'loading'"
            ><label
              v-for="option in analysisAxes"
              :key="option.value"
              class="flex items-center gap-2 text-sm"
              ><UiRadioGroupItem :value="option.value" />{{ option.label }}</label
            ></UiRadioGroup
          ></UiField
        >
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <UiButton type="submit" :disabled="jobsState === 'loading' || positionState === 'loading'"
          ><HugeiconsIcon :icon="Search01Icon" class="size-4 -ml-0.5" aria-hidden="true" />{{
            jobsState === 'loading' ? '正在查询…' : '查询分析'
          }}</UiButton
        ><UiButton
          type="button"
          variant="outline"
          :disabled="jobsState === 'loading'"
          @click="reset"
          >重置筛选</UiButton
        >
        <p v-if="formError" role="alert" class="text-sm text-destructive">{{ formError }}</p>
        <p v-else-if="changed" role="status" class="text-xs text-muted-foreground">
          条件已调整，点击查询分析更新结果。
        </p>
      </div>
    </form>
    <div class="space-y-3">
      <p class="break-words text-xs text-muted-foreground">
        当前结果：{{ scope }}。统计仅基于返回样本；K 薪资之外的格式不参与薪资均值。
      </p>
      <div class="grid gap-4 sm:grid-cols-3">
        <UiCard
          v-for="metric in [
            {
              label: '返回职位样本',
              value: summary?.count,
              unit: '条',
              note: '所选维度数值有效的返回记录',
            },
            {
              label: '涉及公司',
              value: summary?.companies,
              unit: '家',
              note: '按已提供的公司名称去重',
            },
            {
              label: '样本薪资均值',
              value: summary?.average,
              unit: 'K',
              note: summary ? `基于 ${summary.salaryCount} 条可解析薪资样本` : '尚未取得薪资样本',
            },
          ]"
          :key="metric.label"
          class="gap-0 border-[0.5px] border-border/70 py-0 shadow-none ring-0"
          ><UiCardContent class="p-5"
            ><h2 class="text-xs text-muted-foreground">{{ metric.label }}</h2>
            <UiSkeleton v-if="jobsState === 'loading'" class="mt-4 h-8 w-24" />
            <p v-else class="mt-3 text-3xl font-semibold tabular-nums">
              {{ metric.value == null ? '—' : number.format(metric.value)
              }}<span class="ml-2 text-xs font-normal text-muted-foreground">{{
                metric.unit
              }}</span>
            </p>
            <p class="mt-2 text-xs text-muted-foreground">{{ metric.note }}</p></UiCardContent
          ></UiCard
        >
      </div>
    </div>
    <div
      class="min-w-0 rounded-lg border-[0.5px] border-border/70 bg-card"
      :aria-busy="jobsState === 'loading'"
    >
      <div class="border-b-[0.5px] border-border/70 px-5 py-4">
        <h2 class="text-sm font-semibold">职位样本分析</h2>
      </div>
      <div v-if="jobsState === 'idle'" role="status" class="space-y-3 px-6 py-12 text-center">
        <h3 class="text-sm font-medium">选择条件，开始查询</h3>
        <p class="text-sm text-muted-foreground">
          可比较薪资、学历和经验，并查看返回职位的详细信息。
        </p>
        <UiButton variant="outline" :disabled="positionState === 'loading'" @click="analyze"
          >查询当前条件</UiButton
        >
      </div>
      <div v-else-if="jobsState === 'loading'" role="status" class="space-y-4 p-6">
        <p class="text-sm text-muted-foreground">正在读取并分析职位样本…</p>
        <UiSkeleton class="h-52 w-full" />
      </div>
      <div
        v-else-if="jobsState === 'error' || jobsState === 'forbidden'"
        role="alert"
        class="space-y-3 px-6 py-12 text-center"
      >
        <h3 class="text-sm font-medium">
          {{ jobsState === 'forbidden' ? '当前账号无权读取招聘分析' : '招聘分析读取失败' }}
        </h3>
        <p class="text-sm text-muted-foreground">
          {{
            jobsState === 'forbidden'
              ? '请联系管理员确认访问权限。'
              : '请检查服务连接或调整筛选条件后重试。'
          }}
        </p>
        <UiButton variant="outline" @click="analyze">重试当前条件</UiButton>
      </div>
      <div v-else-if="!jobs.length" role="status" class="space-y-3 px-6 py-12 text-center">
        <h3 class="text-sm font-medium">暂无匹配的职位样本</h3>
        <p class="text-sm text-muted-foreground">
          当前条件没有返回所选维度均有效的样本，可调整城市、职位或分析维度。
        </p>
        <UiButton variant="outline" @click="reset">清除筛选条件</UiButton>
      </div>
      <template v-else-if="applied"
        ><div class="border-b-[0.5px] border-border/70 p-5">
          <RecruitmentScatterPlot :jobs="jobs" :x="applied.x" :y="applied.y" />
        </div>
        <UiTable
          ><UiTableCaption class="sr-only">招聘职位样本及薪资、学历和经验要求</UiTableCaption
          ><UiTableHeader
            ><UiTableRow class="border-b-[0.5px]"
              ><UiTableHead class="px-5">职位 / 公司</UiTableHead
              ><UiTableHead class="hidden sm:table-cell">城市</UiTableHead
              ><UiTableHead class="text-right">薪资</UiTableHead
              ><UiTableHead class="hidden md:table-cell">学历</UiTableHead
              ><UiTableHead class="hidden lg:table-cell">经验</UiTableHead
              ><UiTableHead class="w-20 text-center">操作</UiTableHead></UiTableRow
            ></UiTableHeader
          ><UiTableBody
            ><UiTableRow v-for="job in visible" :key="job.key" class="border-b-[0.5px]"
              ><UiTableCell class="min-w-44 max-w-80 whitespace-normal px-5"
                ><UiButton
                  variant="ghost"
                  class="h-auto justify-start whitespace-normal break-words p-0 text-left font-medium hover:bg-transparent"
                  @click="selected = job"
                  >{{ job.positionName || '未提供职位名称' }}</UiButton
                >
                <p class="mt-1 break-words text-xs text-muted-foreground">
                  {{ job.companyName || '未提供公司名称' }}
                </p>
                <p class="mt-1 text-xs text-muted-foreground sm:hidden">
                  {{ job.cityName || '—' }}
                </p></UiTableCell
              ><UiTableCell class="hidden sm:table-cell">{{ job.cityName || '—' }}</UiTableCell
              ><UiTableCell class="whitespace-normal text-right tabular-nums">{{
                job.salary || '—'
              }}</UiTableCell
              ><UiTableCell class="hidden md:table-cell">{{ job.degree || '—' }}</UiTableCell
              ><UiTableCell class="hidden lg:table-cell">{{ job.experience || '—' }}</UiTableCell
              ><UiTableCell class="text-center"
                ><UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`查看${job.positionName || '职位'}详情`"
                  @click="selected = job"
                  >详情</UiButton
                ></UiTableCell
              ></UiTableRow
            ></UiTableBody
          ></UiTable
        >
        <div
          class="flex flex-wrap items-center justify-between gap-4 border-t-[0.5px] border-border/70 p-4"
        >
          <UiRadioGroup v-model="size" aria-label="样本每页条数" class="flex gap-4"
            ><label
              v-for="count in [10, 20, 50]"
              :key="count"
              class="flex items-center gap-2 text-xs"
              ><UiRadioGroupItem :value="String(count)" />{{ count }} 条</label
            ></UiRadioGroup
          >
          <div class="flex items-center gap-3">
            <span class="text-xs tabular-nums text-muted-foreground">共 {{ jobs.length }} 条</span
            ><UiPagination
              v-model:page="page"
              :items-per-page="Number(size)"
              :total="jobs.length"
              :sibling-count="1"
              class="w-auto"
              aria-label="样本分页"
              ><UiPaginationContent v-slot="{ items }"
                ><UiPaginationPrevious aria-label="上一页" /><template
                  v-for="(item, index) in items"
                  :key="index"
                  ><UiPaginationItem v-if="item.type === 'page'" :value="item.value" as-child
                    ><UiButton
                      :variant="item.value === page ? 'outline' : 'ghost'"
                      size="icon-sm"
                      :aria-label="`第 ${item.value} 页`"
                      :aria-current="item.value === page ? 'page' : undefined"
                      >{{ item.value }}</UiButton
                    ></UiPaginationItem
                  ><UiPaginationEllipsis v-else /></template
                ><UiPaginationNext aria-label="下一页" /></UiPaginationContent
            ></UiPagination>
          </div>
        </div>
      </template>
    </div>
    <section
      class="min-w-0 rounded-lg border-[0.5px] border-border/70 bg-card"
      aria-labelledby="salary-ranking-title"
      :aria-busy="rankingState === 'loading'"
    >
      <div
        class="flex flex-wrap items-center justify-between gap-4 border-b-[0.5px] border-border/70 p-5"
      >
        <div class="space-y-1.5">
          <h2 id="salary-ranking-title" class="text-sm font-semibold">全量高薪职位排行</h2>
          <p class="text-xs text-muted-foreground">
            不受上方筛选影响；职位至少 10 条样本，城市至少 5 条样本。
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <UiRadioGroup
            v-model="rankingLimit"
            aria-label="排行数量"
            class="flex gap-3"
            :disabled="rankingState === 'loading'"
            ><label
              v-for="count in [5, 10, 20]"
              :key="count"
              class="flex items-center gap-1.5 text-xs"
              ><UiRadioGroupItem :value="String(count)" />前 {{ count }}</label
            ></UiRadioGroup
          ><UiButton
            variant="ghost"
            size="sm"
            :disabled="rankingState === 'loading'"
            @click="loadRanking"
            ><HugeiconsIcon
              :icon="RefreshIcon"
              class="size-4"
              aria-hidden="true"
            />刷新排行</UiButton
          >
        </div>
      </div>
      <div v-if="rankingState === 'loading'" role="status" class="space-y-3 p-5">
        <p class="text-sm text-muted-foreground">正在读取薪资排行…</p>
        <UiSkeleton class="h-32 w-full" />
      </div>
      <div
        v-else-if="rankingState === 'error' || rankingState === 'forbidden'"
        role="alert"
        class="space-y-3 px-6 py-10 text-center"
      >
        <h3 class="text-sm font-medium">
          {{ rankingState === 'forbidden' ? '当前账号无权读取薪资排行' : '薪资排行读取失败' }}
        </h3>
        <p class="text-sm text-muted-foreground">请检查访问权限和服务连接后重试。</p>
        <UiButton variant="outline" @click="loadRanking">重试薪资排行</UiButton>
      </div>
      <div v-else-if="!ranking.length" role="status" class="space-y-2 px-6 py-10 text-center">
        <h3 class="text-sm font-medium">暂无满足样本量要求的职位排行</h3>
        <p class="text-sm text-muted-foreground">排行仅统计可解析的 K 薪资样本，可稍后刷新。</p>
        <UiButton variant="outline" @click="loadRanking">重新读取排行</UiButton>
      </div>
      <UiTable v-else
        ><UiTableCaption class="sr-only">按样本平均薪资排序的高薪职位，单位 K</UiTableCaption
        ><UiTableHeader
          ><UiTableRow class="border-b-[0.5px]"
            ><UiTableHead class="w-14 px-5 text-right">排名</UiTableHead
            ><UiTableHead>职位</UiTableHead
            ><UiTableHead class="hidden text-right sm:table-cell">样本数</UiTableHead
            ><UiTableHead class="text-right">平均薪资（K）</UiTableHead
            ><UiTableHead class="hidden text-right lg:table-cell">薪资范围（K）</UiTableHead
            ><UiTableHead class="hidden md:table-cell">高薪样本城市</UiTableHead></UiTableRow
          ></UiTableHeader
        ><UiTableBody
          ><UiTableRow
            v-for="(row, index) in ranking"
            :key="`${row.position}:${index}`"
            class="border-b-[0.5px]"
            ><UiTableCell class="px-5 text-right tabular-nums text-muted-foreground">{{
              index + 1
            }}</UiTableCell
            ><UiTableCell class="whitespace-normal break-words font-medium">{{
              row.position
            }}</UiTableCell
            ><UiTableCell class="hidden text-right tabular-nums sm:table-cell">{{
              number.format(row.count)
            }}</UiTableCell
            ><UiTableCell class="text-right tabular-nums">{{
              number.format(row.average)
            }}</UiTableCell
            ><UiTableCell class="hidden text-right tabular-nums lg:table-cell"
              >{{ row.minimum }}–{{ row.maximum }}</UiTableCell
            ><UiTableCell class="hidden md:table-cell"
              >{{ row.city || '—' }}
              <p v-if="row.city" class="mt-1 text-xs tabular-nums text-muted-foreground">
                {{ row.citySalary == null ? '—' : number.format(row.citySalary) + 'K' }} ·
                {{ row.cityCount ?? '—' }} 条样本
              </p></UiTableCell
            ></UiTableRow
          ></UiTableBody
        ></UiTable
      >
    </section>
    <RecruitmentJobSheet :job="selected" @close="selected = null" />
  </section>
</template>
