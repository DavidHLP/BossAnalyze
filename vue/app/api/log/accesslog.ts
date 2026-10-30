import request from '@/utils/request/request';
import type { AccessLogStats, DashboardStats } from './accesslog.d';

/**
 * 获取仪表板统计数据（新接口）
 */
export async function getDashboardStats(): Promise<DashboardStats> {
  try {
    const response = await request<DashboardStats>({
      url: '/api/v1/system/logs/dashboard/stats',
      method: 'get'
    });

    return (response as any) || getDefaultDashboardStats();
  } catch (error) {
    console.error('获取仪表板统计数据出错:', error);
    return getDefaultDashboardStats();
  }
}

/**
 * 获取所有访问日志统计数据（兼容现有接口）
 */
export async function getAccessLogStats(): Promise<AccessLogStats> {
  try {
    const response = await request<AccessLogStats>({
      url: '/api/v1/system/logs/dashboard/stats',
      method: 'get'
    });

    // 处理响应数据 - 适配新的DashboardStats格式
    const statsData = response as any || {};

    // 设置默认值，确保所有必需属性都有值
    return {
      heatmap: {
        minute_heatmap: statsData.heatMapData?.minuteHeatmap || {},
        hour_heatmap: statsData.heatMapData?.hourHeatmap || {}
      },
      ipStats: statsData.geoStats?.countryStats || {},
      httpMethods: statsData.httpMethodStatistics?.methodStats || {},
      weekdayStats: statsData.weekdayStatistics?.weekdayStats || {},
      browserStats: statsData.deviceStats?.browsers || {},
      totalVisits: statsData.overview?.totalVisits || 0,
      uniqueIps: statsData.overview?.uniqueVisitors || 0,
      avgResponseTime: statsData.performanceStats?.avgResponseTime || 0,
      errorRate: statsData.overview?.errorRate || 0
    };
  } catch (error) {
    console.error('获取访问日志统计出错:', error);
    // 返回一个带有默认值的空数据对象，避免前端报错
    return {
      heatmap: { minute_heatmap: {}, hour_heatmap: {} },
      ipStats: {},
      httpMethods: {},
      weekdayStats: {},
      browserStats: {},
      totalVisits: 0,
      uniqueIps: 0,
      avgResponseTime: 0,
      errorRate: 0
    };
  }
}

/**
 * 获取热力图数据
 */
export async function getHeatMapData(): Promise<any> {
  try {
    const response = await request({
      url: '/api/v1/system/logs/heatmap',
      method: 'get'
    });
    return response || {};
  } catch (error) {
    console.error('获取热力图数据出错:', error);
    return {};
  }
}

/**
 * 获取IP统计数据
 */
export async function getIpStatistics(): Promise<any> {
  try {
    const response = await request({
      url: '/api/v1/system/logs/ip-statistics',
      method: 'get'
    });
    return response || {};
  } catch (error) {
    console.error('获取IP统计数据出错:', error);
    return {};
  }
}

/**
 * 获取HTTP方法统计
 */
export async function getHttpMethodStatistics(): Promise<any> {
  try {
    const response = await request({
      url: '/api/v1/system/logs/http-methods',
      method: 'get'
    });
    return response || {};
  } catch (error) {
    console.error('获取HTTP方法统计出错:', error);
    return {};
  }
}

/**
 * 获取浏览器统计
 */
export async function getBrowserStatistics(): Promise<any> {
  try {
    const response = await request({
      url: '/api/v1/system/logs/browser-usage',
      method: 'get'
    });
    return response || {};
  } catch (error) {
    console.error('获取浏览器统计出错:', error);
    return {};
  }
}

/**
 * 手动触发日志分析
 */
export async function triggerLogAnalysis(): Promise<string> {
  try {
    const response = await request<string>({
      url: '/api/v1/system/logs/analysis/trigger',
      method: 'post'
    });
    return (response as any) || '分析任务已触发';
  } catch (error) {
    console.error('触发日志分析出错:', error);
    return '触发分析任务失败';
  }
}

/**
 * 检查服务健康状态
 */
export async function checkServiceHealth(): Promise<any> {
  try {
    const response = await request({
      url: '/api/v1/system/logs/health',
      method: 'get'
    });
    return response || { status: 'DOWN' };
  } catch (error) {
    console.error('检查服务健康状态出错:', error);
    return { status: 'DOWN', error: (error as Error).message };
  }
}

/**
 * 获取默认仪表板数据
 */
function getDefaultDashboardStats(): DashboardStats {
  return {
    generatedAt: new Date().toISOString(),
    timeRange: '24h',
    overview: {
      totalVisits: 0,
      uniqueVisitors: 0,
      avgSessionDuration: 0,
      bounceRate: 0,
      errorRate: 0
    },
    realTimeStats: {
      onlineUsers: 0,
      activePages: 0,
      recentVisits: 0
    },
    heatMapData: {
      minuteHeatmap: {},
      hourHeatmap: {}
    },
    topPages: {
      pages: []
    },
    geoStats: {
      countryStats: {},
      cityStats: {},
      geoPoints: []
    },
    deviceStats: {
      browsers: {},
      operatingSystems: {},
      devices: {}
    },
    performanceStats: {
      avgResponseTime: 0,
      avgPageLoadTime: 0,
      statusCodes: {}
    },
    securityStats: {
      suspiciousIPs: 0,
      attackAttempts: 0,
      threatTypes: {}
    },
    dataQuality: {
      totalLogLines: 0,
      parsedLines: 0,
      failedLines: 0,
      parseSuccessRate: 0,
      qualityScore: 0
    }
  };
}

/**
 * 计算总访问量（备用函数，现在后端直接提供）
 */
function calculateTotalVisits(httpMethods: Record<string, number> = {}): number {
  return Object.values(httpMethods).reduce((sum, count) => sum + count, 0);
}
