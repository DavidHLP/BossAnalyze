import request from '@/utils/request/request';

/**
 * 获取所有职位数据
 */
export const getAllJobs = () => {
  return request({
    url: '/api/boss/jobs',
    method: 'get',
  });
};

/**
 * 根据职位名称获取数据
 */
export const getJobsByPosition = (position: string) => {
  return request({
    url: '/api/boss/jobs/position',
    method: 'get',
    params: { position }
  });
};

/**
 * 根据职位和时间获取数据
 */
export const getJobsByPositionAndTime = (position: string, timePeriod: string) => {
  return request({
    url: '/api/boss/jobs/position/time',
    method: 'get',
    params: { position, timePeriod }
  });
};

/**
 * 获取所有职位名称
 */
export const getAllPositions = (): Promise<string[]> => {
  return request({
    url: '/api/boss/positions',
    method: 'get',
  });
};

/**
 * 获取所有时间段
 */
export const getAllTimePeriods = (): Promise<string[]> => {
  return request({
    url: '/api/boss/timePeriods',
    method: 'get',
  });
};

/**
 * 获取所有城市
 */
export const getAllCities = (): Promise<string[]> => {
  return request({
    url: '/api/boss/cities',
    method: 'get',
  });
};

