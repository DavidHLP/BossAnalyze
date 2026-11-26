import request from '@/utils/request/request';
import { degradedHealthResponse } from '@/lib/settings'
import type { DashboardStats } from './accesslog.d';

/**
 * 获取仪表板统计数据（新接口）
 */
export async function getDashboardStats(): Promise<DashboardStats | null> {
  try {
    const snapshot = await request<DashboardStats, DashboardStats>({
      url: '/api/v1/system/logs/dashboard/stats',
      method: 'get'
    });
    if (snapshot == null) throw new Error('Invalid dashboard response');
    return snapshot;
  } catch (error) {
    // The shared client rejects non-200 responses; only 204 means no snapshot.
    if (error && typeof error === 'object' && 'code' in error && error.code === 204) return null;
    throw error;
  }
}

/**
 * 获取热力图数据
 */
export function getHeatMapData() {
  return request.get<unknown, unknown>('/api/v1/system/logs/heatmap')
}

/**
 * 获取IP统计数据
 */
export function getIpStatistics() {
  return request.get<unknown, unknown>('/api/v1/system/logs/ip-statistics')
}

export function getCompleteLogStats() {
  return request.get<unknown, unknown>('/api/v1/system/logs/stats')
}

/**
 * 手动触发日志分析
 */
export async function triggerLogAnalysis() {
  const receipt = await request.post<unknown, unknown>('/api/v1/system/logs/analysis/trigger')
  if (typeof receipt !== 'string' || !receipt.trim()) throw new Error('Missing log analysis receipt')
}

/**
 * 检查服务健康状态
 */
export async function checkServiceHealth(): Promise<unknown> {
  try {
    return await request<unknown, unknown>({
      url: '/api/v1/system/logs/health',
      method: 'get',
    })
  } catch (error) {
    const degraded = degradedHealthResponse(error)
    if (degraded) return degraded
    throw error
  }
}
