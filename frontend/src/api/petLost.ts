import request from './request'
import type { ApiResult, PageData } from '../types/common'

export interface PetLost {
  id: number
  userId: number
  petName: string | null
  species: string
  description: string | null
  lostTime: string
  lostLocation: string
  status: 'LOOKING' | 'FOUND' | 'WITHDRAWN' | string
  visibilityStatus?: string
  moderationReason?: string | null
  createdAt: string
}

export interface PetLostPayload {
  petName: string | null
  species: string
  description: string | null
  lostTime: string
  lostLocation: string
}

export async function listPetLost(page = 1, size = 10) {
  const response = await request.get<ApiResult<PageData<PetLost>>>('/api/pet-lost', { params: { page, size } })
  return response.data
}
export async function getPetLost(id: number) {
  const response = await request.get<ApiResult<PetLost>>(`/api/pet-lost/${id}`)
  return response.data
}
export async function createPetLost(data: PetLostPayload) {
  const response = await request.post<ApiResult<PetLost>>('/api/pet-lost', data)
  return response.data
}
export async function updatePetLost(id: number, data: PetLostPayload) {
  const response = await request.put<ApiResult<PetLost>>(`/api/pet-lost/${id}`, data)
  return response.data
}
