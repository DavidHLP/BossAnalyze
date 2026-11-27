// Run against an isolated interaction-audit Mock server with Chromium and Playwright installed:
// RESUME_MOCK_URL=http://127.0.0.1:5129 PLAYWRIGHT_MODULE=/path/to/playwright/index.mjs \
//   mise exec node@26.7.0 -- pnpm exec node --test tests/resume-interactions.browser.mjs
import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { readFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import { after, before, test } from 'node:test'

const baseURL = process.env.RESUME_MOCK_URL
assert.ok(baseURL && new URL(baseURL).hostname === '127.0.0.1', 'Use a loopback audit Mock server')
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE ?? 'playwright')
let browser

before(async () => {
  browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH ?? '/usr/bin/chromium' })
})
after(async () => { await browser?.close() })

async function editor(t, { content = '# 已提交正文', mobile = false, colorScheme = 'light' } = {}) {
  const context = await browser.newContext({
    baseURL,
    viewport: mobile ? { width: 390, height: 844 } : { width: 1440, height: 900 },
    isMobile: mobile,
    hasTouch: mobile,
    colorScheme,
    reducedMotion: 'reduce',
  })
  t.after(() => context.close())
  const page = await context.newPage()
  page.setDefaultTimeout(5500)
  await page.goto('/login')
  await page.locator('#email').fill('demo@bossanalyze.invalid')
  await page.locator('#password').fill('demo-only')
  const login = page.waitForResponse(response => response.url().endsWith('/api/auth/login'))
  await page.getByRole('button', { name: '登录', exact: true }).click()
  const response = await login
  assert.equal(response.headers()['x-bossanalyze-preview'], 'mock')
  const { data: { token } } = await response.json()
  await page.waitForURL(url => url.pathname !== '/login')

  async function api(method, path, data) {
    const reply = await context.request.fetch(path, {
      method, data, headers: { Authorization: 'Bearer ' + token },
    })
    assert.equal(reply.headers()['x-bossanalyze-preview'], 'mock')
    assert.equal(reply.ok(), true, method + ' ' + path)
    return (await reply.json()).data
  }

  const resume = await api('POST', '/api/resume', { title: '合成验收 ' + randomUUID().slice(0, 8), content })
  const path = '/resumes/' + resume.id
  const resource = '/api/resume/' + resume.id
  await page.goto(path + '/edit')
  const body = page.getByRole('textbox', { name: '简历正文' })
  await body.waitFor()
  return { page, body, path, resume, resource, api }
}

async function shot(page, name) {
  if (process.env.RESUME_SCREENSHOTS) {
    await page.screenshot({ path: resolve(process.env.RESUME_SCREENSHOTS, name + '.png') })
  }
}

test('a rejected LaTeX download locates the formula in the mobile Markdown editor', async t => {
  const formula = '$x%$'
  const prefix = '# 公式验收\r\n' + '用于滚动核验的正文\r\n'.repeat(40) + '中文😀 '
  const { page, body, resume } = await editor(t, { content: prefix + formula, mobile: true, colorScheme: 'dark' })
  await page.getByRole('button', { name: '预览', exact: true }).click()
  const downloads = []
  page.on('download', download => downloads.push(download.suggestedFilename()))
  await page.getByRole('button', { name: '导出简历' }).click()
  const dialog = page.getByRole('dialog', { name: '导出简历' })
  await dialog.getByRole('button', { name: '下载 LaTeX (.tex)', exact: true }).click()
  const diagnostic = dialog.getByText('第 42 行第 6 列', { exact: true })
  await diagnostic.waitFor()
  await dialog.getByText('x%', { exact: true }).waitFor()
  await dialog.getByText(/未转义的 %/).waitFor()
  assert.equal(await dialog.getByRole('button', { name: '下载 HTML' }).isEnabled(), true)
  assert.equal(await dialog.getByRole('button', { name: '打印 / 另存为 PDF' }).isEnabled(), true)
  await dialog.getByRole('button', { name: '下载 HTML' }).click()
  await dialog.getByRole('alert').filter({ hasText: /公式无法渲染/ }).waitFor()

  const markdown = page.waitForEvent('download')
  await dialog.getByRole('button', { name: '下载 Markdown' }).click()
  await markdown
  assert.deepEqual(downloads, [resume.title + '.md'])

  await dialog.getByRole('button', { name: '定位到公式' }).click()
  await dialog.waitFor({ state: 'hidden' })
  await page.waitForFunction(() => document.activeElement?.id === 'resume-markdown')
  const start = prefix.replaceAll('\r\n', '\n').length
  assert.deepEqual(await body.evaluate(input => [input.selectionStart, input.selectionEnd,
    input.value.slice(input.selectionStart, input.selectionEnd)]), [start, start + formula.length, formula])
  assert.equal(await body.evaluate(input => {
    const { top } = input.getBoundingClientRect()
    const { paddingTop, lineHeight } = getComputedStyle(input)
    const lineTop = top + parseFloat(paddingTop) + 41 * parseFloat(lineHeight) - input.scrollTop
    return lineTop >= 0 && lineTop + parseFloat(lineHeight) <= window.innerHeight
  }), true)
})

const katexChunk = /\/katex(?:-[^/?]+)?\.m?js(?:\?|$)/

test('finishing a stash clears diagnostics and cancels HTML for the replaced draft', async t => {
  const releaseStash = Promise.withResolvers()
  const releaseKatex = Promise.withResolvers()
  t.after(() => { releaseStash.resolve(); releaseKatex.resolve() })
  const { page, body, resume, resource } = await editor(t)
  await page.route('**' + resource + '/version/stash', async route => {
    await releaseStash.promise
    await route.continue()
  })
  await page.route(katexChunk, async route => {
    await releaseKatex.promise
    await route.continue()
  })

  const requested = page.waitForRequest(katexChunk)
  await body.fill('# 储藏草稿\n\n中文 $x%$')
  await requested
  await page.getByRole('button', { name: '储藏', exact: true }).click()
  await page.getByRole('button', { name: '导出简历' }).click()
  const dialog = page.getByRole('dialog', { name: '导出简历' })
  const locate = dialog.getByRole('button', { name: '定位到公式' })
  const html = dialog.getByRole('button', { name: '下载 HTML' })
  await dialog.getByRole('button', { name: '下载 LaTeX (.tex)', exact: true }).click()
  await locate.waitFor()
  await html.click()
  assert.equal(await html.isDisabled(), true)

  releaseStash.resolve()
  await page.getByText('草稿已储藏，可在版本历史中取回。', { exact: true }).waitFor()
  assert.equal(await dialog.isVisible(), true)
  assert.equal(await locate.count(), 0)
  assert.equal(await html.isEnabled(), true)

  releaseKatex.resolve()
  const downloaded = page.waitForEvent('download')
  await html.click()
  const exported = await readFile(await (await downloaded).path(), 'utf8')
  assert.ok(exported.includes(`<h1>${resume.content.slice(2)}</h1>`))
  assert.ok(!exported.includes('$x%$'))
})

test('local HTML and print wait for MathML and closing the dialog cancels the pending download', async t => {
  const release = Promise.withResolvers()
  t.after(() => release.resolve())
  const { page, body, resume } = await editor(t)
  const requests = []
  page.on('request', request => { if (katexChunk.test(request.url())) requests.push(request.url()) })
  const openExport = page.getByRole('button', { name: '导出简历' })
  const dialog = page.getByRole('dialog', { name: '导出简历' })
  const html = dialog.getByRole('button', { name: '下载 HTML' })
  const print = dialog.getByRole('button', { name: '打印 / 另存为 PDF' })
  await openExport.click()
  const plainDownload = page.waitForEvent('download')
  await html.click()
  assert.equal((await plainDownload).suggestedFilename(), resume.title + '.html')
  assert.equal(await page.evaluate(() => performance.getEntriesByType('resource')
    .some(entry => /\/katex(?:[-.]|\/)/.test(entry.name))), false)
  assert.deepEqual(requests, [])
  await dialog.getByRole('button', { name: '取消' }).click()

  await page.route(katexChunk, async route => {
    await release.promise
    await route.continue()
  })
  const requested = page.waitForRequest(katexChunk)
  await body.fill('速度 $a_1$\n\n$$E=mc^2$$\n\n![证书](asset:certificate.png)')
  await requested
  const downloads = []
  page.on('download', download => downloads.push(download.suggestedFilename()))
  await openExport.click()
  await html.click()
  assert.equal(await html.isDisabled(), true)
  assert.equal(await print.isDisabled(), true)
  await dialog.getByRole('button', { name: '取消' }).click()
  await openExport.click()
  assert.equal(await html.isEnabled(), true)
  release.resolve()

  const preview = page.getByRole('article', { name: '简历预览', includeHidden: true })
  await preview.locator('math').nth(1).waitFor({ state: 'attached' })
  const htmlDownload = page.waitForEvent('download')
  await html.click()
  const exportedHtml = await readFile(await (await htmlDownload).path(), 'utf8')
  assert.equal((exportedHtml.match(/<math(?:\s|>)/g) ?? []).length, 2)
  assert.deepEqual(downloads, [resume.title + '.html'])

  await page.evaluate(() => {
    document.addEventListener('load', event => {
      const frame = event.target
      if (!(frame instanceof HTMLIFrameElement) || frame.title !== '简历打印视图') return
      frame.contentWindow.print = () => {
        window.printedResume = frame.contentDocument.querySelectorAll('math').length
        frame.contentWindow.dispatchEvent(new Event('afterprint'))
      }
    }, true)
  })
  await print.click()
  await page.waitForFunction(() => window.printedResume === 2)
  assert.equal(await page.locator('iframe[title="简历打印视图"]').count(), 0)

  const texDownload = page.waitForEvent('download')
  await dialog.getByRole('button', { name: '下载 LaTeX (.tex)', exact: true }).click()
  await dialog.getByRole('status').getByText(/本地导出未包含图片.*certificate\.png/).waitFor()
  const tex = await texDownload
  assert.equal(tex.suggestedFilename(), resume.title + '.tex')
  assert.ok((await readFile(await tex.path(), 'utf8')).includes(String.raw`\resumebody{速度 \(a_1\)}`))
})

test('a failed math chunk allows an explicit HTML download with formula source', async t => {
  const { page, body, resume } = await editor(t)
  await page.route(katexChunk, route => route.abort())
  await body.fill('速度 $a_1$')
  await page.getByRole('button', { name: '导出简历' }).click()
  const dialog = page.getByRole('dialog', { name: '导出简历' })
  const downloads = []
  page.on('download', download => downloads.push(download.suggestedFilename()))
  await dialog.getByRole('button', { name: '下载 HTML' }).click()
  await dialog.getByRole('alert').filter({ hasText: /公式渲染器加载失败.*重试/ }).waitFor()
  const downloaded = page.waitForEvent('download')
  await dialog.getByRole('button', { name: '保留公式源码并继续' }).click()
  const html = await readFile(await (await downloaded).path(), 'utf8')
  assert.ok(html.includes('<p>速度 $a_1$</p>'))
  assert.ok(!html.includes('<math'))
  assert.deepEqual(downloads, [resume.title + '.html'])
})

for (const activation of ['desktop pointer', 'desktop keyboard', 'mobile viewport']) {
  test('native undo removes only toolbar bold with ' + activation, async t => {
    const mobile = activation === 'mobile viewport'
    const { page, body } = await editor(t, { content: '', mobile })
    await body.focus()
    await page.keyboard.insertText('Codex input')
    await body.press('Control+a')
    const bold = page.getByRole('button', { name: '加粗', exact: true })
    if (activation === 'desktop keyboard') {
      await bold.focus()
      await bold.press('Enter')
    } else if (mobile) await bold.tap()
    else await bold.click()
    assert.equal(await body.inputValue(), '**Codex input**')
    assert.deepEqual(await body.evaluate(input => [
      document.activeElement === input, input.selectionStart, input.selectionEnd,
    ]), [true, 2, 13])
    await page.keyboard.press('Control+z')
    assert.equal(await body.inputValue(), 'Codex input')
    if (activation !== 'desktop keyboard') await shot(page, mobile ? 'undo-mobile' : 'undo-desktop')
  })
}

test('inserting an image from mobile preview returns to the editor and preserves native undo', async t => {
  const { page, body, resume } = await editor(t, { mobile: true })
  await body.press('End')
  await page.getByRole('button', { name: '预览', exact: true }).click()
  await page.getByRole('button', { name: '图片素材', exact: true }).click()
  const assets = page.getByRole('dialog', { name: '图片素材管理' })
  const name = 'undo-' + randomUUID().slice(0, 8) + '.png'
  await assets.getByLabel('拖放或选择图片').setInputFiles({
    name,
    mimeType: 'image/png',
    buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a1XcAAAAASUVORK5CYII=', 'base64'),
  })
  await assets.getByRole('button', { name: '插入光标' }).click()
  await page.waitForFunction(() => document.activeElement?.id === 'resume-markdown')
  assert.ok((await body.inputValue()).includes(name + ')'))
  await page.keyboard.press('Control+z')
  assert.equal(await body.inputValue(), resume.content)
})

test('a popped stash can be committed after refresh while HEAD still points to V1', async t => {
  const { page, body, path, resume, resource, api } = await editor(t)
  const recovered = '# 从储藏恢复的 V2'
  await api('POST', resource + '/version/stash', { content: recovered })
  await page.getByRole('link', { name: '版本历史' }).click()
  await page.getByRole('button', { name: '弹出最新储藏', exact: true }).click()
  await page.getByRole('dialog').getByRole('button', { name: '弹出最新储藏' }).click()
  await page.waitForURL(path + '/edit')
  await body.waitFor()
  assert.equal(await body.inputValue(), recovered)

  await page.route('**' + resource + '/version/recent?**', route =>
    route.fulfill({ status: 503, json: { code: 503 } }), { times: 1 })
  await page.reload()
  await body.waitFor()
  assert.equal(await body.inputValue(), recovered)
  const save = page.getByRole('button', { name: '保存并提交' })
  assert.equal(await save.isDisabled(), true)
  await page.getByRole('button', { name: '重新读取', exact: true }).click({ timeout: 2500 })
  assert.equal(await body.inputValue(), recovered)
  const workspace = await api('GET', resource)
  assert.equal(workspace.content, recovered)
  assert.equal(workspace.headCommit, resume.headCommit)
  assert.equal(workspace.stashStack.length, 0)
  await page.getByText('已恢复到工作区，尚未提交').waitFor()
  await page.getByText('HEAD ' + resume.headCommit.slice(0, 8)).waitFor()
  assert.equal(await save.isEnabled(), true)
  assert.equal(await page.getByRole('textbox', { name: '提交备注' }).inputValue(), '')
  await shot(page, 'stash-recovered-after-refresh')

  await save.click()
  await page.getByText('简历已保存并提交。').waitFor()
  await page.reload()
  await page.getByRole('link', { name: '版本历史' }).click()
  await page.getByText('更新简历', { exact: true }).waitFor()
  const [latest, initial] = await api('GET', resource + '/version/history')
  assert.equal(latest.content, recovered)
  assert.equal(initial.id, resume.headCommit)
  assert.deepEqual(latest.parentCommits, [initial.id])
  await shot(page, 'stash-committed-history')
})

test('the stash stack offers to add the current workspace text even when it contains an older stash', async t => {
  const { page, resume, resource, api } = await editor(t)
  await api('POST', resource + '/version/stash', { content: '# 旧储藏正文' })
  await page.getByRole('link', { name: '版本历史' }).click()
  await page.getByText('已储藏 1 项').waitFor()
  await page.getByRole('button', { name: '新增储藏', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '新增储藏' })
  await dialog.getByText(/当前工作区正文/).waitFor()
  assert.equal(await dialog.getByRole('textbox', { name: '要储藏的正文' }).inputValue(), resume.content)
  await shot(page, 'stash-create-workspace')
  await dialog.getByRole('button', { name: '新增储藏' }).click()
  await page.getByText('已储藏 2 项').waitFor()
  const workspace = await api('GET', resource)
  assert.equal(workspace.headCommit, resume.headCommit)
  assert.deepEqual(workspace.stashStack.map(entry => entry.content), [resume.content, '# 旧储藏正文'])
})

for (const applied of [false, true]) {
  test('an unconfirmed 500 opens a read-only version check when the commit was ' +
    (applied ? 'applied' : 'not applied'), async t => {
    const { page, body, path, resume, resource, api } = await editor(t)
    const draft = '# 等待核对的草稿'
    const remark = '核对专用提交备注'
    const note = page.getByRole('textbox', { name: '提交备注' })
    await body.fill(draft)
    await note.fill(remark)
    await page.route('**' + resource + '/version/commit', async route => {
      if (applied) {
        const response = await route.fetch()
        assert.equal(response.ok(), true)
      }
      await route.fulfill({ status: 500, json: { code: 500 } })
    }, { times: 1 })

    const nativeDialogs = []
    page.on('dialog', async dialog => {
      nativeDialogs.push(dialog.type())
      await dialog.dismiss()
    })
    await page.getByRole('button', { name: '保存并提交' }).click()
    const alert = page.getByRole('alert').filter({ hasText: /结果未确认/ })
    await alert.waitFor()
    const requests = []
    const onRequest = request => {
      const url = new URL(request.url())
      if (url.pathname.startsWith(resource)) requests.push([request.method(), url.pathname])
    }
    page.on('request', onRequest)
    const check = alert.getByRole('button', { name: '核对最新版本' })
    await check.click()
    const dialog = page.getByRole('dialog', { name: '核对最新版本' })
    const [latest] = await api('GET', resource + '/version/history')
    await dialog.getByText('HEAD ' + latest.id.slice(0, 8)).waitFor()
    await dialog.getByText(latest.message, { exact: true }).waitFor()
    await dialog.getByText(applied ? draft : resume.content, { exact: true }).waitFor()
    assert.equal(requests.length > 0 && requests.every(([method]) => method === 'GET'), true)
    assert.equal(new URL(page.url()).pathname, path + '/edit')
    await shot(page, applied ? 'check-commit-applied' : 'check-commit-unconfirmed')
    await dialog.getByRole('button', { name: '返回编辑' }).click()
    await dialog.waitFor({ state: 'hidden' })
    page.off('request', onRequest)
    assert.equal(await body.inputValue(), draft)
    assert.equal(await note.inputValue(), remark)
    assert.deepEqual(nativeDialogs, [])
    if (!applied) {
      await page.getByRole('button', { name: '保存并提交' }).click()
      await page.getByText('简历已保存并提交。').waitFor()
    }
    const history = await api('GET', resource + '/version/history')
    assert.equal(history.length, 2)
    assert.equal(history[0].message, remark)
    assert.equal(history[0].content, draft)
  })
}

test('continuing editing retains the caret and scroll, and only discarding leaves the draft', async t => {
  const { page, body, path, resume } = await editor(t)
  await page.getByRole('link', { name: '版本历史' }).click()
  await page.getByRole('link', { name: '返回编辑器' }).click()
  await body.waitFor()
  const draft = '# 仅在当前编辑页保留的草稿\n' + '用于滚动核验的合成正文\n'.repeat(100)
  await body.fill(draft)
  await body.evaluate(input => {
    input.setSelectionRange(130, 138, 'backward')
    window.scrollTo(0, 540)
  })
  const editorPosition = () => body.evaluate(input => ({
    focused: document.activeElement === input,
    scroll: window.scrollY,
    textareaScroll: input.scrollTop,
    selection: [input.selectionStart, input.selectionEnd, input.selectionDirection],
  }))
  const position = await editorPosition()
  assert.equal(position.focused, true)
  assert.ok(position.scroll > 0)

  const nativeDialogs = []
  page.on('dialog', async dialog => {
    nativeDialogs.push(dialog.type())
    await dialog.dismiss()
  })
  await page.evaluate(() => history.back())
  const dialog = page.getByRole('dialog', { name: '丢弃未保存的更改？' })
  await dialog.waitFor()
  await shot(page, 'leave-draft-confirmation')
  await dialog.getByRole('button', { name: '继续编辑' }).click()
  await page.waitForURL(path + '/edit')
  await page.waitForFunction(() => document.activeElement?.id === 'resume-markdown')
  assert.equal(await body.inputValue(), draft)
  assert.deepEqual(await editorPosition(), position)

  // Reload takes the real beforeunload path, unlike a synthetic Event.
  const reloadConfirmation = page.waitForEvent('dialog')
  await page.evaluate(() => location.reload())
  assert.equal((await reloadConfirmation).type(), 'beforeunload')
  assert.deepEqual(nativeDialogs, ['beforeunload'])
  assert.equal(await body.inputValue(), draft)
  assert.equal(await page.evaluate(() => [localStorage, sessionStorage].some(storage =>
    Object.values(storage).some(value => value.includes('仅在当前编辑页保留的草稿')),
  )), false)

  await page.getByRole('link', { name: '版本历史' }).click()
  await dialog.getByRole('button', { name: '丢弃更改' }).click()
  await page.waitForURL(path + '/versions')
  await page.getByRole('link', { name: '返回编辑器' }).click()
  await body.waitFor()
  assert.equal(await body.inputValue(), resume.content)
  await page.getByText('工作区正文对应当前 HEAD').waitFor()
  await shot(page, 'leave-draft-reopened')
})

test('the leave dialog can stash the draft in place and keep its unsaved note', async t => {
  const { page, body, path, resume, resource, api } = await editor(t)
  const draft = '# 从离开确认中储藏的正文'
  const remark = '备注仍在编辑页'
  const note = page.getByRole('textbox', { name: '提交备注' })
  await body.fill(draft)
  await note.fill(remark)
  await page.getByRole('link', { name: '版本历史' }).click()
  const dialog = page.getByRole('dialog', { name: '丢弃未保存的更改？' })
  await dialog.getByRole('button', { name: '储藏正文' }).click()
  await page.getByText('草稿已储藏，可在版本历史中取回。').waitFor()
  assert.equal(new URL(page.url()).pathname, path + '/edit')
  assert.equal(await note.inputValue(), remark)
  assert.equal(await body.inputValue(), resume.content)
  const workspace = await api('GET', resource)
  assert.equal(workspace.stashStack[0].content, draft)
  assert.equal(workspace.headCommit, resume.headCommit)
})
