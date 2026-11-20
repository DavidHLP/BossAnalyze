import { existsSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { createFrontendServer } from './frontend.ts'

const dist = new URL('../dist/', import.meta.url)
if (!existsSync(new URL('index.html', dist))) throw new Error('Run pnpm build before pnpm start.')

const server = createFrontendServer(fileURLToPath(dist))
const port = Number(process.env.PORT ?? '5020')
const host = process.env.HOST ?? '127.0.0.1'
server.listen(port, host, () => console.log(`BossAnalyze frontend listening on ${host}:${port}`))
