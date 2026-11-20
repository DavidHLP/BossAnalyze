export interface CountRow {
  label: string
  count: number
}

export interface DashboardSnapshot {
  generatedAt: string
  visits: number
  visitors: number
  hours: CountRow[]
  pages: CountRow[]
  browsers: CountRow[]
  methods: CountRow[]
}

function object(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function count(value: unknown): number {
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) {
    throw new Error('Invalid dashboard count')
  }
  return value
}

function distribution(value: unknown): CountRow[] {
  if (!object(value)) throw new Error('Invalid dashboard distribution')
  return Object.entries(value)
    .map(([label, item]) => {
      if (!object(item)) throw new Error('Invalid dashboard distribution item')
      return { label, count: count(item.count) }
    })
    .sort((a, b) => b.count - a.count)
}

function validCalendarDate(value: string) {
  const day = new Date(`${value}T00:00:00Z`)
  return Number.isFinite(day.getTime()) && day.toISOString().split('T')[0] === value
}

export function dashboardSnapshot(value: unknown): DashboardSnapshot | null {
  if (value === null) return null
  if (!object(value) || !object(value.overview)) throw new Error('Invalid dashboard response')
  const visits = count(value.overview.totalVisits)
  const visitors = count(value.overview.uniqueVisitors)
  // Hutool's cached LocalDateTime is returned as epoch milliseconds by the gateway.
  const generatedAt =
    typeof value.generatedAt === 'number' &&
    Number.isSafeInteger(value.generatedAt) &&
    Number.isFinite(new Date(value.generatedAt).getTime())
      ? new Date(value.generatedAt).toISOString()
      : value.generatedAt
  if (
    typeof generatedAt !== 'string' ||
    !validCalendarDate(/^(?:\d{4}|[+-]\d{6})-\d{2}-\d{2}(?=[T\s]|$)/i.exec(generatedAt.trim())?.[0] ?? '') ||
    !Number.isFinite(Date.parse(generatedAt)) ||
    !object(value.heatMapData) ||
    !object(value.topPages) ||
    !Array.isArray(value.topPages.pages) ||
    !object(value.deviceStats)
  )
    throw new Error('Invalid dashboard snapshot')

  const hours = distribution(value.heatMapData.hourHeatmap).sort((a, b) =>
    a.label.localeCompare(b.label),
  )
  if (hours.some(({ label }) => !/^\d{4}-\d{2}-\d{2} (?:[01]\d|2[0-3])$/.test(label) || !validCalendarDate(label.slice(0, 10)))) {
    throw new Error('Invalid dashboard hour')
  }
  const pages = value.topPages.pages
    .map((item) => {
      if (!object(item) || typeof item.path !== 'string' || !item.path) {
        throw new Error('Invalid dashboard page')
      }
      return { label: item.path, count: count(item.visits) }
    })
    .sort((a, b) => b.count - a.count)
    .slice(0, 10)

  return {
    generatedAt,
    visits,
    visitors,
    hours,
    pages,
    browsers: distribution(value.deviceStats.browsers ?? {}),
    methods: distribution(
      object(value.httpMethodStatistics) ? value.httpMethodStatistics.methodStats : {},
    ),
  }
}

export function snapshotTime(value: string) {
  // A zone-less LocalDateTime is server wall time; do not invent its timezone.
  if (!/(?:Z|[+-]\d{2}:?\d{2}|GMT|UTC)$/i.test(value.trim())) {
    return `${value.replace('T', ' ')}（服务端时间，时区未提供）`
  }
  return `${new Date(value).toISOString().replace('T', ' ')}（UTC）`
}

export function peakVisits(hours: CountRow[]) {
  return hours.reduce((peak, hour) => Math.max(peak, hour.count), 0)
}

export function visitShare(count: number, total: number) {
  return total ? (count / total) * 100 : 0
}

export const snapshotScope = '统计配置日志目录中可解析的受保护 API 请求。非近 24 小时实时监控，无响应耗时或状态码。'
