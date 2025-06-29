import { ref } from 'vue'

const initialCVState: Map<string, string[]> = new Map()

// 创作模板的默认配置
initialCVState.set('create', ['#333', '#333', '', '25'])

type Module = {
  default: SubModule
}

type SubModule = {
  type: string
  id: number
  name: string
  font?: string
  lineHeight?: number
  content: string
  primaryColor: string
  primaryBackground: string
  img: string
  hot?: number | string
}
export type TemplateType = SubModule

export const templates = ref<SubModule[]>([])

const moduleEntries = Object.entries(import.meta.glob('./modules/*/index.ts', { eager: true }))

for (const [path, curModule] of moduleEntries) {
  const content = (curModule as Module).default
  content.id = Math.ceil(Math.random() * 1000000000)
  content.type = path.split('/')[2]
  templates.value.push(content)
  initialCVState.set(content.type, [
    content.primaryColor,
    content.primaryBackground,
    content.font || '',
    String(content.lineHeight || 25)
  ])
}

const match = (module: SubModule) => {
  const matchResult = module.type.match(/^\d+/)
  if (matchResult) {
    return +matchResult[0]
  }
  // 对于没有数字前缀的模板，返回0作为默认排序值
  return 0
}
templates.value.sort((a, b) => match(b) - match(a))

export function getPrimaryBGColor(type: string) {
  return (initialCVState.get(type) as string[])[1]
}

export function getPrimaryColor(type: string) {
  return (initialCVState.get(type) as string[])[0]
}

export function getFontFamily(type: string) {
  return (initialCVState.get(type) as string[])[2]
}
