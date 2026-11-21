// Run against an isolated interaction-audit Mock server with Chromium and Playwright installed:
// RESUME_MOCK_URL=http://127.0.0.1:5129 PLAYWRIGHT_MODULE=/path/to/playwright/index.mjs \
//   mise exec node@26.7.0 -- pnpm exec node --test tests/resume-interactions.browser.mjs
import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
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

async function editor(t, { content = '# 已提交正文', mobile = false } = {}) {
  const context = await browser.newContext({
    baseURL,
    viewport: mobile ? { width: 390, height: 844 } : { width: 1440, height: 900 },
    isMobile: mobile,
    hasTouch: mobile,
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

test('inserting an image from the toolbar keeps the insertion in the native undo history', async t => {
  const { page, body, resume } = await editor(t)
  await body.press('End')
  await page.getByRole('button', { name: '插入图片', exact: true }).click()
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

  await page.reload()
  await body.waitFor()
  assert.equal(await body.inputValue(), recovered)
  const workspace = await api('GET', resource)
  assert.equal(workspace.content, recovered)
  assert.equal(workspace.headCommit, resume.headCommit)
  assert.equal(workspace.stashStack.length, 0)
  await page.getByText('已恢复到工作区，尚未提交').waitFor()
  await page.getByText('HEAD ' + resume.headCommit.slice(0, 8)).waitFor()
  const save = page.getByRole('button', { name: '保存并提交' })
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
