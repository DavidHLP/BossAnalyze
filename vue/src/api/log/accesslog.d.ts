// 仪表板概览数据
export interface DashboardOverview {
  totalVisits: number;
  uniqueVisitors: number;
  avgSessionDuration: number;
  bounceRate: number;
  errorRate: number;
  visitsTrend?: TrendInfo;
  visitorsTrend?: TrendInfo;
}

// 趋势信息
export interface TrendInfo {
  changePercent: number;
  direction: 'up' | 'down' | 'stable';
  period: 'day' | 'week' | 'month';
}

// 实时统计
export interface RealTimeStats {
  onlineUsers: number;
  activePages: number;
  recentVisits: number;
  recentVisitList?: RealtimeVisit[];
}

// 实时访问记录
export interface RealtimeVisit {
  ip: string;
  path: string;
  method: string;
  timestamp: string;
  userAgent: string;
  location: string;
  responseTime: number;
}

// 热力图数据
export interface HeatMapData {
  minuteHeatmap: Record<string, TimePointInfo>;
  hourHeatmap: Record<string, TimePointInfo>;
}

export interface TimePointInfo {
  count: number;
  intensity: string;
}

// 热门页面统计
export interface TopPagesStats {
  pages: PageInfo[];
}

export interface PageInfo {
  path: string;
  visits: number;
  uniqueVisitors: number;
  avgTime: number;
  bounceRate: number;
  percentage: number;
  trend?: TrendInfo;
}

// 地理位置统计
export interface GeoStats {
  countryStats: Record<string, CountryInfo>;
  cityStats: Record<string, CityInfo>;
  geoPoints: GeoPoint[];
}

export interface CountryInfo {
  visits: number;
  uniqueVisitors: number;
  percentage: number;
  countryCode: string;
  trend?: TrendInfo;
}

export interface CityInfo {
  visits: number;
  latitude: number;
  longitude: number;
  country: string;
  region: string;
}

export interface GeoPoint {
  latitude: number;
  longitude: number;
  value: number;
  location: string;
}

// 设备统计
export interface DeviceStats {
  browsers: Record<string, BrowserInfo>;
  operatingSystems: Record<string, number>;
  devices: Record<string, number>;
  screenResolutions?: Record<string, number>;
}

export interface BrowserInfo {
  count: number;
  percentage: number;
  version: string;
  marketRank: number;
  isModern: boolean;
}

// 性能统计
export interface PerformanceStats {
  avgResponseTime: number;
  avgPageLoadTime: number;
  statusCodes: Record<string, number>;
  endpointPerformance?: Record<string, number>;
  slowRequests?: SlowRequest[];
}

export interface SlowRequest {
  path: string;
  method: string;
  responseTime: number;
  timestamp: string;
  ip: string;
}

// 安全统计
export interface SecurityStats {
  suspiciousIPs: number;
  attackAttempts: number;
  threatTypes: Record<string, number>;
  recentEvents?: SecurityEvent[];
}

export interface SecurityEvent {
  type: string;
  ip: string;
  description: string;
  timestamp: string;
  severity: string;
}

// 数据质量信息
export interface DataQualityInfo {
  totalLogLines: number;
  parsedLines: number;
  failedLines: number;
  parseSuccessRate: number;
  qualityScore: number;
}

// 完整的仪表板数据
export interface DashboardStats {
  generatedAt: string;
  timeRange: string;
  overview: DashboardOverview;
  realTimeStats: RealTimeStats;
  heatMapData: HeatMapData;
  topPages: TopPagesStats;
  geoStats: GeoStats;
  deviceStats: DeviceStats;
  performanceStats: PerformanceStats;
  securityStats: SecurityStats;
  dataQuality: DataQualityInfo;
}

// 访问日志统计数据
export interface AccessLogStats {
  heatmap: {
    minute_heatmap: Record<string, { count: number; intensity: string }>;
    hour_heatmap: Record<string, { count: number; intensity: string }>;
  };
  ipStats: Record<
    string,
    {
      count: number;
      country_name?: string;
      city?: string;
    }
  >;
  httpMethods: Record<
    string,
    {
      count: number;
      percentage: number;
      description: string;
      isSafe: boolean;
      isIdempotent: boolean;
    }
  >;
  weekdayStats: Record<
    string,
    {
      count: number;
      percentage: number;
      dayName: string;
      isWorkday: boolean;
      isWeekend: boolean;
      avgHourlyVisits: number;
    }
  >;
  browserStats: Record<
    string,
    {
      count: number;
      percentage: number;
      version: string;
      osDistribution: object;
      deviceTypeDistribution: object;
      marketRank: number;
      isModern: boolean;
    }
  >;
  totalVisits: number;
  uniqueIps: number;
  avgResponseTime: number;
  errorRate: number;
}

// 分析参数
export interface AnalyzeParams {
  logPath?: string;
  startTime?: string;
  endTime?: string;
  ip?: string;
  method?: string;
  statusCode?: number;
  path?: string;
}
