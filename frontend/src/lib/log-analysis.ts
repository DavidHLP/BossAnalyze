import { parseSnapshotTime } from './dashboard.ts'

export type LogReadResult = 'updated' | 'unchanged' | 'empty' | 'error' | 'forbidden' | null

export function logReadStatus(parts: ReadonlyArray<{ loading: boolean; result: LogReadResult }>) {
  if (parts.some(part => part.loading)) return '正在读取日志缓存…'
  const results = parts.map(part => part.result).filter(result => result !== null)
  if (!results.length) return ''
  if (results.every(result => result === 'forbidden')) return '日志分项读取均被拒绝（403）'
  if (results.includes('forbidden')) {
    return results.includes('error')
      ? '部分日志分项读取被拒绝（403），其他分项读取失败'
      : '部分日志分项读取被拒绝（403）'
  }
  if (results.every(result => result === 'empty')) return '暂无可用的日志快照'
  if (results.every(result => result === 'empty' || result === 'error')) return '未能读取日志快照，请重试'
  if (results.includes('error')) {
    return results.includes('empty') ? '部分日志分项暂无快照或读取失败' : '部分日志分项读取失败'
  }
  if (results.includes('empty')) return '部分日志分项暂无快照'
  return results.includes('unchanged') ? '已读取全部日志分项，星期快照暂无更新' : '已读取全部日志分项'
}

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

function text(value: unknown) {
  return typeof value === 'string' ? value.trim() || null : null
}

function flag(value: unknown) {
  return typeof value === 'boolean' ? value : null
}

function nonnegative(value: unknown) {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : null
}

export function ipDistribution(value: unknown) {
  const data = object(value)
  type Group = { key: string; name: string; count: number }
  const countries = new Map<string, Group>()
  const cities = new Map<string, Group>()
  const known = new Set<string>()
  function add(map: Map<string, Group>, key: string, name: string, count: number) {
    const group = map.get(key)
    if (group) group.count += count
    else map.set(key, { key, name, count })
  }
  const records = Object.entries(object(data.ipStats)).map(([ip, value]) => {
    const item = object(value)
    const count = amount(item.count)
    const country = text(item.countryName) ?? '未知'
    const city = text(item.city) ?? '未知'
    if (country !== '未知') known.add(country)
    add(countries, country, country, count)
    add(cities, JSON.stringify([country, city]), `${city}（${country}）`, count)
    return { ip, country, city, count, province: text(item.province) ?? text(item.region), isLocal: flag(item.isLocal) }
  }).sort((a, b) => b.count - a.count)
  function top(map: Map<string, Group>) {
    return [...map.values()].sort((a, b) => b.count - a.count)
  }
  return { records, countries: top(countries), cities: top(cities), known: known.size }
}

export const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

export function weekdayDistribution(value: unknown) {
  const data = object(value)
  const time = parseSnapshotTime(data.generatedAt)
  const byDay = object(object(data.weekdayStatistics).weekdayStats)
  const noVisits = data.overview != null && object(data.overview).totalVisits === 0
  // Spark dayofweek uses Sunday=1. Print Monday first.
  const days = [2, 3, 4, 5, 6, 7, 1].map((key, i) => {
    const day = byDay[key] == null ? null : object(byDay[key])
    return {
      name: weekdays[i]!,
      count: day ? amount(day.count) : null,
      percentage: noVisits ? null : nonnegative(day?.percentage),
      isWorkday: flag(day?.isWorkday),
      isWeekend: flag(day?.isWeekend),
      avgHourlyVisits: nonnegative(day?.avgHourlyVisits),
    }
  })
  return { time, days }
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
