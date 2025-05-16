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
