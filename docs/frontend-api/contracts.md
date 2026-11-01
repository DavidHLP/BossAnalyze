# Type and transport snapshots

Frozen from `832bfde`, 2026-10-30. These declarations describe the existing client; they are **not validated schemas**. In particular, optional fields, `any`, index signatures, JavaScript Date annotations, and response mismatches must not be mistaken for stronger backend guarantees. Imports below retain their original paths for provenance; a replacement frontend must rehome those types. Everything needed to interpret their definitions is included here.

## Client declarations

### `vue/src/api/auth/auth.d.ts`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/vue/src/api/auth/auth.d.ts).

```ts
import type { Router } from '@/router/index.d'
export interface Token {
  expired?: boolean
  id?: number
  revoked?: boolean
  token?: string
  tokenType?: string
  userId?: number
}

export interface Permissions {
  [index: number]: string
}
export interface Role {
  createTime?: string
  id?: number
  remark?: string
  roleName?: string
  status?: number
  updateTime?: string
  userId?: number
  permissions?: Permissions[]
  routers?: Router[]
  [property: string]: any
}

export interface UserBaseInfo {
  address?: string
  avatar?: string
  createTime?: string
  email?: string
  id?: number
  introduction?: string
  lastLogin?: string
  lastLoginIp?: string
  name?: string
  roleId?: number
  status?: number | string
  userId?: number
  username?: string
  roleName?: string
  [property: string]: any
}
```

### `vue/src/api/boss/user/user.d.ts`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/vue/src/api/boss/user/user.d.ts).

```ts
export interface SalaryJob {
  positionName: string;
  jobCount: number;
  minSalary: number;
  maxSalary: number;
  avgSalary: number;
  recommendedCity: string;
  recommendedCitySalary: number;
  recommendedCityJobCount: number;
}

export interface JobData {
  positionId: string;
  positionName: string;
  cityName: string;
  salary: string;
  salaryValue: number;
  degree: string;
  degreeValue: number;
  experience: string;
  experienceValue: number;
  companyName: string;
  companySize: string;
  industry: string;
  financingStage: string;
  companyUrl: string;
  jobUrl: string;
  address: string;
  employeeBenefits: string[];
  jobRequirements: string[];
  [key: string]: string | number | string[] | number[]; // 添加索引签名以允许使用字符串索引
}

export interface CompanyInfo {
  companyName: string;
  positionName: string;
  cityName: string;
  salary: string;
  degree: string;
  experience: string;
  companySize: string;
  financingStage: string;
  companyUrl?: string;
  jobUrl?: string;
  address?: string;
  employeeBenefits?: string[];
  jobRequirements?: string[];
}
```

### `vue/src/api/log/accesslog.d.ts`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/vue/src/api/log/accesslog.d.ts).

```ts
// 仪表板概览数据
export interface DashboardOverview {
  totalVisits: number;
  uniqueVisitors: number;
  avgSessionDuration: number;
  bounceRate: number;
  errorRate: number;
  visitsTrend?: TrendInfo;
  visitorsTrend?: TrendInfo;
}

// 趋势信息
export interface TrendInfo {
  changePercent: number;
  direction: 'up' | 'down' | 'stable';
  period: 'day' | 'week' | 'month';
}

// 实时统计
export interface RealTimeStats {
  onlineUsers: number;
  activePages: number;
  recentVisits: number;
  recentVisitList?: RealtimeVisit[];
}

// 实时访问记录
export interface RealtimeVisit {
  ip: string;
  path: string;
  method: string;
  timestamp: string;
  userAgent: string;
  location: string;
  responseTime: number;
}

// 热力图数据
export interface HeatMapData {
  minuteHeatmap: Record<string, TimePointInfo>;
  hourHeatmap: Record<string, TimePointInfo>;
}

export interface TimePointInfo {
  count: number;
  intensity: string;
}

// 热门页面统计
export interface TopPagesStats {
  pages: PageInfo[];
}

export interface PageInfo {
  path: string;
  visits: number;
  uniqueVisitors: number;
  avgTime: number;
  bounceRate: number;
  percentage: number;
  trend?: TrendInfo;
}

// 地理位置统计
export interface GeoStats {
  countryStats: Record<string, CountryInfo>;
  cityStats: Record<string, CityInfo>;
  geoPoints: GeoPoint[];
}

export interface CountryInfo {
  visits: number;
  uniqueVisitors: number;
  percentage: number;
  countryCode: string;
  trend?: TrendInfo;
}

export interface CityInfo {
  visits: number;
  latitude: number;
  longitude: number;
  country: string;
  region: string;
}

export interface GeoPoint {
  latitude: number;
  longitude: number;
  value: number;
  location: string;
}

// 设备统计
export interface DeviceStats {
  browsers: Record<string, BrowserInfo>;
  operatingSystems: Record<string, number>;
  devices: Record<string, number>;
  screenResolutions?: Record<string, number>;
}

export interface BrowserInfo {
  count: number;
  percentage: number;
  version: string;
  marketRank: number;
  isModern: boolean;
}

// 性能统计
export interface PerformanceStats {
  avgResponseTime: number;
  avgPageLoadTime: number;
  statusCodes: Record<string, number>;
  endpointPerformance?: Record<string, number>;
  slowRequests?: SlowRequest[];
}

export interface SlowRequest {
  path: string;
  method: string;
  responseTime: number;
  timestamp: string;
  ip: string;
}

// 安全统计
export interface SecurityStats {
  suspiciousIPs: number;
  attackAttempts: number;
  threatTypes: Record<string, number>;
  recentEvents?: SecurityEvent[];
}

export interface SecurityEvent {
  type: string;
  ip: string;
  description: string;
  timestamp: string;
  severity: string;
}

// 数据质量信息
export interface DataQualityInfo {
  totalLogLines: number;
  parsedLines: number;
  failedLines: number;
  parseSuccessRate: number;
  qualityScore: number;
}

// 完整的仪表板数据
export interface DashboardStats {
  generatedAt: string;
  timeRange: string;
  overview: DashboardOverview;
  realTimeStats: RealTimeStats;
  heatMapData: HeatMapData;
  topPages: TopPagesStats;
  geoStats: GeoStats;
  deviceStats: DeviceStats;
  performanceStats: PerformanceStats;
  securityStats: SecurityStats;
  dataQuality: DataQualityInfo;
}

// 访问日志统计数据
export interface AccessLogStats {
  heatmap: {
    minute_heatmap: Record<string, { count: number; intensity: string }>;
    hour_heatmap: Record<string, { count: number; intensity: string }>;
  };
  ipStats: Record<
    string,
    {
      count: number;
      country_name?: string;
      city?: string;
    }
  >;
  httpMethods: Record<
    string,
    {
      count: number;
      percentage: number;
      description: string;
      isSafe: boolean;
      isIdempotent: boolean;
    }
  >;
  weekdayStats: Record<
    string,
    {
      count: number;
      percentage: number;
      dayName: string;
      isWorkday: boolean;
      isWeekend: boolean;
      avgHourlyVisits: number;
    }
  >;
  browserStats: Record<
    string,
    {
      count: number;
      percentage: number;
      version: string;
      osDistribution: object;
      deviceTypeDistribution: object;
      marketRank: number;
      isModern: boolean;
    }
  >;
  totalVisits: number;
  uniqueIps: number;
  avgResponseTime: number;
  errorRate: number;
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
```

### `vue/src/api/minio/minio.d.ts`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/vue/src/api/minio/minio.d.ts).

```ts
// 图片响应接口
export interface ImageResponse {
  success: boolean
  fileName?: string
  url: string
  message?: string
}

// 通用响应接口
export interface Result<T> {
  code: number
  message: string
  data: T
}

// API函数类型定义
export function uploadImage(file: File): Promise<Result<ImageResponse>>
export function getImageUrl(fileName: string): Promise<Result<ImageResponse>>
export function deleteImage(fileName: string): Promise<Result<ImageResponse>>
export function getImage(fileName: string): Promise<Blob>
```

### `vue/src/api/resume/types.ts`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/vue/src/api/resume/types.ts).

```ts
// 简历数据类型定义
export interface Education {
  startDate: string;
  endDate: string;
  school: string;
  major: string;
  gpa: string;
  courses: string;
}

export interface WorkExperience {
  startDate: string;
  endDate: string;
  company: string;
  position: string;
  duties: string[];
}

export interface ResumeData {
  id?: string; // 简历ID，可选字段
  userId?: string; // 用户ID，可选字段
  name: string;
  age: string;
  gender: string;
  location: string[];
  experience: string;
  phone: string;
  email: string;
  avatar: string;
  jobTarget: string;
  expectedSalary: string;
  targetCity: string[];
  availableTime: string;
  education: Education[];
  workExperience: WorkExperience[];
  certificates: { name: string; date: string; description: string }[];
  interestTags: string[];
  selfEvaluation: string;
  customSkills: { name: string; description: string }[];
  sectionOrder?: string[]; // 添加用于组件排序的字段
  createdAt?: Date; // 创建时间
  updatedAt?: Date; // 更新时间
}

/**
 * 简历基础模型 (包含版本控制)
 */
export interface Resume {
  id: string
  userId: number
  title: string
  content: string
  createdAt: string
  updatedAt: string
  // 版本控制相关字段
  currentBranch: string
  headCommit?: string
  repositoryId?: string
  hasVersionControl: boolean
  branches: Record<string, string>
  stashStack: Array<Record<string, unknown>>
}

/**
 * 简历创建数据传输对象
 */
export type ResumeCreate = Omit<Resume, 'id' | 'userId' | 'createdAt' | 'updatedAt'>

/**
 * 简历更新数据传输对象
 */
export type ResumeUpdate = Partial<ResumeCreate>
```

### `vue/src/router/index.d.ts`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/vue/src/router/index.d.ts).

```ts
export interface BackendRoute {
  component: string
  id: number
  menuName: string
  menuType: string
  orderNum: number
  path: string
  permission: string
  pid: number
  remark: string
  status: number
  requiresAuth?: boolean
  permissions?: string[]
  children?: BackendRoute[]
}

// 新架构接口
import type { Permissions } from '@/api/auth/auth.d'
export interface Meta {
  permission?: Permissions
  alwaysShow: boolean
  component: string
  hidden: boolean
  metaHidden: boolean
  metaIcon: null
  metaKeepAlive: boolean
  metaRoles: null
  metaTitle: string
  redirect: null
  type: string | 'M' | 'C' | 'F'
}

export interface Router {
  children: Router[]
  icon: string
  id: number
  menuOrder: number
  meta: Meta
  name: string
  path: string
  permission: Permissions
  pid: null
  remark: string
  status: number
  redirect: null
}
```

### `vue/src/types/common.d.ts`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/vue/src/types/common.d.ts).

```ts
/**
 * 分页信息包装类，与后端分页组件兼容
 *
 * @param T 数据对象泛型
 */
export interface PageInfo<T> {
  /**
   * 分页数据列表
   */
  content: Array<T>

  /**
   * 查询条件对象
   */
  query?: T

  /**
   * 当前页码，从0开始
   */
  number: number

  /**
   * 每页显示条数
   */
  size: number

  /**
   * 总记录数
   */
  totalElements: number

  /**
   * 总页数
   */
  totalPages: number

  /**
   * 是否有下一页
   */
  hasNext: boolean

  /**
   * 是否有上一页
   */
  hasPrevious: boolean

  /**
   * 是否为第一页
   */
  first: boolean

  /**
   * 是否为最后一页
   */
  last: boolean
}

export interface Request<T> {
  code: number
  data: T
  message: string
  timestamp: number
  [property: string]: unknown
}
```

### `vue/src/utils/request/request.d.ts`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/vue/src/utils/request/request.d.ts).

```ts
declare module '@/utils/request' {
  interface RequestConfig {
    url: string;
    method: string;
    data?: Record<string, unknown>;
    // 其他配置项可以根据需要添加
  }

  export default function request(config: RequestConfig): Promise<unknown>;
}

// 通用响应接口
export interface Request<T> {
  code: number
  message: string
  data: T
}
```

### `ResumeCommit`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/vue/src/api/resume/version.ts).

```ts
export interface ResumeCommit {
  id: string
  resumeId: string
  userId: number
  message: string
  content: string
  branch: string
  parentCommits: string[]
  commitType: string
  author?: string
  authorEmail?: string
  commitTime: Date
  treeHash: string
  metadata?: Record<string, unknown>
}
```

### `IResumeConfig`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/vue/src/api/modules/resume.ts).

```ts
export interface IResumeConfig {
  content: string
  style: string
  link: string
  name: string
  type?: number
}
```

## Shared request implementation

The default export is an Axios instance, not a function typed solely by request.d.ts. The ambient declaration targets `@/utils/request`, while implementations import `@/utils/request/request`. Retain Axios, or replace this transport and every import deliberately. The old client does not convert JSON dates, encode interpolated path segments, inspect business codes on HTTP 200, or implement refresh-token/logout requests.

```ts
import axios from 'axios'
import type { Request } from '@/utils/request/request.d'

// 创建一个 axios 实例
const service = axios.create({
  baseURL: '/',
  timeout: 5000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 排除无需鉴权的接口（登录、注册、发送注册验证码等）
    const publicPaths = ['/api/auth/login', '/api/auth/register', '/api/auth/sendRegisterEmail'];
    const isPublicPath = publicPaths.some(path => config.url?.endsWith(path));

    const token = localStorage.getItem('token');

    // 如果不是公开路径且没有token，直接拒绝请求
    if (!isPublicPath && !token) {
      window.location.href = '/login';
      return Promise.reject('No token available');
    }

    // 添加token到请求头
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    console.error(error)
    return Promise.reject(error)
  },
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    if (response.status === 200) {
      return response.data?.data ?? (response.data as Request<unknown>)
    } else {
      return Promise.reject({
        code: response.status,
        message: response.data?.message || '请求失败',
        data: response.data,
      })
    }
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      // 触发全局登出逻辑
      window.dispatchEvent(new CustomEvent('unauthorized'))
    }
    return Promise.reject({
      code: error.response?.status || 500,
      message: error.response?.data?.message || error.message,
      data: error.response?.data,
    })
  },
)
export default service
```

## Statistics adapters and fallback behavior

These adapters are business data transformations, not presentation code. Both the raw dashboard declaration and the legacy AccessLogStats shape are retained above. The fallback object and mappings below are the exact existing behavior; rebuilding the UI may choose better error propagation, but this inventory does not silently change it.

```ts
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
```

## External fetch behavior

Environment **names only** are retained. Actual external service URLs/tokens must come from private environment configuration. No secrets are archived.

```ts
export interface IResumeConfig {
  content: string
  style: string
  link: string
  name: string
  type?: number
}

const UPSTASH_BASE_URL = import.meta.env.VITE_UPSTASH_BASE_URL as string

export async function resumeExport(data: IResumeConfig) {
  const res = await fetch(import.meta.env.VITE_EXPORT_URL as string, {
    method: 'POST',
    body: JSON.stringify(data),
    headers: {
      'Content-Type': 'application/json',
    },
  })
  return await res.json()
}

export function getExportCount() {
  return new Promise((resolve) => {
    // 检查环境变量是否配置
    const baseUrl = UPSTASH_BASE_URL
    const token = import.meta.env.VITE_UPSTASH_GET_TOKEN as string

    if (!baseUrl || !token) {
      console.warn('导出统计API：环境变量未配置，返回默认值0')
      resolve(0)
      return
    }

    fetch(`${baseUrl}/get/count`, {
      headers: {
        Authorization: token,
      },
    })
      .then(async (response) => {
        try {
          // 检查响应状态
          if (!response.ok) {
            console.warn(`导出统计API：HTTP ${response.status} ${response.statusText}`)
            resolve(0)
            return
          }

          // 检查响应的Content-Type
          const contentType = response.headers.get('content-type')
          if (contentType && contentType.includes('text/html')) {
            console.warn('导出统计API：返回HTML页面，可能是API端点错误')
            resolve(0)
            return
          }

          // 获取响应文本
          const text = await response.text()

          // 检查是否为空响应
          if (!text || text.trim() === '') {
            console.warn('导出统计API：返回空响应')
            resolve(0)
            return
          }

          // 尝试解析为JSON
          try {
            const data = JSON.parse(text)
            if (data && typeof data.result !== 'undefined') {
              resolve(data.result)
            } else {
              console.warn('导出统计API：JSON格式不正确，缺少result字段')
              resolve(0)
            }
          } catch (jsonError) {
            // JSON解析失败，尝试直接解析为数字
            const numericValue = parseInt(text, 10)
            if (!isNaN(numericValue)) {
              resolve(numericValue)
            } else {
              console.warn('导出统计API：无法解析返回数据，返回默认值0')
              resolve(0)
            }
          }
        } catch (error) {
          console.warn('导出统计API：处理响应时出错，返回默认值0:', error)
          resolve(0)
        }
      })
      .catch((error) => {
        console.warn('导出统计API：网络请求失败，返回默认值0:', error)
        resolve(0)
      })
  })
}
```

## Backend field declarations

The following snapshots preserve Java model fields and relevant serialization annotations. They do not assert that every field is populated. `@Field` is a MongoDB storage mapping, not a JSON rename: the HTTP field is camelCase (for example `docType`, not `doc_type`). Java dates serialize as JSON values; the client does not instantiate Date objects. Long IDs are currently consumed as JavaScript numbers and can exceed safe integer precision. Java entity `User` contains a password field; do not expose or log whole entity objects in a replacement UI.

### `Resume`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/entity/Resume.java).

```java
package com.david.hlp.web.resume.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历实体类，用于在同一集合中存储简历元数据（meta）和提交历史（commit）
 * 通过 'doc_type' 字段区分
 */
@Data
@Document(collection = "resumes")
public class Resume implements Serializable {

    @Id
    private String id;

    @Field("user_id")
    private Long userId;

    @Field("content")
    private String content; // 对于 "meta" 是当前内容, "commit" 是快照

    @Field("doc_type")
    private String docType; // "meta" or "commit"

    // "meta" type fields
    @Field("title")
    private String title;

    @CreatedDate
    @Field("created_at")
    private Date createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Date updatedAt;

    @Field("current_branch")
    private String currentBranch = "main";

    @Field("head_commit")
    private String headCommit;

    @Field("repository_id")
    private String repositoryId;

    @Field("branches")
    private Map<String, String> branches = new HashMap<>(); // branch -> commitId

    @Field("stash_stack")
    private List<Map<String, Object>> stashStack = new ArrayList<>();

    // "commit" type fields
    @Field("resume_id")
    private String resumeId; // 关联的简历ID

    @Field("message")
    private String message; // 提交信息

    @Field("branch")
    private String branch; // 所属分支

    @Field("parent_commits")
    private List<String> parentCommits = new ArrayList<>(); // 父提交ID列表（支持merge）

    @Field("commit_type")
    private String commitType = "normal"; // normal, merge, cherry-pick

    @Field("author")
    private String author; // 提交者

    @Field("author_email")
    private String authorEmail;

    @Field("commit_time")
    private Date commitTime;

    @Field("tree_hash")
    private String treeHash; // 内容哈希，用于快速比较

    @Field("metadata")
    private Map<String, Object> metadata = new HashMap<>(); // 额外元数据
}
```

### `User`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/entity/user/User.java).

```java
package com.david.hlp.web.system.auth.entity.user;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 用户实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    /** 主键ID */
    private Long id;             // BIGINT 主键

    /** 用户名 */
    private String name;         // 用户名

    /** 头像URL */
    private String avatar;       // 头像

    /** 用户简介 */
    @Default
    private String introduction = "用户未填写";      // 简介

    /** 邮箱地址（唯一） */
    private String email;        // 邮箱（唯一）

    /** 用户状态 */
    private Integer status;      // 用户状态

    /** 用户地址 */
    @Default
    private String address = "用户未填写";      // 地址

    /** 上次登录IP */
    private String lastLoginIp;  // 上次登录 IP

    /** 上次登录时间 */
    private LocalDateTime lastLogin;   // 上次登录时间

    /** 角色ID */
    private Long roleId;         // 角色 ID

    /** 角色名称 */
    private String roleName;     // 角色名称

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createTime;  // 创建时间

    /** 加密后的密码 */
    private String password;     // 加密后的密码

    /**
     * 获取用户ID
     * @return 用户ID
     */
    public Long getUserId() {
        return id;
    }

    /**
     * 获取用户名
     * @return 用户名
     */
    public String getUsername() {
        return name;
    }
}
```

### `Token`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/token/Token.java).

```java
package com.david.hlp.web.system.auth.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import com.david.hlp.web.system.auth.entity.auth.AuthUser;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Token implements Serializable {
    private Long id;
    private AuthUser authUser;
    private Long userId;
    private String token;
    private TokenType tokenType;
    private boolean expired;
    private boolean revoked;
}
```

### `AuthUser`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/entity/auth/AuthUser.java).

```java
package com.david.hlp.web.system.auth.entity.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.AllArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Spring Security 用的用户类，实现 UserDetails 接口。
 */
@Builder
@AllArgsConstructor
public class AuthUser implements UserDetails {

    private final Long userId;               // 数据库中的用户主键ID
    private final String username;           // 登录名（在此示例中使用 email 或其他字段）
    private final String email;              // 邮箱
    private final String password;           // 加密后的密码
    private final Integer status;            // 用户状态：0-禁用，1-启用
    private final Long roleId;               // 角色ID
    private List<String> authorities;

    public AuthUser(
            Long userId,
            String username,
            String password,
            Integer status,
            Long roleId,
            String email
    ) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.status = status;
        this.roleId = roleId;
        this.email = email;
    }

    // ========== 公共方法 ==========
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Objects.nonNull(status) && status == 1;
    }

    // ========== getter/setter方法 ==========

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public String getEmail() {
        return email;
    }

    public Integer getStatus() {
        return status;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
    }

    @Override
    public String toString() {
        return "AuthUser{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", roleId=" + roleId +
                ", authorities=" + authorities +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthUser)) {
            return false;
        }
        AuthUser authUser = (AuthUser) o;
        return Objects.equals(userId, authUser.userId) &&
                Objects.equals(email, authUser.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email);
    }
}
```

### `LoginDTO`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/entity/auth/LoginDTO.java).

```java
package com.david.hlp.web.system.auth.entity.auth;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {
    private String email;
    private String password;
}
```

### `RegistrationDTO`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/entity/auth/RegistrationDTO.java).

```java
package com.david.hlp.web.system.auth.entity.auth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationDTO {

    private String name;

    private String email;

    private String password;

    private String code;
}
```

### `Role`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/entity/role/Role.java).

```java
package com.david.hlp.web.system.auth.entity.role;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.david.hlp.web.system.auth.entity.permission.Permission;
import com.david.hlp.web.system.auth.entity.router.Router;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
/**
 * 对应数据库中的 role 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;         // BIGINT 主键
    private Long userId;     // BIGINT 用户ID
    private String roleName; // 角色名称
    private Integer status;  // 状态
    private String remark;   // 备注信息
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private List<Permission> permissions; // 权限列表
    private List<Router> routers; // 路由列表
}
```

### `RolePermissionUpdateResponse`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/entity/role/RolePermissionUpdateResponse.java).

```java
package com.david.hlp.web.system.auth.entity.role;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * 角色权限更新响应实体类
 *
 * @author david
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RolePermissionUpdateResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long roleId;
    private List<Integer> routerIds;
}
```

### `Permission`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/entity/permission/Permission.java).

```java
package com.david.hlp.web.system.auth.entity.permission;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限实体类
 *
 * @author david
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Permission implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;            // 对应 BIGINT 主键

    /**
     * 权限状态：0-禁用，1-启用
     */
    private Boolean enabled;

    /**
     * 权限备注
     */
    private String remark;      // 备注信息

    /**
     * 权限标识符
     */
    private String permission;  // 权限标识

    /**
     * 创建时间
     */
    private LocalDateTime createTime; // 创建时间

    /**
     * 更新时间
     */
    private LocalDateTime updateTime; // 更新时间
}
```

### `Router`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/entity/router/Router.java).

```java
package com.david.hlp.web.system.auth.entity.router;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;
import lombok.ToString;
import java.io.Serializable;

import java.util.List;
import java.util.ArrayList;

/**
 * 路由实体类
 *
 * @author david
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Router implements Serializable {

    private static final long serialVersionUID = 2L;

    /**
     * 主键ID
     */
    private Long id;            // BIGINT 主键

    /**
     * 父路由ID
     */
    private Long pid;           // 父路由 ID

    /**
     * 菜单顺序
     */
    private Integer menuOrder;  // 菜单顺序

    /**
     * 状态
     */
    private Integer status;     // 状态字段

    /**
     * 备注信息
     */
    private String remark;      // 备注信息

    /**
     * 关联的权限标识
     */
    private String permission;  // 关联的权限标识

    /**
     * 路由路径
     */
    private String path;        // 路由路径

    /**
     * 路由名称
     */
    private String name;        // 路由名称

    /**
     * 路由元数据
     */
    private Meta meta;

    /**
     * 子路由列表
     */
    @Default
    private List<Router> children = new ArrayList<>(); // 子路由
    // 如需记录创建/更新时间，可在表结构或实体里继续添加
    // private LocalDateTime createTime;
    // private LocalDateTime updateTime;
}
```

### `Meta`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/entity/router/Meta.java).

```java
package com.david.hlp.web.system.auth.entity.router;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import java.io.Serializable;

/**
 * 路由元数据实体类
 *
 * @author david
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Meta implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 类型
     */
    private String type;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 重定向路径
     */
    private String redirect;

    /**
     * 是否总是显示
     */
    private Boolean alwaysShow;

    /**
     * 元标题
     */
    private String metaTitle;

    /**
     * 元图标
     */
    private String metaIcon;

    /**
     * 是否在菜单中隐藏
     */
    private Boolean metaHidden;

    /**
     * 存储角色名称数组的 JSON
     */
    private String metaRoles;

    /**
     * 是否缓存页面
     */
    private Boolean metaKeepAlive;

    /**
     * 是否整体隐藏
     */
    private Boolean hidden;
}
```

### `TokenType`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/token/TokenType.java).

```java
package com.david.hlp.web.system.auth.token;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

public enum TokenType implements IEnum<String> {
    BEARER("BEARER"),
    ACCESS("ACCESS"),
    REFRESH("REFRESH"),
    PASS("PASS");
    @EnumValue
    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
```

### `JobAnalysisData`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spark/src/main/java/com/david/hlp/spark/model/User/JobAnalysisData.java).

```java
package com.david.hlp.spark.model.User;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 职位分析数据对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobAnalysisData {
    private String positionId;
    private String positionName;
    private String cityName;
    private String salary;
    private Integer salaryValue;
    private String degree;
    private Integer degreeValue;
    private String experience;
    private Integer experienceValue;
    private String companyName;
    private String companySize;
    private String financingStage;
    private String companyUrl;
    private String JobUrl;
    private String address;
    private List<String> employeeBenefits;
    private List<String> jobRequirements;
}
```

### `SalaryJob`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/spark/src/main/java/com/david/hlp/spark/model/User/SalaryJob.java).

```java
package com.david.hlp.spark.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalaryJob {
    private String positionName;
    private Long jobCount;
    private Long minSalary;
    private Long maxSalary;
    private Double avgSalary;
    private String recommendedCity;
    private Double recommendedCitySalary;
    private Long recommendedCityJobCount;
}
```

### `AccessLogAnalysisResult`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/commons/src/main/java/com/david/hlp/commons/entity/logs/AccessLogAnalysisResult.java).

```java
package com.david.hlp.commons.entity.logs;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessLogAnalysisResult {
    private LocalDateTime analysisTime;
    private LocalDateTime dataStartTime;
    private LocalDateTime dataEndTime;
    private long totalVisits;
    private long uniqueVisitors;
    private HeatMapData heatMapData;
    private IpStatistics ipStatistics;
    private HttpMethodStatistics httpMethodStatistics;
    private WeekdayStatistics weekdayStatistics;
    private BrowserStatistics browserStatistics;
    private DataQualityInfo dataQuality;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataQualityInfo {
        private long totalLogLines;
        private long parsedLines;
        private long failedLines;
        private Double parseSuccessRate;
        private Integer qualityScore;
    }
}
```

### `BrowserStatistics`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/commons/src/main/java/com/david/hlp/commons/entity/logs/BrowserStatistics.java).

```java
package com.david.hlp.commons.entity.logs;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserStatistics {
    private Map<String, BrowserInfo> browserStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrowserInfo {
        private Integer count;
        private Double percentage;
        private String version;
        private Map<String, Integer> osDistribution;
        private Map<String, Integer> deviceTypeDistribution;
        private Integer marketRank;
        private Boolean isModern;
    }
}
```

### `HeatMapData`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/commons/src/main/java/com/david/hlp/commons/entity/logs/HeatMapData.java).

```java
package com.david.hlp.commons.entity.logs;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatMapData {
    private Map<String, TimePointInfo> minuteHeatmap;
    private Map<String, TimePointInfo> hourHeatmap;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimePointInfo {
        private Integer count;
        private String intensity;
    }
}
```

### `HttpMethodStatistics`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/commons/src/main/java/com/david/hlp/commons/entity/logs/HttpMethodStatistics.java).

```java
package com.david.hlp.commons.entity.logs;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpMethodStatistics {
    private Map<String, MethodInfo> methodStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodInfo {
        private Integer count;
        private Double percentage;
        private String description;
        private Boolean isSafe;
        private Boolean isIdempotent;
    }
}
```

### `IpStatistics`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/commons/src/main/java/com/david/hlp/commons/entity/logs/IpStatistics.java).

```java
package com.david.hlp.commons.entity.logs;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpStatistics {
    private Map<String, IpInfo> ipStats;
    private List<String> localNetworks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IpInfo {
        private Integer count;
        private String countryName;
        private String city;
        private String region;
        private Double longitude;
        private Double latitude;
        private Boolean isLocal;
        private String isp;
        private String riskLevel;
    }
}
```

### `WeekdayStatistics`

Original: [source](https://github.com/DavidHLP/BossAnalyze/blob/832bfde1871a1b84eb7ef873e3ff698351c1d8e8/JDK17SERVE/commons/src/main/java/com/david/hlp/commons/entity/logs/WeekdayStatistics.java).

```java
package com.david.hlp.commons.entity.logs;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekdayStatistics {
    private Map<String, DayInfo> weekdayStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayInfo {
        private Integer count;
        private Double percentage;
        private String dayName;
        private Boolean isWorkday;
        private Boolean isWeekend;
        private Double avgHourlyVisits;
    }
}
```

## Backend response helpers

`Result<T>` is a ResponseEntity of ResultBody. The source below records success/error overloads and file behavior; do not assume standard REST status semantics or an unwrapped string.

```java
package com.david.hlp.web.common.entity;

import com.david.hlp.web.common.enums.ResultCode;
import lombok.ToString;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString
@Accessors(chain = true)
public class Result<T> extends ResponseEntity<ResultBody<T>> {

    public Result(ResultBody<T> body, HttpStatus status) {
        super(body, status);
    }

    public Result(ResultBody<T> body, HttpHeaders headers, HttpStatus status) {
        super(body, headers, status);
    }

    public static <T> Result<T> success() {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(ResultCode.SUCCESS.getCode())
                        .message(ResultCode.SUCCESS.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.OK);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(ResultCode.SUCCESS.getCode())
                        .message(ResultCode.SUCCESS.getMessage())
                        .data(data)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.OK);
    }

    public static <T> Result<T> success(String message) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(ResultCode.SUCCESS.getCode())
                        .message(message)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.OK);
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(ResultCode.SUCCESS.getCode())
                        .message(message)
                        .data(data)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.OK);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(code)
                        .message(message)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static <T> Result<T> error(ResultCode code) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(code.getCode())
                        .message(code.getMessage())
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static <T> Result<T> error(ResultCode code, String message) {
        return new Result<>(
                ResultBody.<T>builder()
                        .code(code.getCode())
                        .message(message)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 返回文件响应
     *
     * @param inputStream 文件输入流
     * @param contentType 文件类型
     * @param fileName    文件名
     * @param cacheMaxAge 缓存时间（秒）
     * @return 文件响应结果
     */
    public static Result<InputStreamResource> file(InputStreamResource inputStream, String contentType, String fileName,
            long cacheMaxAge) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("inline", "\"" + fileName + "\"");
        headers.setCacheControl("max-age=" + cacheMaxAge);

        return new Result<>(
                ResultBody.<InputStreamResource>builder()
                        .code(ResultCode.SUCCESS.getCode())
                        .message(ResultCode.SUCCESS.getMessage())
                        .data(inputStream)
                        .timestamp(System.currentTimeMillis())
                        .build(),
                headers,
                HttpStatus.OK);
    }

    /**
     * 返回文件响应（默认缓存一年）
     *
     * @param inputStream 文件输入流
     * @param contentType 文件类型
     * @param fileName    文件名
     * @return 文件响应结果
     */
    public static Result<InputStreamResource> file(InputStreamResource inputStream, String contentType,
            String fileName) {
        return file(inputStream, contentType, fileName, 31536000L);
    }
}
```

### Business code dictionary

```java
package com.david.hlp.web.common.enums;

/**
 * 统一返回状态码枚举
 * 包含系统所有的状态码定义
 *
 * @author david
 */
public enum ResultCode {
    /** 成功状态码 */
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),

    /** 客户端错误 4xx */
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法错误"),

    /** 服务端错误 5xx */
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    /** 自定义业务错误 10xx */
    USER_EXISTS(1001, "用户已存在"),
    INVALID_CREDENTIALS(1002, "用户名或密码错误"),
    CAPTCHA_ERROR(1003, "验证码错误"),
    CAPTCHA_EXPIRED(1004, "验证码过期"),
    USER_NOT_FOUND(1005, "用户不存在"),
    PASSWORD_ERROR(1006, "密码错误"),
    PARAM_ERROR(1007, "参数错误"),
    DUPLICATE_ERROR(1008, "数据重复"),
    REQUEST_ERROR(1009, "请求错误"),
    RESUME_NOT_FOUND(1010, "简历不存在"),
    LOCK_HAS_USED(1011, "已经被使用");

    private final Integer code;
    private final String message;

    /**
     * 构造方法
     *
     * @param code    状态码
     * @param message 状态信息
     */
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取状态信息
     *
     * @return 状态信息
     */
    public String getMessage() {
        return message;
    }
}
```
