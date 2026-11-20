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

definePageMeta({ layout: 'console', middleware: 'signed-in' })
type State = 'idle' | 'loading' | 'ready' | 'error' | 'forbidden'
const route = useRoute()
const router = useRouter()
const recruitmentPath = route.path
const initialQuery = route.query
const restoring = ref(['city', 'position', 'x', 'y', 'page', 'size'].some((key) => key in initialQuery))
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
const scope = computed(() => {
  const filter = applied.value
  if (!filter) return ''
  const city = filter.city === 'all' ? '全部城市' : filter.city
  const position = filter.position === 'all' ? '全部职位' : filter.position
  const axisLabel = (value: string) => analysisAxes.find((axis) => axis.value === value)?.label
  return `${city} / ${position} · X ${axisLabel(filter.x)} / Y ${axisLabel(filter.y)}`
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
  if (Object.entries(params).every(([key, value]) => current.query[key] === value)) return
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
  const city = initialQuery.city
  if (typeof city === 'string' && city !== 'all' && safeCity(city)) {
    await loadingCities
    draft.city = recruitmentQueryCity(city, cities.value) ?? defaultAnalysisFilter.city
  }
  await loadPositions()
  if (!restoring.value) return
  const saved = readRecruitmentQuery(initialQuery, cities.value, positions.value)
  restoring.value = false
  if (!saved) return
  Object.assign(draft, saved.filter)
  size.value = saved.size
  const args = analysisQuery(saved.filter, cities.value, positions.value)
  void runAnalysis({ filter: saved.filter, args }, saved.page)
})
</script>

<template>
  <section class="flex min-w-0 flex-col gap-6" aria-labelledby="recruitment-title">
    <header class="flex flex-wrap items-end justify-between gap-3">
      <div class="space-y-2">
        <h1 id="recruitment-title" class="text-2xl font-semibold tracking-tight">招聘数据分析</h1>
        <p class="text-sm text-muted-foreground">按城市与职位比较招聘样本，查看薪资和任职要求。</p>
      </div>
      <UiButton variant="outline" as-child>
        <a href="#salary-ranking-title">查看全局高薪职位排行</a>
      </UiButton>
    </header>
    <UiAlert v-if="sourcesUnavailable" variant="destructive" role="alert">
      <UiAlertTitle>招聘数据暂时无法读取</UiAlertTitle>
      <UiAlertDescription
        >城市、职位和排行均未读取成功。请稍后分别重试，或检查服务连接。</UiAlertDescription
      >
    </UiAlert>
    <form
      class="min-w-0 space-y-5 rounded-lg border-[0.5px] border-border bg-card p-4 sm:p-5"
      aria-label="招聘分析筛选"
      @submit.prevent="submitDraft()"
    >
      <div class="grid gap-4 sm:grid-cols-2">
        <UiField
          ><UiFieldLabel for="recruit-city">城市</UiFieldLabel
          ><RecruitmentOptionPicker
            id="recruit-city"
            :model-value="draft.city"
            :options="cityOptions"
            :disabled="cityPending || queryPending || restoring"
            described-by="recruit-city-status"
            @update:model-value="changeDraftCity"
          />
          <div id="recruit-city-status">
            <p v-if="cityPending || (cityState === 'ready' && !cities.length)" role="status" class="text-xs text-muted-foreground">
              {{ cityPending ? '正在加载城市选项…' : '暂无城市选项，可使用全部城市。' }}
            </p>
            <UiAlert v-else-if="cityState === 'error' || cityState === 'forbidden'" variant="destructive" role="alert">
              <UiAlertDescription>{{ cityState === 'forbidden' ? '无权读取城市选项。' : '城市选项读取失败。' }}</UiAlertDescription>
            </UiAlert>
          </div>
          <UiButton
            v-if="cityState === 'error' || cityState === 'forbidden'"
            type="button" variant="ghost" size="sm" class="self-start"
            :aria-disabled="cityPending" @click="loadCities"
          >重试城市选项</UiButton></UiField
        ><UiField
          ><UiFieldLabel for="recruit-position">职位</UiFieldLabel
          ><RecruitmentOptionPicker
            id="recruit-position"
            v-model="draft.position"
            :options="positionOptions"
            :disabled="positionPending || queryPending || restoring"
            described-by="recruit-position-status"
          />
          <div id="recruit-position-status">
            <p v-if="positionPending || (positionState === 'ready' && !positions.length)" role="status" class="text-xs text-muted-foreground">
              {{ positionPending ? '正在加载职位选项…' : '当前城市范围暂无职位选项，可使用全部职位。' }}
            </p>
            <UiAlert v-else-if="positionState === 'error' || positionState === 'forbidden'" variant="destructive" role="alert">
              <UiAlertDescription>{{
                positionState === 'forbidden' ? '无权读取职位选项。' : '职位选项读取失败，请重试或切换城市。'
              }}</UiAlertDescription>
            </UiAlert>
          </div>
          <UiButton
            v-if="positionState === 'error' || positionState === 'forbidden'"
            type="button" variant="ghost" size="sm" class="self-start"
            :aria-disabled="positionPending || restoring" @click="retryPositions"
          >重试职位选项</UiButton></UiField
        >
      </div>
      <div class="space-y-3 border-t-[0.5px] border-border pt-4">
        <div class="grid gap-4 lg:grid-cols-2">
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
              aria-describedby="recruit-axis-help"
              class="flex flex-wrap gap-x-5 gap-y-3"
              :disabled="queryPending || restoring"
              ><label
                v-for="option in analysisAxes"
                :key="option.value"
                class="flex items-center gap-2 text-sm has-disabled:text-muted-foreground"
                ><UiRadioGroupItem
                  :value="option.value"
                  :disabled="option.value === draft[axis.key === 'x' ? 'y' : 'x']"
                />{{ option.label }}</label
              ></UiRadioGroup
            ></UiField
          >
        </div>
        <p id="recruit-axis-help" class="text-xs text-muted-foreground">
          横轴和纵轴须使用不同维度。另一轴已选的维度暂不可选。
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <UiButton type="submit" :aria-disabled="draftPending" class="aria-disabled:opacity-50"
          ><HugeiconsIcon :icon="Search01Icon" class="size-4 -ml-0.5" aria-hidden="true" />{{
            queryPending ? '正在查询…' : '开始分析'
          }}</UiButton
        ><UiButton
          type="button"
          variant="outline"
          :aria-disabled="queryPending || restoring"
          class="aria-disabled:opacity-50"
          @click="resetDraft"
          >重置草稿</UiButton
        >
      </div>
      <p v-if="formError" ref="formFeedback" tabindex="-1" role="alert" class="text-sm text-destructive">
        {{ formError }}
      </p>
      <p
        v-if="changed"
        role="status"
        class="text-sm text-status-warning-foreground"
      >
        草稿尚未应用。{{ queryFailed ? '可重试上次条件，或按当前草稿查询。' : '开始分析后更新结果。' }}
      </p>
    </form>
    <div v-if="applied" class="space-y-3">
      <p class="break-words text-xs leading-5 text-muted-foreground">
        {{ queryFailed ? '上次查询条件' : '已应用条件' }}：{{ scope }}。
        <template v-if="summary">指标基于本次返回样本。</template>
      </p>
      <div v-if="queryPending || summary" class="grid gap-4 sm:grid-cols-3">
        <UiCard
          v-for="metric in [
            {
              label: '返回样本总数',
              value: summary?.count,
              unit: '条',
              note: '所选维度数值有效的返回记录',
            },
            {
              label: '去重公司数',
              value: summary?.companies,
              unit: '家',
              note: '按已提供的公司名称去重',
            },
            {
              label: '有效薪资均值',
              value: summary?.average,
              unit: 'K',
              note: summary ? `${number.format(summary.salaryCount)} 条有效薪资参与计算` : '尚未取得薪资样本',
            },
          ]"
          :key="metric.label"
          class="min-w-0 gap-0 border-[0.5px] py-0"
          ><UiCardContent class="p-5"
            ><h2 class="text-xs text-muted-foreground">{{ metric.label }}</h2>
            <UiSkeleton v-if="jobsState === 'loading'" class="mt-4 h-8 w-24" />
            <p v-else class="mt-3 break-all font-mono text-3xl font-semibold tabular-nums">
              {{ metric.value == null ? '—' : number.format(metric.value)
              }}<span
                v-if="metric.value != null"
                class="ml-2 text-xs font-normal text-muted-foreground"
              >{{
                metric.unit
              }}</span>
            </p>
            <p class="mt-2 text-xs text-muted-foreground">{{ metric.note }}</p></UiCardContent
          ></UiCard
        >
      </div>
    </div>
    <div
      class="min-w-0 rounded-lg border-[0.5px] border-border bg-card"
      :aria-busy="jobsState === 'loading'"
    >
      <div class="border-b-[0.5px] border-border/70 px-5 py-4">
        <h2 ref="analysisTitle" tabindex="-1" class="text-sm font-semibold">职位样本分析</h2>
      </div>
      <div v-if="jobsState === 'idle'" role="status" class="space-y-3 px-6 py-12 text-center">
        <h3 class="text-sm font-medium">选择条件，开始查询</h3>
        <p class="text-sm text-muted-foreground">
          在上方选择城市、职位和分析维度，点击“开始分析”查看样本。
        </p>
      </div>
      <div v-else-if="jobsState === 'loading'" role="status" class="space-y-4 p-6">
        <p class="text-sm text-muted-foreground">正在读取并分析职位样本…</p>
        <UiSkeleton class="h-52 w-full" />
      </div>
      <div
        v-else-if="jobsState === 'error' || jobsState === 'forbidden'"
        class="space-y-4 p-5"
      >
        <UiAlert variant="destructive" role="alert">
          <UiAlertTitle>
            {{ jobsState === 'forbidden' ? '当前账号无权读取招聘分析' : '招聘分析读取失败' }}
          </UiAlertTitle>
          <UiAlertDescription>{{
            jobsState === 'forbidden'
              ? '请联系管理员确认访问权限。'
              : '请检查服务连接或调整筛选条件后重试。'
          }}</UiAlertDescription>
        </UiAlert>
        <div class="flex flex-wrap gap-3">
          <UiButton variant="outline" :aria-disabled="queryPending" @click="retryAnalysis">重试上次条件</UiButton>
          <UiButton
            v-if="changed" variant="outline" :aria-disabled="draftPending" class="aria-disabled:opacity-50"
            @click="submitDraft(true)"
          >按当前草稿查询</UiButton>
        </div>
      </div>
      <div v-else-if="!jobs.length" role="status" class="space-y-3 px-6 py-12 text-center">
        <h3 class="text-sm font-medium">暂无匹配的职位样本</h3>
        <p class="text-sm text-muted-foreground">
          当前条件没有返回所选维度均有效的样本，可调整城市、职位或分析维度。
        </p>
        <UiButton
          variant="outline" :aria-disabled="draftPending" class="aria-disabled:opacity-50"
          @click="resetAndRun"
        >重置并重新分析</UiButton>
      </div>
      <template v-else-if="applied"
        ><div class="border-b border-border p-3 sm:p-5">
          <RecruitmentScatterPlot :jobs="jobs" :x="applied.x" :y="applied.y" />
        </div>
        <UiTable
          ><UiTableCaption class="sr-only">招聘职位样本及薪资、学历和经验要求</UiTableCaption
          ><UiTableHeader
            ><UiTableRow class="border-b-[0.5px]"
              ><UiTableHead class="px-3 sm:px-5">职位 / 公司</UiTableHead
              ><UiTableHead class="hidden sm:table-cell">城市</UiTableHead
              ><UiTableHead class="text-right">薪资</UiTableHead
              ><UiTableHead class="hidden md:table-cell">学历</UiTableHead
              ><UiTableHead class="hidden lg:table-cell">经验</UiTableHead
              ><UiTableHead class="w-20 text-center">操作</UiTableHead></UiTableRow
            ></UiTableHeader
          ><UiTableBody
            ><UiTableRow v-for="job in visible" :key="job.key" class="border-b-[0.5px]"
              ><UiTableCell class="min-w-32 max-w-80 whitespace-normal px-3 sm:px-5"
                ><UiButton
                  variant="ghost"
                  class="h-auto justify-start whitespace-normal break-words p-0 text-left font-medium underline-offset-4 hover:bg-transparent hover:text-accent-data-foreground hover:underline"
                  @click="showJob(job, $event)"
                  >{{ job.positionName || '未提供职位名称' }}</UiButton
                >
                <p class="mt-1 break-words text-xs text-muted-foreground">
                  {{ job.companyName || '未提供公司名称' }}
                </p>
                <p class="mt-1 text-xs text-muted-foreground sm:hidden">
                  {{ job.cityName || '—' }}
                </p>
                <StatusBadge :tone="analysisStatusTones[job.analysisStatus]" class="mt-2 max-w-full">
                  {{ analysisStatusLabels[job.analysisStatus] }}
                </StatusBadge>
              </UiTableCell
              ><UiTableCell class="hidden sm:table-cell">{{ job.cityName || '—' }}</UiTableCell
              ><UiTableCell class="whitespace-normal break-all text-right font-mono tabular-nums">{{
                formatSampleSalary(job.salary)
              }}</UiTableCell
              ><UiTableCell class="hidden md:table-cell">{{ job.degree || '—' }}</UiTableCell
              ><UiTableCell class="hidden lg:table-cell">{{ job.experience || '—' }}</UiTableCell
              ><UiTableCell class="text-center"
                ><UiButton
                  variant="ghost"
                  size="sm"
                  :aria-label="`查看${job.positionName || '职位'}详情`"
                  @click="showJob(job, $event)"
                  >详情</UiButton
                ></UiTableCell
              ></UiTableRow
            ></UiTableBody
          ></UiTable
        >
        <div
          class="flex flex-wrap items-center justify-between gap-4 border-t-[0.5px] border-border/70 p-4"
        >
          <UiRadioGroup :model-value="size" aria-label="样本每页条数" class="flex gap-4" @update:model-value="changeSize"
            ><label
              v-for="count in samplePageSizes"
              :key="count"
              class="flex items-center gap-2 text-xs"
              ><UiRadioGroupItem :value="count" />{{ count }} 条</label
            ></UiRadioGroup
          >
          <div class="flex min-w-0 flex-wrap items-center gap-3">
            <span class="font-mono text-xs tabular-nums text-muted-foreground">共 {{ jobs.length }} 条</span
            ><UiPagination
              v-model:page="page"
              :items-per-page="size"
              :total="jobs.length"
              :sibling-count="1"
              class="w-auto max-w-full"
              aria-label="样本分页"
              ><UiPaginationContent v-slot="{ items }" class="flex-wrap"
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
      class="min-w-0 rounded-lg border border-border bg-card shadow-panel"
      aria-labelledby="salary-ranking-title"
      :aria-busy="rankingState === 'loading'"
    >
      <div
        class="flex flex-wrap items-center justify-between gap-4 border-b-[0.5px] border-border/70 p-5"
      >
        <div class="space-y-1.5">
          <h2 id="salary-ranking-title" class="text-sm font-semibold">全局高薪职位排行</h2>
          <p class="text-xs text-muted-foreground">
            全局统计，与分析筛选独立。入榜职位至少 10 条样本，推荐城市至少 5 条样本。
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
              ><UiRadioGroupItem :value="String(count)" />Top {{ count }}</label
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
            ><UiTableHead class="hidden text-right lg:table-cell">样本数</UiTableHead
            ><UiTableHead class="text-right">平均薪资（K）</UiTableHead
            ><UiTableHead class="hidden text-right lg:table-cell">薪资范围（K）</UiTableHead
            ><UiTableHead class="hidden lg:table-cell">推荐城市</UiTableHead></UiTableRow
          ></UiTableHeader
        ><UiTableBody
          ><UiTableRow
            v-for="(row, index) in ranking"
            :key="`${row.position}:${index}`"
            class="border-b-[0.5px]"
            ><UiTableCell class="px-5 text-right font-mono tabular-nums text-muted-foreground">{{
              index + 1
            }}</UiTableCell
            ><UiTableCell class="whitespace-normal break-words">
              <p class="font-medium">{{ row.position }}</p>
              <div class="mt-1 space-y-1 text-xs text-muted-foreground lg:hidden">
                <p class="font-mono tabular-nums">{{ number.format(row.count) }} 条 · {{ row.minimum }}–{{ row.maximum }} K</p>
                <p v-if="row.city">
                  推荐城市 {{ row.city }} · {{ row.citySalary == null ? '—' : number.format(row.citySalary) + ' K' }} · {{ row.cityCount ?? '—' }} 条
                </p>
                <p v-else>推荐城市 —</p>
              </div>
            </UiTableCell
            ><UiTableCell class="hidden text-right font-mono tabular-nums lg:table-cell">{{
              number.format(row.count)
            }}</UiTableCell
            ><UiTableCell class="text-right font-mono tabular-nums">{{
              number.format(row.average)
            }}</UiTableCell
            ><UiTableCell class="hidden text-right font-mono tabular-nums lg:table-cell"
              >{{ row.minimum }}–{{ row.maximum }}</UiTableCell
            ><UiTableCell class="hidden lg:table-cell"
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
    <RecruitmentJobSheet
      :job="selected"
      @close="selected = null"
      @close-auto-focus="restoreSheetFocus"
    />
  </section>
</template>
