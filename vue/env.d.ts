/// <reference types="vite/client" />
interface ImportMetaEnv {
  readonly VITE_APP_BASE_URL: string;
  // 其他环境变量...
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    permissions?: string[]
  }
}
