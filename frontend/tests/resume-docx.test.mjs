import assert from 'node:assert/strict'
import test from 'node:test'
import { inflateRawSync } from 'node:zlib'
import { resumeDocx } from '../src/lib/resume-docx.ts'
import { resumePreview } from '../src/lib/resumes.ts'

function zipXml(zip, name) {
  const end = zip.lastIndexOf(Buffer.from('504b0506', 'hex'))
  let entry = zip.readUInt32LE(end + 16)
  for (let index = 0; index < zip.readUInt16LE(end + 10); index++) {
    const nameLength = zip.readUInt16LE(entry + 28)
    if (zip.toString('utf8', entry + 46, entry + 46 + nameLength) === name) {
      const header = zip.readUInt32LE(entry + 42)
      const start = header + 30 + zip.readUInt16LE(header + 26) + zip.readUInt16LE(header + 28)
      const data = zip.subarray(start, start + zip.readUInt32LE(entry + 20))
      return (zip.readUInt16LE(entry + 10) === 0 ? data : inflateRawSync(data)).toString('utf8')
    }
    entry += 46 + nameLength + zip.readUInt16LE(entry + 30) + zip.readUInt16LE(entry + 32)
  }
  assert.fail(`Missing DOCX entry: ${name}`)
}

test('exports editable Chinese text, headings and bullet lists in A4 page order', async () => {
  const blocks = resumePreview('# 陈小明\n\n## 项目经历\n\n负责前端开发\u0000𠮷\n\n- 工作一\n- 工作二')
  const blob = await resumeDocx(blocks)
  assert.equal(blob.type, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')
  const zip = Buffer.from(await blob.arrayBuffer())
  const xml = zipXml(zip, 'word/document.xml')
  assert.match(xml, /<w:pStyle w:val="Heading1"\/><\/w:pPr>.*?陈小明.*?<w:pStyle w:val="Heading2"\/>.*?项目经历.*?负责前端开发�𠮷/)
  assert.match(xml, /<w:numPr>.*?<\/w:numPr>.*?工作一.*?<w:numPr>.*?<\/w:numPr>.*?工作二/)
  assert.match(zipXml(zip, 'word/numbering.xml'), /<w:numFmt w:val="bullet"\/>/)
  assert.match(xml, /<w:pgSz w:w="1190[56]" w:h="1683[78]"/)
  assert.doesNotMatch(xml, /<w:cols[^>]*w:num="[2-9]/)
})

test('preserves safe links and ordered lists while flattening tables, images and formulas', async () => {
  const blocks = resumePreview([
    '### 项目详情',
    '[作品](https://example.invalid/?x=1&y=2) [拒绝](javascript:alert%281%29)',
    '',
    '3. 第三项',
    '4. 第四项',
    '',
    '| 技能 | 年份 |',
    '| --- | --- |',
    '| Vue | 2026 |',
    '',
    '![证书](asset:certificate.png)',
    '速度 $v_1$',
    '$$',
    'E=mc^2',
    'x=y',
    '$$',
  ].join('\n'))
  const zip = Buffer.from(await (await resumeDocx(blocks)).arrayBuffer())
  const xml = zipXml(zip, 'word/document.xml')
  const relationships = zipXml(zip, 'word/_rels/document.xml.rels')
  assert.match(xml, /<w:hyperlink [^>]+>.*?作品.*?<\/w:hyperlink>/)
  assert.match(relationships, /Target="https:\/\/example\.invalid\/\?x=1&amp;y=2" TargetMode="External"/)
  assert.doesNotMatch(relationships, /javascript:|certificate\.png/)
  assert.match(zipXml(zip, 'word/numbering.xml'), /<w:start w:val="3"\/><w:numFmt w:val="decimal"\/>/)
  const text = [...xml.matchAll(/<w:t\b[^>]*>([^<]*)<\/w:t>/g)].map(match => match[1]).join('')
  assert.match(text, /项目详情.*?第三项第四项技能 \| 年份Vue \| 2026\[图片：证书\]速度 \$v_1\$\$\$E=mc\^2x=y\$\$/)
  assert.match(xml, /E=mc\^2<\/w:t>(?:<\/w:r><w:r>)?<w:br\/><w:t[^>]*>x=y/)
  assert.doesNotMatch(xml, /<w:tbl\b|<w:drawing\b|<m:oMath/)
})

test('compact Word export uses 10pt text and 1.35 line spacing without changing the body', async () => {
  const blocks = resumePreview('# 简历\n\n工作经历')
  const standard = Buffer.from(await (await resumeDocx(blocks)).arrayBuffer())
  const compact = Buffer.from(await (await resumeDocx(blocks, true)).arrayBuffer())
  const defaults = zipXml(compact, 'word/styles.xml').match(/<w:docDefaults>.*?<\/w:docDefaults>/)?.[0]
  assert.match(defaults, /<w:sz w:val="20"\/>/)
  assert.match(defaults, /<w:spacing[^>]*w:line="324"/)
  assert.notEqual(zipXml(compact, 'word/styles.xml'), zipXml(standard, 'word/styles.xml'))
  assert.equal(zipXml(compact, 'word/document.xml'), zipXml(standard, 'word/document.xml'))
})
