function code(value: unknown) {
  if (value && typeof value === 'object' && 'code' in value && typeof value.code === 'number') {
    return value.code
  }
}

export function mutationBusinessCode(value: unknown) {
  return value && typeof value === 'object' && 'data' in value ? code(value.data) : undefined
}

export function requireMutationSuccess(value: unknown) {
  if (code(value) === 200) return
  // Keep only the status from a rejected business envelope, never its message.
  throw Object.assign(new Error('Management write failed'), { code: code(value) })
}

export function mutationError(value: unknown, action: '保存' | '删除', conflict?: string) {
  const status = code(value)
  const business = mutationBusinessCode(value) ?? status
  if (status === 403 || business === 403) return `当前账号无权${action}，请联系管理员确认权限。`
  if (status === 409 || business === 409 || business === 1001 || business === 1008 || business === 1011)
    return conflict ?? `数据发生冲突，请核对最新记录后再${action}。`
  if (status === 400 || business === 400 || business === 1007 || business === 1009)
    return `提交参数未通过校验，请检查后重新${action}。`
  if (status !== undefined && status >= 500 && status < 600)
    return `服务暂不可用，请稍后再${action}。`
  return `${action}失败，请核对当前记录或稍后重试。`
}
