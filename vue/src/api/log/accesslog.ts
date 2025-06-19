import request from '@/utils/request/request';
import type { AccessLogStats } from './accesslog.d';

/**
 * 获取所有访问日志统计数据
 */
export async function getAccessLogStats(): Promise<AccessLogStats> {
  try {
    const response = await request<AccessLogStats>({
      url: '/api/v1/system/logs/stats',
      method: 'get'
    });

    // 后端现在直接返回符合接口格式的数据，无需额外转换
    const statsData = response || {} as AccessLogStats;

    // 设置默认值，确保所有必需属性都有值
    return {
      heatmap: statsData.heatmap || { minute_heatmap: {}, hour_heatmap: {} },
      ipStats: statsData.ipStats || {},
      httpMethods: statsData.httpMethods || {},
      weekdayStats: statsData.weekdayStats || {},
      browserStats: statsData.browserStats || {},
      totalVisits: statsData.totalVisits || 0,
      uniqueIps: statsData.uniqueIps || 0,
      avgResponseTime: statsData.avgResponseTime || 0,
      errorRate: statsData.errorRate || 0
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
 * 计算总访问量（备用函数，现在后端直接提供）
 */
function calculateTotalVisits(httpMethods: Record<string, number> = {}): number {
  return Object.values(httpMethods).reduce((sum, count) => sum + count, 0);
}
