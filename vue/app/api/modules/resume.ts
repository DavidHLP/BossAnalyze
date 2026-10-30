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
