import { createServer, type OutgoingHttpHeaders, type ServerResponse } from 'node:http'
import { extname } from 'node:path'
import send from 'send'
import { proxyApi } from './api-proxy.ts'

type StaticError = { statusCode?: number; headers?: OutgoingHttpHeaders }

function sendError(res: ServerResponse, error: StaticError) {
  if (res.headersSent) {
    res.destroy()
    return
  }
  for (const header of res.getHeaderNames()) res.removeHeader(header)
  res.writeHead(error.statusCode ?? 500, error.headers).end()
}

export function createFrontendServer(dist: string) {
  const options = { root: dist }
  return createServer((req, res) => proxyApi(req, res, () => {
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      res.writeHead(405, { Allow: 'GET, HEAD' }).end()
      return
    }
    const [pathname = '/'] = (req.url ?? '/').split('?', 1)
    function notFound() {
      // send has already verified that the pathname can be decoded.
      const path = decodeURIComponent(pathname)
      const assets = ['/assets', '/.well-known'].some(prefix => path === prefix || path.startsWith(prefix + '/'))
      if (req.headers.accept?.includes('text/html') && !assets &&
          (path.startsWith('/console/') || !extname(path))) {
        send(req, '/index.html', { ...options, acceptRanges: false })
          .on('error', (error: StaticError) => sendError(res, error))
          .pipe(res)
      } else {
        sendError(res, { statusCode: 404 })
      }
    }

    send(req, pathname, options)
      .on('directory', notFound)
      .on('error', (error: StaticError) => {
        if (error.statusCode === 404 && !res.hasHeader('Content-Length')) notFound()
        else sendError(res, error)
      })
      .pipe(res)
  }))
}
