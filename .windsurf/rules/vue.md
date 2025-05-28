---
trigger: always_on
---

# Vue3 + TypeScript 前端开发规范

## 技术栈

- Vue 3
- TypeScript
- Element Plus
- ECharts
- BoostVue-next
- Composition API (setup 语法糖)

## 组件开发规范

### 文件命名

- 组件文件使用 PascalCase 命名法，如：`UserProfile.vue`
- 工具类文件使用 camelCase 命名法，如：`formatDate.ts`
- 常量文件使用全大写下划线分隔，如：`API_CONSTANTS.ts`
- 通用业务组件前缀使用 `Bs`，如：`BsTable.vue`、`BsForm.vue`

## TypeScript 规范

- 始终为 props、emits、状态和函数返回值定义类型
- 使用 interface 而非 type 定义对象结构
- 复杂类型应放在单独的 types.ts 文件中
- 使用 TypeScript 的非空断言（!）和可选链（?.）操作符提高代码健壮性
- 优先使用 `type` 关键字进行类型定义和类型推断
- 禁止使用 `any` 类型，必要时可使用 `unknown` 类型代替
- 对于 API 返回数据类型，应在 `api/模块名称/*.d.ts` 中定义接口类型

## Element Plus 使用规范

- 使用按需导入减小打包体积
- 统一使用组合式 API 风格的 Element Plus 组件
- 自定义主题应在全局样式文件中定义
- 表单验证规则应统一维护
- 尽量使用 Element Plus 内置组件，避免无必要的第三方组件引入
- 表格组件应统一封装，支持常用的分页、排序、过滤等功能
- 弹窗组件应统一使用预设的尺寸（small, default, large）

## ECharts 使用规范

- 图表配置选项应使用 computed 属性生成
- 图表实例应在组件卸载时销毁
- 复杂图表逻辑应封装为可复用的组合式函数
- 建立统一的图表主题配置，支持明暗两套主题
- 大数据量图表应考虑性能优化和数据分片处理
- 移动端图表应优化触摸交互和响应式布局
- 统一使用 `useEcharts` 钩子函数管理图表生命周期

## BoostVue-next 使用规范

- 项目中所有业务组件应基于 BoostVue-next 进行开发
- 使用 BoostVue 内置的主题变量和组件，保持设计一致性
- 自定义组件应遵循 BoostVue 的设计规范和接口约定
- 使用 BoostVue 提供的国际化方案
- 对于扩展 BoostVue 组件的场景，保持原有的 API 设计风格
- 重复使用率高的业务组件应考虑贡献到 BoostVue 组件库

## 代码风格与质量

- 使用 ESLint + Prettier 进行代码风格检查和格式化
- 遵循项目中定义的 .eslintrc.js 和 .prettierrc 规则
- 使用 husky 和 lint-staged 在提交前进行代码检查
- 单个文件不超过 300 行代码，单个函数不超过 80 行
- 公共逻辑必须编写单元测试，测试覆盖率不低于 80%
- 代码审查必须至少通过一位团队成员的 review

## 状态管理

- 简单组件内部状态使用 ref/reactive 管理
- 跨组件状态使用 Pinia 进行管理
- Pinia store 应按业务模块划分
- Store 内的 action 应处理异步逻辑，通常包含 API 调用
- 避免在组件中直接调用 API，统一通过 store 管理数据流
- 复杂表单状态可使用 vee-validate 或 FormKit 管理

## 路由和权限管理

- 路由配置应采用模块化组织，按功能模块拆分
- 使用路由元信息（meta）定义页面权限、标题、图标等信息
- 实现基于角色的访问控制（RBAC）
- 使用路由守卫进行权限校验和登录状态检查
- 动态路由应基于用户权限生成
- 路由懒加载应用于所有非首屏路由

## 性能优化

- 组件应尽可能使用 shallowRef/shallowReactive 减少响应式开销
- 大列表渲染使用虚拟滚动技术（如 vue-virtual-scroller）
- 使用 v-memo 避免不必要的组件重渲染
- 合理使用 v-once 优化静态内容
- 图片资源应使用 WebP 格式并启用懒加载
- 大型第三方库使用动态导入
- 使用 Webpack Bundle Analyzer 或 Rollup Visualizer 分析打包体积

## 项目结构

```
src/
├── assets/          # 静态资源
│   ├── fonts/       # 字体文件
│   ├── images/      # 图片资源
│   └── styles/      # 全局样式
├── components/      # 通用组件
│   ├── common/      # 基础通用组件
│   └── business/    # 业务组件
├── composables/     # 组合式函数
├── constants/       # 常量定义
├── directives/      # 自定义指令
├── hooks/           # 自定义 hooks
├── layouts/         # 布局组件
├── router/          # 路由配置
│   ├── modules/     # 路由模块
│   ├── guards/      # 路由守卫
│   └── index.ts     # 路由入口
├── services/        # API 服务
│   ├── modules/     # API 模块
│   ├── interceptors/# 请求拦截器
│   └── index.ts     # API 入口
├── stores/          # 状态管理
│   ├── modules/     # Store 模块
│   └── index.ts     # Store 入口
├── types/           # 类型定义
├── utils/           # 工具函数
│   ├── auth.ts      # 认证相关
│   ├── date.ts      # 日期处理
│   ├── storage.ts   # 存储相关
│   └── request.ts   # 请求封装
└── views/           # 页面组件
    ├── console/     # 控制台页面
    ├── system/      # 系统管理
    └── error/       # 错误页面
```

## 最佳实践

- 使用 `<script setup>` 语法糖简化组件代码
- 使用组合式函数 (composables) 抽取和复用逻辑
- 优先使用 CSS 变量实现主题定制
- 图表组件应支持响应式调整
- 复杂表单应拆分为多个子组件
- 使用 provide/inject 传递深层组件依赖
- 使用 defineExpose 明确暴露组件内部方法和状态
- 组件内的事件处理函数统一使用 handle 前缀
- API 调用函数统一使用 fetch 前缀
- 工具函数应编写详细的 JSDoc 注释
- 复杂逻辑应添加必要的代码注释
- 及时处理和更新项目依赖，避免安全漏洞

## 国际化处理

- 使用 vue-i18n 实现多语言支持
- 翻译文件按模块组织，避免单个文件过大
- 支持按需加载语言包
- 组件内文本禁止硬编码，统一使用 t 函数
- 日期和数字格式化应遵循目标地区习惯
- 考虑文本长度在不同语言下的差异，做好 UI 适配