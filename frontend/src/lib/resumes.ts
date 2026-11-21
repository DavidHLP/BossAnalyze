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

export function countDuplicateCommits(commits: { id: string }[]) {
  return commits.length - new Set(commits.map(commit => commit.id)).size
}

type PreviewBlock =
  | { kind: 'heading'; level: 1 | 2 | 3; text: string }
  | { kind: 'text' | 'bullet'; text: string }
  | { kind: 'rule' }
  | { kind: 'asset'; text: string; fileName: string }

export function resumePreview(markdown: string): PreviewBlock[] {
  return markdown.split(/\r?\n/).flatMap((line): PreviewBlock[] => {
    if (!line.trim()) return []
    const title = /^(#{1,3})\s+(.+)$/.exec(line)
    if (title) return [{ kind: 'heading', level: title[1]!.length as 1 | 2 | 3, text: title[2]! }]
    if (/^\s*---+\s*$/.test(line)) return [{ kind: 'rule' }]
    const asset = /^!\[([^\]]*)\]\(asset:([^)]+)\)$/.exec(line.trim())
    if (asset) {
      try {
        return [{ kind: 'asset', text: asset[1]!, fileName: decodeURIComponent(asset[2]!) }]
      } catch {
        return [{ kind: 'text', text: line }]
      }
    }
    const bullet = /^\s*[-*+]\s+(.+)$/.exec(line)
    return bullet ? [{ kind: 'bullet', text: bullet[1]! }] : [{ kind: 'text', text: line }]
  })
}

export function assetMarkdown(fileName: string) {
  return '![' + fileName.replace(/[\[\]\\]/g, '') + '](asset:' +
    encodeURIComponent(fileName).replace(/[()]/g, char => char === '(' ? '%28' : '%29') + ')'
}

export function exportDestination(config: string | undefined, mock = false) {
  if (mock) return '/api/mock/resume-export'
  if (!config) return null
  try {
    const url = new URL(config)
    return url.protocol === 'https:' && !url.username && !url.password ? url.href : null
  } catch {
    return null
  }
}

export function resumeMainline(head: string | null | undefined, commits: { id: string; parentCommits?: string[] }[]) {
  const parents = new Map(commits.map(commit => [commit.id, commit.parentCommits?.[0]]))
  const seen = new Set<string>()
  while (head && !seen.has(head)) {
    seen.add(head)
    head = parents.get(head)
  }
  return seen
}
