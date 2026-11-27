import assert from 'node:assert/strict'
import test from 'node:test'
import { assetMarkdown, resumeHtml } from '../src/lib/resumes.ts'

test('standalone HTML uses the A4 body width and typography in screen and print layouts', () => {
  const html = resumeHtml('简历', '# 姓名\n\n## 经历\n\n### 项目\n\n正文')
  assert.match(html, /@page\s*\{\s*size:\s*A4;\s*margin:\s*16mm;/)
  assert.match(html, /\.resume-body\s*\{[^}]*width:\s*178mm;[^}]*font:\s*11pt\/1\.5 /)
  assert.match(html, /<body><article class="resume-body">\s*<h1>姓名<\/h1>\s*<h2>经历<\/h2>\s*<h3>项目<\/h3>/)
  assert.match(html, /break-after:\s*avoid/)
})

test('HTML embeds only raster data URLs, describes unavailable images, and omits figures for ATS export', () => {
  const markdown = '# 姓名\n' + [
    assetMarkdown('7_photo.png').replace('7_photo.png]', '证件照 <正面>]'),
    assetMarkdown('7_missing.jpg'),
    assetMarkdown('7_remote.png'),
    assetMarkdown('7_svg.png'),
    '![远程](https://example.invalid/tracker.png)',
  ].join('\n')
  const photo = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aJ1sAAAAASUVORK5CYII='
  const images = {
    '7_photo.png': photo,
    '7_remote.png': 'https://example.invalid/tracker.png',
    '7_svg.png': 'data:image/svg+xml;base64,PHN2Zy8+',
  }
  const html = resumeHtml('简历', markdown, undefined, { images })
  assert.deepEqual([...html.matchAll(/<img src="([^"]+)" alt="([^"]*)">/g)].map(match => match.slice(1)), [
    [photo, '证件照 &lt;正面&gt;'],
  ])
  for (const file of ['7_missing.jpg', '7_remote.png', '7_svg.png']) {
    assert.ok(html.includes(`<figcaption>${file} · 图片无法加载</figcaption>`))
  }
  assert.match(html, /default-src 'none'; img-src data:/)
  assert.doesNotMatch(html, /src="(?:https?:|blob:)/)

  const ats = resumeHtml('简历', markdown, undefined, { images, includeImages: false })
  assert.match(ats, /<h1>姓名<\/h1>/)
  assert.doesNotMatch(ats, /<(?:img|figure|figcaption)\b|7_missing\.jpg/)
})

test('asset encoding caps raster width, preserves aspect ratio and releases the decoded image', async t => {
  const { resumeImageDataUrl } = await import('../src/lib/resume-render.ts')
  const blob = new Blob(['synthetic JPEG'], { type: 'image/jpeg' })
  const close = t.mock.fn()
  const bitmap = { width: 1200, height: 801, close }
  const canvas = {
    getContext: () => ({ drawImage(image, x, y, width, height) {
      assert.deepEqual([image, x, y, width, height], [bitmap, 0, 0, 600, 401])
    } }),
    toDataURL(type) {
      assert.deepEqual([this.width, this.height, type], [600, 401, 'image/jpeg'])
      return 'data:image/jpeg;base64,AAAA'
    },
  }
  globalThis.createImageBitmap = async input => {
    assert.equal(input, blob)
    return bitmap
  }
  globalThis.document = { createElement(tag) {
    assert.equal(tag, 'canvas')
    return canvas
  } }
  t.after(() => {
    delete globalThis.createImageBitmap
    delete globalThis.document
  })
  assert.equal(await resumeImageDataUrl(blob), 'data:image/jpeg;base64,AAAA')
  assert.equal(close.mock.callCount(), 1)
  canvas.toDataURL = () => 'data:,'
  await assert.rejects(resumeImageDataUrl(blob), /图片编码失败/)
  assert.equal(close.mock.callCount(), 2)
})
