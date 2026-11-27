export type ResumeThemeId = 'basic' | 'modern'

export function resolveResumeThemeId(value: unknown): ResumeThemeId {
  return value === 'modern' ? 'modern' : 'basic'
}

export const resumeThemes = {
  basic: {
    label: '简洁',
    headings: [
      { size: 18, weight: 600, color: '#161616', before: 0, after: 0.4, rule: false },
      { size: 12, weight: 600, color: '#161616', before: 0.8, after: 0.4, rule: true },
      { size: 11, weight: 600, color: '#161616', before: 0.8, after: 0.4, rule: true },
    ],
    ruleColor: '#D6D6D6',
    linkColor: '#161616',
  },
  modern: {
    label: '现代',
    headings: [
      { size: 22, weight: 700, color: '#194F6B', before: 0, after: 0.55, rule: false },
      { size: 13, weight: 700, color: '#194F6B', before: 1, after: 0.45, rule: true },
      { size: 11, weight: 700, color: '#161616', before: 0.65, after: 0.3, rule: false },
    ],
    ruleColor: '#8EA7B4',
    linkColor: '#194F6B',
  },
} as const

export function resumeThemeAttribute(value: unknown) {
  return resolveResumeThemeId(value) === 'modern' ? 'modern' : undefined
}

export const resumeThemeCss = (['basic', 'modern'] as const).map(id => {
  const { headings, ruleColor, linkColor } = resumeThemes[id]
  const headingVariables = headings.map(({ size, weight, color, before, after, rule }, index) => {
    const prefix = '--resume-h' + (index + 1)
    return `
      ${prefix}-size: ${size}pt;
      ${prefix}-weight: ${weight};
      ${prefix}-color: ${color};
      ${prefix}-before: ${before}em;
      ${prefix}-after: ${after}em;
      ${prefix}-rule: ${rule ? '1px solid var(--resume-rule-color)' : '0'};
      ${prefix}-padding: ${rule ? '0.2em' : '0'};
    `
  }).join('')
  return `.resume-body${id === 'modern' ? '[data-resume-theme="modern"]' : ''} {
    --resume-rule-color: ${ruleColor};
    --resume-link-color: ${linkColor};
    ${headingVariables}
  }`
}).join('\n')
