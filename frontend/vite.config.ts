import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import { proxyApi } from './server/api-proxy.ts'
import { themeHeadScript } from './src/lib/theme.ts'

const api = {
  name: 'bossanalyze-api',
  configureServer(server) { server.middlewares.use(proxyApi) },
  configurePreviewServer(server) { server.middlewares.use(proxyApi) },
} satisfies import('vite').Plugin

const remixIcons = {
  name: 'bossanalyze-remix-icons',
  enforce: 'pre',
  transform(source, id) {
    if (!id.endsWith('/@remixicon/vue/index.mjs')) return
    // The registry package constructs every icon with defineComponent at module scope.
    // Annotate those calls so the build can omit icons this application never imports.
    const alias = /defineComponent as ([$\w]+)/.exec(source)?.[1]
    if (!alias) return
    const icon = new RegExp('=(' + alias.replace(/[$]/g, '\\$&') + ')(?=\\(\\{name:"Ri)', 'g')
    const annotated = source.replace(icon, '=/* @__PURE__ */$1')
    return annotated === source ? undefined : annotated
  },
} satisfies import('vite').Plugin

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
    api,
    remixIcons,
    {
      name: 'bossanalyze-theme',
      transformIndexHtml: () => [{ tag: 'script', children: themeHeadScript, injectTo: 'head-prepend' }],
    },
  ],
  server: { host: '127.0.0.1', port: 5020, strictPort: true },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
