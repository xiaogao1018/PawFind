export interface ApiResult<T> {
  code: number
  message: string
  data: T | null
}

export interface PageData<T> {
  page: number
  size: number
  total: number
  totalPages: number
  records: T[]
}
