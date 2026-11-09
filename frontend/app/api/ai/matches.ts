import request from '@/utils/request/request'

export interface MatchRequest {
  resume?: string
  resumeId?: string
  city: string[]
  position: string
}
export interface MatchDimension {
  name: 'skills' | 'relevant_experience' | 'experience' | 'education'
  status: 'scored' | 'unknown' | 'not_applicable'
  rawScore: number | null
  confidence: number | null
  probabilities: Record<string, number> | null
  evidence: { sourcePath: string; start: number; end: number; text: string }[]
}
export interface MatchItem {
  jobId: string
  status: 'scored' | 'insufficient_evidence' | 'failed'
  score: number | null
  dimensions: MatchDimension[]
}
export interface MatchEnvelope {
  items: MatchItem[]
  unrankable: MatchItem[]
  consideredCount: number
  scope: { cities: string[]; position: string; limit: 100; truncated: boolean }
  complete: boolean
  model: string
  policyVersion: string
}
export const getMatches = (input: MatchRequest): Promise<MatchEnvelope> =>
  request.post('/api/v2/ai/matches', input, { timeout: 40000 })
