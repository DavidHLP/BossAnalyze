const copySuffix = '（副本）'
const titleLimit = 120
const assetReference = /!\[([^\]]*)\]\(asset:([^)]+)\)/g

export function resumeCopyTitle(title: string) {
  const base = title.trim() || '未命名简历'
  const stem = base.endsWith(copySuffix) ? base.slice(0, -copySuffix.length) : base
  return stem.slice(0, titleLimit - copySuffix.length) + copySuffix
}

export function resumeCopyContent(content: string) {
  let imageCount = 0
  const text = content.replace(assetReference, (_match, alt: string, encoded: string) => {
    imageCount++
    let fileName = encoded
    try {
      fileName = decodeURIComponent(encoded)
    } catch {
      // A malformed reference still becomes a readable placeholder.
    }
    return '［图片：' + (alt.trim() || fileName) + '（副本未包含图片素材）］'
  })
  return { content: text, imageCount }
}

export function resumeCopySourceLabel(dirty: boolean) {
  return dirty ? '服务器已保存正文（不含编辑区未保存修改）' : '服务器已保存正文'
}

export function resumeCopyVerified(
  item: { title: string; content?: string | null } | null | undefined,
  title: string,
  content: string,
  commits: { content?: string | null }[],
) {
  return Boolean(item) && item!.title === title && (item!.content ?? '') === content &&
    commits.length === 1 && commits[0]?.content === content
}
