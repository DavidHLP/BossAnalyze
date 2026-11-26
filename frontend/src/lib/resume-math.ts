import type { renderToString } from 'katex'
import type { ResumeMathRender } from './resumes'

export const RESUME_MATH_MAX_LENGTH = 4096

export interface ResumeMathEngine {
  render(tex: string, display: boolean): { ok: true; html: string } | { ok: false; message: string }
  renderOrNull: ResumeMathRender
}

export function createResumeMathEngine(katexRenderToString: typeof renderToString): ResumeMathEngine {
  const render: ResumeMathEngine['render'] = (tex, display) => {
    if (tex.length > RESUME_MATH_MAX_LENGTH) return { ok: false, message: '公式超过 4096 个字符。' }
    if (!tex.trim()) return { ok: false, message: '公式为空。' }
    try {
      const html = katexRenderToString(tex, {
        output: 'mathml', displayMode: display, trust: false, throwOnError: true,
        strict: 'error', maxExpand: 250, maxSize: 10, macros: {},
      })
      if (typeof html !== 'string' || !/<math(?:\s|>)/.test(html)) return { ok: false, message: '未能生成 MathML。' }
      // KaTeX shows trust-denied commands as red command names rather than throwing.
      if (/<mstyle mathcolor="#cc0000"><mtext>\\(?:href|url|includegraphics|html(?:Class|Id|Style|Data))<\/mtext><\/mstyle>/.test(html)) {
        return { ok: false, message: '公式包含不受支持的命令。' }
      }
      return { ok: true, html }
    } catch {
      return { ok: false, message: '公式语法无效或超出渲染限制。' }
    }
  }
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
