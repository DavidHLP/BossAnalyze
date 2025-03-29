/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_BASE_URL: string
  readonly BASE_URL: string
  // 其他环境变量...
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

import type { Permissions } from '@/api/auth/auth.d'

declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $hasPermission: (userPermissions: Permissions[], requiredPermissions: Permissions) => boolean
  }
}
