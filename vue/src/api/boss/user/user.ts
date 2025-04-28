import request from '@/utils/request/request';
import type { HotCity } from '@/api/boss/user/user.d';

export const getHotCities = (limit: number): Promise<HotCity[]> =>
  request.get('/api/boss/spark/user/hot-cities', { params: { limit } });
