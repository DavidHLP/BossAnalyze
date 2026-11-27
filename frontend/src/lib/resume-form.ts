const BASIC_KEYS = new Map<string, keyof ResumeBasics>([
  ['姓名', 'name'], ['名字', 'name'], ['name', 'name'],
  ['电话', 'phone'], ['手机', 'phone'], ['手机号', 'phone'], ['联系电话', 'phone'], ['phone', 'phone'],
  ['tel', 'phone'], ['telephone', 'phone'], ['mobile', 'phone'],
  ['邮箱', 'email'], ['电子邮箱', 'email'], ['email', 'email'], ['e-mail', 'email'],
  ['城市', 'city'], ['所在城市', 'city'], ['现居', 'city'], ['现居地', 'city'], ['所在地', 'city'],
  ['location', 'city'], ['city', 'city'],
  ['求职意向', 'objective'], ['意向岗位', 'objective'], ['目标岗位', 'objective'], ['期望职位', 'objective'],
  ['objective', 'objective'], ['job objective', 'objective'], ['target position', 'objective'],
])

const SECTION_LABELS: Record<ResumeFormSection, Set<string>> = {
  basics: new Set([
    '个人信息', '基本信息', '联系方式', '个人资料', 'personal information', 'personal details', 'contact',
    'contact information',
  ]),
  education: new Set([
    '教育经历', '教育背景', '教育', 'education', 'educational background', 'academic background',
  ]),
  work: new Set([
    '工作经历', '工作经验', '实习经历', '工作履历', '职业经历', 'work experience', 'professional experience',
    'experience', 'employment', 'employment history', 'work history',
  ]),
}

const HEADING = /^ {0,3}(#{1,6})[ \t]+(.+?)[ \t]*#*[ \t]*$/
const FENCE = /^ {0,3}(`{3,}|~{3,})/
const BULLET = /^\s*[-*+]\s+(.*)$/

export type ResumeFormSection = 'basics' | 'education' | 'work'

export interface ResumeBasics {
  name: string
  phone: string
  email: string
  city: string
  objective: string
}

export interface ResumeEducationEntry {
  period: string
  school: string
  major: string
  degree: string
  details: string
}

export interface ResumeWorkEntry {
  period: string
  company: string
  role: string
  details: string
}

export interface ResumeFormValues {
  basics: ResumeBasics
  education: ResumeEducationEntry[]
  work: ResumeWorkEntry[]
}

interface SectionSlot {
  kind: ResumeFormSection
  heading: string
  body: string[]
  data: ResumeBasics | ResumeEducationEntry[] | ResumeWorkEntry[]
  extras: string[]
}

type FormPart = { type: 'raw'; lines: string[] } | { type: 'section'; slot: SectionSlot }

export interface ResumeFormDocument {
  source: string
  eol: string
  values: ResumeFormValues
  duplicateSections: ResumeFormSection[]
  parts: FormPart[]
}

export function emptyResumeBasics(): ResumeBasics {
  return { name: '', phone: '', email: '', city: '', objective: '' }
}

export function resumeFormValues(doc: ResumeFormDocument): ResumeFormValues {
  return {
    basics: { ...doc.values.basics },
    education: doc.values.education.map(entry => ({ ...entry })),
    work: doc.values.work.map(entry => ({ ...entry })),
  }
}

export function resumeFormSectionKind(label: string): ResumeFormSection | null {
  const normalized = label.trim().toLowerCase().replace(/\s+/g, ' ')
  for (const kind of ['basics', 'education', 'work'] as const) {
    if (SECTION_LABELS[kind].has(normalized)) return kind
  }
  return null
}

function parseBasics(body: string[]): { data: ResumeBasics; extras: string[] } {
  const data = emptyResumeBasics()
  const extras: string[] = []
  const seen = new Set<keyof ResumeBasics>()
  for (const line of body) {
    if (!line.trim()) continue
    const bullet = line.trim().replace(/^[-*+]\s+/, '')
    const match = /^(.*?)[：:]\s*(.*)$/.exec(bullet)
    const rawKey = match?.[1].trim().replace(/^(\*\*|__)(.*)\1$/, '$2').trim() ?? ''
    const field = BASIC_KEYS.get(rawKey.toLowerCase())
    if (match && field && !seen.has(field)) {
      seen.add(field)
      data[field] = match[2].trim()
    } else {
      extras.push(line)
    }
  }
  return { data, extras }
}

function parseEducation(body: string[]): { data: ResumeEducationEntry[]; extras: string[] } {
  const data: ResumeEducationEntry[] = []
  const extras: string[] = []
  let current: ResumeEducationEntry | null = null
  for (const line of body) {
    if (!line.trim()) continue
    const bullet = BULLET.exec(line)
    if (!bullet) {
      extras.push(line)
      continue
    }
    const content = bullet[1].trim()
    const segments = content.split('｜')
    if (segments.length === 4) {
      current = {
        period: segments[0].trim(),
        school: segments[1].trim(),
        major: segments[2].trim(),
        degree: segments[3].trim(),
        details: '',
      }
      data.push(current)
    } else if (current) {
      current.details = current.details ? current.details + '\n' + content : content
    } else {
      extras.push(line)
    }
  }
  return { data, extras }
}

function parseWork(body: string[]): { data: ResumeWorkEntry[]; extras: string[] } {
  const data: ResumeWorkEntry[] = []
  const extras: string[] = []
  let current: ResumeWorkEntry | null = null
  for (const line of body) {
    if (!line.trim()) continue
    const bullet = BULLET.exec(line)
    if (!bullet) {
      extras.push(line)
      continue
    }
    const content = bullet[1].trim()
    const segments = content.split('｜')
    if (segments.length === 3) {
      current = {
        period: segments[0].trim(),
        company: segments[1].trim(),
        role: segments[2].trim(),
        details: '',
      }
      data.push(current)
    } else if (current) {
      current.details = current.details ? current.details + '\n' + content : content
    } else {
      extras.push(line)
    }
  }
  return { data, extras }
}

function parseSlot(kind: ResumeFormSection, heading: string, body: string[]): SectionSlot {
  const parsed = kind === 'basics' ? parseBasics(body)
    : kind === 'education' ? parseEducation(body) : parseWork(body)
  return { kind, heading, body, data: parsed.data, extras: parsed.extras }
}

export function parseResumeForm(markdown: string): ResumeFormDocument {
  const eol = markdown.includes('\r\n') ? '\r\n' : '\n'
  const lines = markdown.split(/\r?\n/)
  const headings: { index: number; level: number; label: string; kind: ResumeFormSection | null }[] = []
  let fence: string | null = null
  for (let index = 0; index < lines.length; index++) {
    const line = lines[index]
    const fenceMatch = FENCE.exec(line)
    if (fence) {
      if (fenceMatch && fenceMatch[1][0] === fence[0] && fenceMatch[1].length >= fence.length) fence = null
      continue
    }
    if (fenceMatch) {
      fence = fenceMatch[1]
      continue
    }
    const heading = HEADING.exec(line)
    if (heading) {
      headings.push({
        index,
        level: heading[1].length,
        label: heading[2].trim(),
        kind: resumeFormSectionKind(heading[2]),
      })
    }
  }

  const occurrences = new Map<ResumeFormSection, number>()
  for (const heading of headings) {
    if (heading.level === 2 && heading.kind) {
      occurrences.set(heading.kind, (occurrences.get(heading.kind) ?? 0) + 1)
    }
  }
  const duplicateSections = (['basics', 'education', 'work'] as const)
    .filter(kind => (occurrences.get(kind) ?? 0) > 1)

  const values: ResumeFormValues = {
    basics: emptyResumeBasics(),
    education: [],
    work: [],
  }
  const parts: FormPart[] = []
  let cursor = 0
  for (let position = 0; position < headings.length; position++) {
    const heading = headings[position]
    const end = headings[position + 1]?.index ?? lines.length
    if (heading.index > cursor) parts.push({ type: 'raw', lines: lines.slice(cursor, heading.index) })
    if (heading.level === 2 && heading.kind && !duplicateSections.includes(heading.kind)) {
      const slot = parseSlot(heading.kind, lines[heading.index], lines.slice(heading.index + 1, end))
      parts.push({ type: 'section', slot })
      if (heading.kind === 'basics') values.basics = slot.data as ResumeBasics
      else if (heading.kind === 'education') values.education = slot.data as ResumeEducationEntry[]
      else values.work = slot.data as ResumeWorkEntry[]
    } else {
      parts.push({ type: 'raw', lines: lines.slice(heading.index, end) })
    }
    cursor = end
  }
  if (cursor < lines.length) parts.push({ type: 'raw', lines: lines.slice(cursor) })

  return { source: markdown, eol, values, duplicateSections, parts }
}

function sameValue(left: unknown, right: unknown) {
  return JSON.stringify(left) === JSON.stringify(right)
}

function trailingBlanks(lines: string[]) {
  let count = 0
  for (let index = lines.length - 1; index >= 0 && !lines[index].trim(); index--) count++
  return count
}

function detailLines(details: string) {
  return details.split(/\r?\n/).map(line => line.trim()).filter(Boolean).map(line => '- ' + line)
}

function renderBasics(data: ResumeBasics) {
  const lines: string[] = []
  const fields: [string, string][] = [
    ['姓名', data.name],
    ['电话', data.phone],
    ['邮箱', data.email],
    ['城市', data.city],
    ['求职意向', data.objective],
  ]
  for (const [label, value] of fields) {
    if (value.trim()) lines.push(`- ${label}：${value.trim()}`)
  }
  return lines
}

function renderEducation(entries: ResumeEducationEntry[]) {
  const lines: string[] = []
  for (const entry of entries) {
    const segments = [entry.period, entry.school, entry.major, entry.degree].map(value => value.trim())
    if (segments.some(Boolean)) lines.push('- ' + segments.join('｜'))
    lines.push(...detailLines(entry.details))
  }
  return lines
}

function renderWork(entries: ResumeWorkEntry[]) {
  const lines: string[] = []
  for (const entry of entries) {
    const segments = [entry.period, entry.company, entry.role].map(value => value.trim())
    if (segments.some(Boolean)) lines.push('- ' + segments.join('｜'))
    lines.push(...detailLines(entry.details))
  }
  return lines
}

function renderSlot(kind: ResumeFormSection, data: SectionSlot['data'] | ResumeFormValues['basics'] | ResumeEducationEntry[] | ResumeWorkEntry[], extras: string[]) {
  const lines = kind === 'basics' ? renderBasics(data as ResumeBasics)
    : kind === 'education' ? renderEducation(data as ResumeEducationEntry[])
      : renderWork(data as ResumeWorkEntry[])
  return [...lines, ...extras]
}

function valuesFor(values: ResumeFormValues, kind: ResumeFormSection) {
  if (kind === 'basics') return values.basics
  return kind === 'education' ? values.education : values.work
}

function hasContent(kind: ResumeFormSection, values: ResumeFormValues) {
  if (kind === 'basics') {
    const basics = values.basics
    return Boolean(basics.name.trim() || basics.phone.trim() || basics.email.trim() ||
      basics.city.trim() || basics.objective.trim())
  }
  const entries = kind === 'education' ? values.education : values.work
  return entries.some(entry => Object.values(entry).some(value => value.trim()))
}

function newSection(kind: ResumeFormSection, values: ResumeFormValues, eol: string) {
  const heading = kind === 'basics' ? '## 个人信息' : kind === 'education' ? '## 教育经历' : '## 工作经历'
  return [heading, ...renderSlot(kind, valuesFor(values, kind), [])].join(eol)
}

export function renderResumeForm(doc: ResumeFormDocument, values: ResumeFormValues): string {
  const parts = doc.parts.map(part => {
    if (part.type === 'raw') return part.lines.join(doc.eol)
    const { slot } = part
    if (doc.duplicateSections.includes(slot.kind) || sameValue(slot.data, valuesFor(values, slot.kind))) {
      return [slot.heading, ...slot.body].join(doc.eol)
    }
    const blanks = trailingBlanks(slot.body)
    return [slot.heading, ...renderSlot(slot.kind, valuesFor(values, slot.kind), slot.extras),
      ...Array<string>(blanks).fill('')].join(doc.eol)
  })

  const present = new Set(doc.parts.flatMap(part => part.type === 'section' ? [part.slot.kind] : []))
  const additions = (['basics', 'education', 'work'] as const)
    .filter(kind => !present.has(kind) && !doc.duplicateSections.includes(kind))
    .filter(kind => hasContent(kind, values))
    .map(kind => newSection(kind, values, doc.eol))

  const out = parts.join(doc.eol)
  if (!additions.length) return out
  return (out ? out.replace(/(?:\r?\n)+$/, '') + doc.eol + doc.eol : '') + additions.join(doc.eol + doc.eol)
}
