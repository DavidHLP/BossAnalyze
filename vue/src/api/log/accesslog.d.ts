// 访问日志分析结果接口定义
export interface AccessLogSummary {
  totalRequests: number;
  uniqueIPs: number;
  uniquePaths: number;
  apiCategoryStats: Record<string, number>;
  popularPaths: Record<string, number>;
}

export interface IpStats {
  uniqueCount: number;
  topIPs: Record<string, number>;
  ipPathStats?: Record<string, number>; // IP与路径组合分析
  geoDistribution?: Record<string, number>; // IP地理位置分布
}

export interface UrlStats {
  uniqueCount: number;
  topUrls: Record<string, number>;
  methodDistribution?: Record<string, number>; // HTTP方法分布
  apiSequence?: Record<string, string[]>; // API调用序列
}

export interface TimeStats {
  peakHour: number;
  hourlyDistribution: Record<string, number>;
  dailyStats?: Record<string, number>; // 每日统计
  weekdayStats?: Record<string, number>; // 工作日/周末分布
  monthlyStats?: Record<string, number>; // 月度统计
  heatmapData?: Record<string, number>; // 热力图数据
}

// 新增异常检测统计
export interface AnomalyStats {
  avgRequests: number;
  stddevRequests: number;
  p95Requests: number;
  stdThreshold: number;
  stdAnomalies: Record<string, number>;
  p95Anomalies: Record<string, number>;
}

// 新增用户行为分析
export interface UserBehaviorStats {
  uaStats: Record<string, number>; // 用户代理统计
  topReferrers?: Record<string, number>; // 来源页面统计
  sessionDuration?: Record<string, number>; // 会话时长统计
}

export interface AllStats {
  summary?: AccessLogSummary;
  ipStats?: IpStats;
  urlStats?: UrlStats;
  timeStats?: TimeStats;
  anomalyStats?: AnomalyStats; // 新增异常检测
  userBehaviorStats?: UserBehaviorStats; // 新增用户行为
}

// 分析参数接口
export interface AnalyzeParams {
  logPath?: string;
}
