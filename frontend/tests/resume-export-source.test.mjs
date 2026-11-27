import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  resumeExportContent,
  resumeExportFileBase,
  resumeExportLossNotes,
  resumeExportSourceLabel,
  resumeExportSummary,
  resumeExportUnavailableReason,
  resumeImageCount,
} from '../src/lib/resumes.ts'

const state = (overrides = {}) => ({
  source: 'current',
  dirty: false,
  savedContent: '# 服务器正文',
  title: '我的简历',
  imageCount: 0,
  imagesEmbedded: false,
  ...overrides,
})

test('labels each export source without passing edited content off as saved', () => {
  assert.equal(resumeExportSourceLabel('current', false), '当前内容（与服务器正文一致）')
  assert.equal(resumeExportSourceLabel('current', true), '当前内容（含未保存修改）')
  assert.equal(resumeExportSourceLabel('saved', true), '服务器正文（不含编辑区未保存修改）')
  assert.equal(resumeExportSourceLabel('save-first', false), '服务器正文（保存已回读确认）')
})

test('exports the content that matches the selected source', () => {
  assert.equal(resumeExportContent('current', '# 当前', '# 服务器'), '# 当前')
  assert.equal(resumeExportContent('saved', '# 当前', '# 服务器'), '# 服务器')
  assert.equal(resumeExportContent('save-first', '# 当前', '# 服务器'), '# 服务器')
  assert.equal(resumeExportContent('saved', '# 当前', null), '')
})

test('disables save-first until the save readback is confirmed, and saved without a server body', () => {
  assert.equal(resumeExportUnavailableReason('current', { dirty: true, savedContent: null }), null)
  assert.match(resumeExportUnavailableReason('save-first', { dirty: true, savedContent: '# 服务器' }) ?? '', /保存.*回读/)
  assert.match(resumeExportUnavailableReason('save-first', { dirty: false, savedContent: null }) ?? '', /服务器/)
  assert.equal(resumeExportUnavailableReason('save-first', { dirty: false, savedContent: '# 服务器' }), null)
  assert.match(resumeExportUnavailableReason('saved', { dirty: false, savedContent: null }) ?? '', /服务器/)
  assert.equal(resumeExportUnavailableReason('saved', { dirty: true, savedContent: '# 服务器' }), null)
})

test('counts unique asset images in the exported Markdown only', () => {
  assert.equal(resumeImageCount('![a](asset:a.png)\n![b](asset:b.png)\n![a](asset:a.png)'), 2)
  assert.equal(resumeImageCount('![remote](https://example.com/a.png)'), 0)
  assert.equal(resumeImageCount(''), 0)
})

test('builds the fixed summary line for source, unsaved state, images and file name', () => {
  assert.deepEqual(resumeExportSummary(state({ source: 'current', dirty: true, imageCount: 2 })), {
    source: '当前内容（含未保存修改）',
    unsaved: '有未保存修改',
    images: '2 张图片，未嵌入',
    fileName: '我的简历.pdf / .html / .md / .tex',
  })
  assert.deepEqual(resumeExportSummary(state({ source: 'saved', dirty: true, imagesEmbedded: true, imageCount: 1 })), {
    source: '服务器正文（不含编辑区未保存修改）',
    unsaved: '不含未保存修改',
    images: '1 张图片，已嵌入',
    fileName: '我的简历.pdf / .html / .md / .tex',
  })
  assert.equal(resumeExportSummary(state()).images, '无图片')
})

test('states per-format losses truthfully, including images, DOCX and .tex compilation', () => {
  const withImage = { imageCount: 2, imagesEmbedded: false }
  assert.deepEqual(resumeExportLossNotes('html', withImage), ['2 张图片以文字占位，未嵌入导出文件。'])
  assert.deepEqual(resumeExportLossNotes('pdf', { imageCount: 2, imagesEmbedded: true }), ['2 张图片已嵌入导出文件。'])
  assert.deepEqual(resumeExportLossNotes('markdown', withImage), ['Markdown 保留公式源码与图片引用，不嵌入图片文件。'])
  assert.match(resumeExportLossNotes('tex', withImage).join(''), /XeLaTeX/)
  assert.match(resumeExportLossNotes('docx', withImage).join(''), /公式.*图片|图片.*公式/)
  assert.match(resumeExportLossNotes('external', withImage).join(''), /只发送.*标题.*正文/)
})

test('uses a safe download base name instead of an empty or path-like title', () => {
  assert.equal(resumeExportFileBase('我的简历'), '我的简历')
  assert.equal(resumeExportFileBase('  a/b:c  '), 'a_b_c')
  assert.equal(resumeExportFileBase('   '), '简历')
})
