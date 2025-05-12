import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import vueSetupName from 'vite-plugin-vue-setup-extend'
import { fileURLToPath } from 'url'
// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools(), vueSetupName()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    port: 5020,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
    open: true,
    hmr: {
      overlay: true,
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/assets/style/variables.scss" as *; @use "@/assets/style/theme.scss" as *;`,
      },
    },
  },
})
