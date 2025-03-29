// 分页表格通用返回结构
export interface PageInfo<T> {
  items: Array<T>
  pageNum: number
  pageSize: number
  total: number
  pages: number
}
