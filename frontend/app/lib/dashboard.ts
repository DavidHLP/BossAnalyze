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

export function dashboardSnapshot(value: unknown): DashboardSnapshot | null {
  if (!object(value) || !object(value.overview)) throw new Error('Invalid dashboard response')
  const visits = count(value.overview.totalVisits)
  const visitors = count(value.overview.uniqueVisitors)
  // The gateway returns only these sections when the upstream snapshot is unavailable.
  if (!('heatMapData' in value) && !('topPages' in value) && !('deviceStats' in value)) {
    return null
  }
  // Hutool's cached LocalDateTime is returned as epoch milliseconds by the gateway.
  const generatedAt =
    typeof value.generatedAt === 'number' &&
    Number.isSafeInteger(value.generatedAt) &&
    Number.isFinite(new Date(value.generatedAt).getTime())
      ? new Date(value.generatedAt).toISOString()
      : value.generatedAt
  if (
    typeof generatedAt !== 'string' ||
    !generatedAt.trim() ||
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
  if (hours.some((row) => !/^\d{4}-\d{2}-\d{2} (?:[01]\d|2[0-3])$/.test(row.label))) {
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
  if (!/(?:Z|[+-]\d{2}:\d{2})$/i.test(value)) return `${value.replace('T', ' ')}（服务端时间，时区未提供）`
  return `${new Date(value).toISOString().replace('T', ' ')}（UTC）`
}

export const snapshotScope = '当前配置日志目录中可解析访问记录的分析快照；非固定近 24 小时统计'
