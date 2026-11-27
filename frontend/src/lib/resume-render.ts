const pageWidthMm = 210
const pageMarginMm = 16

export const resumePageWidth = `${pageWidthMm}mm`
export const resumePageHeight = '297mm'
export const resumePageMargin = `${pageMarginMm}mm`

export const resumeLayoutCss = `
  .resume-body {
    width: ${pageWidthMm - pageMarginMm * 2}mm;
    margin-inline: auto;
    font: 11pt/1.5 system-ui, sans-serif;
    overflow-wrap: anywhere;
  }
  .resume-body :is(h1, h2, h3) { margin: 0 0 0.4em; font-weight: 600; break-after: avoid; }
  .resume-body h1 { font-size: 18pt; line-height: 1.33; letter-spacing: -0.025em; }
  .resume-body :is(h2, h3) { margin-top: 0.8em; border-bottom: 1px solid #9996; padding-bottom: 0.2em; }
  .resume-body h2 { font-size: 12pt; }
  .resume-body h3 { font-size: 11pt; }
  .resume-body :is(p, pre, ul, ol, figure, table, .resume-math-block) { margin: 0.5em 0; }
  .resume-body > :first-child { margin-top: 0; }
  .resume-body > :last-child { margin-bottom: 0; }
  .resume-body p { orphans: 2; widows: 2; }
  .resume-body hr { margin: 0.75em 0; border: 0; border-top: 1px solid #9996; }
  .resume-body :is(ul, ol) { padding-inline-start: 1.6em; }
  .resume-body ul { list-style: disc; }
  .resume-body ol { list-style: decimal; }
  .resume-body :is(tr, li, figure, pre, .resume-math-block) { break-inside: avoid; }
  .resume-body pre { white-space: pre-wrap; padding: 0.5em; background: #9992; }
  .resume-body code { font: 1em ui-monospace, monospace; }
  .resume-body table { width: 100%; border-collapse: collapse; }
  .resume-body :is(th, td) { border: 1px solid #9998; padding: 0.25em 0.5em; text-align: left; }
  .resume-body th { background: #9992; font-weight: 600; }
  .resume-body a { color: inherit; text-decoration: underline; }
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
