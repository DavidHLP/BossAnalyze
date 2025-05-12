import request from '@/utils/request/request';
import type { SalaryJob, JobData } from '@/api/boss/user/user.d';

/**
 * 获取薪资热门职位列表
 * @param limit - 限制返回数量
 * @returns Promise<SalaryJob[]>
 */
export const getSalaryHotJob = (limit: number): Promise<SalaryJob[]> => {
  return request.get('/api/boss/user/salary-hot-job', { params: { limit } });
};

/**
 * 获取二维分析图表数据
 * @param cityName - 城市名称
 * @param positionName - 职位名称
 * @param xAxis - X轴数据
 * @param yAxis - Y轴数据
 * @returns Promise<JobData[]>
 */
export const getTwoDimensionalAnalysisChart = (
  cityName: string,
  positionName: string,
  xAxis: string,
  yAxis: string
): Promise<JobData[]> => {
  return request.get('/api/boss/user/two-dimensional-analysis-chart', {
    params: { cityName, positionName, xAxis, yAxis }
  });
};

/**
 * 获取城市名称列表
 * @returns Promise<string[]>
 */
export const getCityNameList = (): Promise<string[]> => {
  return request.get('/api/boss/basic/city-name-list');
};

/**
 * 获取职位名称列表
 * @param cityName - 城市名称（可选）
 * @returns Promise<string[]>
 */
export const getPositionNameList = (cityName?: string): Promise<string[]> => {
  return request.get('/api/boss/basic/position-name-list', { params: { cityName } });
};

/**
 * 获取公司列表
 * @param cityName - 城市名称
 * @param positionName - 职位名称
 * @returns Promise<JobData[]>
 */
export const getCompanyList = (
  cityName: string,
  positionName: string
): Promise<JobData[]> => {
  return request.get('/api/boss/basic/company-list', { params: { cityName, positionName } });
};

/**
 * 获取三维分析图表数据
 * @param cityName - 城市名称
 * @param positionName - 职位名称
 * @returns Promise<JobData[]>
 */
export const getThreeDimensionalAnalysisChart = (
  cityName: string,
  positionName: string
): Promise<JobData[]> => {
  return request.get('/api/boss/user/three-dimensional-analysis-chart', {
    params: { cityName, positionName }
  });
};