import { test } from 'node:test'
import assert from 'node:assert/strict'
import { importResumeText, resumeSkeleton } from '../src/lib/resume-import.ts'
import { resumePreview } from '../src/lib/resumes.ts'

function textOf(blocks) {
  return blocks.flatMap(block => {
    if (block.kind === 'heading') return [block.inline.map(token => token.text ?? '').join('')]
    if (block.kind === 'text') return [block.inline.map(token => token.text ?? '').join('')]
    if (block.kind === 'list') return block.items.map(item => item.map(token => token.text ?? '').join(''))
    return []
  })
}

test('plain text import extracts the name, recognises sections and converts bullet symbols', () => {
  const imported = importResumeText([
    '张三',
    '电话：138-0000-0000',
    '',
    '教育经历',
    '• 2019 年 09 月 - 2023 年 06 月 某某大学 本科',
    '· 主修课程：数据结构',
    '1、 校级奖学金',
    '（1） 优秀毕业生',
    '① 英语六级',
    '',
    'Work Experience',
    '* 某某公司 前端开发工程师',
    '',
    '项目经验',
    '- 某某系统',
    '',
    '技能',
    '● Vue、TypeScript',
  ].join('\n'))

  assert.equal(imported.title, '张三')
  assert.equal(imported.content, [
    '# 张三',
    '电话：138-0000-0000',
    '',
    '## 教育经历',
    '- 2019 年 09 月 - 2023 年 06 月 某某大学 本科',
    '- 主修课程：数据结构',
    '- 校级奖学金',
    '- 优秀毕业生',
    '- 英语六级',
    '',
    '## Work Experience',
    '- 某某公司 前端开发工程师',
    '',
    '## 项目经验',
    '- 某某系统',
    '',
    '## 技能',
    '- Vue、TypeScript',
  ].join('\n'))

  const kinds = resumePreview(imported.content).map(block => block.kind)
  assert.equal(kinds.filter(kind => kind === 'heading').length, 5)
  assert.equal(kinds.filter(kind => kind === 'list').length, 4)
})

test('a date range is never turned into an ordered list', () => {
  const imported = importResumeText('李四\n2019. 09 - 2021. 06 某某公司\n2019.09 短期项目\n1. 备注一行')
  const blocks = resumePreview(imported.content)
  assert.deepEqual(blocks.map(block => block.kind), ['heading', 'text', 'text', 'text'])
  assert.deepEqual(textOf(blocks), ['李四', '2019. 09 - 2021. 06 某某公司', '2019.09 短期项目', '1. 备注一行'])
  assert.ok(imported.content.includes('2019\\. 09 - 2021. 06'))
})

test('literal table, emphasis and heading markers stay literal text', () => {
  const imported = importResumeText([
    '王五',
    '技能：Java | TypeScript | * SQL',
    '*重点* 与 # 号',
    '# 保留这一行',
    '| 姓名 | 年龄 |',
    '| --- | --- |',
  ].join('\n'))
  const blocks = resumePreview(imported.content)
  assert.ok(!blocks.some(block => block.kind === 'table'))
  assert.equal(blocks.some(block => block.kind === 'rule'), false)
  assert.deepEqual(textOf(blocks), [
    '王五',
    '技能：Java | TypeScript | * SQL',
    '*重点* 与 # 号',
    '# 保留这一行',
    '| 姓名 | 年龄 |',
    '| --- | --- |',
  ])
})

test('skeleton includes every requested section and replaces the title', () => {
  const content = resumeSkeleton('我的简历')
  assert.match(content, /^# 我的简历\n/)
  for (const heading of ['个人信息', '教育经历', '工作经历', '项目经历', '技能']) {
    assert.match(content, new RegExp('^## ' + heading + '$', 'm'))
  }
  assert.ok(resumePreview(content).some(block => block.kind === 'heading' && block.level === 2))
})

test('empty input produces no title or body', () => {
  assert.deepEqual(importResumeText('  \n \n'), { title: '', content: '' })
})
