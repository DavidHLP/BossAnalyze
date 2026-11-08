import { sanitizeIpHeaders } from '../utils/client-ip'

export default defineEventHandler((event) => {
  sanitizeIpHeaders(event.node.req.headers, event.node.req.socket.remoteAddress, process.env.BOSS_TRUSTED_PROXY_IPS)
})
