import request from '@/utils/request/request';
import type { SalaryJob, JobData } from '@/api/boss/user/user.d';

export const getSalaryHotJob = (limit: number): Promise<SalaryJob[]> =>
  request.get('/api/boss/user/salary-hot-job', { params: { limit } });

export const getTwoDimensionalAnalysisChart = (cityName: string, positionName: string, xAxis: string, yAxis: string): Promise<JobData[]> =>
  request.get('/api/boss/user/two-dimensional-analysis-chart', { params: { cityName, positionName, xAxis, yAxis } });

export const getCityNameList = (): Promise<string[]> =>
  request.get('/api/boss/basic/city-name-list');

export const getPositionNameList = (): Promise<string[]> =>
  request.get('/api/boss/basic/position-name-list');
