import type { renderToString } from 'katex'
import type { ResumeMathRender } from './resumes'

export const RESUME_MATH_MAX_LENGTH = 4096

export type ResumeMathResult = { ok: true; html: string } | { ok: false; message: string }

export interface ResumeMathEngine {
  render(tex: string, display: boolean): ResumeMathResult
  renderOrNull: ResumeMathRender
}

export function renderResumeMath(
  katexRenderToString: typeof renderToString,
  tex: string,
  display: boolean,
): ResumeMathResult {
  if (tex.length > RESUME_MATH_MAX_LENGTH) return { ok: false, message: '公式超过 4096 个字符。' }
  if (!tex.trim()) return { ok: false, message: '公式为空。' }
  let trustDenied = false
  const unsupported = { ok: false, message: '公式包含不受支持的命令。' } as const
  try {
    const html = katexRenderToString(tex, {
      output: 'mathml', displayMode: display, throwOnError: true,
      strict: 'error', maxExpand: 250, maxSize: 10, macros: {},
      trust: () => {
        trustDenied = true
        return false
      },
    })
    if (trustDenied) return unsupported
    if (typeof html === 'string' && /<a(?=[\s/>])|<[a-z][^>]*\shref\s*=/i.test(html)) {
      return unsupported
    }
    if (typeof html !== 'string' || !/<math(?:\s|>)/.test(html)) {
      return { ok: false, message: '未能生成 MathML。' }
    }
    return { ok: true, html }
  } catch {
    return trustDenied ? unsupported : { ok: false, message: '公式语法无效或超出渲染限制。' }
  }
}

export function createResumeMathEngine(
  katexRenderToString: typeof renderToString,
): ResumeMathEngine {
  const render: ResumeMathEngine['render'] = (tex, display) =>
    renderResumeMath(katexRenderToString, tex, display)
  return {
    render,
    renderOrNull(tex, display) {
      const result = render(tex, display)
      return result.ok ? result.html : null
    },
  }
}

let pending: Promise<ResumeMathEngine> | undefined

export function loadResumeMath(): Promise<ResumeMathEngine> {
  return pending ??= import('katex')
    .then(katex => createResumeMathEngine(katex.default.renderToString))
    .catch(error => {
      pending = undefined
      throw error
    })
}
