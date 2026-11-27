import { resumeFormSectionKind, type ResumeFormSection } from './resume-form.ts'
import { resumePageContentHeightPx } from './resume-render.ts'
import { resumePreview, type ResumeInline, type ResumeSourceRange } from './resumes.ts'

export interface ResumePageBlock {
  height: number
  gapBefore?: number
  keepWithNext?: boolean
  repeatOnBreak?: number
}

export interface ResumePagination {
  pages: number
  overflowLines: number | null
}

export function estimateResumePagination(
  blocks: readonly ResumePageBlock[], textLineHeight?: number,
): ResumePagination | null {
  const pageHeight = resumePageContentHeightPx
  let pages = 1
  let used = 0
  let total = 0
  let group: ResumePageBlock | undefined
  let groupHeight = 0
  for (const [index, block] of blocks.entries()) {
    const { height, gapBefore = 0, repeatOnBreak = 0 } = block
    if (![height, gapBefore, repeatOnBreak].every(value => Number.isFinite(value) && value >= 0)) return null
    total += gapBefore + height
    if (group) {
      groupHeight += height + gapBefore
    } else {
      group = block
      groupHeight = height
    }
    if (block.keepWithNext && index < blocks.length - 1) continue

    const end = used + (group.gapBefore ?? 0) + groupHeight
    if (end > pageHeight && used > 0) {
      pages++
      // Gap before a block is discarded at an automatic break. A table header repeats there.
      used = (group.repeatOnBreak ?? 0) + groupHeight
    } else {
      used = end
    }
    if (used > pageHeight) return null
    group = undefined
    groupHeight = 0
  }
  const overflowLines = pages > 1 && textLineHeight && Number.isFinite(textLineHeight) && textLineHeight > 0
    ? Math.ceil((total - pageHeight) / textLineHeight) : 0
  return { pages, overflowLines: overflowLines > 0 ? overflowLines : null }
}

export function measureResumePagination(article: HTMLElement, textOnly: boolean): ResumePagination | null {
  const view = article.ownerDocument?.defaultView
  if (!view) return null
  const articleRect = article.getBoundingClientRect()
  const articleStyle = view.getComputedStyle(article)
  const zoom = articleRect.width / parseFloat(articleStyle.width)
  if (!Number.isFinite(zoom) || zoom <= 0 || article.scrollWidth > article.clientWidth + 1) return null
  if (article.ownerDocument.fonts?.status !== 'loaded' || article.querySelector('[role="note"]')) return null
  for (const figure of article.querySelectorAll('figure')) {
    const image = figure.querySelector('img')
    if (!image?.complete || !image.naturalHeight) return null
  }

  const blocks: ResumePageBlock[] = []
  let previousBottom = articleRect.top
  function append(top: number, bottom: number, options: Omit<ResumePageBlock, 'height' | 'gapBefore'> = {}) {
    blocks.push({ height: (bottom - top) / zoom, gapBefore: Math.max(0, (top - previousBottom) / zoom), ...options })
    previousBottom = bottom
  }

  for (const element of article.children) {
    const { top, bottom, height } = element.getBoundingClientRect()
    if (element instanceof view.HTMLTableElement) {
      const rows = Array.from(element.rows, row => row.getBoundingClientRect())
      const header = rows[1] ? (rows[1].top - top) / zoom : 0
      rows.forEach((row, index) => append(index === 0 ? top : row.top,
        index === rows.length - 1 ? bottom : row.bottom,
        { keepWithNext: index === 0 && rows.length > 1, repeatOnBreak: index > 1 ? header : 0 }))
    } else if (element.matches('ul, ol')) {
      Array.from(element.children).forEach((item, index, items) => {
        const rect = item.getBoundingClientRect()
        append(index === 0 ? top : rect.top, index === items.length - 1 ? bottom : rect.bottom)
      })
    } else if (element.matches('p')) {
      const style = view.getComputedStyle(element)
      const lineHeight = parseFloat(style.lineHeight)
      const lines = Math.round(height / zoom / lineHeight)
      if (element.querySelector('math') || !Number.isFinite(lines) || lines < 1 ||
        Math.abs(height / zoom - lines * lineHeight) > lines / 32) {
        if (articleRect.height / zoom > resumePageContentHeightPx) return null
        append(top, bottom)
        continue
      }
      const orphans = Number(style.getPropertyValue('orphans')) || 2
      const widows = Number(style.getPropertyValue('widows')) || 2
      for (let line = 1; line <= lines; line++) {
        append(top + height * (line - 1) / lines, top + height * line / lines,
          { keepWithNext: line < lines && (line < orphans || lines - line < widows) })
      }
    } else {
      append(top, bottom, { keepWithNext: view.getComputedStyle(element).breakAfter === 'avoid' })
    }
  }

  return estimateResumePagination(blocks, textOnly ? parseFloat(articleStyle.lineHeight) : undefined)
}

export type ResumePaginationSuggestion = ResumeSourceRange & {
  kind: 'oldest-experience' | 'merge-points' | 'shorten-summary'
}

function startMonth(label: string) {
  // A single year in a project or company name does not identify an employment period.
  const period = /(?:^|[^\d])((?:19|20)\d{2})(?:[./年-](0?[1-9]|1[0-2])(?!\d)月?)?\s*(?:[-–—~～至到]\s*)(?=(?:19|20)\d{2}|至今|今|present|current)/i.exec(label)
  return period ? Number(period[1]) * 12 + Number(period[2] ?? 0) : null
}

function renderedText(tokens: ResumeInline[]) {
  return tokens.map(token => token.kind === 'math' ? token.raw : token.text).join('')
}

export function resumePaginationSuggestions(source: string): ResumePaginationSuggestion[] {
  let section: { level: number; kind: ResumeFormSection | 'summary' } | undefined
  const experiences: (ResumeSourceRange & { month: number | null })[] = []
  let pendingExperience: typeof experiences[number] | undefined
  let pointRange: ResumeSourceRange | undefined
  let summaryRange: ResumeSourceRange | undefined

  resumePreview(source, (block, range) => {
    if (block.kind === 'heading') {
      const label = renderedText(block.inline).trim().replace(/\s+#+$/, '')
      pendingExperience = undefined
      if (section && block.level <= section.level) section = undefined
      if (section?.kind === 'work' && block.level === section.level + 1) {
        pendingExperience = { ...range, month: startMonth(label) }
        experiences.push(pendingExperience)
      } else if (block.level < 3) {
        const kind = resumeFormSectionKind(label)
        if (kind) section = { level: block.level, kind }
        else if (/^(摘要|个人摘要|个人简介|个人总结|个人概述|职业概述|自我评价|summary|professional summary|profile)$/i.test(label)) {
          section = { level: block.level, kind: 'summary' }
        }
      }
      return
    }

    if (block.kind === 'text') {
      const text = renderedText(block.inline).trim()
      if (pendingExperience?.month === null) pendingExperience.month = startMonth(text)
      if (section?.kind === 'summary' && text.length >= 80 &&
        (!summaryRange || range.end - range.start > summaryRange.end - summaryRange.start)) summaryRange = range
    }
    pendingExperience = undefined

    if (block.kind === 'list' && section?.kind !== 'basics' && section?.kind !== 'education') {
      let previousPointStart: number | undefined
      let index = 0
      for (const line of source.slice(range.start, range.end).matchAll(/[^\r\n]+/g)) {
        const start = range.start + line.index
        const end = start + line[0].length
        const fields = renderedText(block.items[index++]!).split('｜')
        if (section?.kind === 'work' && fields.length === 3) {
          experiences.push({ start, end, month: startMonth(fields[0]!) })
          previousPointStart = undefined
        } else {
          if (!pointRange && previousPointStart !== undefined) pointRange = { start: previousPointStart, end }
          previousPointStart = start
        }
      }
    }
  })

  const suggestions: ResumePaginationSuggestion[] = []
  if (experiences.length > 1 && experiences.every(entry => entry.month !== null)) {
    const oldest = experiences.reduce((first, entry) => entry.month! < first.month! ? entry : first)
    suggestions.push({ kind: 'oldest-experience', start: oldest.start, end: oldest.end })
  }
  if (pointRange) suggestions.push({ kind: 'merge-points', ...pointRange })
  if (summaryRange) suggestions.push({ kind: 'shorten-summary', ...summaryRange })
  return suggestions
}
