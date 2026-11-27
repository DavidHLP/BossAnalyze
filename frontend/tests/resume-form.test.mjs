import { test } from 'node:test'
import assert from 'node:assert/strict'
import { parseResumeForm, renderResumeForm, resumeFormValues } from '../src/lib/resume-form.ts'

const source = [
  '# 张三',
  '',
  '## 个人信息',
  '- 姓名：张三',
  '- 电话：138-0000-0000',
  '- 邮箱：zhangsan@example.com',
  '- 城市：北京',
  '- 求职意向：前端开发工程师',
  '',
  '## 教育经历',
  '- 2019 年 09 月 - 2023 年 06 月｜某某大学｜计算机科学与技术｜本科',
  '- 主修数据结构、操作系统等课程。',
  '',
  '## 工作经历',
  '- 2023 年 07 月 - 至今｜某某公司｜前端开发工程师',
  '- 负责核心页面开发。',
  '',
  '## 项目经历',
  '- 表格与公式保持原文：| 指标 | 值 |',
].join('\n')

test('form parses the supported three sections without touching the rest', () => {
  const doc = parseResumeForm(source)
  assert.deepEqual(doc.values.basics, {
    name: '张三',
    phone: '138-0000-0000',
    email: 'zhangsan@example.com',
    city: '北京',
    objective: '前端开发工程师',
  })
  assert.deepEqual(doc.values.education, [{
    period: '2019 年 09 月 - 2023 年 06 月',
    school: '某某大学',
    major: '计算机科学与技术',
    degree: '本科',
    details: '主修数据结构、操作系统等课程。',
  }])
  assert.deepEqual(doc.values.work, [{
    period: '2023 年 07 月 - 至今',
    company: '某某公司',
    role: '前端开发工程师',
    details: '负责核心页面开发。',
  }])
  assert.deepEqual(doc.duplicateSections, [])
})

test('rendering unchanged form values returns the source byte for byte', () => {
  const doc = parseResumeForm(source)
  assert.equal(renderResumeForm(doc, resumeFormValues(doc)), source)
})

test('an untouched CRLF document keeps its line endings', () => {
  const crlf = source.replaceAll('\n', '\r\n')
  const doc = parseResumeForm(crlf)
  assert.equal(renderResumeForm(doc, resumeFormValues(doc)), crlf)
})

test('editing basics rewrites only the basic section and keeps other sections verbatim', () => {
  const doc = parseResumeForm(source)
  const values = resumeFormValues(doc)
  values.basics.name = '李四'
  values.basics.objective = ''

  const rendered = renderResumeForm(doc, values)
  assert.match(rendered, /^## 个人信息\n- 姓名：李四\n- 电话：138-0000-0000\n- 邮箱：zhangsan@example.com\n- 城市：北京\n\n/m)
  assert.ok(!rendered.includes('求职意向'))
  assert.ok(rendered.includes('## 教育经历\n- 2019 年 09 月 - 2023 年 06 月｜某某大学｜计算机科学与技术｜本科\n- 主修数据结构、操作系统等课程。'))
  assert.ok(rendered.includes('## 项目经历\n- 表格与公式保持原文：| 指标 | 值 |'))
  assert.match(rendered, /^# 张三\n\n## 个人信息/)
})

test('duplicate supported headings stay verbatim and are reported instead of rewritten', () => {
  const duplicated = [
    '# 张三',
    '',
    '## 工作经历',
    '- 2023 年 07 月 - 至今｜甲公司｜前端开发',
    '',
    '## 工作经历',
    '- 2021 年 01 月 - 2023 年 06 月｜乙公司｜测试',
  ].join('\n')
  const doc = parseResumeForm(duplicated)
  assert.deepEqual(doc.duplicateSections, ['work'])
  assert.deepEqual(doc.values.work, [])

  const values = resumeFormValues(doc)
  values.basics.name = '张三'
  const rendered = renderResumeForm(doc, values)
  assert.ok(rendered.includes('## 工作经历\n- 2023 年 07 月 - 至今｜甲公司｜前端开发\n\n## 工作经历\n- 2021 年 01 月 - 2023 年 06 月｜乙公司｜测试'))
  assert.ok(rendered.includes('\n\n## 个人信息\n- 姓名：张三'))

  values.work.push({ period: '2020 年', company: '丙公司', role: '开发', details: '' })
  assert.ok(!renderResumeForm(doc, values).includes('丙公司'))
})

test('headings inside fenced code never become form sections', () => {
  const fenced = [
    '# 标题',
    '',
    '## 个人信息',
    '- 姓名：张三',
    '',
    '```md',
    '## 工作经历',
    '- 不该被解析',
    '```',
  ].join('\n')
  const doc = parseResumeForm(fenced)
  assert.deepEqual(doc.values.work, [])
  assert.deepEqual(doc.duplicateSections, [])

  const values = resumeFormValues(doc)
  values.basics.city = '上海'
  const rendered = renderResumeForm(doc, values)
  assert.ok(rendered.endsWith('```md\n## 工作经历\n- 不该被解析\n```'))
})

test('unknown lines inside a supported section survive a form edit', () => {
  const withExtra = [
    '## 教育经历',
    '- 2019 年 09 月 - 2023 年 06 月｜某某大学｜计算机｜本科',
    '> 交换生经历，原文保留',
    '- 主修课程',
  ].join('\n')
  const doc = parseResumeForm(withExtra)
  const values = resumeFormValues(doc)
  values.education[0].degree = '硕士'
  const rendered = renderResumeForm(doc, values)
  assert.ok(rendered.includes('- 2019 年 09 月 - 2023 年 06 月｜某某大学｜计算机｜硕士'))
  assert.ok(rendered.includes('- 主修课程'))
  assert.ok(rendered.includes('> 交换生经历，原文保留'))
})

test('missing sections are generated in canonical order when values are filled', () => {
  const doc = parseResumeForm('# 李四\n')
  const values = resumeFormValues(doc)
  values.basics.name = '李四'
  values.basics.phone = '139-0000-0000'
  values.work.push({ period: '2024 年 01 月 - 至今', company: '某某公司', role: '开发', details: '' })

  assert.equal(renderResumeForm(doc, values), [
    '# 李四',
    '',
    '## 个人信息',
    '- 姓名：李四',
    '- 电话：139-0000-0000',
    '',
    '## 工作经历',
    '- 2024 年 01 月 - 至今｜某某公司｜开发',
  ].join('\n'))
})

test('an empty form and empty additions do not create sections', () => {
  const doc = parseResumeForm('# 标题\n')
  assert.equal(renderResumeForm(doc, resumeFormValues(doc)), '# 标题\n')
  const values = resumeFormValues(doc)
  values.work.push({ period: '', company: '', role: '', details: '' })
  assert.equal(renderResumeForm(doc, values), '# 标题\n')
})
