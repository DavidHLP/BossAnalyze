export type ResumeQualityKind = 'missing-section' | 'empty-section' | 'sparse-section' | 'quantify'

export interface ResumeQualityItem {
  id: string
  kind: ResumeQualityKind
  label: string
  message: string
  start: number
  end: number
}

export interface ResumeWordCount {
  han: number
  words: number
  punctuation: number
  total: number
  characters: number
}

export interface ResumeQualityReport {
  hasContent: boolean
  items: ResumeQualityItem[]
  wordCount: ResumeWordCount
}

type SectionId = 'basics' | 'education' | 'work' | 'projects' | 'skills'

const GROUPS: { id: SectionId; label: string; hint: string; aliases: string[] }[] = [
  {
    id: 'basics',
    label: '个人信息',
    hint: '电话、邮箱等联系方式',
    aliases: [
      '个人信息', '基本信息', '联系方式', '个人资料', '个人简介', '自我介绍', '自我评价', '关于我',
      'contact', 'contact information', 'personal information', 'personal details', 'profile', 'summary', 'about me',
    ],
  },
  {
    id: 'education',
    label: '教育经历',
    hint: '学校、专业与起止时间',
    aliases: [
      '教育经历', '教育背景', '教育', '学习经历', '求学经历', '学历',
      'education', 'educational background', 'academic background', 'academic experience', 'education experience',
    ],
  },
  {
    id: 'work',
    label: '工作经历',
    hint: '公司、职位、职责与结果',
    aliases: [
      '工作经历', '工作经验', '实习经历', '工作履历', '职业经历', '职业履历', '从业经历',
      'experience', 'work experience', 'professional experience', 'employment', 'employment history', 'work history',
      'internship', 'internships', 'internship experience',
    ],
  },
  {
    id: 'projects',
    label: '项目经历',
    hint: '项目名称、职责与结果',
    aliases: [
      '项目经历', '项目经验', '项目', '个人项目', '开源项目',
      'projects', 'project', 'project experience', 'personal projects', 'open source',
    ],
  },
  {
    id: 'skills',
    label: '技能',
    hint: '技能清单或证书',
    aliases: [
      '技能', '专业技能', '技能特长', '技术技能', '技能证书', '证书', '资质',
      'skills', 'skill', 'technical skills', 'certifications', 'certificates',
    ],
  },
]

const HEADING = /^ {0,3}(#{1,6})[ \t]+(.+?)[ \t]*#*[ \t]*$/
const FENCE = /^ {0,3}(`{3,}|~{3,})/
const BULLET = /^\s*(?:[-*+]|\d{1,3}[.)、]|[（(]\d{1,3}[）)]|[①-⑳])\s+/
const SPARSE_GROUPS = new Set<SectionId>(['education', 'work', 'projects'])
const QUANTIFY_GROUPS = new Set<SectionId>(['work', 'projects'])

const STRIP_PATTERNS = [
  /`[^`]*`/g,
  /<[^>]*>/g,
  /!?\[([^\]]*)\]\([^)]*\)/g,
  /(?:https?:\/\/|www\.)\S+/gi,
  /[\w.+-]+@[\w.-]+\.\w+/g,
  /\b(?:19|20)\d{2}\s*年(?:\s*\d{1,2}\s*月)?(?:\s*\d{1,2}\s*日)?/g,
  /\b(?:19|20)\d{2}\s*[-–—~至到/.]\s*(?:(?:19|20)\d{2}|\d{1,2})(?:\s*[-–—~至到/.]\s*\d{1,2})?/g,
  /\b\d{1,2}\s*\/\s*(?:19|20)\d{2}\b/g,
  /\b(?:0?[1-9]|1[0-2])\s*月(?:\s*\d{1,2}\s*日)?/g,
  /(?:\+?86[-\s]?)?1[3-9]\d{9}/g,
  /\b\d{3,4}[- ]\d{7,8}\b/g,
  /\b400[- ]\d{3}[- ]\d{4}\b/g,
  /\b[vV]\d+(?:\.\d+){0,2}\b/g,
  /\b\d+(?:\.\d+){2,}\b/g,
  /\b(?:version|版本)\s*\d+(?:\.\d+)*/gi,
  /\b(?:Vue|React|Node(?:\.js)?|Java|Python|TypeScript|JavaScript|Spring|MySQL|Redis|Kafka|Docker|Kubernetes|K8s|Excel|Windows|macOS|Android|iOS)\s*v?\d+(?:\.\d+)*/gi,
]

const QUANTIFIED = new RegExp([
  String.raw`\d+(?:\.\d+)?\s*[%％]`,
  String.raw`\d+(?:\.\d+)?\s*倍`,
  String.raw`\d+(?:\.\d+)?\s*(?:万|亿|千)`,
  String.raw`\d+(?:\.\d+)?\s*(?:人|名|位|个|项|条|次|款|台|套|篇|份|家|页|行|字|门|种|张|块|组|轮)`,
  String.raw`\d+(?:\.\d+)?\s*(?:个月|天|周|月|年|小时|分钟|秒|毫秒|ms|s|min|h|MB|GB|TB|KB|QPS|TPS|RPS)`,
  String.raw`[¥￥$]\s*\d+(?:\.\d+)?`,
  String.raw`\d+(?:\.\d+)?\s*(?:元|万元|美元)`,
  String.raw`\d+(?:\.\d+)?\s*(?:→|->|至|到)\s*\d+`,
  String.raw`\d+\s*\+`,
  String.raw`(?:前|TOP|Top|top)\s*\d+`,
  String.raw`百分之\s*[一二三四五六七八九十百零\d.]+`,
  String.raw`\d+(?:\.\d+)?\s*(?:users?|people|persons?|documents?|requests?|downloads?|orders?|customers?|clients?|projects?|seconds?|minutes?|hours?|days?|weeks?|months?|years?|k|K|M|B|x)\b`,
].join('|'), 'i')

function nonEmptyText(lines: Line[]) {
  return lines
    .filter(line => !line.heading)
    .map(line => line.text.replace(BULLET, '').trim())
    .filter(Boolean)
    .join('\n')
}

function hasQuantifiedResult(text: string) {
  const cleaned = STRIP_PATTERNS.reduce((value, pattern) => value.replace(pattern, ' '), text)
  return QUANTIFIED.test(cleaned)
}

interface Line {
  text: string
  start: number
  textEnd: number
  heading: { level: number; group: (typeof GROUPS)[number] | null } | null
}

function normalizeLabel(label: string) {
  return label.trim().toLowerCase().replace(/\s+/g, ' ').replace(/[:：]\s*$/, '').trim()
}

function groupFor(label: string) {
  const normalized = normalizeLabel(label)
  const bare = normalized.replace(/\s*[（(][^）)]*[）)]\s*$/, '').trim()
  return GROUPS.find(group => group.aliases.includes(normalized) || group.aliases.includes(bare)) ?? null
}

function splitLines(markdown: string): Line[] {
  const lines: Line[] = []
  let start = 0
  while (start <= markdown.length) {
    const newline = markdown.indexOf('\n', start)
    const end = newline === -1 ? markdown.length : newline
    const textEnd = end > start && markdown[end - 1] === '\r' ? end - 1 : end
    const text = markdown.slice(start, textEnd)
    const heading = HEADING.exec(text)
    lines.push({
      text,
      start,
      textEnd,
      heading: heading
        ? { level: heading[1].length, group: groupFor(heading[2]) }
        : null,
    })
    if (newline === -1) break
    start = newline + 1
  }
  return lines
}

interface Section {
  group: (typeof GROUPS)[number]
  start: number
  end: number
  lines: Line[]
}

function sectionsOf(lines: Line[]): Section[] {
  const sections: Section[] = []
  let fence: string | null = null
  for (let index = 0; index < lines.length; index++) {
    const line = lines[index]
    const fenceMatch = FENCE.exec(line.text)
    if (fence) {
      if (fenceMatch && fenceMatch[1][0] === fence[0] && fenceMatch[1].length >= fence.length) fence = null
      continue
    }
    if (fenceMatch) {
      fence = fenceMatch[1]
      continue
    }
    if (!line.heading?.group) continue
    let end = lines.length
    let innerFence: string | null = null
    for (let next = index + 1; next < lines.length; next++) {
      const candidate = lines[next]
      const candidateFence = FENCE.exec(candidate.text)
      if (innerFence) {
        if (candidateFence && candidateFence[1][0] === innerFence[0] &&
          candidateFence[1].length >= innerFence.length) innerFence = null
        continue
      }
      if (candidateFence) {
        innerFence = candidateFence[1]
        continue
      }
      if (candidate.heading && candidate.heading.level <= line.heading.level) {
        end = next
        break
      }
    }
    sections.push({
      group: line.heading.group,
      start: line.start,
      end: end < lines.length ? lines[end].start : lines[lines.length - 1].textEnd,
      lines: lines.slice(index + 1, end),
    })
  }
  return sections
}

// Counts body text only: fenced/inline code, URLs and email addresses are excluded.
// Han characters and English words each count once in `total`; punctuation is reported separately.
export function countResumeWords(markdown: string): ResumeWordCount {
  // The first H1 is the resume title and is not part of the body word count.
  const title = /^(?:[ \t]*\r?\n)* {0,3}#[ \t]+[^\r\n]*(?:\r?\n|$)/.exec(markdown)
  if (title) markdown = markdown.slice(title[0].length)
  let text = markdown
    .replace(/```[\s\S]*?(?:```|$)/g, ' ')
    .replace(/~~~[\s\S]*?(?:~~~|$)/g, ' ')
    .replace(/`[^`]*`/g, ' ')
    .replace(/!?\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/(?:https?:\/\/|www\.)\S+/gi, ' ')
    .replace(/[\w.+-]+@[\w.-]+\.\w+/g, ' ')
    .replace(/<[^>]*>/g, ' ')
  text = text.split(/\r?\n/).map(line => line
    .replace(/^ {0,3}#{1,6}\s+/, '')
    .replace(/^\s*(?:[-*+]|\d{1,3}[.)、]|[（(]\d{1,3}[）)]|[①-⑳])\s+/, '')
    .replace(/^\s*>\s?/, ''),
  ).join('\n').replace(/[*_~|]/g, ' ')
  const han = (text.match(/[㐀-䶿一-鿿豈-﫿]/g) ?? []).length
  const words = (text.match(/[A-Za-z][A-Za-z'’\-]*/g) ?? []).length
  const punctuation = (text.match(/[　-〿＀-￯.,!?;:'"()\[\]{}<>\/\\@#$%^&*+=_\-]/g) ?? []).length
  return {
    han,
    words,
    punctuation,
    total: han + words,
    characters: text.replace(/\s+/g, '').length,
  }
}

function contentOutsideFences(lines: Line[]) {
  const content: Line[] = []
  let fence: string | null = null
  for (const line of lines) {
    const fenceMatch = FENCE.exec(line.text)
    if (fence) {
      if (fenceMatch && fenceMatch[1][0] === fence[0] && fenceMatch[1].length >= fence.length) fence = null
      continue
    }
    if (fenceMatch) {
      fence = fenceMatch[1]
      continue
    }
    content.push(line)
  }
  return content
}

export function analyzeResumeQuality(markdown: string): ResumeQualityReport {
  const lines = splitLines(markdown)
  const sections = sectionsOf(lines)
  const present = new Set(sections.map(section => section.group.id))
  const hasContent = sections.length > 0 || contentOutsideFences(lines).some(line => {
    if (!line.text.trim()) return false
    // A lone H1 is the resume title, not body content; any other prose or subheading is content.
    return !line.heading || (line.heading.level > 1 && !line.heading.group)
  })

  if (!hasContent) {
    return { hasContent: false, items: [], wordCount: countResumeWords(markdown) }
  }

  const items: ResumeQualityItem[] = []
  const occurrences = new Map<SectionId, number>()
  for (const section of sections) {
    const index = occurrences.get(section.group.id) ?? 0
    occurrences.set(section.group.id, index + 1)
    const content = contentOutsideFences(section.lines)
    const empty = content.every(line => !line.text.trim() || line.heading)
    if (empty) {
      items.push({
        id: 'empty:' + section.group.id + ':' + index,
        kind: 'empty-section',
        label: section.group.label,
        message: '「' + section.group.label + '」章节还没有内容，可补充' + section.group.hint + '。',
        start: section.start,
        end: section.end,
      })
      continue
    }
    if (SPARSE_GROUPS.has(section.group.id)) {
      const bullets = content.filter(line => !line.heading && BULLET.test(line.text)).length
      if (bullets < 2) {
        items.push({
          id: 'sparse:' + section.group.id + ':' + index,
          kind: 'sparse-section',
          label: section.group.label,
          message: bullets === 0
            ? '「' + section.group.label + '」还没有分条要点，建议用 2–4 条要点分别写职责和结果。'
            : '「' + section.group.label + '」只有 1 条要点，建议再补充一条职责或结果。',
          start: section.start,
          end: section.end,
        })
      }
    }
    if (QUANTIFY_GROUPS.has(section.group.id) && !hasQuantifiedResult(nonEmptyText(content))) {
      items.push({
        id: 'quantify:' + section.group.id + ':' + index,
        kind: 'quantify',
        label: section.group.label,
        message: '「' + section.group.label + '」还没有可量化的数字，可补充规模、数量、比例或耗时等结果。',
        start: section.start,
        end: section.end,
      })
    }
  }

  items.push(...GROUPS
    .filter(group => !present.has(group.id))
    .map(group => ({
      id: 'missing:' + group.id,
      kind: 'missing-section' as const,
      label: group.label,
      message: '没有找到「' + group.label + '」章节，可补充' + group.hint + '。',
      start: markdown.length,
      end: markdown.length,
    })))
  items.sort((left, right) => left.start - right.start)

  return { hasContent: true, items, wordCount: countResumeWords(markdown) }
}
