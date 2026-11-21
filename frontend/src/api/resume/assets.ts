import request from '@/utils/request/request'

interface ImageResult {
  success: boolean
  fileName?: string
}

export function ownsResumeAsset(userId: number, fileName: string) {
  return Number.isSafeInteger(userId) && userId > 0 && fileName.startsWith(userId + '_') &&
    !/[\/\\\u0000-\u001f\u007f]/.test(fileName)
}

function assetName(userId: number, fileName: string) {
  if (!ownsResumeAsset(userId, fileName)) throw new Error('Invalid asset name')
  return encodeURIComponent(fileName)
}

export async function uploadResumeAsset(userId: number, file: File) {
  if (!file.type.startsWith('image/') || !file.size || file.size > 50 * 1024 * 1024 ||
      !ownsResumeAsset(userId, userId + '_' + file.name)) throw new Error('Invalid image')
  const body = new FormData()
  body.append('file', file)
  const response = await request.post<ImageResult, ImageResult>('/api/image/upload', body, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  if (!response?.success || !response.fileName || !ownsResumeAsset(userId, response.fileName)) {
    throw new Error('Upload not confirmed')
  }
  return response.fileName
}

export async function readResumeAsset(userId: number, fileName: string) {
  const name = assetName(userId, fileName)
  if (fileName.toLowerCase().endsWith('.svg')) return null
  const image = await request.get<Blob, Blob>('/api/image/view/' + name, { responseType: 'blob' })
  if (!(image instanceof Blob) || !/^image\/(png|jpeg|webp|gif|bmp|avif|x-icon)$/i.test(image.type)) {
    throw new Error('Invalid image response')
  }
  return image
}

export async function deleteResumeAsset(userId: number, fileName: string) {
  assetName(userId, fileName)
  const response = await request.delete<ImageResult, ImageResult>('/api/image/delete', { params: { fileName } })
  if (!response?.success) throw new Error('Deletion not confirmed')
}

export async function resumeAssetAvailable(userId: number, fileName: string) {
  const name = assetName(userId, fileName)
  const response = await request.get<ImageResult, ImageResult>('/api/image/url/' + name)
  return response?.success === true && response.fileName === fileName
}
