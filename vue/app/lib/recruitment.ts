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
  // The existing position lookup interpolates city into a Spark SQL expression.
  return Boolean(value && value.length <= 255 && !/['\\\u0000-\u001f\u007f]/u.test(value))
}
export function analysisQuery(filter: AnalysisFilter, cities: string[], positions: string[]) {
  if (
    !safeCity(filter.city) ||
    (filter.city !== 'all' && !cities.includes(filter.city)) ||
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
    .map(text)
    .filter(
      (item): item is string =>
        Boolean(item) && item !== '职位福利目前未解析' && item !== '工作需求目前未解析',
    )
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
  // shortcut: render at most 1000 coordinate groups; use a canvas plot for larger interactive views.
  const points = Array.from(
    { length: Math.min(all.length, 1000) },
    (_, index) => all[Math.floor((index * all.length) / Math.min(all.length, 1000))]!,
  ).map((point) => ({
    ...point,
    cx: minX === maxX ? 330 : 60 + ((point.x - minX) / (maxX - minX)) * 540,
    cy: minY === maxY ? 125 : 220 - ((point.y - minY) / (maxY - minY)) * 190,
  }))
  return {
    points,
    valid,
    groups: all.length,
    minX,
    maxX,
    minY,
    maxY,
    xLabel: xAxis.label,
    yLabel: yAxis.label,
  }
}
