import { request, type IncomingHttpHeaders, type IncomingMessage, type ServerResponse } from 'node:http'
import { sanitizeIpHeaders } from './utils/client-ip.ts'

const connectionHeaders = [
  'connection', 'keep-alive', 'proxy-authenticate', 'proxy-authorization',
  'proxy-connection', 'te', 'trailer', 'transfer-encoding', 'upgrade',
]

function endToEndHeaders(headers: IncomingHttpHeaders) {
  const outgoing = { ...headers }
  const named = (headers.connection ?? '').split(',').map(name => name.trim().toLowerCase())
  for (const name of [...connectionHeaders, ...named]) delete outgoing[name]
  return outgoing
}

export function apiPort(path: string) {
  const pathname = path.split('?', 1)[0]
  if (pathname === '/api') return 8080
  if (!pathname?.startsWith('/api/')) return null
  return /^\/api\/v[12]\/ai(?:\/|$)/.test(pathname) ? 8081 : 8080
}

export function proxyApi(req: IncomingMessage, res: ServerResponse, next: () => void) {
  const path = req.url ?? '/'
  const port = apiPort(path)
  if (port === null) return next()

  const headers = endToEndHeaders(req.headers)
  delete headers.host
  sanitizeIpHeaders(headers, req.socket.remoteAddress, process.env.BOSS_TRUSTED_PROXY_IPS)

  const upstream = request({ host: '127.0.0.1', port, path, method: req.method, headers }, response => {
    res.writeHead(response.statusCode ?? 502, endToEndHeaders(response.headers))
    response.on('error', () => res.destroy())
    response.pipe(res)
  })
  upstream.setTimeout(45_000, () => upstream.destroy())
  upstream.on('error', () => {
    if (res.headersSent) {
      res.destroy()
      return
    }
    res.writeHead(502, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ code: 502, message: '后端服务暂不可用' }))
  })
  req.on('error', () => upstream.destroy())
  res.on('close', () => { if (!res.writableEnded) upstream.destroy() })
  req.pipe(upstream)
}
