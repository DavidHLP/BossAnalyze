/// <reference lib="webworker" />

import { resumeDocx } from './resume-docx.ts'
import { resumePreview } from './resumes.ts'

declare const self: DedicatedWorkerGlobalScope

self.onmessage = async ({ data }: MessageEvent<string>) => {
  try {
    self.postMessage(await resumeDocx(resumePreview(data)))
  } catch {
    self.postMessage(null)
  }
}
