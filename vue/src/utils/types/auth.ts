export interface Request<T> {
  code: number
  data: T
  message: string
  timestamp: number
  [property: string]: unknown
}
