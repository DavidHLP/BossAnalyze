import { isResumeImageDataUrl, resumeLayoutCss, resumePageMargin } from './resume-render.ts'

export function shortCommit(id: string | null | undefined) {
  return id?.slice(0, 8) || '—'
}

export function resumeStamp(value: string | number | undefined) {
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0
  if (!value) return 0
  const text = value.trim()
  if (/^\d+$/.test(text)) {
    const epoch = Number(text)
    return Number.isFinite(epoch) ? epoch : 0
  }
  const stamp = Date.parse(text)
  return Number.isNaN(stamp) ? 0 : stamp
}

function resumeClock(value: Date) {
  return Number.isNaN(value.getTime()) ? '—' : value.toISOString().replace('T', ' ').slice(0, 19)
}

export function resumeTime(value: string | number | undefined) {
  if (typeof value === 'number') return resumeClock(new Date(value))
  if (!value) return '—'
  const text = value.trim()
  if (/^\d+$/.test(text)) return resumeClock(new Date(Number(text)))
  return Number.isNaN(Date.parse(text)) ? '—' : text.replace('T', ' ').slice(0, 19)
}

export interface ResumeConflictState {
  workspaceRevision: number | null
  headCommit: string | null
}

export function resumeWriteConflict(cause: unknown): ResumeConflictState | null {
  if (!cause || typeof cause !== 'object' || !('code' in cause) || cause.code !== 409) return null
  const body = 'data' in cause ? cause.data : undefined
  const state = body && typeof body === 'object' && 'data' in body ? body.data : body
  const revision = state && typeof state === 'object' && 'workspaceRevision' in state ? state.workspaceRevision : undefined
  const head = state && typeof state === 'object' && 'headCommit' in state ? state.headCommit : null
  return {
    workspaceRevision: typeof revision === 'number' && Number.isInteger(revision) && revision >= 0 ? revision : null,
    headCommit: typeof head === 'string' ? head : null,
  }
}

export function resumeConflictCopyTitle(title: string) {
  return (title.trim() || '简历') + '（冲突副本）'
}

export function resumeReadMessage(cause: unknown) {
  const code = cause && typeof cause === 'object' && 'code' in cause ? cause.code : undefined
  if (code === 403) return '当前账号无法访问这份简历。'
  if (code === 404) return '找不到这份简历。'
  return '暂时无法读取简历，请重试。'
}

export type ResumeListChange =
  | { kind: 'create'; id?: string; title: string; content: string; previousIds: ReadonlySet<string> }
  | { kind: 'rename'; id: string; title: string }
  | { kind: 'delete'; id: string }

export function resumeListReadback(change: ResumeListChange, items: { id: string; title: string; content?: string | null }[]) {
  if (change.kind === 'create') {
    const matches = (item: { title: string; content?: string | null } | undefined) =>
      Boolean(item) && item!.title === change.title && item!.content === change.content
    return change.id
      ? matches(items.find(item => item.id === change.id))
      : items.some(item => matches(item) && !change.previousIds.has(item.id))
  }
  const item = items.find(item => item.id === change.id)
  return change.kind === 'delete' ? !item : item?.title === change.title
}

export interface ResumeDraft {
  content: string
  message: string
  baseHeadCommit: string | null
}

export function resumeDraftStore(storage: Storage, account: string, sessionStart: string, resumeId: string) {
  const key = 'bossanalyze:resume-draft:v2:' +
    [account, sessionStart, resumeId].map(encodeURIComponent).join(':')
  return {
    read(): ResumeDraft | null {
      // Storage access and quota errors surface so the editor can report drafts as unavailable.
      const raw = storage.getItem(key)
      if (raw === null) return null
      let value: unknown
      try {
        value = JSON.parse(raw)
      } catch {
        storage.removeItem(key)
        return null
      }
      if (!value || typeof value !== 'object' ||
        !('account' in value) || value.account !== account ||
        !('resumeId' in value) || value.resumeId !== resumeId ||
        !('content' in value) || typeof value.content !== 'string' ||
        !('message' in value) || typeof value.message !== 'string' ||
        ('baseHeadCommit' in value && value.baseHeadCommit !== null && typeof value.baseHeadCommit !== 'string')) {
        storage.removeItem(key)
        return null
      }
      return {
        content: value.content,
        message: value.message,
        baseHeadCommit: 'baseHeadCommit' in value && typeof value.baseHeadCommit === 'string'
          ? value.baseHeadCommit
          : null,
      }
    },
    write(draft: ResumeDraft) {
      storage.setItem(key, JSON.stringify({
        account,
        resumeId,
        content: draft.content,
        message: draft.message,
        baseHeadCommit: draft.baseHeadCommit ?? null,
      }))
    },
    clear() {
      storage.removeItem(key)
    },
  }
}

export function countDuplicateCommits(commits: { id: string }[]) {
  return commits.length - new Set(commits.map(commit => commit.id)).size
}

export type ResumeMathNode = {
  display: boolean
  tex: string
  raw: string
  start: number
  end: number
  bodyStart: number
}

export type ResumeMathRender = (tex: string, display: boolean) => string | null

type ResumeMathToken = { kind: 'math' } & Omit<ResumeMathNode, 'display'>

export type ResumeInline =
  | { kind: 'text' | 'strong' | 'em' | 'code'; text: string }
  | { kind: 'link'; text: string; href: string }
  | ResumeMathToken

export type PreviewBlock =
  | { kind: 'heading'; level: 1 | 2 | 3; inline: ResumeInline[] }
  | { kind: 'text'; inline: ResumeInline[] }
  | { kind: 'code'; text: string }
  | { kind: 'list'; start: number | null; items: ResumeInline[][] }
  | { kind: 'table'; headers: ResumeInline[][]; rows: ResumeInline[][][] }
  | { kind: 'rule' }
  | { kind: 'asset'; text: string; fileName: string }
  | ResumeMathToken

function mathNode({ tex, raw, start, end, bodyStart }: ResumeMathToken, display: boolean): ResumeMathNode {
  return { display, tex, raw, start, end, bodyStart }
}

function* inlineMathNodes(tokens: readonly ResumeInline[]): Generator<ResumeMathNode> {
  for (const token of tokens) {
    if (token.kind === 'math') yield mathNode(token, false)
  }
}

function* mathNodes(blocks: readonly PreviewBlock[]): Generator<ResumeMathNode> {
  for (const block of blocks) {
    switch (block.kind) {
      case 'math':
        yield mathNode(block, true)
        break
      case 'heading':
      case 'text':
        yield* inlineMathNodes(block.inline)
        break
      case 'list':
        for (const item of block.items) yield* inlineMathNodes(item)
        break
      case 'table':
        for (const cell of block.headers) yield* inlineMathNodes(cell)
        for (const row of block.rows) {
          for (const cell of row) yield* inlineMathNodes(cell)
        }
        break
    }
  }
}

export function resumeHasMath(blocks: readonly PreviewBlock[]): boolean {
  return !mathNodes(blocks).next().done
}

export function resumeMathNodes(blocks: readonly PreviewBlock[]): ResumeMathNode[] {
  return [...mathNodes(blocks)]
}

export function offsetToPosition(source: string, offset: number): { line: number; column: number } {
  const clamped = Math.min(source.length, Math.max(0, Math.trunc(offset) || 0))
  let line = 1
  let lineStart = 0
  for (const match of source.matchAll(/\r\n?|\n/g)) {
    const nextLineStart = match.index! + match[0].length
    if (nextLineStart > clamped) break
    line++
    lineStart = nextLineStart
  }
  return { line, column: clamped - lineStart + 1 }
}

export function markdownHref(value: string) {
  if (!/^(?:https?:\/\/|mailto:)/i.test(value) || /[\s\\\x00-\x1f\x7f]/.test(value)) return null
  try {
    const url = new URL(value)
    return url.username || url.password || !url.pathname ? null : url.href
  } catch {
    return null
  }
}

type InlineSpan = { kind: 'code' | 'math'; start: number; end: number }
const linkPattern = /(!?)\[([^\[\]]*)\]\(([^()\s]+)\)/g
const emphasisPattern = /\*\*([^*]+)\*\*|__([^_]+)__|\*([^*]+)\*|(?<![\w_])_([^_]+)_(?![\w_])/
const inlineTextPattern = new RegExp(`${emphasisPattern.source}|${linkPattern.source}`, 'g')

function isEscaped(text: string, index: number) {
  let start = index
  while (text[start - 1] === '\\') start--
  return (index - start) % 2 === 1
}

function* inlineMathSpans(text: string, offset: number): Generator<InlineSpan> {
  // Mask dollars in link destinations without changing the offsets into the original text.
  const scan = text.replace(linkPattern, (literal, image, label, destination) =>
    !image && destination.includes('$') && !literal.includes('`') && markdownHref(destination)
      ? `[${label}](${destination.replaceAll('$', 'x')})`
      : literal)
  let opener = -1
  for (const match of scan.matchAll(/\$+/g)) {
    const first = match.index!
    const start = isEscaped(text, first) ? first + 1 : first
    const end = first + match[0].length
    if (start === end) continue
    if (end - start !== 1) {
      opener = -1
      continue
    }

    if (opener !== -1 && /\S/.test(text[start - 1] ?? '') && !/[0-9]/.test(text[end] ?? '')) {
      yield { kind: 'math', start: offset + opener, end: offset + end }
      opener = -1
    } else {
      opener = /\S/.test(text[end] ?? '') ? start : -1
    }
  }
}

function* codeSpans(text: string): Generator<InlineSpan> {
  for (const match of text.matchAll(/(?<!`)`[^`]+`(?!`)/g)) {
    const start = match.index!
    yield { kind: 'code', start, end: start + match[0].length }
  }
}

function* inlineSpans(text: string): Generator<InlineSpan> {
  let end = 0
  for (const span of codeSpans(text)) {
    yield* inlineMathSpans(text.slice(end, span.start), end)
    yield span
    end = span.end
  }
  yield* inlineMathSpans(text.slice(end), end)
}

function appendInlineText(tokens: ResumeInline[], text: string) {
  let end = 0
  for (const match of text.matchAll(inlineTextPattern)) {
    const [literal, strong, alternateStrong, emphasis, alternateEmphasis, image, label, destination] = match
    const start = match.index!
    if (start > end) tokens.push({ kind: 'text', text: text.slice(end, start) })
    const href = destination && !image ? markdownHref(destination) : null
    if (strong || alternateStrong) tokens.push({ kind: 'strong', text: (strong ?? alternateStrong)! })
    else if (emphasis || alternateEmphasis) tokens.push({ kind: 'em', text: (emphasis ?? alternateEmphasis)! })
    else if (href) tokens.push({ kind: 'link', text: label || href, href })
    else tokens.push({ kind: 'text', text: literal })
    end = start + literal.length
  }
  if (end < text.length) tokens.push({ kind: 'text', text: text.slice(end) })
}

function maskedEscapes(text: string) {
  const values: string[] = []
  const masked = text.replace(/\\([*|#.`_[\]{}()>+\-!])/g, (literal: string, char: string) => {
    if (values.length >= 0x6400) return literal
    values.push(char)
    return String.fromCharCode(0xe000 + values.length - 1)
  })
  return {
    text: masked,
    restore: (value: string) => values.length
      ? value.replace(/[\uE000-\uF8FF]/g, marker => values[marker.charCodeAt(0) - 0xe000] ?? marker)
      : value,
  }
}

function resumeInline(text: string, baseOffset: number, inTable = false): ResumeInline[] {
  const tokens: ResumeInline[] = []
  let plainStart = 0
  const unescapePipes = (value: string) => inTable ? value.replaceAll('\\|', '|') : value
  const appendPlain = (end: number) => {
    const escaped = maskedEscapes(unescapePipes(text.slice(plainStart, end))
      .replace(/\\+(\$)?/g, (literal, dollar) => dollar && literal.length % 2 === 0 ? literal.slice(1) : literal))
    const start = tokens.length
    appendInlineText(tokens, escaped.text)
    for (let i = start; i < tokens.length; i++) {
      const token = tokens[i]!
      if ('text' in token) token.text = escaped.restore(token.text)
    }
  }
  for (const { kind, start, end } of inlineSpans(text)) {
    appendPlain(start)
    const body = text.slice(start + 1, end - 1)
    if (kind === 'code') {
      tokens.push({ kind, text: unescapePipes(body) })
    } else {
      tokens.push({ kind, tex: body, raw: text.slice(start, end),
        start: baseOffset + start, end: baseOffset + end, bodyStart: baseOffset + start + 1 })
    }
    plainStart = end
  }
  appendPlain(text.length)
  return tokens
}

const listItem = /^\s*(?:([-*+])|(\d{1,9})[.)])[ \t]+(.+)$/
const codeFence = /^\s*(`{3,}|~{3,})[^`]*$/
const displayDelimiter = /^([ \t]*)\$\$[ \t]*$/

type SourceLine = { text: string; offset: number }

function mathBlock(markdown: string, lines: SourceLine[], index: number) {
  const { text, offset } = lines[index]!
  const single = /^([ \t]*)\$\$(?!\$)(.+?)(?<!\$)\$\$[ \t]*$/s.exec(text)
  if (single) {
    const start = offset + single[1]!.length
    const bodyStart = start + 2
    const tex = single[2]!
    const close = bodyStart + tex.length
    if (tex.trim() && !isEscaped(markdown, close)) {
      const node: ResumeMathToken = { kind: 'math', tex, raw: markdown.slice(start, close + 2), start, end: close + 2, bodyStart }
      return { node, lastLine: index }
    }
  }

  const opener = displayDelimiter.exec(text)
  if (!opener || !lines[index + 1]) return null
  const start = offset + opener[1]!.length
  const bodyStart = lines[index + 1]!.offset
  for (let lastLine = index + 1; lastLine < lines.length; lastLine++) {
    const line = lines[lastLine]!
    if (codeFence.test(line.text)) break
    const closer = displayDelimiter.exec(line.text)
    if (!closer) continue
    const previous = lines[lastLine - 1]!
    const tex = markdown.slice(bodyStart, previous.offset + previous.text.length)
    if (!tex.trim()) break
    const end = line.offset + closer[1]!.length + 2
    const node: ResumeMathToken = { kind: 'math', tex, raw: markdown.slice(start, end), start, end, bodyStart }
    return { node, lastLine }
  }
  return null
}

function tableCells(line: SourceLine): SourceLine[] {
  if (!line.text.includes('|')) return []
  const leftTrimmed = line.text.trimStart()
  const offset = line.offset + line.text.length - leftTrimmed.length
  const text = leftTrimmed.trimEnd()
  const cells: SourceLine[] = []
  let cellStart = 0
  const append = (end: number) => {
    const raw = text.slice(cellStart, end)
    const cell = raw.trimStart()
    cells.push({ text: cell.trimEnd(), offset: offset + cellStart + raw.length - cell.length })
  }
  const split = (start: number, end: number) => {
    for (let i = start; i < end; i++) {
      if (text[i] !== '|' || text[i - 1] === '\\') continue
      if (i > 0) append(i)
      cellStart = i + 1
    }
  }
  let plainStart = 0
  for (const span of codeSpans(text)) {
    split(plainStart, span.start)
    plainStart = span.end
  }
  split(plainStart, text.length)
  if (cellStart && (cellStart < text.length || !cells.length)) append(text.length)
  return cells
}

export function resumePreview(markdown: string): PreviewBlock[] {
  // shortcut: this flat résumé Markdown subset omits nested blocks and reference links.
  const lines: SourceLine[] = []
  let lineStart = 0
  for (const match of markdown.matchAll(/\r\n?|\n/g)) {
    const end = match.index!
    lines.push({ text: markdown.slice(lineStart, end), offset: lineStart })
    lineStart = end + match[0].length
  }
  lines.push({ text: markdown.slice(lineStart), offset: lineStart })
  const blocks: PreviewBlock[] = []
  for (let i = 0; i < lines.length; i++) {
    const { text: line, offset } = lines[i]!
    if (!line.trim()) continue
    const fence = codeFence.exec(line)
    if (fence) {
      const close = new RegExp('^\\s*' + fence[1]![0] + '{' + fence[1]!.length + ',}\\s*$')
      const body: string[] = []
      while (++i < lines.length && !close.test(lines[i]!.text)) body.push(lines[i]!.text)
      blocks.push({ kind: 'code', text: body.join('\n') })
      continue
    }
    const math = mathBlock(markdown, lines, i)
    if (math) {
      blocks.push(math.node)
      i = math.lastLine
      continue
    }
    const title = /^(#{1,3})\s+(.+)$/.exec(line)
    if (title) {
      const content = title[2]!
      blocks.push({ kind: 'heading', level: title[1]!.length as 1 | 2 | 3,
        inline: resumeInline(content, offset + line.length - content.length) })
      continue
    }
    if (/^\s*---+\s*$/.test(line)) {
      blocks.push({ kind: 'rule' })
      continue
    }
    const asset = /^!\[([^\]]*)\]\(asset:([^)]+)\)$/.exec(line.trim())
    if (asset) {
      try {
        blocks.push({ kind: 'asset', text: asset[1]!, fileName: decodeURIComponent(asset[2]!) })
        continue
      } catch {
        // A malformed private image reference remains plain text.
      }
    }
    const bullet = listItem.exec(line)
    if (bullet) {
      const ordered = bullet[2] !== undefined
      const content = bullet[3]!
      const items = [resumeInline(content, offset + line.length - content.length)]
      while (i + 1 < lines.length) {
        const nextLine = lines[i + 1]!
        const next = listItem.exec(nextLine.text)
        if (!next || ordered !== (next[2] !== undefined)) break
        i++
        const nextContent = next[3]!
        items.push(resumeInline(nextContent, nextLine.offset + nextLine.text.length - nextContent.length))
      }
      blocks.push({ kind: 'list', start: ordered ? Number(bullet[2]) : null, items })
      continue
    }
    const headers = tableCells(lines[i]!)
    const separator = headers.length && lines[i + 1] ? tableCells(lines[i + 1]!) : []
    if (headers.length && headers.length === separator.length && separator.every(cell => /^:?-{3,}:?$/.test(cell.text))) {
      const parseCell = (cell: SourceLine | undefined) => cell ? resumeInline(cell.text, cell.offset, true) : []
      i++
      const rows: ResumeInline[][][] = []
      while (i + 1 < lines.length) {
        const next = lines[i + 1]!
        if (codeFence.test(next.text) || mathBlock(markdown, lines, i + 1)) break
        const cells = tableCells(next)
        if (!cells.length) break
        i++
        rows.push(headers.map((_, index) => parseCell(cells[index])))
      }
      blocks.push({ kind: 'table', headers: headers.map(parseCell), rows })
      continue
    }
    blocks.push({ kind: 'text', inline: resumeInline(line, offset) })
  }
  return blocks
}

export function assetMarkdown(fileName: string) {
  return '![' + fileName.replace(/[\[\]\\]/g, '') + '](asset:' +
    encodeURIComponent(fileName).replace(/[()]/g, char => char === '(' ? '%28' : '%29') + ')'
}

export function resumeAssetNames(blocks: PreviewBlock[]) {
  return [...new Set(blocks.flatMap(block => block.kind === 'asset' ? [block.fileName] : []))]
}

export type ResumeExportSource = 'current' | 'saved' | 'save-first'
export type ResumeExportFormat = 'html' | 'pdf' | 'markdown' | 'tex' | 'docx' | 'external'

export interface ResumeExportSourceState {
  dirty: boolean
  savedContent: string | null
}

export function resumeExportContent(source: ResumeExportSource, content: string, savedContent: string | null) {
  return source === 'current' ? content : savedContent ?? ''
}

export function resumeExportSourceLabel(source: ResumeExportSource, dirty: boolean) {
  if (source === 'saved') return '服务器正文（不含编辑区未保存修改）'
  if (source === 'save-first') return '服务器正文（保存已回读确认）'
  return dirty ? '当前内容（含未保存修改）' : '当前内容（与服务器正文一致）'
}

export function resumeExportUnavailableReason(source: ResumeExportSource, state: ResumeExportSourceState) {
  if (source === 'current') return null
  if (state.savedContent === null) return '服务器尚无可导出的正文，可先导出当前内容。'
  if (source === 'save-first' && state.dirty) return '保存尚未回读确认：请先在编辑页保存并核对，或直接导出当前内容。'
  return null
}

export function resumeImageCount(markdown: string) {
  return resumeAssetNames(resumePreview(markdown)).length
}

export function resumeExportFileBase(title: string) {
  return title.trim().replace(/[<>:"/\\|?*\u0000-\u001f]/g, '_') || '简历'
}

interface ResumeExportImages {
  imageCount: number
  embeddedImageCount?: number
  includeImages?: boolean
}

export function resumeExportSummary(input: ResumeExportImages & {
  source: ResumeExportSource
  dirty: boolean
  savedContent: string | null
  title: string
}) {
  return {
    source: resumeExportSourceLabel(input.source, input.dirty),
    unsaved: input.source === 'current' && input.dirty ? '有未保存修改' : '不含未保存修改',
    images: resumeExportLossNotes('html', input)[0],
    fileName: resumeExportFileBase(input.title) + '.pdf / .html / .md / .tex',
  }
}

export function resumeExportLossNotes(
  format: ResumeExportFormat,
  { imageCount, embeddedImageCount, includeImages = true }: ResumeExportImages,
) {
  switch (format) {
    case 'html':
    case 'pdf':
      if (!imageCount) return ['无图片。']
      if (!includeImages) return ['已选择省略全部图片和占位文字。']
      if (embeddedImageCount === undefined) return [`${imageCount} 张图片待处理，可用图片将嵌入，失败时保留文字占位。`]
      if (embeddedImageCount === imageCount) return [`${imageCount} 张图片已嵌入。`]
      if (!embeddedImageCount) return [`${imageCount} 张图片加载失败，已保留文字占位。`]
      return [`已嵌入 ${embeddedImageCount} 张图片，另有 ${imageCount - embeddedImageCount} 张保留文字占位。`]
    case 'markdown':
      return ['Markdown 保留公式源码与图片引用，不嵌入图片文件。']
    case 'tex':
      return ['需要用本机 XeLaTeX 编译，并禁用 --shell-escape；不含图片，且无法回导版本库。']
    case 'docx':
      return ['DOCX 导出尚未开放；开放后公式与图片仅以文字占位。']
    case 'external':
      return ['外部导出只发送标题与 Markdown 正文，图片不会发送；版式由对方服务决定。']
  }
}

export function exportDestination(config: string | undefined, mock = false) {
  if (mock) return '/api/mock/resume-export'
  const address = config?.trim()
  if (!address || !/^https:\/\/[^/]/i.test(address) || /[\s\\]/.test(address)) return null
  try {
    const url = new URL(address)
    return url.protocol === 'https:' && !url.username && !url.password ? url.href : null
  } catch {
    return null
  }
}

export function resumeExportResult(payload: unknown) {
  if (!payload || typeof payload !== 'object' || ('success' in payload && payload.success === false)) return null
  for (const field of ['url', 'downloadUrl', 'previewUrl'] as const) {
    const value = (payload as Record<string, unknown>)[field]
    if (typeof value !== 'string') continue
    const url = exportDestination(value)
    if (url) return url
  }
  return null
}

function escapeHtml(value: string) {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;').replaceAll("'", '&#39;')
}

function inlineHtml(tokens: ResumeInline[], renderMath?: ResumeMathRender) {
  return tokens.map(token => {
    switch (token.kind) {
      case 'text': return escapeHtml(token.text)
      case 'strong': return '<strong>' + escapeHtml(token.text) + '</strong>'
      case 'em': return '<em>' + escapeHtml(token.text) + '</em>'
      case 'code': return '<code>' + escapeHtml(token.text) + '</code>'
      case 'link':
        return '<a href="' + escapeHtml(token.href) + '" rel="noreferrer">' + escapeHtml(token.text) + '</a>'
      case 'math': {
        const rendered = renderMath?.(token.tex, false)
        return rendered ? '<span class="resume-math-inline">' + rendered + '</span>' : escapeHtml(token.raw)
      }
    }
  }).join('')
}

export function resumeHtml(title: string, content: string | PreviewBlock[], renderMath?: ResumeMathRender,
  { images, includeImages = true }: { images?: Readonly<Record<string, string>>; includeImages?: boolean } = {}): string {
  const blocks = typeof content === 'string' ? resumePreview(content) : content
  const renderInline = (tokens: ResumeInline[]) => inlineHtml(tokens, renderMath)
  const body = blocks.map(block => {
    switch (block.kind) {
      case 'heading': return '<h' + block.level + '>' + renderInline(block.inline) + '</h' + block.level + '>'
      case 'text': return '<p>' + renderInline(block.inline) + '</p>'
      case 'rule': return '<hr>'
      case 'asset': {
        if (!includeImages) return ''
        const src = images && Object.hasOwn(images, block.fileName) ? images[block.fileName] : undefined
        const alt = escapeHtml(block.text)
        return isResumeImageDataUrl(src)
          ? `<figure><img src="${src}" alt="${alt}"></figure>`
          : `<figure><figcaption>${alt || '图片素材'} · 图片无法加载</figcaption></figure>`
      }
      case 'code': return '<pre><code>' + escapeHtml(block.text) + '</code></pre>'
      case 'math': {
        const rendered = renderMath?.(block.tex, true)
        return rendered ? '<div class="resume-math-block">' + rendered + '</div>' : '<pre><code>' + escapeHtml(block.raw) + '</code></pre>'
      }
      case 'list': {
        const ordered = block.start !== null
        return (ordered ? '<ol start="' + block.start + '">' : '<ul>') +
          block.items.map(item => '<li>' + renderInline(item) + '</li>').join('') + (ordered ? '</ol>' : '</ul>')
      }
      case 'table':
        return '<table><thead><tr>' + block.headers.map(cell => '<th scope="col">' + renderInline(cell) + '</th>').join('') +
          '</tr></thead><tbody>' + block.rows.map(row =>
            '<tr>' + row.map(cell => '<td>' + renderInline(cell) + '</td>').join('') + '</tr>').join('') + '</tbody></table>'
    }
  }).join('\n')

  return `<!doctype html>
<html lang="zh-CN"><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'">
<meta name="referrer" content="no-referrer">
<title>${escapeHtml(title)}</title>
<style>
  @page { size: A4; margin: ${resumePageMargin}; }
  body { margin: 0; padding: ${resumePageMargin}; color: #161616; background: white; }
  ${resumeLayoutCss}
  @media print { body { padding: 0; } }
</style>
<body><article class="resume-body">${body}</article></body></html>`
}

type CommitParents = { id: string; parentCommits?: string[] }[]

export function resumeMainline(head: string | null | undefined, commits: CommitParents) {
  const parents = new Map(commits.map(commit => [commit.id, commit.parentCommits?.[0]]))
  const seen = new Set<string>()
  while (head && !seen.has(head)) {
    seen.add(head)
    head = parents.get(head)
  }
  return seen
}

export function resumeFirstParentRelation(from: string, to: string, commits: CommitParents) {
  if (resumeMainline(to, commits).has(from)) return 'forward'
  if (resumeMainline(from, commits).has(to)) return 'reverse'
  return 'separate'
}

export function resumeLineDiff(previous: string, next: string) {
  const split = (content: string) => content ? content.replace(/\r\n?/g, '\n').replace(/\n$/, '').split('\n') : []
  const before = split(previous)
  const after = split(next)
  const lines: { kind: 'unchanged' | 'added' | 'removed'; text: string }[] = []
  let prefix = 0
  let left = before.length
  let right = after.length
  while (prefix < left && prefix < right && before[prefix] === after[prefix]) {
    lines.push({ kind: 'unchanged', text: before[prefix++]! })
  }
  while (left > prefix && right > prefix && before[left - 1] === after[right - 1]) {
    left--
    right--
  }

  const height = left - prefix + 1
  const width = right - prefix + 1
  // shortcut: mark the changed middle as a replacement when exact alignment would block the editor.
  const limited = height > 1 && width > 1 && height * width > 1_000_000
  if (limited || height === 1 || width === 1) {
    for (let i = prefix; i < left; i++) lines.push({ kind: 'removed', text: before[i]! })
    for (let j = prefix; j < right; j++) lines.push({ kind: 'added', text: after[j]! })
  } else {
    const lengths = new Uint32Array(height * width)
    for (let i = height - 2; i >= 0; i--) {
      for (let j = width - 2; j >= 0; j--) {
        const cell = i * width + j
        lengths[cell] = before[prefix + i] === after[prefix + j] ? lengths[cell + width + 1]! + 1
          : Math.max(lengths[cell + width]!, lengths[cell + 1]!)
      }
    }
    let i = 0
    let j = 0
    while (i < height - 1 || j < width - 1) {
      const cell = i * width + j
      if (i < height - 1 && j < width - 1 && before[prefix + i] === after[prefix + j]) {
        lines.push({ kind: 'unchanged', text: before[prefix + i++]! })
        j++
      } else if (i < height - 1 && (j === width - 1 || lengths[cell + width]! >= lengths[cell + 1]!)) {
        lines.push({ kind: 'removed', text: before[prefix + i++]! })
      } else {
        lines.push({ kind: 'added', text: after[prefix + j++]! })
      }
    }
  }
  for (let i = left; i < before.length; i++) lines.push({ kind: 'unchanged', text: before[i]! })
  return { lines, limited }
}
