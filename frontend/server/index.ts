import { existsSync } from 'node:fs'
import { createServer } from 'node:http'
import { fileURLToPath } from 'node:url'
import sirv from 'sirv'
import { proxyApi } from './api-proxy.ts'

const dist = new URL('../dist/', import.meta.url)
if (!existsSync(new URL('index.html', dist))) throw new Error('Run pnpm build before pnpm start.')

const staticFiles = sirv(fileURLToPath(dist))
const application = sirv(fileURLToPath(dist), { single: true })
const server = createServer((req, res) => proxyApi(req, res, () => {
  if (req.method !== 'GET' && req.method !== 'HEAD') {
    res.writeHead(405, { Allow: 'GET, HEAD' }).end()
    return
  }
  const accept = req.headers.accept ?? ''
  const serve = accept.includes('text/html') ? application : staticFiles
  serve(req, res, () => { res.writeHead(404).end() })
}))

const port = Number(process.env.PORT ?? '5020')
const host = process.env.HOST ?? '127.0.0.1'
server.listen(port, host, () => console.log(`BossAnalyze frontend listening on ${host}:${port}`))
