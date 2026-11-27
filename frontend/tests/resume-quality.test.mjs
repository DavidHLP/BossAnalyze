import { test } from 'node:test'
import assert from 'node:assert/strict'
import { analyzeResumeQuality } from '../src/lib/resume-quality.ts'

test('empty body reports no suggestions so the panel can show a positive empty state', () => {
  for (const markdown of ['   \n', '# 张三\n']) {
    const report = analyzeResumeQuality(markdown)
    assert.equal(report.hasContent, false)
    assert.deepEqual(report.items, [])
    assert.equal(report.wordCount.total, 0)
  }
})

const sectioned = [
  '# 张三',
  '',
  '## 个人信息',
  '- 姓名：张三',
  '- 电话：138-0000-0000',
  '- 邮箱：zhangsan@example.com',
  '',
  '## 学习经历',
  '- 2019 年 09 月 - 2023 年 06 月｜某某大学｜计算机科学与技术｜本科',
  '- 主修数据结构，获校级奖学金。',
  '',
  '## 实习经历',
  '- 2023 年 07 月 - 2023 年 09 月｜某某公司｜前端开发',
  '- 负责活动页开发，页面加载时间降低 40%。',
  '',
  '## 项目经验',
  '- 某某系统｜前端负责',
  '- 服务 2 万用户，接口耗时从 500 ms 降到 200 ms。',
  '',
  '## 技能',
  '- TypeScript、Vue、Node.js',
].join('\n')

test('section aliases such as 学习经历 are recognised instead of reported missing', () => {
  const report = analyzeResumeQuality(sectioned)
  assert.equal(report.items.some(item => item.kind === 'missing-section'), false)
})

test('English headings are recognised without false missing-section hints', () => {
  const english = [
    '# Zhang San',
    '',
    '## Contact',
    '- Email: zhang@example.com',
    '',
    '## Education',
    '- 2019 - 2023 | Example University | BSc',
    '- Graduated with honours.',
    '',
    '## Work Experience',
    '- 2023 | Example Ltd | Frontend Developer',
    '- Built dashboards used by 200 people.',
    '',
    '## Projects',
    '- Resume editor',
    '- Handled 10k documents.',
    '',
    '## Skills',
    '- TypeScript, Vue',
  ].join('\n')
  assert.equal(analyzeResumeQuality(english).items.some(item => item.kind === 'missing-section'), false)
})

test('missing sections are listed once, without flagging the sections that exist', () => {
  const report = analyzeResumeQuality(['# 张三', '', '## 个人信息', '- 姓名：张三'].join('\n'))
  const missing = report.items.filter(item => item.kind === 'missing-section').map(item => item.id)
  assert.deepEqual(missing, ['missing:education', 'missing:work', 'missing:projects', 'missing:skills'])
})

test('a recognised section with no content is reported as empty and can be located', () => {
  const markdown = [
    '# 张三',
    '',
    '## 个人信息',
    '- 姓名：张三',
    '',
    '## 技能',
    '',
    '## 教育经历',
    '- 2019 - 2023｜某某大学',
    '- 本科',
  ].join('\n')
  const report = analyzeResumeQuality(markdown)
  const empty = report.items.filter(item => item.kind === 'empty-section')
  assert.deepEqual(empty.map(item => item.id), ['empty:skills:0'])
  assert.equal(markdown.slice(empty[0].start, empty[0].end).startsWith('## 技能'), true)
  assert.match(empty[0].message, /技能/)
})

test('a section containing only a fenced code block is still empty', () => {
  const markdown = [
    '# 张三',
    '',
    '## 个人信息',
    '- 姓名：张三',
    '',
    '## 技能',
    '```ts',
    'const skills = []',
    '```',
  ].join('\n')
  const empty = analyzeResumeQuality(markdown).items.filter(item => item.kind === 'empty-section')
  assert.deepEqual(empty.map(item => item.id), ['empty:skills:0'])
})

test('an experience section with fewer than two bullet points suggests adding details', () => {
  const markdown = [
    '# 张三',
    '',
    '## 个人信息',
    '- 姓名：张三',
    '- 电话：138-0000-0000',
    '',
    '## 工作经历',
    '- 2023 年 07 月 - 至今｜某某公司｜前端开发工程师',
    '',
    '## 项目经历',
    '- 某某系统｜前端负责',
    '- 服务 2 万用户。',
    '',
    '## 技能',
    '- TypeScript',
  ].join('\n')
  const report = analyzeResumeQuality(markdown)
  const sparse = report.items.filter(item => item.kind === 'sparse-section')
  assert.deepEqual(sparse.map(item => item.id), ['sparse:work:0'])
  assert.match(sparse[0].message, /工作经历/)
})

test('bullets inside fenced code do not count as section points', () => {
  const markdown = [
    '# 张三',
    '',
    '## 个人信息',
    '- 姓名：张三',
    '',
    '## 工作经历',
    '- 2023 - 至今｜某某公司｜开发',
    '```md',
    '- 示例要点',
    '```',
  ].join('\n')
  const sparse = analyzeResumeQuality(markdown).items.filter(item => item.kind === 'sparse-section')
  assert.deepEqual(sparse.map(item => item.id), ['sparse:work:0'])
  assert.equal(sparse.length, 1)
})

test('quantified results are accepted without a quantify hint', () => {
  const report = analyzeResumeQuality(sectioned)
  assert.equal(report.items.some(item => item.kind === 'quantify'), false)
})

test('dates, phones, versions, URLs and code never count as quantified results', () => {
  const markdown = [
    '# 张三',
    '',
    '## 个人信息',
    '- 姓名：张三',
    '',
    '## 工作经历',
    '- 2019 年 09 月 - 2023 年 06 月｜某某公司｜前端开发',
    '- 电话 138-0000-0000，版本 v1.2.3，使用 Vue 3 与 `2.5` 倍缩放',
    '- 参考 https://example.com/v2/1234 与 09/2023 的排期',
    '- 负责页面开发与维护。',
    '```ts',
    'const port = 8080',
    '```',
    '',
    '## 项目经历',
    '- 某某系统｜前端负责',
    '- 负责交互与联调。',
  ].join('\n')
  const quantify = analyzeResumeQuality(markdown).items.filter(item => item.kind === 'quantify')
  assert.deepEqual(quantify.map(item => item.id), ['quantify:work:0', 'quantify:projects:0'])
})

test('word count uses an explicit basis: Han characters plus English words, punctuation separate', () => {
  const { wordCount } = analyzeResumeQuality('## 技能\n- TypeScript、Vue\n- 熟悉 Node.js。\n')
  assert.deepEqual(wordCount, { han: 4, words: 4, punctuation: 3, total: 8, characters: 26 })
})

test('word count excludes fenced code, inline code, URLs and email addresses', () => {
  const markdown = [
    '## 技能',
    '- 正文 10 条 TypeScript',
    '```ts',
    'const 汉字 = 1',
    '```',
    'https://example.com/abc123',
    '`inline 汉字`',
    'zhang@example.com',
  ].join('\n')
  const { wordCount } = analyzeResumeQuality(markdown)
  assert.deepEqual(wordCount, { han: 5, words: 1, punctuation: 0, total: 6, characters: 17 })
})

test('a year-shaped count with a unit is still a quantified result', () => {
  const markdown = [
    '# 张三',
    '',
    '## 个人信息',
    '- 姓名：张三',
    '',
    '## 工作经历',
    '- 2023 年 07 月 - 至今｜某某公司｜开发',
    '- 服务 2020 名用户，满意度 98%。',
  ].join('\n')
  assert.equal(analyzeResumeQuality(markdown).items.some(item => item.kind === 'quantify'), false)
})

test('English metrics with units count as quantified results', () => {
  const english = [
    '# Zhang San',
    '',
    '## Contact',
    '- Email: zhang@example.com',
    '',
    '## Work Experience',
    '- 2023 | Example Ltd | Frontend Developer',
    '- Built dashboards used by 200 people.',
    '',
    '## Projects',
    '- Resume editor',
    '- Handled 10k documents in 2 months.',
  ].join('\n')
  const report = analyzeResumeQuality(english)
  assert.equal(report.items.some(item => item.kind === 'quantify'), false)
})
