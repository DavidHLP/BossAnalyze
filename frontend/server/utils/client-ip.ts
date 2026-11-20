import { isIP } from 'node:net'

const literal = (ip: string) => !ip.includes('%') && isIP(ip)

// Trust exact socket peers only; the configured edge must overwrite X-Forwarded-For.
export function clientIp(peer: string | undefined, forwarded: string | string[] | undefined, trusted = '') {
  const proxies = trusted.split(',').map((ip) => ip.trim()).filter((ip) => literal(ip))
  if (peer && proxies.includes(peer) && typeof forwarded === 'string' && literal(forwarded)) return forwarded
  return peer && literal(peer) ? peer : undefined
}

export function sanitizeIpHeaders(headers: Record<string, string | string[] | undefined>, peer: string | undefined, trusted = '') {
  const ip = clientIp(peer, headers['x-forwarded-for'], trusted)
  for (const name of Object.keys(headers)) {
    if (name === 'forwarded' || name === 'x-real-ip' || name === 'x-boss-client-ip' || name.startsWith('x-forwarded-')) delete headers[name]
  }
  if (ip) headers['x-boss-client-ip'] = ip
}
