<script setup lang="ts">
import {
  RiArrowDownSLine,
  RiArrowLeftSLine,
  RiArrowRightSLine,
  RiBriefcaseLine,
  RiBuilding2Line,
  RiCoinsLine,
  RiEqualizerLine,
  RiErrorWarningLine,
  RiLineChartLine,
  RiRefreshLine,
  RiTrophyLine,
} from '@remixicon/vue'
import { useMediaQuery } from '@vueuse/core'
import { computed, nextTick, onMounted, reactive, ref, shallowRef, useTemplateRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import JobSheet from '@/components/recruitment/JobSheet.vue'
import OptionPicker from '@/components/recruitment/OptionPicker.vue'
import ScatterPlot from '@/components/recruitment/ScatterPlot.vue'
import {
  getCityNameList,
  getPositionNameList,
  getSalaryHotJob,
  getTwoDimensionalAnalysisChart,
} from '@/api/boss/user/user'
import {
  analysisAxes,
  analysisQuery,
  analysisStatusLabels,
  analysisStatusTones,
  defaultAnalysisFilter,
  defaultSampleSize,
  formatSampleSalary,
  jobSamples,
  jobSummary,
  optionNames,
  readRecruitmentQuery,
  recruitmentQueryCity,
  safeCity,
  salaryRanking,
  samplePageSizes,
  sameAnalysisFilter,
} from '@/lib/recruitment'
import type { JobSample, AnalysisFilter } from '@/lib/recruitment'
import { settingsFailure } from '@/lib/settings'

type State = 'idle' | 'loading' | 'ready' | 'error' | 'forbidden'
const route = useRoute()
const router = useRouter()
const mobile = useMediaQuery('(width < 640px)')
const recruitmentPath = route.path
const initialQuery = route.query
const restoring = ref(['city', 'position', 'x', 'y', 'page', 'size'].some((key) => key in initialQuery))
const mobileFiltersOpen = ref(!restoring.value)
const draft = reactive<AnalysisFilter>({ ...defaultAnalysisFilter })
type AnalysisRequest = { filter: AnalysisFilter; args: ReturnType<typeof analysisQuery> }
const appliedRequest = shallowRef<AnalysisRequest | null>(null)
const applied = computed(() => appliedRequest.value?.filter ?? null)
const cities = shallowRef<string[]>([])
const positions = shallowRef<string[]>([])
const cityState = ref<State>('idle')
const positionState = ref<State>('idle')
const jobs = shallowRef<JobSample[]>([])
const jobsState = ref<State>('idle')
const ranking = shallowRef<ReturnType<typeof salaryRanking>>([])
const rankingState = ref<State>('idle')
const sourcesUnavailable = computed(() =>
  [cityState.value, positionState.value, rankingState.value].every((state) => state === 'error'),
)
const rankingLimit = ref('10')
const selected = shallowRef<JobSample | null>(null)
const formError = ref('')
const formFeedback = useTemplateRef<HTMLElement>('formFeedback')
const cityFeedback = useTemplateRef<HTMLElement>('cityFeedback')
const positionFeedback = useTemplateRef<HTMLElement>('positionFeedback')
const analysisTitle = useTemplateRef<HTMLElement>('analysisTitle')
const rankingTitle = useTemplateRef<HTMLElement>('rankingTitle')
const queryPending = computed(() => jobsState.value === 'loading')
const cityPending = computed(() => cityState.value === 'idle' || cityState.value === 'loading')
const positionPending = computed(() => positionState.value === 'idle' || positionState.value === 'loading')
const rankingPending = computed(() => rankingState.value === 'idle' || rankingState.value === 'loading')
const draftPending = computed(() => queryPending.value || restoring.value || positionPending.value)
const queryFailed = computed(() => jobsState.value === 'error' || jobsState.value === 'forbidden')
let sheetTrigger: HTMLElement | null = null
const page = ref(1)
const size = ref<number>(defaultSampleSize)
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
  jobs.value.slice((page.value - 1) * size.value, page.value * size.value),
)
const summary = computed(() => (jobsState.value === 'ready' ? jobSummary(jobs.value) : null))
const changed = computed(
  () => applied.value !== null && !sameAnalysisFilter(draft, applied.value),
)
const number = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 2 })
const axisFields = [
  { key: 'x', other: 'y', label: '横轴 X' },
  { key: 'y', other: 'x', label: '纵轴 Y' },
] as const

const axisName = (value: string) => analysisAxes.find((axis) => axis.value === value)?.label
const conditionScope = ({ city, position }: AnalysisFilter) =>
  `${city === 'all' ? '全部城市' : city} · ${position === 'all' ? '全部职位' : position}`
const axesScope = ({ x, y }: AnalysisFilter) => `${axisName(x)} × ${axisName(y)}`
const metrics = computed(() => {
  const data = summary.value
  return [
    {
      label: '返回样本总数',
      value: data?.count,
      note: '二维查询返回的全部样本',
      icon: RiBriefcaseLine,
    },
    {
      label: '去重公司数',
      value: data?.companies,
      note: '按非空公司名称去重',
      icon: RiBuilding2Line,
    },
    {
      label: '有效薪资均值',
      value: data?.average,
      unit: 'K',
      note: data && `${number.format(data.salaryCount)} / ${number.format(data.count)} 条有效薪资`,
      icon: RiCoinsLine,
    },
  ]
})

async function loadCities() {
  if (cityState.value === 'loading') return
  cityState.value = 'loading'
  cities.value = []
  try {
    cities.value = optionNames(await getCityNameList())
    cityState.value = 'ready'
  } catch (error) {
    cityState.value = settingsFailure(error)
  }
}

function retryCities() {
  if (cityPending.value || restoring.value) return
  cityFeedback.value?.focus()
  void loadCities()
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
function changeDraftCity(city: string) {
  if (city === draft.city) return
  draft.city = city
  draft.position = defaultAnalysisFilter.position
  void loadPositions()
}

function retryPositions() {
  if (positionPending.value || restoring.value) return
  positionFeedback.value?.focus()
  void loadPositions()
}

function changeSize(value: unknown) {
  const nextSize = samplePageSizes.find((option) => option === value)
  if (nextSize === undefined || nextSize === size.value) return
  size.value = nextSize
  page.value = 1
}

watch([applied, page, size], ([filter, currentPage, currentSize]) => {
  const current = router.currentRoute.value
  if (!filter || current.path !== recruitmentPath) return
  const params = { ...filter, page: String(currentPage), size: String(currentSize) }
  void router.replace({ query: { ...current.query, ...params }, hash: current.hash })
})
watch(rankingLimit, () => {
  void loadRanking()
})
async function loadRanking() {
  if (rankingState.value === 'loading') return
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
function retryRanking() {
  if (rankingPending.value) return
  rankingTitle.value?.focus()
  void loadRanking()
}

function goToRanking() {
  rankingTitle.value?.focus({ preventScroll: true })
  rankingTitle.value?.scrollIntoView({ block: 'start' })
}

async function submitDraft(fromRecovery = false) {
  if (draftPending.value) return
  const filter = { ...draft }
  let args: ReturnType<typeof analysisQuery>
  try {
    args = analysisQuery(filter, cities.value, positions.value)
  } catch {
    formError.value = '请选择有效的城市、职位与不同的分析维度。'
    await nextTick()
    formFeedback.value?.focus()
    return
  }
  if (fromRecovery) analysisTitle.value?.focus()
  await runAnalysis({ filter, args })
}

async function runAnalysis(request: AnalysisRequest, initialPage = 1) {
  if (queryPending.value) return
  formError.value = ''
  jobsState.value = 'loading'
  jobs.value = []
  page.value = initialPage
  selected.value = null
  appliedRequest.value = request
  try {
    jobs.value = jobSamples(await getTwoDimensionalAnalysisChart(...request.args))
    page.value = Math.min(page.value, Math.max(1, Math.ceil(jobs.value.length / size.value)))
    jobsState.value = 'ready'
  } catch (error) {
    jobsState.value = settingsFailure(error)
  }
}

function retryAnalysis() {
  if (queryPending.value || !appliedRequest.value) return
  analysisTitle.value?.focus()
  void runAnalysis(appliedRequest.value)
}

function showJob(job: JobSample, event: MouseEvent) {
  sheetTrigger = event.currentTarget as HTMLElement
  selected.value = job
}

function restoreSheetFocus(event: Event) {
  if (sheetTrigger?.isConnected) {
    event.preventDefault()
    sheetTrigger.focus()
  }
  sheetTrigger = null
}

function resetDraft() {
  if (queryPending.value || restoring.value) return
  changeDraftCity(defaultAnalysisFilter.city)
  Object.assign(draft, defaultAnalysisFilter)
  formError.value = ''
}

function resetAndRun() {
  if (draftPending.value) return
  resetDraft()
  analysisTitle.value?.focus()
  const filter = { ...draft }
  void runAnalysis({ filter, args: analysisQuery(filter, cities.value, positions.value) })
}

onMounted(async () => {
  void loadRanking()
  const loadingCities = loadCities()
  const { city, position } = initialQuery
  const needsCity = typeof city === 'string' && city !== 'all' && safeCity(city)
  const needsPosition = typeof position === 'string' && position !== 'all' && position.trim() !== ''
  if (needsCity) {
    await loadingCities
    draft.city = recruitmentQueryCity(city, cities.value) ?? defaultAnalysisFilter.city
  }
  await loadPositions()
  if (!restoring.value) return
  restoring.value = false
  if ((needsCity && cityState.value !== 'ready') || (needsPosition && positionState.value !== 'ready')) {
    formError.value = '暂无法验证链接中的城市或职位。选项恢复后，可重新选择并开始分析。'
    return
  }
  const saved = readRecruitmentQuery(initialQuery, cities.value, positions.value)
  if (!saved) return
  Object.assign(draft, saved.filter)
  size.value = saved.size
  const args = analysisQuery(saved.filter, cities.value, positions.value)
  void runAnalysis({ filter: saved.filter, args }, saved.page)
})
</script>

<template>
  <section class="@container flex min-w-0 flex-col gap-3" aria-labelledby="recruitment-title">
    <header class="flex flex-wrap items-end justify-between gap-2">
      <div class="min-w-0 space-y-1">
        <h1 id="recruitment-title" class="text-2xl font-bold tracking-tight sm:text-3xl">招聘数据分析</h1>
        <p class="text-sm text-muted-foreground">比较职位样本的薪资、学历与经验分布，查看独立的全局高薪排行。</p>
      </div>
      <a
        href="#salary-ranking-title"
        class="sr-only text-xs underline-offset-4 hover:underline focus:not-sr-only"
        @click.prevent="goToRanking"
      >跳至全局高薪职位排行</a>
    </header>

    <UiAlert v-if="sourcesUnavailable" variant="destructive" role="alert">
      <UiAlertTitle>招聘数据暂时无法读取</UiAlertTitle>
      <UiAlertDescription>城市、职位和排行均未读取成功。请稍后分别重试，或检查服务连接。</UiAlertDescription>
    </UiAlert>

    <form
      class="min-w-0 space-y-3 rounded-lg border bg-card p-4"
      aria-label="招聘分析筛选"
      @submit.prevent="submitDraft()"
    >
      <UiCollapsible :open="!mobile || mobileFiltersOpen" @update:open="mobileFiltersOpen = $event">
        <UiCollapsibleTrigger v-if="mobile" as-child>
          <UiButton type="button" variant="ghost" class="h-auto w-full justify-start px-0">
            <RiEqualizerLine aria-hidden="true" />
            筛选与分析
            <RiArrowDownSLine class="ml-auto" aria-hidden="true" />
          </UiButton>
        </UiCollapsibleTrigger>
        <p v-if="mobile" class="my-2 [overflow-wrap:anywhere] text-xs text-muted-foreground">
          草稿 {{ conditionScope(draft) }} · {{ axesScope(draft) }}
        </p>
        <UiCollapsibleContent>
          <div class="grid grid-cols-2 items-start gap-3 @2xl:grid-cols-4 @5xl:grid-cols-[1fr_1.18fr_.76fr_.76fr_auto_auto]">
            <UiField class="min-w-0 gap-1.5">
              <UiFieldLabel for="recruit-city">城市</UiFieldLabel>
              <OptionPicker
                id="recruit-city"
                :model-value="draft.city"
                :options="cityOptions"
                :disabled="cityPending || queryPending || restoring"
                described-by="recruit-city-status"
                @update:model-value="changeDraftCity"
              />
              <div id="recruit-city-status" ref="cityFeedback" tabindex="-1" class="empty:sr-only">
                <p v-if="cityPending || (cityState === 'ready' && !cities.length)" role="status" class="text-xs text-muted-foreground">
                  {{ cityPending ? '正在加载城市选项…' : '暂无城市选项，可使用全部城市。' }}
                </p>
                <p v-else-if="cityState === 'error' || cityState === 'forbidden'" role="alert" class="text-xs text-destructive">
                  {{ cityState === 'forbidden' ? '无权读取城市选项。' : '城市选项读取失败。' }}
                </p>
              </div>
              <UiButton
                v-if="cityState === 'error' || cityState === 'forbidden'"
                type="button" variant="outline" size="sm" class="self-start"
                :aria-disabled="cityPending || restoring"
                @click="retryCities"
              >重试城市选项</UiButton>
            </UiField>

            <UiField class="min-w-0 gap-1.5">
              <UiFieldLabel for="recruit-position">职位</UiFieldLabel>
              <OptionPicker
                id="recruit-position"
                v-model="draft.position"
                :options="positionOptions"
                :disabled="positionPending || queryPending || restoring"
                described-by="recruit-position-status"
              />
              <div id="recruit-position-status" ref="positionFeedback" tabindex="-1" class="empty:sr-only">
                <p v-if="positionPending || (positionState === 'ready' && !positions.length)" role="status" class="text-xs text-muted-foreground">
                  {{ positionPending ? '正在加载职位选项…' : '当前范围暂无职位选项，可使用全部职位。' }}
                </p>
                <p v-else-if="positionState === 'error' || positionState === 'forbidden'" role="alert" class="text-xs text-destructive">
                  {{ positionState === 'forbidden' ? '无权读取职位选项。' : '职位选项读取失败，可重试或切换城市。' }}
                </p>
              </div>
              <UiButton
                v-if="positionState === 'error' || positionState === 'forbidden'"
                type="button" variant="outline" size="sm" class="self-start"
                :aria-disabled="positionPending || restoring"
                @click="retryPositions"
              >重试职位选项</UiButton>
            </UiField>

            <UiField v-for="axis in axisFields" :key="axis.key" class="min-w-0 gap-1.5">
              <UiFieldLabel :for="`recruit-axis-${axis.key}`">{{ axis.label }}</UiFieldLabel>
              <div class="relative min-w-0">
                <select
                  :id="`recruit-axis-${axis.key}`"
                  v-model="draft[axis.key]"
                  :disabled="queryPending || restoring"
                  aria-describedby="recruit-axis-help"
                  class="h-9 w-full appearance-none truncate rounded-md border border-input bg-background py-1.5 pr-7 pl-2.5 text-sm text-foreground outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50"
                >
                  <option
                    v-for="option in analysisAxes"
                    :key="option.value"
                    :value="option.value"
                    :disabled="option.value === draft[axis.other]"
                    class="bg-popover text-popover-foreground"
                  >{{ option.label }}</option>
                </select>
                <RiArrowDownSLine class="pointer-events-none absolute top-2.5 right-2 size-4 text-muted-foreground" aria-hidden="true" />
              </div>
            </UiField>

            <div class="col-span-2 flex flex-wrap items-center gap-2 @5xl:col-span-1 @5xl:pt-[22px]">
              <UiButton type="submit" :aria-disabled="draftPending" class="aria-disabled:opacity-50">
                <RiEqualizerLine aria-hidden="true" />
                {{ queryPending ? '正在查询…' : '开始分析' }}
              </UiButton>
              <UiButton
                type="button" variant="outline" :aria-disabled="queryPending || restoring"
                class="aria-disabled:opacity-50" @click="resetDraft"
              >重置</UiButton>
            </div>
            <p id="recruit-axis-help" class="col-span-2 text-xs leading-5 text-muted-foreground @5xl:col-span-1 @5xl:pt-[22px]">
              两轴须不同<span class="hidden @5xl:block">另可选经验折算值</span>
            </p>
          </div>
        </UiCollapsibleContent>
      </UiCollapsible>

      <p v-if="formError" ref="formFeedback" tabindex="-1" role="alert" class="text-sm text-destructive">
        {{ formError }}
      </p>
      <div
        v-if="changed && applied" role="status"
        class="flex items-start gap-2 rounded-md border border-status-warning-foreground/25 bg-status-warning-surface px-2.5 py-2 text-xs leading-5 text-status-warning-foreground"
      >
        <RiErrorWarningLine class="mt-0.5 size-4 shrink-0" aria-hidden="true" />
        <p class="min-w-0 break-words">
          <strong>草稿尚未应用。</strong>
          {{ queryFailed ? '上次条件' : '当前结果' }} {{ conditionScope(applied) }} · {{ axesScope(applied) }}。
          草稿 {{ conditionScope(draft) }} · {{ axesScope(draft) }}。
          {{ queryFailed ? '可重试上次条件，或按当前草稿查询。' : '点击「开始分析」刷新结果。' }}
        </p>
      </div>
    </form>

    <div v-if="queryPending || summary" class="grid grid-cols-3 gap-2 sm:gap-3" aria-label="已应用条件的核心指标">
      <UiCard
        v-for="metric in metrics" :key="metric.label"
        class="min-w-0 gap-0 rounded-lg border py-0 shadow-none ring-0"
      >
        <UiCardContent class="relative min-h-[74px] space-y-1 p-2.5 sm:px-3.5">
          <h2 class="text-xs font-medium text-muted-foreground">{{ metric.label }}</h2>
          <component :is="metric.icon" class="absolute top-3 right-3.5 hidden size-4 text-accent-data-foreground @3xl:block" aria-hidden="true" />
          <UiSkeleton v-if="queryPending" class="h-7 w-2/3" />
          <div v-else class="flex flex-wrap items-baseline gap-x-4 gap-y-1">
            <p class="break-all font-mono text-xl font-semibold tabular-nums sm:text-2xl">
              {{ metric.value == null ? '—' : number.format(metric.value) }}<span v-if="metric.value != null">{{ metric.unit }}</span>
            </p>
            <p class="min-w-0 text-xs leading-4 text-muted-foreground" :class="{ 'hidden @2xl:block': !metric.unit }">{{ metric.note }}</p>
          </div>
        </UiCardContent>
      </UiCard>
    </div>

    <section class="min-w-0 rounded-lg border bg-card" :aria-busy="queryPending" aria-labelledby="sample-analysis-title">
      <header class="px-4 pt-3">
        <h2 id="sample-analysis-title" ref="analysisTitle" tabindex="-1" class="flex items-center gap-2 text-base font-semibold">
          <RiLineChartLine class="size-4 shrink-0 text-accent-data-foreground" aria-hidden="true" />
          {{ applied ? `${axesScope(applied)}分布` : '职位样本分析' }}
        </h2>
        <p v-if="applied" class="mt-1 break-words text-xs text-muted-foreground">
          {{ queryFailed ? '上次查询条件' : '已应用' }} {{ conditionScope(applied) }}
        </p>
      </header>
      <div v-if="jobsState === 'idle'" role="status" class="space-y-2 px-4 py-8 text-center">
        <h3 class="text-sm font-medium">选择条件，开始查询</h3>
        <p class="text-sm text-muted-foreground">在上方选择城市、职位和分析维度，点击「开始分析」查看样本。</p>
      </div>
      <div v-else-if="queryPending" role="status" class="space-y-3 p-4">
        <p class="text-sm text-muted-foreground">正在读取并分析职位样本…</p>
        <UiSkeleton class="h-44 w-full" />
      </div>
      <div v-else-if="queryFailed" class="space-y-3 p-4">
        <UiAlert variant="destructive" role="alert">
          <UiAlertTitle>{{ jobsState === 'forbidden' ? '当前账号无权读取招聘分析' : '招聘分析读取失败' }}</UiAlertTitle>
          <UiAlertDescription>{{
            jobsState === 'forbidden'
              ? '请联系管理员确认访问权限。'
              : '可重试上次条件，或用草稿发起新查询。'
          }}</UiAlertDescription>
        </UiAlert>
        <div class="flex flex-wrap gap-2">
          <UiButton variant="outline" :aria-disabled="queryPending" @click="retryAnalysis">重试上次条件</UiButton>
          <UiButton
            v-if="changed" variant="outline" :aria-disabled="draftPending" class="aria-disabled:opacity-50"
            @click="submitDraft(true)"
          >按当前草稿查询</UiButton>
        </div>
      </div>
      <div v-else-if="!jobs.length" role="status" class="space-y-2 px-4 py-8 text-center">
        <h3 class="text-sm font-medium">暂无匹配的职位样本</h3>
        <p class="text-sm text-muted-foreground">当前条件没有返回所选维度均有效的样本，可调整城市、职位或分析维度。</p>
        <UiButton variant="outline" :aria-disabled="draftPending" class="aria-disabled:opacity-50" @click="resetAndRun">
          重置并重新分析
        </UiButton>
      </div>
      <ScatterPlot v-else-if="applied" :jobs="jobs" :x="applied.x" :y="applied.y" />
    </section>

    <details v-if="summary && jobs.length && applied" :open="!mobile" class="min-w-0 rounded-lg border bg-card">
      <summary class="cursor-pointer px-4 py-3 text-sm font-semibold sm:hidden">
        职位样本明细 · {{ number.format(jobs.length) }} 条
      </summary>
      <section class="space-y-2.5 p-4" aria-labelledby="job-samples-title">
        <header>
          <h2 id="job-samples-title" class="text-base font-semibold">职位样本明细</h2>
          <p class="mt-1 break-words text-xs text-muted-foreground">
            已应用 {{ conditionScope(applied) }} · 同名及相同分类码可重复
          </p>
        </header>
        <div
          :key="`${page}:${size}`"
          class="max-h-50 overflow-auto [&_[data-slot=table-container]]:overflow-visible"
          role="region" aria-label="当前页职位样本" tabindex="0"
        >
          <UiTable class="text-xs">
            <UiTableCaption class="sr-only">招聘职位样本及薪资、学历和经验要求</UiTableCaption>
            <UiTableHeader class="sticky top-0 z-10 bg-muted">
              <UiTableRow>
                <UiTableHead class="h-7">职位名称</UiTableHead>
                <UiTableHead class="hidden h-7 @3xl:table-cell">公司名称</UiTableHead>
                <UiTableHead class="hidden h-7 @xl:table-cell">城市</UiTableHead>
                <UiTableHead class="h-7">薪资 / 折算</UiTableHead>
                <UiTableHead class="hidden h-7 @2xl:table-cell">学历</UiTableHead>
                <UiTableHead class="hidden h-7 @3xl:table-cell">经验</UiTableHead>
                <UiTableHead class="hidden h-7 @2xl:table-cell">AI 分析来源</UiTableHead>
                <UiTableHead class="h-7 text-center">操作</UiTableHead>
              </UiTableRow>
            </UiTableHeader>
            <UiTableBody>
              <UiTableRow v-for="job in visible" :key="job.key">
                <UiTableCell class="max-w-48 py-1 whitespace-normal">
                  <UiButton
                    variant="link"
                    class="h-auto justify-start py-0 px-0 text-start text-xs font-medium whitespace-normal [overflow-wrap:anywhere] text-accent-data-foreground underline underline-offset-2"
                    @click="showJob(job, $event)"
                  >{{ job.positionName || '未提供职位名称' }}</UiButton>
                  <p class="mt-1 [overflow-wrap:anywhere] text-muted-foreground @3xl:hidden">{{ job.companyName || '未提供公司名称' }}</p>
                  <p class="mt-1 [overflow-wrap:anywhere] text-muted-foreground @xl:hidden">{{ job.cityName || '—' }}</p>
                  <StatusBadge :tone="analysisStatusTones[job.analysisStatus]" class="mt-1 max-w-full whitespace-normal @2xl:hidden">
                    {{ analysisStatusLabels[job.analysisStatus] }}
                  </StatusBadge>
                </UiTableCell>
                <UiTableCell class="hidden max-w-32 py-1 whitespace-normal [overflow-wrap:anywhere] @3xl:table-cell">{{ job.companyName || '—' }}</UiTableCell>
                <UiTableCell class="hidden max-w-20 py-1 whitespace-normal [overflow-wrap:anywhere] @xl:table-cell">{{ job.cityName || '—' }}</UiTableCell>
                <UiTableCell class="max-w-28 py-1 whitespace-normal [overflow-wrap:anywhere] font-mono tabular-nums">
                  {{ formatSampleSalary(job.salary) }}
                  <p v-if="job.salaryValue != null" class="text-[11px] text-muted-foreground">{{ number.format(job.salaryValue) }} K</p>
                </UiTableCell>
                <UiTableCell class="hidden py-1 @2xl:table-cell">{{ job.degree || '—' }}</UiTableCell>
                <UiTableCell class="hidden py-1 @3xl:table-cell">{{ job.experience || '—' }}</UiTableCell>
                <UiTableCell class="hidden py-1 @2xl:table-cell">
                  <StatusBadge :tone="analysisStatusTones[job.analysisStatus]">{{ analysisStatusLabels[job.analysisStatus] }}</StatusBadge>
                </UiTableCell>
                <UiTableCell class="py-1 text-center">
                  <UiButton
                    variant="outline" size="sm" class="h-6 px-2 text-xs"
                    :aria-label="`查看${job.positionName || '职位'}详情`"
                    @click="showJob(job, $event)"
                  >详情</UiButton>
                </UiTableCell>
              </UiTableRow>
            </UiTableBody>
          </UiTable>
        </div>

        <div class="flex flex-wrap items-center justify-between gap-2">
          <span class="font-mono text-xs tabular-nums text-muted-foreground" aria-live="polite">
            {{ number.format((page - 1) * size + 1) }}–{{ number.format(Math.min(page * size, jobs.length)) }} / {{ number.format(jobs.length) }} 条
          </span>
          <div class="flex min-w-0 flex-wrap items-center gap-3">
            <UiRadioGroup
              :model-value="size" aria-label="样本每页条数"
              class="inline-flex w-auto gap-0.5 rounded-md border bg-muted p-0.5"
              @update:model-value="changeSize"
            >
              <label
                v-for="count in samplePageSizes" :key="count"
                class="cursor-pointer rounded-sm px-2.5 py-1 font-mono text-xs tabular-nums has-[[data-state=checked]]:bg-background has-focus-visible:ring-2 has-focus-visible:ring-ring"
              ><UiRadioGroupItem :value="count" :aria-label="String(count)" class="sr-only" />{{ count }}</label>
            </UiRadioGroup>
            <UiPagination
              v-model:page="page" :items-per-page="size" :total="jobs.length" :sibling-count="1"
              class="w-auto max-w-full" aria-label="样本分页"
            >
              <UiPaginationContent v-slot="{ items }" class="flex-wrap">
                <UiPaginationPrevious size="icon-sm" aria-label="上一页"><RiArrowLeftSLine aria-hidden="true" /></UiPaginationPrevious>
                <template v-for="(item, index) in items" :key="index">
                  <UiPaginationItem
                    v-if="item.type === 'page'" :value="item.value" :is-active="item.value === page"
                    :aria-label="`第 ${item.value} 页`" :aria-current="item.value === page ? 'page' : undefined" size="icon-sm"
                  >{{ item.value }}</UiPaginationItem>
                  <UiPaginationEllipsis v-else />
                </template>
                <UiPaginationNext size="icon-sm" aria-label="下一页"><RiArrowRightSLine aria-hidden="true" /></UiPaginationNext>
              </UiPaginationContent>
            </UiPagination>
          </div>
        </div>
      </section>
    </details>

    <section
      class="min-w-0 space-y-2.5 rounded-lg border bg-card p-4"
      aria-labelledby="salary-ranking-title" :aria-busy="rankingPending"
    >
      <header class="flex flex-wrap items-start justify-between gap-2">
        <div class="space-y-1">
          <h2 id="salary-ranking-title" ref="rankingTitle" tabindex="-1" class="flex scroll-mt-20 items-center gap-2 text-base font-semibold">
            <RiTrophyLine class="size-4 shrink-0 text-accent-data-foreground" aria-hidden="true" />
            全局高薪职位排行
          </h2>
          <p class="text-xs leading-5 text-muted-foreground">
            全局口径，独立于上方筛选。职位至少 10 条样本入榜，推荐城市至少 5 条。
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <UiRadioGroup
            v-model="rankingLimit" aria-label="排行数量" :disabled="rankingPending"
            class="inline-flex w-auto gap-0.5 rounded-md border bg-muted p-0.5"
          >
            <label
              v-for="count in [5, 10, 20]" :key="count"
              class="cursor-pointer rounded-sm px-2.5 py-1 font-mono text-xs has-[[data-state=checked]]:bg-background has-focus-visible:ring-2 has-focus-visible:ring-ring has-disabled:cursor-default"
            ><UiRadioGroupItem :value="String(count)" :aria-label="`Top ${count}`" class="sr-only" />Top {{ count }}</label>
          </UiRadioGroup>
          <UiButton variant="outline" size="sm" :aria-disabled="rankingPending" class="aria-disabled:opacity-50" @click="loadRanking">
            <RiRefreshLine :class="{ 'motion-safe:animate-spin': rankingPending }" aria-hidden="true" />
            {{ rankingPending ? '正在刷新…' : '刷新榜单' }}
          </UiButton>
        </div>
      </header>
      <div v-if="rankingPending" role="status" class="space-y-2">
        <p class="text-sm text-muted-foreground">正在读取薪资排行…</p>
        <UiSkeleton class="h-32 w-full" />
      </div>
      <div v-else-if="rankingState === 'error' || rankingState === 'forbidden'" class="space-y-2">
        <UiAlert variant="destructive" role="alert">
          <UiAlertTitle>{{ rankingState === 'forbidden' ? '当前账号无权读取薪资排行' : '薪资排行读取失败' }}</UiAlertTitle>
          <UiAlertDescription>请检查访问权限和服务连接后重试。</UiAlertDescription>
        </UiAlert>
        <UiButton variant="outline" :aria-disabled="rankingPending" @click="retryRanking">重试薪资排行</UiButton>
      </div>
      <div v-else-if="!ranking.length" role="status" class="space-y-2 px-4 py-6 text-center">
        <h3 class="text-sm font-medium">暂无满足样本量要求的职位排行</h3>
        <p class="text-sm text-muted-foreground">排行仅统计可解析的 K 薪资样本，可稍后刷新。</p>
        <UiButton variant="outline" :aria-disabled="rankingPending" @click="retryRanking">重新读取排行</UiButton>
      </div>
      <div
        v-else role="region" aria-label="排行结果" tabindex="0"
        class="max-h-50 overflow-auto [&_[data-slot=table-container]]:overflow-visible"
      >
        <UiTable class="text-xs">
          <UiTableCaption class="sr-only">按样本平均薪资排序的高薪职位，单位 K</UiTableCaption>
          <UiTableHeader class="sticky top-0 z-10 bg-muted">
            <UiTableRow>
              <UiTableHead class="h-7 w-14">排名</UiTableHead>
              <UiTableHead class="h-7">职位</UiTableHead>
              <UiTableHead class="hidden h-7 @2xl:table-cell">样本数</UiTableHead>
              <UiTableHead class="h-7">均薪</UiTableHead>
              <UiTableHead class="hidden h-7 @2xl:table-cell">最低–最高</UiTableHead>
              <UiTableHead class="hidden h-7 @2xl:table-cell">推荐城市 / 城市均薪 / 样本</UiTableHead>
            </UiTableRow>
          </UiTableHeader>
          <UiTableBody>
            <UiTableRow v-for="(row, index) in ranking" :key="`${row.position}:${index}`">
              <UiTableCell class="py-1 font-mono tabular-nums text-muted-foreground">{{ String(index + 1).padStart(2, '0') }}</UiTableCell>
              <UiTableCell class="py-1 font-medium whitespace-normal [overflow-wrap:anywhere]">
                {{ row.position }}
                <p class="mt-0.5 text-muted-foreground @2xl:hidden">
                  {{ number.format(row.count) }} 条 · {{ number.format(row.minimum) }}–{{ number.format(row.maximum) }} K
                </p>
                <p class="mt-0.5 text-muted-foreground @2xl:hidden">
                  推荐城市 {{ row.city || '—' }}
                  <template v-if="row.city">
                    · {{ row.citySalary == null ? '—' : number.format(row.citySalary) + ' K' }} · {{ row.cityCount ?? '—' }} 条
                  </template>
                </p>
              </UiTableCell>
              <UiTableCell class="hidden py-1 font-mono tabular-nums @2xl:table-cell">{{ number.format(row.count) }}</UiTableCell>
              <UiTableCell class="py-1 font-mono font-semibold tabular-nums text-accent-data-foreground">{{ number.format(row.average) }} K</UiTableCell>
              <UiTableCell class="hidden py-1 font-mono tabular-nums @2xl:table-cell">{{ number.format(row.minimum) }}–{{ number.format(row.maximum) }} K</UiTableCell>
              <UiTableCell class="hidden py-1 whitespace-normal [overflow-wrap:anywhere] @2xl:table-cell">
                {{ row.city || '—' }}
                <span v-if="row.city" class="ml-2 font-mono tabular-nums text-muted-foreground">
                  · {{ row.citySalary == null ? '—' : number.format(row.citySalary) + ' K' }} · {{ row.cityCount ?? '—' }} 条
                </span>
              </UiTableCell>
            </UiTableRow>
          </UiTableBody>
        </UiTable>
      </div>
    </section>

    <JobSheet :job="selected" @close="selected = null" @close-auto-focus="restoreSheetFocus" />
  </section>
</template>
