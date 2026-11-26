function code(value: unknown) {
  if (value && typeof value === 'object' && 'code' in value && typeof value.code === 'number') {
    return value.code
  }
}

export function mutationBusinessCode(value: unknown) {
  return value && typeof value === 'object' && 'data' in value ? code(value.data) : undefined
}

export function requireMutationSuccess(value: unknown) {
  const status = code(value)
  if (status === 200) return
  // Keep only the status from a rejected business envelope, never its message.
  throw Object.assign(new Error('Management write failed'), {
    code: status,
    resultUnknown: status === undefined,
  })
}

export function mutationResultUnknown(value: unknown) {
  return !!(value && typeof value === 'object' && 'resultUnknown' in value && value.resultUnknown === true)
}

export function permissionDenied(value: unknown) {
  return code(value) === 403 || mutationBusinessCode(value) === 403
}

export function checkReadAccess<T>(value: T): T {
  if (permissionDenied(value)) throw value
  return value
}

export const sessionChangedMessage = '会话已变更。请刷新页面后继续，当前页面的修改无法提交。'
export const unknownWriteMessage = '请求可能已提交，请先刷新列表核对实际状态'

export function mutationError(value: unknown, action: '保存' | '删除', conflict?: string) {
  if (mutationResultUnknown(value)) return unknownWriteMessage
  const status = code(value)
  const business = mutationBusinessCode(value) ?? status
  if (permissionDenied(value)) return `当前账号无权${action}，请联系管理员确认权限。`
  if (status === 404 || business === 404) return '目标已不存在，请重新读取列表。'
  if (status === 409 || business === 409 || business === 1001 || business === 1008 || business === 1011)
    return conflict ?? `数据发生冲突，请核对最新记录后再${action}。`
  if (status === 400 || business === 400 || business === 1007 || business === 1009)
    return `提交参数未通过校验，请检查后重新${action}。`
  if (status !== undefined && status >= 500 && status < 600)
    return `服务暂不可用，请稍后再${action}。`
  return `${action}失败，请核对当前记录或稍后重试。`
}

export function restoreActionFocus(event: Event, trigger: HTMLElement | null, fallback: HTMLElement | null) {
  event.preventDefault()
  void requestAnimationFrame(() => {
    if (trigger?.isConnected && !trigger.matches(':disabled')) trigger.focus()
    else if (fallback?.isConnected) fallback.focus()
  })
}

export function listPage(value: unknown, size: number) {
  const page = typeof value === 'string' && /^\d+$/.test(value) ? Number(value) : 1
  return Number.isSafeInteger(page) && page > 0 && (page - 1) * size < 2 ** 31 - 1 ? page : 1
}

export function listSize(value: unknown) {
  return value === '20' ? 20 : value === '50' ? 50 : 10
}

export function listStatus(value: unknown) {
  return value === '0' || value === '1' ? value : 'all'
}
