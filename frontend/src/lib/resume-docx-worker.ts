/// <reference lib="webworker" />

import { resumeDocx } from './resume-docx.ts'
import { resumePreview } from './resumes.ts'

declare const self: DedicatedWorkerGlobalScope

self.onmessage = async ({ data }: MessageEvent<{ content: string; compact?: boolean }>) => {
  try {
    self.postMessage(await resumeDocx(resumePreview(data.content), data.compact))
  } catch {
    self.postMessage(null)
  }
}
