// 访问日志统计数据
export interface AccessLogStats {
  // 热力图数据
  heatmap: {
    minute_heatmap: Record<string, number>;
    hour_heatmap: Record<string, number>;
  };

  // IP统计
  ipStats: {
    [ip: string]: {
      count: number;
      country_name?: string;
      city?: string;
      location?: string;
      lat?: number;
      lng?: number;
    };
  };

  // HTTP方法统计
  httpMethods: {
    [method: string]: number;
  };

  // 星期统计
  weekdayStats: {
    [weekday: string]: number;
  };

  // 浏览器统计
  browserStats: {
    [browser: string]: number;
  };

  // 计算的指标 (前端计算)
  totalVisits?: number;
  uniqueIps?: number;
  avgResponseTime?: number;
  errorRate?: number;
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
