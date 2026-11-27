import {
  BorderStyle, Document, ExternalHyperlink, HeadingLevel, LevelFormat, Packer, Paragraph, TextRun,
  convertMillimetersToTwip, type NumberingDefinition,
} from 'docx'
import { resumePageHeight, resumePageMargin, resumePageWidth } from './resume-render.ts'
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

export function resumeDocx(blocks: readonly PreviewBlock[]): Promise<Blob> {
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
        return [new Paragraph({ border: { bottom: { style: BorderStyle.SINGLE, size: 4 } } })]
    }
  })
  const margin = convertMillimetersToTwip(Number.parseInt(resumePageMargin))
  return Packer.toBlob(new Document({
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
