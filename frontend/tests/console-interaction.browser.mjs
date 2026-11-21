import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import { join } from 'node:path'

// Run separately against the same-origin interaction-audit mock.
const origin = process.env.CONSOLE_TEST_ORIGIN
const screenshots = process.env.CONSOLE_SCREENSHOTS
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE ?? 'playwright')
let browser
before(async () => {
  assert.ok(origin, 'Set CONSOLE_TEST_ORIGIN to the local audit mock')
  const probe = await fetch(origin + '/api/v1/boss/basic/city-name-list')
  assert.equal(probe.headers.get('X-BossAnalyze-Preview'), 'mock')
  browser = await chromium.launch({ executablePath: process.env.CHROMIUM_EXECUTABLE })
})
after(async () => browser?.close())

const desktop = { width: 1440, height: 900 }
const mobile = { width: 390, height: 844 }
async function logIn(page, email = 'demo@bossanalyze.invalid', password = 'demo-only', path = '/console') {
  await page.goto(origin + '/login?redirect=' + encodeURIComponent(path))
  await page.locator('input[type=email]').fill(email)
  await page.locator('input[type=password]').fill(password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL(origin + path)
}
async function registered(page) {
  await page.goto(origin + '/login')
  const email = 'issue81-' + crypto.randomUUID() + '@bossanalyze.invalid'
  const password = 'synthetic-password'
  const send = await page.request.post(origin + '/api/auth/sendRegisterEmail', { data: { email } })
  assert.equal(send.status(), 200)
  const create = await page.request.post(origin + '/api/auth/register', {
    data: { email, password, name: '验收用户', code: '246810' },
  })
  assert.equal(create.status(), 200)
  return { email, password }
}
async function screenshot(page, name) {
  if (screenshots) await page.screenshot({ path: join(screenshots, name + '.png'), animations: 'disabled' })
}
test('mobile navigation focuses main, restores history, and preserves anchors and filters', async () => {
  const page = await browser.newPage({ viewport: mobile, hasTouch: true, isMobile: true })
  const atTopWithMainFocused = () => page.waitForFunction(
    () => window.scrollY === 0 && document.activeElement?.id === 'main-content',
    undefined, { timeout: 3500 },
  )
  const trigger = page.getByRole('button', { name: '切换侧栏', exact: true })
  await logIn(page)
  await atTopWithMainFocused()
  for (const [name, path] of [
    ['招聘数据分析', '/recruitment'],
    ['我的简历', '/resumes'],
    ['扩展日志分析', '/console/logs'],
    ['数据看板', '/console/dashboard'],
    ['工作台', '/console'],
  ]) {
    const rect = await trigger.boundingBox()
    assert.ok(rect.y >= 0 && rect.y + rect.height < mobile.height)
    await trigger.click()
    await page.getByRole('dialog').getByRole('link', { name, exact: true }).click()
    await page.waitForURL(origin + path)
    await atTopWithMainFocused()
  }
  for (const path of ['/recruitment', '/resumes', '/console/dashboard', '/console/logs', '/console']) {
    await page.goto(origin + path)
    await atTopWithMainFocused()
    assert.ok((await trigger.boundingBox()).y >= 0)
  }

  await page.goto(origin + '/console/logs')
  await page.getByRole('heading', { name: '访问时间热力图' }).waitFor()
  await atTopWithMainFocused()
  await screenshot(page, 'mobile-logs-entry')
  await page.waitForFunction(() => document.documentElement.scrollHeight - innerHeight > 900)
  const oldScroll = await page.evaluate(() => {
    scrollTo(0, document.documentElement.scrollHeight - innerHeight - 50)
    return scrollY
  })
  await page.keyboard.press('Control+b')
  await page.getByRole('dialog').getByRole('link', { name: '工作台', exact: true }).click()
  await page.waitForURL(origin + '/console')
  await atTopWithMainFocused()
  const shortly = () => new Promise(resolve => setTimeout(resolve, 700))
  await page.route(url => url.pathname.startsWith('/api/v1/system/logs'), async route => {
    await shortly()
    await route.continue()
  })
  await page.goBack()
  await page.waitForURL(origin + '/console/logs')
  await page.waitForFunction(y => Math.abs(scrollY - y) <= 2, oldScroll, { timeout: 6000 })
  assert.equal(await page.evaluate(() => document.activeElement.id), 'main-content')

  await page.goto(origin + '/console/settings#appearance')
  await page.waitForFunction(() => {
    const target = document.getElementById('appearance')
    return scrollY > 0 && target && target.getBoundingClientRect().top < innerHeight
  })
  await page.getByRole('link', { name: '服务状态' }).click()
  await page.waitForURL(origin + '/console/settings#service-status')
  await page.waitForFunction(() => document.activeElement.id === 'service-status')
  await page.close()

  const filters = await browser.newPage({ viewport: desktop })
  await logIn(filters, undefined, undefined, '/console/users')
  const name = filters.getByRole('textbox', { name: /姓名/ })
  await name.fill('陈')
  await name.press('Enter')
  await filters.waitForURL(origin + '/console/users?name=%E9%99%88')
  assert.notEqual(await filters.evaluate(() => document.activeElement.id), 'main-content')
  await filters.getByRole('row', { name: /陈晓/ }).waitFor()
  await filters.close()
})

test('management errors prioritize field and role evidence from authorized reads', async () => {
  const page = await browser.newPage({ viewport: desktop })
  await logIn(page, undefined, undefined, '/console/users')
  await page.getByRole('row', { name: /周宁/ }).waitFor()
  await page.getByRole('button', { name: '新增用户', exact: true }).first().click()
  const editor = page.getByRole('dialog')
  const name = editor.getByRole('textbox', { name: '姓名' })
  const email = editor.getByRole('textbox', { name: '邮箱' })
  const picker = editor.getByRole('combobox', { name: '用户角色' })
  await name.fill('验证草稿')
  await email.fill('demo@bossanalyze.invalid')
  await editor.locator('input[type=password]').fill('synthetic-password')
  await picker.click()
  await page.getByRole('option', { name: '访问审计员', exact: true }).click()
  await editor.getByRole('button', { name: '创建用户' }).click()
  await editor.getByText(/邮箱已被其他账号使用/).waitFor({ timeout: 2500 })
  assert.equal(await name.inputValue(), '验证草稿')
  assert.equal(await email.inputValue(), 'demo@bossanalyze.invalid')
  assert.equal(await email.getAttribute('aria-invalid'), 'true')
  assert.equal(await email.evaluate(node => node === document.activeElement), true)
  await screenshot(page, 'duplicate-email-focus')

  const rejectRequest = route => route.fulfill({ status: 400, json: { code: 400, message: 'server-internal-details' } })
  const releasedEmail = async route => {
    const response = await route.fetch()
    const body = await response.json()
    const formerOwner = body.data.content.find(user => user.email === 'demo@bossanalyze.invalid')
    if (formerOwner) formerOwner.email = 'released-email@bossanalyze.invalid'
    await route.fulfill({ response, json: body })
  }
  await page.route('**/api/user/addUser', rejectRequest)
  await page.route('**/api/user/getUserManageInfo', releasedEmail)
  await editor.getByRole('button', { name: '创建用户' }).click()
  await editor.getByText(/参数未通过校验/).waitFor({ timeout: 2500 })
  assert.equal(await email.getAttribute('aria-invalid'), 'false')
  await page.unroute('**/api/user/addUser', rejectRequest)
  await page.unroute('**/api/user/getUserManageInfo', releasedEmail)


  await email.fill('new-synthetic@bossanalyze.invalid')
  let changedRole = false
  const bindRoleFailure = route => {
    changedRole = true
    return route.fulfill({ status: 400, json: { code: 400, message: 'server-internal-role-details' } })
  }
  await page.route('**/api/user/addUser', bindRoleFailure)
  await page.route('**/api/role/getRoleList?**', async route => {
    const response = await route.fetch()
    const body = await response.json()
    if (changedRole) body.data.find(role => role.id === 2).status = 0
    await route.fulfill({ response, json: body })
  })
  await editor.getByRole('button', { name: '创建用户' }).click()
  await editor.getByText(/该角色已停用/).waitFor({ timeout: 2500 })
  assert.equal(await picker.evaluate(node => node === document.activeElement), true)
  assert.equal(await name.inputValue(), '验证草稿')
  assert.equal(await editor.getByText('server-internal-role-details').count(), 0)
  await screenshot(page, 'stale-role-focus')

  await picker.fill('数据分析员')
  await page.getByRole('option', { name: '数据分析员', exact: true }).click()
  await editor.getByRole('button', { name: '创建用户' }).click()
  await editor.getByText(/参数未通过校验/).waitFor({ timeout: 2500 })
  assert.equal(await email.getAttribute('aria-invalid'), 'false')
  assert.equal(await editor.getByText(/邮箱已被其他账号使用|server-internal-role-details/).count(), 0)
  await page.close()

  const rolePage = await browser.newPage({ viewport: desktop })
  await logIn(rolePage, undefined, undefined, '/console/roles')
  await rolePage.getByRole('button', { name: '删除访问审计员', exact: true }).click()
  const remove = rolePage.getByRole('dialog')
  await remove.getByRole('button', { name: '删除角色', exact: true }).click()
  await remove.getByText(/该角色仍有用户使用/).waitFor({ timeout: 2500 })
  const users = remove.getByRole('link', { name: '查看关联用户' })
  assert.equal(await users.getAttribute('href'), '/console/users?roleId=2')
  await screenshot(rolePage, 'role-bound-users')
  const read = rolePage.waitForRequest(request => request.url().endsWith('/getUserManageInfo'))
  await users.click()
  await rolePage.waitForURL(origin + '/console/users?roleId=2')
  assert.equal((await read).postDataJSON().query.roleId, 2)
  await rolePage.getByRole('row', { name: /陈晓/ }).waitFor()
  await rolePage.close()

  const rolesOnly = await browser.newPage({ viewport: desktop })
  const userReads = []
  await rolesOnly.route('**/api/auth/getRouters', async route => {
    const response = await route.fetch()
    const body = await response.json()
    const console = body.data.find(menu => menu.path === '/console')
    console.children = console.children.filter(menu => menu.path !== 'users')
    await route.fulfill({ response, json: body })
  })
  rolesOnly.on('request', request => {
    if (request.url().endsWith('/getUserManageInfo')) userReads.push(request.url())
  })
  await rolesOnly.route('**/api/auth/deleteRole', route => route.fulfill({
    status: 500, json: { code: 500, message: 'server-internal-delete-details' },
  }))
  await logIn(rolesOnly, undefined, undefined, '/console/roles')
  await rolesOnly.getByRole('button', { name: '删除访问审计员', exact: true }).click()
  const dialog = rolesOnly.getByRole('dialog')
  await dialog.getByRole('button', { name: '删除角色', exact: true }).click()
  await dialog.getByText(/服务暂不可用/).waitFor()
  assert.equal(await dialog.getByRole('link', { name: '查看关联用户' }).count(), 0)
  assert.equal(await dialog.getByText('server-internal-delete-details').count(), 0)
  assert.deepEqual(userReads, [])
  await rolesOnly.close()
})

test('disabled roles cannot be assigned by pointer or keyboard and historical roles can be replaced', async () => {
  const page = await browser.newPage({ viewport: desktop })
  let reassigned = false
  await page.route('**/api/user/getUserManageInfo', async route => {
    const response = await route.fetch()
    const body = await response.json()
    if (!reassigned) {
      const historical = body.data.content.find(row => row.name === '陈晓')
      if (historical) Object.assign(historical, { roleId: 6, roleName: '历史数据查看员' })
    }
    await route.fulfill({ response, json: body })
  })
  page.on('response', (response) => {
    if (response.url().includes('/api/user/updateUser') && response.ok()) reassigned = true
  })
  await logIn(page, undefined, undefined, '/console/users')
  await page.getByRole('button', { name: '新增用户', exact: true }).first().click()
  const editor = page.getByRole('dialog')
  const picker = editor.getByRole('combobox', { name: '用户角色' })
  await picker.click()
  const disabled = page.getByRole('option', { name: /历史数据查看员/ })
  await disabled.waitFor()
  assert.equal(await disabled.isDisabled(), true)
  assert.match(await disabled.innerText(), /停用，无法指派/)
  await screenshot(page, 'disabled-role-option')
  const { x, y, width, height } = await disabled.boundingBox()
  await page.mouse.click(x + width / 2, y + height / 2)
  assert.ok(!(await picker.inputValue()).includes('历史数据查看员'))
  await picker.fill('历史数据查看员')
  await picker.press('ArrowDown')
  await picker.press('Enter')
  await editor.getByText('初始密码', { exact: false }).first().click()
  assert.ok(!(await picker.inputValue()).includes('历史数据查看员'))
  await editor.getByRole('button', { name: '关闭用户面板' }).click()

  await page.getByRole('button', { name: '编辑陈晓' }).click()
  await editor.getByText(/原角色.*历史数据查看员.*停用/).waitFor()
  assert.match(await picker.inputValue(), /历史数据查看员/)
  await picker.click()
  await picker.fill('数据分析员')
  await page.getByRole('option', { name: '数据分析员', exact: true }).click()
  assert.equal(await picker.inputValue(), '数据分析员')
  await screenshot(page, 'reassign-historical-role')
  await editor.getByRole('button', { name: '保存更改' }).click()
  await editor.waitFor({ state: 'hidden' })
  const row = page.getByRole('row', { name: /陈晓/ })
  await row.getByRole('cell', { name: '数据分析员', exact: true }).waitFor()
  await page.close()
})

test('authorized placeholder is labeled, titled, and reachable directly', async () => {
  const page = await browser.newPage({ viewport: desktop })
  await logIn(page)
  const main = page.getByRole('main')
  assert.equal(await main.getByRole('link', { name: /运营中心/ }).count(), 0)
  const reserved = page.getByRole('link', { name: /运营中心.*尚未开放/ })
  assert.equal(await reserved.getAttribute('href', { timeout: 2500 }), '/console/operations')
  await reserved.click()
  await page.waitForURL(origin + '/console/operations')
  await main.getByRole('heading', { name: '运营中心', exact: true }).waitFor()
  await main.getByText('此功能尚未开放').waitFor()
  assert.equal(await page.title(), '运营中心 · BossAnalyze')
  await screenshot(page, 'operations-reserved')
  await main.getByRole('button', { name: '返回上一页' }).click()
  await page.waitForURL(origin + '/console')
  await page.goto(origin + '/console/operations')
  await main.getByRole('link', { name: '返回工作台' }).click()
  await page.waitForURL(origin + '/console')
  await page.close()

  const ordinary = await browser.newPage()
  const { email, password } = await registered(ordinary)
  await logIn(ordinary, email, password)
  await ordinary.goto(origin + '/console/operations')
  await ordinary.waitForURL(origin + '/system-error/403')
  await ordinary.close()
})

test('new accounts can reach resumes and recruitment from the first screen and admins retain stats', async () => {
  let ordinary
  for (const [label, viewport] of [['desktop', desktop], ['mobile', mobile]]) {
    const page = await browser.newPage({ viewport })
    ordinary ??= await registered(page)
    const stats = []
    page.on('request', (request) => {
      if (request.url().includes('/dashboard/stats')) stats.push(request.url())
    })
    await logIn(page, ordinary.email, ordinary.password)
    const main = page.getByRole('main')
    for (const [title, url] of [['我的简历', '/resumes'], ['招聘分析', '/recruitment']]) {
      const link = main.getByRole('link', { name: title, exact: true })
      const rect = await link.boundingBox({ timeout: 3000 })
      assert.equal(await link.getAttribute('href'), url)
      assert.ok(rect.y >= 0 && rect.y + rect.height <= viewport.height)
    }
    assert.equal(await main.getByText('日志访问概览').count(), 0)
    assert.equal(await main.getByText('尚未分配管理功能', { exact: false }).count(), 0)
    assert.deepEqual(stats, [])
    await screenshot(page, 'workbench-' + label)
    await main.getByRole('link', { name: '我的简历', exact: true }).click()
    await page.waitForURL(origin + '/resumes')
    await page.goBack()
    await main.getByRole('link', { name: '招聘分析', exact: true }).click()
    await page.waitForURL(origin + '/recruitment')
    await page.close()
  }
  const admin = await browser.newPage({ viewport: desktop })
  await logIn(admin)
  await admin.getByRole('region', { name: '日志访问概览' }).locator('time').waitFor()
  await screenshot(admin, 'workbench-admin')
})
