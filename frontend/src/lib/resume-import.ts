const SECTION_ALIASES = new Set([
  '个人信息', '基本信息', '联系方式', '个人资料', 'personal information', 'personal details', 'contact', 'contact information',
  '教育经历', '教育背景', '教育', 'education', 'educational background', 'academic background',
  '工作经历', '工作经验', '实习经历', '工作履历', '职业经历', 'work experience', 'professional experience', 'experience',
  'employment', 'employment history', 'work history',
  '项目经历', '项目经验', 'projects', 'project', 'project experience',
  '技能', '专业技能', '技能特长', '技术技能', 'skills', 'skill', 'technical skills',
])

function sectionLabel(line: string) {
  const label = line.trim().replace(/[:：]\s*$/, '').trim()
  return SECTION_ALIASES.has(label.toLowerCase().replace(/\s+/g, ' ')) ? label : null
}

function bulletContent(line: string) {
  const match = /^[•·●▪◦‣∙]\s*(.+)$/.exec(line) ??
    /^[-*]\s+(.+)$/.exec(line) ??
    /^\d{1,3}、\s*(.+)$/.exec(line) ??
    /^[（(]\d{1,3}[）)]\s*(.+)$/.exec(line) ??
    /^[①-⑳]\s*(.+)$/.exec(line)
  return match?.[1] ?? null
}

function escapeInline(value: string) {
  return value.replaceAll('\\', '\\\\').replaceAll('|', '\\|').replaceAll('*', '\\*')
}

function literalLine(value: string) {
  let out = escapeInline(value)
  // Keep a date such as `2019. 09` and a leading `# ` as text instead of Markdown structure.
  if (/^#{1,3}\s/.test(value)) out = '\\' + out
  if (/^\d{1,9}[.)](?=\s|$)/.test(value)) out = out.replace(/^(\d{1,9})([.)])/, '$1\\$2')
  return out
}

export function importResumeText(text: string) {
  const lines = text.replace(/\r\n?/g, '\n').split('\n').map(line => line.trimEnd())
  const firstIndex = lines.findIndex(line => line.trim())
  const first = lines[firstIndex]
  const title = first && !sectionLabel(first) && !bulletContent(first) ? first.trim() : ''
  const body: string[] = title ? ['# ' + escapeInline(title)] : []
  let blank = false
  for (const line of title ? lines.slice(firstIndex + 1) : lines) {
    const value = line.trim()
    if (!value) {
      blank = body.length > 0
      continue
    }
    if (blank) body.push('')
    blank = false
    const section = sectionLabel(value)
    if (section) {
      body.push('## ' + escapeInline(section))
      continue
    }
    const bullet = bulletContent(value)
    body.push(bullet !== null ? '- ' + escapeInline(bullet) : literalLine(value))
  }
  return { title, content: body.join('\n') }
}

export function resumeBlank(title: string) {
  return '# ' + escapeInline(title.trim() || '未命名简历') + '\n'
}

export function resumeSkeleton(title: string) {
  return [
    '# ' + escapeInline(title.trim() || '我的简历'),
    '',
    '## 个人信息',
    '- 姓名：张三（示例，请替换）',
    '- 电话：138-0000-0000',
    '- 邮箱：zhangsan@example.com',
    '- 城市：北京',
    '',
    '## 教育经历',
    '- 2019 年 09 月 - 2023 年 06 月｜某某大学｜计算机科学与技术｜本科',
    '- 示例：主修数据结构、操作系统等课程，曾获校级奖学金。',
    '',
    '## 工作经历',
    '- 2023 年 07 月 - 至今｜某某公司｜前端开发工程师',
    '- 示例：负责 XX 功能开发，通过 XX 方法将 XX 指标从 A 提升到 B。',
    '',
    '## 项目经历',
    '- 项目名称｜担任角色｜起止时间',
    '- 示例：用一句话说明项目目标，再列出你的职责和可量化结果。',
    '',
    '## 技能',
    '- 示例：TypeScript、Vue、Node.js；英语 CET-6。',
  ].join('\n')
}
