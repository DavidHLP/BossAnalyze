import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFile } from 'node:fs/promises'
import {
  analysisQuery,
  analysisStatusTones,
  defaultAnalysisFilter,
  degreeLabel,
  formatSampleSalary,
  jobSamples,
  jobSummary,
  optionNames,
  readRecruitmentQuery,
  safeCity,
  salaryRanking,
  sameAnalysisFilter,
  safeSourceUrl,
  scatterData,
  scatterTicks,
} from '../src/lib/recruitment.ts'
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

test('the failed applied scope can be compared independently of the current draft', () => {
  const applied = { city: '北京', position: '前端开发', x: 'salary_value', y: 'degree_value' }
  const args = analysisQuery(applied, ['北京', '上海'], ['前端开发'])
  assert.equal(sameAnalysisFilter(applied, { ...applied }), true)
  for (const change of [
    { city: '上海' }, { position: 'all' }, { x: 'experience_value' }, { y: 'experience_value' },
  ]) {
    assert.equal(sameAnalysisFilter(applied, { ...applied, ...change }), false)
  }
  assert.deepEqual(args, ['北京', '前端开发', 'salary_value', 'degree_value'])
})

test('recruitment links restore only dictionary-backed conditions and supported pagination', () => {
  const filter = { city: '北京', position: '前端开发', x: 'degree_value', y: 'salary_value' }
  const cities = ['北京', "O'City"]
  const positions = ['前端开发']
  assert.deepEqual(
    readRecruitmentQuery({ ...filter, page: '3', size: '50', unrelated: 'keep' }, cities, positions),
    { filter, page: 3, size: 50 },
  )
  assert.deepEqual(
    readRecruitmentQuery({
      city: "O'City", position: ['前端开发', 'all'], x: 'injected_column', y: 'salary_value',
      page: '9007199254740992', size: '100',
    }, cities, positions),
    { filter: { ...defaultAnalysisFilter, x: 'degree_value', y: 'salary_value' }, page: 1, size: 20 },
  )
  assert.deepEqual(
    readRecruitmentQuery({ city: 'all', x: 'experience_value', y: 'experience_value', size: '10' }, [], []),
    { filter: defaultAnalysisFilter, page: 1, size: 10 },
  )
})

test('recruitment links without valid conditions or pagination leave analysis idle', () => {
  for (const query of [
    {},
    { unrelated: 'value' },
    { city: '未知城市', position: '未知职位' },
    { city: ['北京'], position: null, x: ['salary_value'], page: ['2'], size: ['20'] },
    { x: 'degree_value', y: 'degree_value' },
    { page: '0', size: '25' },
    { page: '1e2', size: 20 },
  ]) {
    assert.equal(readRecruitmentQuery(query, ['北京'], ['前端开发']), null)
  }
  assert.deepEqual(
    readRecruitmentQuery({ page: '2' }, [], []),
    { filter: defaultAnalysisFilter, page: 2, size: 20 },
  )
})

test('sample table separates K from the amount while preserving source text and zero', () => {
  assert.equal(formatSampleSalary('20-30K·13薪'), '20-30 K·13薪')
  assert.equal(formatSampleSalary('0K'), '0 K')
  assert.equal(formatSampleSalary('20 K'), '20 K')
  assert.equal(formatSampleSalary('面议'), '面议')
  assert.equal(formatSampleSalary(null), '—')
})

test('scatter ticks use domain levels and keep a single centered tick for a constant axis', () => {
  assert.deepEqual(scatterTicks('degree_value', 0, 6).map(degreeLabel), [
    '不限', '初中', '高中/中专', '大专', '本科', '硕士', '博士',
  ])
  assert.deepEqual(scatterTicks('degree_value', 4, 4), [4])
  assert.deepEqual(scatterTicks('experience_value', 0, 10), [0, 1, 2, 4, 7, 10])
  assert.deepEqual(scatterTicks('salary_value', 5, 20), [5, 12.5, 20])
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
  assert.equal(plot.points.length, 2)
  assert.equal(
    plot.points.reduce((sum, point) => sum + point.count, 0),
    3,
  )
  assert.ok(plot.points.some((point) => point.x === 0 && point.y === 0))
  const constant = scatterData([jobs[0]], 'salary_value', 'degree_value')
  assert.equal(constant.points[0].cx, 330)
  assert.equal(constant.points[0].cy, 125)
  assert.deepEqual(constant.xTicks, [{ value: 25, x: 330 }])
  assert.deepEqual(constant.yTicks, [{ value: 4, y: 125 }])
  const many = Array.from({ length: 1201 }, (_, i) => ({ ...jobs[0], salaryValue: i }))
  const bounded = scatterData(many, 'salary_value', 'degree_value')
  assert.equal(bounded.valid, 1201)
  assert.equal(bounded.groups, 1201)
  assert.equal(bounded.points.length, 1000)
  assert.equal(bounded.groupLimit, 1000)
  assert.equal(bounded.allGroups.length, 1201)
  assert.equal(bounded.allGroups.reduce((sum, point) => sum + point.count, 0), 1201)
  assert.deepEqual(bounded.allGroups.at(-1), { x: 1200, y: jobs[0].degreeValue, count: 1 })
  assert.equal(scatterData([], 'salary_value', 'degree_value').valid, 0)
})

test('decimal salaries survive projection and unknown dimensions are excluded only from affected axes', () => {
  const samples = jobSamples([
    { salaryValue: 4.5, degreeValue: 0, experienceValue: 0 },
    { salaryValue: 5, degreeValue: null, experienceValue: 2 },
    { salaryValue: 6, degreeValue: 4, experienceValue: null },
  ])
  assert.equal(jobSummary(samples).average, (4.5 + 5 + 6) / 3)
  const degree = scatterData(samples, 'salary_value', 'degree_value')
  assert.equal(degree.valid, 2)
  assert.deepEqual(degree.allGroups[0], { x: 4.5, y: 0, count: 1 })
  assert.equal(scatterData(samples, 'degree_value', 'experience_value').valid, 1)
  assert.equal(scatterData(samples, 'salary_value', 'experience_value').valid, 2)
})

test('analysis provenance stays explicit and array punctuation survives normalization', () => {
  const requirements = ['Java, Python', ' [A] and (B) ', 'say "hi"', '  ', '']
  const [job] = jobSamples([
    {
      jobRequirements: requirements,
      analysisStatus: 'failed',
      analysisModel: 'typesafe/jev-1.14.0',
      analysisVersion: 'jev-1',
    },
  ])
  assert.deepEqual(job.jobRequirements, requirements)
  assert.equal(job.analysisStatus, 'failed')
  assert.equal(job.analysisModel, 'typesafe/jev-1.14.0')
  assert.equal(jobSamples([{}])[0].analysisStatus, 'unknown')
  assert.equal(jobSamples([{ analysisStatus: 'future_status' }])[0].analysisStatus, 'unknown')
  assert.equal(analysisStatusTones.running, 'info')
  for (const analysisStatus of [
    'unparsed',
    'legacy_unverified',
    'pending',
    'running',
    'complete',
    'no_evidence',
    'needs_review',
    'failed',
    'stale',
  ])
    assert.equal(jobSamples([{ analysisStatus }])[0].analysisStatus, analysisStatus)
})
