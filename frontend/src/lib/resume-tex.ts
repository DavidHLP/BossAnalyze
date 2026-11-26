import { offsetToPosition, resumeMathNodes, resumePreview, type PreviewBlock, type ResumeInline } from './resumes.ts'
import { resumeTexTemplates, type ResumeTexTemplateId } from './resume-tex-templates.ts'

export interface ResumeTexError {
  line: number
  column: number
  start: number
  end: number
  formula: string
  message: string
}

export type ResumeLatexResult =
  | { ok: true; tex: string; warnings: string[]; effectiveTemplate: ResumeTexTemplateId }
  | { ok: false; errors: ResumeTexError[] }

const raw = String.raw
const textEscapes: Record<string, string> = {
  '\\': raw`\textbackslash{}`, '{': raw`\{`, '}': raw`\}`, '$': raw`\$`, '&': raw`\&`,
  '#': raw`\#`, '^': raw`\^{}`, '_': raw`\_`, '%': raw`\%`, '~': raw`\textasciitilde{}`,
}

export function escapeLatexText(value: string): string {
  return value.replace(/[\\{}$&#^_%~]/g, char => textEscapes[char]!)
}

const dangerousCommands = new Set(`write immediate input include includeonly openin openout read readline closein closeout
endinput special shipout catcode lccode uccode mathcode delcode sfcode csname endcsname scantokens def edef gdef xdef let
futurelet expandafter afterassignment aftergroup newcommand renewcommand providecommand usepackage RequirePackage
documentclass directlua latelua pdfximage lstinputlisting verbatiminput includegraphics`.split(/\s+/))
const mathAtoms = new Set(`alpha beta gamma delta epsilon varepsilon zeta eta theta vartheta iota kappa varkappa lambda mu nu
xi pi varpi rho varrho sigma varsigma tau upsilon phi varphi chi psi omega
Gamma Delta Theta Lambda Xi Pi Sigma Upsilon Phi Psi Omega
sum prod coprod int iint iiint oint lim limsup liminf min max sup inf sin cos tan cot sec csc arcsin arccos arctan
sinh cosh tanh log ln exp det gcd infty partial nabla cdot times div pm mp ast star circ bullet oplus otimes
le leq ge geq ne neq approx equiv sim simeq cong propto in notin ni subset subseteq supset supseteq cup cap setminus
emptyset varnothing to gets rightarrow leftarrow leftrightarrow Rightarrow Leftarrow Leftrightarrow mapsto
ldots cdots vdots ddots dots vert Vert mid parallel perp prime forall exists nexists lnot neg land lor wedge vee
Re Im ell hbar quad qquad`.split(/\s+/))
const oneArgument = new Set(`mathrm mathbf mathit mathsf mathtt mathbb mathcal operatorname overline underline
hat widehat bar vec dot ddot`.split(/\s+/))
const twoArguments = new Set('frac dfrac tfrac binom overset underset'.split(' '))
const environments = new Set('matrix pmatrix bmatrix Bmatrix vmatrix Vmatrix cases aligned'.split(' '))
const delimiters = new Set(raw`( ) [ ] < > | . \{ \} \langle \rangle \lbrace \rbrace \lbrack \rbrack
\lvert \rvert \lVert \rVert \vert \Vert \| \lfloor \rfloor \lceil \rceil \uparrow \downarrow \updownarrow`.split(/\s+/))
const escapedChars = new Set([...'{}$&#_% '])
const textCommands = new Set(['textbackslash', 'textasciicircum', 'textasciitilde'])

function normalizeNewlines(value: string): string {
  return value.replace(/\r\n?/g, '\n')
}

function whitespace(token: string): boolean {
  return /^[ \t\n]$/.test(token)
}

export function validateLatexMath(tex: string): string | null {
  if (tex.length > 4096) return '公式超过 4096 个 UTF-16 码元。'
  const normalized = normalizeNewlines(tex)
  // Tokenize control symbols as units: pairs of backslashes do not escape the next character.
  const tokens = normalized.match(/\\(?:[A-Za-z]+|[^\n])|[\s\S]/gu) ?? []
  for (let i = 0, offset = 0; i < tokens.length; i++) {
    const token = tokens[i]!
    offset += token.length
    if (token.startsWith('\\') && (dangerousCommands.has(token.slice(1)) ||
      (token === raw`\end` && /^[ \t\n]*\{[ \t\n]*document[ \t\n]*\}/.test(normalized.slice(offset))))) {
      return `公式包含危险的 TeX 命令 ${token === raw`\end` ? raw`\end{document}` : token}。`
    }
  }
  if (normalized.includes('^^')) return '公式不能包含 ^^。'
  if (/[\x00-\x08\x0b-\x1f\x7f]/.test(normalized)) return '公式不能包含 ASCII 控制字符。'
  if (!normalized.trim()) return '公式不能为空。'
  let braces = 0
  for (const token of tokens) {
    if (token === '%') return raw`公式不能包含未转义的 %；请使用 \%。`
    if (token === '$') return raw`公式不能包含未转义的 $；请使用 \$。`
    if (token === '{') braces++
    if (token === '}' && --braces < 0) return '公式包含多余的右花括号。'
  }
  return braces ? '公式包含未闭合的花括号。' : validateMathTokens(tokens)
}

type MathScope =
  | { kind: 'group'; text: boolean; remaining: number }
  | { kind: 'delimiter' }
  | { kind: 'environment'; name: string }

function validateMathTokens(tokens: readonly string[]): string | null {
  const scopes: MathScope[] = []
  let index = 0
  function peek(): string {
    while (whitespace(tokens[index] ?? '')) index++
    return tokens[index] ?? ''
  }
  function openGroup(text: boolean, remaining = 0): boolean {
    if (peek() !== '{') return false
    index++
    scopes.push({ kind: 'group', text, remaining })
    return true
  }
  function environmentName(): string {
    if (peek() !== '{') return ''
    index++
    let name = ''
    while (/^[a-z]$/i.test(tokens[index] ?? '')) name += tokens[index++]
    return tokens[index++] === '}' ? name : ''
  }

  while (peek()) {
    const token = tokens[index++]!
    const scope = scopes.at(-1)
    const text = scope?.kind === 'group' && scope.text
    if (token === '{') {
      scopes.push({ kind: 'group', text, remaining: 0 })
      continue
    }
    if (token === '}') {
      if (scope?.kind !== 'group') return '花括号与数学环境或定界符交错。'
      scopes.pop()
      if (scope.remaining && !openGroup(scope.text, scope.remaining - 1)) return '数学命令缺少花括号参数。'
      continue
    }
    if (!token.startsWith('\\')) {
      if (text ? /[&#_^~]/.test(token) : !/^[A-Za-z0-9+*/=()[\]<>.,:;!?|_^'-]$/.test(token)) {
        if (!text && token === '&' && scope?.kind === 'environment') continue
        return `公式${text ? '文本' : ''}中不允许字符 ${token}。`
      }
      continue
    }

    const command = token.slice(1)
    // A doubled backslash is an alignment row break, never a text escape.
    if (command === '\\') {
      if (scope?.kind !== 'environment') return '换行命令只能用于允许的数学环境。'
      if (peek() === '[' || peek() === '*') return '数学换行命令不接受修饰符。'
      continue
    }
    if (escapedChars.has(command) || (text && textCommands.has(command))) continue
    if (text && (command === '^' || command === '~')) {
      if (!openGroup(true)) return `公式文本命令 ${token} 缺少花括号参数。`
      continue
    }
    if (!text) {
      if (mathAtoms.has(command) || [',', ';', ':', '!', '|', '>'].includes(command)) continue
      if (command === 'begin' || command === 'end') {
        const name = environmentName()
        if (!environments.has(name)) return `不允许数学环境 ${name || token}。`
        if (command === 'begin') {
          if (name === 'aligned' && peek() === '[') return 'aligned 环境不接受可选参数。'
          scopes.push({ kind: 'environment', name })
        } else {
          if (scope?.kind !== 'environment' || scope.name !== name) return `数学环境 ${name} 未正确配对。`
          scopes.pop()
        }
        continue
      }
      if (command === 'left' || command === 'right') {
        if (!delimiters.has(peek())) return `数学定界符 ${token} 缺少允许的符号。`
        index++
        if (command === 'left') scopes.push({ kind: 'delimiter' })
        else if (scope?.kind === 'delimiter') scopes.pop()
        else return raw`\right 缺少匹配的 \left。`
        continue
      }
      if (command === 'sqrt' && peek() === '[') {
        index++
        const first = index
        while (/^[0-9]$/.test(tokens[index] ?? '')) index++
        if (index === first || tokens[index++] !== ']') return '平方根的可选根指数只能包含 ASCII 数字。'
      }
      if (command === 'sqrt' || command === 'text' || oneArgument.has(command) || twoArguments.has(command)) {
        if (!openGroup(command === 'text', twoArguments.has(command) ? 1 : 0)) return `数学命令 ${token} 缺少花括号参数。`
        continue
      }
    }
    return `不允许${text ? '公式文本' : '数学'}命令 ${token}。`
  }

  const pending = scopes.at(-1)
  if (pending?.kind === 'environment') return `数学环境 ${pending.name} 未闭合。`
  if (pending?.kind === 'delimiter') return raw`\left 缺少匹配的 \right。`
  return null
}

function renderMath(value: string): string {
  // Blank physical lines would become paragraph breaks inside TeX math mode.
  return normalizeNewlines(value).split('\n').filter(line => /[^ \t]/.test(line)).join('\n')
}

function inlineText(value: string): string {
  return escapeLatexText(value.replace(/[\x00-\x1f\x7f]/g, ' '))
}

function latexHref(value: string): string | null {
  if (!/^(?:https?:\/\/|mailto:)/i.test(value) || /[\s\\\x00-\x1f\x7f]/.test(value)) return null
  try {
    const url = new URL(value)
    if (url.username || url.password || !url.pathname) return null
    return url.href.replace(/[{}^~%#&_$]/g, char => '{}^~'.includes(char)
      ? raw`\%${char.charCodeAt(0).toString(16).toUpperCase()}`
      : '\\' + char)
  } catch {
    return null
  }
}

function renderInline(tokens: readonly ResumeInline[]): string {
  return tokens.map(token => {
    switch (token.kind) {
      case 'text': return inlineText(token.text)
      case 'strong': return raw`\textbf{${inlineText(token.text)}}`
      case 'em': return raw`\emph{${inlineText(token.text)}}`
      case 'code': return raw`\texttt{${inlineText(token.text)}}`
      case 'link': {
        const label = inlineText(token.text)
        const href = latexHref(token.href)
        return href ? raw`\href{${href}}{${label}}` : label
      }
      case 'math': return raw`\(${renderMath(token.tex)}\)`
    }
  }).join('')
}

function renderCode(text: string): string {
  const lines = text.split(/\r\n?|\n/).map(line => {
    const escaped = escapeLatexText(line.replace(/[\x00-\x08\x0b-\x1f\x7f]/g, ' '))
    const spaced = escaped.replace(/[ \t]/g, char => raw`\ `.repeat(char === '\t' ? 4 : 1))
    return raw`\noindent\strut ${spaced}\par`
  })
  return [raw`{\ttfamily\raggedright`, ...lines, '}'].join('\n')
}

function renderTable(headers: ResumeInline[][], rows: ResumeInline[][][]): string {
  if (!headers.length) return ''
  // Group cells so an inner math alignment cannot supply outer table separators.
  const row = (cells: string[]) => cells.join(' & ') + raw` \tabularnewline`
  const header = row(headers.map(cell => raw`\textbf{${renderInline(cell)}}`))
  const head = [raw`\toprule`, header, raw`\midrule`]
  return [raw`\begin{resumetable}{${headers.length}}`, ...head, raw`\endfirsthead`, ...head, raw`\endhead`,
    raw`\bottomrule`, raw`\endlastfoot`, ...rows.map(cells => row(headers.map((_, i) => raw`{${renderInline(cells[i] ?? [])}}`))),
    raw`\end{resumetable}`].join('\n')
}

const omittedAsset = '本地导出未包含图片'

function assetName(fileName: string): string {
  return fileName.replace(/[^\p{L}\p{M}\p{N}._ ()-]+/gu, '_').trim() || '图片素材'
}

// Template callers must not emit unchecked raw TeX, even when rendering only a body.
export function renderResumeLatexBlocks(blocks: readonly PreviewBlock[]): string {
  for (const node of resumeMathNodes(blocks)) {
    const message = validateLatexMath(node.tex)
    if (message) throw new Error(message)
  }
  return renderBlocks(blocks)
}

function renderBlocks(blocks: readonly PreviewBlock[], professional = false): string {
  return blocks.map((block, index) => {
    switch (block.kind) {
      case 'heading': return professional && index === 0
        ? raw`\resumename{${renderInline(block.inline)}}`
        : raw`\resumeheading{${block.level}}{${renderInline(block.inline)}}`
      case 'text': return professional && index === 1
        ? raw`\resumecontact{${renderInline(block.inline)}}`
        : raw`\resumebody{${renderInline(block.inline)}}`
      case 'rule': return raw`\resumerule`
      case 'math': return raw`\[
${renderMath(block.tex)}
\]`
      case 'code': return renderCode(block.text)
      case 'table': return renderTable(block.headers, block.rows)
      case 'asset': return raw`% asset: ${assetName(block.fileName)} (omitted)
\resumeasset{${inlineText(block.text.trim() || '图片素材')} · ${omittedAsset}}`
      case 'list': {
        const environment = block.start === null ? 'resumeitemize' : 'resumeenumerate'
        const options = Number.isSafeInteger(block.start) && block.start !== 1 ? `[start=${block.start}]` : ''
        return [raw`\begin{${environment}}${options}`, ...block.items.map(item => raw`\item {${renderInline(item)}}`),
          raw`\end{${environment}}`].join('\n')
      }
    }
  }).join('\n')
}

export function inspectResumeTexTemplate(
  blocks: readonly PreviewBlock[],
  templateId: ResumeTexTemplateId,
): { fallbackToBasic: boolean; warnings: string[] } {
  if (templateId === 'basic') return { fallbackToBasic: false, warnings: [] }

  const names = blocks.filter(block => block.kind === 'heading' && block.level === 1)
  if (names.length > 1) {
    return { fallbackToBasic: true, warnings: ['正文包含多个 H1（一级标题），下载时将使用基础模板。'] }
  }
  const first = blocks[0]
  if (first?.kind !== 'heading' || first.level !== 1 ||
    !first.inline.some(token => token.kind === 'math' || token.text.trim())) {
    return { fallbackToBasic: true, warnings: ['正文第一块须为非空的 H1（姓名），下载时将使用基础模板。'] }
  }
  const warnings: string[] = []
  if (blocks[1]?.kind !== 'text') warnings.push('姓名后没有联系行，将省略这一行。')
  if (!blocks.some(block => block.kind === 'heading' && block.level === 2)) {
    warnings.push('正文没有 H2（二级标题）分区，将按原顺序排版。')
  }
  return { fallbackToBasic: false, warnings }
}

export function resumeLatex(
  title: string,
  content: string | readonly PreviewBlock[],
  options?: { sourceMarkdown?: string; template?: ResumeTexTemplateId } | string,
): ResumeLatexResult {
  const { sourceMarkdown, template = 'basic' } = typeof options === 'string'
    ? { sourceMarkdown: options }
    : options ?? {}
  const blocks = typeof content === 'string' ? resumePreview(content) : content
  const source = typeof content === 'string' ? content : sourceMarkdown ?? ''
  const errors: ResumeTexError[] = []
  for (const { tex: formula, start, end } of resumeMathNodes(blocks)) {
    const message = validateLatexMath(formula)
    if (message) errors.push({ ...offsetToPosition(source, start), start, end, formula, message })
  }
  if (errors.length) return { ok: false, errors }
  const { fallbackToBasic, warnings } = inspectResumeTexTemplate(blocks, template)
  const effectiveTemplate = fallbackToBasic ? 'basic' : template
  for (const block of blocks) {
    if (block.kind === 'asset') warnings.push(`${omittedAsset}：${assetName(block.fileName)}`)
  }
  return { ok: true, effectiveTemplate, warnings, tex: raw`${resumeTexTemplates[effectiveTemplate].preamble(inlineText(title))}
\begin{document}
${renderBlocks(blocks, effectiveTemplate !== 'basic')}
\end{document}
` }
}
