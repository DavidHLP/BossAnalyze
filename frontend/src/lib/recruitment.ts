export const analysisStatusLabels = {
  unparsed: '未分析',
  legacy_unverified: '历史结果（来源未验证）',
  pending: '等待分析',
  running: '分析中',
  complete: '已完成（当前来源）',
  no_evidence: '已分析，原文未说明',
  needs_review: '待复核',
  failed: '分析失败',
  stale: '来源已变更',
  unknown: '分析状态未知',
} as const
export type AnalysisStatus = keyof typeof analysisStatusLabels
export const analysisStatusTones = {
  unparsed: 'neutral',
  legacy_unverified: 'warning',
  pending: 'neutral',
  running: 'info',
  complete: 'success',
  no_evidence: 'success',
  needs_review: 'warning',
  failed: 'danger',
  stale: 'warning',
  unknown: 'neutral',
} as const satisfies Record<AnalysisStatus, 'success' | 'warning' | 'danger' | 'neutral' | 'info'>

function analysisStatus(value: unknown): AnalysisStatus {
  return typeof value === 'string' && Object.hasOwn(analysisStatusLabels, value)
    ? value as AnalysisStatus : 'unknown'
}
export const analysisAxes = [
  { value: 'salary_value', key: 'salaryValue', label: '薪资（K）' },
  { value: 'degree_value', key: 'degreeValue', label: '学历等级' },
  { value: 'experience_value', key: 'experienceValue', label: '经验折算值' },
] as const
export interface AnalysisFilter {
  city: string
  position: string
  x: string
  y: string
}

export const defaultAnalysisFilter = {
  city: 'all',
  position: 'all',
  x: 'salary_value',
  y: 'degree_value',
} as const satisfies AnalysisFilter
export const samplePageSizes = [10, 20, 50] as const
export const defaultSampleSize = 20

export function sameAnalysisFilter(first: AnalysisFilter, second: AnalysisFilter) {
  return first.city === second.city && first.position === second.position &&
    first.x === second.x && first.y === second.y
}

export interface JobSample {
  key: string
  positionId: string | null
  positionName: string | null
  companyName: string | null
  cityName: string | null
  salary: string | null
  salaryValue: number | null
  degree: string | null
  degreeValue: number | null
  experience: string | null
  experienceValue: number | null
  companySize: string | null
  financingStage: string | null
  address: string | null
  companyUrl: string | null
  jobUrl: string | null
  analysisStatus: AnalysisStatus
  analysisModel: string | null
  analysisVersion: string | null
  employeeBenefits: string[]
  jobRequirements: string[]
}
function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value))
    throw new Error('Invalid recruitment data')
  const data = value as Record<string, unknown>
  if ('code' in data && data.code !== 200) throw data
  return data
}
function list(value: unknown): unknown[] {
  if (!Array.isArray(value)) {
    record(value)
    throw new Error('Invalid recruitment list')
  }
  return value
}
function text(value: unknown): string | null {
  if (value == null) return null
  if (typeof value !== 'string') throw new Error('Invalid recruitment text')
  return value.trim() ? value : null
}
function numeric(value: unknown): number | null {
  if (value == null) return null
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0)
    throw new Error('Invalid recruitment number')
  return value
}
function count(value: unknown): number {
  const result = numeric(value)
  if (result === null || !Number.isSafeInteger(result)) throw new Error('Invalid recruitment count')
  return result
}
export function optionNames(value: unknown): string[] {
  return [
    ...new Set(
      list(value)
        .map(text)
        .filter((name): name is string => name !== null && name.toLowerCase() !== 'all'),
    ),
  ]
}
export function safeCity(value: string) {
  // Keep the supported city-filter values consistent between option selection and submission.
  return Boolean(value && value.length <= 255 && !/['\\\u0000-\u001f\u007f]/u.test(value))
}

export function recruitmentQueryCity(value: unknown, cities: string[]) {
  if (typeof value === 'string' && safeCity(value) && (value === 'all' || cities.includes(value))) {
    return value
  }
}

export function readRecruitmentQuery(query: Record<string, unknown>, cities: string[], positions: string[]) {
  const defaults = defaultAnalysisFilter
  const city = recruitmentQueryCity(query.city, cities)
  const position = typeof query.position === 'string' && (query.position === 'all' || positions.includes(query.position))
    ? query.position : undefined
  const x = analysisAxes.find((axis) => axis.value === query.x)?.value
  const y = analysisAxes.find((axis) => axis.value === query.y)?.value
  const page = typeof query.page === 'string' && /^[1-9]\d*$/.test(query.page) ? Number(query.page) : NaN
  const validPage = Number.isSafeInteger(page)
  const size = samplePageSizes.find((value) => String(value) === query.size)
  if (!city && !position && x === y && !validPage && size === undefined) return null

  const sameAxis = x !== undefined && x === y
  return {
    filter: {
      city: city ?? defaults.city,
      position: position ?? defaults.position,
      x: sameAxis ? defaults.x : x ?? (y === defaults.x ? defaults.y : defaults.x),
      y: sameAxis ? defaults.y : y ?? (x === defaults.y ? defaults.x : defaults.y),
    },
    page: validPage ? page : 1,
    size: size ?? defaultSampleSize,
  }
}

export function formatSampleSalary(value: string | null) {
  return value?.replace(/(\d)\s*K\b/g, '$1 K') ?? '—'
}

const degreeLabels = ['不限', '初中', '高中/中专', '大专', '本科', '硕士', '博士']
const experienceTicks = [0, 1, 2, 4, 7, 10]

export function scatterTicks(axis: string, min: number, max: number) {
  if (min === max) return [min]
  const candidates = axis === 'degree_value' ? degreeLabels.map((_, index) => index)
    : axis === 'experience_value' ? experienceTicks : []
  const interior = candidates.filter((value) => value > min && value < max)
  if (interior.length || candidates.length) return [min, ...interior, max]
  return [min, min + (max - min) / 2, max]
}

export function degreeLabel(value: number) {
  return degreeLabels[value]
}
export function analysisQuery(filter: AnalysisFilter, cities: string[], positions: string[]) {
  if (
    !recruitmentQueryCity(filter.city, cities) ||
    (filter.position !== 'all' && !positions.includes(filter.position)) ||
    !analysisAxes.some((axis) => axis.value === filter.x) ||
    !analysisAxes.some((axis) => axis.value === filter.y) ||
    filter.x === filter.y
  )
    throw new Error('Invalid analysis filter')
  return [filter.city, filter.position, filter.x, filter.y] as const
}
function strings(value: unknown) {
  if (value == null) return []
  return list(value)
    .map((item) => {
      if (typeof item !== 'string') throw new Error('Invalid recruitment array item')
      return item
    })
    .filter((item) => item !== '职位福利目前未解析' && item !== '工作需求目前未解析')
}
export function jobSamples(value: unknown): JobSample[] {
  return list(value).map((item, index) => {
    const data = record(item)
    const fields = Object.fromEntries(
      [
        'positionId',
        'positionName',
        'companyName',
        'cityName',
        'salary',
        'degree',
        'experience',
        'companySize',
        'financingStage',
        'address',
        'companyUrl',
      ].map((key) => [key, text(data[key])]),
    )
    return {
      ...fields,
      key: `${fields.positionId ?? 'row'}:${index}`,
      jobUrl: text(data.jobUrl ?? data.JobUrl),
      salaryValue: numeric(data.salaryValue),
      degreeValue: numeric(data.degreeValue),
      experienceValue: numeric(data.experienceValue),
      analysisStatus: analysisStatus(data.analysisStatus),
      analysisModel: text(data.analysisModel),
      analysisVersion: text(data.analysisVersion),
      employeeBenefits: strings(data.employeeBenefits),
      jobRequirements: strings(data.jobRequirements),
    } as JobSample
  })
}
export function salaryRanking(value: unknown) {
  return list(value).map((item) => {
    const data = record(item)
    const position = text(data.positionName)
    const minimum = numeric(data.minSalary),
      maximum = numeric(data.maxSalary),
      average = numeric(data.avgSalary)
    if (
      !position ||
      minimum === null ||
      maximum === null ||
      average === null ||
      minimum > maximum ||
      average < minimum ||
      average > maximum
    )
      throw new Error('Invalid salary ranking')
    const cityCount =
      data.recommendedCityJobCount == null ? null : count(data.recommendedCityJobCount)
    return {
      position,
      count: count(data.jobCount),
      minimum,
      maximum,
      average,
      city: text(data.recommendedCity),
      citySalary: numeric(data.recommendedCitySalary),
      cityCount,
    }
  })
}
export function jobSummary(jobs: JobSample[]) {
  const salaries = jobs.flatMap((job) => (job.salaryValue === null ? [] : [job.salaryValue]))
  return {
    count: jobs.length,
    companies: new Set(jobs.map((job) => job.companyName).filter(Boolean)).size,
    salaryCount: salaries.length,
    average: salaries.length
      ? salaries.reduce((sum, value) => sum + value, 0) / salaries.length
      : null,
  }
}
export function safeSourceUrl(value: string | null) {
  if (!value || !/^https?:\/\//i.test(value)) return null
  try {
    const url = new URL(value)
    return ['http:', 'https:'].includes(url.protocol) && !url.username && !url.password
      ? url.href
      : null
  } catch {
    return null
  }
}
export function scatterData(jobs: JobSample[], x: string, y: string) {
  const xAxis = analysisAxes.find((axis) => axis.value === x),
    yAxis = analysisAxes.find((axis) => axis.value === y)
  if (!xAxis || !yAxis || x === y) throw new Error('Invalid plot axes')
  const groups = new Map<string, { x: number; y: number; count: number }>()
  let valid = 0
  for (const job of jobs) {
    const a = job[xAxis.key],
      b = job[yAxis.key]
    if (a === null || b === null) continue
    valid++
    const key = `${a}:${b}`
    const existing = groups.get(key)
    if (existing) existing.count++
    else groups.set(key, { x: a, y: b, count: 1 })
  }
  const all = [...groups.values()]
  const xs = all.map((point) => point.x),
    ys = all.map((point) => point.y)
  const minX = xs.reduce((min, value) => Math.min(min, value), Infinity)
  const maxX = xs.reduce((max, value) => Math.max(max, value), -Infinity)
  const minY = ys.reduce((min, value) => Math.min(min, value), Infinity)
  const maxY = ys.reduce((max, value) => Math.max(max, value), -Infinity)
  const plotX = (value: number) => minX === maxX ? 330 : 60 + ((value - minX) / (maxX - minX)) * 540
  const plotY = (value: number) => minY === maxY ? 125 : 220 - ((value - minY) / (maxY - minY)) * 190
  // shortcut: render at most 1000 coordinate groups; use a canvas plot for larger interactive views.
  const groupLimit = 1000
  const plotted = Math.min(all.length, groupLimit)
  const points = Array.from(
    { length: plotted },
    (_, index) => all[Math.floor((index * all.length) / plotted)]!,
  ).map((point) => ({
    ...point,
    cx: plotX(point.x),
    cy: plotY(point.y),
  }))
  return {
    points,
    valid,
    groups: all.length,
    groupLimit,
    allGroups: all,
    minX,
    maxX,
    minY,
    maxY,
    xLabel: xAxis.label,
    yLabel: yAxis.label,
    xTicks: valid ? scatterTicks(x, minX, maxX).map((value) => ({ value, x: plotX(value) })) : [],
    yTicks: valid ? scatterTicks(y, minY, maxY).map((value) => ({ value, y: plotY(value) })) : [],
  }
}
