import assert from 'node:assert/strict'
import test from 'node:test'
import { resumeHtml } from '../src/lib/resumes.ts'

const content = '# 陈小明\n\n## 项目经历\n\n正文 [作品](https://example.invalid)\n\n---'

test('old records and unrecognized themes export with the basic appearance', () => {
  const basic = resumeHtml('简历', content, undefined, { themeId: 'basic' })
  assert.equal(resumeHtml('简历', content), basic)
  assert.equal(resumeHtml('简历', content, undefined, { themeId: 'unknown' }), basic)
  assert.match(basic, /<article class="resume-body">/)
})

test('modern HTML and print share the theme without changing A4 or compact body sizing', () => {
  const html = resumeHtml('简历', content, undefined, { themeId: 'modern', compact: true })
  assert.match(html, /<article class="resume-body resume-body-compact" data-resume-theme="modern">/)
  assert.match(html, /\.resume-body\[data-resume-theme="modern"\][^{]*\{[^}]*--resume-h1-size:\s*22pt/)
  assert.match(html, /\.resume-body\s*\{[^}]*width:\s*178mm;[^}]*font:\s*11pt\/1\.5 /)
  assert.match(html, /\.resume-body\.resume-body-compact\s*\{[^}]*font-size:\s*10pt/)
  assert.match(html, /@page\s*\{\s*size:\s*A4;\s*margin:\s*16mm;/)
  assert.match(html, /<h1>陈小明<\/h1>\s*<h2>项目经历<\/h2>/)
})
