/**
 * 日期时间格式化工具
 */

/**
 * 将日期时间字符串格式化为指定格式
 * @param dateTime 日期时间字符串
 * @param format 格式化模板，默认为 'YYYY-MM-DD HH:mm:ss'
 * @returns 格式化后的日期时间字符串
 */
export function formatDateTime(dateTime: string | Date, format: string = 'YYYY-MM-DD HH:mm:ss'): string {
  if (!dateTime) return '-'

  const date = typeof dateTime === 'string' ? new Date(dateTime) : dateTime

  if (!(date instanceof Date) || isNaN(date.getTime())) {
    return '-'
  }

  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hours = date.getHours()
  const minutes = date.getMinutes()
  const seconds = date.getSeconds()

  const formatMap: Record<string, string | number> = {
    'YYYY': year,
    'MM': padZero(month),
    'DD': padZero(day),
    'HH': padZero(hours),
    'mm': padZero(minutes),
    'ss': padZero(seconds)
  }

  return format.replace(/(YYYY|MM|DD|HH|mm|ss)/g, (match) => {
    return formatMap[match].toString()
  })
}

/**
 * 数字补零
 * @param num 需要补零的数字
 * @returns 补零后的字符串
 */
function padZero(num: number): string {
  return num < 10 ? `0${num}` : `${num}`
}
