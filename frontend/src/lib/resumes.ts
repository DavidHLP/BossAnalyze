export function shortCommit(id: string | null | undefined) {
  return id?.slice(0, 8) || '—'
}

export function resumeTime(value: string | undefined) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 19)
}

export function resumeReadMessage(cause: unknown) {
  const code = cause && typeof cause === 'object' && 'code' in cause ? cause.code : undefined
  if (code === 403) return '当前账号无法访问这份简历。'
  if (code === 404) return '找不到这份简历。'
  return '暂时无法读取简历，请重试。'
}

export type ResumeListChange =
  | { kind: 'create'; id?: string; title: string; previousIds: ReadonlySet<string> }
  | { kind: 'rename'; id: string; title: string }
  | { kind: 'delete'; id: string }

export function resumeListReadback(change: ResumeListChange, items: { id: string; title: string }[]) {
  if (change.kind === 'create' && !change.id) {
    return items.some(item => item.title === change.title && !change.previousIds.has(item.id))
  }
  const item = items.find(item => item.id === change.id)
  return change.kind === 'delete' ? !item : item?.title === change.title
}

export interface ResumeDraft {
  content: string
  message: string
}

export function resumeDraftStore(storage: Storage, account: string, sessionStart: string, resumeId: string) {
  const prefix = 'bossanalyze:resume-draft:'
  const scope = prefix + JSON.stringify([account, sessionStart]) + ':'
  const key = scope + resumeId
  try {
    for (let i = storage.length - 1; i >= 0; i--) {
      const previous = storage.key(i)
      if (previous?.startsWith(prefix) && !previous.startsWith(scope)) storage.removeItem(previous)
    }
  } catch {
    // A blocked store cannot prevent editing or server saves.
  }
  return {
    read(): ResumeDraft | null {
      try {
        const raw = storage.getItem(key)
        if (raw === null) return null
        const value: unknown = JSON.parse(raw)
        if (value && typeof value === 'object' && 'content' in value && 'message' in value &&
          typeof value.content === 'string' && typeof value.message === 'string') {
          return { content: value.content, message: value.message }
        }
      } catch {
        // Malformed or unavailable local storage leaves the server copy in place.
      }
      return null
    },
    write(draft: ResumeDraft) {
      try { storage.setItem(key, JSON.stringify(draft)) } catch { /* The editor still holds the draft. */ }
    },
    clear() {
      try { storage.removeItem(key) } catch { /* A blocked store cannot be cleared. */ }
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

function markdownHref(value: string) {
  if (!/^(?:https?:\/\/|mailto:)/i.test(value) || /[\s\\]/.test(value)) return null
  try {
    const url = new URL(value)
    return url.username || url.password || !url.pathname ? null : url.href
  } catch {
    return null
  }
}

function resumeInline(text: string): ResumeInline[] {
  const tokens: ResumeInline[] = []
  const pattern = /(?<!`)`([^`]+)`(?!`)|\*\*([^*]+)\*\*|__([^_]+)__|\*([^*]+)\*|(?<![\w_])_([^_]+)_(?![\w_])|(!?)\[([^\[\]]*)\]\(([^()\s]+)\)/g
  let end = 0
  for (const match of text.matchAll(pattern)) {
    const [literal, code, strong, alternateStrong, emphasis, alternateEmphasis, image, label, destination] = match
    const start = match.index!
    if (start > end) tokens.push({ kind: 'text', text: text.slice(end, start) })
    const href = destination && !image ? markdownHref(destination) : null
    if (code) tokens.push({ kind: 'code', text: code })
    else if (strong || alternateStrong) tokens.push({ kind: 'strong', text: (strong ?? alternateStrong)! })
    else if (emphasis || alternateEmphasis) tokens.push({ kind: 'em', text: (emphasis ?? alternateEmphasis)! })
    else if (href) tokens.push({ kind: 'link', text: label || href, href })
    else tokens.push({ kind: 'text', text: literal })
    end = start + literal.length
  }
  if (end < text.length) tokens.push({ kind: 'text', text: text.slice(end) })
  return tokens
}

const listItem = /^\s*(?:([-*+])|(\d{1,9})[.)])[ \t]+(.+)$/

function tableCells(line: string) {
  if (!/(?<!\\)\|/.test(line)) return []
  return line.trim().replace(/^\||(?<!\\)\|$/g, '').split(/(?<!\\)\|/).map(cell => cell.trim().replaceAll('\\|', '|'))
}

export function resumePreview(markdown: string): PreviewBlock[] {
  // shortcut: this flat résumé Markdown subset omits nested blocks and reference links.
  const lines = markdown.split(/\r\n?|\n/)
  const blocks: PreviewBlock[] = []
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]!
    if (!line.trim()) continue
    const fence = /^\s*(`{3,}|~{3,})[^`]*$/.exec(line)
    if (fence) {
      const close = new RegExp('^\\s*' + fence[1]![0] + '{' + fence[1]!.length + ',}\\s*$')
      const body: string[] = []
      while (++i < lines.length && !close.test(lines[i]!)) body.push(lines[i]!)
      blocks.push({ kind: 'code', text: body.join('\n') })
      continue
    }
    const title = /^(#{1,3})\s+(.+)$/.exec(line)
    if (title) {
      blocks.push({ kind: 'heading', level: title[1]!.length as 1 | 2 | 3, inline: resumeInline(title[2]!) })
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
      const items = [resumeInline(bullet[3]!)]
      while (i + 1 < lines.length) {
        const next = listItem.exec(lines[i + 1]!)
        if (!next || ordered !== (next[2] !== undefined)) break
        i++
        items.push(resumeInline(next[3]!))
      }
      blocks.push({ kind: 'list', start: ordered ? Number(bullet[2]) : null, items })
      continue
    }
    const headers = tableCells(line)
    const separator = headers.length ? tableCells(lines[i + 1] ?? '') : []
    if (headers.length && headers.length === separator.length && separator.every(cell => /^:?-{3,}:?$/.test(cell))) {
      i++
      const rows: ResumeInline[][][] = []
      while (i + 1 < lines.length) {
        const cells = tableCells(lines[i + 1]!)
        if (!cells.length) break
        i++
        rows.push(headers.map((_, index) => resumeInline(cells[index] ?? '')))
      }
      blocks.push({ kind: 'table', headers: headers.map(resumeInline), rows })
      continue
    }
    blocks.push({ kind: 'text', inline: resumeInline(line) })
  }
  return blocks
}

export function assetMarkdown(fileName: string) {
  return '![' + fileName.replace(/[\[\]\\]/g, '') + '](asset:' +
    encodeURIComponent(fileName).replace(/[()]/g, char => char === '(' ? '%28' : '%29') + ')'
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

export function resumeHtml(title: string, content: string | PreviewBlock[], renderMath?: ResumeMathRender): string {
  const blocks = typeof content === 'string' ? resumePreview(content) : content
  const renderInline = (tokens: ResumeInline[]) => inlineHtml(tokens, renderMath)
  const body = blocks.map(block => {
    switch (block.kind) {
      case 'heading': return '<h' + block.level + '>' + renderInline(block.inline) + '</h' + block.level + '>'
      case 'text': return '<p>' + renderInline(block.inline) + '</p>'
      case 'rule': return '<hr>'
      case 'asset': return '<p>' + escapeHtml(block.text || '图片素材') + ' · 本地导出未包含图片</p>'
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
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'">
<meta name="referrer" content="no-referrer">
<title>${escapeHtml(title)}</title>
<style>
  @page { size: A4; margin: 16mm; }
  :root { color: #161616; background: white; font: 11pt/1.5 system-ui, sans-serif; }
  body { max-width: 178mm; margin: 2rem auto; padding: 0 1rem; overflow-wrap: anywhere; }
  h1, h2, h3 { break-after: avoid; }
  p, pre, ul, ol { margin: 0.45rem 0; }
  pre { white-space: pre-wrap; }
  table { width: 100%; border-collapse: collapse; }
  th, td { border: 1px solid #777; padding: 0.25rem 0.5rem; text-align: left; }
  tr { break-inside: avoid; }
  a { color: inherit; }
  @media print { body { margin: 0; padding: 0; } }
</style>
<body>${body}</body></html>`
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
