export interface IResumeConfig {
  content: string
  style: string
  link: string
  name: string
  type?: number
}

export async function resumeExport(data: IResumeConfig) {
  const res = await fetch(import.meta.env.VITE_EXPORT_URL as string, {
    method: 'POST',
    body: JSON.stringify(data),
    headers: {
      'Content-Type': 'application/json',
    },
  })
  return await res.json()
}

// Export statistics are not enabled; never ship private datastore credentials to browsers.
export async function getExportCount() {
  return 0
}
