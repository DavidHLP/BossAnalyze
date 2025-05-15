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

/**
 * 创建空的分页对象
 *
 * @param params 分页参数
 * @returns 空分页对象
 */
export function createEmptyPageInfo<T>(params?: {
  size?: number
  number?: number
}): PageInfo<T> {
  return {
    content: [],
    number: params?.number || 0,
    size: params?.size || 10,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
    first: true,
    last: true
  }
}

/**
 * 将旧版分页结构转换为新版分页结构
 *
 * @param oldPageInfo 旧版分页数据
 * @returns 新版分页数据
 */
export function convertLegacyPageInfo<T>(oldPageInfo: {
  items: Array<T>
  pageNum: number
  pageSize: number
  total: number
  pages: number
}): PageInfo<T> {
  return {
    content: oldPageInfo.items,
    number: oldPageInfo.pageNum - 1, // 旧版从1开始，新版从0开始
    size: oldPageInfo.pageSize,
    totalElements: oldPageInfo.total,
    totalPages: oldPageInfo.pages,
    hasNext: oldPageInfo.pageNum < oldPageInfo.pages,
    hasPrevious: oldPageInfo.pageNum > 1,
    first: oldPageInfo.pageNum === 1,
    last: oldPageInfo.pageNum === oldPageInfo.pages
  }
}

export interface Request<T> {
  code: number
  data: T
  message: string
  timestamp: number
  [property: string]: unknown
}
