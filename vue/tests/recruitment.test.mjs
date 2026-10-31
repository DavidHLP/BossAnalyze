import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFile } from 'node:fs/promises'
import {
  analysisQuery,
  jobSamples,
  jobSummary,
  optionNames,
  safeCity,
  salaryRanking,
  safeSourceUrl,
  scatterData,
} from '../app/lib/recruitment.ts'
const fixture = JSON.parse(await readFile(new URL('./fixtures/recruitment.json', import.meta.url)))
const jobs = jobSamples(fixture.jobs)

test('recruitment options and query values are allowlisted, including SQL-sensitive city lookup', () => {
  assert.deepEqual(optionNames(['北京', null, '', '北京', 'all', '上海']), ['北京', '上海'])
  assert.equal(safeCity("O'City"), false)
  const filter = { city: '北京', position: '前端开发', x: 'salary_value', y: 'degree_value' }
  assert.deepEqual(analysisQuery(filter, ['北京'], ['前端开发']), [
    '北京',
    '前端开发',
    'salary_value',
    'degree_value',
  ])
  for (const change of [
    { city: '未知城市' },
    { position: '未知职位' },
    { x: 'injected_column' },
    { y: 'salary_value' },
  ])
    assert.throws(() => analysisQuery({ ...filter, ...change }, ['北京'], ['前端开发']))
  assert.throws(() => analysisQuery({ ...filter, city: "O'City" }, ["O'City"], ['前端开发']))
})
test('job projection preserves null and zero, retains duplicate sample IDs and removes synthetic fallback text', () => {
  assert.equal(jobs[1].salaryValue, null)
  assert.equal(jobs[2].salaryValue, 0)
  assert.notEqual(jobs[0].key, jobs[1].key)
  assert.deepEqual(jobs[1].employeeBenefits, [])
  assert.deepEqual(jobs[1].jobRequirements, [])
  assert.equal('industry' in jobs[0], false)
  assert.deepEqual(jobSamples([]), [])
  for (const value of [
    { code: 500 },
    [null],
    [{ salaryValue: -1 }],
    [{ salaryValue: Infinity }],
    [{ employeeBenefits: [{}] }],
  ])
    assert.throws(() => jobSamples(value))
})
test('sample salary summary excludes unavailable values instead of averaging them as zero', () => {
  assert.deepEqual(jobSummary(jobs), { count: 3, companies: 2, salaryCount: 2, average: 12.5 })
  assert.equal(jobSummary([jobs[1]]).average, null)
  assert.deepEqual(jobSummary([]), { count: 0, companies: 0, salaryCount: 0, average: null })
})
test('ranking keeps absent city comparisons unknown and rejects invalid ranges or counts', () => {
  const rows = salaryRanking(fixture.ranking)
  assert.equal(rows[1].citySalary, null)
  assert.equal(rows[1].cityCount, null)
  assert.equal(rows[0].average, 30)
  assert.throws(() => salaryRanking([{ ...fixture.ranking[0], minSalary: 50 }]))
  assert.throws(() => salaryRanking([{ ...fixture.ranking[0], jobCount: 1.5 }]))
})
test('source links reject executable, relative and credential-bearing URLs', () => {
  assert.equal(
    safeSourceUrl('https://www.zhipin.com/job_detail/a.html'),
    'https://www.zhipin.com/job_detail/a.html',
  )
  for (const value of [
    null,
    'javascript:alert(1)',
    'data:text/html,test',
    '//example.test',
    '/relative',
    'https://user:secret@example.test',
  ])
    assert.equal(safeSourceUrl(value), null)
})
test('scatter aggregation preserves zero coordinates and all valid sample counts, with bounded rendering', () => {
  const plot = scatterData([...jobs, jobs[0]], 'salary_value', 'degree_value')
  assert.equal(plot.valid, 3)
  assert.equal(plot.groups, 2)
  assert.equal(
    plot.points.reduce((sum, point) => sum + point.count, 0),
    3,
  )
  assert.ok(plot.points.some((point) => point.x === 0 && point.y === 0))
  const constant = scatterData([jobs[0]], 'salary_value', 'degree_value')
  assert.equal(constant.points[0].cx, 330)
  assert.equal(constant.points[0].cy, 125)
  const many = Array.from({ length: 1201 }, (_, i) => ({ ...jobs[0], salaryValue: i }))
  const bounded = scatterData(many, 'salary_value', 'degree_value')
  assert.equal(bounded.valid, 1201)
  assert.equal(bounded.groups, 1201)
  assert.equal(bounded.points.length, 1000)
  assert.equal(scatterData([], 'salary_value', 'degree_value').valid, 0)
})
