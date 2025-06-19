export interface IResumeConfig {
  content: string
  style: string
  link: string
  name: string
  type?: number
}

const UPSTASH_BASE_URL = import.meta.env.VITE_UPSTASH_BASE_URL as string

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

export function getExportCount() {
  return new Promise((resolve, reject) => {
    fetch(`${UPSTASH_BASE_URL}/get/count`, {
      headers: {
        Authorization: import.meta.env.VITE_UPSTASH_GET_TOKEN as string,
      },
    })
      .then((response) => response.json())
      .then((data) => resolve(data.result))
      .catch(reject)
  })
}
