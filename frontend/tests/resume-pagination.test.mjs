import assert from 'node:assert/strict'
import test from 'node:test'
import { estimateResumePagination, resumePaginationSuggestions } from '../src/lib/resume-pagination.ts'
import { resumeHtml } from '../src/lib/resumes.ts'

test('the A4 print area distinguishes content on either side of the one-page boundary', () => {
  assert.equal(estimateResumePagination([{ height: 1000 }]).pages, 1)
  assert.equal(estimateResumePagination([{ height: 990 }, { height: 12 }]).pages, 2)
})

test('a heading and its image move together and retain the full image height', () => {
  const blocks = [
    { height: 900 },
    { height: 30, keepWithNext: true },
    { height: 208 },
    { height: 769 },
  ]
  assert.deepEqual(estimateResumePagination(blocks), { pages: 3, overflowLines: null })
})

test('a long table can break between rows and includes its header on continuation pages', () => {
  const blocks = [
    { height: 120, keepWithNext: true },
    ...Array.from({ length: 8 }, () => ({ height: 300, repeatOnBreak: 120 })),
  ]
  assert.equal(estimateResumePagination(blocks).pages, 4)
})

test('unusable measurements and oversized indivisible content leave the page count unconfirmed', () => {
  assert.equal(estimateResumePagination([{ height: NaN }]), null)
  assert.equal(estimateResumePagination([{ height: 1002 }]), null)
})

test('the text-only estimate reports overflow in print line heights', () => {
  const blocks = [{ height: 990 }, { height: 50, gapBefore: 8 }]
  assert.deepEqual(estimateResumePagination(blocks, 22), { pages: 2, overflowLines: 3 })
  assert.deepEqual(estimateResumePagination(blocks), { pages: 2, overflowLines: null })
})

test('compact HTML and print opt into 10pt without changing the resume content', () => {
  const markdown = '# 姓名\n\n正文'
  const standard = resumeHtml('简历', markdown)
  const compact = resumeHtml('简历', markdown, undefined, { compact: true })
  assert.match(compact, /\.resume-body\.resume-body-compact\s*\{[^}]*font-size:\s*10pt/)
  assert.match(compact, /@page\s*\{\s*size:\s*A4;/)
  assert.equal(compact.replace('class="resume-body resume-body-compact"', 'class="resume-body"'), standard)
})

test('suggestions locate the actual oldest job, adjacent points and long summary in CRLF Markdown', () => {
  const summary = '负责跨团队项目协调和交付管理，并持续梳理可量化的工作成果。'.repeat(4)
  const markdown = [
    '## 个人摘要', summary, '## 工作经历',
    '- 2020.04–2022.04｜公司甲｜工程师', '- 负责项目 A', '- 维护项目 B',
    '- 2018.04–2019.04｜公司乙｜工程师',
    '- 2023.04–至今｜公司丙｜工程师',
    '```md', '## 工作经历', '- 1990｜示例｜示例', '```',
  ].join('\r\n')
  const suggestions = resumePaginationSuggestions(markdown)
  assert.deepEqual(suggestions.map(({ kind, start, end }) => [kind, markdown.slice(start, end)]), [
    ['oldest-experience', '- 2018.04–2019.04｜公司乙｜工程师'],
    ['merge-points', '- 负责项目 A\r\n- 维护项目 B'],
    ['shorten-summary', summary],
  ])
})

test('specialized suggestions require recognizable resume content', () => {
  assert.deepEqual(resumePaginationSuggestions('## 个人摘要\n简短概述\n## 工作经历\n### 日期未知'), [])
})

test('contact fields and distinct education entries are skipped when looking for points to combine', () => {
  const markdown = [
    '## 个人信息', '- 姓名：示例', '- 电话：123',
    '## 教育经历', '- 2020–2024｜学校甲｜专业甲｜本科', '- 2024–2026｜学校乙｜专业乙｜硕士',
    '## 项目', '- 跟踪运营数据', '- 汇总运营数据',
  ].join('\n')
  const [suggestion] = resumePaginationSuggestions(markdown)
  assert.equal(markdown.slice(suggestion.start, suggestion.end), '- 跟踪运营数据\n- 汇总运营数据')
})
