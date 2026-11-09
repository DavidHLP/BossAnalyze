import { sanitizeIpHeaders } from '../utils/client-ip.ts'

export default defineNitroPlugin((nitroApp) => {
  // Request hooks run before routeRules can return a proxied response.
  nitroApp.hooks.hook('request', (event) => {
    sanitizeIpHeaders(
      event.node.req.headers,
      event.node.req.socket.remoteAddress,
      process.env.BOSS_TRUSTED_PROXY_IPS,
    )
  })
})
