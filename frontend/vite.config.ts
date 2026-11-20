import { fileURLToPath, URL } from 'node:url'
import tailwindcss from '@tailwindcss/vite'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import { proxyApi } from './server/api-proxy'

const api = {
  name: 'bossanalyze-api',
  configureServer(server) { server.middlewares.use(proxyApi) },
  configurePreviewServer(server) { server.middlewares.use(proxyApi) },
} satisfies import('vite').Plugin

export default defineConfig({
  plugins: [vue(), tailwindcss(), api],
  server: { host: '127.0.0.1', port: 5020, strictPort: true },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
