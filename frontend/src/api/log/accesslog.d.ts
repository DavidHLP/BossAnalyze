export interface DashboardStats {
  generatedAt: string | number
  overview: {
    totalVisits: number
    uniqueVisitors: number
  }
  heatMapData: {
    minuteHeatmap: Record<string, TimePointInfo>
    hourHeatmap: Record<string, TimePointInfo>
  }
  topPages: {
    pages: { path: string; visits: number }[]
  }
  geoStats: {
    countryStats: Record<string, CountryInfo>
    geoPoints: { latitude: number; longitude: number; value: number; location: string }[]
  }
  deviceStats: {
    browsers?: Record<string, BrowserInfo>
  }
  httpMethodStatistics?: {
    methodStats: Record<string, HttpMethodInfo>
  }
  weekdayStatistics?: {
    weekdayStats: Record<string, WeekdayInfo>
  }
  dataQuality: {
    totalLogLines: number
    parsedLines: number
    failedLines: number
    parseSuccessRate: number
    qualityScore: number
  }
}

interface TimePointInfo {
  count: number
  intensity: string
}

interface CountryInfo {
  visits: number
  uniqueVisitors: number
  percentage: number
  countryCode: string
}

interface BrowserInfo {
  count: number
  percentage: number
  version: string
  osDistribution: Record<string, number>
  deviceTypeDistribution: Record<string, number>
  marketRank: number
  isModern: boolean
}

interface HttpMethodInfo {
  count: number
  percentage: number
  description: string
  isSafe: boolean
  isIdempotent: boolean
}

interface WeekdayInfo {
  count: number
  percentage: number
  dayName: string
  isWorkday: boolean
  isWeekend: boolean
  avgHourlyVisits: number
}
