import tailwindcss from '@tailwindcss/vite'

export default defineNuxtConfig({
  css: ['~/assets/css/tailwind.css'],
  compatibilityDate: '2025-01-01',
  devServer: { host: '127.0.0.1', port: 5020 },
  app: { head: { title: 'BossAnalyze', htmlAttrs: { lang: 'zh-CN' } } },
  routeRules: {
    '/console': { ssr: false },
    '/console/**': { ssr: false },
    '/api/v1/boss/**': { proxy: 'http://127.0.0.1:8082/api/v1/boss/**' },
    '/api/v1/ai/**': { proxy: 'http://127.0.0.1:8081/api/v1/ai/**' },
    '/api/**': { proxy: 'http://127.0.0.1:8080/api/**' },
  },
  vite: {
    plugins: [tailwindcss()],
    define: {
      'import.meta.env.VITE_EXPORT_URL': JSON.stringify(process.env.VITE_EXPORT_URL) ?? 'undefined',
      'import.meta.env.VITE_UPSTASH_BASE_URL':
        JSON.stringify(process.env.VITE_UPSTASH_BASE_URL) ?? 'undefined',
      'import.meta.env.VITE_UPSTASH_GET_TOKEN':
        JSON.stringify(process.env.VITE_UPSTASH_GET_TOKEN) ?? 'undefined',
    },
  },
  modules: ['shadcn-nuxt'],
  shadcn: { prefix: 'Ui', componentDir: '@/components/ui' },
})
