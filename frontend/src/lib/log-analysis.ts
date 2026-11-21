function object(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('Invalid log response')
  return value as Record<string, unknown>
}

function amount(value: unknown): number {
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value < 0) throw new Error('Invalid log count')
  return value
}

export function logTotals(value: unknown) {
  const data = object(value)
  return { visits: amount(data.totalVisits), ips: amount(data.uniqueIps) }
}

export function heatBuckets(value: unknown) {
  const data = object(value)
  function read(key: 'hourHeatmap' | 'minuteHeatmap', expression: RegExp) {
    if (data[key] == null) return []
    return Object.entries(object(data[key])).map(([label, entry]) => {
      if (!expression.test(label)) throw new Error('Invalid bucket time')
      return { label, count: amount(object(entry).count) }
    }).sort((a, b) => a.label.localeCompare(b.label))
  }
  return {
    hours: read('hourHeatmap', /^\d{4}-\d{2}-\d{2} (?:[01]\d|2[0-3])$/),
    minutes: read('minuteHeatmap', /^\d{4}-\d{2}-\d{2} (?:[01]\d|2[0-3]):[0-5]\d$/),
  }
}

export function ipDistribution(value: unknown) {
  const data = object(value)
  const countries = new Map<string, number>()
  const cities = new Map<string, number>()
  const known = new Set<string>()
  const numberFor = (map: Map<string, number>, label: string, count: number) => map.set(label, (map.get(label) ?? 0) + count)
  const labelFor = (value: unknown) => typeof value === 'string' && value.trim() ? value.trim() : '未知'
  const records = Object.entries(data.ipStats == null ? {} : object(data.ipStats)).map(([ip, value]) => {
    const item = object(value)
    const count = amount(item.count)
    const country = labelFor(item.countryName)
    const city = labelFor(item.city)
    if (country !== '未知') known.add(country)
    numberFor(countries, country, count)
    numberFor(cities, city, count)
    return { ip, country, city, count }
  }).sort((a, b) => b.count - a.count)
  function top(map: Map<string, number>) {
    return [...map].map(([name, count]) => ({ name, count })).sort((a, b) => b.count - a.count)
  }
  return { records, countries: top(countries), cities: top(cities), known: known.size }
}

export const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

export function weekdayDistribution(value: unknown) {
  const data = object(value)
  const times = typeof data.generatedAt === 'number' && Number.isFinite(new Date(data.generatedAt).getTime())
    ? new Date(data.generatedAt).toISOString() : typeof data.generatedAt === 'string' ? data.generatedAt : ''
  const distribution = data.weekdayStatistics == null ? {} : object(data.weekdayStatistics)
  const byDay = distribution.weekdayStats == null ? {} : object(distribution.weekdayStats)
  // Spark dayofweek uses Sunday=1. Print Monday first.
  return { time: times, days: [2, 3, 4, 5, 6, 7, 1].map((key, i) => ({
    name: weekdays[i]!,
    count: byDay[key] == null ? null : amount(object(byDay[key]).count),
  })) }
}

export function weekdayAverages(hours: { label: string; count: number }[]) {
  const dates = new Map<string, number>()
  for (const { label, count } of hours) {
    const day = label.slice(0, 10)
    dates.set(day, (dates.get(day) ?? 0) + count)
  }
  const workdays: number[] = []
  const weekends: number[] = []
  for (const [label, count] of dates) {
    const day = new Date(label + 'T00:00:00Z').getUTCDay()
    if (!Number.isNaN(day)) (day === 0 || day === 6 ? weekends : workdays).push(count)
  }
  const average = (values: number[]) => values.length ? values.reduce((a, b) => a + b, 0) / values.length : null
  return { workdays: average(workdays), weekends: average(weekends) }
}

export function heatLevel(count: number | undefined) {
  if (count === undefined) return 0
  if (count === 0) return 1
  return count >= 1500 ? 6 : count >= 800 ? 5 : count >= 400 ? 4 : count >= 100 ? 3 : 2
}
