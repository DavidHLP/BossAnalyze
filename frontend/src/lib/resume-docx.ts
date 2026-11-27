import {
  BorderStyle, Document, ExternalHyperlink, HeadingLevel, LevelFormat, Packer, Paragraph, TextRun, UnderlineType,
  convertMillimetersToTwip, type IBaseParagraphStyleOptions, type NumberingDefinition,
} from 'docx'
import {
  resumeCompactFontSizePt, resumeCompactLineHeight, resumeFontSizePt, resumeLineHeight,
  resumePageHeight, resumePageMargin, resumePageWidth,
} from './resume-render.ts'
import { resolveResumeThemeId, resumeThemes, type ResumeThemeId } from './resume-theme.ts'
import { markdownHref, type PreviewBlock, type ResumeInline } from './resumes.ts'

const headings = [HeadingLevel.HEADING_1, HeadingLevel.HEADING_2, HeadingLevel.HEADING_3] as const

function xmlText(text: string) {
  return text.replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F\uD800-\uDFFF\uFFFE\uFFFF]/gu, '\uFFFD')
}

function sourceRuns(text: string) {
  return xmlText(text).split(/\r\n?|\n/).map((line, index) => new TextRun({ text: line, break: index ? 1 : undefined }))
}

function inlineRuns(inline: ResumeInline[]) {
  return inline.map(token => {
    if (token.kind === 'math') return new TextRun(xmlText(token.raw))
    const text = xmlText(token.text)
    if (token.kind === 'link') {
      const link = markdownHref(token.href)
      if (link) return new ExternalHyperlink({ link, children: [new TextRun({ text, style: 'Hyperlink' })] })
    }
    return new TextRun({
      text,
      bold: token.kind === 'strong' ? true : undefined,
      italics: token.kind === 'em' ? true : undefined,
    })
  })
}

export function resumeDocx(blocks: readonly PreviewBlock[], compact = false, themeId?: ResumeThemeId): Promise<Blob> {
  const theme = resumeThemes[resolveResumeThemeId(themeId)]
  const separator = { style: BorderStyle.SINGLE, size: 4, color: theme.ruleColor.slice(1) }
  const [heading1, heading2, heading3] = theme.headings.map(
    ({ size, weight, color, before, after, rule }, level): IBaseParagraphStyleOptions => ({
      run: { size: size * 2, sizeComplexScript: size * 2, bold: weight >= 600, color: color.slice(1) },
      paragraph: {
        outlineLevel: level,
        keepNext: true,
        spacing: { before: Math.round(before * size * 20), after: Math.round(after * size * 20) },
        border: rule ? { bottom: { ...separator, space: Math.round(size * 0.2) } } : undefined,
      },
    }),
  )
  const numbering: NumberingDefinition[] = []
  const paragraphs = blocks.flatMap((block, index) => {
    switch (block.kind) {
      case 'heading':
        return [new Paragraph({ heading: headings[block.level - 1], children: inlineRuns(block.inline) })]
      case 'text':
        return [new Paragraph({ children: inlineRuns(block.inline) })]
      case 'list': {
        if (block.start === null) {
          return block.items.map(item => new Paragraph({ bullet: { level: 0 }, children: inlineRuns(item) }))
        }
        const reference = 'list-' + index
        numbering.push({
          reference,
          levels: [{
            level: 0, format: LevelFormat.DECIMAL, text: '%1.', start: block.start,
            style: { paragraph: { indent: { left: 360, hanging: 360 } } },
          }],
        })
        return block.items.map(item => new Paragraph({
          numbering: { reference, level: 0 }, children: inlineRuns(item),
        }))
      }
      case 'table':
        return [block.headers, ...block.rows].map(row => new Paragraph({
          children: row.flatMap((cell, column) => column
            ? [new TextRun(' | '), ...inlineRuns(cell)]
            : inlineRuns(cell)),
        }))
      case 'asset':
        return [new Paragraph(xmlText('[图片：' + (block.text || block.fileName) + ']'))]
      case 'code':
        return [new Paragraph({ children: sourceRuns(block.text) })]
      case 'math':
        return [new Paragraph({ children: sourceRuns(block.raw) })]
      case 'rule':
        return [new Paragraph({ border: { bottom: separator } })]
    }
  })
  const margin = convertMillimetersToTwip(Number.parseInt(resumePageMargin))
  const fontSize = (compact ? resumeCompactFontSizePt : resumeFontSizePt) * 2
  const lineHeight = compact ? resumeCompactLineHeight : resumeLineHeight
  return Packer.toBlob(new Document({
    styles: { default: {
      document: {
        run: { size: fontSize, sizeComplexScript: fontSize },
        paragraph: { spacing: { line: Math.round(lineHeight * 240) } },
      },
      heading1, heading2, heading3,
      hyperlink: { run: { color: theme.linkColor.slice(1), underline: { type: UnderlineType.SINGLE } } },
    } },
    numbering: { config: numbering },
    sections: [{
      properties: {
        page: {
          size: {
            width: convertMillimetersToTwip(Number.parseInt(resumePageWidth)),
            height: convertMillimetersToTwip(Number.parseInt(resumePageHeight)),
          },
          margin: { top: margin, bottom: margin, left: margin, right: margin },
        },
      },
      children: paragraphs,
    }],
  }))
}
