import { ref } from 'vue'

// 定义接口类型
export interface Blog {
  id: string;
  title: string;
  summary: string;
  content: string;
  categoryId: string;
  categoryName: string;
  tags: Tag[];
  status: number;
  createTime: string;
}

export interface Category {
  id: string;
  name: string;
  description: string;
  createTime: string;
  sortOrder: number;
  parentId: string | null;
}

export interface Tag {
  id: string;
  name: string;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// 模拟数据
const mockCategories: Category[] = [
  {
    id: '1',
    name: '技术',
    description: '技术相关的文章和教程',
    createTime: '2023-01-15T08:30:00Z',
    sortOrder: 1,
    parentId: null
  },
  {
    id: '2',
    name: '前端开发',
    description: '前端开发相关技术和框架',
    createTime: '2023-01-16T09:15:00Z',
    sortOrder: 1,
    parentId: '1'
  },
  {
    id: '3',
    name: 'Vue.js',
    description: 'Vue.js框架学习和实践',
    createTime: '2023-01-17T10:20:00Z',
    sortOrder: 1,
    parentId: '2'
  },
  {
    id: '4',
    name: 'React',
    description: 'React框架学习和实践',
    createTime: '2023-01-18T11:25:00Z',
    sortOrder: 2,
    parentId: '2'
  },
  {
    id: '5',
    name: '后端开发',
    description: '后端开发相关技术和框架',
    createTime: '2023-01-19T12:30:00Z',
    sortOrder: 2,
    parentId: '1'
  },
  {
    id: '6',
    name: 'Spring Boot',
    description: 'Spring Boot框架学习和实践',
    createTime: '2023-01-20T13:35:00Z',
    sortOrder: 1,
    parentId: '5'
  },
  {
    id: '7',
    name: 'Node.js',
    description: 'Node.js技术学习和实践',
    createTime: '2023-01-21T14:40:00Z',
    sortOrder: 2,
    parentId: '5'
  },
  {
    id: '8',
    name: '生活',
    description: '生活方式和日常分享',
    createTime: '2023-01-22T15:45:00Z',
    sortOrder: 2,
    parentId: null
  },
  {
    id: '9',
    name: '美食',
    description: '美食制作和分享',
    createTime: '2023-01-23T16:50:00Z',
    sortOrder: 1,
    parentId: '8'
  },
  {
    id: '10',
    name: '旅行',
    description: '旅行经验和攻略',
    createTime: '2023-01-24T17:55:00Z',
    sortOrder: 2,
    parentId: '8'
  },
  {
    id: '11',
    name: '国内旅行',
    description: '国内旅行目的地推荐',
    createTime: '2023-01-25T18:00:00Z',
    sortOrder: 1,
    parentId: '10'
  },
  {
    id: '12',
    name: '国际旅行',
    description: '国际旅行目的地推荐',
    createTime: '2023-01-26T19:05:00Z',
    sortOrder: 2,
    parentId: '10'
  },
  {
    id: '13',
    name: '读书',
    description: '读书笔记和书评',
    createTime: '2023-01-27T20:10:00Z',
    sortOrder: 3,
    parentId: '8'
  },
  {
    id: '14',
    name: '分享',
    description: '资源分享和链接',
    createTime: '2023-01-28T21:15:00Z',
    sortOrder: 3,
    parentId: null
  }
];

const mockTags: Tag[] = [
  { id: '1', name: 'JavaScript' },
  { id: '2', name: 'Vue' },
  { id: '3', name: 'React' },
  { id: '4', name: 'TypeScript' },
  { id: '5', name: '后端' },
  { id: '6', name: '前端' }
]

// 模拟博客数据
let mockBlogs: Blog[] = [
  {
    id: '1',
    title: 'Vue3 组合式API详解',
    summary: 'Vue3的组合式API带来了全新的编程范式，本文详细解析其用法与优势',
    content: '# Vue3 组合式API详解\n\n组合式 API 是 Vue 3 引入的一组新 API，使我们可以使用函数而不是声明选项的方式书写 Vue 组件。\n\n## 为什么要有组合式 API？\n\n- **更好的逻辑复用**：组合式 API 使我们能够通过组合函数来实现更加简洁和灵活的逻辑复用。\n- **更灵活的代码组织**：我们可以把相关功能的代码组织在一起，而不是分散在不同的选项中。\n- **更好的类型推导**：组合式 API 对 TypeScript 的支持更加友好。',
    categoryId: '1',
    categoryName: '技术',
    tags: [{ id: '2', name: 'Vue' }, { id: '6', name: '前端' }],
    status: 1,
    createTime: '2023-10-15T10:30:00Z'
  },
  {
    id: '2',
    title: 'TypeScript高级类型系统',
    summary: '深入探讨TypeScript的类型系统，包括泛型、条件类型和映射类型',
    content: '# TypeScript高级类型系统\n\nTypeScript提供了非常强大的类型系统，本文我们深入探讨一些高级特性。\n\n## 泛型\n\n泛型是TypeScript最强大的特性之一，允许我们创建可重用的组件。\n\n## 条件类型\n\n条件类型使类型系统具有了条件判断的能力，极大增强了类型的表达能力。\n\n## 映射类型\n\n映射类型允许我们基于旧类型创建新类型，非常适合转换类型结构。',
    categoryId: '1',
    categoryName: '技术',
    tags: [{ id: '4', name: 'TypeScript' }, { id: '6', name: '前端' }],
    status: 1,
    createTime: '2023-11-20T14:45:00Z'
  },
  {
    id: '3',
    title: '日本旅行攻略',
    summary: '分享我在日本旅行的经验和推荐景点',
    content: '# 日本旅行攻略\n\n## 最佳旅行时间\n\n春季（3-5月）和秋季（9-11月）是访问日本的最佳时间，樱花和红叶季节尤其美丽。\n\n## 推荐景点\n\n### 东京\n- 浅草寺\n- 明治神宫\n- 涩谷十字路口\n\n### 京都\n- 伏见稻荷大社\n- 金阁寺\n- 岚山竹林',
    categoryId: '3',
    categoryName: '旅行',
    tags: [],
    status: 0,
    createTime: '2023-12-05T09:15:00Z'
  },
  {
    id: '4',
    title: '家常菜食谱分享',
    summary: '分享几道简单易做又美味的家常菜食谱',
    content: '# 家常菜食谱分享\n\n## 番茄炒蛋\n\n材料：番茄2个，鸡蛋3个，食用油，盐，糖\n\n做法：\n1. 番茄切块，鸡蛋打散\n2. 热锅冷油，倒入蛋液炒熟盛出\n3. 锅中加油，放入番茄翻炒出汁\n4. 加入炒好的鸡蛋，调味即可\n\n## 青椒土豆丝\n\n材料：土豆2个，青椒2个，醋，盐，花椒\n\n做法：\n1. 土豆和青椒切丝\n2. 土豆丝用水泡去淀粉\n3. 热锅冷油，放入花椒爆香\n4. 放入土豆丝翻炒3分钟\n5. 加入青椒丝翻炒1分钟\n6. 加醋、盐调味即可',
    categoryId: '4',
    categoryName: '美食',
    tags: [],
    status: 1,
    createTime: '2024-01-10T18:20:00Z'
  },
  {
    id: '5',
    title: 'React Hooks最佳实践',
    summary: '探讨React Hooks的常见模式和最佳实践',
    content: '# React Hooks最佳实践\n\nReact Hooks是React 16.8引入的特性，它允许我们在不编写class的情况下使用state和其他React特性。\n\n## useState\n\n`useState`是最基础的Hook，用于在函数组件中添加状态。\n\n```jsx\nconst [count, setCount] = useState(0);\n```\n\n## useEffect\n\n`useEffect`用于处理副作用，如数据获取、订阅或手动更改DOM。\n\n```jsx\nuseEffect(() => {\n  document.title = `You clicked ${count} times`;\n}, [count]);\n```\n\n## 自定义Hooks\n\n自定义Hooks是一种强大的逻辑复用机制。\n\n```jsx\nfunction useWindowSize() {\n  const [size, setSize] = useState({ width: 0, height: 0 });\n  \n  useEffect(() => {\n    const handleResize = () => {\n      setSize({ width: window.innerWidth, height: window.innerHeight });\n    };\n    \n    window.addEventListener(\'resize\', handleResize);\n    handleResize();\n    \n    return () => window.removeEventListener(\'resize\', handleResize);\n  }, []);\n  \n  return size;\n}\n```',
    categoryId: '1',
    categoryName: '技术',
    tags: [{ id: '3', name: 'React' }, { id: '6', name: '前端' }],
    status: 1,
    createTime: '2024-02-15T11:10:00Z'
  }
]

// 创建博客接口
export interface BlogCreateDTO {
  id: string;
  title: string;
  summary: string;
  content: string;
  categoryId: string;
  tags: string[];
  status: number;
}

// 模拟API函数
export const getBlogList = (params: any) => {
  // 解构查询参数
  const { page = 1, size = 10, keyword, categoryId, status } = params

  // 过滤博客
  let filteredBlogs = [...mockBlogs]

  if (keyword) {
    filteredBlogs = filteredBlogs.filter(blog =>
      blog.title.toLowerCase().includes(keyword.toLowerCase())
    )
  }

  if (categoryId) {
    filteredBlogs = filteredBlogs.filter(blog =>
      blog.categoryId === categoryId
    )
  }

  if (status !== undefined) {
    filteredBlogs = filteredBlogs.filter(blog =>
      blog.status === status
    )
  }

  // 计算分页
  const totalElements = filteredBlogs.length
  const totalPages = Math.ceil(totalElements / size)
  const startIndex = (page - 1) * size
  const content = filteredBlogs.slice(startIndex, startIndex + size)

  // 返回Promise以保持与真实API一致的接口
  return Promise.resolve({
    data: {
      content,
      totalElements,
      totalPages,
      size,
      number: page
    }
  })
}

// 获取博客详情
export const getBlogDetail = (id: string) => {
  const blog = mockBlogs.find(blog => blog.id === id)

  if (!blog) {
    return Promise.reject(new Error('博客不存在'))
  }

  return Promise.resolve({ data: blog })
}

// 创建博客
export const createBlog = (data: BlogCreateDTO) => {
  // 生成新ID
  const id = (Math.max(...mockBlogs.map(blog => parseInt(blog.id))) + 1).toString()

  // 查找分类名称
  const category = mockCategories.find(cat => cat.id === data.categoryId)
  const categoryName = category ? category.name : '未分类'

  // 查找标签详情
  const blogTags = data.tags && data.tags.length > 0
    ? mockTags.filter(tag => data.tags.includes(tag.id))
    : []

  // 创建新博客
  const newBlog: Blog = {
    id,
    title: data.title,
    summary: data.summary,
    content: data.content,
    categoryId: data.categoryId,
    categoryName,
    tags: blogTags,
    status: data.status,
    createTime: new Date().toISOString()
  }

  mockBlogs.unshift(newBlog)

  return Promise.resolve({ data: newBlog })
}

// 更新博客
export const updateBlog = (id: string, data: Partial<Omit<Blog, 'tags'>> & { tags?: string[] }) => {
  const index = mockBlogs.findIndex(blog => blog.id === id)

  if (index === -1) {
    return Promise.reject(new Error('博客不存在'))
  }

  // 更新分类名称
  let categoryName = mockBlogs[index].categoryName
  if (data.categoryId && data.categoryId !== mockBlogs[index].categoryId) {
    const category = mockCategories.find(cat => cat.id === data.categoryId)
    categoryName = category ? category.name : '未分类'
  }

  // 更新标签
  let blogTags = mockBlogs[index].tags
  if (data.tags) {
    blogTags = mockTags.filter(tag => data.tags!.includes(tag.id))
  }

  // 更新博客
  mockBlogs[index] = {
    ...mockBlogs[index],
    ...data,
    categoryName,
    tags: blogTags
  }

  return Promise.resolve({ data: mockBlogs[index] })
}

// 删除博客
export const deleteBlog = (id: string) => {
  const index = mockBlogs.findIndex(blog => blog.id === id)

  if (index === -1) {
    return Promise.reject(new Error('博客不存在'))
  }

  mockBlogs.splice(index, 1)

  return Promise.resolve({ data: null })
}

// 获取博客分类列表
export const getBlogCategories = () => {
  return Promise.resolve({ data: mockCategories })
}

// 获取博客标签列表
export const getBlogTags = () => {
  return Promise.resolve({ data: mockTags })
}

// 创建博客分类
export const createBlogCategory = (data: Omit<Category, 'id'>) => {
  const id = (Math.max(...mockCategories.map(cat => parseInt(cat.id))) + 1).toString()
  const newCategory = { id, name: data.name, description: data.description, createTime: new Date().toISOString(), sortOrder: 0, parentId: null }
  mockCategories.push(newCategory)
  return Promise.resolve({ data: newCategory })
}

// 更新博客分类
export const updateBlogCategory = (id: string, data: Partial<Category>) => {
  const index = mockCategories.findIndex(cat => cat.id === id)

  if (index === -1) {
    return Promise.reject(new Error('分类不存在'))
  }

  mockCategories[index] = { ...mockCategories[index], ...data }

  // 同步更新博客中的分类名称
  if (data.name) {
    mockBlogs.forEach(blog => {
      if (blog.categoryId === id) {
        blog.categoryName = data.name!
      }
    })
  }

  return Promise.resolve({ data: mockCategories[index] })
}

// 删除博客分类
export const deleteBlogCategory = (id: string) => {
  const index = mockCategories.findIndex(cat => cat.id === id)

  if (index === -1) {
    return Promise.reject(new Error('分类不存在'))
  }

  mockCategories.splice(index, 1)

  return Promise.resolve({ data: null })
}

// 创建博客标签
export const createBlogTag = (data: Omit<Tag, 'id'>) => {
  const id = (Math.max(...mockTags.map(tag => parseInt(tag.id))) + 1).toString()
  const newTag = { id, name: data.name }
  mockTags.push(newTag)
  return Promise.resolve({ data: newTag })
}

// 更新博客标签
export const updateBlogTag = (id: string, data: Partial<Tag>) => {
  const index = mockTags.findIndex(tag => tag.id === id)

  if (index === -1) {
    return Promise.reject(new Error('标签不存在'))
  }

  mockTags[index] = { ...mockTags[index], ...data }

  return Promise.resolve({ data: mockTags[index] })
}

// 删除博客标签
export const deleteBlogTag = (id: string) => {
  const index = mockTags.findIndex(tag => tag.id === id)

  if (index === -1) {
    return Promise.reject(new Error('标签不存在'))
  }

  mockTags.splice(index, 1)

  // 从博客中移除此标签
  mockBlogs.forEach(blog => {
    blog.tags = blog.tags.filter(tag => tag.id !== id)
  })

  return Promise.resolve({ data: null })
}
