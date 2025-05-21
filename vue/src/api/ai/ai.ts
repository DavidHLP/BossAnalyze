import request from '@/utils/request/request'
import type { SimilarityRequest, UserSimilarity } from '@/api/ai/ai.d'

/**
 * 获取用户简历与职位的相似度匹配
 */
export const getUserSimilarity = (data: SimilarityRequest): Promise<UserSimilarity[]> => {
  return request({
    url: '/api/ai/get-user-similarity',
    method: 'POST',
    data
  })
}
