import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  resumeCopyContent,
  resumeCopyFromServer,
  resumeCopySourceLabel,
  resumeCopyTitle,
  resumeCopyVerified,
} from '../src/lib/resume-copy.ts'

test('suggests an editable copy title without doubling the copy suffix', () => {
  assert.equal(resumeCopyTitle('张三的简历'), '张三的简历（副本）')
  assert.equal(resumeCopyTitle('  张三的简历  '), '张三的简历（副本）')
  assert.equal(resumeCopyTitle('张三的简历（副本）'), '张三的简历（副本）')
  assert.equal(resumeCopyTitle(''), '未命名简历（副本）')
  assert.equal(resumeCopyTitle('长'.repeat(200)).length, 120)
})

test('keeps the copied Markdown while turning private image references into text placeholders', () => {
  const source = [
    '# 张三',
    '',
    '![证件照](asset:%E8%AF%81%E4%BB%B6%E7%85%A7.png)',
    '',
    '- Vue',
    '![远程图](https://example.com/a.png)',
  ].join('\n')

  assert.deepEqual(resumeCopyContent(source), {
    content: [
      '# 张三',
      '',
      '［图片：证件照（副本未包含图片素材）］',
      '',
      '- Vue',
      '![远程图](https://example.com/a.png)',
    ].join('\n'),
    imageCount: 1,
  })
})

test('uses the decoded file name when an asset has no alt text and counts each reference', () => {
  const copied = resumeCopyContent('![](asset:a%20b.png)\n![](asset:c.png)')
  assert.equal(copied.content, '［图片：a b.png（副本未包含图片素材）］\n［图片：c.png（副本未包含图片素材）］')
  assert.equal(copied.imageCount, 2)
})

test('reads the copy body from the server even when the list snapshot is stale', async () => {
  const snapshot = { id: 'resume-1', content: '# 旧的列表正文' }
  const copied = await resumeCopyFromServer(snapshot.id, async id => {
    assert.equal(id, 'resume-1')
    return { content: '# 最新的服务器正文\n![旧图](asset:a.png)' }
  })
  assert.equal(copied.content, '# 最新的服务器正文\n［图片：旧图（副本未包含图片素材）］')
  assert.equal(copied.imageCount, 1)
})

test('states that the copy source is the saved server body, never editor-only changes', () => {
  assert.match(resumeCopySourceLabel(true), /服务器.*已保存/)
  assert.match(resumeCopySourceLabel(true), /不含.*未保存|未保存.*不/)
  assert.doesNotMatch(resumeCopySourceLabel(false), /未保存/)
})

test('accepts only a readback with the copied title, body and exactly one initial version', () => {
  const commits = [{ content: '# 正文' }]
  assert.equal(resumeCopyVerified({ title: '副本', content: '# 正文' }, '副本', '# 正文', commits), true)
  assert.equal(resumeCopyVerified({ title: '副本', content: null }, '副本', '', [{ content: '' }]), true)
  assert.equal(resumeCopyVerified({ title: '别的标题', content: '# 正文' }, '副本', '# 正文', commits), false)
  assert.equal(resumeCopyVerified({ title: '副本', content: '# 别的正文' }, '副本', '# 正文', commits), false)
  assert.equal(resumeCopyVerified({ title: '副本', content: '# 正文' }, '副本', '# 正文', []), false)
  assert.equal(resumeCopyVerified({ title: '副本', content: '# 正文' }, '副本', '# 正文', [...commits, ...commits]), false)
  assert.equal(resumeCopyVerified(null, '副本', '# 正文', commits), false)
})
