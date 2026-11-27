/// <reference lib="webworker" />

import { resumeDocx } from './resume-docx.ts'
import type { ResumeThemeId } from './resume-theme.ts'
import { resumePreview } from './resumes.ts'

declare const self: DedicatedWorkerGlobalScope

self.onmessage = async ({ data }: MessageEvent<{ content: string; compact?: boolean; themeId?: ResumeThemeId }>) => {
  try {
    self.postMessage(await resumeDocx(resumePreview(data.content), data.compact, data.themeId))
  } catch {
    self.postMessage(null)
  }
}
