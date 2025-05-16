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