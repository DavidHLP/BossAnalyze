import type { HostUrl } from '@/api/repeater/hosturl'
import type { PortUrl } from '@/api/repeater/porturl'
import type { RouterUrl } from '@/api/repeater/routerurl'
import type { ProjectUrl } from '@/api/repeater/projecturl'

export interface RoleUrl {
  id?: number
  originalId?: number
  roleName: string
  protocol: string
  hostId: number
  host?: HostUrl
  portId: number
  port?: PortUrl
  routerId: number
  router?: RouterUrl
  projectId: number
  project?: ProjectUrl
  method: string
  isActive: boolean
  description?: string
  createdAt?: string
}
