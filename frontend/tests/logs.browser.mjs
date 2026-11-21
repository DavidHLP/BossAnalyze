// Run with Node's test runner against the isolated interaction-audit Mock frontend.
// Set LOGS_MOCK_URL and, if needed, PLAYWRIGHT_MODULE and CHROMIUM_PATH.
// From frontend: mise exec node@26.7.0 -- pnpm exec node --test tests/logs.browser.mjs
import assert from 'node:assert/strict'
import { join } from 'node:path'
import { after, before, test } from 'node:test'

const { chromium } = await import(process.env.PLAYWRIGHT_MODULE ?? 'playwright')
const baseURL = process.env.LOGS_MOCK_URL
assert.match(baseURL ?? '', /^http:\/\/(127\.0\.0\.1|localhost):\d+$/)

let browser
before(async () => {
  browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH })
})
after(async () => browser?.close())

async function logsSession(t, { viewport = { width: 1280, height: 900 }, configure } = {}) {
  const context = await browser.newContext({ baseURL, viewport, hasTouch: viewport.width < 640 })
  t.after(() => context.close())
  await configure?.(context)
  const page = await context.newPage()
  await page.goto('/console/logs')
  await signIn(page)
  return { context, page }
}

async function signIn(page) {
  await page.getByLabel('邮箱').fill('demo@bossanalyze.invalid')
  await page.getByLabel('密码', { exact: true }).fill('demo-only')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await logsHeading(page).waitFor()
}

const logsHeading = page => page.getByRole('heading', { name: '扩展日志分析' })
const trigger = page => page.getByRole('button', { name: '手动触发日志分析' })
const confirm = page => page.getByRole('button', { name: '确认触发' })
const receipt = page => page.getByText('后台分析任务已受理', { exact: true })
const cooldown = page => page.getByRole('button', { name: /冷却中/ })
const triggerRoute = '**/api/v1/system/logs/analysis/trigger'

async function screenshot(locator, name) {
  if (process.env.LOGS_SCREENSHOTS) {
    await locator.screenshot({ path: join(process.env.LOGS_SCREENSHOTS, name + '.png') })
  }
}

test('同账号跨标签竞争只提交一次，受理后跨路由和刷新保留回执，30 秒后解锁', async t => {
  const { context, page } = await logsSession(t)
  await page.getByLabel('breadcrumb').getByRole('link', { name: '数据看板', exact: true }).click()
  const openLogs = page.getByRole('main').getByRole('link', { name: '扩展日志分析', exact: true })
  await openLogs.click()
  await logsHeading(page).waitFor()
  const second = await context.newPage()
  await second.goto('/console/logs')
  await logsHeading(second).waitFor()
  await page.clock.install()
  await second.clock.install()
  let posts = 0
  const seen = Promise.withResolvers()
  const release = Promise.withResolvers()
  await context.route(triggerRoute, async route => {
    posts++
    seen.resolve()
    await release.promise
    await route.fulfill({ status: 200, contentType: 'text/plain', body: '分析任务已触发' })
  })

  await trigger(page).click()
  await trigger(second).click()
  await Promise.all([confirm(page).dispatchEvent('click'), confirm(second).dispatchEvent('click')])
  await seen.promise
  await second.getByRole('button', { name: '正在触发…' }).first().waitFor({ timeout: 2000 })
  assert.equal(await receipt(second).count(), 0)

  await page.goBack()
  await openLogs.click()
  await logsHeading(page).waitFor()
  assert.ok(await page.getByRole('button', { name: '正在触发…' }).isDisabled({ timeout: 2000 }))
  release.resolve()
  await receipt(page).waitFor({ timeout: 3000 })
  await receipt(second).waitFor({ timeout: 3000 })
  await second.reload()
  await receipt(second).waitFor({ timeout: 3000 })
  assert.ok(await cooldown(second).isDisabled())
  await screenshot(second, 'p1-trigger-receipt')
  assert.equal(posts, 1)

  await second.clock.fastForward(29_000)
  assert.ok(await cooldown(second).isDisabled())
  await second.clock.fastForward(2_000)
  await trigger(second).click()
  await confirm(second).click()
  await receipt(second).waitFor()
  assert.equal(posts, 2)
})

test('读取日志区分初次延迟、同一快照、读取失败、空缓存与真零', async t => {
  const time = '2026-11-20T10:00:00.000Z'
  let phase = 'sample'
  let delay = Promise.withResolvers()
  const { page } = await logsSession(t, {
    async configure(context) {
      await context.route('**/api/v1/system/logs/**', async route => {
        const path = new URL(route.request().url()).pathname
        const dashboard = path.endsWith('/dashboard/stats')
        const totals = path.endsWith('/logs/stats')
        if (phase === 'empty') return route.fulfill({ status: 204 })
        await delay.promise
        if (!dashboard && !totals) return route.fallback()
        if (totals && phase === 'error') {
          return route.fulfill({ status: 503, json: { code: 503 } })
        }
        return route.fulfill({
          status: 200,
          json: { code: 200, data: dashboard
            ? { generatedAt: time, weekdayStatistics: { weekdayStats: { 6: { count: 0 } } } }
            : { totalVisits: phase === 'zero' ? 0 : 13, uniqueIps: phase === 'zero' ? 0 : 6 } },
        })
      })
    },
  })
  const metrics = page.getByRole('region', { name: '快照指标' })
  const busy = page.getByRole('button', { name: '正在读取…', exact: true })
  const refresh = page.getByRole('button', { name: '刷新缓存', exact: true })
  const checked = page.getByText('已检查，暂无新快照', { exact: true })
  const snapshotTime = page.getByText('快照时间 ' + time, { exact: true })

  await busy.waitFor({ timeout: 2000 })
  assert.equal(await metrics.getByText('—', { exact: true }).count(), 0)
  await screenshot(metrics, 'p3-initial-loading')
  delay.resolve()
  await snapshotTime.waitFor()
  await metrics.getByText('13', { exact: true }).waitFor()
  delay = Promise.withResolvers()
  await refresh.click()
  assert.ok(await busy.isDisabled())
  assert.ok(await metrics.getByText('13', { exact: true }).isVisible())
  assert.ok(await snapshotTime.isVisible())
  await screenshot(page, 'p3-refresh-in-progress')
  delay.resolve()
  await checked.waitFor()
  assert.ok(await snapshotTime.isVisible())
  await screenshot(page, 'p3-unchanged-snapshot')

  phase = 'error'
  await refresh.click()
  const retry = page.getByRole('button', { name: '重试读取概览' })
  await retry.waitFor()
  assert.ok(await metrics.getByText('13', { exact: true }).isVisible())
  phase = 'zero'
  await retry.click()
  await metrics.getByText('0', { exact: true }).first().waitFor()
  assert.equal(await metrics.getByText('0', { exact: true }).count(), 2)
  await screenshot(metrics, 'p3-zero-snapshot')

  phase = 'empty'
  await refresh.click()
  await metrics.getByText('暂无可用的日志快照', { exact: true }).waitFor()
  assert.equal(await metrics.getByText('0', { exact: true }).count(), 0)
  assert.equal(await snapshotTime.count(), 0)
  assert.equal(await retry.count(), 0)
})

test('390px 下趋势和热力图提供精确时段的触控入口，并保留方向键导航', async t => {
  const { page } = await logsSession(t, { viewport: { width: 390, height: 844 } })
  await page.goto('/console/dashboard')
  const trendPicker = page.getByRole('combobox', { name: '选择趋势时段' })
  await trendPicker.waitFor({ timeout: 2500 })
  await touchTarget(trendPicker)
  const trendHour = await trendPicker.getByRole('option', { name: '06:00 · 148 次访问' }).getAttribute('value')
  await trendPicker.selectOption(trendHour)
  const trend = page.getByRole('heading', { name: '访问趋势' }).locator('../../..')
  const selectedHour = trend.getByText(trendHour + ' 时', { exact: true })
  await selectedHour.locator('..').getByText('148', { exact: true }).waitFor()
  await screenshot(trend, 'p2-mobile-trend')
  const bars = page.getByRole('radiogroup', { name: '每小时访问量' }).getByRole('radio')
  await bars.first().focus()
  await bars.first().press('ArrowRight')
  assert.equal(await bars.nth(1).getAttribute('aria-checked'), 'true')
  assert.ok(await page.getByText('小时明细', { exact: false }).first().isVisible())

  await page.getByRole('main').getByRole('link', { name: '扩展日志分析' }).click()
  const heat = page.getByRole('region', { name: '可横向滚动的访问热力表' })
  const heatPicker = page.getByRole('combobox', { name: '选择热力图时段' })
  await page.getByText('左右滑动查看更多时间', { exact: true }).waitFor()
  await touchTarget(heatPicker)
  await touchTarget(heat.getByRole('button').first())
  assert.ok(await heat.getByRole('columnheader', { name: '日期' }).isVisible())
  await page.getByRole('button', { name: '分钟热力', exact: true }).tap()
  const minuteHour = await page.getByRole('combobox', { name: '选择分钟热力的小时' }).inputValue()
  await heatPicker.selectOption(minuteHour + ':00')
  await page.getByText('当前焦点 · ' + minuteHour + ':00 · 0 次访问', { exact: true }).waitFor()
  await screenshot(page.getByRole('heading', { name: '访问时间热力图' }).locator('../..'), 'p2-mobile-heatmap')
  const zero = heat.getByRole('button', { name: minuteHour + ':00 · 0 次访问', exact: true })
  await zero.focus()
  await zero.press('ArrowRight')
  assert.equal(await heat.getByRole('button', { name: minuteHour + ':01 · 无记录', exact: true }).getAttribute('tabindex'), '0')
  assert.equal(await heatPicker.inputValue(), minuteHour + ':01')
})

async function touchTarget(locator) {
  const box = await locator.boundingBox()
  assert.ok(box && box.width >= 24 && box.height >= 24, '触控区域至少为 24×24px')
}

test('发出 POST 后立刻刷新，缺失回执时保留冷却并显示未确认', async t => {
  const { context, page } = await logsSession(t)
  const seen = Promise.withResolvers()
  let posts = 0
  await context.route(triggerRoute, async route => {
    posts++
    seen.resolve()
    // Leave the response pending until browser navigation interrupts the request.
    await page.waitForEvent('domcontentloaded')
    await route.abort().catch(() => {})
  })
  await trigger(page).click()
  await confirm(page).click()
  await seen.promise
  await page.reload()

  await page.getByText(/启动结果未确认/).waitFor({ timeout: 2500 })
  assert.equal(await receipt(page).count(), 0)
  assert.ok(await cooldown(page).isDisabled())
  assert.equal(posts, 1)
})

test('同一浏览器切换账号时隔离回执，拒绝与未确认结果都不冒充受理', async t => {
  const { context, page } = await logsSession(t)
  let status = 200
  let secondAccount = false
  await context.route('**/api/auth/getUserBaseInfo', async route => {
    if (!secondAccount) return route.fallback()
    const response = await route.fetch()
    const json = await response.json()
    await route.fulfill({ response, json: { ...json, data: { ...json.data, id: 101, email: 'other@bossanalyze.invalid' } } })
  })
  await context.route(triggerRoute, route => route.fulfill({
    status, contentType: 'text/plain', body: status === 200 ? '分析任务已触发' : '模拟触发失败',
  }))

  await trigger(page).click()
  await confirm(page).click()
  await receipt(page).waitFor()
  secondAccount = true
  await signOut(page)
  await signIn(page)
  assert.equal(await receipt(page).count(), 0)
  await trigger(page).click()
  status = 403
  await confirm(page).click()
  await page.getByText('当前账号缺少手动触发权限。', { exact: true }).waitFor()
  assert.equal(await receipt(page).count(), 0)
  assert.ok(await trigger(page).isEnabled())

  status = 503
  await trigger(page).click()
  await confirm(page).click()
  await page.getByText(/启动结果未确认/).waitFor()
  assert.equal(await receipt(page).count(), 0)
  assert.ok(await cooldown(page).isDisabled())

  secondAccount = false
  await signOut(page)
  await signIn(page)
  await receipt(page).waitFor()
  assert.ok(await cooldown(page).isDisabled())
})

async function signOut(page) {
  await page.getByRole('button', { name: '账号菜单' }).click()
  await page.getByRole('menuitem', { name: '退出登录' }).click()
  await page.getByLabel('邮箱').waitFor()
  await page.goto('/login?redirect=/console/logs')
}
