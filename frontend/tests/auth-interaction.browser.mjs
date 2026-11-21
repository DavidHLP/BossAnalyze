// Run with node --test against an isolated copy of the audit's same-origin Mock.
// Set AUTH_MOCK_URL and make playwright-core available (for example via NODE_PATH).
// CHROMIUM_EXECUTABLE and AUTH_SCREENSHOT_DIR are optional.
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { join } from 'node:path'
import { after, before, test } from 'node:test'

const { chromium } = createRequire(import.meta.url)('playwright-core')
const baseURL = process.env.AUTH_MOCK_URL
assert.ok(baseURL, 'Set AUTH_MOCK_URL to the isolated audit Mock before running this suite')

let browser
before(async () => {
  browser = await chromium.launch({ executablePath: process.env.CHROMIUM_EXECUTABLE })
})
after(async () => browser?.close())

async function openPage(t, path = '/register', viewport = { width: 1440, height: 900 }) {
  const page = await browser.newPage({ baseURL, viewport })
  page.setDefaultTimeout(4000)
  t.after(() => page.close())
  await page.goto(path)
  return page
}

async function screenshot(page, filename) {
  const directory = process.env.AUTH_SCREENSHOT_DIR
  if (directory) await page.screenshot({ path: join(directory, filename), fullPage: true })
}

function address(label) {
  return `${label}-${crypto.randomUUID().slice(0, 8)}@bossanalyze.invalid`
}

async function sendCode(page) {
  const sent = page.waitForResponse(response => response.url().endsWith('/api/auth/sendRegisterEmail'))
  await page.getByRole('button', { name: '获取验证码', exact: true }).click()
  return sent
}

test('switching email clears the code and returning resumes the original cooldown', async t => {
  const page = await openPage(t)
  await page.clock.install()
  const first = address('first')
  const second = address('second')
  const email = page.getByLabel('邮箱地址')
  const code = page.getByLabel('邮箱验证码')
  const cooldown = page.getByRole('button', { name: /\d+s 后重发/ })

  await email.fill(` ${first} `)
  const firstSend = await sendCode(page)
  assert.equal(firstSend.status(), 200)
  assert.deepEqual(firstSend.request().postDataJSON(), { email: first })
  await cooldown.waitFor()
  await page.clock.fastForward(12_000)

  await code.fill('246810')
  await email.fill(second)
  assert.equal(await code.inputValue(), '')
  assert.equal(await page.getByRole('button', { name: '获取验证码', exact: true }).isEnabled(), true)
  assert.equal(await page.getByText(first, { exact: false }).isVisible(), false)
  assert.equal((await sendCode(page)).status(), 200)
  await page.getByText(second, { exact: false }).waitFor()

  await code.fill('135790')
  await email.fill(first)
  assert.equal(await code.inputValue(), '')
  await page.getByText(first, { exact: false }).waitFor()
  assert.equal(await cooldown.isDisabled(), true)
  assert.ok(Number.parseInt(await cooldown.innerText(), 10) <= 48)
  await screenshot(page, 'register-email-cooldown-desktop.png')

  await page.clock.fastForward(49_000)
  assert.equal(await page.getByRole('button', { name: '获取验证码', exact: true }).isEnabled(), true)
  await email.fill(second)
  assert.equal(await cooldown.isDisabled(), true)

  // A new page has no frontend cooldown, so the server response is authoritative.
  await page.reload()
  await email.fill(first)
  assert.equal((await sendCode(page)).status(), 409)
  await page.getByRole('alert').filter({ hasText: '请求过于频繁' }).waitFor()
})

test('late send success and throttling belong to the email that initiated each request', { timeout: 15_000 }, async t => {
  const page = await openPage(t)
  const email = page.getByLabel('邮箱地址')
  const code = page.getByLabel('邮箱验证码')
  const sendButton = page.getByRole('button', { name: '获取验证码', exact: true })
  const first = address('slow')
  const second = address('quick')
  const limited = address('limited')
  const requests = new Map([first, second, limited].map(target => [target, Promise.withResolvers()]))
  await page.route('**/api/auth/sendRegisterEmail', route => {
    const { email: target } = route.request().postDataJSON()
    requests.get(target).resolve(route)
  })

  await email.fill(first)
  await sendButton.click()
  await page.getByRole('button', { name: '正在发送…' }).waitFor()
  await email.fill(second)
  await sendButton.click()
  await (await requests.get(second).promise).fulfill({ json: { code: 200 } })
  await page.getByText(second, { exact: false }).waitFor()
  await code.fill('246810')
  await (await requests.get(first).promise).fulfill({ json: { code: 200 } })
  assert.equal(await code.inputValue(), '246810')
  assert.equal(await page.getByText(first, { exact: false }).isVisible(), false)

  await email.fill(first)
  await page.getByText(first, { exact: false }).waitFor()
  assert.equal(await page.getByRole('button', { name: /\d+s 后重发/ }).isDisabled(), true)
  assert.equal(await code.inputValue(), '')
  await email.fill(limited)
  await sendButton.click()
  await email.fill(second)
  await (await requests.get(limited).promise).fulfill({ status: 429, json: { code: 1011 } })
  assert.equal(await page.getByText('请求过于频繁', { exact: false }).isVisible(), false)
  await email.fill(limited)
  await page.getByRole('alert').filter({ hasText: '请求过于频繁' }).waitFor()
  assert.equal(await sendButton.isEnabled(), true)
  await email.fill(second)
  assert.equal(await page.getByText('请求过于频繁', { exact: false }).isVisible(), false)
})

test('keyboard registration focuses the first invalid field and summary links focus their targets', async t => {
  const page = await openPage(t, '/register', { width: 390, height: 844 })
  const submit = page.getByRole('button', { name: '完成注册' })
  const name = page.getByRole('textbox', { name: '显示姓名' })
  const email = page.getByRole('textbox', { name: '邮箱地址' })
  const code = page.getByRole('textbox', { name: '邮箱验证码' })
  const active = page.locator(':focus')
  for (let i = 0; i < 14 && !await submit.evaluate(element => element === document.activeElement); i++) {
    await page.keyboard.press('Tab')
  }
  assert.equal(await active.innerText(), '完成注册')
  await page.keyboard.press('Enter')

  assert.equal(await active.getAttribute('id'), await name.getAttribute('id'))
  assert.match(await name.ariaSnapshot(), /请输入显示姓名/)
  assert.equal(await name.getAttribute('aria-invalid'), 'true')
  const box = await name.boundingBox()
  assert.ok(box.y >= 0 && box.y + box.height <= 844)
  await page.getByText('请检查以下字段', { exact: true }).waitFor()
  await screenshot(page, 'register-errors-mobile.png')

  await page.keyboard.press('Tab')
  assert.equal(await active.getAttribute('id'), await email.getAttribute('id'))
  const codeLink = page.getByRole('link', { name: '邮箱验证码' })
  await codeLink.press('Enter')
  assert.equal(await active.getAttribute('id'), await code.getAttribute('id'))

  await name.fill('测试用户')
  await email.fill(address('wrong-code'))
  assert.equal((await sendCode(page)).status(), 200)
  await code.fill('000000')
  await page.getByLabel('登录密码', { exact: true }).fill('Example-Password-80')
  await page.getByLabel('确认密码', { exact: true }).fill('Example-Password-80')
  await submit.press('Enter')
  await page.getByText('验证码不正确，请检查后重试。', { exact: true }).waitFor()
  assert.equal(await active.getAttribute('id'), await code.getAttribute('id'))
})

test('both registration passwords toggle independently and preserve values and selection', async t => {
  const page = await openPage(t)
  const password = page.getByLabel('登录密码', { exact: true })
  const confirmation = page.getByLabel('确认密码', { exact: true })
  const active = page.locator(':focus')
  assert.equal(await password.getAttribute('type'), 'password')
  assert.equal(await confirmation.getAttribute('type'), 'password')
  await confirmation.fill('Confirmation-80')
  await password.fill('Example-Password-80')
  await password.evaluate(input => input.setSelectionRange(2, 6, 'backward'))

  await page.keyboard.press('Tab')
  assert.equal(await active.getAttribute('aria-label'), '显示登录密码')
  await page.keyboard.press('Space')
  assert.equal(await active.getAttribute('aria-label'), '隐藏登录密码')
  assert.equal(await password.getAttribute('type'), 'text')
  assert.equal(await confirmation.getAttribute('type'), 'password')
  assert.deepEqual(await password.evaluate(input => [input.selectionStart, input.selectionEnd, input.selectionDirection]), [2, 6, 'backward'])

  await page.keyboard.press('Tab')
  assert.equal(await active.getAttribute('id'), await confirmation.getAttribute('id'))
  await page.keyboard.press('Tab')
  assert.equal(await active.getAttribute('aria-label'), '显示确认密码')
  await page.keyboard.press('Enter')
  assert.equal(await confirmation.getAttribute('type'), 'text')
  assert.equal(await password.getAttribute('type'), 'text')
  await screenshot(page, 'register-password-toggles-desktop.png')

  await password.focus()
  await password.evaluate(input => input.setSelectionRange(3, 3))
  await page.getByRole('button', { name: '隐藏登录密码' }).click()
  assert.equal(await active.getAttribute('id'), await password.getAttribute('id'))
  assert.equal(await password.evaluate(input => input.selectionStart), 3)
  assert.equal(await password.getAttribute('type'), 'password')
  assert.equal(await confirmation.getAttribute('type'), 'text')
  assert.equal(await password.inputValue(), 'Example-Password-80')
  assert.equal(await confirmation.inputValue(), 'Confirmation-80')
})

test('login distinguishes self-registration and admin help, then restores a new user session', async t => {
  const page = await openPage(t, '/login?redirect=/resumes', { width: 390, height: 844 })
  const signIn = page.getByRole('button', { name: '登录', exact: true })
  const signUp = page.getByRole('link', { name: '注册账号' })
  const adminHelp = page.getByText('如需系统分配账号或调整访问权限，请联系系统管理员。', { exact: true })
  await page.getByText('使用邮箱和密码登录。', { exact: true }).waitFor()
  const [loginBox, registerBox, adminBox] = await Promise.all([signIn.boundingBox(), signUp.boundingBox(), adminHelp.boundingBox()])
  assert.ok(loginBox.y < registerBox.y && registerBox.y < adminBox.y && adminBox.y + adminBox.height <= 844)
  await screenshot(page, 'login-paths-mobile.png')

  const email = address('new-account')
  const password = 'Example-Password-80'
  await signUp.click()
  assert.equal(new URL(page.url()).searchParams.get('redirect'), '/resumes')
  await page.getByLabel('显示姓名').fill('新注册用户')
  await page.getByLabel('邮箱地址').fill(email)
  assert.equal((await sendCode(page)).status(), 200)
  await page.getByLabel('邮箱验证码').fill('246810')
  await page.getByLabel('登录密码', { exact: true }).fill(password)
  await page.getByLabel('确认密码', { exact: true }).fill(password)
  await page.getByRole('button', { name: '完成注册' }).click()
  await page.getByRole('status').filter({ hasText: '注册成功，请使用新账号登录。' }).waitFor()
  assert.equal(new URL(page.url()).searchParams.get('redirect'), '/resumes')
  await signUp.waitFor()
  await adminHelp.waitFor()
  await screenshot(page, 'login-after-registration-mobile.png')

  await page.route('**/api/auth/getUserBaseInfo', route => route.fulfill({ status: 503, json: { code: 503 } }), { times: 1 })
  await page.getByLabel('邮箱', { exact: true }).fill(email)
  const passwordInput = page.getByLabel('密码', { exact: true })
  assert.equal(await passwordInput.getAttribute('type'), 'password')
  await passwordInput.fill(password)
  await page.getByRole('button', { name: '显示密码', exact: true }).press('Enter')
  assert.equal(await passwordInput.getAttribute('type'), 'text')
  await signIn.click()
  await page.getByRole('alert').filter({ hasText: '会话恢复失败' }).waitFor()
  await page.getByRole('button', { name: '重试恢复会话' }).click()
  await page.waitForURL('**/resumes')
})
