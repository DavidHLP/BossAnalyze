import assert from 'node:assert/strict'
import { copyFile, mkdtemp, readdir, readFile, rm, symlink } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, relative } from 'node:path'
import test from 'node:test'
import { fileURLToPath, pathToFileURL } from 'node:url'
import {
  createResumeMathEngine, loadResumeMath, renderResumeMath, RESUME_MATH_MAX_LENGTH,
} from '../src/lib/resume-math.ts'

test('renderResumeMath denies trusted commands even if the renderer returns MathML', () => {
  const unsupported = { ok: false, message: '公式包含不受支持的命令。' }
  let trustDecision
  const denied = renderResumeMath((_, { trust }) => {
    trustDecision = trust({ command: '\\href', url: 'javascript:alert(1)' })
    return '<math></math>'
  }, String.raw`\href{javascript:alert(1)}{x}`, false)
  assert.equal(trustDecision, false)
  assert.deepEqual(denied, unsupported)
  for (const html of [
    '<math><a>x</a></math>', '<math><mtext href="https://example.com">x</mtext></math>',
  ]) {
    assert.deepEqual(renderResumeMath(() => html, 'x', false), unsupported)
  }
})

test('rejected links and invalid TeX fail locally, macros never leak between formulas', async () => {
  const engine = await loadResumeMath()
  const { default: katex } = await import('katex')
  const link = String.raw`\href{javascript:alert(1)}{x}`
  assert.deepEqual(renderResumeMath(katex.renderToString, link, false),
    { ok: false, message: '公式包含不受支持的命令。' })
  assert.equal(engine.renderOrNull(link, true), null)
  assert.equal(engine.renderOrNull('x^{', false), null)
  const literal = String.raw`\text{\textbackslash url}`
  assert.match(engine.renderOrNull(literal, false), /<mtext>\\url<\/mtext>/)
  const redLiteral = String.raw`\textcolor{#cc0000}{${literal}}`
  assert.match(engine.renderOrNull(redLiteral, false), /<mtext>\\url<\/mtext>/)
  assert.match(engine.renderOrNull(String.raw`\gdef\resumeMacro{x}\resumeMacro`, false), /<mi>x<\/mi>/)
  assert.equal(engine.renderOrNull(String.raw`\resumeMacro`, false), null)
  assert.match(engine.renderOrNull('y', false), /<mi>y<\/mi>/)
})

test('bounds each formula before rendering and accepts only MathML output', () => {
  assert.equal(RESUME_MATH_MAX_LENGTH, 4096)
  const calls = []
  const engine = createResumeMathEngine((tex, options) => {
    calls.push(options)
    return tex === 'x' ? '<span>x</span>' : '<math></math>'
  })
  for (const tex of ['', ' \t\n', '𝑥'.repeat(2048) + 'x']) assert.equal(engine.render(tex, false).ok, false)
  assert.equal(calls.length, 0)
  assert.equal(engine.renderOrNull('𝑥'.repeat(2048), true), '<math></math>')
  assert.equal(engine.renderOrNull('x', false), null)
  const { trust, ...options } = calls[0]
  assert.equal(typeof trust, 'function')
  assert.deepEqual(options, { output: 'mathml', displayMode: true, throwOnError: true,
    strict: 'error', maxExpand: 250, maxSize: 10, macros: {} })
  assert.notEqual(calls[0].macros, calls[1].macros)
})

test('a missing renderer can be loaded after it becomes available', async t => {
  const dir = await mkdtemp(join(tmpdir(), 'resume-math-retry-'))
  t.after(() => rm(dir, { recursive: true, force: true }))
  const modulePath = join(dir, 'resume-math.mts')
  await copyFile(new URL('../src/lib/resume-math.ts', import.meta.url), modulePath)
  const { loadResumeMath } = await import(pathToFileURL(modulePath).href)
  await assert.rejects(loadResumeMath(), { code: 'ERR_MODULE_NOT_FOUND' })
  await symlink(new URL('../node_modules', import.meta.url), join(dir, 'node_modules'), 'junction')
  assert.match((await loadResumeMath()).renderOrNull('x', false), /<math\b/)
})

test('only MathNode inserts raw markup, no source imports KaTeX styles or fonts', async () => {
  const root = fileURLToPath(new URL('../src/', import.meta.url))
  const sinks = []
  for (const entry of await readdir(root, { recursive: true, withFileTypes: true })) {
    if (!entry.isFile()) continue
    const path = join(entry.parentPath, entry.name)
    const source = await readFile(path, 'utf8')
    if (/v-html|innerHTML/.test(source)) sinks.push(relative(root, path).replaceAll('\\', '/'))
    assert.doesNotMatch(source, /katex[^'"\n]*(?:\.css|\.woff2)/i, path)
  }
  assert.deepEqual(sinks, ['components/resumes/MathNode.vue'])
})

test('preview keeps good inline math beside escaped failures and exposes block loading states', async t => {
  const { createServer } = await import('vite')
  const { h } = await import('vue')
  const { renderToString } = await import('vue/server-renderer')
  const vite = await createServer({ server: { middlewareMode: true, ws: false, watch: null }, optimizeDeps: { noDiscovery: true } })
  t.after(() => vite.close())
  const load = async name => (await vite.ssrLoadModule(`/src/components/resumes/${name}.vue`)).default
  const Inline = await load('ResumePreviewInline')
  const MathNode = await load('MathNode')
  const bad = { kind: 'math', tex: 'x^{', raw: '$<img>$' }
  const tex = String.raw`\href{javascript:alert(1)}{x}`
  const inline = await renderToString(h(Inline, {
    mathEngine: await loadResumeMath(), mathLoading: false, mathLoadFailed: false,
    tokens: [bad, { kind: 'math', tex, raw: `$${tex}$` }, { kind: 'math', tex: 'y', raw: '$y$' }],
  }))
  assert.doesNotMatch(inline, /<a(?:\s|>)/)
  assert.match(inline, /<mi>y<\/mi>/)
  assert.match(inline, /<code>\$&lt;img&gt;\$<\/code>/)
  assert.match(inline, /role="note"/)
  for (const [state, notice] of [[{ loading: true }, /加载中/], [{ loadFailed: true }, /加载失败/]]) {
    const block = await renderToString(h(MathNode, { tex: 'x', raw: '$$<img>$$', display: true, engine: null, ...state }))
    assert.match(block, /<pre[^>]*><code>\$\$&lt;img&gt;\$\$<\/code><\/pre>/)
    assert.match(block, notice)
  }
})

test('shared lazy engine renders inline and display MathML without the HTML layout', async () => {
  const pending = loadResumeMath()
  assert.equal(loadResumeMath(), pending)
  const engine = await pending
  const html = engine.renderOrNull('x^2', false)
  assert.match(html, /<msup>/)
  assert.doesNotMatch(html, /katex-html|display="block"/)
  assert.match(engine.renderOrNull('x^2', true), /<math\b[^>]*display="block"/)
})
