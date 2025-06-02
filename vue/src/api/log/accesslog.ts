import request from '@/utils/request/request';
import type { AccessLogStats } from './accesslog.d';

/**
 * 获取所有访问日志统计数据
 */
export async function getAccessLogStats(): Promise<AccessLogStats> {
  try {
    const response = await request<Promise<AccessLogStats>>({
      url: '/api/v1/system/logs/stats',
      method: 'get'
    });

    // 解构响应数据
    const responseData = response as unknown as AccessLogStats;

    if (responseData ) {
      // 确保返回的数据符合 AccessLogStats 接口
      const statsData = responseData || {} as AccessLogStats;

      // 计算派生指标
      const totalVisits = calculateTotalVisits(statsData.httpMethods);
      const uniqueIps = Object.keys(statsData.ipStats || {}).length;

      // 设置默认值，确保所有必需属性都有值
      return {
        heatmap: statsData.heatmap || { minute_heatmap: {}, hour_heatmap: {} },
        ipStats: statsData.ipStats || {},
        httpMethods: statsData.httpMethods || {},
        weekdayStats: statsData.weekdayStats || {},
        browserStats: statsData.browserStats || {},
        // 计算得到的指标
        totalVisits: totalVisits,
        uniqueIps: uniqueIps,
        avgResponseTime: 0, // 后端暂未提供此数据
        errorRate: 0 // 后端暂未提供此数据
      };
    } else {
      throw new Error('获取访问日志统计失败');
    }
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
 * 计算总访问量
 */
function calculateTotalVisits(httpMethods: Record<string, number> = {}): number {
  return Object.values(httpMethods).reduce((sum, count) => sum + count, 0);
}
