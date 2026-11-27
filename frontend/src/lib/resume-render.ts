import { resumeThemeCss } from './resume-theme.ts'

const pageWidthMm = 210
const pageHeightMm = 297
const pageMarginMm = 16

export const resumePageWidth = `${pageWidthMm}mm`
export const resumePageHeight = `${pageHeightMm}mm`
export const resumePageMargin = `${pageMarginMm}mm`
export const resumePageContentHeightPx = (pageHeightMm - pageMarginMm * 2) * 96 / 25.4
export const resumeCompactClass = 'resume-body-compact'
export const resumeFontSizePt = 11
export const resumeLineHeight = 1.5
export const resumeCompactFontSizePt = 10
export const resumeCompactLineHeight = 1.35

export const resumeLayoutCss = `
  .resume-body {
    width: ${pageWidthMm - pageMarginMm * 2}mm;
    margin-inline: auto;
    font: ${resumeFontSizePt}pt/${resumeLineHeight} system-ui, sans-serif;
    color: #161616;
    overflow-wrap: anywhere;
  }
  .resume-body.${resumeCompactClass} { font-size: ${resumeCompactFontSizePt}pt; line-height: ${resumeCompactLineHeight}; }
  ${resumeThemeCss}
  ${[1, 2, 3].map(level => {
    const prefix = '--resume-h' + level
    return `.resume-body h${level} {
      margin: var(${prefix}-before) 0 var(${prefix}-after);
      font-size: var(${prefix}-size);
      font-weight: var(${prefix}-weight);
      color: var(${prefix}-color);
      border-bottom: var(${prefix}-rule);
      padding-bottom: var(${prefix}-padding);
      break-after: avoid;
    }`
  }).join('\n')}
  .resume-body h1 { line-height: 1.33; letter-spacing: -0.025em; }
  .resume-body :is(p, pre, ul, ol, figure, table, .resume-math-block) { margin: 0.5em 0; }
  .resume-body > :first-child { margin-top: 0; }
  .resume-body > :last-child { margin-bottom: 0; }
  .resume-body p { orphans: 2; widows: 2; }
  .resume-body hr { margin: 0.75em 0; border: 0; border-top: 1px solid var(--resume-rule-color); }
  .resume-body :is(ul, ol) { padding-inline-start: 1.6em; }
  .resume-body ul { list-style: disc; }
  .resume-body ol { list-style: decimal; }
  .resume-body :is(tr, li, figure, pre, .resume-math-block) { break-inside: avoid; }
  .resume-body pre { white-space: pre-wrap; padding: 0.5em; background: #9992; }
  .resume-body code { font: 1em ui-monospace, monospace; }
  .resume-body table { width: 100%; border-collapse: collapse; }
  .resume-body thead { break-inside: avoid; }
  .resume-body :is(th, td) { border: 1px solid #9998; padding: 0.25em 0.5em; text-align: left; }
  .resume-body th { background: #9992; font-weight: 600; }
  .resume-body a { color: var(--resume-link-color); text-decoration: underline; }
  .resume-body img { display: block; max-width: 100%; max-height: 208px; width: auto; height: auto; }
  .resume-body figcaption { opacity: 0.75; }
  .resume-body .resume-math-block { display: block; overflow-x: auto; }
`

export function isResumeImageDataUrl(value: string | undefined): value is string {
  return typeof value === 'string' && /^data:image\/(jpeg|png);base64,[A-Za-z0-9+/]+={0,2}$/.test(value)
}

export async function resumeImageDataUrl(blob: Blob): Promise<string> {
  const bitmap = await createImageBitmap(blob)
  try {
    const canvas = document.createElement('canvas')
    canvas.width = Math.min(bitmap.width, 600)
    canvas.height = Math.max(1, Math.round(bitmap.height * canvas.width / bitmap.width))
    const context = canvas.getContext('2d')
    if (!context) throw new Error('图片编码失败')
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    const url = canvas.toDataURL(blob.type === 'image/jpeg' ? 'image/jpeg' : 'image/png', 0.9)
    if (!isResumeImageDataUrl(url)) throw new Error('图片编码失败')
    return url
  } finally {
    bitmap.close()
  }
}
