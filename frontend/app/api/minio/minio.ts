import request from '@/utils/request/request'
import type { ImageResponse } from './minio.d'

// 图片上传接口
export function uploadImage(file: File): Promise<ImageResponse> {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/image/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取图片URL接口
export function getImageUrl(fileName: string): Promise<ImageResponse> {
  return request({
    url: `/api/image/url/${fileName}`,
    method: 'get',
  })
}

// 删除图片接口
export function deleteImage(fileName: string) {
  return request({
    url: '/api/image/delete',
    method: 'delete',
    params: { fileName }
  })
}

// 获取图片接口（直接返回图片流）
export function getImage(fileName: string) {
  return request({
    url: `/api/image/view/${fileName}`,
    method: 'get',
    responseType: 'blob'
  })
}
