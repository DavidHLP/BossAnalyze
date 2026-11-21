import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'

// Run against the same-origin interaction-audit mock with Node's test runner.
const origin = process.env.RECRUITMENT_TEST_ORIGIN
const screenshots = process.env.RECRUITMENT_SCREENSHOTS
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE ?? 'playwright')
const recruitmentApi = '/api/v1/boss'
const analysisPath = recruitmentApi + '/user/two-dimensional-analysis-chart'
const rankingPath = recruitmentApi + '/user/salary-hot-job'
let browser
let token

before(async () => {
  assert.ok(origin, 'Set RECRUITMENT_TEST_ORIGIN to the local audit mock')
  const probe = await fetch(origin + recruitmentApi + '/basic/city-name-list')
  assert.equal(probe.headers.get('X-BossAnalyze-Preview'), 'mock')
  const login = await fetch(origin + '/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'demo@bossanalyze.invalid', password: 'demo-only' }),
  })
  assert.equal(login.status, 200)
  token = (await login.json()).data.token
  browser = await chromium.launch({ executablePath: process.env.CHROMIUM_EXECUTABLE })
})

after(async () => browser?.close())

async function openRecruitment(t, path = '/recruitment', options = {}) {
  const context = await browser.newContext({ reducedMotion: 'reduce', ...options })
  t.after(() => context.close())
  await context.addInitScript(({ origin, token }) => {
    if (location.origin !== origin) return
    localStorage.setItem('token', token)
    localStorage.setItem('token_timestamp', String(Date.now()))
  }, { origin, token })
  const page = await context.newPage()
  page.setDefaultTimeout(5000)
  await page.goto(origin + path)
  await page.getByRole('heading', { name: '招聘数据分析', exact: true }).waitFor()
  return page
}

async function screenshot(page, name) {
  if (screenshots) await page.screenshot({ path: screenshots + '/' + name + '.png', animations: 'disabled' })
}

test('reset keeps applied results until clear, then a copied URL and reload both query the defaults', async (t) => {
  const page = await openRecruitment(t,
    '/recruitment?city=北京&position=all&x=experience_value&y=salary_value&page=2&size=20&ref=audit')
  const ranking = page.getByRole('region', { name: '排行结果' })
  await ranking.waitFor()
  const existingRanking = await ranking.innerText()
  const analysis = page.getByRole('region', { name: /分布$/ })
  await analysis.getByText('已应用 北京 · 全部职位', { exact: true }).waitFor()
  const plot = page.getByRole('group', { name: /散点图/ })
  await plot.waitFor()
  const analysisRequests = []
  const rankingRequests = []
  page.on('request', (request) => {
    const url = new URL(request.url())
    if (url.pathname === analysisPath) analysisRequests.push(url.searchParams.get('cityName'))
    if (url.pathname === rankingPath) rankingRequests.push(url.href)
  })
  const city = page.getByRole('button', { name: '城市', exact: true })

  await page.getByRole('button', { name: '重置草稿', exact: true }).click()
  assert.equal(await city.innerText(), '全部城市')
  await page.getByText(/图表.*仍按已应用条件显示/).waitFor()
  await analysis.getByText('已应用 北京 · 全部职位', { exact: true }).waitFor()
  await plot.waitFor()
  assert.deepEqual(analysisRequests, [])
  assert.equal(new URL(page.url()).searchParams.get('city'), '北京')
  await screenshot(page, 'desktop-draft-reset')

  const draftXAxis = page.getByLabel('横轴 X')
  await draftXAxis.selectOption('experience_value')
  const clear = analysis.getByRole('button', { name: '清除并重新查询', exact: true })
  const [response] = await Promise.all([
    page.waitForResponse((response) => new URL(response.url()).pathname === analysisPath),
    clear.click(),
  ])
  assert.equal(response.status(), 200)
  assert.equal(response.headers()['x-bossanalyze-preview'], 'mock')
  assert.deepEqual(Object.fromEntries(new URL(response.url()).searchParams), {
    cityName: 'all', positionName: 'all', xAxis: 'salary_value', yAxis: 'degree_value',
  })
  const defaults = { city: 'all', position: 'all', x: 'salary_value', y: 'degree_value', page: '1', size: '20', ref: 'audit' }
  await page.waitForURL((url) => url.searchParams.get('city') === 'all')
  assert.deepEqual(Object.fromEntries(new URL(page.url()).searchParams), defaults)
  assert.equal(await draftXAxis.inputValue(), 'salary_value')
  await analysis.getByText('已应用 全部城市 · 全部职位', { exact: true }).waitFor()
  await plot.waitFor()
  assert.equal(await ranking.innerText(), existingRanking)
  assert.deepEqual(rankingRequests, [])
  await screenshot(page, 'desktop-cleared-results')

  const copied = await page.context().newPage()
  await copied.goto(page.url())
  await copied.getByRole('group', { name: /散点图/ }).waitFor()
  assert.deepEqual(Object.fromEntries(new URL(copied.url()).searchParams), defaults)
  assert.equal(await copied.getByRole('button', { name: '城市', exact: true }).innerText(), '全部城市')
  await page.reload()
  await plot.waitFor()
  await analysis.getByText('已应用 全部城市 · 全部职位', { exact: true }).waitFor()
  assert.deepEqual(analysisRequests, ['all', 'all'])
})

test('city search replaces the display text, defers filtering during IME, and refreshes positions only after selection', async (t) => {
  const page = await openRecruitment(t, '/recruitment?city=北京&position=前端工程师', {
    userAgent: 'Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/140.0.0.0 Mobile Safari/537.36',
  })
  await page.getByRole('group', { name: /散点图/ }).waitFor()
  const city = page.getByLabel('城市', { exact: true })
  const position = page.getByLabel('职位', { exact: true })
  const positionPath = recruitmentApi + '/basic/position-name-list'
  const positionRequests = []
  page.on('request', (request) => {
    const url = new URL(request.url())
    if (url.pathname === positionPath) positionRequests.push(url.searchParams.get('cityName'))
  })

  await city.click()
  const shanghai = page.getByRole('option', { name: '上海', exact: true })
  const search = page.getByRole('combobox', { name: '搜索城市' })
  await shanghai.waitFor()
  assert.equal(await search.inputValue(), '')
  await page.keyboard.insertText('上海')
  await page.getByRole('option', { name: '北京', exact: true }).waitFor({ state: 'hidden' })
  await shanghai.waitFor()
  await screenshot(page, 'desktop-city-search')
  await page.keyboard.press('Escape')
  await shanghai.waitFor({ state: 'hidden' })
  assert.equal(await city.evaluate((element) => document.activeElement === element), true)
  assert.equal(await city.innerText(), '北京')
  assert.deepEqual(positionRequests, [])

  await city.click()
  await search.waitFor()
  const cdp = await page.context().newCDPSession(page)
  await cdp.send('Input.imeSetComposition', { text: 'shang', selectionStart: 5, selectionEnd: 5 })
  assert.equal(await search.inputValue(), 'shang')
  await shanghai.waitFor()
  await page.getByRole('option', { name: '北京', exact: true }).waitFor()
  assert.deepEqual(positionRequests, [])
  await cdp.send('Input.insertText', { text: '上海' })
  await shanghai.waitFor()
  await page.getByRole('option', { name: '北京', exact: true }).waitFor({ state: 'hidden' })

  const [response] = await Promise.all([
    page.waitForResponse((response) => new URL(response.url()).pathname === positionPath),
    page.keyboard.press('Enter'),
  ])
  assert.equal(response.status(), 200)
  assert.deepEqual(positionRequests, ['上海'])
  assert.equal(await position.innerText(), '全部职位')
  assert.equal(await city.innerText(), '上海')
  const { data: positions } = await response.json()
  await position.click()
  await page.getByRole('option', { name: positions[0], exact: true }).waitFor()
  await page.keyboard.press('Escape')
  await page.getByText('已应用 北京 · 前端工程师', { exact: true }).waitFor()
  assert.equal(new URL(page.url()).searchParams.get('city'), '北京')
})

test('at 390 pixels, chart and samples share the query and three full rows restore their focus and scroll', async (t) => {
  const page = await openRecruitment(t, '/recruitment?city=上海&page=2&size=10', {
    viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true,
  })
  const plot = page.getByRole('group', { name: /散点图/ })
  await plot.waitFor()
  await page.getByText('全量坐标分组明细', { exact: true }).tap()
  await page.getByRole('region', { name: '当前页坐标分组' }).waitFor()
  const switchView = page.getByRole('radiogroup', { name: '样本视图' })
  const panel = await page.getByRole('region', { name: /分布$/ }).boundingBox()
  const switchBox = await switchView.boundingBox()
  assert.ok(switchBox.x + switchBox.width <= panel.x + panel.width)
  await switchView.getByText('明细', { exact: true }).tap()
  await plot.waitFor({ state: 'hidden' })
  const samples = page.getByRole('region', { name: '职位样本明细' })
  const list = samples.getByRole('list', { name: '当前页职位样本' })
  await list.waitFor()
  assert.equal(await list.getByRole('button').count(), 10)
  assert.equal(await list.evaluate((element) => {
    for (let node = element; node && node.tagName !== 'SECTION'; node = node.parentElement) {
      if (/auto|scroll/.test(getComputedStyle(node).overflowY)) return false
    }
    return true
  }), true)
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth), 390)
  assert.equal(await samples.getByRole('button', { name: '第 2 页', exact: true }).getAttribute('aria-current'), 'page')

  const opened = new Set()
  for (const index of [5, 6, 7]) {
    const row = list.getByRole('button').nth(index)
    await row.scrollIntoViewIfNeeded()
    const box = await row.boundingBox()
    const title = (await row.innerText()).split('\n')[0]
    const url = page.url()
    await row.tap({ position: { x: box.width - 4, y: box.height - 4 } })
    const sheet = page.getByRole('dialog', { name: title, exact: true })
    await sheet.waitFor()
    const id = await sheet.getByText(/^demo-\d+$/).innerText()
    opened.add(id)
    if (index === 7) await screenshot(page, 'mobile-job-detail')
    await sheet.getByRole('button', { name: '关闭', exact: true }).first().tap()
    await sheet.waitFor({ state: 'hidden' })
    await page.waitForFunction(() => document.activeElement?.closest('ul[aria-label="当前页职位样本"]'))
    assert.equal(await row.evaluate((element) => document.activeElement === element), true, id)
    assert.ok(Math.abs((await row.boundingBox()).y - box.y) <= 2, id)
    assert.equal(page.url(), url)
  }
  assert.equal(opened.size, 3)
  await screenshot(page, 'mobile-samples-restored')

  await switchView.getByText('图表', { exact: true }).tap()
  await plot.waitFor()
  await page.getByRole('region', { name: '当前页坐标分组' }).waitFor()
  await switchView.getByText('明细', { exact: true }).tap()
  await list.waitFor()
  assert.equal(new URL(page.url()).searchParams.get('city'), '上海')
  assert.equal(new URL(page.url()).searchParams.get('page'), '2')
})
