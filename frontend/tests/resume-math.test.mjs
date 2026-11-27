import { test } from 'node:test'
import assert from 'node:assert/strict'
import { offsetToPosition, resumeHasMath, resumeHtml, resumeMathNodes, resumePreview } from '../src/lib/resumes.ts'

test('preview headings accept 0-3 leading spaces and clamp levels 4-6 to the smallest style', () => {
  const blocks = resumePreview('   # 标题\n#### 四级\n###### 六级')
  assert.deepEqual(blocks.map(block => [block.kind, block.level, block.inline]), [
    ['heading', 1, [{ kind: 'text', text: '标题' }]],
    ['heading', 3, [{ kind: 'text', text: '四级' }]],
    ['heading', 3, [{ kind: 'text', text: '六级' }]],
  ])
  assert.deepEqual(resumePreview('    # 四个空格仍是正文')[0], { kind: 'text', inline: [{ kind: 'text', text: '    # 四个空格仍是正文' }] })
  const html = resumeHtml('简历', '#### 四级')
  assert.match(html, /<h3>四级<\/h3>/)
  assert.doesNotMatch(html, /####/)
})

test('escaped Markdown never rewrites literal private-use glyphs', () => {
  assert.deepEqual(resumePreview('\uE000 x \\| y'), [
    { kind: 'text', inline: [{ kind: 'text', text: '\uE000 x | y' }] },
  ])
})

test('inline math uses the nearest unescaped dollar run and respects the currency guard', () => {
  for (const text of ['$100-$200', '$100–$200', '$5', '$100 and $200', '$ x$', '$x $', '$x$9',
    '$x$$y$', '中间 $$x$$ 文本', '$x$9 more $', '$x\ny$']) {
    const blocks = resumePreview(text)
    assert.equal(resumeHasMath(blocks), false, text)
    assert.equal(resumeMathNodes(blocks).length, 0, text)
    assert.deepEqual(blocks, text.split('\n').map(line => ({ kind: 'text', inline: [{ kind: 'text', text: line }] })), text)
  }

  for (const [text, tex] of [
    ['$2$', ['2']], ['$2x$', ['2x']], ['$a_1$ 与 $b_2$', ['a_1', 'b_2']], ['$x$９', ['x']],
    ['$bad$$ tail $ok$', ['ok']], [String.raw`$x\$y$`, [String.raw`x\$y`]],
    [String.raw`$x\$$ next $ok$`, [String.raw`x\$`, 'ok']],
  ]) {
    const blocks = resumePreview(text)
    assert.equal(resumeHasMath(blocks), true, text)
    assert.deepEqual(resumeMathNodes(blocks).map(node => node.tex), tex, text)
  }
})

test('math and code take priority over emphasis and links', () => {
  const blocks = resumePreview('`$code$` **$x_y$** [标题 $label$](https://example.invalid) $before `code` after$ *em* **bold**')
  assert.deepEqual(blocks[0].inline.filter(token => token.kind !== 'text').map(token => token.kind),
    ['code', 'math', 'math', 'code', 'em', 'strong'])
  assert.deepEqual(resumeMathNodes(blocks).map(node => node.tex), ['x_y', 'label'])
  assert.deepEqual(resumePreview('[文档](https://example.invalid) $x$')[0].inline[0],
    { kind: 'link', text: '文档', href: 'https://example.invalid/' })
})

test('dollars in valid Markdown link destinations stay out of inline math', () => {
  const source = '# 姓名\n[主页](https://example.com/?a=$x$) · [邮箱](mailto:me@example.com)\n\n' +
    '公式 $y$，以及 [标签 $z$](https://example.com/?a=$query$)'
  const blocks = resumePreview(source)
  assert.deepEqual(blocks[1].inline, [
    { kind: 'link', text: '主页', href: 'https://example.com/?a=$x$' },
    { kind: 'text', text: ' · ' },
    { kind: 'link', text: '邮箱', href: 'mailto:me@example.com' },
  ])
  assert.deepEqual(resumeMathNodes(blocks).map(node => node.tex), ['y', 'z'])
  assert.match(resumeHtml('简历', blocks), /href="https:\/\/example\.com\/\?a=\$x\$"/)
  for (const target of ['file:///tmp/?q=$x$', 'https://example.com/?q=`$x$']) {
    assert.deepEqual(resumeMathNodes(resumePreview(`[链接](${target})`)).map(node => node.tex), ['x'])
  }
})

test('dollar escapes remove one backslash from plain text and preserve code and TeX', () => {
  const text = String.raw`\$5 \\\$6 \\$x$ ` + '`\\$code`'
  const [block] = resumePreview(text)
  assert.deepEqual(block.inline, [
    { kind: 'text', text: String.raw`$5 \\$6 \\` },
    { kind: 'math', tex: 'x', raw: '$x$', start: 12, end: 15, bodyStart: 13 },
    { kind: 'text', text: ' ' },
    { kind: 'code', text: String.raw`\$code` },
  ])
})

test('a literal dollar can sit beside an inline formula', () => {
  for (const [source, literal] of [[String.raw`\$$x$`, '$'], [String.raw`\$ $x$`, '$ ']]) {
    const start = literal.length + 1
    assert.deepEqual(resumePreview(source)[0].inline, [
      { kind: 'text', text: literal },
      { kind: 'math', tex: 'x', raw: '$x$', start, end: start + 3, bodyStart: start + 1 },
    ])
  }
  assert.equal(resumeHasMath(resumePreview(String.raw`\$$$x$`)), false)
})

test('inline offsets use original UTF-16 positions in headings and lists across mixed line endings', () => {
  const markdown = '# 中文😀 $x$\r\n- 技能 $y$\r- 经验 $z$\n1. 学历 $w$'
  const nodes = resumeMathNodes(resumePreview(markdown))
  assert.deepEqual(nodes, [
    { display: false, tex: 'x', raw: '$x$', start: 7, end: 10, bodyStart: 8 },
    { display: false, tex: 'y', raw: '$y$', start: 17, end: 20, bodyStart: 18 },
    { display: false, tex: 'z', raw: '$z$', start: 26, end: 29, bodyStart: 27 },
    { display: false, tex: 'w', raw: '$w$', start: 36, end: 39, bodyStart: 37 },
  ])
  assert.deepEqual(nodes.map(node => offsetToPosition(markdown, node.bodyStart)), [
    { line: 1, column: 9 }, { line: 2, column: 7 }, { line: 3, column: 7 }, { line: 4, column: 8 },
  ])
})

test('display math accepts exactly two delimiters and retains its source line endings', () => {
  const markdown = '前言\r\n \t$$ E=mc^2 $$\t\r\t$$ \r\n  α + β\r\n😀\r $$\t\n结尾'
  const blocks = resumePreview(markdown)
  assert.deepEqual(blocks.map(block => block.kind), ['text', 'math', 'math', 'text'])
  assert.deepEqual(resumeMathNodes(blocks), [
    { display: true, tex: ' E=mc^2 ', raw: '$$ E=mc^2 $$', start: 6, end: 18, bodyStart: 8 },
    { display: true, tex: '  α + β\r\n😀', raw: '$$ \r\n  α + β\r\n😀\r $$', start: 21, end: 41, bodyStart: 26 },
  ])
})

test('empty or unmatched display math leaves subsequent text and fences available', () => {
  for (const text of ['$$ $$', '$$$x$$$', '$$x$$$', '$$$x$$', '$$$$', String.raw`$$x\$$`, '$$\n \t\n$$',
    '前面 $$x$$', '$$x$$ 后面']) {
    assert.equal(resumeHasMath(resumePreview(text)), false, text)
  }

  assert.deepEqual(resumePreview('$$\n# 次行\n- 条目'), [
    { kind: 'text', inline: [{ kind: 'text', text: '$$' }] },
    { kind: 'heading', level: 1, inline: [{ kind: 'text', text: '次行' }] },
    { kind: 'list', start: null, items: [[{ kind: 'text', text: '条目' }]] },
  ])

  for (const fence of ['```', '~~~']) {
    const blocks = resumePreview(`$$\n仍是正文\n${fence}md\n$$\n$a$\n$$x$$\n${fence}\n结尾`)
    assert.deepEqual(blocks.map(block => block.kind), ['text', 'text', 'code', 'text'])
    assert.equal(blocks[2].text, '$$\n$a$\n$$x$$')
  }
})

test('table pipes split cells outside closed code and pipe escapes', () => {
  const markdown = [
    String.raw`| 中文😀 \| $\lvert v\rvert$ | 后列 |`,
    '| --- | --- |',
    String.raw`| $a\|b$ | ` + '`a|b` ' + String.raw`\| ` + '`c\\|d` |',
    '| $unclosed | 另一格 |',
    '| `unclosed | 独立 |',
    '$only|math$',
    '',
    '$only|math$',
  ].join('\r\n')
  const [table, rest] = resumePreview(markdown)
  assert.equal(table.kind, 'table')
  assert.equal(table.headers.length, 2)
  assert.deepEqual(table.headers[0][0], { kind: 'text', text: '中文😀 | ' })
  assert.deepEqual(table.rows[0][1], [
    { kind: 'code', text: 'a|b' }, { kind: 'text', text: ' | ' }, { kind: 'code', text: 'c|d' },
  ])
  assert.deepEqual(table.rows.slice(1), [
    [[{ kind: 'text', text: '$unclosed' }], [{ kind: 'text', text: '另一格' }]],
    [[{ kind: 'text', text: '`unclosed' }], [{ kind: 'text', text: '独立' }]],
    [[{ kind: 'text', text: '$only' }], [{ kind: 'text', text: 'math$' }]],
  ])
  assert.equal(rest.kind, 'text')
  assert.deepEqual(resumeMathNodes([rest]).map(node => node.tex), ['only|math'])

  const formulas = [String.raw`$\lvert v\rvert$`, String.raw`$a\|b$`]
  const nodes = resumeMathNodes([table])
  assert.deepEqual(nodes, formulas.map(raw => {
    const start = markdown.indexOf(raw)
    return { display: false, tex: raw.slice(1, -1), raw, start, end: start + raw.length, bodyStart: start + 1 }
  }))
  assert.deepEqual(nodes.map(node => offsetToPosition(markdown, node.start)), [
    { line: 1, column: 11 }, { line: 3, column: 3 },
  ])
})

test('table currency does not pair with the math opener in the next cell', () => {
  const source = '| 价格 | 公式 |\n| --- | --- |\n|$20|$x$|'
  const [table] = resumePreview(source)
  assert.equal(table.kind, 'table')
  const start = source.indexOf('$x$')
  assert.deepEqual(table.rows, [[
    [{ kind: 'text', text: '$20' }],
    [{ kind: 'math', tex: 'x', raw: '$x$', start, end: start + 3, bodyStart: start + 1 }],
  ]])
})

test('a table stops before display math or a code fence even when the next line contains a pipe', () => {
  const table = '| 前 | 后 |\n| --- | --- |\n| 1 | 2 |\n'
  for (const math of ['$$x|y$$', '$$\r\nx|y\r$$']) {
    const blocks = resumePreview(table + math)
    assert.deepEqual(blocks.map(block => block.kind), ['table', 'math'])
    assert.equal(blocks[0].rows.length, 1)
    assert.equal(blocks[1].tex, 'x|y')
  }
  const blocks = resumePreview(table + '```md|\n$code$\n```')
  assert.deepEqual(blocks.map(block => block.kind), ['table', 'code'])
  assert.equal(blocks[1].text, '$code$')
})

test('standalone HTML synchronously inserts MathML for Markdown and pre-parsed blocks', () => {
  const markdown = '# 公式 $x_1$\n$$E=mc^2$$'
  const inline = '<math><msub><mi>x</mi><mn>1</mn></msub></math>'
  const display = '<math display="block"><mi>E</mi></math>'
  for (const content of [markdown, resumePreview(markdown)]) {
    const formulas = []
    const html = resumeHtml('简历', content, (tex, displayMode) => {
      formulas.push([tex, displayMode])
      return displayMode ? display : inline
    })
    assert.deepEqual(formulas, [['x_1', false], ['E=mc^2', true]])
    assert.ok(html.includes(`<h1>公式 <span class="resume-math-inline">${inline}</span></h1>`))
    assert.ok(html.includes(`<div class="resume-math-block">${display}</div>`))
    assertOfflineHtml(html)
  }
})

test('standalone HTML escapes original formula markup when the renderer yields no MathML', () => {
  const markdown = '标记 $<script>&$\n$$\r\n<mrow>&\r\n$$'
  for (const render of [undefined, () => null, () => '']) {
    const html = resumeHtml('简历', markdown, render)
    assert.ok(html.includes('<p>标记 $&lt;script&gt;&amp;$</p>'))
    assert.ok(html.includes('<pre><code>$$\r\n&lt;mrow&gt;&amp;\r\n$$</code></pre>'))
    assertOfflineHtml(html)
  }
})

function assertOfflineHtml(html) {
  assert.ok(html.includes('<meta http-equiv="Content-Security-Policy" content="default-src \'none\'; img-src data:; style-src \'unsafe-inline\'; base-uri \'none\'; form-action \'none\'">'))
  assert.doesNotMatch(html, /<script\b|\bsrc=|url\(|font-src/i)
}
