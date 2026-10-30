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
  if (
    typeof value.generatedAt !== 'string' ||
    !value.generatedAt.trim() ||
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
    generatedAt: value.generatedAt,
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
