import { test } from 'node:test'
import assert from 'node:assert/strict'
import { escapeLatexText, inspectResumeTexTemplate, renderResumeLatexBlocks, resumeLatex, validateLatexMath } from '../src/lib/resume-tex.ts'
import { resumePreview } from '../src/lib/resumes.ts'

const tex = String.raw

test('exports raw Markdown math while escaping Pandoc-guarded currency as text', () => {
  const source = '报价 $100-$200，公式 $a_1$。\r\n\r\n$$\r\nE=mc^2\r\n$$'
  const result = resumeLatex('标题', source)
  assert.equal(result.ok, true)
  assert.ok(result.tex.includes(tex`\resumebody{报价 \$100-\$200，公式 \(a_1\)。}
\[
E=mc^2
\]`))

  const invalid = source + '\r\n中文😀 $x%$'
  const rejected = resumeLatex('标题', invalid)
  assert.equal(rejected.ok, false)
  assert.equal(rejected.errors.length, 1)
  const [{ line, column, start, end, formula, message }] = rejected.errors
  assert.deepEqual([line, column, invalid.slice(start, end), formula], [6, 6, '$x%$', 'x%'])
  assert.match(message, /\\%/)
})

test('escapes each TeX text special once, leaving Chinese unchanged', () => {
  assert.equal(escapeLatexText(tex`简历\{}$&#^_%~`), tex`简历\textbackslash{}\{\}\$\&\#\^{}\_\%\textasciitilde{}`)
})

const text = value => [{ kind: 'text', text: value }]

test('exports a synchronous ctexart document with reusable semantic body', () => {
  const blocks = [
    { kind: 'heading', level: 1, inline: text('姓名') },
    { kind: 'heading', level: 2, inline: text('工作') },
    { kind: 'heading', level: 3, inline: text('项目') },
    { kind: 'text', inline: [
      { kind: 'text', text: '经历 ' }, { kind: 'strong', text: 'A&B' },
      { kind: 'em', text: '100%' }, { kind: 'code', text: 'a_b' },
    ] },
    { kind: 'list', start: null, items: [text('一'), text('二')] },
    { kind: 'list', start: 1, items: [text('三')] },
    { kind: 'list', start: 4, items: [text('四')] },
    { kind: 'rule' },
  ]
  const body = tex`\resumeheading{1}{姓名}
\resumeheading{2}{工作}
\resumeheading{3}{项目}
\resumebody{经历 \textbf{A\&B}\emph{100\%}\texttt{a\_b}}
\begin{resumeitemize}
\item {一}
\item {二}
\end{resumeitemize}
\begin{resumeenumerate}
\item {三}
\end{resumeenumerate}
\begin{resumeenumerate}[start=4]
\item {四}
\end{resumeenumerate}
\resumerule`
  assert.equal(renderResumeLatexBlocks(blocks), body)
  const result = resumeLatex('标题 & 100%', blocks)
  assert.equal(result.ok, true)
  assert.deepEqual(result.warnings, [])
  assert.ok(result.tex.startsWith(tex`\documentclass[UTF8,a4paper,11pt,fontset=fandol]{ctexart}` + '\n'))
  assert.deepEqual(result.tex.split('\n').filter(line => line.startsWith(tex`\usepackage`)), [
    tex`\usepackage[margin=16mm]{geometry}`, tex`\usepackage{amsmath}`, tex`\usepackage{amssymb}`,
    tex`\usepackage{booktabs}`, tex`\usepackage{enumitem}`, tex`\usepackage{longtable}`,
    tex`\usepackage[hidelinks]{hyperref}`,
  ])
  for (const setting of [tex`\pagestyle{empty}`, tex`\setlength{\parindent}{0pt}`,
    tex`\setlength{\parskip}{0.35em}`, tex`\hypersetup{pdftitle={标题 \& 100\%}}`]) {
    assert.ok(result.tex.includes(setting), setting)
  }
  assert.ok(result.tex.endsWith(`${tex`\begin{document}`}\n${body}\n${tex`\end{document}`}\n`))
  assert.ok(resumeLatex('姓名', '# 姓名\r\n---').tex.includes(tex`\resumeheading{1}{姓名}` + '\n' + tex`\resumerule`))
})

test('modern exports a name and linked contact line with the shared semantic body', () => {
  const source = '\n# 林 & Lee\n\n[主页](https://example.com/portfolio) · [邮箱](mailto:lee@example.com)\n\n## 经历\n\n### 工程师\n\n正文'
  const result = resumeLatex('简历 & CV', source, { template: 'modern' })
  assert.equal(result.ok, true)
  assert.equal(result.effectiveTemplate, 'modern')
  assert.deepEqual(result.warnings, [])
  assert.ok(result.tex.includes(tex`\usepackage[margin=16mm]{geometry}`))
  assert.ok(result.tex.includes(tex`\pagestyle{empty}`))
  assert.ok(result.tex.includes(tex`\hypersetup{pdftitle={简历 \& CV}}`))
  assert.ok(result.tex.includes(tex`\centering\LARGE\sffamily\bfseries`))
  assert.ok(result.tex.includes(tex`\noindent\rule{\linewidth}{0.4pt}\par\nobreak`))
  assert.ok(result.tex.includes(tex`\raggedright\normalsize\bfseries #2`))
  assert.ok(result.tex.includes(tex`\begin{document}
\resumename{林 \& Lee}
\resumecontact{\href{https://example.com/portfolio}{主页} · \href{mailto:lee@example.com}{邮箱}}
\resumeheading{2}{经历}
\resumeheading{3}{工程师}
\resumebody{正文}
\end{document}`))
})

test('academic changes page layout but exports a byte-identical body to modern', () => {
  const source = '# 林\n\n[主页](https://example.com/)\n\n## 成果\n\n### 论文\n\n| 研究 | 指标 |\n| --- | --- |\n| A | $x^2$ |\n\n- 一\n- 二'
  const basic = resumeLatex('简历', source, { template: 'basic' })
  assert.deepEqual(basic, resumeLatex('简历', source))
  assert.equal(basic.effectiveTemplate, 'basic')
  const modern = resumeLatex('简历', source, { template: 'modern' })
  const academic = resumeLatex('简历', source, { template: 'academic' })
  assert.equal(academic.ok, true)
  assert.equal(academic.effectiveTemplate, 'academic')
  assert.ok(academic.tex.includes(tex`\usepackage[margin=20mm]{geometry}`))
  assert.ok(academic.tex.includes(tex`\pagestyle{plain}`))
  assert.ok(academic.tex.includes(tex`\ctexset{subsection={beforeskip=1.6em`))
  assert.ok(academic.tex.includes(tex`\subsection*{#2}\or\subsubsection*{#2}`))
  for (const result of [modern, academic]) {
    assert.ok(result.tex.startsWith(tex`\documentclass[UTF8,a4paper,11pt,fontset=fandol]{ctexart}` + '\n'))
    assert.deepEqual(result.tex.split('\n').filter(line => line.startsWith(tex`\usepackage`) && !line.endsWith('{geometry}')),
      basic.tex.split('\n').filter(line => line.startsWith(tex`\usepackage`) && !line.endsWith('{geometry}')))
    assert.ok(result.tex.includes(tex`\newcommand{\resumename}[1]{\par{\centering`))
    assert.ok(result.tex.includes(tex`\newcommand{\resumecontact}[1]{\par{\centering`))
    assert.ok(result.tex.includes(tex`\begin{longtable}`))
    assert.ok(result.tex.includes(tex`\begin{resumetable}{2}`))
  }
  const document = result => result.tex.slice(result.tex.indexOf(tex`\begin{document}`))
  assert.equal(document(academic), document(modern))
})

test('Markdown accepts a template ID and AST accepts source text as the third argument', () => {
  const source = '# 姓名\n\n[主页](https://example.com/)\n\n## 经历'
  for (const template of ['basic', 'modern', 'academic']) {
    const result = resumeLatex('标题', source, template)
    assert.equal(result.effectiveTemplate, template)
    assert.deepEqual(result, resumeLatex('标题', source, { template }))
  }
  for (const unknown of ['missing', '__proto__']) {
    assert.deepEqual(resumeLatex('标题', source, unknown), resumeLatex('标题', source))
  }
  const invalid = '# 姓名\r\n\r\n中文 $x%$'
  const result = resumeLatex('标题', resumePreview(invalid), invalid)
  assert.equal(result.ok, false)
  assert.deepEqual(result.errors.map(({ line, column }) => [line, column]), [[3, 4]])
})

test('professional templates fall back to the unchanged basic document without a unique first H1', () => {
  const cases = [
    ['', /第一块.*H1/],
    ['正文\n\n## 经历', /第一块.*H1/],
    ['---\n\n# 姓名\n\n## 经历', /第一块.*H1/],
    ['#  \n\n## 经历', /第一块.*H1/],
    ['# 姓名\n\n## 经历\n\n# 第二个姓名', /多个 H1/],
  ]
  for (const [source, reason] of cases) {
    const basic = resumeLatex('简历', source)
    assert.deepEqual(basic.warnings, [])
    for (const template of ['modern', 'academic']) {
      const result = resumeLatex('简历', source, { template })
      assert.equal(result.ok, true)
      assert.equal(result.effectiveTemplate, 'basic')
      assert.equal(result.tex, basic.tex)
      assert.equal(result.warnings.length, 1)
      assert.match(result.warnings[0], reason)
      assert.match(result.warnings[0], /基础模板/)
      assert.deepEqual(inspectResumeTexTemplate(resumePreview(source), template), {
        fallbackToBasic: true, warnings: result.warnings,
      })
    }
  }
})

test('template preflight allows missing contact and H2, keeping later blocks in order', () => {
  const noContact = '# 姓名\n\n## 经历\n\n项目正文'
  const noSections = '# 姓名\n\n[主页](https://example.com/)\n\n- 第一项\n- 第二项'
  for (const [source, reason] of [[noContact, /联系行/], [noSections, /H2.*原顺序/]]) {
    const blocks = resumePreview(source)
    assert.deepEqual(inspectResumeTexTemplate(blocks, 'basic'), { fallbackToBasic: false, warnings: [] })
    for (const template of ['modern', 'academic']) {
      const diagnosis = inspectResumeTexTemplate(blocks, template)
      assert.equal(diagnosis.fallbackToBasic, false)
      assert.equal(diagnosis.warnings.length, 1)
      assert.match(diagnosis.warnings[0], reason)
      const result = resumeLatex('简历', blocks, { template, sourceMarkdown: source })
      assert.equal(result.ok, true)
      assert.equal(result.effectiveTemplate, template)
      assert.deepEqual(result.warnings, diagnosis.warnings)
      if (source === noContact) {
        assert.ok(result.tex.includes(tex`\resumename{姓名}
\resumeheading{2}{经历}
\resumebody{项目正文}`))
      } else {
        assert.ok(result.tex.includes(tex`\resumename{姓名}
\resumecontact{\href{https://example.com/}{主页}}
\begin{resumeitemize}
\item {第一项}
\item {第二项}`))
      }
    }
  }
})

test('renders each code line as escaped text, including leading spaces, tabs and empty lines', () => {
  const body = renderResumeLatexBlocks([{ kind: 'code', text: '  x\t  y\r\n\r\n' + tex`\end{verbatim}` }])
  assert.equal(body, tex`{\ttfamily\raggedright
\noindent\strut \ \ x\ \ \ \ \ \ y\par
\noindent\strut \par
\noindent\strut \textbackslash{}end\{verbatim\}\par
}`)
})

test('uses TeX-safe URL serialization and falls back to text for unapproved destinations', () => {
  const links = [
    ['A&B', 'https://example.com/p_~^?a=$&b={x}%20#c'], ['邮件', 'mailto:me@example.com'],
    ['HTTP', 'http://example.com/p'], ['不允许#', 'javascript:void(0)'], ['纯文字', 'file:///tmp/test'],
    ['有凭据', 'https://user:pass@example.com/'], ['含控制字符', 'https://example.com/\u0000'],
  ].map(([label, href]) => ({ kind: 'link', text: label, href }))
  const escaped = tex`\href{https://example.com/p\_\%7E\%5E?a=\$\&b=\%7Bx\%7D\%20\#c}{A\&B}` +
    tex`\href{mailto:me@example.com}{邮件}\href{http://example.com/p}{HTTP}不允许\#纯文字有凭据含控制字符`
  assert.equal(renderResumeLatexBlocks([{ kind: 'text', inline: links }]),
    tex`\resumebody{${escaped}}`)
})

test('writes asset placeholders and single-line sanitized comments without image commands', () => {
  const result = resumeLatex('标题', [
    { kind: 'asset', fileName: '证书\r\nA%{x}.png', text: '文档 & 证书' },
    { kind: 'asset', fileName: 'photo.jpg', text: '' },
  ])
  assert.equal(result.ok, true)
  assert.ok(result.tex.includes(tex`% asset: 证书_A_x_.png (omitted)
\resumeasset{文档 \& 证书 · 本地导出未包含图片}
% asset: photo.jpg (omitted)
\resumeasset{图片素材 · 本地导出未包含图片}`))
  assert.ok(!result.tex.includes(tex`\includegraphics`))
  assert.equal(result.warnings.length, 2)
  assert.ok(result.warnings.every(warning => warning.includes('本地导出未包含图片')))
})

test('renders wrapping longtable columns with bold repeating headers and a final bottom rule', () => {
  const blocks = [{ kind: 'table', headers: [text('技能'), text('水平')], rows: [[text('A&B'), text('熟练')]] }]
  const result = resumeLatex('标题', blocks)
  assert.equal(result.ok, true)
  assert.ok(result.tex.includes(tex`\setlength{\tabcolsep}{\dimexpr\linewidth/#1/4\relax}`))
  assert.ok(result.tex.includes(tex`\begin{longtable}{*{#1}{p{\dimexpr\linewidth/#1-2\tabcolsep\relax}}}`))
  assert.equal(renderResumeLatexBlocks(blocks), tex`\begin{resumetable}{2}
\toprule
{\textbf{技能}} & {\textbf{水平}} \tabularnewline
\midrule
\endfirsthead
\toprule
{\textbf{技能}} & {\textbf{水平}} \tabularnewline
\midrule
\endhead
\bottomrule
\endlastfoot
{A\&B} & {熟练} \tabularnewline
\end{resumetable}`)
})

test('diagnoses each explicitly banned TeX command before whitelist checks', () => {
  const commands = `write immediate input include includeonly openin openout read readline closein closeout endinput
    special shipout catcode lccode uccode mathcode delcode sfcode csname endcsname scantokens def edef gdef xdef let
    futurelet expandafter afterassignment aftergroup newcommand renewcommand providecommand usepackage RequirePackage
    documentclass directlua latelua pdfximage lstinputlisting verbatiminput includegraphics`.split(/\s+/)
  for (const command of [...commands, 'write18', 'end {document}']) {
    const formula = '\\' + command
    assert.match(validateLatexMath(formula) ?? '', /危险/, formula)
  }
})

test('rejects structural hazards, with odd and even escape parity', () => {
  for (const [formula, diagnostic] of [
    ['x%y', /\\%/], [tex`x\\%`, /\\%/], ['x$y', /\$/], [tex`x\\$`, /\$/],
    ['x^^y', /\^\^/], ['x\u0000y', /控制字符/], ['x\u000by', /控制字符/], ['x\u007fy', /控制字符/],
    ['{x', /花括号/], ['x}', /花括号/], ['', /空/], [' \t\n', /空/], ['x'.repeat(4097), /4096/],
  ]) {
    assert.match(validateLatexMath(formula) ?? '', diagnostic, JSON.stringify(formula.slice(0, 32)))
  }
  for (const formula of [tex`100\%`, tex`\{x\}\$`, 'x'.repeat(4096)]) {
    assert.equal(validateLatexMath(formula), null)
  }
})

test('accepts whitelisted math, fixed-arity commands, Chinese text, and all permitted environments', () => {
  for (const formula of [
    tex`E=mc^2`, tex`\sum_{i=1}^{n}\frac{\alpha_i}{\sqrt[3]{x}}`,
    tex`\left\langle a,\left|b\right|\right\rangle`, tex`\left.\frac{1}{x}\right\vert`,
    tex`\text{中文 \% \& \{括号\} \textbackslash{} \_ \$ \# \^{}}`,
    tex`\operatorname{foo}\overset{a}{b}\underset{c}{d}\mathrm{X}\mathbb{R}\widehat{x}+\varphi+\Gamma`,
    ...['matrix', 'pmatrix', 'bmatrix', 'Bmatrix', 'vmatrix', 'Vmatrix', 'cases', 'aligned']
      .map(env => tex`\begin{${env}}x & \frac{1}{2}\\ y & z\end{${env}}`),
  ]) {
    assert.equal(validateLatexMath(formula), null, formula)
  }
})

test('rejects unknown commands, malformed arguments, crossed scopes, and misplaced alignment separators', () => {
  for (const formula of [
    tex`\unknown{x}`, tex`\begin{verbatim}x\end{verbatim}`, tex`\begin{filecontents}x\end{filecontents}`,
    tex`\begin{matrix}x\end{pmatrix}`, tex`\begin{matrix}x`, tex`\end{matrix}`,
    tex`\frac{1}2`, tex`\mathbb R`, tex`\sqrt[\alpha]{x}`, tex`\text 中文`, tex`\text{a & b}`,
    tex`\text{\sum}`, tex`\text{\^}`, tex`x\~`, tex`中文`, 'x#y', 'x&y', tex`x\\y`,
    tex`\left(x`, tex`x\right)`, tex`\left{x\right)}`,
    tex`\begin{aligned}{x&y}\end{aligned}`, tex`\begin{matrix}x\\[2pt]y\end{matrix}`,
    tex`{\begin{aligned}x}\end{aligned}`, tex`\begin{matrix}\left(x\end{matrix}\right)`, 'x\\',
  ]) {
    assert.notEqual(validateLatexMath(formula), null, formula)
  }
})

test('LaTeX preflight rejects a third cases column and resets the limit each row', () => {
  const formula = tex`\begin{cases}a&b&c\end{cases}`
  const source = `标题\n公式 $${formula}$`
  const result = resumeLatex('标题', source)
  assert.equal(result.ok, false)
  assert.equal(result.errors.length, 1)
  const [{ line, column, start, end, message }] = result.errors
  assert.deepEqual([line, column, source.slice(start, end)], [2, 4, `$${formula}$`])
  assert.match(message, /cases.*两列/)

  for (const valid of [
    tex`\begin{cases}a&b\end{cases}`,
    tex`\begin{cases}a\&b&c\\d&e\end{cases}`,
    tex`\begin{cases}\begin{matrix}a&b&c\end{matrix}&d\end{cases}`,
    tex`\begin{aligned}a&b&c\end{aligned}`,
  ]) {
    assert.equal(resumeLatex('标题', `$${valid}$`).ok, true, valid)
  }
})

test('LaTeX export rejects missing or repeated scripts on the same math atom', () => {
  const rejected = ['x^', 'x_', 'x_1_2', 'x^a^b', 'x^_1', 'x^{y_}', 'x_1^{a}_2',
    tex`\begin{matrix}x^&y\end{matrix}`, tex`\left(x_\right)`]
  const result = resumeLatex('标题', rejected.map(formula => `$${formula}$`).join('\n'))
  assert.equal(result.ok, false)
  assert.deepEqual(result.errors.map(error => error.formula), rejected)
  assert.ok(result.errors.every(error => /上标|下标/.test(error.message)))

  for (const formula of ['x_1^2', 'x^2_1', 'x_{1_2}', 'x^{}', 'x_1 y_2',
    tex`x^\alpha_1`, tex`x^{\frac{a}{b}}_1`, tex`\begin{matrix}x_1 & y_2\\ z_3\end{matrix}`]) {
    assert.equal(resumeLatex('标题', `$${formula}$`).ok, true, formula)
  }
})

function math(source, raw, formula, delimiterLength = 1) {
  const start = source.indexOf(raw)
  return { kind: 'math', tex: formula, raw, start, end: start + raw.length, bodyStart: start + delimiterLength }
}

test('renders validated math inline and as an LF-normalized display block', () => {
  const formula = tex`\begin{aligned}x&=1\\` + '\r\n \t\r\ny&=2' + tex`\end{aligned}`
  const source = `速度 $a_1$\r\n$$${formula}$$`
  const blocks = [
    { kind: 'text', inline: [...text('速度 '), math(source, '$a_1$', 'a_1')] },
    math(source, `$$${formula}$$`, formula, 2),
  ]
  const result = resumeLatex('标题', blocks, source)
  assert.equal(result.ok, true)
  const body = tex`\resumebody{速度 \(a_1\)}
\[
\begin{aligned}x&=1\\
y&=2\end{aligned}
\]`
  assert.equal(renderResumeLatexBlocks(blocks), body)
  assert.ok(result.tex.includes(body))
  assert.ok(!result.tex.includes('\r'))
})

test('all templates reject every unsafe formula at its UTF-16 selection, including CRLF and Chinese prefixes', () => {
  const source = '中文😀 $x%$\r\n第二行 $y$\r\n$$' + tex`\write18` + '$$\r\n四 $z&$'
  const blocks = [
    { kind: 'heading', level: 1, inline: mathInline('$x%$', 'x%') },
    { kind: 'list', start: null, items: [mathInline('$y$', 'y')] },
    math(source, tex`$$\write18$$`, tex`\write18`, 2),
    { kind: 'table', headers: [text('列')], rows: [[mathInline('$z&$', 'z&')]] },
  ]
  function mathInline(raw, formula) { return [math(source, raw, formula)] }
  const result = resumeLatex('标题', blocks, source)
  assert.equal(result.ok, false)
  assert.ok(!('tex' in result))
  assert.deepEqual(result.errors.map(({ line, column, start, end, formula }) =>
    [line, column, start, end, formula, source.slice(start, end)]), [
    [1, 6, 5, 9, 'x%', '$x%$'],
    [3, 1, 20, 32, tex`\write18`, tex`$$\write18$$`],
    [4, 3, 36, 40, 'z&', '$z&$'],
  ])
  assert.match(result.errors[0].message, /\\%/)
  assert.match(result.errors[1].message, /危险/)
  assert.match(result.errors[2].message, /不允许/)
  const contactBlocks = [blocks[0], { kind: 'text', inline: blocks[1].items[0] }, ...blocks.slice(2)]
  for (const template of ['basic', 'modern', 'academic']) {
    assert.deepEqual(resumeLatex('标题', contactBlocks, { template, sourceMarkdown: source }), result)
    assert.deepEqual(resumeLatex('标题', contactBlocks.slice(1), { template, sourceMarkdown: source }).errors, result.errors.slice(1))
    const invalidContact = resumeLatex('标题', '# 姓名\r\n\r\n中文 $x%$\r\n\r\n## 经历', { template })
    assert.equal(invalidContact.ok, false)
    assert.deepEqual(invalidContact.errors.map(({ line, column, formula }) => [line, column, formula]), [[3, 4, 'x%']])
  }
  assert.throws(() => renderResumeLatexBlocks(blocks), /\\%/)
})
