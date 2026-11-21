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
