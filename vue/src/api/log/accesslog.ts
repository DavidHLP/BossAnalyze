import request from '@/utils/request/request';
import type { AccessLogSummary, IpStats, UrlStats, TimeStats, AllStats, AnomalyStats, UserBehaviorStats } from './accesslog.d';

/**
 * 获取访问日志分析概要数据
 */
export function getAccessLogSummary(): Promise<AccessLogSummary> {
  return request({
    url: '/api/v1/system/logs/summary',
    method: 'get'
  });
}

/**
 * 获取IP统计数据
 */
export function getIpStats(): Promise<IpStats> {
  return request({
    url: '/api/v1/system/logs/ip-stats',
    method: 'get'
  });
}

/**
 * 获取URL统计数据
 */
export function getUrlStats(): Promise<UrlStats> {
  return request({
    url: '/api/v1/system/logs/url-stats',
    method: 'get'
  });
}

/**
 * 获取时间统计数据
 */
export function getTimeStats(): Promise<TimeStats> {
  return request({
    url: '/api/v1/system/logs/time-stats',
    method: 'get'
  });
}

/**
 * 获取所有统计数据
 */
export function getAllStats(): Promise<AllStats> {
  return request({
    url: '/api/v1/system/logs/all-stats',
    method: 'get'
  });
}

/**
 * 获取异常检测数据
 */
export function getAnomalyStats(): Promise<AnomalyStats> {
  return request({
    url: '/api/v1/system/logs/anomaly-stats',
    method: 'get'
  });
}

/**
 * 获取用户行为分析数据
 */
export function getUserBehaviorStats(): Promise<UserBehaviorStats> {
  return request({
    url: '/api/v1/system/logs/user-behavior',
    method: 'get'
  });
}

/**
 * 获取热力图数据（按日期和小时的访问分布）
 */
export function getHeatmapData(): Promise<Record<string, number>> {
  return request({
    url: '/api/v1/system/logs/heatmap',
    method: 'get'
  });
}
